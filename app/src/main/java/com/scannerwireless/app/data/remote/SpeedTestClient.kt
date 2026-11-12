package com.scannerwireless.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Uses Cloudflare's public speed endpoints. */
@Singleton
class SpeedTestClient @Inject constructor(private val client: OkHttpClient) {
    private val base = "https://speed.cloudflare.com"

    suspend fun latency(): Long = withContext(Dispatchers.IO) {
        val times = (1..5).mapNotNull {
            try {
                val t0 = System.nanoTime()
                client.newCall(Request.Builder().url("$base/__down?bytes=0").build())
                    .execute().use { r -> r.body?.bytes() }
                (System.nanoTime() - t0) / 1_000_000
            } catch (_: Exception) {
                null
            }
        }
        times.minOrNull() ?: -1L
    }

    suspend fun download(onProgress: (Double) -> Unit): Double = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base/__down?bytes=100000000").build()
        var total = 0L
        val start = System.nanoTime()
        var lastEmit = start
        var mbps = 0.0
        client.newCall(req).execute().use { resp ->
            val stream = resp.body?.byteStream() ?: return@use
            val buf = ByteArray(64 * 1024)
            while (isActive) {
                val n = stream.read(buf)
                if (n < 0) break
                total += n
                val now = System.nanoTime()
                val sec = (now - start) / 1e9
                if (sec > 0.3) mbps = total * 8 / 1e6 / sec
                if (now - lastEmit > 200_000_000) {
                    lastEmit = now
                    onProgress(mbps)
                }
                if (sec >= 8.0) break
            }
        }
        val sec = (System.nanoTime() - start) / 1e9
        if (sec > 0) total * 8 / 1e6 / sec else 0.0
    }

    suspend fun upload(onProgress: (Double) -> Unit): Double = withContext(Dispatchers.IO) {
        val bytes = ByteArray(10 * 1024 * 1024)
        val body = bytes.toRequestBody("application/octet-stream".toMediaType())
        val req = Request.Builder().url("$base/__up").post(body).build()
        val start = System.nanoTime()
        client.newCall(req).execute().use { r -> r.body?.bytes() }
        val sec = (System.nanoTime() - start) / 1e9
        val mbps = if (sec > 0) bytes.size * 8 / 1e6 / sec else 0.0
        onProgress(mbps)
        mbps
    }
}
