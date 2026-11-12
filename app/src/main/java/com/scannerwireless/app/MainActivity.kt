package com.scannerwireless.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.scannerwireless.app.presentation.navigation.AppNavHost
import com.scannerwireless.app.presentation.theme.ScannerWirelessTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScannerWirelessTheme(darkTheme = true, forceRtl = true) {
                AppNavHost()
            }
        }
    }
}
