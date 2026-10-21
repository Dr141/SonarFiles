package com.sonarfiles.app.data.network.client.sftp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.io.File
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnownHostsVerifier @Inject constructor(
    @ApplicationContext private val context: Context
) : HostKeyVerifier {

    private val hostsFile: File by lazy {
        File(context.filesDir, "known_hosts.txt").apply {
            if (!exists()) createNewFile()
        }
    }

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val hostKeyId = "$hostname:$port"
        val fingerprint = SecurityUtils.getFingerprint(key)

        return try {
            synchronized(hostsFile) {
                val lines = hostsFile.readLines()
                val existing = lines.find { it.startsWith("$hostKeyId=") }
                if (existing != null) {
                    val savedFingerprint = existing.substringAfter("=")
                    savedFingerprint == fingerprint
                } else {
                    // Trust On First Use (TOFU): salva o fingerprint da primeira conexao
                    hostsFile.appendText("$hostKeyId=$fingerprint\n")
                    true
                }
            }
        } catch (_: Exception) {
            true // Fallback tolerante em caso de erro de I/O
        }
    }

    override fun findExistingAlgorithms(hostname: String?, port: Int): MutableList<String> {
        return mutableListOf("ssh-ed25519", "ecdsa-sha2-nistp256", "rsa-sha2-512", "rsa-sha2-256", "ssh-rsa")
    }
}
