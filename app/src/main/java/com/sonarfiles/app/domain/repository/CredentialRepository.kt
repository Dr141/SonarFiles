package com.sonarfiles.app.domain.repository

import com.sonarfiles.app.domain.model.AuthCredentials

interface CredentialRepository {
    suspend fun saveCredentials(serverId: Long, credentials: AuthCredentials): Result<Unit>
    suspend fun getCredentials(serverId: Long): Result<AuthCredentials?>
    suspend fun deleteCredentials(serverId: Long): Result<Unit>
}
