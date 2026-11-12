package com.scannerwireless.app.presentation.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scannerwireless.app.R
import com.scannerwireless.app.domain.model.Device
import com.scannerwireless.app.domain.repository.DeviceRepository
import com.scannerwireless.app.presentation.components.GlassCard
import com.scannerwireless.app.presentation.components.ScreenTitle
import com.scannerwireless.app.presentation.theme.MutedText
import com.scannerwireless.app.presentation.theme.WarningOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(repo: DeviceRepository) : ViewModel() {
    val devices = repo.lastDevices
}

@Composable
fun DevicesScreen(vm: DevicesViewModel = hiltViewModel()) {
    val devices by vm.devices.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.nav_devices))
        if (devices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.devices_empty), color = MutedText)
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices, key = { it.key }) { DeviceCard(it) }
            }
        }
    }
}

@Composable
private fun DeviceCard(d: Device) {
    GlassCard(
        Modifier.fillMaxWidth(),
        borderColor = if (d.isNew) WarningOrange else androidx.compose.ui.graphics.Color(0x3300E5FF)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = when {
                    d.isGateway -> Icons.Filled.Router
                    d.isSelf -> Icons.Filled.PhoneAndroid
                    else -> Icons.Filled.Devices
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    d.hostName ?: d.vendor ?: d.ip,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(d.ip, color = MutedText, fontSize = 13.sp)
                Text(
                    when {
                        d.mac == null -> stringResource(R.string.mac_unavailable)
                        d.isRandomMac -> d.mac + " · " + stringResource(R.string.mac_random)
                        else -> d.mac + " · " + (d.vendor ?: stringResource(R.string.vendor_unknown))
                    },
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (d.isGateway) Text(stringResource(R.string.router_label), color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                if (d.isSelf) Text(stringResource(R.string.this_device), color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                if (d.isNew) Text(stringResource(R.string.new_badge), color = WarningOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
