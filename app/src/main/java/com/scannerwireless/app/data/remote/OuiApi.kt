package com.scannerwireless.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Vendor lookup through api.macvendors.com (free tier: ~1 request/second). */
@Singleton
class OuiApi @Inject constructor(private val client: OkHttpClient) {
    private val cache = ConcurrentHashMap<String, String>()
    private val mutex = Mutex()
    private var lastCall = 0L

    suspend fun lookup(mac: String): String? {
        val oui = mac.uppercase().replace("-", ":").take(8)
        cache[oui]?.let { return it.takeIf { v -> v.isNotEmpty() } }

        return mutex.withLock {
            val wait = 1100 - (System.currentTimeMillis() - lastCall)
            if (wait > 0) delay(wait)
            val result: String? = withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder().url("https://api.macvendors.com/$oui").build()
                    client.newCall(req).execute().use { r ->
                        if (r.isSuccessful) r.body?.string()?.trim()
                        else if (r.code == 404) ""
                        else null
                    }
                } catch (_: Exception) {
                    null
                }
            }
            lastCall = System.currentTimeMillis()
            if (result != null) cache[oui] = result
            result?.takeIf { it.isNotEmpty() }
        }
    }
}
