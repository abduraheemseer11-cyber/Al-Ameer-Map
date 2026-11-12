package com.scannerwireless.app.domain.repository

import com.scannerwireless.app.domain.model.Device
import com.scannerwireless.app.domain.model.NetworkInfo
import com.scannerwireless.app.domain.model.ScanProgress
import com.scannerwireless.app.domain.model.SpeedProgress
import com.scannerwireless.app.domain.model.SpeedResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val lastDevices: StateFlow<List<Device>>
    fun currentNetwork(): NetworkInfo?
    fun scan(info: NetworkInfo): Flow<ScanProgress>
}

interface SpeedTestRepository {
    val last: StateFlow<SpeedResult?>
    fun run(): Flow<SpeedProgress>
}
