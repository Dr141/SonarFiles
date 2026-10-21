package com.sonarfiles.app.data.network.scanner

import com.sonarfiles.app.core.common.DispatchersProvider
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

data class HostResolvedInfo(
    val hostname: String?,
    val isWindowsOrSamba: Boolean
)

@Singleton
class NetBiosResolver @Inject constructor(
    private val dispatchers: DispatchersProvider
) {

    /**
     * Tenta resolver o nome NetBIOS via UDP 137 (Padrão Windows/Samba).
     * Se falhar ou timeout, tenta DNS reverso padrão.
     */
    suspend fun resolveHostname(ip: String, timeoutMs: Int = 400): HostResolvedInfo = withContext(dispatchers.io) {
        val netBiosName = queryNetBiosName(ip, timeoutMs)
        if (!netBiosName.isNullOrBlank()) {
            return@withContext HostResolvedInfo(
                hostname = netBiosName,
                isWindowsOrSamba = true
            )
        }

        // Fallback: DNS reverso padrão
        val dnsName = try {
            val addr = InetAddress.getByName(ip)
            val host = addr.hostName
            if (host != ip && host.isNotBlank()) host else null
        } catch (_: Exception) {
            null
        }

        HostResolvedInfo(
            hostname = dnsName,
            isWindowsOrSamba = false
        )
    }

    private fun queryNetBiosName(ip: String, timeoutMs: Int): String? {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            // NetBIOS Node Status Request Packet (RFC 1002)
            val request = byteArrayOf(
                0x82.toByte(), 0x28.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x20.toByte(), // Name length (32 bytes)
                // NetBIOS encoded '*' name: 'CKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
                0x43.toByte(), 0x4B.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x21.toByte(), // Type: NBSTAT
                0x00.toByte(), 0x01.toByte()  // Class: IN
            )

            val targetAddress = InetAddress.getByName(ip)
            val packet = DatagramPacket(request, request.size, targetAddress, 137)
            socket.send(packet)

            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            val data = receivePacket.data
            // Verifica se a resposta contém contagem de nomes no offset 56
            if (data.size > 57) {
                val numNames = data[56].toInt() and 0xFF
                if (numNames > 0) {
                    // O primeiro nome de 15 caracteres inicia no offset 57
                    val rawName = String(data, 57, 15, Charsets.US_ASCII).trim()
                    if (rawName.isNotBlank()) {
                        return rawName
                    }
                }
            }
        } catch (_: Exception) {
            // Timeout ou não respondeu a NetBIOS
        } finally {
            socket?.close()
        }
        return null
    }
}
