package com.sonarfiles.app.core.security

import android.content.Context
import com.sonarfiles.app.core.common.DispatchersProvider
import com.sonarfiles.app.domain.model.AuthCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SerializedCredential(
    val type: String, // "PASSWORD" | "SSH_KEY" | "ANONYMOUS"
    val username: String? = null,
    val secret: String? = null,
    val extra: String? = null
)

@Serializable
private data class VaultStore(
    val credentials: Map<String, SerializedCredential> = emptyMap()
)

@Singleton
class CredentialVaultDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    private val dispatchers: DispatchersProvider
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val vaultFile: File by lazy {
        File(context.filesDir, "vault_credentials.enc")
    }

    suspend fun save(serverId: Long, credentials: AuthCredentials): Result<Unit> = withContext(dispatchers.io) {
        mutex.withLock {
            runCatching {
                val currentStore = readStoreInternal()
                val serialized = when (credentials) {
                    is AuthCredentials.Anonymous -> SerializedCredential(type = "ANONYMOUS")
                    is AuthCredentials.Password -> SerializedCredential(
                        type = "PASSWORD",
                        username = credentials.username,
                        secret = String(credentials.password),
                        extra = credentials.domain
                    )
                    is AuthCredentials.SshKey -> SerializedCredential(
                        type = "SSH_KEY",
                        username = credentials.username,
                        secret = String(credentials.privateKeyPem),
                        extra = credentials.passphrase?.let { String(it) }
                    )
                }

                val updatedMap = currentStore.credentials.toMutableMap()
                updatedMap[serverId.toString()] = serialized
                writeStoreInternal(VaultStore(updatedMap))
            }
        }
    }

    suspend fun get(serverId: Long): Result<AuthCredentials?> = withContext(dispatchers.io) {
        mutex.withLock {
            runCatching {
                val currentStore = readStoreInternal()
                val item = currentStore.credentials[serverId.toString()] ?: return@runCatching null

                when (item.type) {
                    "ANONYMOUS" -> AuthCredentials.Anonymous
                    "PASSWORD" -> AuthCredentials.Password(
                        username = item.username ?: "",
                        password = (item.secret ?: "").toCharArray(),
                        domain = item.extra
                    )
                    "SSH_KEY" -> AuthCredentials.SshKey(
                        username = item.username ?: "",
                        privateKeyPem = (item.secret ?: "").toCharArray(),
                        passphrase = item.extra?.toCharArray()
                    )
                    else -> null
                }
            }
        }
    }

    suspend fun delete(serverId: Long): Result<Unit> = withContext(dispatchers.io) {
        mutex.withLock {
            runCatching {
                val currentStore = readStoreInternal()
                val updatedMap = currentStore.credentials.toMutableMap()
                updatedMap.remove(serverId.toString())
                writeStoreInternal(VaultStore(updatedMap))
            }
        }
    }

    private fun readStoreInternal(): VaultStore {
        if (!vaultFile.exists() || vaultFile.length() == 0L) {
            return VaultStore()
        }
        return try {
            val encryptedBytes = vaultFile.readBytes()
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            json.decodeFromString<VaultStore>(jsonString)
        } catch (_: Exception) {
            VaultStore()
        }
    }

    private fun writeStoreInternal(store: VaultStore) {
        val jsonString = json.encodeToString(VaultStore.serializer(), store)
        val encryptedBytes = cryptoManager.encrypt(jsonString.toByteArray(Charsets.UTF_8))
        vaultFile.writeBytes(encryptedBytes)
    }
}
