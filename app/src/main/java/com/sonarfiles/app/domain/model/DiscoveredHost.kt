package com.sonarfiles.app.domain.model

data class DiscoveredHost(
    val ipAddress: String,
    val hostname: String? = null,
    val supportedProtocols: Set<ProtocolType>,
    val openPorts: Set<Int>,
    val osHint: OsHint = OsHint.UNKNOWN,
    val macAddress: String? = null,
    val responseTimeMs: Long
)
