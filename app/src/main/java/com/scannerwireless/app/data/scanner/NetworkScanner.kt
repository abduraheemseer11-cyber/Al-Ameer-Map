package com.scannerwireless.app.data.scanner

import android.os.Build
import com.scannerwireless.app.data.remote.OuiApi
import com.scannerwireless.app.domain.model.Device
import com.scannerwireless.app.domain.model.NetworkInfo
import com.scannerwireless.app.domain.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import kotlin.coroutines.resume

@Singleton
class NetworkScanner @Inject constructor(
    private val macResolver: MacResolver,
    private val ouiApi: OuiApi
) {
    private val probePorts = intArrayOf(80, 443, 445, 22, 62078, 8080)

    fun scan(info: NetworkInfo): Flow<ScanProgress> = channelFlow {
        val hosts = hostsOf(info.localIp, info.prefixLength).filter { it != info.localIp }
        val alive = Collections.synchronizedList(mutableListOf<String>())
        val done = AtomicInteger(0)
        val semaphore = Semaphore(64)

        coroutineScope {
            hosts.forEach { ip ->
                launch(Dispatchers.IO) {
                    val ok = semaphore.withPermit { isAlive(ip) }
                    if (ok) alive.add(ip)
                    val n = done.incrementAndGet()
                    trySend(ScanProgress(progress = 0.8f * n / hosts.size))
                }
            }
        }

        val arp = macResolver.readArp()
        val devices = (alive.toList() + info.localIp).distinct().map { ip ->
            Device(
                ip = ip,
                mac = arp[ip],
                isGateway = ip == info.gateway,
                isSelf = ip == info.localIp
            )
        }

        val named = coroutineScope {
            devices.map { d ->
                async(Dispatchers.IO) {
                    if (d.isSelf) d.copy(hostName = Build.MODEL)
                    else d.copy(hostName = resolveHost(d.ip))
                }
            }.awaitAll()
        }
        send(ScanProgress(progress = 0.9f))

        val withVendor = named.map { d ->
            val m = d.mac
            if (m != null && !d.isRandomMac) d.copy(vendor = ouiApi.lookup(m)) else d
        }
        val sorted = withVendor.sortedBy { ipToLong(it.ip) }
        send(ScanProgress(progress = 1f, devices = sorted, done = true))
    }

    private fun hostsOf(ip: String, prefix: Int): List<String> {
        val p = prefix.coerceIn(24, 30)
        val mask = (0xFFFFFFFFL shl (32 - p)) and 0xFFFFFFFFL
        val net = ipToLong(ip) and mask
        val broadcast = net or (mask.inv() and 0xFFFFFFFFL)
        return ((net + 1) until broadcast).map { longToIp(it) }
    }

    private fun isAlive(ip: String): Boolean {
        try {
            if (InetAddress.getByName(ip).isReachable(600)) return true
        } catch (_: Exception) {
        }
        for (port in probePorts) {
            try {
                Socket().use { it.connect(InetSocketAddress(ip, port), 200) }
                return true
            } catch (e: ConnectException) {
                // "Connection refused" means the host answered, so it is alive.
                if (e.message?.contains("refused", ignoreCase = true) == true) return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    private suspend fun resolveHost(ip: String): String? = withTimeoutOrNull(1500) {
        suspendCancellableCoroutine<String?> { cont ->
            thread(isDaemon = true) {
                val name = try {
                    InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
                } catch (_: Exception) {
                    null
                }
                if (cont.isActive) cont.resume(name)
            }
        }
    }

    companion object {
        fun ipToLong(ip: String): Long =
            ip.split('.').fold(0L) { acc, s -> (acc shl 8) or s.toLong() }

        fun longToIp(v: Long): String =
            "${(v shr 24) and 255}.${(v shr 16) and 255}.${(v shr 8) and 255}.${v and 255}"
    }
}
