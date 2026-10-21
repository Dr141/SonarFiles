package com.sonarfiles.app.presentation.screens.sonar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonarfiles.app.data.network.scanner.SubnetCalculator
import com.sonarfiles.app.domain.model.DiscoveredHost
import com.sonarfiles.app.domain.usecase.scan.ScanLocalNetworkUseCase
import com.sonarfiles.app.domain.usecase.scan.WakeOnLanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SonarViewModel @Inject constructor(
    private val scanLocalNetworkUseCase: ScanLocalNetworkUseCase,
    private val wakeOnLanUseCase: WakeOnLanUseCase,
    private val subnetCalculator: SubnetCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SonarUiState())
    val uiState: StateFlow<SonarUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        loadSubnetInfo()
        startScan()
    }

    fun loadSubnetInfo() {
        val subnet = subnetCalculator.getLocalSubnet()
        if (subnet != null) {
            _uiState.update {
                it.copy(
                    localIp = subnet.localIp,
                    subnetPrefix = "${subnet.localIp.substringBeforeLast('.')}.0/24"
                )
            }
        } else {
            _uiState.update {
                it.copy(errorMessage = "Dispositivo desconectado da rede Wi-Fi.")
            }
        }
    }

    fun startScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(hosts = emptyList(), errorMessage = null) }

        scanJob = viewModelScope.launch {
            scanLocalNetworkUseCase()
                .onStart {
                    _uiState.update { it.copy(isScanning = true) }
                }
                .onCompletion {
                    _uiState.update { it.copy(isScanning = false) }
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = error.localizedMessage ?: "Erro ao varrer a sub-rede"
                        )
                    }
                }
                .collect { host ->
                    _uiState.update { current ->
                        val existingIndex = current.hosts.indexOfFirst { it.ipAddress == host.ipAddress }
                        val updatedList = if (existingIndex >= 0) {
                            current.hosts.toMutableList().apply { set(existingIndex, host) }
                        } else {
                            current.hosts + host
                        }
                        current.copy(hosts = updatedList.sortedBy { it.responseTimeMs })
                    }
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun sendWakeOnLan(macAddress: String) {
        viewModelScope.launch {
            wakeOnLanUseCase(macAddress)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
