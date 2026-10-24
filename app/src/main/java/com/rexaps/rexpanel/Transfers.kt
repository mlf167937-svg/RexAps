package com.rexaps.rexpanel

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

const val DOWNLOAD_SUBDIR = "RexAps/RexPanel"
const val DOWNLOAD_LABEL = "Download/RexAps/RexPanel"

data class TransferUi(
    val title: String,
    val index: Int,
    val count: Int,
    val name: String,
    val done: Long,
    val total: Long
)

/** Mengelola unduhan (ke /sdcard/Download/RexAps/RexPanel) dan unggahan (ke server). */
@Stable
class TransferManager(
    private val sftp: SftpClient,
    private val scope: CoroutineScope,
    private val context: Context,
    private val onUploaded: () -> Unit,
    private val onNotice: (String) -> Unit
) {
    var ui by mutableStateOf<TransferUi?>(null)
        private set

    private var job: Job? = null

    fun cancel() { job?.cancel() }

    private fun throttled(): (Long) -> Unit {
        var last = 0L
        return { done ->
            val now = SystemClock.uptimeMillis()
            if (now - last >= 100) {
                last = now
                ui = ui?.copy(done = done)
            }
        }
    }

    /* ------------------------------ unduh ------------------------------ */

    /** @return true jika transfer dimulai. */
    fun download(files: List<RemoteFile>): Boolean {
        if (ui != null) { onNotice("Masih ada transfer yang berjalan."); return false }
        val list = files.filter { !it.isDir }
        val skipped = files.size - list.size
        if (list.isEmpty()) { onNotice("Folder belum bisa diunduh. Pilih berkas."); return false }

        job = scope.launch {
            var ok = 0
            var fail = 0
            var lastError: String? = null
            try {
                list.forEachIndexed { i, f ->
                    ui = TransferUi("Mengunduh", i + 1, list.size, f.name, 0, f.size)
                    try {
                        saveToDownloads(f, throttled())
                        ok++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        currentCoroutineContext().ensureActive()
                        fail++
                        lastError = sftpMessage(e)
                    }
                }
            } catch (e: CancellationException) {
                ui = null
                onNotice("Unduhan dibatalkan ($ok berkas selesai).")
                throw e
            }
            ui = null
            onNotice(buildString {
                if (ok > 0) append("$ok berkas diunduh ke $DOWNLOAD_LABEL")
                if (skipped > 0) append(if (isEmpty()) "" else ". ").append("$skipped folder dilewati")
                if (fail > 0) append(if (isEmpty()) "" else ". ").append("$fail gagal: ${lastError ?: "kesalahan tidak diketahui"}")
            })
        }
        return true
    }

    private fun mimeOf(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private suspend fun saveToDownloads(f: RemoteFile, onProgress: (Long) -> Unit) {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, f.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeOf(f.name))
                put(MediaStore.Downloads.RELATIVE_PATH, DOWNLOAD_LABEL)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Tidak bisa membuat berkas di folder Download.")
            try {
                val out = resolver.openOutputStream(uri) ?: error("Tidak bisa menulis ke folder Download.")
                out.use { sftp.downloadTo(f.path, it, onProgress) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: Throwable) {
                withContext(NonCancellable) { runCatching { resolver.delete(uri, null, null) } }
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(root, DOWNLOAD_SUBDIR)
            if (!dir.exists() && !dir.mkdirs()) {
                error("Tidak bisa membuat folder. Izinkan akses penyimpanan untuk aplikasi ini di Pengaturan.")
            }
            val dest = uniqueLocal(dir, f.name)
            try {
                FileOutputStream(dest).use { out -> sftp.downloadTo(f.path, out, onProgress) }
            } catch (e: Throwable) {
                dest.delete()
                throw e
            }
            MediaScannerConnection.scanFile(context, arrayOf(dest.path), null, null)
        }
    }

    private fun splitName(name: String): Pair<String, String> {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) to name.substring(dot) else name to ""
    }

    private fun uniqueLocal(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val (base, ext) = splitName(name)
        var i = 1
        while (f.exists() && i < 1000) { f = File(dir, "$base ($i)$ext"); i++ }
        return f
    }

    /* ------------------------------ unggah ----------------------------- */

    fun upload(uris: List<Uri>, dir: String): Boolean {
        if (uris.isEmpty()) return false
        if (ui != null) { onNotice("Masih ada transfer yang berjalan."); return false }

        job = scope.launch {
            var ok = 0
            var fail = 0
            var lastError: String? = null
            try {
                uris.forEachIndexed { i, uri ->
                    val (name, size) = withContext(Dispatchers.IO) { queryMeta(uri) }
                    ui = TransferUi("Mengunggah", i + 1, uris.size, name, 0, size)
                    var remote: String? = null
                    try {
                        val target = uniqueRemote(dir, name)
                        remote = target
                        val input = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri) }
                            ?: error("Tidak bisa membaca berkas dari penyimpanan.")
                        input.use { sftp.upload(it, target, throttled()) }
                        ok++
                    } catch (e: CancellationException) {
                        remote?.let { r -> withContext(NonCancellable) { sftp.removeQuiet(r) } }
                        throw e
                    } catch (e: Exception) {
                        remote?.let { r -> withContext(NonCancellable) { sftp.removeQuiet(r) } }
                        currentCoroutineContext().ensureActive()
                        fail++
                        lastError = sftpMessage(e)
                    }
                }
            } catch (e: CancellationException) {
                ui = null
                onNotice("Unggahan dibatalkan ($ok berkas selesai).")
                onUploaded()
                throw e
            }
            ui = null
            onUploaded()
            onNotice(buildString {
                if (ok > 0) append("$ok berkas diunggah")
                if (fail > 0) append(if (isEmpty()) "" else ". ").append("$fail gagal: ${lastError ?: "kesalahan tidak diketahui"}")
            })
        }
        return true
    }

    private fun queryMeta(uri: Uri): Pair<String, Long> {
        var name: String? = null
        var size = 0L
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) name = c.getString(ni)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        val n = (name ?: uri.lastPathSegment ?: "berkas").substringAfterLast('/').replace('/', '_')
        return n to size
    }

    private suspend fun uniqueRemote(dir: String, name: String): String {
        fun join(n: String) = if (dir.endsWith("/")) dir + n else "$dir/$n"
        if (!sftp.exists(join(name))) return join(name)
        val (base, ext) = splitName(name)
        for (i in 1..999) {
            val cand = join("$base ($i)$ext")
            if (!sftp.exists(cand)) return cand
        }
        return join("${base}_${System.currentTimeMillis()}$ext")
    }
}