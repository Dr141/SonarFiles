package com.sonarfiles.app.domain.model

sealed interface DiagnosticStepResult {
    data class InProgress(val step: Step) : DiagnosticStepResult
    data class StepPassed(val step: Step, val latencyMs: Long, val details: String? = null) : DiagnosticStepResult
    data class StepFailed(val step: Step, val reason: String, val errorCode: String? = null) : DiagnosticStepResult

    enum class Step {
        HOST_REACHABILITY,
        PORT_OPEN,
        AUTHENTICATION,
        ROOT_ACCESS
    }
}
