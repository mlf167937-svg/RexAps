package com.rexaps.rexmonitor.device

import android.content.Context
import android.os.Build
import android.os.PowerManager
import java.io.File
import kotlin.math.abs

enum class RexThermalLevel { NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN }

data class RexTemperatureInfo(
    val cpuC: Float? = null,
    val gpuC: Float? = null,
    val batteryC: Float? = null,
    val cpuSource: String? = null,
    val gpuSource: String? = null,
    val thermalLevel: RexThermalLevel? = null,
    val cpuHistory: List<Float> = emptyList(),
    val gpuHistory: List<Float> = emptyList(),
    val batteryHistory: List<Float> = emptyList()
)

/**
 * CPU/GPU hanya diisi bila thermal zone sistem benar-benar dapat dibaca.
 * Suhu baterai TIDAK pernah dipakai sebagai suhu CPU/GPU.
 */
class RexTemperatureReader(context: Context) {
    private val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private class Zone(val file: File, val type: String)

    private val cpuZones: List<Zone>
    private val gpuZones: List<Zone>

    private val cpuHist = ArrayDeque<Float>()
    private val gpuHist = ArrayDeque<Float>()
    private val batHist = ArrayDeque<Float>()

    init {
        val all = runCatching {
            File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
                ?.mapNotNull { dir ->
                    val type = runCatching { File(dir, "type").readText().trim() }.getOrNull()
                        ?: return@mapNotNull null
                    val temp = File(dir, "temp")
                    if (parse(temp) == null) null else Zone(temp, type)
                }.orEmpty()
        }.getOrDefault(emptyList())
        cpuZones = all.filter { it.type.contains("cpu", ignoreCase = true) }
        gpuZones = all.filter { it.type.contains("gpu", ignoreCase = true) }
    }

    fun read(batteryC: Float?): RexTemperatureInfo {
        val (cpu, cpuSrc) = hottest(cpuZones)
        val (gpu, gpuSrc) = hottest(gpuZones)
        push(cpuHist, cpu)
        push(gpuHist, gpu)
        push(batHist, batteryC)
        return RexTemperatureInfo(
            cpuC = cpu,
            gpuC = gpu,
            batteryC = batteryC,
            cpuSource = cpuSrc,
            gpuSource = gpuSrc,
            thermalLevel = thermalLevel(),
            cpuHistory = cpuHist.toList(),
            gpuHistory = gpuHist.toList(),
            batteryHistory = batHist.toList()
        )
    }

    private fun push(h: ArrayDeque<Float>, v: Float?) {
        if (v == null) return
        h.addLast(v)
        while (h.size > MAX_HISTORY) h.removeFirst()
    }

    private fun hottest(zones: List<Zone>): Pair<Float?, String?> {
        var best: Float? = null
        var src: String? = null
        for (z in zones) {
            val v = parse(z.file) ?: continue
            if (best == null || v > best) {
                best = v
                src = z.type
            }
        }
        return best to src
    }

    private fun parse(file: File): Float? {
        val raw = runCatching { file.readText().trim().toFloatOrNull() }.getOrNull() ?: return null
        val c = when {
            abs(raw) >= 1000f -> raw / 1000f
            abs(raw) > 150f -> raw / 10f
            else -> raw
        }
        return c.takeIf { it > 0f && it < 130f }
    }

    private fun thermalLevel(): RexThermalLevel? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return when (pm?.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> RexThermalLevel.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> RexThermalLevel.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> RexThermalLevel.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> RexThermalLevel.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> RexThermalLevel.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> RexThermalLevel.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> RexThermalLevel.SHUTDOWN
            else -> null
        }
    }

    private companion object {
        const val MAX_HISTORY = 30
    }
}
