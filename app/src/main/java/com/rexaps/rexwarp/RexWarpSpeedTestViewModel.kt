package com.rexaps.rexwarp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rexwarp.data.RexWarpDatabase
import com.rexaps.rexwarp.data.RexWarpSpeedTestAggregate
import com.rexaps.rexwarp.data.RexWarpSpeedTestEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

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

enum class RexWarpSpeedTestPeriod(val label: String, val durationMs: Long?) {
    HOURS_24("24 jam", 24L * 60 * 60 * 1000),
    DAYS_7("7 hari", 7L * 24 * 60 * 60 * 1000),
    DAYS_30("30 hari", 30L * 24 * 60 * 60 * 1000),
    DAYS_90("90 hari", 90L * 24 * 60 * 60 * 1000),
    ALL_TIME("Semua", null)
}

enum class RexWarpSpeedTestVpnFilter(val label: String, val vpnConnected: Boolean?) {
    ALL("Semua kondisi", null), WARP_ON("WARP aktif", true), WARP_OFF("WARP nonaktif", false)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RexWarpSpeedTestViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RexWarpSpeedTestRepository(RexWarpDatabase.get(app).speedTestDao())
    private val _uiState = MutableStateFlow(RexWarpSpeedTestUiState())
    val uiState: StateFlow<RexWarpSpeedTestUiState> = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(RexWarpSpeedTestPeriod.DAYS_30)
    val selectedPeriod: StateFlow<RexWarpSpeedTestPeriod> = _selectedPeriod.asStateFlow()
    private val _selectedVpnFilter = MutableStateFlow(RexWarpSpeedTestVpnFilter.ALL)
    val selectedVpnFilter: StateFlow<RexWarpSpeedTestVpnFilter> = _selectedVpnFilter.asStateFlow()
    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

    /** null means the query has not emitted yet; an empty list is a real empty state. */
    val history: StateFlow<List<RexWarpSpeedTestEntity>?> = repository.observeRecent(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val benchmark: StateFlow<RexWarpSpeedTestAggregate?> = kotlinx.coroutines.flow.combine(_selectedPeriod, _selectedVpnFilter) { period, vpnFilter ->
        period to vpnFilter
    }.flatMapLatest { (period, vpnFilter) ->
        val cutoff = period.durationMs?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) } ?: 0L
        repository.observeAggregateSince(cutoff, vpnFilter.vpnConnected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val runLock = Any()
    private var runStartedAtEpochMs: Long? = null
    private var runVpnConnected: Boolean? = null
    private var runAlreadyRecorded = false

    fun setHistoryPeriod(period: RexWarpSpeedTestPeriod) { _selectedPeriod.value = period }
    fun setVpnFilter(filter: RexWarpSpeedTestVpnFilter) { _selectedVpnFilter.value = filter }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onSuccess { _historyError.value = if (it) null else "Hasil tidak ditemukan atau sudah dihapus." }
                .onFailure { _historyError.value = "Riwayat tidak dapat dihapus dari database lokal." }
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            runCatching { repository.deleteAll() }
                .onSuccess { _historyError.value = null }
                .onFailure { _historyError.value = "Riwayat tidak dapat dihapus dari database lokal." }
        }
    }

    fun clearHistoryError() { _historyError.value = null }

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
            error = sanitizeError(message, "Mesin Cloudflare tidak dapat dimuat. Periksa koneksi internet lalu coba lagi."),
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
            val phase = obj.optString("phase", "Mengukur kualitas koneksi…").take(100)
            _uiState.value = _uiState.value.copy(
                phase = phase,
                phaseIndex = index.coerceAtMost(count),
                phaseCount = count
            )
        }
    }

    /** Bandwidth payload JS memakai bps; state UI dan histori menggunakan Mbps. */
    fun onResults(json: String, completed: Boolean = false) {
        val parsed = runCatching {
            val obj = JSONObject(json)
            fun number(key: String): Double? = when (val raw = obj.opt(key)) {
                is Number -> raw.toDouble().takeIf { it.isFinite() && it >= 0.0 }
                else -> null
            }
            ParsedMetrics(
                downloadMbps = number("downloadBps")?.div(1_000_000.0),
                uploadMbps = number("uploadBps")?.div(1_000_000.0),
                latencyMs = number("latencyMs"),
                jitterMs = number("jitterMs"),
                durationMs = number("durationMs")?.toLong()
            )
        }.getOrNull() ?: return

        val old = _uiState.value
        val metrics = ParsedMetrics(
            downloadMbps = parsed.downloadMbps ?: old.downloadMbps,
            uploadMbps = parsed.uploadMbps ?: old.uploadMbps,
            latencyMs = parsed.latencyMs ?: old.latencyMs,
            jitterMs = parsed.jitterMs ?: old.jitterMs,
            durationMs = parsed.durationMs ?: old.durationMs
        )

        if (completed && !metrics.hasAnyMetric()) {
            val message = "Pengujian selesai tanpa metrik yang dapat dibaca. Hasil tidak ditandai sebagai sukses."
            _uiState.value = old.copy(running = false, completed = false, phase = "Pengujian gagal", error = message)
            recordCurrentRun(metrics, successful = false, error = message)
            return
        }

        _uiState.value = old.copy(
            downloadMbps = metrics.downloadMbps,
            uploadMbps = metrics.uploadMbps,
            latencyMs = metrics.latencyMs,
            jitterMs = metrics.jitterMs,
            durationMs = metrics.durationMs,
            running = if (completed) false else old.running,
            completed = completed || old.completed,
            phase = if (completed) "Pengujian selesai" else old.phase,
            phaseIndex = if (completed) old.phaseCount else old.phaseIndex,
            error = null
        )
        if (completed) recordCurrentRun(metrics, successful = true, error = null)
    }

    fun onTestError(message: String) {
        val clean = sanitizeError(message, "Pengujian gagal. Periksa koneksi lalu coba lagi.")
        val old = _uiState.value
        _uiState.value = old.copy(running = false, completed = false, phase = "Pengujian gagal", error = clean)
        recordCurrentRun(
            ParsedMetrics(old.downloadMbps, old.uploadMbps, old.latencyMs, old.jitterMs, old.durationMs),
            successful = false,
            error = clean
        )
    }

    fun onStartRequested(vpnConnected: Boolean? = null) {
        synchronized(runLock) {
            runStartedAtEpochMs = System.currentTimeMillis()
            runVpnConnected = vpnConnected
            runAlreadyRecorded = false
        }
        _uiState.value = _uiState.value.copy(
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
        synchronized(runLock) {
            runStartedAtEpochMs = null
            runVpnConnected = null
            runAlreadyRecorded = true
        }
        _uiState.value = _uiState.value.copy(running = false, completed = false, phase = "Pengujian dibatalkan", error = null)
    }

    fun onScreenDisposed() {
        synchronized(runLock) {
            runStartedAtEpochMs = null
            runVpnConnected = null
            runAlreadyRecorded = true
        }
        _uiState.value = RexWarpSpeedTestUiState()
    }

    private fun recordCurrentRun(metrics: ParsedMetrics, successful: Boolean, error: String?) {
        val metadata = synchronized(runLock) {
            val value = runStartedAtEpochMs
            if (value == null || runAlreadyRecorded) null
            else {
                runAlreadyRecorded = true
                runStartedAtEpochMs = null
                val vpn = runVpnConnected
                runVpnConnected = null
                value to vpn
            }
        } ?: return

        val start = metadata.first
        val ended = System.currentTimeMillis().coerceAtLeast(start)
        val result = RexWarpSpeedTestEntity(
            id = UUID.randomUUID().toString(),
            startedAtEpochMs = start,
            completedAtEpochMs = ended,
            downloadMbps = metrics.downloadMbps,
            uploadMbps = metrics.uploadMbps,
            latencyMs = metrics.latencyMs,
            jitterMs = metrics.jitterMs,
            durationMs = metrics.durationMs,
            vpnConnected = metadata.second,
            isSuccessful = successful && metrics.hasAnyMetric(),
            errorSummary = if (successful) null else sanitizeError(error, "Pengujian tidak berhasil."),
            source = "cloudflare-webview"
        )
        viewModelScope.launch {
            runCatching { repository.save(result) }
                .onSuccess { _historyError.value = null }
                .onFailure { _historyError.value = "Hasil tes tidak dapat disimpan. Ruang penyimpanan atau database perlu diperiksa." }
        }
    }

    private data class ParsedMetrics(
        val downloadMbps: Double?,
        val uploadMbps: Double?,
        val latencyMs: Double?,
        val jitterMs: Double?,
        val durationMs: Long?
    ) {
        fun hasAnyMetric() = downloadMbps != null || uploadMbps != null || latencyMs != null || jitterMs != null
    }

    private fun sanitizeError(message: String?, fallback: String): String {
        val text = message.orEmpty().trim()
            .replace(Regex("(?i)\\b(bearer|token|api[_ -]?key|private[_ -]?key)\\s*[:=]?\\s*[^\\s,;]+"), "\$1=[redacted]")
            .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "[url]")
            .replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "[ip]")
            .replace(Regex("(?i)(?<![a-f0-9])(?:[a-f0-9]{0,4}:){2,7}[a-f0-9]{0,4}(?![a-f0-9])"), "[ip]")
            .replace(Regex("\\b[a-f0-9]{40,}\\b", RegexOption.IGNORE_CASE), "[redacted]")
            .take(240)
        return text.ifBlank { fallback }
    }
}
