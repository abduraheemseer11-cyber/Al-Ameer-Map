package com.scannerwireless.app.data.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android 10+ blocks apps from reading the ARP table of other devices.
 * We try /proc/net/arp, then "ip neigh". Both usually return nothing on
 * non-rooted modern devices, in which case MAC addresses stay null and the
 * UI shows an explanation.
 */
@Singleton
class MacResolver @Inject constructor() {
    private val ipRegex = Regex("""\d{1,3}(\.\d{1,3}){3}""")
    private val macRegex = Regex("""([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}""")

    suspend fun readArp(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        try {
            File("/proc/net/arp").forEachLine { line ->
                val p = line.trim().split(Regex("\\s+"))
                if (p.size >= 4 && ipRegex.matches(p[0]) && macRegex.matches(p[3]) &&
                    p[3] != "00:00:00:00:00:00"
                ) {
                    map[p[0]] = p[3].uppercase()
                }
            }
        } catch (_: Exception) {
        }
        if (map.isEmpty()) {
            try {
                val proc = ProcessBuilder("ip", "neigh").redirectErrorStream(true).start()
                val lines = proc.inputStream.bufferedReader().readLines()
                proc.waitFor()
                for (line in lines) {
                    val parts = line.trim().split(Regex("\\s+"))
                    val i = parts.indexOf("lladdr")
                    if (i >= 0 && i + 1 < parts.size && ipRegex.matches(parts[0]) &&
                        macRegex.matches(parts[i + 1])
                    ) {
                        map[parts[0]] = parts[i + 1].uppercase()
                    }
                }
            } catch (_: Exception) {
            }
        }
        map
    }
}
