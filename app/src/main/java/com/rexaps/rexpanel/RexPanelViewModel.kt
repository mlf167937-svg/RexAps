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
    val memUsedKb: Long?,
    val memTotalKb: Long?,
    val load: String?,
    val uptimeSec: Long?,
    val diskPct: Int?,
    val diskUsedKb: Long?,
    val diskTotalKb: Long?,
    val rxBps: Long?,
    val txBps: Long?
)

class RawSample(
    val cpuTotal: Long?, val cpuIdle: Long?,
    val memTotalKb: Long?, val memAvailKb: Long?,
    val load: String?, val uptimeSec: Long?,
    val diskPct: Int?, val diskUsedKb: Long?, val diskTotalKb: Long?,
    val rx: Long?, val tx: Long?,
    val host: String?,
    val at: Long = System.currentTimeMillis()
)

private const val MON_CMD =
    "head -n1 /proc/stat; grep -E '^(MemTotal|MemAvailable):' /proc/meminfo; " +
        "cat /proc/loadavg; cat /proc/uptime; df -P / | tail -n1; cat /proc/net/dev; echo HOST=\$(hostname 2>/dev/null)"

private val LOAD_RE = Regex("""^\d+\.\d+ \d+\.\d+ \d+\.\d+ \d+/\d+ \d+$""")
private val UP_RE = Regex("""^\d+\.\d+ \d+\.\d+$""")
private val WS = Regex("\\s+")

fun parseSample(raw: String): RawSample {
    var cpuT: Long? = null; var cpuI: Long? = null
    var mt: Long? = null; var ma: Long? = null
    var load: String? = null; var up: Long? = null
    var dp: Int? = null; var du: Long? = null; var dt: Long? = null
    var rx = 0L; var tx = 0L; var hasNet = false
    var host: String? = null

    for (line in raw.lines().map { it.trim() }) {
        when {
            line.startsWith("cpu ") -> {
                val n = line.split(WS).drop(1).mapNotNull { it.toLongOrNull() }
                if (n.size >= 5) { cpuT = n.take(8).sum(); cpuI = n[3] + n[4] }
            }
            line.startsWith("MemTotal:") -> mt = line.filter { it.isDigit() }.toLongOrNull()
            line.startsWith("MemAvailable:") -> ma = line.filter { it.isDigit() }.toLongOrNull()
            line.startsWith("HOST=") -> host = line.removePrefix("HOST=").ifBlank { null }
            LOAD_RE.matches(line) -> load = line.split(" ").take(3).joinToString(" ")
            UP_RE.matches(line) -> up = line.substringBefore('.').toLongOrNull()
            line.endsWith(" /") && line.contains('%') -> {
                val t = line.split(WS)
                if (t.size >= 6) {
                    dt = t[1].toLongOrNull(); du = t[2].toLongOrNull()
                    dp = t[4].removeSuffix("%").toIntOrNull()
                }
            }
            line.contains(':') && !line.contains('|') -> {
                val name = line.substringBefore(':').trim()
                if (name != "lo" && !name.contains(' ')) {
                    val f = line.substringAfter(':').trim().split(WS).mapNotNull { it.toLongOrNull() }
                    if (f.size >= 9) { rx += f[0]; tx += f[8]; hasNet = true }
                }
            }
        }
    }
    return RawSample(
        cpuT, cpuI, mt, ma, load, up, dp, du, dt,
        if (hasNet) rx else null, if (hasNet) tx else null, host
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
        memUsedKb = if (total != null && avail != null) total - avail else null,
        memTotalKb = total,
        load = cur.load,
        uptimeSec = cur.uptimeSec,
        diskPct = cur.diskPct,
        diskUsedKb = cur.diskUsedKb,
        diskTotalKb = cur.diskTotalKb,
        rxBps = rxBps,
        txBps = txBps
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
                        m.cpu?.let {
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
