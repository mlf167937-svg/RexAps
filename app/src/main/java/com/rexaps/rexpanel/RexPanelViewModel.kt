package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    val cpuTotal: Long?, val cpuIdle: Long?,
    val freqFrac: Float?, val freqMhz: Int?, val cores: Int?,
    val memTotalKb: Long?, val memAvailKb: Long?,
    val load: String?,
    val uptimeSec: Long?, val uptimeText: String?,
    val diskPct: Int?, val diskUsedKb: Long?, val diskTotalKb: Long?, val diskMount: String?,
    val rx: Long?, val tx: Long?,
    val batPct: Int?, val batStatus: String?, val batTemp: Float?,
    val host: String?,
    val at: Long = System.currentTimeMillis()
)

// § diganti menjadi $ supaya tidak bentrok dengan string template Kotlin.
private val MON_CMD = """
T=""; command -v timeout >/dev/null 2>&1 && T="timeout 3"
rd() {
  cat "§1" 2>/dev/null && return 0
  [ "§(id -u)" = 0 ] && return 1
  [ -n "§T" ] || return 1
  §T sudo -n cat "§1" 2>/dev/null && return 0
  command -v su >/dev/null 2>&1 && §T su -c "cat §1" 2>/dev/null
}
echo "##STAT"; rd /proc/stat | head -n1
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

private val LOAD_RE = Regex("""^\d+\.\d+ \d+\.\d+ \d+\.\d+ \d+/\d+ \d+$""")
private val UPCMD_LOAD_RE = Regex("""load averages?:\s*([\d.]+),?\s+([\d.]+),?\s+([\d.]+)""")
private val UPCMD_UP_RE = Regex("""up\s+(.+?),\s+(?:\d+\s+users?|load)""")
private val WS = Regex("\\s+")

fun parseSample(raw: String): RawSample {
    val sec = HashMap<String, MutableList<String>>()
    var key = ""
    for (l in raw.lines()) {
        val t = l.trim()
        if (t.startsWith("##")) {
            key = t.removePrefix("##")
            sec[key] = mutableListOf()
        } else if (t.isNotEmpty()) {
            sec[key]?.add(t)
        }
    }
    fun lines(k: String): List<String> = sec[k].orEmpty()

    // CPU (delta dari /proc/stat)
    var cpuT: Long? = null
    var cpuI: Long? = null
    lines("STAT").firstOrNull { it.startsWith("cpu ") }?.let { line ->
        val n = line.split(WS).drop(1).mapNotNull { it.toLongOrNull() }
        if (n.size >= 5) { cpuT = n.take(8).sum(); cpuI = n[3] + n[4] }
    }

    // Frekuensi CPU (cadangan)
    val freqs = lines("FREQ").mapNotNull { l ->
        val t = l.split(WS)
        val cur = t.getOrNull(0)?.toLongOrNull()
        val max = t.getOrNull(1)?.toLongOrNull()
        if (cur != null && max != null && max > 0) cur to max else null
    }
    val freqFrac = if (freqs.isNotEmpty())
        (freqs.map { it.first }.average() / freqs.maxOf { it.second }).toFloat().coerceIn(0f, 1f) else null
    val freqMhz = if (freqs.isNotEmpty()) (freqs.map { it.first }.average() / 1000).toInt() else null
    val cores = lines("CORES").firstOrNull()?.toIntOrNull()
        ?: lines("FREQ").size.takeIf { it > 0 }

    // Memori
    var mt: Long? = null
    var ma: Long? = null
    for (l in lines("MEM")) {
        if (l.startsWith("MemTotal:")) mt = l.filter { it.isDigit() }.toLongOrNull()
        if (l.startsWith("MemAvailable:")) ma = l.filter { it.isDigit() }.toLongOrNull()
    }

    // Load
    val upCmd = lines("UPCMD").joinToString(" ")
    val load = lines("LOAD").firstOrNull { LOAD_RE.matches(it) }?.split(" ")?.take(3)?.joinToString(" ")
        ?: UPCMD_LOAD_RE.find(upCmd)?.let { "${it.groupValues[1]} ${it.groupValues[2]} ${it.groupValues[3]}" }

    // Uptime
    val upSec = lines("UPTIME").firstOrNull()?.substringBefore('.')?.toLongOrNull()
    val upText = UPCMD_UP_RE.find(upCmd)?.groupValues?.get(1)

    // Disk
    fun df(k: String): List<String>? =
        lines(k).lastOrNull()?.split(WS)?.takeIf { it.size >= 6 && it[4].endsWith("%") }
    val d = df("DF") ?: df("DFH")

    // Jaringan
    var rx = 0L
    var tx = 0L
    var hasNet = false
    for (l in lines("NET")) {
        if ('|' in l || ':' !in l) continue
        val name = l.substringBefore(':').trim()
        if (name == "lo") continue
        val f = l.substringAfter(':').trim().split(WS).mapNotNull { it.toLongOrNull() }
        if (f.size >= 9) { rx += f[0]; tx += f[8]; hasNet = true }
    }
    if (!hasNet) {
        for (l in lines("SYSNET")) {
            val t = l.split(WS)
            val a = t.getOrNull(1)?.toLongOrNull()
            val b = t.getOrNull(2)?.toLongOrNull()
            if (a != null && b != null) { rx += a; tx += b; hasNet = true }
        }
    }

    // Baterai (termux-api)
    val bat = lines("BAT").joinToString(" ")
    val batPct = Regex(""""percentage"\s*:\s*(\d+)""").find(bat)?.groupValues?.get(1)?.toIntOrNull()
    val batStatus = Regex(""""status"\s*:\s*"(\w+)"""").find(bat)?.groupValues?.get(1)
    val batTemp = Regex(""""temperature"\s*:\s*([\d.]+)""").find(bat)?.groupValues?.get(1)?.toFloatOrNull()

    return RawSample(
        cpuTotal = cpuT, cpuIdle = cpuI,
        freqFrac = freqFrac, freqMhz = freqMhz, cores = cores,
        memTotalKb = mt, memAvailKb = ma,
        load = load,
        uptimeSec = upSec, uptimeText = upText,
        diskPct = d?.get(4)?.removeSuffix("%")?.toIntOrNull(),
        diskUsedKb = d?.get(2)?.toLongOrNull(),
        diskTotalKb = d?.get(1)?.toLongOrNull(),
        diskMount = d?.drop(5)?.joinToString(" "),
        rx = if (hasNet) rx else null, tx = if (hasNet) tx else null,
        batPct = batPct, batStatus = batStatus, batTemp = batTemp,
        host = lines("HOST").firstOrNull()
    )
}

private fun buildMetrics(cur: RawSample, prev: RawSample?): Metrics {
    var cpu: Float? = null
    var rxBps: Long? = null
    var txBps: Long? = null
    if (prev != null) {
        val ct = cur.cpuTotal; val ci = cur.cpuIdle
        val pt = prev.cpuTotal; val pi = prev.cpuIdle
        if (ct != null && ci != null && pt != null && pi != null) {
            val dT = ct - pt
            val dI = ci - pi
            if (dT > 0) cpu = ((dT - dI).toFloat() / dT).coerceIn(0f, 1f)
        }
        val secs = (cur.at - prev.at) / 1000.0
        val crx = cur.rx; val ctx = cur.tx; val prx = prev.rx; val ptx = prev.tx
        if (secs > 0 && crx != null && ctx != null && prx != null && ptx != null) {
            rxBps = ((crx - prx) / secs).toLong().coerceAtLeast(0)
            txBps = ((ctx - ptx) / secs).toLong().coerceAtLeast(0)
        }
    }
    val total = cur.memTotalKb
    val avail = cur.memAvailKb
    return Metrics(
        host = cur.host ?: "server",
        cpu = cpu,
        cpuFreqFrac = cur.freqFrac,
        cpuFreqMhz = cur.freqMhz,
        cores = cur.cores,
        memUsedKb = if (total != null && avail != null) total - avail else null,
        memTotalKb = total,
        load = cur.load,
        uptimeSec = cur.uptimeSec,
        uptimeText = cur.uptimeText,
        diskPct = cur.diskPct,
        diskUsedKb = cur.diskUsedKb,
        diskTotalKb = cur.diskTotalKb,
        diskMount = cur.diskMount,
        rxBps = rxBps,
        txBps = txBps,
        batteryPct = cur.batPct,
        batteryStatus = cur.batStatus,
        batteryTemp = cur.batTemp
    )
}

private fun friendly(e: Exception): String {
    val m = e.message.orEmpty()
    val l = m.lowercase()
    return when {
        "auth" in l -> "Login ditolak. Cek user dan password."
        "refused" in l -> "Koneksi ditolak. Cek port dan pastikan SSH server berjalan."
        "timeout" in l || "timed out" in l -> "Server tidak merespons. Cek IP, port, dan jaringan."
        "unknownhost" in l -> "Host tidak ditemukan."
        else -> m.ifBlank { "Gagal terhubung." }
    }
}

class RexPanelViewModel : ViewModel() {

    var conn by mutableStateOf<ConnState>(ConnState.Idle)
        private set
    var metrics by mutableStateOf<Metrics?>(null)
        private set
    val cpuHistory = mutableStateListOf<Float>()

    val term = TermBuffer(80, 24)

    var ctrl by mutableStateOf(false)
        private set
    var alt by mutableStateOf(false)
        private set
    var shift by mutableStateOf(false)
        private set

    private var ssh: SshSession? = null
    private var monitorJob: Job? = null
    private var cols = 80
    private var rows = 24
    private val incoming = Channel<String>(Channel.UNLIMITED)

    init {
        viewModelScope.launch { for (s in incoming) term.feed(s) }
    }

    fun toggleCtrl() { ctrl = !ctrl }
    fun toggleAlt() { alt = !alt }
    fun toggleShift() { shift = !shift }

    fun connect(command: String, password: String) {
        val target = parseSshCommand(command)
        if (target == null) {
            conn = ConnState.Error("Format salah. Contoh: ssh -p 8022 root@192.168.0.101")
            return
        }
        if (conn is ConnState.Connecting) return
        conn = ConnState.Connecting
        term.reset()
        viewModelScope.launch {
            val session = SshSession()
            try {
                session.connect(
                    target, password, cols, rows,
                    onText = { incoming.trySend(it) },
                    onClosed = { viewModelScope.launch { onClosed(session) } }
                )
                ssh = session
                conn = ConnState.Connected(target)
                startMonitor(session)
            } catch (e: Exception) {
                session.close()
                conn = ConnState.Error(friendly(e))
            }
        }
    }

    private fun onClosed(session: SshSession) {
        if (ssh === session) {
            monitorJob?.cancel()
            ssh = null
            metrics = null
            conn = ConnState.Error("Koneksi terputus.")
        }
    }

    private fun startMonitor(session: SshSession) {
        monitorJob?.cancel()
        cpuHistory.clear()
        monitorJob = viewModelScope.launch {
            var prev: RawSample? = null
            while (isActive) {
                try {
                    val raw = withTimeoutOrNull(8_000) { session.exec(MON_CMD) }
                    if (raw != null) {
                        val sample = parseSample(raw)
                        val m = buildMetrics(sample, prev)
                        metrics = m
                        (m.cpu ?: m.cpuFreqFrac)?.let {
                            cpuHistory.add(it)
                            if (cpuHistory.size > 40) cpuHistory.removeAt(0)
                        }
                        prev = sample
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
                delay(2_000)
            }
        }
    }

    fun disconnect() {
        monitorJob?.cancel()
        ssh?.close()
        ssh = null
        metrics = null
        conn = ConnState.Idle
    }

    fun resize(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return
        cols = newCols; rows = newRows
        term.resize(newCols, newRows)
        ssh?.resize(newCols, newRows)
    }

    /* ------------------------------- input ------------------------------ */

    private fun clearMods() { ctrl = false; alt = false; shift = false }

    fun sendRaw(s: String) {
        clearMods()
        ssh?.send(s.toByteArray(Charsets.UTF_8))
    }

    fun sendText(text: String) {
        if (text.isEmpty()) return
        var s = text
        val c = s[0]
        if (ctrl) {
            val lc = c.lowercaseChar()
            val code = when {
                lc in 'a'..'z' -> lc.code - 96
                c == '[' -> 27
                c == '\\' -> 28
                c == ']' -> 29
                c == '^' -> 30
                c == '_' -> 31
                c == ' ' || c == '@' -> 0
                else -> -1
            }
            if (code >= 0) s = code.toChar().toString() + s.drop(1)
        } else if (shift) {
            s = c.uppercaseChar().toString() + s.drop(1)
        }
        if (alt) s = "\u001B" + s
        sendRaw(s)
    }

    /** Panah, Home, End. final: A=atas B=bawah C=kanan D=kiri H=home F=end */
    fun sendCursor(final: Char) {
        val mod = 1 + (if (shift) 1 else 0) + (if (alt) 2 else 0) + (if (ctrl) 4 else 0)
        sendRaw(if (mod == 1) "\u001B[$final" else "\u001B[1;$mod$final")
    }

    /** PgUp=5, PgDn=6, Delete=3 */
    fun sendTilde(n: Int) = sendRaw("\u001B[$n~")

    override fun onCleared() {
        monitorJob?.cancel()
        ssh?.close()
        super.onCleared()
    }
}
