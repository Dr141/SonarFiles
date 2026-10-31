package com.sonarfiles.app.presentation.screens.home

import com.sonarfiles.app.domain.model.ServerConfig

data class HomeUiState(
    val isLoading: Boolean = true,
    val servers: List<ServerConfig> = emptyList(),
    val favoriteServers: List<ServerConfig> = emptyList(),
    val isWifiConnected: Boolean = true,
    val isGridView: Boolean = false,
    val serverReachability: Map<Long, Boolean?> = emptyMap(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && servers.isEmpty()
}
