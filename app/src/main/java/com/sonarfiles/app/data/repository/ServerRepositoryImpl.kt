package com.sonarfiles.app.data.repository

import com.sonarfiles.app.core.common.DispatchersProvider
import com.sonarfiles.app.data.local.dao.ServerDao
import com.sonarfiles.app.data.local.entity.ServerEntity
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val dispatchers: DispatchersProvider
) : ServerRepository {

    override fun getAllServers(): Flow<List<ServerConfig>> =
        serverDao.getAllServers()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun getFavoriteServers(): Flow<List<ServerConfig>> =
        serverDao.getFavoriteServers()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override suspend fun getServerById(id: Long): ServerConfig? =
        withContext(dispatchers.io) {
            serverDao.getServerById(id)?.toDomain()
        }

    override suspend fun insertOrUpdateServer(server: ServerConfig): Long =
        withContext(dispatchers.io) {
            serverDao.insertOrUpdateServer(ServerEntity.fromDomain(server))
        }

    override suspend fun deleteServer(server: ServerConfig) =
        withContext(dispatchers.io) {
            serverDao.deleteServer(ServerEntity.fromDomain(server))
        }

    override suspend fun updateLastConnected(serverId: Long, timestamp: Long) =
        withContext(dispatchers.io) {
            serverDao.updateLastConnected(serverId, timestamp)
        }

    override suspend fun toggleFavorite(serverId: Long, isFavorite: Boolean) =
        withContext(dispatchers.io) {
            serverDao.updateFavoriteStatus(serverId, isFavorite)
        }
}
