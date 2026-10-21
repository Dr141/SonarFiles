package com.sonarfiles.app.service.streaming

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.sonarfiles.app.domain.repository.RemoteFileSystem
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * DataSource customizado para Android Media3 (ExoPlayer).
 * Permite reproduzir vídeos e áudios de servidores SMB ou SFTP
 * via streaming sob demanda com suporte a seek sem download prévio.
 */
class RemoteMediaDataSource(
    private val remoteFileSystem: RemoteFileSystem,
    private val remotePath: String
) : BaseDataSource(/* isNetwork = */ true) {

    private var currentInputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        try {
            // Abre o InputStream posicionado exatamente no offset de seek do player
            val streamResult = kotlinx.coroutines.runBlocking {
                remoteFileSystem.openReadStream(remotePath, dataSpec.position)
            }

            currentInputStream = streamResult.getOrThrow()
            opened = true
            transferStarted(dataSpec)

            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                val statResult = kotlinx.coroutines.runBlocking {
                    remoteFileSystem.stat(remotePath)
                }
                val totalSize = statResult.getOrNull()?.sizeBytes ?: C.LENGTH_UNSET.toLong()
                if (totalSize != C.LENGTH_UNSET.toLong()) totalSize - dataSpec.position else C.LENGTH_UNSET.toLong()
            }

            return bytesRemaining
        } catch (e: Exception) {
            throw IOException("Erro ao abrir stream de mídia remota: ${e.localizedMessage}", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val stream = currentInputStream ?: throw IOException("Stream não aberto")
        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(bytesRemaining, length.toLong()).toInt()
        }

        val bytesRead = stream.read(buffer, offset, bytesToRead)
        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                throw EOFException()
            }
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }

        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        if (opened) {
            opened = false
            try {
                currentInputStream?.close()
            } catch (_: Exception) {}
            currentInputStream = null
            transferEnded()
        }
    }
}
