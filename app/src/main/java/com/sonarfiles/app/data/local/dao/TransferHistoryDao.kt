package com.sonarfiles.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonarfiles.app.data.local.entity.TransferHistoryEntity
import com.sonarfiles.app.domain.model.TransferStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferHistoryDao {

    @Query("SELECT * FROM transfers ORDER BY createdAt DESC")
    fun getAllTransfers(): Flow<List<TransferHistoryEntity>>

    @Query("SELECT * FROM transfers WHERE status IN ('QUEUED', 'RUNNING') ORDER BY createdAt DESC")
    fun getActiveTransfers(): Flow<List<TransferHistoryEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id LIMIT 1")
    suspend fun getTransferById(id: Long): TransferHistoryEntity?

    @Query("SELECT * FROM transfers WHERE workRequestId = :workRequestId LIMIT 1")
    suspend fun getTransferByWorkRequestId(workRequestId: String): TransferHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(entity: TransferHistoryEntity): Long

    @Update
    suspend fun updateTransfer(entity: TransferHistoryEntity)

    @Query("UPDATE transfers SET transferredBytes = :transferred, speedBytesPerSec = :speed, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, transferred: Long, speed: Long, status: TransferStatus)

    @Query("UPDATE transfers SET status = :status, errorMessage = :error, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, status: TransferStatus, error: String?, completedAt: Long?)

    @Query("DELETE FROM transfers WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun clearCompletedTransfers()

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransfer(id: Long)
}
