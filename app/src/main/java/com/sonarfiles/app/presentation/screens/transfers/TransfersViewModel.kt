package com.sonarfiles.app.presentation.screens.transfers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TransfersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferRepository: TransferRepository
) : ViewModel() {

    val uiState: StateFlow<TransfersUiState> = transferRepository.getAllTransfers()
        .map { all ->
            val active = all.filter { !it.isFinished }
            val completed = all.filter { it.isFinished }
            TransfersUiState(
                activeTransfers = active,
                completedTransfers = completed,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransfersUiState(isLoading = true)
        )

    fun cancelTransfer(task: TransferTask) {
        viewModelScope.launch {
            if (task.workRequestId != null) {
                try {
                    WorkManager.getInstance(context).cancelWorkById(UUID.fromString(task.workRequestId))
                } catch (_: Exception) {}
            }
            transferRepository.markCompleted(task.id, com.sonarfiles.app.domain.model.TransferStatus.CANCELLED, "Cancelado pelo usuário")
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            transferRepository.clearCompletedTransfers()
        }
    }

    fun deleteTransfer(task: TransferTask) {
        viewModelScope.launch {
            transferRepository.deleteTransfer(task.id)
        }
    }
}
