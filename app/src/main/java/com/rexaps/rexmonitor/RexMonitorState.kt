package com.rexaps.rexmonitor

import com.rexaps.rexmonitor.booster.RexOptimizationResult
import com.rexaps.rexmonitor.device.RexBatteryInfo
import com.rexaps.rexmonitor.device.RexCpuInfo
import com.rexaps.rexmonitor.device.RexDeviceInfo
import com.rexaps.rexmonitor.device.RexGpuInfo
import com.rexaps.rexmonitor.device.RexNetworkInfo
import com.rexaps.rexmonitor.device.RexRamInfo
import com.rexaps.rexmonitor.device.RexStorageInfo
import com.rexaps.rexmonitor.device.RexSystemInfo
import com.rexaps.rexmonitor.device.RexTemperatureInfo
import com.rexaps.rexmonitor.device.RexThermalLevel
import com.rexaps.rexmonitor.monitoring.RexServiceStatus

/** Satu snapshot metrik hasil pembacaan perangkat. Nilai null = tidak tersedia. */
data class RexMetrics(
    val device: RexDeviceInfo = RexDeviceInfo(),
    val cpu: RexCpuInfo = RexCpuInfo(),
    val gpu: RexGpuInfo = RexGpuInfo(),
    val ram: RexRamInfo = RexRamInfo(),
    val storage: RexStorageInfo = RexStorageInfo(),
    val battery: RexBatteryInfo = RexBatteryInfo(),
    val temperature: RexTemperatureInfo = RexTemperatureInfo(),
    val network: RexNetworkInfo = RexNetworkInfo(),
    val system: RexSystemInfo = RexSystemInfo(),
    val errors: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
)

enum class RexHealthLevel { NORMAL, WARM, ATTENTION }

data class RexHealth(
    val level: RexHealthLevel = RexHealthLevel.NORMAL,
    val label: String = "Perangkat dalam kondisi normal",
    val reasons: List<String> = emptyList()
)

/** Status dihitung dari kondisi aktual, bukan selalu "Normal". */
fun computeHealth(m: RexMetrics): RexHealth {
    val reasons = mutableListOf<String>()
    var level = RexHealthLevel.NORMAL
    var thermalCause = false

    fun raise(to: RexHealthLevel, reason: String, thermal: Boolean = false) {
        reasons += reason
        if (to.ordinal > level.ordinal) level = to
        if (thermal) thermalCause = true
    }

    when (m.temperature.thermalLevel) {
        RexThermalLevel.LIGHT, RexThermalLevel.MODERATE ->
            raise(RexHealthLevel.WARM, "Status termal Android: sedang naik", true)
        RexThermalLevel.SEVERE, RexThermalLevel.CRITICAL,
        RexThermalLevel.EMERGENCY, RexThermalLevel.SHUTDOWN ->
            raise(RexHealthLevel.ATTENTION, "Status termal Android: tinggi", true)
        else -> Unit
    }
    m.battery.temperatureC?.let {
        when {
            it >= 48f -> raise(RexHealthLevel.ATTENTION, "Suhu baterai sangat tinggi", true)
            it >= 42f -> raise(RexHealthLevel.WARM, "Suhu baterai tinggi", true)
            else -> Unit
        }
    }
    if (m.ram.lowMemory) raise(RexHealthLevel.ATTENTION, "Memori rendah")
    m.storage.usedPercent?.let {
        when {
            it >= 97f -> raise(RexHealthLevel.ATTENTION, "Penyimpanan hampir penuh")
            it >= 90f -> raise(RexHealthLevel.WARM, "Penyimpanan hampir penuh")
            else -> Unit
        }
    }
    val pct = m.battery.percent
    if (pct != null && pct <= 10 && m.battery.isCharging == false) {
        raise(RexHealthLevel.WARM, "Baterai hampir habis")
    }

    val label = when (level) {
        RexHealthLevel.NORMAL -> "Perangkat dalam kondisi normal"
        RexHealthLevel.WARM -> if (thermalCause) "Perangkat sedang panas" else "Ada hal yang perlu dicek"
        RexHealthLevel.ATTENTION -> "Perlu perhatian"
    }
    return RexHealth(level, label, reasons)
}

enum class RexNotificationPermission { UNKNOWN, GRANTED, DENIED }

enum class RexPermissionDialog { NONE, RATIONALE, DENIED }

enum class RexBoosterPhase { IDLE, RUNNING, DONE }

data class RexBoosterState(
    val phase: RexBoosterPhase = RexBoosterPhase.IDLE,
    val result: RexOptimizationResult? = null,
    val failed: Boolean = false
)

/** Single source of truth untuk UI. */
data class RexMonitorState(
    val device: RexDeviceInfo = RexDeviceInfo(),
    val cpu: RexCpuInfo = RexCpuInfo(),
    val gpu: RexGpuInfo = RexGpuInfo(),
    val ram: RexRamInfo = RexRamInfo(),
    val storage: RexStorageInfo = RexStorageInfo(),
    val battery: RexBatteryInfo = RexBatteryInfo(),
    val temperature: RexTemperatureInfo = RexTemperatureInfo(),
    val network: RexNetworkInfo = RexNetworkInfo(),
    val system: RexSystemInfo = RexSystemInfo(),
    val health: RexHealth = RexHealth(),
    val monitoringStatus: RexServiceStatus = RexServiceStatus.STOPPED,
    val notificationPermission: RexNotificationPermission = RexNotificationPermission.UNKNOWN,
    val permissionDialog: RexPermissionDialog = RexPermissionDialog.NONE,
    val rootAvailable: Boolean = false,
    val rootGranted: Boolean = false,
    val booster: RexBoosterState = RexBoosterState(),
    val loading: Boolean = true,
    val lastUpdatedMs: Long? = null,
    val errors: List<String> = emptyList()
)
