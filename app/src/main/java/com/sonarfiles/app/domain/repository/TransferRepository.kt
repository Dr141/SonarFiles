package com.sonarfiles.app.domain.repository

import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferTask
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getAllTransfers(): Flow<List<TransferTask>>
    fun getActiveTransfers(): Flow<List<TransferTask>>
    suspend fun getTransferById(id: Long): TransferTask?
    suspend fun insertTransfer(task: TransferTask): Long
    suspend fun updateTransfer(task: TransferTask)
    suspend fun updateProgress(id: Long, transferred: Long, speed: Long, status: TransferStatus)
    suspend fun markCompleted(id: Long, status: TransferStatus, error: String? = null)
    suspend fun clearCompletedTransfers()
    suspend fun deleteTransfer(id: Long)
}
