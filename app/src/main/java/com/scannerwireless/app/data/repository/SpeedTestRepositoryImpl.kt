package com.scannerwireless.app.data.repository

import com.scannerwireless.app.data.remote.SpeedTestClient
import com.scannerwireless.app.domain.model.SpeedPhase
import com.scannerwireless.app.domain.model.SpeedProgress
import com.scannerwireless.app.domain.model.SpeedResult
import com.scannerwireless.app.domain.repository.SpeedTestRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedTestRepositoryImpl @Inject constructor(
    private val client: SpeedTestClient
) : SpeedTestRepository {

    private val _last = MutableStateFlow<SpeedResult?>(null)
    override val last: StateFlow<SpeedResult?> = _last.asStateFlow()

    override fun run(): Flow<SpeedProgress> = channelFlow {
        try {
            val p0 = SpeedProgress(SpeedPhase.LATENCY)
            send(p0)
            val latency = client.latency()

            val p1 = p0.copy(phase = SpeedPhase.DOWNLOAD, latencyMs = latency)
            send(p1)
            val down = client.download { trySend(p1.copy(liveMbps = it)) }

            val p2 = p1.copy(phase = SpeedPhase.UPLOAD, downloadMbps = down, liveMbps = 0.0)
            send(p2)
            val up = client.upload { trySend(p2.copy(liveMbps = it)) }

            _last.value = SpeedResult(down, up, latency)
            send(p2.copy(phase = SpeedPhase.DONE, uploadMbps = up, liveMbps = 0.0))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            send(SpeedProgress(SpeedPhase.ERROR))
        }
    }
}
