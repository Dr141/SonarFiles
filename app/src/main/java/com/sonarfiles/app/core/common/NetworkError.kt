package com.sonarfiles.app.core.common

sealed class NetworkError(
    val userFriendlyMessage: String,
    val technicalDetails: String? = null,
    val cause: Throwable? = null
) {
    class HostUnreachable(
        val host: String,
        details: String? = null,
        cause: Throwable? = null
    ) : NetworkError(
        userFriendlyMessage = "Dispositivo '$host' inacessível ou offline na rede local.",
        technicalDetails = details,
        cause = cause
    )

    class PortClosed(
        val host: String,
        val port: Int,
        cause: Throwable? = null
    ) : NetworkError(
        userFriendlyMessage = "Porta $port no host '$host' está fechada ou bloqueada por firewall.",
        technicalDetails = "TCP connection refused on port $port",
        cause = cause
    )

    class InvalidCredentials(
        val username: String? = null,
        details: String? = null,
        cause: Throwable? = null
    ) : NetworkError(
        userFriendlyMessage = "Credenciais de acesso incorretas. Verifique usuário e senha.",
        technicalDetails = details,
        cause = cause
    )

    class PermissionDenied(
        val path: String,
        details: String? = null,
        cause: Throwable? = null
    ) : NetworkError(
        userFriendlyMessage = "Acesso negado ao diretório ou arquivo: $path",
        technicalDetails = details,
        cause = cause
    )

    class ShareOrPathNotFound(
        val path: String,
        details: String? = null,
        cause: Throwable? = null
    ) : NetworkError(
        userFriendlyMessage = "O compartilhamento ou caminho '$path' não foi encontrado no servidor.",
        technicalDetails = details,
        cause = cause
    )

    class HostKeyChanged(
        val host: String,
        val newFingerprint: String
    ) : NetworkError(
        userFriendlyMessage = "Aviso de Segurança: A chave SSH do host '$host' foi alterada.",
        technicalDetails = "Host key verification failed. Fingerprint: $newFingerprint"
    )

    class ConnectionTimeout(
        val timeoutMs: Long,
        cause: Throwable? = null
    ) : NetworkError(
        userFriendlyMessage = "Tempo limite de conexão esgotado ($timeoutMs ms).",
        technicalDetails = "Socket timeout",
        cause = cause
    )

    class BiometricAuthenticationFailed(
        details: String? = null
    ) : NetworkError(
        userFriendlyMessage = "Falha na autenticação biométrica para desbloquear credenciais.",
        technicalDetails = details
    )

    class Generic(
        message: String,
        details: String? = null,
        cause: Throwable? = null
    ) : NetworkError(
        userFriendlyMessage = message,
        technicalDetails = details,
        cause = cause
    )
}
