package com.scannerwireless.app.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scannerwireless.app.R
import com.scannerwireless.app.presentation.theme.Cyan
import com.scannerwireless.app.presentation.theme.MutedText
import com.scannerwireless.app.presentation.theme.Teal

/** Glassmorphism card: translucent gradient + thin cyan border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0x3300E5FF),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))))
            .border(1.dp, borderColor, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MutedText)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/** Large animated circle showing the number of connected devices. */
@Composable
fun CircularScanner(
    count: Int,
    scanning: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "scanner")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.9f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )
    val animProgress by animateFloatAsState(progress, label = "progress")

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val arcSize = Size(size.width - 2 * stroke, size.height - 2 * stroke)
            val topLeft = Offset(stroke, stroke)

            if (scanning) {
                drawCircle(Cyan.copy(alpha = 0.10f), radius = size.minDimension / 2 * pulse)
            }
            drawArc(
                color = Color(0x2200E5FF), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke)
            )
            rotate(-90f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Teal, Cyan, Teal)),
                    startAngle = 0f,
                    sweepAngle = if (scanning) 360f * animProgress.coerceAtLeast(0.03f) else 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            if (scanning) {
                rotate(rotation) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.5f), startAngle = -5f, sweepAngle = 10f,
                        useCenter = false, topLeft = Offset(stroke * 2.2f, stroke * 2.2f),
                        size = Size(size.width - 4.4f * stroke, size.height - 4.4f * stroke),
                        style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.connected_devices),
                color = MutedText,
                fontSize = 14.sp
            )
        }
    }
}
