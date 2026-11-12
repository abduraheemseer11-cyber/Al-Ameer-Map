package com.scannerwireless.app.domain.usecase

import com.scannerwireless.app.domain.model.NetworkInfo
import com.scannerwireless.app.domain.repository.DeviceRepository
import com.scannerwireless.app.domain.repository.SpeedTestRepository
import javax.inject.Inject

class ScanNetworkUseCase @Inject constructor(private val repo: DeviceRepository) {
    operator fun invoke(info: NetworkInfo) = repo.scan(info)
}

class RunSpeedTestUseCase @Inject constructor(private val repo: SpeedTestRepository) {
    operator fun invoke() = repo.run()
}
