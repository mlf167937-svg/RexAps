package com.rexaps.rexpanel

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
internal val MON_CMD = """
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

internal val UPCMD_LOAD_RE =
    Regex("""load averages?:\s*([\d.]+),?\s+([\d.]+),?\s+([\d.]+)""")

internal val UPCMD_UP_RE =
    Regex("""up\s+(.+?),\s+(?:\d+\s+users?|load)""")

internal val WS = Regex("\\s+")

internal val TOP_TOYBOX =
    Regex("""(\d+)%cpu.*?(\d+)%idle""", RegexOption.IGNORE_CASE)

internal val TOP_LINUX =
    Regex("""(\d+(?:[.,]\d+)?)\s*id\b""")

internal val TOP_BUSY =
    Regex("""(\d+)%\s*idle""", RegexOption.IGNORE_CASE)

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

internal val LOAD_RE =
    Regex("""^\d+\.\d+ \d+\.\d+ \d+\.\d+ \d+/\d+ \d+$""")

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
