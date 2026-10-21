package com.sonarfiles.app.data.network.client.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.sonarfiles.app.core.common.DispatchersProvider
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.RemoteFileSystem
import com.sonarfiles.app.domain.repository.TransferProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean

class SmbRemoteFileSystem(
    override val serverConfig: ServerConfig,
    private val smbConfigProvider: SmbConfigProvider,
    private val dispatchers: DispatchersProvider
) : RemoteFileSystem {

    private var smbClient: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var activeShare: DiskShare? = null
    private var currentShareName: String = ""

    override suspend fun connect(credentials: AuthCredentials): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val client = SMBClient(smbConfigProvider.createDefaultConfig())
            smbClient = client

            val conn = client.connect(serverConfig.hostOrIp, serverConfig.port)
            connection = conn

            val authContext = when (credentials) {
                is AuthCredentials.Password -> {
                    AuthenticationContext(
                        credentials.username,
                        credentials.password,
                        credentials.domain ?: serverConfig.domainOrWorkgroup ?: ""
                    )
                }
                is AuthCredentials.Anonymous -> AuthenticationContext.anonymous()
                is AuthCredentials.SshKey -> throw IllegalArgumentException("Chave SSH não é suportada no protocolo SMB")
            }

            val sess = conn.authenticate(authContext)
            session = sess

            // Conecta ao share inicial configurado (padrão ou extraído de defaultShareOrPath)
            val shareToConnect = resolveShareName(serverConfig.defaultShareOrPath)
            if (shareToConnect.isNotBlank()) {
                activeShare = sess.connectShare(shareToConnect) as? DiskShare
                currentShareName = shareToConnect
            }
        }
    }

    override suspend fun isAlive(): Boolean = withContext(dispatchers.io) {
        connection?.isConnected == true && activeShare?.isConnected == true
    }

    override suspend fun listDirectory(remotePath: String): Result<List<RemoteFile>> = withContext(dispatchers.io) {
        runCatching {
            val share = ensureShareConnected(remotePath)
            val relativePath = extractRelativePath(remotePath)

            val fileInfos: List<FileIdBothDirectoryInformation> = share.list(relativePath)
            fileInfos.filter { it.fileName != "." && it.fileName != ".." }
                .map { info ->
                    val isDir = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                    val isHidden = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_HIDDEN.value) != 0L
                    val childPath = if (relativePath.isBlank()) info.fileName else "$relativePath/${info.fileName}"

                    RemoteFile(
                        name = info.fileName,
                        fullPath = "/$currentShareName/$childPath".replace("//", "/"),
                        isDirectory = isDir,
                        sizeBytes = if (isDir) 0L else info.endOfFile,
                        lastModifiedEpochMillis = info.changeTime.toEpochMillis(),
                        isHidden = isHidden
                    )
                }
        }
    }

    override suspend fun stat(remotePath: String): Result<RemoteFile> = withContext(dispatchers.io) {
        runCatching {
            val share = ensureShareConnected(remotePath)
            val relativePath = extractRelativePath(remotePath)
            val info = share.getFileInformation(relativePath)
            val isDir = (info.standardInformation.isDirectory)

            RemoteFile(
                name = relativePath.substringAfterLast('/'),
                fullPath = remotePath,
                isDirectory = isDir,
                sizeBytes = if (isDir) 0L else info.standardInformation.endOfFile,
                lastModifiedEpochMillis = info.basicInformation.changeTime.toEpochMillis()
            )
        }
    }

    override suspend fun createDirectory(remotePath: String, recursive: Boolean): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val share = ensureShareConnected(remotePath)
            val relativePath = extractRelativePath(remotePath)
            share.mkdir(relativePath)
        }
    }

    override suspend fun rename(sourcePath: String, destinationPath: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val share = ensureShareConnected(sourcePath)
            val relSource = extractRelativePath(sourcePath)
            val relDest = extractRelativePath(destinationPath)

            val file = share.openFile(
                relSource,
                EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            file.use {
                it.rename(relDest)
            }
        }
    }

    override suspend fun delete(remotePath: String, recursive: Boolean): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val share = ensureShareConnected(remotePath)
            val relativePath = extractRelativePath(remotePath)
            val isDir = share.folderExists(relativePath)
            if (isDir) {
                if (recursive) {
                    share.rmdir(relativePath, true)
                } else {
                    share.rmdir(relativePath, false)
                }
            } else {
                share.rm(relativePath)
            }
        }
    }

    override suspend fun openReadStream(remotePath: String, offsetBytes: Long): Result<InputStream> = withContext(dispatchers.io) {
        runCatching {
            val share = ensureShareConnected(remotePath)
            val relativePath = extractRelativePath(remotePath)

            val smbFile = share.openFile(
                relativePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
            )

            val rawStream = smbFile.inputStream
            if (offsetBytes > 0) {
                rawStream.skip(offsetBytes)
            }

            // Stream auto-encerrante: ao fechar o stream, fecha também o handle do arquivo SMB
            val closed = AtomicBoolean(false)
            object : FilterInputStream(rawStream) {
                override fun close() {
                    if (closed.compareAndSet(false, true)) {
                        try {
                            super.close()
                        } finally {
                            smbFile.close()
                        }
                    }
                }
            }
        }
    }

    override fun downloadFile(
        remotePath: String,
        localOutputStreamProvider: suspend () -> OutputStream,
        totalBytes: Long
    ): Flow<TransferProgress> = flow {
        val stream = openReadStream(remotePath).getOrThrow()
        val outStream = localOutputStreamProvider()

        stream.use { input ->
            outStream.use { output ->
                val buffer = ByteArray(64 * 1024) // 64KB buffer
                var bytesRead: Int
                var totalTransferred = 0L
                var lastTimeNs = System.nanoTime()
                var bytesSinceLastTime = 0L
                var speedBps = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalTransferred += bytesRead
                    bytesSinceLastTime += bytesRead

                    val nowNs = System.nanoTime()
                    val diffNs = nowNs - lastTimeNs
                    if (diffNs >= 500_000_000L) { // Atualiza velocidade a cada 500ms
                        speedBps = (bytesSinceLastTime * 1_000_000_000L) / diffNs
                        bytesSinceLastTime = 0L
                        lastTimeNs = nowNs
                    }

                    emit(TransferProgress(totalTransferred, totalBytes, speedBps))
                }
            }
        }
    }.flowOn(dispatchers.io)

    override fun uploadFile(
        localInputStreamProvider: suspend () -> InputStream,
        remoteDestinationPath: String,
        totalBytes: Long
    ): Flow<TransferProgress> = flow {
        val share = ensureShareConnected(remoteDestinationPath)
        val relativePath = extractRelativePath(remoteDestinationPath)

        val smbFile = share.openFile(
            relativePath,
            EnumSet.of(AccessMask.GENERIC_WRITE, AccessMask.GENERIC_READ),
            EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OVERWRITE_IF,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        )

        val inStream = localInputStreamProvider()
        smbFile.use { remoteFile ->
            inStream.use { input ->
                val remoteOut = remoteFile.outputStream
                remoteOut.use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalTransferred = 0L
                    var lastTimeNs = System.nanoTime()
                    var bytesSinceLastTime = 0L
                    var speedBps = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalTransferred += bytesRead
                        bytesSinceLastTime += bytesRead

                        val nowNs = System.nanoTime()
                        val diffNs = nowNs - lastTimeNs
                        if (diffNs >= 500_000_000L) {
                            speedBps = (bytesSinceLastTime * 1_000_000_000L) / diffNs
                            bytesSinceLastTime = 0L
                            lastTimeNs = nowNs
                        }

                        emit(TransferProgress(totalTransferred, totalBytes, speedBps))
                    }
                }
            }
        }
    }.flowOn(dispatchers.io)

    override fun close() {
        try {
            activeShare?.close()
        } catch (_: Exception) {}
        try {
            session?.close()
        } catch (_: Exception) {}
        try {
            connection?.close()
        } catch (_: Exception) {}
        try {
            smbClient?.close()
        } catch (_: Exception) {}
    }

    private fun resolveShareName(path: String): String {
        val clean = path.trim().replace("\\", "/").trimStart('/')
        return clean.substringBefore('/')
    }

    private fun extractRelativePath(fullPath: String): String {
        val clean = fullPath.trim().replace("\\", "/").trimStart('/')
        val withoutShare = clean.substringAfter('/', "")
        return withoutShare.replace('/', '\\')
    }

    private fun ensureShareConnected(path: String): DiskShare {
        val targetShare = resolveShareName(path).ifBlank { currentShareName }
        if (activeShare == null || currentShareName != targetShare) {
            val sess = session ?: throw IllegalStateException("Sessão SMB não autenticada")
            activeShare?.close()
            activeShare = sess.connectShare(targetShare) as? DiskShare
                ?: throw IllegalArgumentException("Compartilhamento '$targetShare' não encontrado")
            currentShareName = targetShare
        }
        return activeShare!!
    }
}
