package com.rexaps.rexwarp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.rexaps.rexwarp.data.RexWarpMinuteEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mirror CSV harian ke folder pilihan user (Storage Access Framework).
 * File tetap ada setelah app di-uninstall; setelah install ulang, pilih folder yang sama lalu Restore.
 */
class RexWarpBackupStore private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("rexwarp_backup", Context.MODE_PRIVATE)
    private var cachedDate: LocalDate? = null
    private var cachedUri: Uri? = null

    var recording: Boolean
        get() = prefs.getBoolean(KEY_RECORDING, false)
        set(v) { prefs.edit().putBoolean(KEY_RECORDING, v).apply() }

    fun setTree(uri: Uri) {
        app.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit().putString(KEY_TREE, uri.toString()).apply()
        synchronized(this) { cachedDate = null; cachedUri = null }
    }

    /** Nama folder terpilih, atau null jika belum dipilih / izin hilang. Sebaiknya dipanggil dari IO. */
    fun folderName(): String? = root()?.name

    @Synchronized
    fun append(date: LocalDate, line: String): Boolean {
        val uri = cachedUri.takeIf { cachedDate == date } ?: openFile(date) ?: return false
        return try {
            app.contentResolver.openOutputStream(uri, "wa")?.use { it.write((line + "\n").toByteArray()) } != null
        } catch (e: Exception) {
            cachedUri = null; cachedDate = null
            false
        }
    }

    /** Membaca seluruh CSV. Baris menit yang sama dijumlahkan. Baris rusak dilewati. */
    fun readAll(): List<RexWarpMinuteEntity> {
        val dir = root() ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val map = HashMap<Long, RexWarpMinuteEntity>()
        for (f in dir.listFiles()) {
            if (!f.isFile || f.name?.endsWith(".csv") != true) continue
            val stream = app.contentResolver.openInputStream(f.uri) ?: continue
            stream.bufferedReader().useLines { lines ->
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
        return map.values.toList()
    }

    @Synchronized
    fun deleteDay(date: LocalDate) {
        root()?.findFile("$date.csv")?.delete()
        cachedDate = null; cachedUri = null
    }

    @Synchronized
    fun deleteAll() {
        root()?.listFiles()?.forEach { if (it.isFile && it.name?.endsWith(".csv") == true) it.delete() }
        cachedDate = null; cachedUri = null
    }

    private fun openFile(date: LocalDate): Uri? {
        val dir = root() ?: return null
        val name = "$date.csv"
        val file = dir.findFile(name)
            ?: (dir.createFile("text/csv", name)?.also { writeHeader(it.uri) })
            ?: return null
        cachedDate = date; cachedUri = file.uri
        return file.uri
    }

    private fun writeHeader(uri: Uri) {
        runCatching {
            app.contentResolver.openOutputStream(uri, "wa")?.use { it.write("time,downloadBytes,uploadBytes,warp\n".toByteArray()) }
        }
    }

    private fun root(): DocumentFile? {
        val uri = prefs.getString(KEY_TREE, null)?.let(Uri::parse) ?: return null
        val granted = app.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        if (!granted) return null
        return DocumentFile.fromTreeUri(app, uri)?.takeIf { it.exists() && it.canWrite() }
    }

    companion object {
        private const val KEY_RECORDING = "recording"
        private const val KEY_TREE = "tree_uri"
        @Volatile private var instance: RexWarpBackupStore? = null
        fun get(context: Context): RexWarpBackupStore = instance ?: synchronized(this) {
            instance ?: RexWarpBackupStore(context).also { instance = it }
        }
    }
}