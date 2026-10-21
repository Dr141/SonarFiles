package com.sonarfiles.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProtocolType(val defaultPort: Int, val label: String) {
    SMB(defaultPort = 445, label = "SMB (Windows / Samba)"),
    SFTP(defaultPort = 22, label = "SFTP (Linux / SSH)")
}

enum class OsHint {
    WINDOWS,
    LINUX,
    MACOS,
    NAS,
    UNKNOWN
}
