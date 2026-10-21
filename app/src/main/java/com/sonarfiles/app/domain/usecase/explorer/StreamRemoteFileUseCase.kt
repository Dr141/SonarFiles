package com.sonarfiles.app.domain.usecase.explorer

import com.sonarfiles.app.domain.repository.RemoteFileSystem
import java.io.InputStream
import javax.inject.Inject

class StreamRemoteFileUseCase @Inject constructor() {

    suspend operator fun invoke(
        remoteFileSystem: RemoteFileSystem,
        remotePath: String,
        offsetBytes: Long = 0L
    ): Result<InputStream> {
        return remoteFileSystem.openReadStream(remotePath, offsetBytes)
    }
}
