package com.scannerwireless.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerwireless.app.domain.model.Device
import com.scannerwireless.app.domain.model.NetworkInfo
import com.scannerwireless.app.domain.model.SpeedResult
import com.scannerwireless.app.domain.repository.DeviceRepository
import com.scannerwireless.app.domain.repository.SpeedTestRepository
import com.scannerwireless.app.domain.usecase.ScanNetworkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val network: NetworkInfo? = null,
    val devices: List<Device> = emptyList(),
    val scanning: Boolean = false,
    val progress: Float = 0f,
    val speed: SpeedResult? = null
) {
    val newCount: Int get() = devices.count { it.isNew }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scanUseCase: ScanNetworkUseCase,
    private val repo: DeviceRepository,
    speedRepo: SpeedTestRepository
) : ViewModel() {

    private val local = MutableStateFlow(DashboardUiState(network = repo.currentNetwork()))

    val state: StateFlow<DashboardUiState> =
        combine(local, repo.lastDevices, speedRepo.last) { s, devices, speed ->
            s.copy(devices = devices, speed = speed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), local.value)

    fun refreshNetwork() {
        local.update { it.copy(network = repo.currentNetwork()) }
    }

    fun scan() {
        if (local.value.scanning) return
        val net = repo.currentNetwork()
        local.update { it.copy(network = net) }
        if (net == null) return
        local.update { it.copy(scanning = true, progress = 0f) }
        viewModelScope.launch {
            try {
                scanUseCase(net).collect { p -> local.update { it.copy(progress = p.progress) } }
            } finally {
                local.update { it.copy(scanning = false, progress = 1f) }
            }
        }
    }
}
