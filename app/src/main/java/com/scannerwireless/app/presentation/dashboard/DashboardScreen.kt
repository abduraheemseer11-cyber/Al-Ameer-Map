package com.scannerwireless.app.presentation.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scannerwireless.app.R
import com.scannerwireless.app.presentation.components.CircularScanner
import com.scannerwireless.app.presentation.components.GlassCard
import com.scannerwireless.app.presentation.components.InfoRow
import com.scannerwireless.app.presentation.theme.MutedText
import com.scannerwireless.app.presentation.theme.Teal
import com.scannerwireless.app.presentation.theme.WarningOrange

@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refreshNetwork() }

    LaunchedEffect(Unit) {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        launcher.launch(perms.toTypedArray())
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.app_name_en),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(stringResource(R.string.nav_dashboard), color = MutedText, fontSize = 13.sp)
        }

        CircularScanner(
            count = state.devices.size,
            scanning = state.scanning,
            progress = state.progress,
            modifier = Modifier.size(250.dp)
        )

        Button(
            onClick = vm::scan,
            enabled = !state.scanning,
            modifier = Modifier.fillMaxWidth(0.6f).height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text(
                stringResource(if (state.scanning) R.string.scanning else R.string.scan),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Network status
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.network_status),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            val net = state.network
            if (net == null) {
                Text(
                    stringResource(R.string.not_connected),
                    color = WarningOrange,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                if (net.ssid.isNotEmpty()) InfoRow(stringResource(R.string.network_name), net.ssid)
                InfoRow(stringResource(R.string.ip_address), net.localIp)
                InfoRow(stringResource(R.string.gateway), net.gateway.ifEmpty { "-" })
                if (net.linkSpeedMbps > 0) {
                    InfoRow(stringResource(R.string.link_speed), "${net.linkSpeedMbps} Mbps")
                }
                InfoRow(stringResource(R.string.signal_strength), "${net.signalPercent}%")
            }
        }

        // Speed
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.speed_download_upload),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            val speed = state.speed
            if (speed == null) {
                Text(
                    stringResource(R.string.no_speed_yet),
                    color = MutedText,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Metric(stringResource(R.string.download), "%.1f".format(speed.downloadMbps), "Mbps")
                    Metric(stringResource(R.string.upload), "%.1f".format(speed.uploadMbps), "Mbps")
                    Metric(stringResource(R.string.latency), speed.latencyMs.toString(), "ms")
                }
            }
        }

        // Alerts
        val hasAlert = state.newCount > 0
        GlassCard(
            Modifier.fillMaxWidth(),
            borderColor = if (hasAlert) WarningOrange else Teal.copy(alpha = 0.4f)
        ) {
            Text(
                stringResource(R.string.security_alerts),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (hasAlert) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (hasAlert) WarningOrange else Teal
                )
                Text(
                    if (hasAlert) stringResource(R.string.new_devices_alert, state.newCount)
                    else stringResource(R.string.no_alerts)
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedText, fontSize = 12.sp)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Teal)
        Text(unit, color = MutedText, fontSize = 12.sp)
    }
}
