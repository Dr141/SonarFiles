package com.sonarfiles.app.data.network.client

import com.sonarfiles.app.core.common.DispatchersProvider
import com.sonarfiles.app.data.network.client.sftp.KnownHostsVerifier
import com.sonarfiles.app.data.network.client.sftp.SftpRemoteFileSystem
import com.sonarfiles.app.data.network.client.smb.SmbConfigProvider
import com.sonarfiles.app.data.network.client.smb.SmbRemoteFileSystem
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import com.sonarfiles.app.domain.repository.RemoteFileSystem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConnectionFactoryImpl @Inject constructor(
    private val smbConfigProvider: SmbConfigProvider,
    private val knownHostsVerifier: KnownHostsVerifier,
    private val connectionPoolManager: ConnectionPoolManager,
    private val dispatchers: DispatchersProvider
) : RemoteConnectionFactory {

    override suspend fun getOrCreateConnection(
        config: ServerConfig,
        credentials: AuthCredentials
    ): Result<RemoteFileSystem> {
        return connectionPoolManager.getOrCreate(config, credentials) { cfg ->
            createEphemeralConnection(cfg)
        }
    }

    override suspend fun createEphemeralConnection(config: ServerConfig): RemoteFileSystem {
        return when (config.protocol) {
            ProtocolType.SMB -> SmbRemoteFileSystem(
                serverConfig = config,
                smbConfigProvider = smbConfigProvider,
                dispatchers = dispatchers
            )
            ProtocolType.SFTP -> SftpRemoteFileSystem(
                serverConfig = config,
                knownHostsVerifier = knownHostsVerifier,
                dispatchers = dispatchers
            )
        }
    }
}
