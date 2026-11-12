package com.scannerwireless.app.presentation.speedtest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scannerwireless.app.R
import com.scannerwireless.app.domain.model.SpeedPhase
import com.scannerwireless.app.domain.model.SpeedProgress
import com.scannerwireless.app.domain.usecase.RunSpeedTestUseCase
import com.scannerwireless.app.presentation.components.GlassCard
import com.scannerwireless.app.presentation.components.ScreenTitle
import com.scannerwireless.app.presentation.theme.Cyan
import com.scannerwireless.app.presentation.theme.MutedText
import com.scannerwireless.app.presentation.theme.Teal
import com.scannerwireless.app.presentation.theme.WarningOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ln

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val runTest: RunSpeedTestUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SpeedProgress())
    val state: StateFlow<SpeedProgress> = _state.asStateFlow()
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = viewModelScope.launch { runTest().collect { _state.value = it } }
    }
}

@Composable
fun SpeedTestScreen(vm: SpeedTestViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val running = s.phase == SpeedPhase.LATENCY || s.phase == SpeedPhase.DOWNLOAD || s.phase == SpeedPhase.UPLOAD

    val shown = when (s.phase) {
        SpeedPhase.DOWNLOAD, SpeedPhase.UPLOAD -> s.liveMbps
        SpeedPhase.DONE -> s.downloadMbps
        else -> 0.0
    }

    Column(
        Modifier.fillMaxSize().padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenTitle(stringResource(R.string.nav_speed_test))

        Gauge(value = shown, modifier = Modifier.size(260.dp).padding(8.dp))

        Text(
            text = when (s.phase) {
                SpeedPhase.LATENCY -> stringResource(R.string.speed_testing_latency)
                SpeedPhase.DOWNLOAD -> stringResource(R.string.speed_testing_download)
                SpeedPhase.UPLOAD -> stringResource(R.string.speed_testing_upload)
                SpeedPhase.DONE -> stringResource(R.string.speed_done)
                SpeedPhase.ERROR -> stringResource(R.string.speed_error)
                SpeedPhase.IDLE -> ""
            },
            color = if (s.phase == SpeedPhase.ERROR) WarningOrange else MutedText,
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = vm::start,
            enabled = !running,
            modifier = Modifier.fillMaxWidth(0.6f).height(52.dp).padding(top = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text(stringResource(R.string.speed_start), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        GlassCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(stringResource(R.string.download), if (s.downloadMbps > 0) "%.1f".format(s.downloadMbps) else "-", "Mbps")
                Stat(stringResource(R.string.upload), if (s.uploadMbps > 0) "%.1f".format(s.uploadMbps) else "-", "Mbps")
                Stat(stringResource(R.string.latency), if (s.latencyMs >= 0) s.latencyMs.toString() else "-", "ms")
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedText, fontSize = 12.sp)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Teal)
        Text(unit, color = MutedText, fontSize = 12.sp)
    }
}

@Composable
private fun Gauge(value: Double, modifier: Modifier = Modifier) {
    // Logarithmic scale so both 5 Mbps and 500 Mbps look sensible.
    val target = (ln(1 + value) / ln(1001.0)).toFloat().coerceIn(0f, 1f)
    val fraction by animateFloatAsState(target, label = "gauge")

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = Color(0x2200E5FF), startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(listOf(Teal, Cyan)),
                startAngle = 135f, sweepAngle = 270f * fraction, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%.1f".format(value), fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text("Mbps", color = MutedText)
        }
    }
}
