package com.sonarfiles.app.data.repository

import com.sonarfiles.app.core.common.DispatchersProvider
import com.sonarfiles.app.data.local.dao.TransferHistoryDao
import com.sonarfiles.app.data.local.entity.TransferHistoryEntity
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val transferDao: TransferHistoryDao,
    private val dispatchers: DispatchersProvider
) : TransferRepository {

    override fun getAllTransfers(): Flow<List<TransferTask>> =
        transferDao.getAllTransfers()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun getActiveTransfers(): Flow<List<TransferTask>> =
        transferDao.getActiveTransfers()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override suspend fun getTransferById(id: Long): TransferTask? = withContext(dispatchers.io) {
        transferDao.getTransferById(id)?.toDomain()
    }

    override suspend fun insertTransfer(task: TransferTask): Long = withContext(dispatchers.io) {
        transferDao.insertTransfer(TransferHistoryEntity.fromDomain(task))
    }

    override suspend fun updateTransfer(task: TransferTask) = withContext(dispatchers.io) {
        transferDao.updateTransfer(TransferHistoryEntity.fromDomain(task))
    }

    override suspend fun updateProgress(
        id: Long,
        transferred: Long,
        speed: Long,
        status: TransferStatus
    ) = withContext(dispatchers.io) {
        transferDao.updateProgress(id, transferred, speed, status)
    }

    override suspend fun markCompleted(id: Long, status: TransferStatus, error: String?) = withContext(dispatchers.io) {
        transferDao.markCompleted(id, status, error, System.currentTimeMillis())
    }

    override suspend fun clearCompletedTransfers() = withContext(dispatchers.io) {
        transferDao.clearCompletedTransfers()
    }

    override suspend fun deleteTransfer(id: Long) = withContext(dispatchers.io) {
        transferDao.deleteTransfer(id)
    }
}
