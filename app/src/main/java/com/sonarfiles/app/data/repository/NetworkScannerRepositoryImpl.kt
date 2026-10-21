package com.sonarfiles.app.data.repository

import com.sonarfiles.app.core.common.DispatchersProvider
import com.sonarfiles.app.data.network.scanner.MdnsDiscoveryDataSource
import com.sonarfiles.app.data.network.scanner.NetBiosResolver
import com.sonarfiles.app.data.network.scanner.PortProber
import com.sonarfiles.app.data.network.scanner.SubnetCalculator
import com.sonarfiles.app.data.network.scanner.WakeOnLanSender
import com.sonarfiles.app.domain.model.DiscoveredHost
import com.sonarfiles.app.domain.model.OsHint
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.repository.NetworkScannerRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkScannerRepositoryImpl @Inject constructor(
    private val subnetCalculator: SubnetCalculator,
    private val portProber: PortProber,
    private val netBiosResolver: NetBiosResolver,
    private val mdnsDiscoveryDataSource: MdnsDiscoveryDataSource,
    private val wakeOnLanSender: WakeOnLanSender,
    private val dispatchers: DispatchersProvider
) : NetworkScannerRepository {

    override fun scanSubnet(): Flow<DiscoveredHost> = channelFlow {
        val subnetInfo = subnetCalculator.getLocalSubnet() ?: return@channelFlow
        val discoveredIps = ConcurrentHashMap<String, DiscoveredHost>()

        // 1. Escuta passiva via mDNS em paralelo
        val mdnsJob = launch(dispatchers.io) {
            mdnsDiscoveryDataSource.discoverServices().collect { mdnsService ->
                val ip = mdnsService.hostAddress ?: return@collect
                val protocols = mutableSetOf<ProtocolType>()
                val openPorts = mutableSetOf<Int>()

                if (mdnsService.port == 445 || mdnsService.serviceType.contains("smb")) {
                    protocols.add(ProtocolType.SMB)
                    openPorts.add(445)
                }
                if (mdnsService.port == 22 || mdnsService.serviceType.contains("sftp") || mdnsService.serviceType.contains("ssh")) {
                    protocols.add(ProtocolType.SFTP)
                    openPorts.add(22)
                }

                val existing = discoveredIps[ip]
                val updatedHost = if (existing != null) {
                    existing.copy(
                        hostname = existing.hostname ?: mdnsService.serviceName,
                        supportedProtocols = existing.supportedProtocols + protocols,
                        openPorts = existing.openPorts + openPorts
                    )
                } else {
                    DiscoveredHost(
                        ipAddress = ip,
                        hostname = mdnsService.serviceName,
                        supportedProtocols = protocols,
                        openPorts = openPorts,
                        osHint = if (protocols.contains(ProtocolType.SMB)) OsHint.WINDOWS else OsHint.LINUX,
                        responseTimeMs = 15L
                    )
                }
                discoveredIps[ip] = updatedHost
                send(updatedHost)
            }
        }

        // 2. Varredura ativa de portas TCP com limite estrito de concorrência (Semaphore 48)
        val concurrencyLimit = Semaphore(permits = 48)
        coroutineScope {
            subnetInfo.targetIps.forEach { ip ->
                launch(dispatchers.io) {
                    concurrencyLimit.withPermit {
                        val smbProbe = async { portProber.probePort(ip, 445, timeoutMs = 380) }
                        val sftpProbe = async { portProber.probePort(ip, 22, timeoutMs = 380) }

                        val smbResult = smbProbe.await()
                        val sftpResult = sftpProbe.await()

                        // Fallback porta 139 (NetBIOS Session) se 445 estiver fechada
                        val netbiosResult = if (!smbResult.isOpen) {
                            portProber.probePort(ip, 139, timeoutMs = 300)
                        } else null

                        val isSmbOpen = smbResult.isOpen || (netbiosResult?.isOpen == true)
                        val isSftpOpen = sftpResult.isOpen

                        if (isSmbOpen || isSftpOpen) {
                            val openPorts = mutableSetOf<Int>()
                            val protocols = mutableSetOf<ProtocolType>()
                            var bestLatency = Long.MAX_VALUE

                            if (smbResult.isOpen) {
                                openPorts.add(445)
                                protocols.add(ProtocolType.SMB)
                                bestLatency = minOf(bestLatency, smbResult.latencyMs)
                            }
                            if (netbiosResult?.isOpen == true) {
                                openPorts.add(139)
                                protocols.add(ProtocolType.SMB)
                                bestLatency = minOf(bestLatency, netbiosResult.latencyMs)
                            }
                            if (isSftpOpen) {
                                openPorts.add(22)
                                protocols.add(ProtocolType.SFTP)
                                bestLatency = minOf(bestLatency, sftpResult.latencyMs)
                            }

                            // Resolução de nome de máquina via NetBIOS / DNS reverso
                            val hostInfo = netBiosResolver.resolveHostname(ip, timeoutMs = 400)

                            val osHint = when {
                                hostInfo.isWindowsOrSamba || isSmbOpen -> OsHint.WINDOWS
                                isSftpOpen -> OsHint.LINUX
                                else -> OsHint.UNKNOWN
                            }

                            val host = DiscoveredHost(
                                ipAddress = ip,
                                hostname = hostInfo.hostname,
                                supportedProtocols = protocols,
                                openPorts = openPorts,
                                osHint = osHint,
                                responseTimeMs = if (bestLatency != Long.MAX_VALUE) bestLatency else 10L
                            )

                            discoveredIps[ip] = host
                            send(host)
                        }
                    }
                }
            }
        }

        mdnsJob.cancel()
    }.flowOn(dispatchers.io)

    override suspend fun sendWakeOnLan(
        macAddress: String,
        broadcastIp: String
    ): Result<Unit> {
        val calculatedBroadcast = subnetCalculator.getLocalSubnet()?.broadcastIp ?: broadcastIp
        return wakeOnLanSender.sendMagicPacket(macAddress, calculatedBroadcast)
    }
}
