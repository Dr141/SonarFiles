package com.sonarfiles.app.data.repository

import com.sonarfiles.app.core.security.CredentialVaultDataSource
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.repository.CredentialRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialRepositoryImpl @Inject constructor(
    private val vaultDataSource: CredentialVaultDataSource
) : CredentialRepository {

    override suspend fun saveCredentials(serverId: Long, credentials: AuthCredentials): Result<Unit> {
        return vaultDataSource.save(serverId, credentials)
    }

    override suspend fun getCredentials(serverId: Long): Result<AuthCredentials?> {
        return vaultDataSource.get(serverId)
    }

    override suspend fun deleteCredentials(serverId: Long): Result<Unit> {
        return vaultDataSource.delete(serverId)
    }
}
