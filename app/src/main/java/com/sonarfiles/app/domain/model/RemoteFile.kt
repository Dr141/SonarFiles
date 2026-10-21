package com.sonarfiles.app.domain.model

data class RemoteFile(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
    val mimeType: String? = null,
    val isHidden: Boolean = false,
    val permissionsString: String? = null
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()
}
