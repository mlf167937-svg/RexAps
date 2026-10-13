package com.rexaps.rexwarp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import kotlin.math.max

/** State UI untuk pengujian Cloudflare. Nilai bandwidth disimpan dalam Mbps. */
data class RexWarpSpeedTestUiState(
    val engineReady: Boolean = false,
    val running: Boolean = false,
    val completed: Boolean = false,
    val phase: String = "Siap melakukan pengujian",
    val phaseIndex: Int = 0,
    val phaseCount: Int = 0,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val durationMs: Long? = null,
    val error: String? = null
)

/** Menjaga state pengujian di luar composable dan mengonversi payload mesin Cloudflare. */
class RexWarpSpeedTestViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RexWarpSpeedTestUiState())
    val uiState: StateFlow<RexWarpSpeedTestUiState> = _uiState.asStateFlow()

    fun onEngineReady() {
        _uiState.value = _uiState.value.copy(engineReady = true, error = null)
    }

    fun onRetryLoading() {
        _uiState.value = RexWarpSpeedTestUiState()
    }

    fun onEngineError(message: String) {
        _uiState.value = _uiState.value.copy(
            engineReady = false,
            running = false,
            completed = false,
            error = message.ifBlank { "Mesin Cloudflare tidak dapat dimuat. Periksa koneksi internet lalu coba lagi." },
            phase = "Gagal memuat mesin"
        )
    }

    fun onRunning(running: Boolean) {
        val old = _uiState.value
        _uiState.value = old.copy(
            running = running,
            completed = if (running) false else old.completed,
            phase = if (running) "Mengukur kualitas koneksi…" else old.phase,
            error = if (running) null else old.error
        )
    }

    fun onPhase(json: String) {
        runCatching {
            val obj = JSONObject(json)
            val index = obj.optInt("index", 0)
            val count = obj.optInt("count", 0)
            val phase = obj.optString("phase", "Mengukur kualitas koneksi…")
            _uiState.value = _uiState.value.copy(
                phase = phase,
                phaseIndex = index,
                phaseCount = count
            )
        }
    }

    fun onResults(json: String, completed: Boolean = false) {
        runCatching {
            val obj = JSONObject(json)
            fun number(key: String): Double? = if (obj.isNull(key) || !obj.has(key)) null else obj.optDouble(key).takeIf { it.isFinite() && it >= 0.0 }
            val downloadBps = number("downloadBps")
            val uploadBps = number("uploadBps")
            val latency = number("latencyMs")
            val jitter = number("jitterMs")
            val duration = number("durationMs")
            val old = _uiState.value
            _uiState.value = old.copy(
                downloadMbps = downloadBps?.div(1_000_000.0) ?: old.downloadMbps,
                uploadMbps = uploadBps?.div(1_000_000.0) ?: old.uploadMbps,
                latencyMs = latency ?: old.latencyMs,
                jitterMs = jitter ?: old.jitterMs,
                durationMs = duration?.toLong() ?: old.durationMs,
                running = !completed && old.running,
                completed = completed,
                phase = if (completed) "Pengujian selesai" else old.phase,
                error = null
            )
        }
    }

    fun onTestError(message: String) {
        _uiState.value = _uiState.value.copy(
            running = false,
            completed = false,
            phase = "Pengujian gagal",
            error = message.ifBlank { "Pengujian gagal. Periksa koneksi lalu coba lagi." }
        )
    }

    fun onStartRequested() {
        val old = _uiState.value
        _uiState.value = old.copy(
            running = true,
            completed = false,
            phase = "Menyiapkan pengujian…",
            phaseIndex = 0,
            phaseCount = 0,
            downloadMbps = null,
            uploadMbps = null,
            latencyMs = null,
            jitterMs = null,
            durationMs = null,
            error = null
        )
    }

    fun onCancelRequested() {
        _uiState.value = _uiState.value.copy(
            running = false,
            completed = false,
            phase = "Pengujian dibatalkan",
            error = null
        )
    }

    fun onScreenDisposed() {
        _uiState.value = RexWarpSpeedTestUiState()
    }
}
