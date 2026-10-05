package com.rexaps.rexwarp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.rexaps.rexwarp.data.RexWarpMinuteEntity
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * CSV harian disimpan OTOMATIS di:
 *   /storage/emulated/0/Download/RexAps/RexWARP/data/YYYY-MM-DD.csv
 * Tanpa folder picker. Folder dibuat sendiri. Tetap ada setelah uninstall.
 * Android 11+ butuh izin "Akses semua file" (MANAGE_EXTERNAL_STORAGE), Android 10- butuh WRITE_EXTERNAL_STORAGE.
 */
class RexWarpBackupStore private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("rexwarp_backup", Context.MODE_PRIVATE)

    var recording: Boolean
        get() = prefs.getBoolean(KEY_RECORDING, false)
        set(v) { prefs.edit().putBoolean(KEY_RECORDING, v).apply() }

    /** Lokasi tetap folder data. */
    val dir: File
        get() = File(Environment.getExternalStorageDirectory(), "Download/RexAps/RexWARP/data")

    fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(app, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    /** Path untuk ditampilkan di UI, atau null jika izin belum diberikan. Panggil dari IO. */
    fun folderName(): String? = if (hasAccess() && ensureDir()) dir.absolutePath else null

    private fun ensureDir(): Boolean = runCatching { dir.isDirectory || dir.mkdirs() }.getOrDefault(false)

    @Synchronized
    fun append(date: LocalDate, line: String): Boolean {
        if (!hasAccess() || !ensureDir()) return false
        return runCatching {
            val f = File(dir, "$date.csv")
            val isNew = !f.exists() || f.length() == 0L
            f.appendText((if (isNew) HEADER else "") + line + "\n")
            true
        }.getOrDefault(false)
    }

    /** Membaca seluruh CSV. Baris menit yang sama dijumlahkan. Baris rusak dilewati. */
    fun readAll(): List<RexWarpMinuteEntity> {
        if (!hasAccess() || !dir.isDirectory) return emptyList()
        val zone = ZoneId.systemDefault()
        val map = HashMap<Long, RexWarpMinuteEntity>()
        for (f in dir.listFiles().orEmpty()) {
            if (!f.isFile || !f.name.endsWith(".csv")) continue
            runCatching {
                f.bufferedReader().useLines { lines ->
                    lines.forEach { raw ->
                        val p = raw.split(',')
                        if (p.size < 3) return@forEach
                        val time = runCatching { LocalDateTime.parse(p[0].trim()) }.getOrNull() ?: return@forEach
                        val dl = p[1].trim().toLongOrNull() ?: return@forEach
                        val ul = p[2].trim().toLongOrNull() ?: return@forEach
                        if (dl < 0 || ul < 0) return@forEach
                        val warp = if (p.getOrNull(3)?.trim() == "1") 1 else 0
                        val minute = time.atZone(zone).toEpochSecond() / 60
                        val old = map[minute]
                        map[minute] = if (old == null) RexWarpMinuteEntity(minute, dl, ul, warp)
                        else RexWarpMinuteEntity(minute, old.downloadBytes + dl, old.uploadBytes + ul, maxOf(old.warp, warp))
                    }
                }
            }
        }
        return map.values.toList()
    }

    @Synchronized
    fun deleteDay(date: LocalDate) { if (hasAccess()) runCatching { File(dir, "$date.csv").delete() } }

    @Synchronized
    fun deleteAll() {
        if (!hasAccess()) return
        dir.listFiles()?.forEach { if (it.isFile && it.name.endsWith(".csv")) it.delete() }
    }

    companion object {
        private const val KEY_RECORDING = "recording"
        private const val HEADER = "time,downloadBytes,uploadBytes,warp\n"
        @Volatile private var instance: RexWarpBackupStore? = null
        fun get(context: Context): RexWarpBackupStore = instance ?: synchronized(this) {
            instance ?: RexWarpBackupStore(context).also { instance = it }
        }
    }
}
