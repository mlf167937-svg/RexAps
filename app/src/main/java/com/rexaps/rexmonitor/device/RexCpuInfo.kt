package com.rexaps.rexmonitor.device

import android.os.Build
import com.rexaps.rexmonitor.booster.RexRootChecker
import java.io.File

data class RexCpuInfo(
    val model: String? = null,
    val architecture: String? = null,
    val coreCount: Int = 0,
    /** Frekuensi tertinggi di antara core yang dapat dibaca (kHz). */
    val currentFreqKhz: Long? = null,
    val minFreqKhz: Long? = null,
    val maxFreqKhz: Long? = null,
    val perCoreFreqKhz: List<Long?> = emptyList(),
    /** null = tidak dapat dihitung secara andal pada perangkat ini. */
    val usagePercent: Float? = null,
    val usageSource: String? = null
)

class RexCpuReader(private val rootChecker: RexRootChecker) {
    @Volatile
    var allowRoot: Boolean = false

    private var prevTotal = -1L
    private var prevIdle = -1L
    private var lastUsage: Float? = null

    private val staticModel: String? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val maker = Build.SOC_MANUFACTURER.takeUnless { it.isBlank() || it == Build.UNKNOWN }
            val soc = Build.SOC_MODEL.takeUnless { it.isBlank() || it == Build.UNKNOWN }
            listOfNotNull(maker, soc).joinToString(" ").ifBlank { null }
                ?: Build.HARDWARE.takeUnless { it.isBlank() || it == Build.UNKNOWN }
        } else {
            Build.HARDWARE.takeUnless { it.isBlank() || it == Build.UNKNOWN }
        }
    }

    fun read(): RexCpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val cur = List(cores) { readLong("/sys/devices/system/cpu/cpu$it/cpufreq/scaling_cur_freq") }
        val mins = List(cores) { readLong("/sys/devices/system/cpu/cpu$it/cpufreq/cpuinfo_min_freq") }
        val maxs = List(cores) { readLong("/sys/devices/system/cpu/cpu$it/cpufreq/cpuinfo_max_freq") }
        val (usage, source) = readUsage()
        return RexCpuInfo(
            model = staticModel,
            architecture = Build.SUPPORTED_ABIS?.firstOrNull(),
            coreCount = cores,
            currentFreqKhz = cur.filterNotNull().maxOrNull(),
            minFreqKhz = mins.filterNotNull().minOrNull(),
            maxFreqKhz = maxs.filterNotNull().maxOrNull(),
            perCoreFreqKhz = cur,
            usagePercent = usage,
            usageSource = source
        )
    }

    private fun readUsage(): Pair<Float?, String?> {
        var source = "/proc/stat"
        var line = runCatching { File("/proc/stat").useLines { it.firstOrNull() } }.getOrNull()
        if (line == null || !line.startsWith("cpu ")) {
            line = if (allowRoot) rootChecker.readProcStatLine() else null
            source = "root"
        }
        if (line == null || !line.startsWith("cpu ")) {
            prevTotal = -1L
            return null to null
        }
        val f = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (f.size < 4) return null to null
        val idle = f[3] + f.getOrElse(4) { 0L }
        val total = f.take(8).sum()
        val usage = if (prevTotal >= 0 && total > prevTotal) {
            val dTotal = (total - prevTotal).toFloat()
            val dIdle = (idle - prevIdle).toFloat()
            ((dTotal - dIdle) / dTotal * 100f).coerceIn(0f, 100f)
        } else null
        prevTotal = total
        prevIdle = idle
        if (usage != null) lastUsage = usage
        return (usage ?: lastUsage) to source
    }

    private fun readLong(path: String): Long? =
        runCatching { File(path).readText().trim().toLongOrNull() }.getOrNull()
}
