package com.sonarfiles.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val id: Long = 0L,
    val displayName: String,
    val hostOrIp: String,
    val port: Int,
    val protocol: ProtocolType,
    val defaultShareOrPath: String = "/",
    val domainOrWorkgroup: String? = null,
    val macAddress: String? = null,
    val requiresBiometrics: Boolean = true,
    val isFavorite: Boolean = false,
    val lastConnectedAt: Long? = null
)
