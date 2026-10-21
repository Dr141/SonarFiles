package com.sonarfiles.app.presentation.navigation

import com.sonarfiles.app.domain.model.ProtocolType
import kotlinx.serialization.Serializable

sealed interface ScreenRoutes {

    @Serializable
    data object Home : ScreenRoutes

    @Serializable
    data object SonarScan : ScreenRoutes

    @Serializable
    data class ServerForm(
        val serverId: Long? = null,
        val initialHost: String? = null,
        val initialProtocol: ProtocolType? = null,
        val initialHostname: String? = null
    ) : ScreenRoutes

    @Serializable
    data class Explorer(val serverId: Long, val initialPath: String = "/") : ScreenRoutes

    @Serializable
    data object Transfers : ScreenRoutes
}
