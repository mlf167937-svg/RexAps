package com.rexaps.rexpanel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.media.ExifInterface
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.io.File

enum class FileKind { Image, Video, Audio, Text, Archive, Other }

data class RemoteFile(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val isLink: Boolean,
    val size: Long,
    val mtimeSec: Long,
    val perms: String,
    val kind: FileKind
)

internal val ARCHIVE_EXT = setOf("zip", "tar", "gz", "tgz", "bz2", "xz", "7z", "rar")

fun decodeFileSampled(file: File, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0) return null
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, maxSide) }
    val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
    val o = runCatching {
        ExifInterface(file.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    return orient(bmp, o)
}

/* -------------------------------- SFTP ---------------------------------- */

@Stable
class FileBrowserState(
    internal val sftp: SftpClient,
    internal val scope: CoroutineScope,
    private val exec: suspend (String) -> String,
    internal val cacheDir: File,
    appContext: Context
) {
    var path by mutableStateOf("")
        private set
    var entries by mutableStateOf<List<RemoteFile>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
    var showHidden by mutableStateOf(false)
    var grid by mutableStateOf(false)
    var sort by mutableStateOf(SortBy.Name)
    var preview by mutableStateOf<RemoteFile?>(null)

    /** Path berkas yang sedang dipilih (multi-select). */
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set
    val selecting: Boolean get() = selected.isNotEmpty()

    val transfers = TransferManager(
        sftp = sftp,
        scope = scope,
        context = appContext,
        onUploaded = { refresh() },
        onNotice = { notice = it }
    )

    internal var home = ""
    internal var job: Job? = null
    internal var req = 0
    internal val failedThumbs = HashSet<String>()

    val canGoUp: Boolean get() = path.isNotEmpty() && path != "/"
    val isHome: Boolean get() = home.isNotEmpty() && path == home

    fun toggleSelect(f: RemoteFile) {
        selected = if (f.path in selected) selected - f.path else selected + f.path
    }

    fun selectAll(list: List<RemoteFile>) { selected = list.map { it.path }.toSet() }
    fun clearSelection() { selected = emptySet() }

    fun start() = this.__split_start_1()

    fun go(target: String) = this.__split_go_2(target)

    fun refresh() = go(path.ifEmpty { home })
    fun goHome() { if (home.isNotEmpty()) go(home) }
    fun up() {
        if (!canGoUp) return
        val p = path.trimEnd('/').substringBeforeLast('/', "")
        go(if (p.isEmpty()) "/" else p)
    }

    fun open(f: RemoteFile) { if (f.isDir) go(f.path) else preview = f }

    private fun join(dir: String, name: String) = if (dir.endsWith("/")) dir + name else "$dir/$name"

    private fun mutate(ok: String, block: suspend () -> Unit) = this.__split_mutate_3(ok, block)

    fun mkdir(name: String) = mutate("Folder dibuat") { sftp.mkdir(join(path, name)) }
    fun rename(f: RemoteFile, newName: String) = mutate("Nama diubah") { sftp.rename(f.path, join(path, newName)) }
    fun delete(f: RemoteFile) = mutate("Dihapus") { sftp.remove(f) }

    fun deleteAll(list: List<RemoteFile>) = this.__split_deleteAll_4(list)

    internal fun thumbKey(f: RemoteFile) = "${f.path}|${f.mtimeSec}"

    fun cachedThumb(f: RemoteFile): Bitmap? = ThumbCache.get(thumbKey(f))

    suspend fun thumbnail(f: RemoteFile): Bitmap? = this.__split_thumbnail_5(f)

    /** Unduh ke cache (dipakai ulang jika sudah ada). */
    suspend fun fetch(f: RemoteFile, onProgress: (Long) -> Unit): File = this.__split_fetch_6(f, onProgress)

    suspend fun readText(f: RemoteFile): String = exec("head -c 65536 -- ${shq(f.path)} 2>&1")

    fun close() {
        job?.cancel()
        transfers.cancel()
        sftp.close()
        ThumbCache.clear()
        runCatching { File(cacheDir, "rexpanel").deleteRecursively() }
    }
}
