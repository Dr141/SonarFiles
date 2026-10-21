package com.sonarfiles.app.domain.repository

import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

data class TransferProgress(
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
) {
    val fraction: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

interface RemoteFileSystem : AutoCloseable {

    val serverConfig: ServerConfig

    suspend fun connect(credentials: AuthCredentials): Result<Unit>

    suspend fun isAlive(): Boolean

    suspend fun listDirectory(remotePath: String): Result<List<RemoteFile>>

    suspend fun stat(remotePath: String): Result<RemoteFile>

    suspend fun createDirectory(remotePath: String, recursive: Boolean = false): Result<Unit>

    suspend fun rename(sourcePath: String, destinationPath: String): Result<Unit>

    suspend fun delete(remotePath: String, recursive: Boolean = true): Result<Unit>

    suspend fun openReadStream(remotePath: String, offsetBytes: Long = 0L): Result<InputStream>

    fun downloadFile(
        remotePath: String,
        localOutputStreamProvider: suspend () -> OutputStream,
        totalBytes: Long
    ): Flow<TransferProgress>

    fun uploadFile(
        localInputStreamProvider: suspend () -> InputStream,
        remoteDestinationPath: String,
        totalBytes: Long
    ): Flow<TransferProgress>

    override fun close()
}

interface RemoteConnectionFactory {
    suspend fun getOrCreateConnection(
        config: ServerConfig,
        credentials: AuthCredentials
    ): Result<RemoteFileSystem>

    suspend fun createEphemeralConnection(
        config: ServerConfig
    ): RemoteFileSystem
}
