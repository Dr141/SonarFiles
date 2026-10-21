package com.sonarfiles.app.data.network.scanner

import com.sonarfiles.app.core.common.DispatchersProvider
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeOnLanSender @Inject constructor(
    private val dispatchers: DispatchersProvider
) {

    /**
     * Monta o Magic Packet (6x 0xFF + 16x MAC Address) e envia via Broadcast UDP
     * nas portas padrão 9 e 7.
     */
    suspend fun sendMagicPacket(
        macAddress: String,
        broadcastIp: String = "255.255.255.255"
    ): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val macBytes = parseMacAddress(macAddress)
                ?: throw IllegalArgumentException("Endereço MAC inválido: $macAddress")

            // Magic Packet: 6 bytes 0xFF + 16x 6 bytes do MAC = 102 bytes
            val magicPacketData = ByteArray(6 + 16 * macBytes.size)
            for (i in 0 until 6) {
                magicPacketData[i] = 0xFF.toByte()
            }
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, magicPacketData, 6 + i * macBytes.size, macBytes.size)
            }

            val targetAddress = InetAddress.getByName(broadcastIp)
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true

                // Dispara nas portas 9 (padrão) e 7 (Echo/Discard WoL)
                for (port in listOf(9, 7)) {
                    val packet = DatagramPacket(magicPacketData, magicPacketData.size, targetAddress, port)
                    socket.send(packet)
                }
            } finally {
                socket?.close()
            }
        }
    }

    private fun parseMacAddress(mac: String): ByteArray? {
        val clean = mac.replace(":", "").replace("-", "").trim()
        if (clean.length != 12) return null

        val bytes = ByteArray(6)
        return try {
            for (i in 0 until 6) {
                val byteHex = clean.substring(i * 2, i * 2 + 2)
                bytes[i] = byteHex.toInt(16).toByte()
            }
            bytes
        } catch (_: Exception) {
            null
        }
    }
}
