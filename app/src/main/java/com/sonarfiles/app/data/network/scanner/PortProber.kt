package com.sonarfiles.app.data.network.scanner

import com.sonarfiles.app.core.common.DispatchersProvider
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

data class PortProbeResult(
    val port: Int,
    val isOpen: Boolean,
    val latencyMs: Long
)

@Singleton
class PortProber @Inject constructor(
    private val dispatchers: DispatchersProvider
) {

    suspend fun probePort(
        ip: String,
        port: Int,
        timeoutMs: Int = 380
    ): PortProbeResult = withContext(dispatchers.io) {
        val startNs = System.nanoTime()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            PortProbeResult(port = port, isOpen = true, latencyMs = elapsedMs.coerceAtLeast(1))
        } catch (e: IOException) {
            PortProbeResult(port = port, isOpen = false, latencyMs = -1)
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
            }
        }
    }
}
