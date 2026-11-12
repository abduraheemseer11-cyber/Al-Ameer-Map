package com.scannerwireless.app.presentation.tools

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerwireless.app.R
import com.scannerwireless.app.data.scanner.NetworkTools
import com.scannerwireless.app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Tool(
    @StringRes val titleRes: Int,
    @StringRes val hintRes: Int,
    val needsInput: Boolean,
    val defaultInput: String
) {
    PING(R.string.tool_ping, R.string.hint_host, true, "8.8.8.8"),
    TRACEROUTE(R.string.tool_traceroute, R.string.hint_host, true, "8.8.8.8"),
    DNS(R.string.tool_dns, R.string.hint_host, true, "google.com"),
    PORT_SCAN(R.string.tool_port_scan, R.string.hint_host, true, ""),
    WOL(R.string.tool_wol, R.string.hint_mac, true, ""),
    CAMERAS(R.string.tool_camera_detect, R.string.hint_cameras, false, "")
}

data class ToolsUiState(val output: List<String> = emptyList(), val running: Boolean = false)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val tools: NetworkTools,
    private val repo: DeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ToolsUiState())
    val state: StateFlow<ToolsUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun run(tool: Tool, input: String) {
        job?.cancel()
        val text = input.trim()
        _state.value = ToolsUiState(running = true)
        val source: Flow<String> = when (tool) {
            Tool.PING -> tools.ping(text)
            Tool.TRACEROUTE -> tools.traceroute(text)
            Tool.DNS -> tools.dnsLookup(text)
            Tool.PORT_SCAN -> tools.portScan(text)
            Tool.WOL -> flow {
                emit(if (tools.wakeOnLan(text)) "Magic packet sent to $text" else "Invalid MAC or send failed")
            }
            Tool.CAMERAS -> tools.detectCameras(repo.lastDevices.value)
        }
        job = viewModelScope.launch {
            source
                .catch { e -> emit("Error: ${e.message}") }
                .onCompletion { _state.update { it.copy(running = false) } }
                .collect { line -> _state.update { it.copy(output = it.output + line) } }
        }
    }

    fun clear() {
        job?.cancel()
        _state.value = ToolsUiState()
    }
}
