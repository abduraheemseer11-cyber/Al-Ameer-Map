package com.scannerwireless.app.data.scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.scannerwireless.app.domain.model.NetworkInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Suppress("DEPRECATION")
    fun get(): NetworkInfo? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(network) ?: return null
        val isLan = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        if (!isLan) return null

        val lp = cm.getLinkProperties(network) ?: return null
        val addr = lp.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return null
        val gateway = lp.routes
            .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress.orEmpty()

        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val wi = wifi?.connectionInfo
        val rawSsid = wi?.ssid.orEmpty().trim('"')
        val ssid = if (rawSsid == "<unknown ssid>") "" else rawSsid
        val rssi = wi?.rssi ?: -100
        val percent = ((rssi + 100) * 2).coerceIn(0, 100)

        return NetworkInfo(
            ssid = ssid,
            localIp = addr.address.hostAddress.orEmpty(),
            gateway = gateway,
            prefixLength = addr.prefixLength,
            linkSpeedMbps = (wi?.linkSpeed ?: 0).coerceAtLeast(0),
            signalPercent = percent
        )
    }
}
