package com.rexaps.rexwarp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rexwarp.model.RexWarpUsageSummary
import com.rexaps.rexwarp.tunnel.RexWarpTunnelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

class RexWarpViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RexWarpRepository.get(app)
    private val store = RexWarpBackupStore.get(app)
    private val sharing = SharingStarted.WhileSubscribed(5_000)

    val state: StateFlow<RexWarpState> = repo.state
    val capabilities = RexWarpTunnelRegistry.provider.capabilities
    val settings: StateFlow<RexWarpSettings> =
        repo.preferences.settings.stateIn(viewModelScope, sharing, RexWarpSettings())

    private val today = MutableStateFlow(LocalDate.now())
    private val tick = MutableStateFlow(0)
    private val selection = MutableStateFlow(RexWarpSelection())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private val _backup = MutableStateFlow(RexWarpBackupUi(null, store.recording))
    val backup: StateFlow<RexWarpBackupUi> = _backup.asStateFlow()

    init {
        refreshBackup()
        // Auto-restore dari folder data bila database kosong (mis. setelah install ulang)
        viewModelScope.launch(Dispatchers.IO) {
            if (store.hasAccess() && repo.usage.isEmpty()) repo.usage.restoreFromFolder()
        }
    }

    /** null = masih loading. */
    val allUsage: StateFlow<List<RexWarpDailyUsage>?> =
        repo.usage.observeAll().stateIn<List<RexWarpDailyUsage>?>(viewModelScope, sharing, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val hourly: Flow<List<RexWarpBucket>> =
        combine(selection.map { it.graph == RexWarpGraphRange.HOURS_24 }.distinctUntilChanged(), tick) { h, _ -> h }
            .flatMapLatest { wanted -> if (wanted) repo.usage.observeHours(24) else flowOf(emptyList()) }

    val periods: StateFlow<RexWarpPeriodSummaries?> = combine(allUsage, today) { rows, t ->
        rows?.let {
            val weekStart = t.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
            RexWarpPeriodSummaries(
                today = it.summary(DateRange(t, t)), week = it.summary(DateRange(weekStart, t)),
                month = it.summary(DateRange(t.withDayOfMonth(1), t)),
                allTime = it.fold(RexWarpUsageSummary()) { a, d -> a + RexWarpUsageSummary(d.downloadBytes, d.uploadBytes) }
            )
        }
    }.stateIn(viewModelScope, sharing, null)

    val usageUi: StateFlow<RexWarpUsageUi?> = combine(allUsage, selection, today, hourly) { rows, sel, t, hours ->
        rows?.let {
            val range = sel.preset.resolve(t, sel.custom)
            val buckets: List<RexWarpBucket> = if (sel.graph == RexWarpGraphRange.HOURS_24) {
                hours
            } else {
                val graphRange = sel.graph.days?.let { n -> DateRange(t.minusDays((n - 1).toLong()), t) } ?: sel.custom
                val byDate = it.associateBy { d -> d.date }
                graphRange?.days()?.map { d ->
                    byDate[d]?.let { u -> RexWarpBucket(d.toString(), u.downloadBytes, u.uploadBytes) }
                        ?: RexWarpBucket(d.toString(), 0, 0)
                }.orEmpty()
            }
            RexWarpUsageUi(
                selection = sel, range = range,
                rangeSummary = range?.let { r -> it.summary(r) } ?: RexWarpUsageSummary(),
                graphBuckets = buckets, hasAnyData = it.isNotEmpty(),
                hasDataInGraphRange = buckets.any { b -> b.totalBytes > 0 }
            )
        }
    }.stateIn(viewModelScope, sharing, null)

    /** Histori: hanya hari yang punya data, terbaru di atas. */
    val history: StateFlow<List<RexWarpDailyUsage>?> =
        allUsage.map { it?.filter { d -> d.totalBytes > 0 }?.sortedByDescending { d -> d.date } }
            .stateIn<List<RexWarpDailyUsage>?>(viewModelScope, sharing, null)

    private fun List<RexWarpDailyUsage>.summary(range: DateRange) =
        filter { it.date in range }.fold(RexWarpUsageSummary()) { a, d -> a + RexWarpUsageSummary(d.downloadBytes, d.uploadBytes) }

    fun refreshToday() { today.value = LocalDate.now(); tick.update { it + 1 } }
    fun selectPreset(p: RexWarpRangePreset) = selection.update { it.copy(preset = p) }
    fun selectGraph(g: RexWarpGraphRange) = selection.update { it.copy(graph = g) }
    fun setCustomRange(r: DateRange) = selection.update { it.copy(custom = r) }

    // ---- koneksi ----
    fun startVpn() = RexWarpVpnService.start(getApplication())
    fun disconnect() = RexWarpVpnService.stop(getApplication())
    fun onPermissionDenied() = repo.setError(RexWarpError.PERMISSION_DENIED)

    // ---- settings ----
    fun saveSettings(s: RexWarpSettings) { viewModelScope.launch { repo.preferences.save(s) } }

    // ---- pencatatan & backup (folder otomatis) ----
    fun refreshBackup() {
        viewModelScope.launch(Dispatchers.IO) { _backup.value = RexWarpBackupUi(store.folderName(), store.recording) }
    }

    fun setRecording(on: Boolean) {
        store.recording = on
        if (on) RexWarpRecorderService.start(getApplication()) else RexWarpRecorderService.stop(getApplication())
        refreshBackup()
    }

    fun restoreFromFolder() = viewModelScope.launch {
        val result = repo.usage.restoreFromFolder()
        _messages.emit(
            result.fold({ "Restored $it day(s) from the backup folder." },
                { (it as? IllegalArgumentException)?.message ?: "Restore failed. Check the folder and try again." })
        )
    }

    // ---- data ----
    fun resetToday() = viewModelScope.launch { repo.usage.deleteDay(LocalDate.now()); _messages.emit("Today's statistics were reset.") }
    fun resetAll() = viewModelScope.launch { repo.usage.deleteAll(); _messages.emit("All statistics were reset.") }

    fun export(uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            val json = repo.usage.exportJson()
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }
        }.isSuccess
        _messages.emit(if (ok) "Usage data exported." else "Export failed. Try a different location.")
    }

    fun importUsage(uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            val text = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)!!.use { String(readCapped(it)) }
            }
            repo.usage.importJson(text).getOrThrow()
        }
        _messages.emit(
            result.fold({ "Imported $it day(s). Existing dates were overwritten." },
                { (it as? IllegalArgumentException)?.message ?: "Import failed. The file could not be read." })
        )
    }

    private fun readCapped(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream(); val buf = ByteArray(8192); var total = 0
        while (true) {
            val n = input.read(buf); if (n < 0) break
            total += n; require(total <= MAX_IMPORT_BYTES) { "This file is too large to import." }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private companion object { const val MAX_IMPORT_BYTES = 5 * 1024 * 1024 }
}

