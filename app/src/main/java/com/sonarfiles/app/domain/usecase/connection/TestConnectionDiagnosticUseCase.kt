package com.sonarfiles.app.domain.usecase.connection

import com.sonarfiles.app.core.common.ConnectionErrorFormatter
import com.sonarfiles.app.core.common.DispatchersProvider
import com.sonarfiles.app.data.network.scanner.PortProber
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.DiagnosticStepResult
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetAddress
import javax.inject.Inject

class TestConnectionDiagnosticUseCase @Inject constructor(
    private val portProber: PortProber,
    private val connectionFactory: RemoteConnectionFactory,
    private val dispatchers: DispatchersProvider
) {

    operator fun invoke(
        serverConfig: ServerConfig,
        credentials: AuthCredentials
    ): Flow<DiagnosticStepResult> = flow {
        val host = serverConfig.hostOrIp
        val username = when (credentials) {
            is AuthCredentials.Password -> credentials.username
            is AuthCredentials.SshKey -> credentials.username
            is AuthCredentials.Anonymous -> null
        }

        // Passo 1: Alcançabilidade do Host (DNS / IP Reachable)
        emit(DiagnosticStepResult.InProgress(DiagnosticStepResult.Step.HOST_REACHABILITY))
        val step1Start = System.currentTimeMillis()
        val isReachable = try {
            val addr = InetAddress.getByName(host)
            addr.isReachable(3000) || true // Se ICMP for bloqueado por firewall, prossegue para checagem de porta
        } catch (e: Exception) {
            val formatted = ConnectionErrorFormatter.format(
                step = DiagnosticStepResult.Step.HOST_REACHABILITY,
                throwable = e,
                host = host
            )
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.HOST_REACHABILITY,
                    reason = formatted.reason,
                    errorCode = formatted.errorCode,
                    hint = formatted.hint
                )
            )
            return@flow
        }
        val step1Latency = System.currentTimeMillis() - step1Start
        emit(DiagnosticStepResult.StepPassed(DiagnosticStepResult.Step.HOST_REACHABILITY, latencyMs = step1Latency))

        // Passo 2: Verificação de Porta Aberta
        emit(DiagnosticStepResult.InProgress(DiagnosticStepResult.Step.PORT_OPEN))
        val probe = portProber.probePort(host, serverConfig.port, timeoutMs = 4000)
        if (!probe.isOpen) {
            val formatted = ConnectionErrorFormatter.format(
                step = DiagnosticStepResult.Step.PORT_OPEN,
                throwable = null,
                host = host,
                port = serverConfig.port
            )
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.PORT_OPEN,
                    reason = formatted.reason,
                    errorCode = formatted.errorCode,
                    hint = formatted.hint
                )
            )
            return@flow
        }
        emit(DiagnosticStepResult.StepPassed(DiagnosticStepResult.Step.PORT_OPEN, latencyMs = probe.latencyMs))

        // Passo 3: Autenticação com o Servidor
        emit(DiagnosticStepResult.InProgress(DiagnosticStepResult.Step.AUTHENTICATION))
        val connection = connectionFactory.createEphemeralConnection(serverConfig)
        val step3Start = System.currentTimeMillis()
        val connectResult = connection.connect(credentials)
        val step3Latency = System.currentTimeMillis() - step3Start

        if (connectResult.isFailure) {
            val err = connectResult.exceptionOrNull()
            val formatted = ConnectionErrorFormatter.format(
                step = DiagnosticStepResult.Step.AUTHENTICATION,
                throwable = err,
                username = username,
                host = host,
                port = serverConfig.port
            )
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.AUTHENTICATION,
                    reason = formatted.reason,
                    errorCode = formatted.errorCode,
                    hint = formatted.hint
                )
            )
            connection.close()
            return@flow
        }
        emit(DiagnosticStepResult.StepPassed(DiagnosticStepResult.Step.AUTHENTICATION, latencyMs = step3Latency))

        // Passo 4: Acesso ao Diretório Raiz / Compartilhamento
        emit(DiagnosticStepResult.InProgress(DiagnosticStepResult.Step.ROOT_ACCESS))
        val step4Start = System.currentTimeMillis()
        val listResult = connection.listDirectory(serverConfig.defaultShareOrPath)
        val step4Latency = System.currentTimeMillis() - step4Start

        if (listResult.isFailure) {
            val err = listResult.exceptionOrNull()
            val formatted = ConnectionErrorFormatter.format(
                step = DiagnosticStepResult.Step.ROOT_ACCESS,
                throwable = err,
                username = username,
                host = host,
                pathOrShare = serverConfig.defaultShareOrPath
            )
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.ROOT_ACCESS,
                    reason = formatted.reason,
                    errorCode = formatted.errorCode,
                    hint = formatted.hint
                )
            )
        } else {
            val itemsCount = listResult.getOrNull()?.size ?: 0
            emit(
                DiagnosticStepResult.StepPassed(
                    DiagnosticStepResult.Step.ROOT_ACCESS,
                    latencyMs = step4Latency,
                    details = "$itemsCount itens encontrados no caminho"
                )
            )
        }

        connection.close()
    }.flowOn(dispatchers.io)
}
