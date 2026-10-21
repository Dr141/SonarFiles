package com.sonarfiles.app.domain.usecase.scan

import com.sonarfiles.app.domain.model.DiscoveredHost
import com.sonarfiles.app.domain.repository.NetworkScannerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanLocalNetworkUseCase @Inject constructor(
    private val networkScannerRepository: NetworkScannerRepository
) {
    operator fun invoke(): Flow<DiscoveredHost> {
        return networkScannerRepository.scanSubnet()
    }
}
