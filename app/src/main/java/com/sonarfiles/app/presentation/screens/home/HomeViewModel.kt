package com.sonarfiles.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonarfiles.app.core.network.NetworkConnectivityObserver
import com.sonarfiles.app.data.network.scanner.PortProber
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.CredentialRepository
import com.sonarfiles.app.domain.repository.ServerRepository
import com.sonarfiles.app.domain.usecase.scan.WakeOnLanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val credentialRepository: CredentialRepository,
    private val wakeOnLanUseCase: WakeOnLanUseCase,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val portProber: PortProber
) : ViewModel() {

    private val _isGridView = MutableStateFlow(false)
    private val _reachabilityMap = MutableStateFlow<Map<Long, Boolean?>>(emptyMap())
    private val _errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        serverRepository.getAllServers(),
        serverRepository.getFavoriteServers(),
        connectivityObserver.isWifiConnected,
        _isGridView,
        _reachabilityMap,
        _errorFlow
    ) { allServers, favorites, isWifi, isGrid, reachability, error ->
        HomeUiState(
            isLoading = false,
            servers = allServers,
            favoriteServers = favorites,
            isWifiConnected = isWifi,
            isGridView = isGrid,
            serverReachability = reachability,
            errorMessage = error
        )
    }.catch { throwable ->
        emit(HomeUiState(isLoading = false, errorMessage = throwable.localizedMessage))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            isLoading = true,
            isWifiConnected = connectivityObserver.isCurrentlyWifiConnected()
        )
    )

    init {
        viewModelScope.launch {
            combine(
                serverRepository.getAllServers(),
                connectivityObserver.isWifiConnected
            ) { servers, isWifi ->
                servers to isWifi
            }.collectLatest { (servers, isWifi) ->
                if (isWifi && servers.isNotEmpty()) {
                    probeServers(servers)
                } else if (!isWifi) {
                    _reachabilityMap.value = servers.associate { it.id to false }
                } else {
                    _reachabilityMap.value = emptyMap()
                }
            }
        }
    }

    fun probeServers(servers: List<ServerConfig> = uiState.value.servers) {
        if (servers.isEmpty()) return
        viewModelScope.launch {
            _reachabilityMap.update { current ->
                val updated = current.toMutableMap()
                servers.forEach { server ->
                    if (updated[server.id] == null) {
                        updated[server.id] = null
                    }
                }
                updated
            }

            servers.forEach { server ->
                launch {
                    val result = portProber.probePort(
                        ip = server.hostOrIp,
                        port = server.port,
                        timeoutMs = 900
                    )
                    _reachabilityMap.update { current ->
                        current + (server.id to result.isOpen)
                    }
                }
            }
        }
    }

    fun isWifiConnected(): Boolean = connectivityObserver.isCurrentlyWifiConnected()

    fun toggleGridView() {
        _isGridView.update { !it }
    }

    fun toggleFavorite(server: ServerConfig) {
        viewModelScope.launch {
            serverRepository.toggleFavorite(server.id, !server.isFavorite)
        }
    }

    fun deleteServer(server: ServerConfig) {
        viewModelScope.launch {
            try {
                credentialRepository.deleteCredentials(server.id)
                serverRepository.deleteServer(server)
            } catch (e: Exception) {
                _errorFlow.value = "Falha ao excluir servidor: ${e.localizedMessage}"
            }
        }
    }

    fun sendWakeOnLan(macAddress: String) {
        viewModelScope.launch {
            wakeOnLanUseCase(macAddress)
        }
    }

    fun clearError() {
        _errorFlow.value = null
    }
}
