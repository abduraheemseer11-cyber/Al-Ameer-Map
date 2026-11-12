package com.scannerwireless.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scannerwireless.app.R
import com.scannerwireless.app.presentation.components.GlassCard
import com.scannerwireless.app.presentation.components.ScreenTitle
import com.scannerwireless.app.presentation.theme.MutedText
import com.scannerwireless.app.presentation.theme.WarningOrange

@Composable
fun SettingsScreen() {
    val premium = listOf(
        R.string.premium_presence, R.string.premium_block, R.string.premium_parental,
        R.string.premium_bandwidth, R.string.premium_auto_speed, R.string.premium_hotspot
    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenTitle(stringResource(R.string.nav_settings))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_about), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.app_name_en), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.settings_version), color = MutedText)
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_premium), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                premium.forEach { res ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(res), modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.coming_soon), color = WarningOrange, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
