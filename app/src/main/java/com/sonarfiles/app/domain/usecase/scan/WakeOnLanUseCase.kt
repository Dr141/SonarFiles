package com.sonarfiles.app.domain.usecase.scan

import com.sonarfiles.app.domain.repository.NetworkScannerRepository
import javax.inject.Inject

class WakeOnLanUseCase @Inject constructor(
    private val networkScannerRepository: NetworkScannerRepository
) {
    suspend operator fun invoke(macAddress: String): Result<Unit> {
        return networkScannerRepository.sendWakeOnLan(macAddress)
    }
}
