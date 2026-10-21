package com.sonarfiles.app.presentation.screens.home

import com.sonarfiles.app.domain.model.ServerConfig

data class HomeUiState(
    val isLoading: Boolean = true,
    val servers: List<ServerConfig> = emptyList(),
    val favoriteServers: List<ServerConfig> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && servers.isEmpty()
}
