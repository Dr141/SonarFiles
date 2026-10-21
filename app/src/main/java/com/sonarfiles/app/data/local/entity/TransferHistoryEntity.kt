package com.sonarfiles.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.model.TransferType

@Entity(tableName = "transfers")
data class TransferHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val workRequestId: String? = null,
    val serverId: Long,
    val serverDisplayName: String,
    val remotePath: String,
    val localUriOrPath: String,
    val fileName: String,
    val type: TransferType,
    val totalBytes: Long,
    val transferredBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: TransferStatus = TransferStatus.QUEUED,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    fun toDomain(): TransferTask = TransferTask(
        id = id,
        workRequestId = workRequestId,
        serverId = serverId,
        serverDisplayName = serverDisplayName,
        remotePath = remotePath,
        localUriOrPath = localUriOrPath,
        fileName = fileName,
        type = type,
        totalBytes = totalBytes,
        transferredBytes = transferredBytes,
        speedBytesPerSec = speedBytesPerSec,
        status = status,
        errorMessage = errorMessage,
        createdAt = createdAt,
        completedAt = completedAt
    )

    companion object {
        fun fromDomain(domain: TransferTask): TransferHistoryEntity = TransferHistoryEntity(
            id = domain.id,
            workRequestId = domain.workRequestId,
            serverId = domain.serverId,
            serverDisplayName = domain.serverDisplayName,
            remotePath = domain.remotePath,
            localUriOrPath = domain.localUriOrPath,
            fileName = domain.fileName,
            type = domain.type,
            totalBytes = domain.totalBytes,
            transferredBytes = domain.transferredBytes,
            speedBytesPerSec = domain.speedBytesPerSec,
            status = domain.status,
            errorMessage = domain.errorMessage,
            createdAt = domain.createdAt,
            completedAt = domain.completedAt
        )
    }
}
