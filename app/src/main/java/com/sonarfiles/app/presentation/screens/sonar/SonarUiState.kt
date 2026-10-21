package com.sonarfiles.app.presentation.screens.sonar

import com.sonarfiles.app.domain.model.DiscoveredHost

data class SonarUiState(
    val isScanning: Boolean = false,
    val localIp: String? = null,
    val subnetPrefix: String? = null,
    val hosts: List<DiscoveredHost> = emptyList(),
    val errorMessage: String? = null
) {
    val totalFound: Int
        get() = hosts.size
}
