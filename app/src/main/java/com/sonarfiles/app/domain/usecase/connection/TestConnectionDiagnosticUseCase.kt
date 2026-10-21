package com.sonarfiles.app.domain.usecase.connection

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

        // Passo 1: Alcançabilidade do Host (DNS / IP Reachable)
        emit(DiagnosticStepResult.InProgress(DiagnosticStepResult.Step.HOST_REACHABILITY))
        val step1Start = System.currentTimeMillis()
        val isReachable = try {
            val addr = InetAddress.getByName(host)
            addr.isReachable(3000) || true // Se ICMP for bloqueado por firewall, prossegue para checagem de porta
        } catch (e: Exception) {
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.HOST_REACHABILITY,
                    reason = "Não foi possível resolver ou alcançar o host '$host': ${e.localizedMessage}"
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
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.PORT_OPEN,
                    reason = "Porta ${serverConfig.port} fechada ou bloqueada por firewall no host '$host'."
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
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.AUTHENTICATION,
                    reason = "Falha na autenticação: ${err?.localizedMessage ?: "Credenciais inválidas"}"
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
            emit(
                DiagnosticStepResult.StepFailed(
                    DiagnosticStepResult.Step.ROOT_ACCESS,
                    reason = "Conexão aceita, mas acesso ao diretório '${serverConfig.defaultShareOrPath}' falhou: ${err?.localizedMessage}"
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
