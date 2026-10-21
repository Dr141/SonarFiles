package com.sonarfiles.app.domain.usecase.transfer

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.model.TransferType
import com.sonarfiles.app.domain.repository.TransferRepository
import com.sonarfiles.app.service.worker.FileTransferWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EnqueueTransferUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferRepository: TransferRepository
) {

    suspend operator fun invoke(
        serverId: Long,
        serverDisplayName: String,
        remotePath: String,
        localPath: String,
        fileName: String,
        type: TransferType,
        totalBytes: Long
    ): Long {
        val initialTask = TransferTask(
            serverId = serverId,
            serverDisplayName = serverDisplayName,
            remotePath = remotePath,
            localUriOrPath = localPath,
            fileName = fileName,
            type = type,
            totalBytes = totalBytes,
            status = TransferStatus.QUEUED
        )

        val transferId = transferRepository.insertTransfer(initialTask)

        val inputData = Data.Builder()
            .putLong(FileTransferWorker.KEY_TRANSFER_ID, transferId)
            .putLong(FileTransferWorker.KEY_SERVER_ID, serverId)
            .putString(FileTransferWorker.KEY_REMOTE_PATH, remotePath)
            .putString(FileTransferWorker.KEY_LOCAL_PATH, localPath)
            .putString(FileTransferWorker.KEY_FILE_NAME, fileName)
            .putString(FileTransferWorker.KEY_TYPE, type.name)
            .putLong(FileTransferWorker.KEY_TOTAL_BYTES, totalBytes)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<FileTransferWorker>()
            .setInputData(inputData)
            .addTag("transfer_$transferId")
            .build()

        transferRepository.updateTransfer(initialTask.copy(id = transferId, workRequestId = workRequest.id.toString()))
        WorkManager.getInstance(context).enqueue(workRequest)

        return transferId
    }
}
