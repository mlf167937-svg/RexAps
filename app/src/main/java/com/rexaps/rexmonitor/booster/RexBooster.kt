package com.rexaps.rexmonitor.booster

import android.content.Context
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexBatteryReader
import com.rexaps.rexmonitor.device.RexRamReader
import com.rexaps.rexmonitor.device.RexStorageReader
import com.rexaps.rexmonitor.device.RexTemperatureReader
import com.rexaps.rexmonitor.device.RexThermalLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Booster nyata dan aman: hanya menyentuh data milik RexAps sendiri (cache lama > 24 jam),
 * lalu memeriksa kondisi perangkat dan melaporkan perubahan yang BENAR-BENAR terukur.
 * Tidak mematikan aplikasi lain, tidak memanipulasi CPU/GPU, tidak mengklaim persentase boost.
 */
class RexBooster(context: Context) {
    private val app = context.applicationContext
    private val ramReader = RexRamReader(app)
    private val batteryReader = RexBatteryReader(app)
    private val storageReader = RexStorageReader()
    private val tempReader = RexTemperatureReader(app)

    suspend fun optimize(rootGranted: Boolean): RexOptimizationResult = withContext(Dispatchers.IO) {
        val ramBefore = ramReader.read()
        val batBefore = batteryReader.read()

        val checks = mutableListOf<RexCheck>()
        val recommendations = mutableListOf<String>()

        // 1. Cache lama milik RexAps sendiri.
        val freed = deleteStale(app.cacheDir) + deleteStale(app.externalCacheDir)
        checks += if (freed > 0) {
            RexCheck("Cache RexAps", RexCheckStatus.APPLIED, "Membersihkan ${RexFormat.bytes(freed)} cache lama milik RexAps")
        } else {
            RexCheck("Cache RexAps", RexCheckStatus.OK, "Tidak ada cache lama yang perlu dibersihkan")
        }

        // 2. Kesehatan penyimpanan.
        val storage = storageReader.read()
        val used = storage.usedPercent
        var suggestStorage = false
        if (used != null && used >= 90f) {
            checks += RexCheck("Penyimpanan", RexCheckStatus.WARNING, "Terpakai ${used.toInt()}% — hampir penuh")
            recommendations += "Penyimpanan hampir penuh. Hapus file/aplikasi yang tidak dipakai lewat Pengaturan."
            suggestStorage = true
        } else if (used != null) {
            checks += RexCheck("Penyimpanan", RexCheckStatus.OK, "Terpakai ${used.toInt()}% — sehat")
        } else {
            checks += RexCheck("Penyimpanan", RexCheckStatus.INFO, "Tidak tersedia")
        }

        // 3. Pemeriksaan termal.
        val tempNow = tempReader.read(batBefore.temperatureC)
        val hot = tempNow.thermalLevel.let {
            it != null && it >= RexThermalLevel.MODERATE
        } || (batBefore.temperatureC ?: 0f) >= 42f
        if (hot) {
            checks += RexCheck("Termal", RexCheckStatus.WARNING, "Suhu perangkat tinggi")
            recommendations += "Device temperature is high. Performance may be limited by Android thermal protection."
        } else {
            checks += RexCheck("Termal", RexCheckStatus.OK, "Suhu dalam batas normal")
        }

        // 4. Memori.
        if (ramBefore.lowMemory) {
            checks += RexCheck("Memori", RexCheckStatus.WARNING, "Sistem melaporkan memori rendah")
            recommendations += "Tutup aplikasi yang tidak dipakai secara manual dari layar recent apps."
        } else {
            checks += RexCheck("Memori", RexCheckStatus.OK, "Memori tersedia mencukupi")
        }

        if (rootGranted) {
            checks += RexCheck(
                "Mode lanjutan", RexCheckStatus.INFO,
                "Root aktif — hanya dipakai untuk membaca statistik CPU, tidak mengubah sistem"
            )
        }

        val ramAfter = ramReader.read()
        val batAfter = batteryReader.read()

        RexOptimizationResult(
            checks = checks,
            freedBytes = freed,
            ramBeforeAvailableBytes = ramBefore.availableBytes,
            ramAfterAvailableBytes = ramAfter.availableBytes,
            batteryTempBeforeC = batBefore.temperatureC,
            batteryTempAfterC = batAfter.temperatureC,
            recommendations = recommendations,
            suggestStorageSettings = suggestStorage
        )
    }

    private fun deleteStale(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        val cutoff = System.currentTimeMillis() - STALE_AFTER_MS
        var freed = 0L
        runCatching {
            dir.walkBottomUp().forEach { f ->
                if (f.isFile && f.lastModified() < cutoff) {
                    val size = f.length()
                    if (f.delete()) freed += size
                }
            }
        }
        return freed
    }

    private companion object {
        const val STALE_AFTER_MS = 24L * 60 * 60 * 1000
    }
}
