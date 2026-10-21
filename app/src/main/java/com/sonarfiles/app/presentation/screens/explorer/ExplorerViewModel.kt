package com.sonarfiles.app.presentation.screens.explorer

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonarfiles.app.core.network.NetworkConnectivityObserver
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import com.sonarfiles.app.domain.repository.RemoteFileSystem
import com.sonarfiles.app.domain.repository.ServerRepository
import com.sonarfiles.app.domain.usecase.connection.UnlockServerCredentialsUseCase
import com.sonarfiles.app.domain.usecase.explorer.ListDirectoryUseCase
import com.sonarfiles.app.domain.usecase.explorer.ManageRemoteFileUseCase
import com.sonarfiles.app.domain.usecase.explorer.SortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val connectionFactory: RemoteConnectionFactory,
    private val unlockServerCredentialsUseCase: UnlockServerCredentialsUseCase,
    private val listDirectoryUseCase: ListDirectoryUseCase,
    private val manageRemoteFileUseCase: ManageRemoteFileUseCase,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private var activeFileSystem: RemoteFileSystem? = null
    private val pathStack = ArrayDeque<String>()

    fun initSession(activity: FragmentActivity, serverId: Long, initialPath: String) {
        if (activeFileSystem != null && _uiState.value.serverConfig?.id == serverId) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isAuthenticating = true, errorMessage = null) }

            if (!connectivityObserver.isCurrentlyWifiConnected()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticating = false,
                        errorMessage = "Conexão Wi-Fi não detectada. Conecte-se à rede Wi-Fi local para acessar este servidor."
                    )
                }
                return@launch
            }

            val server = serverRepository.getServerById(serverId)
            if (server == null) {
                _uiState.update { it.copy(isLoading = false, isAuthenticating = false, errorMessage = "Servidor não encontrado.") }
                return@launch
            }

            // Desbloqueio seguro de credenciais (com biometria se exigido)
            val credentialsResult = unlockServerCredentialsUseCase(activity, server)
            if (credentialsResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticating = false,
                        errorMessage = "Autenticação biométrica cancelada ou falhou."
                    )
                }
                return@launch
            }

            val credentials = credentialsResult.getOrNull() ?: AuthCredentials.Anonymous
            val connResult = connectionFactory.getOrCreateConnection(server, credentials)

            if (connResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticating = false,
                        errorMessage = "Falha ao conectar ao servidor: ${connResult.exceptionOrNull()?.localizedMessage}"
                    )
                }
                return@launch
            }

            activeFileSystem = connResult.getOrThrow()
            serverRepository.updateLastConnected(server.id, System.currentTimeMillis())

            val targetPath = if (initialPath.isNotBlank()) initialPath else server.defaultShareOrPath
            _uiState.update {
                it.copy(
                    serverConfig = server,
                    currentPath = targetPath,
                    isAuthenticating = false
                )
            }
            pathStack.clear()
            pathStack.add(targetPath)
            loadDirectory(targetPath)
        }
    }

    fun loadDirectory(path: String) {
        val fs = activeFileSystem ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, currentPath = path) }
            val state = _uiState.value
            val result = listDirectoryUseCase(
                remoteFileSystem = fs,
                directoryPath = path,
                showHidden = state.showHidden,
                sortMode = state.sortMode,
                searchQuery = state.searchQuery
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(files = result.getOrThrow(), isLoading = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro ao listar diretório: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }

    fun navigateTo(newPath: String) {
        pathStack.add(newPath)
        loadDirectory(newPath)
    }

    fun navigateUp(): Boolean {
        if (pathStack.size > 1) {
            pathStack.removeLast()
            val previousPath = pathStack.last()
            loadDirectory(previousPath)
            return true
        }
        return false
    }

    fun navigateToBreadcrumb(targetPath: String) {
        while (pathStack.isNotEmpty() && pathStack.last() != targetPath) {
            pathStack.removeLast()
        }
        if (pathStack.isEmpty()) {
            pathStack.add(targetPath)
        }
        loadDirectory(targetPath)
    }

    fun refresh() {
        loadDirectory(_uiState.value.currentPath)
    }

    fun toggleGridView() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleShowHidden() {
        val newShowHidden = !_uiState.value.showHidden
        _uiState.update { it.copy(showHidden = newShowHidden) }
        refresh()
    }

    fun setSortMode(mode: SortMode) {
        _uiState.update { it.copy(sortMode = mode) }
        refresh()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refresh()
    }

    fun toggleSearchActive() {
        val newActive = !_uiState.value.isSearchActive
        _uiState.update {
            it.copy(
                isSearchActive = newActive,
                searchQuery = if (!newActive) "" else it.searchQuery
            )
        }
        if (!newActive) refresh()
    }

    // Ações de arquivo
    fun selectFileForAction(file: RemoteFile?) {
        _uiState.update { it.copy(selectedFileForAction = file) }
    }

    fun openCreateFolderDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isCreateFolderDialogOpen = isOpen) }
    }

    fun createFolder(folderName: String) {
        val fs = activeFileSystem ?: return
        val current = _uiState.value.currentPath
        val newFolderPath = if (current.endsWith("/")) "$current$folderName" else "$current/$folderName"

        viewModelScope.launch {
            val result = manageRemoteFileUseCase.createDirectory(fs, newFolderPath)
            if (result.isSuccess) {
                openCreateFolderDialog(false)
                refresh()
            } else {
                _uiState.update { it.copy(errorMessage = "Erro ao criar pasta: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun openRenameDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isRenameDialogOpen = isOpen) }
    }

    fun renameSelectedFile(newName: String) {
        val fs = activeFileSystem ?: return
        val file = _uiState.value.selectedFileForAction ?: return
        val parentPath = file.fullPath.substringBeforeLast('/', "")
        val destinationPath = if (parentPath.isBlank()) "/$newName" else "$parentPath/$newName"

        viewModelScope.launch {
            val result = manageRemoteFileUseCase.rename(fs, file.fullPath, destinationPath)
            if (result.isSuccess) {
                openRenameDialog(false)
                selectFileForAction(null)
                refresh()
            } else {
                _uiState.update { it.copy(errorMessage = "Erro ao renomear: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun openConfirmDeleteDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isConfirmDeleteDialogOpen = isOpen) }
    }

    fun deleteSelectedFile() {
        val fs = activeFileSystem ?: return
        val file = _uiState.value.selectedFileForAction ?: return

        viewModelScope.launch {
            val result = manageRemoteFileUseCase.delete(fs, file.fullPath, recursive = true)
            if (result.isSuccess) {
                openConfirmDeleteDialog(false)
                selectFileForAction(null)
                refresh()
            } else {
                _uiState.update { it.copy(errorMessage = "Erro ao excluir: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Conexões continuam no pool para reaproveitamento rápido
    }
}
