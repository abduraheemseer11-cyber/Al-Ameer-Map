package com.scannerwireless.app.domain.model

data class NetworkInfo(
    val ssid: String,
    val localIp: String,
    val gateway: String,
    val prefixLength: Int,
    val linkSpeedMbps: Int,
    val signalPercent: Int
)

data class Device(
    val ip: String,
    val mac: String? = null,
    val hostName: String? = null,
    val vendor: String? = null,
    val isNew: Boolean = false,
    val isGateway: Boolean = false,
    val isSelf: Boolean = false
) {
    val key: String get() = mac ?: ip

    /** Locally-administered bit set => randomized (private) MAC address. */
    val isRandomMac: Boolean
        get() = mac?.take(2)?.toIntOrNull(16)?.let { (it and 2) != 0 } ?: false
}

data class ScanProgress(
    val progress: Float,
    val devices: List<Device>? = null,
    val done: Boolean = false
)

data class SpeedResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Long
)

enum class SpeedPhase { IDLE, LATENCY, DOWNLOAD, UPLOAD, DONE, ERROR }

data class SpeedProgress(
    val phase: SpeedPhase = SpeedPhase.IDLE,
    val liveMbps: Double = 0.0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val latencyMs: Long = -1
)
