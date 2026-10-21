package com.sonarfiles.app.service.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.sonarfiles.app.data.local.SonarDatabase
import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Android Storage Access Framework (SAF) DocumentsProvider.
 * Expõe servidores salvos no SonarFiles como fontes de arquivos nativas
 * acessíveis por outros aplicativos do sistema.
 */
class SonarDocumentsProvider : DocumentsProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        fun database(): SonarDatabase
        fun connectionFactory(): RemoteConnectionFactory
    }

    private val entryPoint: ProviderEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            ProviderEntryPoint::class.java
        )
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

        val servers = runBlocking {
            entryPoint.database().serverDao().getAllServers().first()
        }

        for (server in servers) {
            val row = result.newRow()
            row.add(DocumentsContract.Root.COLUMN_ROOT_ID, server.id.toString())
            row.add(DocumentsContract.Root.COLUMN_ICON, android.R.drawable.stat_sys_data_bluetooth)
            row.add(DocumentsContract.Root.COLUMN_TITLE, server.displayName)
            row.add(DocumentsContract.Root.COLUMN_SUMMARY, "${server.protocol.name} • ${server.hostOrIp}")
            row.add(
                DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD or DocumentsContract.Root.FLAG_LOCAL_ONLY
            )
            row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, encodeDocumentId(server.id, "/"))
        }

        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val (serverId, remotePath) = decodeDocumentId(documentId)

        val row = result.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, remotePath.substringAfterLast('/').ifBlank { "Raiz" })
        row.add(DocumentsContract.Document.COLUMN_SIZE, 0L)
        row.add(
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            if (remotePath == "/" || remotePath.isBlank()) DocumentsContract.Document.MIME_TYPE_DIR else "application/octet-stream"
        )
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())

        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val (serverId, remotePath) = decodeDocumentId(parentDocumentId)

        val server = runBlocking {
            entryPoint.database().serverDao().getServerById(serverId)?.toDomain()
        } ?: return result

        val connection = runBlocking {
            entryPoint.connectionFactory().createEphemeralConnection(server)
        }

        try {
            val list = runBlocking {
                connection.listDirectory(remotePath).getOrDefault(emptyList())
            }

            for (file in list) {
                val row = result.newRow()
                val docId = encodeDocumentId(serverId, file.fullPath)
                row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId)
                row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
                row.add(DocumentsContract.Document.COLUMN_SIZE, file.sizeBytes)
                row.add(
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else (file.mimeType ?: "application/octet-stream")
                )
                row.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
                row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModifiedEpochMillis)
            }
        } finally {
            connection.close()
        }

        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        throw UnsupportedOperationException("Abertura direta via SAF planejada para próxima versão.")
    }

    private fun encodeDocumentId(serverId: Long, remotePath: String): String = "$serverId:$remotePath"

    private fun decodeDocumentId(documentId: String): Pair<Long, String> {
        val parts = documentId.split(":", limit = 2)
        val serverId = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val path = parts.getOrNull(1) ?: "/"
        return serverId to path
    }

    companion object {
        const val AUTHORITY = "com.sonarfiles.app.documents"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
    }
}
