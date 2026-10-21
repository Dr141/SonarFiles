package com.sonarfiles.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonarfiles.app.core.network.NetworkConnectivityObserver
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.CredentialRepository
import com.sonarfiles.app.domain.repository.ServerRepository
import com.sonarfiles.app.domain.usecase.scan.WakeOnLanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val credentialRepository: CredentialRepository,
    private val wakeOnLanUseCase: WakeOnLanUseCase,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        serverRepository.getAllServers(),
        serverRepository.getFavoriteServers(),
        connectivityObserver.isWifiConnected,
        _errorFlow
    ) { allServers, favorites, isWifi, error ->
        HomeUiState(
            isLoading = false,
            servers = allServers,
            favoriteServers = favorites,
            isWifiConnected = isWifi,
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

    fun isWifiConnected(): Boolean = connectivityObserver.isCurrentlyWifiConnected()

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
