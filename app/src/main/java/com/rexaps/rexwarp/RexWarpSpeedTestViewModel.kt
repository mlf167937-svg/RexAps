package com.rexaps.rexwarp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** UI state terpusat agar tampilan tetap konsisten pada setiap tahap pengukuran. */
data class RexWarpSpeedTestUiState(
    val engineReady: Boolean = false,
    val running: Boolean = false,
    val completed: Boolean = false,
    val phase: String = "Menyiapkan mesin pengukuran…",
    val phaseIndex: Int = 0,
    val phaseCount: Int = 8,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val durationMs: Long? = null,
    val error: String? = null
)

class RexWarpSpeedTestViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RexWarpSpeedTestUiState())
    val uiState: StateFlow<RexWarpSpeedTestUiState> = _uiState.asStateFlow()

    fun onEngineReady() {
        _uiState.value = _uiState.value.copy(
            engineReady = true,
            error = null,
            phase = if (_uiState.value.completed) "Pengujian selesai" else "Siap melakukan pengujian"
        )
    }

    fun onRetryLoading() {
        _uiState.value = RexWarpSpeedTestUiState(phase = "Memuat ulang mesin Cloudflare…")
    }

    fun onEngineError(message: String) {
        _uiState.value = _uiState.value.copy(
            engineReady = false,
            running = false,
            completed = false,
            error = message.cleanError("Mesin Cloudflare tidak dapat dimuat. Periksa koneksi internet lalu coba lagi."),
            phase = "Gagal memuat mesin"
        )
    }

    fun onRunning(running: Boolean) {
        val old = _uiState.value
        _uiState.value = old.copy(
            running = running,
            completed = if (running) false else old.completed,
            phase = when {
                running -> "Mengukur kualitas koneksi…"
                old.completed -> "Pengujian selesai"
                else -> old.phase
            },
            error = if (running) null else old.error
        )
    }

    fun onPhase(json: String) {
        runCatching {
            val obj = JSONObject(json)
            val index = obj.optInt("index", 0).coerceAtLeast(0)
            val count = obj.optInt("count", 8).coerceAtLeast(1)
            val phase = obj.optString("phase", "Mengukur kualitas koneksi…")
            _uiState.value = _uiState.value.copy(
                phase = phase,
                phaseIndex = index.coerceAtMost(count),
                phaseCount = count
            )
        }
    }

    /** Bandwidth payload dari JS sudah berformat bps; UI state menggunakan Mbps. */
    fun onResults(json: String, completed: Boolean = false) {
        runCatching {
            val obj = JSONObject(json)
            fun number(key: String): Double? {
                if (!obj.has(key) || obj.isNull(key)) return null
                return obj.optDouble(key).takeIf { it.isFinite() && it >= 0.0 }
            }
            val download = number("downloadBps")?.div(1_000_000.0)
            val upload = number("uploadBps")?.div(1_000_000.0)
            val latency = number("latencyMs")
            val jitter = number("jitterMs")
            val duration = number("durationMs")?.toLong()
            val old = _uiState.value
            _uiState.value = old.copy(
                downloadMbps = download ?: old.downloadMbps,
                uploadMbps = upload ?: old.uploadMbps,
                latencyMs = latency ?: old.latencyMs,
                jitterMs = jitter ?: old.jitterMs,
                durationMs = duration ?: old.durationMs,
                running = if (completed) false else old.running,
                completed = completed || old.completed,
                phase = if (completed) "Pengujian selesai" else old.phase,
                phaseIndex = if (completed) old.phaseCount else old.phaseIndex,
                error = null
            )
        }
    }

    fun onTestError(message: String) {
        _uiState.value = _uiState.value.copy(
            running = false,
            completed = false,
            phase = "Pengujian gagal",
            error = message.cleanError("Pengujian gagal. Periksa koneksi lalu coba lagi.")
        )
    }

    fun onStartRequested() {
        val old = _uiState.value
        _uiState.value = old.copy(
            running = true,
            completed = false,
            phase = "Menyiapkan pengujian…",
            phaseIndex = 0,
            phaseCount = 8,
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

    private fun String?.cleanError(fallback: String): String {
        val text = this.orEmpty().trim().take(240)
        return text.ifBlank { fallback }
    }
}
