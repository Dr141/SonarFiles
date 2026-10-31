package com.sonarfiles.app.presentation.screens.explorer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonarfiles.app.core.common.ConnectionErrorFormatter
import com.sonarfiles.app.core.network.NetworkConnectivityObserver
import com.sonarfiles.app.domain.model.AuthCredentials
import com.sonarfiles.app.domain.model.DiagnosticStepResult
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.model.TransferType
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import com.sonarfiles.app.domain.repository.RemoteFileSystem
import com.sonarfiles.app.domain.repository.ServerRepository
import com.sonarfiles.app.domain.repository.TransferRepository
import com.sonarfiles.app.domain.usecase.connection.UnlockServerCredentialsUseCase
import com.sonarfiles.app.domain.usecase.explorer.ListDirectoryUseCase
import com.sonarfiles.app.domain.usecase.explorer.ManageRemoteFileUseCase
import com.sonarfiles.app.domain.usecase.explorer.SortMode
import com.sonarfiles.app.domain.usecase.transfer.EnqueueTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class OpenFileEvent(
    val file: File,
    val mimeType: String
)

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val connectionFactory: RemoteConnectionFactory,
    private val unlockServerCredentialsUseCase: UnlockServerCredentialsUseCase,
    private val listDirectoryUseCase: ListDirectoryUseCase,
    private val manageRemoteFileUseCase: ManageRemoteFileUseCase,
    private val enqueueTransferUseCase: EnqueueTransferUseCase,
    private val transferRepository: TransferRepository,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _openFileEvent = MutableSharedFlow<OpenFileEvent>()
    val openFileEvent: SharedFlow<OpenFileEvent> = _openFileEvent.asSharedFlow()

    private var activeFileSystem: RemoteFileSystem? = null
    private val pathStack = ArrayDeque<String>()
    private val knownCompletedUploadIds = mutableSetOf<Long>()
    private var transferObserverStarted = false

    private fun startObservingTransfers(serverId: Long) {
        if (transferObserverStarted) return
        transferObserverStarted = true

        viewModelScope.launch {
            var isFirstEmission = true
            transferRepository.getAllTransfers().collectLatest { transfers ->
                val active = transfers.filter { it.serverId == serverId && !it.isFinished }
                _uiState.update { it.copy(activeTransfers = active) }

                val completedUploads = transfers.filter {
                    it.serverId == serverId &&
                    it.type == TransferType.UPLOAD &&
                    it.status == TransferStatus.COMPLETED
                }

                if (isFirstEmission) {
                    completedUploads.forEach { knownCompletedUploadIds.add(it.id) }
                    isFirstEmission = false
                } else {
                    val newlyCompleted = completedUploads.filter { it.id !in knownCompletedUploadIds }
                    if (newlyCompleted.isNotEmpty()) {
                        newlyCompleted.forEach { knownCompletedUploadIds.add(it.id) }
                        refresh()
                        val names = newlyCompleted.joinToString { it.fileName }
                        _uiState.update { it.copy(infoMessage = "Upload concluído: $names") }
                    }
                }
            }
        }
    }

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
                val username = (credentials as? AuthCredentials.Password)?.username
                val formatted = ConnectionErrorFormatter.format(
                    step = DiagnosticStepResult.Step.AUTHENTICATION,
                    throwable = connResult.exceptionOrNull(),
                    username = username,
                    host = server.hostOrIp,
                    port = server.port
                )
                val hintText = if (formatted.hint != null) " • ${formatted.hint}" else ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticating = false,
                        errorMessage = "${formatted.reason}$hintText"
                    )
                }
                return@launch
            }

            activeFileSystem = connResult.getOrThrow()
            serverRepository.updateLastConnected(server.id, System.currentTimeMillis())
            startObservingTransfers(server.id)

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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentPath = path,
                    isSelectionMode = false,
                    selectedFilePaths = emptySet()
                )
            }
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
                val formatted = ConnectionErrorFormatter.format(
                    step = DiagnosticStepResult.Step.ROOT_ACCESS,
                    throwable = result.exceptionOrNull(),
                    pathOrShare = path
                )
                val hintText = if (formatted.hint != null) " (${formatted.hint})" else ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "${formatted.reason}$hintText"
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
        if (_uiState.value.isSelectionMode) {
            clearSelection()
            return true
        }
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

    // --- Modo de Seleção Múltipla ---
    fun toggleSelectionMode() {
        _uiState.update { current ->
            val nextMode = !current.isSelectionMode
            current.copy(
                isSelectionMode = nextMode,
                selectedFilePaths = if (nextMode) current.selectedFilePaths else emptySet()
            )
        }
    }

    fun toggleFileSelection(file: RemoteFile) {
        _uiState.update { current ->
            val mutable = current.selectedFilePaths.toMutableSet()
            if (file.fullPath in mutable) {
                mutable.remove(file.fullPath)
            } else {
                mutable.add(file.fullPath)
            }
            current.copy(
                isSelectionMode = mutable.isNotEmpty(),
                selectedFilePaths = mutable
            )
        }
    }

    fun selectAllFiles() {
        _uiState.update { current ->
            val allPaths = current.files.map { it.fullPath }.toSet()
            val shouldSelectAll = current.selectedFilePaths.size < allPaths.size
            current.copy(
                isSelectionMode = true,
                selectedFilePaths = if (shouldSelectAll) allPaths else emptySet()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedFilePaths = emptySet(),
                isConfirmBatchDeleteDialogOpen = false
            )
        }
    }

    fun openConfirmBatchDeleteDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isConfirmBatchDeleteDialogOpen = isOpen) }
    }

    fun deleteSelectedItems() {
        val fs = activeFileSystem ?: return
        val itemsToDelete = _uiState.value.selectedItems
        if (itemsToDelete.isEmpty()) return

        viewModelScope.launch {
            var deletedCount = 0
            var lastError: String? = null

            itemsToDelete.forEach { item ->
                val result = manageRemoteFileUseCase.delete(fs, item.fullPath, recursive = true)
                if (result.isSuccess) {
                    deletedCount++
                } else {
                    lastError = ConnectionErrorFormatter.cleanTechnicalJargon(
                        result.exceptionOrNull()?.localizedMessage.orEmpty()
                    )
                }
            }

            openConfirmBatchDeleteDialog(false)
            clearSelection()
            refresh()

            if (deletedCount > 0) {
                _uiState.update {
                    it.copy(infoMessage = "$deletedCount item(ns) excluído(s) com sucesso!")
                }
            }
            if (lastError != null && deletedCount < itemsToDelete.size) {
                _uiState.update {
                    it.copy(errorMessage = "Erro ao excluir alguns itens: $lastError")
                }
            }
        }
    }

    // Ações de arquivo individual
    fun selectFileForAction(file: RemoteFile?) {
        _uiState.update { it.copy(selectedFileForAction = file) }
    }

    fun openCreateFolderDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isCreateFolderDialogOpen = isOpen) }
    }

    fun createFolder(folderName: String) {
        val fs = activeFileSystem ?: return
        val current = _uiState.value.currentPath
        val isSmb = _uiState.value.serverConfig?.protocol == ProtocolType.SMB
        val isRoot = current.trim().trim('/') == ""

        if (isSmb && isRoot) {
            _uiState.update {
                it.copy(
                    errorMessage = "Não é possível criar pastas na raiz do servidor SMB. Acesse uma das pastas compartilhadas (ex: /testes) para criar novas subpastas."
                )
            }
            openCreateFolderDialog(false)
            return
        }

        val newFolderPath = if (current.endsWith("/")) "$current$folderName" else "$current/$folderName"

        viewModelScope.launch {
            val result = manageRemoteFileUseCase.createDirectory(fs, newFolderPath)
            if (result.isSuccess) {
                openCreateFolderDialog(false)
                refresh()
            } else {
                val clean = ConnectionErrorFormatter.cleanTechnicalJargon(result.exceptionOrNull()?.localizedMessage.orEmpty())
                _uiState.update { it.copy(errorMessage = "Erro ao criar pasta: $clean") }
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
                val clean = ConnectionErrorFormatter.cleanTechnicalJargon(result.exceptionOrNull()?.localizedMessage.orEmpty())
                _uiState.update { it.copy(errorMessage = "Erro ao renomear: $clean") }
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
                val clean = ConnectionErrorFormatter.cleanTechnicalJargon(result.exceptionOrNull()?.localizedMessage.orEmpty())
                _uiState.update { it.copy(errorMessage = "Erro ao excluir: $clean") }
            }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun openFile(file: RemoteFile) {
        if (file.isDirectory) return
        val fs = activeFileSystem ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isOpeningFile = true, openingFileName = file.name) }
            try {
                val previewDir = File(context.cacheDir, "preview_cache")
                previewDir.mkdirs()
                val cachedFile = File(previewDir, file.name)

                val stream = fs.openReadStream(file.fullPath).getOrThrow()
                stream.use { input ->
                    FileOutputStream(cachedFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val ext = MimeTypeMap.getFileExtensionFromUrl(file.name).lowercase()
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

                _uiState.update { it.copy(isOpeningFile = false, openingFileName = null) }
                _openFileEvent.emit(OpenFileEvent(cachedFile, mimeType))
            } catch (e: Exception) {
                val clean = ConnectionErrorFormatter.cleanTechnicalJargon(e.localizedMessage.orEmpty())
                _uiState.update {
                    it.copy(
                        isOpeningFile = false,
                        openingFileName = null,
                        errorMessage = "Falha ao abrir o arquivo: $clean"
                    )
                }
            }
        }
    }

    private fun resolveDefaultDownloadsDir(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return if (publicDownloads != null && (publicDownloads.exists() || publicDownloads.mkdirs()) && publicDownloads.canWrite()) {
            publicDownloads
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }
    }

    fun downloadFileToPhone(file: RemoteFile) {
        val server = _uiState.value.serverConfig ?: return
        val downloadsDir = resolveDefaultDownloadsDir()
        val destFile = File(downloadsDir, file.name)

        viewModelScope.launch {
            selectFileForAction(null)
            enqueueTransferUseCase(
                serverId = server.id,
                serverDisplayName = server.displayName,
                remotePath = file.fullPath,
                localPath = destFile.absolutePath,
                fileName = file.name,
                type = TransferType.DOWNLOAD,
                totalBytes = file.sizeBytes
            )
            _uiState.update {
                it.copy(infoMessage = "Download iniciado: ${file.name}")
            }
        }
    }

    fun downloadSelectedFilesToPhone() {
        val server = _uiState.value.serverConfig ?: return
        val selected = _uiState.value.selectedFiles
        if (selected.isEmpty()) return
        val downloadsDir = resolveDefaultDownloadsDir()

        viewModelScope.launch {
            selected.forEach { file ->
                val destFile = File(downloadsDir, file.name)
                enqueueTransferUseCase(
                    serverId = server.id,
                    serverDisplayName = server.displayName,
                    remotePath = file.fullPath,
                    localPath = destFile.absolutePath,
                    fileName = file.name,
                    type = TransferType.DOWNLOAD,
                    totalBytes = file.sizeBytes
                )
            }
            clearSelection()
            _uiState.update {
                it.copy(infoMessage = "${selected.size} arquivo(s) adicionado(s) para download!")
            }
        }
    }

    fun downloadSelectedFilesToTreeUri(treeUri: Uri) {
        val server = _uiState.value.serverConfig ?: return
        val selected = _uiState.value.selectedFiles
        if (selected.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            try {
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)

                selected.forEach { file ->
                    try {
                        val ext = MimeTypeMap.getFileExtensionFromUrl(file.name).lowercase()
                        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                        val destUri = DocumentsContract.createDocument(
                            context.contentResolver,
                            parentDocUri,
                            mimeType,
                            file.name
                        )
                        if (destUri != null) {
                            enqueueTransferUseCase(
                                serverId = server.id,
                                serverDisplayName = server.displayName,
                                remotePath = file.fullPath,
                                localPath = destUri.toString(),
                                fileName = file.name,
                                type = TransferType.DOWNLOAD,
                                totalBytes = file.sizeBytes
                            )
                            count++
                        }
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {}

            clearSelection()
            if (count > 0) {
                _uiState.update {
                    it.copy(infoMessage = "Download iniciado para $count arquivo(s)!")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Não foi possível gravar os arquivos na pasta escolhida.")
                }
            }
        }
    }

    fun downloadFileToUri(file: RemoteFile, destinationUriStr: String) {
        val server = _uiState.value.serverConfig ?: return

        viewModelScope.launch {
            selectFileForAction(null)
            enqueueTransferUseCase(
                serverId = server.id,
                serverDisplayName = server.displayName,
                remotePath = file.fullPath,
                localPath = destinationUriStr,
                fileName = file.name,
                type = TransferType.DOWNLOAD,
                totalBytes = file.sizeBytes
            )
            _uiState.update {
                it.copy(infoMessage = "Download iniciado: ${file.name}")
            }
        }
    }

    fun cancelTransfer(task: TransferTask) {
        viewModelScope.launch {
            task.workRequestId?.let { reqId ->
                try {
                    androidx.work.WorkManager.getInstance(context).cancelWorkById(java.util.UUID.fromString(reqId))
                } catch (e: Exception) {}
            }
            transferRepository.markCompleted(task.id, TransferStatus.CANCELLED, "Cancelado pelo usuário")
        }
    }

    fun uploadFilesFromUris(uris: List<Uri>, contentResolver: ContentResolver) {
        val server = _uiState.value.serverConfig ?: return
        val current = _uiState.value.currentPath
        val isSmb = server.protocol == ProtocolType.SMB
        val isRoot = current.trim().trim('/') == ""

        if (isSmb && isRoot) {
            _uiState.update {
                it.copy(
                    errorMessage = "Abra uma pasta compartilhada (ex: /testes) para enviar arquivos do telefone."
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            val stagingDir = File(context.cacheDir, "upload_staging")
            stagingDir.mkdirs()

            uris.forEach { uri ->
                var fileName = "arquivo_${System.currentTimeMillis()}"
                var fileSize = 0L

                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                            if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                        }
                    }

                    val stagingFile = File(stagingDir, fileName)
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(stagingFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val cleanRemoteDir = if (current.endsWith("/")) current else "$current/"
                    val remoteDest = "$cleanRemoteDir$fileName"

                    enqueueTransferUseCase(
                        serverId = server.id,
                        serverDisplayName = server.displayName,
                        remotePath = remoteDest,
                        localPath = stagingFile.absolutePath,
                        fileName = fileName,
                        type = TransferType.UPLOAD,
                        totalBytes = if (fileSize > 0L) fileSize else stagingFile.length()
                    )
                    count++
                } catch (e: Exception) {}
            }

            if (count > 0) {
                _uiState.update {
                    it.copy(infoMessage = "$count arquivo(s) adicionado(s) à fila de envio!")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            activeFileSystem?.close()
        } catch (e: Exception) {
            // Ignorado
        }
    }
}
