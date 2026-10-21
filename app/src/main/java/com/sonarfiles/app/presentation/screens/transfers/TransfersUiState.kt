package com.sonarfiles.app.presentation.screens.transfers

import com.sonarfiles.app.domain.model.TransferTask

data class TransfersUiState(
    val activeTransfers: List<TransferTask> = emptyList(),
    val completedTransfers: List<TransferTask> = emptyList(),
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean
        get() = !isLoading && activeTransfers.isEmpty() && completedTransfers.isEmpty()
}
