package com.sonarfiles.app.domain.usecase.connection

import androidx.fragment.app.FragmentActivity
import com.sonarfiles.app.core.security.BiometricAuthenticator
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.CredentialRepository
import javax.inject.Inject

class UnlockServerCredentialsUseCase @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val biometricAuthenticator: BiometricAuthenticator
) {

    suspend operator fun invoke(
        activity: FragmentActivity,
        serverConfig: ServerConfig
    ): Result<AuthCredentials?> {
        if (serverConfig.requiresBiometrics && biometricAuthenticator.canAuthenticate()) {
            val authResult = biometricAuthenticator.authenticate(
                activity = activity,
                title = "Acesso ao Servidor",
                subtitle = "Desbloqueie para conectar a '${serverConfig.displayName}'"
            )
            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull() ?: SecurityException("Autenticação biométrica falhou"))
            }
        }

        return credentialRepository.getCredentials(serverConfig.id)
    }
}
