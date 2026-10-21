package com.sonarfiles.app.data.network.client.sftp

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
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.userauth.method.AuthNone
import net.schmizz.sshj.userauth.password.PasswordUtils
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean

class SftpRemoteFileSystem(
    override val serverConfig: ServerConfig,
    private val knownHostsVerifier: KnownHostsVerifier,
    private val dispatchers: DispatchersProvider
) : RemoteFileSystem {

    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null

    override suspend fun connect(credentials: AuthCredentials): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val client = SSHClient()
            client.addHostKeyVerifier(knownHostsVerifier)
            client.connectTimeout = 8_000
            client.timeout = 30_000

            client.connect(serverConfig.hostOrIp, serverConfig.port)
            sshClient = client

            when (credentials) {
                is AuthCredentials.Password -> {
                    client.authPassword(credentials.username, String(credentials.password))
                }
                is AuthCredentials.SshKey -> {
                    val passwordFinder = PasswordUtils.createOneOff(credentials.passphrase)
                    val keyProvider = client.loadKeys(String(credentials.privateKeyPem), null, passwordFinder)
                    client.authPublickey(credentials.username, keyProvider)
                }
                is AuthCredentials.Anonymous -> {
                    client.auth(serverConfig.domainOrWorkgroup ?: "anonymous", AuthNone())
                }
            }

            sftpClient = client.newSFTPClient()
        }
    }

    override suspend fun isAlive(): Boolean = withContext(dispatchers.io) {
        sshClient?.isConnected == true && sshClient?.isAuthenticated == true
    }

    override suspend fun listDirectory(remotePath: String): Result<List<RemoteFile>> = withContext(dispatchers.io) {
        runCatching {
            val sftp = sftpClient ?: throw IllegalStateException("SFTP não conectado")
            val targetPath = remotePath.ifBlank { "/" }
            val list: List<RemoteResourceInfo> = sftp.ls(targetPath)

            list.filter { it.name != "." && it.name != ".." }
                .map { res ->
                    val isDir = res.isDirectory
                    val isHidden = res.name.startsWith(".")
                    RemoteFile(
                        name = res.name,
                        fullPath = res.path,
                        isDirectory = isDir,
                        sizeBytes = if (isDir) 0L else res.attributes.size,
                        lastModifiedEpochMillis = res.attributes.mtime * 1000L,
                        isHidden = isHidden,
                        permissionsString = res.attributes.mode.toString()
                    )
                }
        }
    }

    override suspend fun stat(remotePath: String): Result<RemoteFile> = withContext(dispatchers.io) {
        runCatching {
            val sftp = sftpClient ?: throw IllegalStateException("SFTP não conectado")
            val attrs = sftp.stat(remotePath)
            val isDir = attrs.mode.type == FileMode.Type.DIRECTORY

            RemoteFile(
                name = remotePath.substringAfterLast('/'),
                fullPath = remotePath,
                isDirectory = isDir,
                sizeBytes = if (isDir) 0L else attrs.size,
                lastModifiedEpochMillis = attrs.mtime * 1000L,
                permissionsString = attrs.mode.toString()
            )
        }
    }

    override suspend fun createDirectory(remotePath: String, recursive: Boolean): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val sftp = sftpClient ?: throw IllegalStateException("SFTP não conectado")
            if (recursive) {
                sftp.mkdirs(remotePath)
            } else {
                sftp.mkdir(remotePath)
            }
        }
    }

    override suspend fun rename(sourcePath: String, destinationPath: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val sftp = sftpClient ?: throw IllegalStateException("SFTP não conectado")
            sftp.rename(sourcePath, destinationPath)
        }
    }

    override suspend fun delete(remotePath: String, recursive: Boolean): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val sftp = sftpClient ?: throw IllegalStateException("SFTP não conectado")
            val isDir = try {
                sftp.stat(remotePath).mode.type == FileMode.Type.DIRECTORY
            } catch (_: Exception) {
                false
            }

            if (isDir) {
                if (recursive) {
                    deleteRecursively(sftp, remotePath)
                } else {
                    sftp.rmdir(remotePath)
                }
            } else {
                sftp.rm(remotePath)
            }
        }
    }

    private fun deleteRecursively(sftp: SFTPClient, path: String) {
        val children = sftp.ls(path)
        for (child in children) {
            if (child.name == "." || child.name == "..") continue
            if (child.isDirectory) {
                deleteRecursively(sftp, child.path)
            } else {
                sftp.rm(child.path)
            }
        }
        sftp.rmdir(path)
    }

    override suspend fun openReadStream(remotePath: String, offsetBytes: Long): Result<InputStream> = withContext(dispatchers.io) {
        runCatching {
            val sftp = sftpClient ?: throw IllegalStateException("SFTP não conectado")
            val remoteFile = sftp.open(remotePath, EnumSet.of(OpenMode.READ))
            val rawStream = remoteFile.RemoteFileInputStream(offsetBytes)

            val closed = AtomicBoolean(false)
            object : FilterInputStream(rawStream) {
                override fun close() {
                    if (closed.compareAndSet(false, true)) {
                        try {
                            super.close()
                        } finally {
                            remoteFile.close()
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
    }.flowOn(dispatchers.io)

    override fun uploadFile(
        localInputStreamProvider: suspend () -> InputStream,
        remoteDestinationPath: String,
        totalBytes: Long
    ): Flow<TransferProgress> = flow {
        val sftp = sftpClient ?: throw IllegalStateException("SFTP não conectado")
        val remoteFile = sftp.open(
            remoteDestinationPath,
            EnumSet.of(OpenMode.CREAT, OpenMode.WRITE, OpenMode.TRUNC)
        )

        val inStream = localInputStreamProvider()
        remoteFile.use { file ->
            inStream.use { input ->
                val outStream = file.RemoteFileOutputStream()
                outStream.use { output ->
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
            sftpClient?.close()
        } catch (_: Exception) {}
        try {
            sshClient?.disconnect()
        } catch (_: Exception) {}
    }
}
