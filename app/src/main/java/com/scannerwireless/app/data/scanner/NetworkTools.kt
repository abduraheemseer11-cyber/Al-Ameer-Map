package com.scannerwireless.app.data.scanner

import com.scannerwireless.app.domain.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkTools @Inject constructor() {

    private val commonPorts = mapOf(
        21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP", 53 to "DNS",
        80 to "HTTP", 110 to "POP3", 139 to "NetBIOS", 143 to "IMAP", 443 to "HTTPS",
        445 to "SMB", 554 to "RTSP", 1900 to "UPnP", 3389 to "RDP", 5900 to "VNC",
        8000 to "HTTP-Alt", 8080 to "HTTP-Proxy", 8443 to "HTTPS-Alt",
        8554 to "RTSP-Alt", 37777 to "Dahua"
    )
    private val riskyPorts = setOf(21, 23, 139, 445, 3389, 5900)
    private val cameraPorts = listOf(554, 8554, 37777, 34567)
    private val cameraVendors = listOf(
        "hikvision", "dahua", "reolink", "ezviz", "xiongmai", "foscam",
        "amcrest", "axis comm", "wyze", "tapo", "imou", "uniview", "vivotek"
    )

    fun ping(host: String, count: Int = 4): Flow<String> = flow {
        val addr = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            emit("Cannot resolve host: $host")
            return@flow
        }
        emit("PING $host (${addr.hostAddress})")
        var ok = 0
        val times = mutableListOf<Long>()
        for (i in 1..count) {
            val t0 = System.nanoTime()
            val reachable = try { addr.isReachable(1500) } catch (_: Exception) { false }
            val ms = (System.nanoTime() - t0) / 1_000_000
            if (reachable) {
                ok++
                times.add(ms)
                emit("reply from ${addr.hostAddress}: seq=$i time=$ms ms")
            } else {
                emit("seq=$i request timed out")
            }
            delay(500)
        }
        val avg = if (times.isEmpty()) 0 else times.sum() / times.size
        emit("--- $ok/$count received, avg $avg ms")
    }.flowOn(Dispatchers.IO)

    fun traceroute(host: String): Flow<String> = flow {
        val addr = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            emit("Cannot resolve host: $host")
            return@flow
        }
        val target = addr.hostAddress.orEmpty()
        emit("traceroute to $host ($target), max 20 hops")
        val ipRegex = Regex("(?i)from\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)")
        for (ttl in 1..20) {
            val out = pingOnce(target, ttl)
            val hop = ipRegex.find(out)?.groupValues?.get(1)
            emit(if (hop == null) "$ttl  * * *" else "$ttl  $hop")
            if (out.contains("bytes from", ignoreCase = true)) break
        }
        emit("done")
    }.flowOn(Dispatchers.IO)

    private fun pingOnce(ip: String, ttl: Int): String = try {
        val p = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "2", "-t", ttl.toString(), ip)
            .redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText()
        p.waitFor()
        text
    } catch (_: Exception) {
        ""
    }

    fun dnsLookup(host: String): Flow<String> = flow {
        try {
            val all = InetAddress.getAllByName(host)
            emit("Query: $host")
            all.forEach { emit("  ${if (it.address.size == 4) "A   " else "AAAA"}  ${it.hostAddress}") }
            if (host.matches(Regex("[0-9.]+"))) emit("PTR  ${all.first().canonicalHostName}")
        } catch (e: Exception) {
            emit("Lookup failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    fun portScan(host: String): Flow<String> = flow {
        emit("Scanning $host ...")
        val open = coroutineScope {
            commonPorts.map { (port, name) ->
                async(Dispatchers.IO) { if (isOpen(host, port, 600)) port to name else null }
            }.awaitAll().filterNotNull().sortedBy { it.first }
        }
        if (open.isEmpty()) emit("No common open ports found.")
        open.forEach { (port, name) ->
            emit("OPEN  $port  $name${if (port in riskyPorts) "  [!] risky if exposed" else ""}")
        }
        emit("done")
    }

    suspend fun wakeOnLan(mac: String, broadcast: String = "255.255.255.255"): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bytes = mac.split(":", "-").map { it.toInt(16).toByte() }.toByteArray()
                require(bytes.size == 6)
                val packet = ByteArray(6) { 0xFF.toByte() } + ByteArray(96) { bytes[it % 6] }
                DatagramSocket().use { s ->
                    s.broadcast = true
                    s.send(DatagramPacket(packet, packet.size, InetAddress.getByName(broadcast), 9))
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    fun detectCameras(devices: List<Device>): Flow<String> = flow {
        if (devices.isEmpty()) {
            emit("Run a network scan first (Dashboard).")
            return@flow
        }
        emit("Checking ${devices.size} device(s) ...")
        var found = 0
        for (d in devices.filter { !it.isSelf }) {
            val vendorHit = d.vendor?.let { v -> cameraVendors.any { v.contains(it, ignoreCase = true) } } == true
            val openPorts = coroutineScope {
                cameraPorts.map { p -> async(Dispatchers.IO) { if (isOpen(d.ip, p, 500)) p else null } }
                    .awaitAll().filterNotNull()
            }
            if (vendorHit || openPorts.isNotEmpty()) {
                found++
                emit("[!] ${d.ip}  vendor=${d.vendor ?: "?"}  ports=$openPorts")
            }
        }
        emit(if (found == 0) "No suspicious camera devices found." else "$found suspicious device(s).")
    }

    private fun isOpen(host: String, port: Int, timeout: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), timeout) }
        true
    } catch (_: Exception) {
        false
    }
}
