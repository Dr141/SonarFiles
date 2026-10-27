package com.sonarfiles.app.core.common

import com.sonarfiles.app.domain.model.DiagnosticStepResult

data class FormattedDiagnosticError(
    val reason: String,
    val hint: String? = null,
    val errorCode: String? = null
)

object ConnectionErrorFormatter {

    fun format(
        step: DiagnosticStepResult.Step,
        throwable: Throwable?,
        username: String? = null,
        host: String? = null,
        port: Int? = null,
        pathOrShare: String? = null
    ): FormattedDiagnosticError {
        val rawMessage = throwable?.message.orEmpty()
        val causeMessage = throwable?.cause?.message.orEmpty()
        val fullTrace = "$rawMessage $causeMessage ${throwable?.javaClass?.name.orEmpty()}"

        return when (step) {
            DiagnosticStepResult.Step.HOST_REACHABILITY -> formatHostReachability(host, fullTrace, throwable)
            DiagnosticStepResult.Step.PORT_OPEN -> formatPortOpen(host, port, fullTrace, throwable)
            DiagnosticStepResult.Step.AUTHENTICATION -> formatAuthentication(username, fullTrace, throwable)
            DiagnosticStepResult.Step.ROOT_ACCESS -> formatRootAccess(pathOrShare, fullTrace, throwable)
        }
    }

    private fun formatHostReachability(
        host: String?,
        trace: String,
        throwable: Throwable?
    ): FormattedDiagnosticError {
        val targetHost = host ?: "informado"
        return when {
            trace.contains("UnknownHostException", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Não foi possível resolver o endereço de rede '$targetHost'.",
                    hint = "Verifique a digitação do IP ou Hostname e certifique-se de que o aparelho está na mesma rede local.",
                    errorCode = "DNS_UNRESOLVED"
                )
            }
            trace.contains("NoRouteToHostException", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Sem rota de rede até o host '$targetHost'.",
                    hint = "Certifique-se de que o celular e o computador estão na mesma rede Wi-Fi (evite redes de convidados isoladas ou dados móveis).",
                    errorCode = "NO_ROUTE_TO_HOST"
                )
            }
            else -> {
                val clean = cleanTechnicalJargon(throwable?.localizedMessage ?: "Dispositivo inacessível")
                FormattedDiagnosticError(
                    reason = "Não foi possível alcançar o host '$targetHost': $clean",
                    hint = "Verifique se o computador ou NAS está ligado e conectado ao mesmo roteador Wi-Fi.",
                    errorCode = "HOST_UNREACHABLE"
                )
            }
        }
    }

    private fun formatPortOpen(
        host: String?,
        port: Int?,
        trace: String,
        throwable: Throwable?
    ): FormattedDiagnosticError {
        val targetPort = port ?: 445
        val targetHost = host ?: "servidor"
        return when {
            trace.contains("Connection refused", ignoreCase = true) ||
            trace.contains("ConnectException", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Porta $targetPort fechada ou serviço inativo no host '$targetHost'.",
                    hint = if (targetPort == 445) {
                        "Certifique-se de que o Compartilhamento de Arquivos e Impressoras (SMB) está ativado no Windows/Linux."
                    } else {
                        "Verifique se o serviço SSH/SFTP está rodando e escutando na porta $targetPort."
                    },
                    errorCode = "PORT_CLOSED"
                )
            }
            trace.contains("SocketTimeoutException", ignoreCase = true) ||
            trace.contains("timed out", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Tempo limite esgotado ao tentar alcançar a porta $targetPort em '$targetHost'.",
                    hint = "O Firewall do Windows pode estar bloqueando a porta $targetPort. Verifique as regras de entrada de rede privada no Defender.",
                    errorCode = "PORT_TIMEOUT"
                )
            }
            else -> {
                FormattedDiagnosticError(
                    reason = "Porta $targetPort inacessível ou bloqueada no host '$targetHost'.",
                    hint = "Verifique as configurações de rede e firewall do servidor.",
                    errorCode = "PORT_UNREACHABLE"
                )
            }
        }
    }

    private fun formatAuthentication(
        username: String?,
        trace: String,
        throwable: Throwable?
    ): FormattedDiagnosticError {
        val isEmail = username?.contains("@") == true

        return when {
            // STATUS_LOGON_FAILURE (0xc000006d)
            trace.contains("STATUS_LOGON_FAILURE", ignoreCase = true) ||
            trace.contains("0xc000006d", ignoreCase = true) -> {
                val hint = if (isEmail) {
                    "No Windows, contas Microsoft (@hotmail/@outlook) exigem a senha da conta (e NÃO o PIN do Windows Hello). No Windows 11, o compartilhamento NTLM costuma falhar com conta da nuvem; o recomendado é criar um Usuário Local dedicado no Windows."
                } else {
                    "Verifique usuário e senha. Se você utiliza login com PIN no Windows, insira a senha real da conta (o PIN não funciona via rede). Se o computador estiver em domínio específico, preencha o campo Domínio."
                }
                FormattedDiagnosticError(
                    reason = "Falha na autenticação: Usuário ou senha incorretos (STATUS_LOGON_FAILURE).",
                    hint = hint,
                    errorCode = "STATUS_LOGON_FAILURE"
                )
            }

            // STATUS_ACCESS_DENIED (0xc0000022)
            trace.contains("STATUS_ACCESS_DENIED", ignoreCase = true) ||
            trace.contains("0xc0000022", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Acesso negado: a conta informada não tem permissão de logon de rede.",
                    hint = "Verifique nas configurações do Windows se a conta possui permissão para acessar o computador pela rede.",
                    errorCode = "STATUS_ACCESS_DENIED"
                )
            }

            // STATUS_ACCOUNT_RESTRICTION (0xc000006e)
            trace.contains("STATUS_ACCOUNT_RESTRICTION", ignoreCase = true) ||
            trace.contains("0xc000006e", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Restrição na conta do Windows (STATUS_ACCOUNT_RESTRICTION).",
                    hint = "O Windows bloqueia acessos de rede para contas sem senha por padrão. Configure uma senha para o usuário local no Windows.",
                    errorCode = "STATUS_ACCOUNT_RESTRICTION"
                )
            }

            // STATUS_ACCOUNT_DISABLED (0xc0000072)
            trace.contains("STATUS_ACCOUNT_DISABLED", ignoreCase = true) ||
            trace.contains("0xc0000072", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "A conta informada está desativada no servidor (STATUS_ACCOUNT_DISABLED).",
                    hint = "Ative a conta no painel de Usuários do Windows ou Linux antes de autenticar.",
                    errorCode = "STATUS_ACCOUNT_DISABLED"
                )
            }

            // STATUS_ACCOUNT_LOCKED_OUT (0xc0000234)
            trace.contains("STATUS_ACCOUNT_LOCKED_OUT", ignoreCase = true) ||
            trace.contains("0xc0000234", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Conta bloqueada por excesso de tentativas incorretas (STATUS_ACCOUNT_LOCKED_OUT).",
                    hint = "Aguarde o período de bloqueio ou desbloqueie o usuário no Windows.",
                    errorCode = "STATUS_ACCOUNT_LOCKED_OUT"
                )
            }

            // STATUS_PASSWORD_EXPIRED (0xc0000071)
            trace.contains("STATUS_PASSWORD_EXPIRED", ignoreCase = true) ||
            trace.contains("0xc0000071", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "A senha da conta informada expirou no servidor.",
                    hint = "Altere a senha da conta no sistema operacional antes de conectar.",
                    errorCode = "STATUS_PASSWORD_EXPIRED"
                )
            }

            // SSH UserAuthException
            trace.contains("UserAuthException", ignoreCase = true) ||
            trace.contains("Auth fail", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Falha na autenticação SSH: credenciais ou chave privada rejeitadas.",
                    hint = "Verifique se o usuário, senha ou o par de chaves SSH configurado estão corretos no servidor.",
                    errorCode = "SSH_AUTH_FAILED"
                )
            }

            else -> {
                val clean = cleanTechnicalJargon(throwable?.localizedMessage ?: "Credenciais inválidas")
                FormattedDiagnosticError(
                    reason = "Falha na autenticação: $clean",
                    hint = if (isEmail) {
                        "Se estiver usando Windows, lembre-se que contas Microsoft não aceitam PIN via rede. Experimente usar o nome de usuário local ou crie um usuário local no Windows."
                    } else {
                        "Verifique se o nome de usuário e a senha foram digitados corretamente."
                    },
                    errorCode = "AUTH_FAILED"
                )
            }
        }
    }

    private fun formatRootAccess(
        pathOrShare: String?,
        trace: String,
        throwable: Throwable?
    ): FormattedDiagnosticError {
        val targetPath = pathOrShare?.trimStart('/') ?: ""

        return when {
            // STATUS_BAD_NETWORK_NAME (0xc00000cc)
            trace.contains("STATUS_BAD_NETWORK_NAME", ignoreCase = true) ||
            trace.contains("0xc00000cc", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Compartilhamento '$targetPath' não encontrado no servidor.",
                    hint = "Verifique o nome exato da pasta compartilhada no Windows (ex: 'testes' ou 'Users') e insira no campo Diretório Raiz.",
                    errorCode = "STATUS_BAD_NETWORK_NAME"
                )
            }

            // STATUS_ACCESS_DENIED (0xc0000022)
            trace.contains("STATUS_ACCESS_DENIED", ignoreCase = true) ||
            trace.contains("0xc0000022", ignoreCase = true) ||
            trace.contains("Permission denied", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "Acesso negado ao diretório/compartilhamento '$targetPath'.",
                    hint = "A conta autenticou com sucesso, mas não tem permissão NTFS ou de compartilhamento para acessar esta pasta. Conceda permissão nas Propriedades da pasta no servidor.",
                    errorCode = "STATUS_ACCESS_DENIED"
                )
            }

            // STATUS_NETWORK_NAME_DELETED
            trace.contains("STATUS_NETWORK_NAME_DELETED", ignoreCase = true) -> {
                FormattedDiagnosticError(
                    reason = "O compartilhamento foi desativado ou desconectado pelo servidor.",
                    hint = "Verifique se o computador não suspendeu ou se o compartilhamento ainda está ativo.",
                    errorCode = "STATUS_NETWORK_NAME_DELETED"
                )
            }

            else -> {
                val clean = cleanTechnicalJargon(throwable?.localizedMessage ?: "Não foi possível acessar o caminho")
                FormattedDiagnosticError(
                    reason = "Acesso ao diretório '$targetPath' falhou: $clean",
                    hint = "Verifique se o compartilhamento está ativo e se o usuário tem permissão de leitura.",
                    errorCode = "ROOT_ACCESS_FAILED"
                )
            }
        }
    }

    /**
     * Remove vazamento de classes internas Java (ex: com.hierynomus.smbj.auth.NtlmSealer@52350fb)
     * e pacotes desnecessários de stack trace.
     */
    fun cleanTechnicalJargon(raw: String): String {
        return raw
            .replace(Regex("""using\s+com\.hierynomus\.[a-zA-Z0-9_.]*@\w+"""), "")
            .replace(Regex("""com\.hierynomus\.[a-zA-Z0-9_.]*:\s*"""), "")
            .replace(Regex("""net\.schmizz\.sshj\.[a-zA-Z0-9_.]*:\s*"""), "")
            .replace(Regex("""java\.net\.[a-zA-Z0-9_.]*:\s*"""), "")
            .replace(Regex("""java\.io\.[a-zA-Z0-9_.]*:\s*"""), "")
            .replace(Regex("""@[0-9a-fA-F]+"""), "")
            .trim()
            .trimEnd(':')
            .trim()
    }
}
