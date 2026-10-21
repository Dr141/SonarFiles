package com.sonarfiles.app.domain.usecase.explorer

import com.sonarfiles.app.domain.repository.RemoteFileSystem
import javax.inject.Inject

class ManageRemoteFileUseCase @Inject constructor() {

    suspend fun createDirectory(
        remoteFileSystem: RemoteFileSystem,
        directoryPath: String
    ): Result<Unit> {
        return remoteFileSystem.createDirectory(directoryPath)
    }

    suspend fun rename(
        remoteFileSystem: RemoteFileSystem,
        sourcePath: String,
        destinationPath: String
    ): Result<Unit> {
        return remoteFileSystem.rename(sourcePath, destinationPath)
    }

    suspend fun delete(
        remoteFileSystem: RemoteFileSystem,
        remotePath: String,
        recursive: Boolean = true
    ): Result<Unit> {
        return remoteFileSystem.delete(remotePath, recursive)
    }
}
