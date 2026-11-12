package com.scannerwireless.app.data.repository

import com.scannerwireless.app.data.local.DeviceDao
import com.scannerwireless.app.data.local.DeviceEntity
import com.scannerwireless.app.data.scanner.NetworkInfoProvider
import com.scannerwireless.app.data.scanner.NetworkScanner
import com.scannerwireless.app.data.scanner.SecurityNotifier
import com.scannerwireless.app.domain.model.Device
import com.scannerwireless.app.domain.model.NetworkInfo
import com.scannerwireless.app.domain.model.ScanProgress
import com.scannerwireless.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val scanner: NetworkScanner,
    private val provider: NetworkInfoProvider,
    private val dao: DeviceDao,
    private val notifier: SecurityNotifier
) : DeviceRepository {

    private val _last = MutableStateFlow<List<Device>>(emptyList())
    override val lastDevices: StateFlow<List<Device>> = _last.asStateFlow()

    override fun currentNetwork(): NetworkInfo? = provider.get()

    override fun scan(info: NetworkInfo): Flow<ScanProgress> =
        scanner.scan(info).transform { p ->
            val devices = p.devices
            if (p.done && devices != null) {
                val marked = markNew(devices)
                _last.value = marked
                emit(p.copy(devices = marked))
            } else {
                emit(p)
            }
        }

    private suspend fun markNew(devices: List<Device>): List<Device> {
        val known = dao.getAll().associateBy { it.key }
        val now = System.currentTimeMillis()
        val result = devices.map { d ->
            d.copy(isNew = known.isNotEmpty() && d.key !in known && !d.isSelf)
        }
        dao.upsertAll(result.map { d ->
            DeviceEntity(
                key = d.key, ip = d.ip, mac = d.mac, name = d.hostName, vendor = d.vendor,
                firstSeen = known[d.key]?.firstSeen ?: now, lastSeen = now
            )
        })
        val newCount = result.count { it.isNew }
        if (newCount > 0) notifier.notifyNewDevices(newCount)
        return result
    }
}
