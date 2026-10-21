package com.sonarfiles.app.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BiometricAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Desbloqueio de Credenciais",
        subtitle: String = "Confirme sua identidade para acessar o servidor"
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(SecurityException(errString.toString())))
                    }
                }

                override fun onAuthenticationFailed() {
                    // Usuário tentou biometria mas o sensor não reconheceu; aguarda próxima tentativa ou cancelamento
                }
            }
        )

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }

        biometricPrompt.authenticate(promptInfo)
    }
}
