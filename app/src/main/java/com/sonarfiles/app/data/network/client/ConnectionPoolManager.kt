package com.sonarfiles.app.data.network.client

import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import com.sonarfiles.app.domain.repository.RemoteFileSystem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionPoolManager @Inject constructor() {

    private val mutex = Mutex()
    private val activeSessions = ConcurrentHashMap<Long, RemoteFileSystem>()

    suspend fun getOrCreate(
        config: ServerConfig,
        credentials: AuthCredentials,
        factory: suspend (ServerConfig) -> RemoteFileSystem
    ): Result<RemoteFileSystem> = mutex.withLock {
        runCatching {
            val existing = activeSessions[config.id]
            if (existing != null && existing.isAlive()) {
                return@runCatching existing
            }

            existing?.close()
            val newConnection = factory(config)
            newConnection.connect(credentials).getOrThrow()
            activeSessions[config.id] = newConnection
            newConnection
        }
    }

    suspend fun closeSession(serverId: Long) = mutex.withLock {
        activeSessions.remove(serverId)?.close()
    }

    suspend fun closeAll() = mutex.withLock {
        activeSessions.values.forEach {
            try { it.close() } catch (_: Exception) {}
        }
        activeSessions.clear()
    }
}
