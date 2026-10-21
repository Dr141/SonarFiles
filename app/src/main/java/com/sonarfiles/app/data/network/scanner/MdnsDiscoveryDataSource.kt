package com.sonarfiles.app.data.network.scanner

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredMdnsService(
    val serviceName: String,
    val serviceType: String,
    val hostAddress: String?,
    val port: Int
)

@Singleton
class MdnsDiscoveryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager: NsdManager?
        get() = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    fun discoverServices(serviceTypes: List<String> = listOf("_smb._tcp.", "_sftp-ssh._tcp.")): Flow<DiscoveredMdnsService> = callbackFlow {
        val manager = nsdManager
        if (manager == null) {
            close()
            return@callbackFlow
        }

        val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()

        serviceTypes.forEach { serviceType ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String?) {}
                override fun onDiscoveryStopped(serviceType: String?) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    if (serviceInfo == null) return
                    try {
                        manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {}

                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                                if (resolvedInfo == null) return
                                val host: InetAddress? = resolvedInfo.host
                                val ip = host?.hostAddress ?: return

                                trySend(
                                    DiscoveredMdnsService(
                                        serviceName = resolvedInfo.serviceName ?: "Unknown",
                                        serviceType = resolvedInfo.serviceType ?: serviceType,
                                        hostAddress = ip,
                                        port = resolvedInfo.port
                                    )
                                )
                            }
                        })
                    } catch (_: Exception) {
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            }

            activeListeners.add(listener)
            try {
                manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (_: Exception) {
            }
        }

        awaitClose {
            activeListeners.forEach { listener ->
                try {
                    manager.stopServiceDiscovery(listener)
                } catch (_: Exception) {
                }
            }
        }
    }
}
