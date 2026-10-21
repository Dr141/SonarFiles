package com.sonarfiles.app.domain.repository

import com.sonarfiles.app.domain.model.DiscoveredHost
import kotlinx.coroutines.flow.Flow

interface NetworkScannerRepository {
    fun scanSubnet(): Flow<DiscoveredHost>
    suspend fun sendWakeOnLan(macAddress: String, broadcastIp: String = "255.255.255.255"): Result<Unit>
}
