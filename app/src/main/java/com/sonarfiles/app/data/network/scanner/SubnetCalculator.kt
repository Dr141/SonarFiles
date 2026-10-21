package com.sonarfiles.app.data.network.scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

data class SubnetInfo(
    val localIp: String,
    val prefixLength: Int,
    val targetIps: List<String>,
    val broadcastIp: String
)

@Singleton
class SubnetCalculator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getLocalSubnet(): SubnetInfo? {
        val localInet = findLocalIpAddress() ?: return null
        val localIpStr = localInet.hostAddress ?: return null

        val parts = localIpStr.split(".")
        if (parts.size != 4) return null

        val baseSubnet = "${parts[0]}.${parts[1]}.${parts[2]}"
        val broadcastIp = "$baseSubnet.255"
        val currentLastOctet = parts[3].toIntOrNull() ?: -1

        // Gera a lista de IPs /24 (1 a 254), priorizando os hosts próximos ao IP local
        val ips = (1..254).map { octet ->
            "$baseSubnet.$octet"
        }.filter { it != localIpStr } // Não testa o próprio aparelho

        return SubnetInfo(
            localIp = localIpStr,
            prefixLength = 24,
            targetIps = ips,
            broadcastIp = broadcastIp
        )
    }

    private fun findLocalIpAddress(): InetAddress? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val linkProps: LinkProperties? = cm.getLinkProperties(activeNetwork)
                    linkProps?.linkAddresses?.forEach { linkAddress ->
                        val address = linkAddress.address
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            return address
                        }
                    }
                }
            }

            // Fallback via NetworkInterface
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address
                    }
                }
            }
        } catch (_: Exception) {
            // Ignora e retorna nulo
        }
        return null
    }
}
