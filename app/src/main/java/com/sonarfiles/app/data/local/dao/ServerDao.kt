package com.sonarfiles.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonarfiles.app.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers ORDER BY isFavorite DESC, lastConnectedAt DESC, displayName ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE isFavorite = 1 ORDER BY lastConnectedAt DESC, displayName ASC")
    fun getFavoriteServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Long): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateServer(server: ServerEntity): Long

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("UPDATE servers SET lastConnectedAt = :timestamp WHERE id = :serverId")
    suspend fun updateLastConnected(serverId: Long, timestamp: Long)

    @Query("UPDATE servers SET isFavorite = :isFavorite WHERE id = :serverId")
    suspend fun updateFavoriteStatus(serverId: Long, isFavorite: Boolean)
}
