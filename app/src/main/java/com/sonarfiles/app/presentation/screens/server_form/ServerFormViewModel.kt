package com.sonarfiles.app.presentation.screens.server_form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.DiagnosticStepResult
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.CredentialRepository
import com.sonarfiles.app.domain.repository.ServerRepository
import com.sonarfiles.app.domain.usecase.connection.TestConnectionDiagnosticUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val credentialRepository: CredentialRepository,
    private val testConnectionDiagnosticUseCase: TestConnectionDiagnosticUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerFormUiState())
    val uiState: StateFlow<ServerFormUiState> = _uiState.asStateFlow()

    private var diagnosticJob: Job? = null

    fun onDisplayNameChanged(value: String) = _uiState.update { it.copy(displayName = value) }
    fun onHostOrIpChanged(value: String) = _uiState.update { it.copy(hostOrIp = value) }
    fun onPortChanged(value: String) = _uiState.update { it.copy(port = value) }
    fun onProtocolChanged(value: ProtocolType) = _uiState.update {
        it.copy(
            protocol = value,
            port = value.defaultPort.toString(),
            defaultShareOrPath = if (value == ProtocolType.SMB) "/" else "/home"
        )
    }
    fun onDefaultPathChanged(value: String) = _uiState.update { it.copy(defaultShareOrPath = value) }
    fun onUsernameChanged(value: String) = _uiState.update { it.copy(username = value) }
    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value) }
    fun onDomainChanged(value: String) = _uiState.update { it.copy(domainOrWorkgroup = value) }
    fun onMacAddressChanged(value: String) = _uiState.update { it.copy(macAddress = value) }
    fun onRequiresBiometricsChanged(value: Boolean) = _uiState.update { it.copy(requiresBiometrics = value) }

    fun prefillFromDiscoveredHost(ip: String, protocol: ProtocolType, suggestedHostname: String?) {
        _uiState.update {
            it.copy(
                hostOrIp = ip,
                protocol = protocol,
                port = protocol.defaultPort.toString(),
                displayName = suggestedHostname ?: "Servidor $ip"
            )
        }
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.hostOrIp.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o IP ou Hostname do servidor.") }
            return
        }

        val portNumber = state.port.toIntOrNull() ?: state.protocol.defaultPort
        val serverConfig = ServerConfig(
            id = state.serverId ?: 0L,
            displayName = state.displayName.ifBlank { state.hostOrIp },
            hostOrIp = state.hostOrIp.trim(),
            port = portNumber,
            protocol = state.protocol,
            defaultShareOrPath = state.defaultShareOrPath.ifBlank { "/" },
            domainOrWorkgroup = state.domainOrWorkgroup.ifBlank { null },
            macAddress = state.macAddress.ifBlank { null },
            requiresBiometrics = state.requiresBiometrics
        )

        val credentials = if (state.username.isNotBlank()) {
            AuthCredentials.Password(
                username = state.username.trim(),
                password = state.password.toCharArray(),
                domain = state.domainOrWorkgroup.ifBlank { null }
            )
        } else {
            AuthCredentials.Anonymous
        }

        diagnosticJob?.cancel()
        _uiState.update {
            it.copy(
                isTestingConnection = true,
                errorMessage = null,
                diagnosticResults = mapOf(
                    DiagnosticStepResult.Step.HOST_REACHABILITY to StepStatus.Pending,
                    DiagnosticStepResult.Step.PORT_OPEN to StepStatus.Pending,
                    DiagnosticStepResult.Step.AUTHENTICATION to StepStatus.Pending,
                    DiagnosticStepResult.Step.ROOT_ACCESS to StepStatus.Pending
                )
            )
        }

        diagnosticJob = viewModelScope.launch {
            testConnectionDiagnosticUseCase(serverConfig, credentials).collect { stepResult ->
                _uiState.update { current ->
                    val currentMap = current.diagnosticResults.toMutableMap()
                    when (stepResult) {
                        is DiagnosticStepResult.InProgress -> {
                            currentMap[stepResult.step] = StepStatus.InProgress
                        }
                        is DiagnosticStepResult.StepPassed -> {
                            currentMap[stepResult.step] = StepStatus.Passed(stepResult.latencyMs, stepResult.details)
                        }
                        is DiagnosticStepResult.StepFailed -> {
                            currentMap[stepResult.step] = StepStatus.Failed(stepResult.reason)
                        }
                    }
                    current.copy(
                        diagnosticResults = currentMap,
                        isTestingConnection = stepResult !is DiagnosticStepResult.StepFailed &&
                                currentMap.values.any { it is StepStatus.InProgress || it is StepStatus.Pending }
                    )
                }
            }
            _uiState.update { it.copy(isTestingConnection = false) }
        }
    }

    fun saveServer() {
        val state = _uiState.value
        if (state.hostOrIp.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o IP ou Hostname do servidor.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val portNumber = state.port.toIntOrNull() ?: state.protocol.defaultPort
                val config = ServerConfig(
                    id = state.serverId ?: 0L,
                    displayName = state.displayName.ifBlank { state.hostOrIp },
                    hostOrIp = state.hostOrIp.trim(),
                    port = portNumber,
                    protocol = state.protocol,
                    defaultShareOrPath = state.defaultShareOrPath.ifBlank { "/" },
                    domainOrWorkgroup = state.domainOrWorkgroup.ifBlank { null },
                    macAddress = state.macAddress.ifBlank { null },
                    requiresBiometrics = state.requiresBiometrics
                )

                val generatedId = serverRepository.insertOrUpdateServer(config)
                val targetServerId = if (config.id != 0L) config.id else generatedId

                if (state.username.isNotBlank()) {
                    val credentials = AuthCredentials.Password(
                        username = state.username.trim(),
                        password = state.password.toCharArray(),
                        domain = state.domainOrWorkgroup.ifBlank { null }
                    )
                    credentialRepository.saveCredentials(targetServerId, credentials).getOrThrow()
                }

                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage ?: "Erro ao salvar") }
            }
        }
    }
}
