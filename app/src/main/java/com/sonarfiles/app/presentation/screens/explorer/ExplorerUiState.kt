package com.sonarfiles.app.presentation.screens.explorer

import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.domain.usecase.explorer.SortMode

data class ExplorerUiState(
    val serverConfig: ServerConfig? = null,
    val currentPath: String = "/",
    val files: List<RemoteFile> = emptyList(),
    val isLoading: Boolean = true,
    val isAuthenticating: Boolean = false,
    val isGridView: Boolean = false,
    val showHidden: Boolean = false,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedFileForAction: RemoteFile? = null,
    val isCreateFolderDialogOpen: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val isConfirmDeleteDialogOpen: Boolean = false,
    val isOpeningFile: Boolean = false,
    val openingFileName: String? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && files.isEmpty()
}
