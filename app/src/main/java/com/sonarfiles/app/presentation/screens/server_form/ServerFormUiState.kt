package com.sonarfiles.app.presentation.screens.server_form

import com.sonarfiles.app.domain.model.DiagnosticStepResult
import com.sonarfiles.app.domain.model.ProtocolType

sealed interface StepStatus {
    data object Pending : StepStatus
    data object InProgress : StepStatus
    data class Passed(val latencyMs: Long, val details: String? = null) : StepStatus
    data class Failed(val reason: String) : StepStatus
}

data class ServerFormUiState(
    val serverId: Long? = null,
    val displayName: String = "",
    val hostOrIp: String = "",
    val port: String = ProtocolType.SMB.defaultPort.toString(),
    val protocol: ProtocolType = ProtocolType.SMB,
    val defaultShareOrPath: String = "/",
    val username: String = "",
    val password: String = "",
    val domainOrWorkgroup: String = "",
    val macAddress: String = "",
    val requiresBiometrics: Boolean = true,
    val isTestingConnection: Boolean = false,
    val diagnosticResults: Map<DiagnosticStepResult.Step, StepStatus> = mapOf(
        DiagnosticStepResult.Step.HOST_REACHABILITY to StepStatus.Pending,
        DiagnosticStepResult.Step.PORT_OPEN to StepStatus.Pending,
        DiagnosticStepResult.Step.AUTHENTICATION to StepStatus.Pending,
        DiagnosticStepResult.Step.ROOT_ACCESS to StepStatus.Pending
    ),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)
