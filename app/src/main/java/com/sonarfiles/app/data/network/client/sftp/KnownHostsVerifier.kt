package com.sonarfiles.app.data.network.client.sftp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import java.io.File
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnownHostsVerifier @Inject constructor(
    @ApplicationContext private val context: Context
) : HostKeyVerifier {

    private val knownHostsFile: File by lazy {
        File(context.filesDir, "known_hosts").apply {
            if (!exists()) createNewFile()
        }
    }

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        return try {
            val openSSHKnownHosts = OpenSSHKnownHosts(knownHostsFile)
            if (openSSHKnownHosts.verify(hostname, port, key)) {
                true
            } else {
                // Trust On First Use (TOFU): se for o primeiro acesso a este host, salva a chave
                val entries = openSSHKnownHosts.entries()
                val isKnownHost = entries.any { entry ->
                    entry.appliesTo("$hostname:$port") || entry.appliesTo(hostname)
                }
                if (!isKnownHost) {
                    openSSHKnownHosts.add(OpenSSHKnownHosts.SimpleEntry(null, "$hostname:$port", OpenSSHKnownHosts.KeyType.fromKey(key), key), true)
                    true
                } else {
                    // Chave existente divergente (possível ataque MITM ou reinstalação do servidor)
                    false
                }
            }
        } catch (_: Exception) {
            true // Fallback tolerante em caso de erro de parse
        }
    }

    override fun findExistingAlgorithms(hostname: String?, port: Int): MutableList<String> {
        return mutableListOf("ssh-ed25519", "ecdsa-sha2-nistp256", "rsa-sha2-512", "rsa-sha2-256", "ssh-rsa")
    }
}
