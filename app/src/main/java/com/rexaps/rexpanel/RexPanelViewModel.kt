package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import android.content.Context

sealed interface ConnState {
    object Idle : ConnState
    object Connecting : ConnState
    data class Connected(val target: SshTarget) : ConnState
    data class Error(val message: String) : ConnState
}

data class Metrics(
    val host: String,
    val cpu: Float?,
    val cpuFreqFrac: Float?,
    val cpuFreqMhz: Int?,
    val cores: Int?,
    val memUsedKb: Long?,
    val memTotalKb: Long?,
    val load: String?,
    val uptimeSec: Long?,
    val uptimeText: String?,
    val diskPct: Int?,
    val diskUsedKb: Long?,
    val diskTotalKb: Long?,
    val diskMount: String?,
    val rxBps: Long?,
    val txBps: Long?,
    val batteryPct: Int?,
    val batteryStatus: String?,
    val batteryTemp: Float?
)

class RawSample(
    val cpuTotal: Long?,
    val cpuIdle: Long?,
    val freqFrac: Float?,
    val freqMhz: Int?,
    val cores: Int?,
    val memTotalKb: Long?,
    val memAvailKb: Long?,
    val load: String?,
    val uptimeSec: Long?,
    val uptimeText: String?,
    val diskPct: Int?,
    val diskUsedKb: Long?,
    val diskTotalKb: Long?,
    val diskMount: String?,
    val rx: Long?,
    val tx: Long?,
    val batPct: Int?,
    val batStatus: String?,
    val batTemp: Float?,
    val host: String?,
    val topCpu: Float?,
    val at: Long = System.currentTimeMillis()
)

// § diganti menjadi $ supaya tidak bentrok dengan string template Kotlin.
// @@ELEV@@ = 1 jika pengguna mengizinkan sudo/su.
// Default 0: aman untuk akun biasa (non-root).
private val MON_CMD = """
T=""; command -v timeout >/dev/null 2>&1 && T="timeout 3"
E=@@ELEV@@
rd() {
  cat "§1" 2>/dev/null && return 0
  [ "§(id -u)" = 0 ] && return 1
  [ "§E" = 1 ] || return 1
  [ -n "§T" ] || return 1
  §T sudo -n cat "§1" 2>/dev/null && return 0
  command -v su >/dev/null 2>&1 && §T su -c "cat §1" 2>/dev/null
}
STATLINE="§(rd /proc/stat 2>/dev/null | head -n1)"
echo "##STAT"; echo "§STATLINE"
echo "##TOP"
[ -z "§STATLINE" ] && §T top -bn1 2>/dev/null | head -n5
echo "##MEM"; grep -E '^(MemTotal|MemAvailable):' /proc/meminfo 2>/dev/null
echo "##LOAD"; rd /proc/loadavg
echo "##UPTIME"; cat /proc/uptime 2>/dev/null
echo "##UPCMD"; uptime 2>/dev/null
echo "##DF"; df -P / 2>/dev/null | tail -n1
echo "##DFH"; df -P "§HOME" 2>/dev/null | tail -n1
echo "##NET"; rd /proc/net/dev
echo "##SYSNET"
for d in /sys/class/net/*; do
  n="§{d##*/}"; [ "§n" = lo ] && continue
  echo "§n §(cat §d/statistics/rx_bytes 2>/dev/null) §(cat §d/statistics/tx_bytes 2>/dev/null)"
done
echo "##FREQ"
for c in /sys/devices/system/cpu/cpu[0-9]*; do
  echo "§(cat §c/cpufreq/scaling_cur_freq 2>/dev/null) §(cat §c/cpufreq/cpuinfo_max_freq 2>/dev/null)"
done
echo "##CORES"; nproc 2>/dev/null
echo "##BAT"
command -v termux-battery-status >/dev/null 2>&1 && §T termux-battery-status 2>/dev/null
echo "##HOST"; hostname 2>/dev/null
""".trimIndent().replace("§", "$")

internal fun monCmd(elevate: Boolean): String =
    MON_CMD.replace("@@ELEV@@", if (elevate) "1" else "0")

internal val LOAD_RE =
    Regex("""^\d+\.\d+ \d+\.\d+ \d+\.\d+ \d+/\d+ \d+$""")

internal val UPCMD_LOAD_RE =
    Regex("""load averages?:\s*([\d.]+),?\s+([\d.]+),?\s+([\d.]+)""")

internal val UPCMD_UP_RE =
    Regex("""up\s+(.+?),\s+(?:\d+\s+users?|load)""")

internal val WS = Regex("\\s+")

private val TOP_TOYBOX =
    Regex("""(\d+)%cpu.*?(\d+)%idle""", RegexOption.IGNORE_CASE)

private val TOP_LINUX =
    Regex("""(\d+(?:[.,]\d+)?)\s*id\b""")

private val TOP_BUSY =
    Regex("""(\d+)%\s*idle""", RegexOption.IGNORE_CASE)

/**
 * CPU dari keluaran `top`
 * Android toybox, procps Linux, busybox.
 *
 * Dipakai saat /proc/stat tidak bisa dibaca.
 */
internal fun parseTopCpu(t: String): Float? {
    TOP_TOYBOX.find(t)?.let {
        val tot = it.groupValues[1].toFloat()
        val idle = it.groupValues[2].toFloat()

        if (tot > 0f) {
            return ((tot - idle) / tot).coerceIn(0f, 1f)
        }
    }

    TOP_LINUX.find(t)?.let {
        return (
            1f -
                it.groupValues[1]
                    .replace(',', '.')
                    .toFloat() / 100f
            ).coerceIn(0f, 1f)
    }

    TOP_BUSY.find(t)?.let {
        return (
            1f -
                it.groupValues[1].toFloat() / 100f
            ).coerceIn(0f, 1f)
    }

    return null
}

fun parseSample(raw: String): RawSample {
        return __splitImpl_parseSample(raw)
    }

internal fun buildMetrics(
    cur: RawSample,
    prev: RawSample?
): Metrics {
        return __splitImpl_buildMetrics(cur, prev)
    }

internal fun friendly(e: Exception): String {
    val m = e.message.orEmpty()
    val l = m.lowercase()

    return when {
        "auth" in l ->
            "Login ditolak. Cek user dan password."

        "refused" in l ->
            "Koneksi ditolak. Cek port dan pastikan SSH server berjalan."

        "timeout" in l ||
            "timed out" in l ->
            "Server tidak merespons. Cek IP, port, dan jaringan."

        "unknownhost" in l ->
            "Host tidak ditemukan."

        else ->
            m.ifBlank {
                "Gagal terhubung."
            }
    }
}

class RexPanelViewModel : ViewModel() {

    var conn by mutableStateOf<ConnState>(
        ConnState.Idle
    )
        internal set

    var metrics by mutableStateOf<Metrics?>(null)
        internal set

    var files by mutableStateOf<FileBrowserState?>(null)
        internal set

    val cpuHistory =
        mutableStateListOf<Float>()

    val term =
        TermBuffer(80, 24)

    var ctrl by mutableStateOf(false)
        internal set

    var alt by mutableStateOf(false)
        internal set

    var shift by mutableStateOf(false)
        internal set

    internal var ssh: SshSession? = null

    internal var monitorJob: Job? = null

    internal var cols = 80
    internal var rows = 24

    internal val incoming =
        Channel<String>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (s in incoming) {
                term.feed(s)
            }
        }
    }

    fun toggleCtrl() {
        ctrl = !ctrl
    }

    fun toggleAlt() {
        alt = !alt
    }

    fun toggleShift() {
        shift = !shift
    }

    fun connect(
        command: String,
        password: String,
        elevate: Boolean,
        context: Context
    ) {
        return this.__splitImpl_connect(command, password, elevate, context)
    }

    internal fun onClosed(
        session: SshSession
    ) {
        if (ssh === session) {

            monitorJob?.cancel()

            files?.close()
            files = null

            ssh = null

            metrics = null

            conn =
                ConnState.Error(
                    "Koneksi terputus."
                )
        }
    }

    internal fun startMonitor(
        session: SshSession,
        elevate: Boolean
    ) {
        return this.__splitImpl_startMonitor(session, elevate)
    }

    fun disconnect() {

        monitorJob?.cancel()

        files?.close()
        files = null

        ssh?.close()
        ssh = null

        metrics = null

        conn =
            ConnState.Idle
    }

    fun resize(
        newCols: Int,
        newRows: Int
    ) {
        if (
            newCols == cols &&
            newRows == rows
        ) {
            return
        }

        cols = newCols
        rows = newRows

        term.resize(
            newCols,
            newRows
        )

        ssh?.resize(
            newCols,
            newRows
        )
    }

    /* ------------------------------- input ------------------------------ */

    private fun clearMods() {
        ctrl = false
        alt = false
        shift = false
    }

    fun sendRaw(s: String) {
        clearMods()

        ssh?.send(
            s.toByteArray(
                Charsets.UTF_8
            )
        )
    }

    fun sendText(text: String) {
        return this.__splitImpl_sendText(text)
    }

    /**
     * Panah, Home, End.
     *
     * final:
     * A = atas
     * B = bawah
     * C = kanan
     * D = kiri
     * H = home
     * F = end
     */
    fun sendCursor(final: Char) {

        val mod =
            1 +
                (if (shift) 1 else 0) +
                (if (alt) 2 else 0) +
                (if (ctrl) 4 else 0)

        sendRaw(
            if (mod == 1) {
                "\u001B[$final"
            } else {
                "\u001B[1;$mod$final"
            }
        )
    }

    /**
     * PgUp = 5
     * PgDn = 6
     * Delete = 3
     */
    fun sendTilde(n: Int) {
        sendRaw(
            "\u001B[$n~"
        )
    }

    override fun onCleared() {

        monitorJob?.cancel()

        files?.close()

        ssh?.close()

        super.onCleared()
    }
}