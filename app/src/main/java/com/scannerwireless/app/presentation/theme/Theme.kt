package com.scannerwireless.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val DeepBlue = Color(0xFF0A1628)
val NavyBlue = Color(0xFF12284C)
val Cyan = Color(0xFF00E5FF)
val Teal = Color(0xFF1DE9B6)
val WarningOrange = Color(0xFFFF9800)
val MutedText = Color(0xFF8FA6C4)

private val DarkScheme = darkColorScheme(
    primary = Cyan,
    secondary = Teal,
    tertiary = WarningOrange,
    background = DeepBlue,
    surface = NavyBlue,
    onPrimary = DeepBlue,
    onBackground = Color(0xFFE6F1FF),
    onSurface = Color(0xFFE6F1FF)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006C80),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFE65100)
)

@Composable
fun ScannerWirelessTheme(
    darkTheme: Boolean = true,
    forceRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val direction = if (forceRtl) LayoutDirection.Rtl else LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content
        )
    }
}
