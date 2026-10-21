package com.sonarfiles.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransferType {
    DOWNLOAD,
    UPLOAD
}

@Serializable
enum class TransferStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class TransferTask(
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
    val progressFraction: Float
        get() = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isFinished: Boolean
        get() = status == TransferStatus.COMPLETED || status == TransferStatus.FAILED || status == TransferStatus.CANCELLED
}
