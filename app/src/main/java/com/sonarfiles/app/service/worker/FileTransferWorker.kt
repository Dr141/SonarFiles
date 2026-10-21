package com.sonarfiles.app.service.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sonarfiles.app.core.ui.components.formatFileSize
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferType
import com.sonarfiles.app.domain.repository.CredentialRepository
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import com.sonarfiles.app.domain.repository.ServerRepository
import com.sonarfiles.app.domain.repository.TransferRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@HiltWorker
class FileTransferWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val serverRepository: ServerRepository,
    private val credentialRepository: CredentialRepository,
    private val connectionFactory: RemoteConnectionFactory,
    private val transferRepository: TransferRepository
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val transferId = inputData.getLong(KEY_TRANSFER_ID, -1L)
        val serverId = inputData.getLong(KEY_SERVER_ID, -1L)
        val remotePath = inputData.getString(KEY_REMOTE_PATH) ?: return Result.failure()
        val localPath = inputData.getString(KEY_LOCAL_PATH) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "arquivo"
        val transferTypeStr = inputData.getString(KEY_TYPE) ?: TransferType.DOWNLOAD.name
        val totalBytes = inputData.getLong(KEY_TOTAL_BYTES, 0L)
        val transferType = TransferType.valueOf(transferTypeStr)

        createNotificationChannel()
        setForeground(createForegroundInfo(fileName, 0, 0L, totalBytes))

        try {
            transferRepository.updateProgress(transferId, 0L, 0L, TransferStatus.RUNNING)

            val server = serverRepository.getServerById(serverId)
                ?: throw IllegalStateException("Servidor não encontrado")
            val credentials = credentialRepository.getCredentials(serverId).getOrNull()
                ?: AuthCredentials.Anonymous

            val connection = connectionFactory.getOrCreateConnection(server, credentials).getOrThrow()

            var lastNotificationTime = System.currentTimeMillis()

            if (transferType == TransferType.DOWNLOAD) {
                val localFile = File(localPath)
                localFile.parentFile?.mkdirs()

                connection.downloadFile(
                    remotePath = remotePath,
                    localOutputStreamProvider = { FileOutputStream(localFile) },
                    totalBytes = totalBytes
                ).collectLatest { progress ->
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationTime >= 500) {
                        lastNotificationTime = now
                        val percent = (progress.fraction * 100).toInt()
                        setForeground(createForegroundInfo(fileName, percent, progress.speedBytesPerSec, totalBytes))
                        transferRepository.updateProgress(
                            transferId,
                            progress.bytesTransferred,
                            progress.speedBytesPerSec,
                            TransferStatus.RUNNING
                        )
                    }
                }
            } else {
                val localFile = File(localPath)
                if (!localFile.exists()) throw IllegalArgumentException("Arquivo local não encontrado")

                connection.uploadFile(
                    localInputStreamProvider = { FileInputStream(localFile) },
                    remoteDestinationPath = remotePath,
                    totalBytes = totalBytes
                ).collectLatest { progress ->
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationTime >= 500) {
                        lastNotificationTime = now
                        val percent = (progress.fraction * 100).toInt()
                        setForeground(createForegroundInfo(fileName, percent, progress.speedBytesPerSec, totalBytes))
                        transferRepository.updateProgress(
                            transferId,
                            progress.bytesTransferred,
                            progress.speedBytesPerSec,
                            TransferStatus.RUNNING
                        )
                    }
                }
            }

            transferRepository.markCompleted(transferId, TransferStatus.COMPLETED)
            showCompletionNotification(fileName, isSuccess = true)
            return Result.success()
        } catch (e: Exception) {
            transferRepository.markCompleted(transferId, TransferStatus.FAILED, e.localizedMessage)
            showCompletionNotification(fileName, isSuccess = false, errorMessage = e.localizedMessage)
            return Result.failure(workDataOf("error" to (e.localizedMessage ?: "Falha na transferência")))
        }
    }

    private fun createForegroundInfo(
        fileName: String,
        progressPercent: Int,
        speedBytesPerSec: Long,
        totalBytes: Long
    ): ForegroundInfo {
        val speedStr = if (speedBytesPerSec > 0) "${formatFileSize(speedBytesPerSec)}/s" else "Calculando..."
        val cancelIntent = androidx.work.WorkManager.getInstance(context)
            .createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Transferindo: $fileName")
            .setContentText("$progressPercent% • $speedStr")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(fileName: String, isSuccess: Boolean, errorMessage: String? = null) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(if (isSuccess) "Transferência Concluída" else "Falha na Transferência")
            .setContentText(if (isSuccess) fileName else "$fileName: $errorMessage")
            .setSmallIcon(if (isSuccess) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(fileName.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transferências de Arquivos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progresso de uploads e downloads em segundo plano"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sonar_file_transfers"
        const val NOTIFICATION_ID = 2001

        const val KEY_TRANSFER_ID = "transfer_id"
        const val KEY_SERVER_ID = "server_id"
        const val KEY_REMOTE_PATH = "remote_path"
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_TYPE = "type"
        const val KEY_TOTAL_BYTES = "total_bytes"
    }
}
