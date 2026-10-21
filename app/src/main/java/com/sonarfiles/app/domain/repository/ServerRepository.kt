package com.sonarfiles.app.domain.repository

import com.sonarfiles.app.domain.model.ServerConfig
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getAllServers(): Flow<List<ServerConfig>>
    fun getFavoriteServers(): Flow<List<ServerConfig>>
    suspend fun getServerById(id: Long): ServerConfig?
    suspend fun insertOrUpdateServer(server: ServerConfig): Long
    suspend fun deleteServer(server: ServerConfig)
    suspend fun updateLastConnected(serverId: Long, timestamp: Long)
    suspend fun toggleFavorite(serverId: Long, isFavorite: Boolean)
}
