package com.sonarfiles.app.domain.usecase.explorer

import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.repository.RemoteFileSystem
import javax.inject.Inject

enum class SortMode {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC
}

class ListDirectoryUseCase @Inject constructor() {

    suspend operator fun invoke(
        remoteFileSystem: RemoteFileSystem,
        directoryPath: String,
        showHidden: Boolean = false,
        sortMode: SortMode = SortMode.NAME_ASC,
        searchQuery: String = ""
    ): Result<List<RemoteFile>> {
        return remoteFileSystem.listDirectory(directoryPath).map { files ->
            files
                // Filtro de ocultos
                .filter { showHidden || !it.isHidden }
                // Filtro de busca textual
                .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                // Ordenação: pastas sempre no topo, seguidas por arquivos com o sortMode escolhido
                .sortedWith(
                    compareBy<RemoteFile> { !it.isDirectory } // Pastas primeiro (false < true)
                        .then(
                            when (sortMode) {
                                SortMode.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                                SortMode.NAME_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name }
                                SortMode.DATE_DESC -> compareByDescending { it.lastModifiedEpochMillis }
                                SortMode.DATE_ASC -> compareBy { it.lastModifiedEpochMillis }
                                SortMode.SIZE_DESC -> compareByDescending { it.sizeBytes }
                                SortMode.SIZE_ASC -> compareBy { it.sizeBytes }
                            }
                        )
                )
        }
    }
}
