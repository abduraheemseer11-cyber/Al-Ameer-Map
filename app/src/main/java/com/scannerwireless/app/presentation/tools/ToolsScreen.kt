package com.scannerwireless.app.presentation.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scannerwireless.app.R
import com.scannerwireless.app.presentation.components.GlassCard
import com.scannerwireless.app.presentation.components.ScreenTitle
import com.scannerwireless.app.presentation.theme.MutedText

private fun Tool.icon() = when (this) {
    Tool.PING -> Icons.Filled.NetworkCheck
    Tool.TRACEROUTE -> Icons.Filled.Timeline
    Tool.DNS -> Icons.Filled.Dns
    Tool.PORT_SCAN -> Icons.Filled.Security
    Tool.WOL -> Icons.Filled.PowerSettingsNew
    Tool.CAMERAS -> Icons.Filled.Videocam
}

@Composable
fun ToolsScreen(vm: ToolsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val tool = selected?.let { Tool.entries[it] }

    BackHandler(enabled = tool != null) {
        vm.clear()
        selected = null
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(stringResource(tool?.titleRes ?: R.string.nav_tools))
        if (tool == null) {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Tool.entries.toList()) { t ->
                    GlassCard(Modifier.fillMaxWidth().clickable {
                        vm.clear()
                        selected = t.ordinal
                    }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(t.icon(), null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(t.titleRes), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        } else {
            ToolDetail(tool, state, onRun = vm::run, onClear = vm::clear)
        }
    }
}

@Composable
private fun ToolDetail(
    tool: Tool,
    state: ToolsUiState,
    onRun: (Tool, String) -> Unit,
    onClear: () -> Unit
) {
    var input by rememberSaveable(tool) { mutableStateOf(tool.defaultInput) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (tool.needsInput) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(tool.hintRes)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(stringResource(tool.hintRes), color = MutedText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onRun(tool, input) },
                enabled = !state.running && (!tool.needsInput || input.isNotBlank())
            ) { Text(stringResource(R.string.run)) }
            OutlinedButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
        }
        GlassCard(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.output) { line ->
                        Text(line, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
