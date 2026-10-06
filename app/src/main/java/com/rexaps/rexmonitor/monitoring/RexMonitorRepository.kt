package com.rexaps.rexmonitor.monitoring

import android.content.Context
import com.rexaps.rexmonitor.RexMetrics
import com.rexaps.rexmonitor.booster.RexRootChecker
import com.rexaps.rexmonitor.device.RexBatteryInfo
import com.rexaps.rexmonitor.device.RexBatteryReader
import com.rexaps.rexmonitor.device.RexCpuInfo
import com.rexaps.rexmonitor.device.RexCpuReader
import com.rexaps.rexmonitor.device.RexDeviceInfo
import com.rexaps.rexmonitor.device.RexDeviceInfoReader
import com.rexaps.rexmonitor.device.RexGpuInfo
import com.rexaps.rexmonitor.device.RexGpuReader
import com.rexaps.rexmonitor.device.RexNetworkInfo
import com.rexaps.rexmonitor.device.RexNetworkReader
import com.rexaps.rexmonitor.device.RexRamInfo
import com.rexaps.rexmonitor.device.RexRamReader
import com.rexaps.rexmonitor.device.RexStorageInfo
import com.rexaps.rexmonitor.device.RexStorageReader
import com.rexaps.rexmonitor.device.RexSystemInfo
import com.rexaps.rexmonitor.device.RexSystemReader
import com.rexaps.rexmonitor.device.RexTemperatureInfo
import com.rexaps.rexmonitor.device.RexTemperatureReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Abstraction layer: provider perangkat -> RexMetrics. Satu provider gagal tidak
 * menjatuhkan provider lain; nilainya menjadi default (null = Unavailable).
 * Instance ini memegang sampel sebelumnya (CPU/traffic), jadi pakai satu instance per konsumen.
 */
class RexMonitorRepository(context: Context) {
    private val app = context.applicationContext

    val rootChecker = RexRootChecker()

    private val deviceReader = RexDeviceInfoReader()
    private val cpuReader = RexCpuReader(rootChecker)
    private val gpuReader = RexGpuReader(app)
    private val ramReader = RexRamReader(app)
    private val storageReader = RexStorageReader()
    private val batteryReader = RexBatteryReader(app)
    private val tempReader = RexTemperatureReader(app)
    private val networkReader = RexNetworkReader(app)
    private val systemReader = RexSystemReader(app)

    private val staticDevice: RexDeviceInfo by lazy { deviceReader.read() }

    fun setRootAllowed(allowed: Boolean) {
        cpuReader.allowRoot = allowed
    }

    fun snapshot(): RexMetrics {
        val errors = mutableListOf<String>()
        fun <T> safe(name: String, default: T, block: () -> T): T =
            try {
                block()
            } catch (e: Exception) {
                errors += "$name: ${e.javaClass.simpleName}"
                default
            }

        val battery = safe("Battery", RexBatteryInfo()) { batteryReader.read() }
        return RexMetrics(
            device = safe("Device", RexDeviceInfo()) { staticDevice },
            cpu = safe("CPU", RexCpuInfo()) { cpuReader.read() },
            gpu = safe("GPU", RexGpuInfo()) { gpuReader.read() },
            ram = safe("RAM", RexRamInfo()) { ramReader.read() },
            storage = safe("Storage", RexStorageInfo()) { storageReader.read() },
            battery = battery,
            temperature = safe("Temperature", RexTemperatureInfo()) { tempReader.read(battery.temperatureC) },
            network = safe("Network", RexNetworkInfo()) { networkReader.read() },
            system = safe("System", RexSystemInfo()) { systemReader.read() },
            errors = errors,
            timestampMs = System.currentTimeMillis()
        )
    }

    /** Cold flow: polling berhenti otomatis saat kolektor dibatalkan. */
    fun stream(intervalMs: Long): Flow<RexMetrics> = flow {
        while (true) {
            emit(snapshot())
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.Default)

    fun close() {
        rootChecker.close()
    }
}
