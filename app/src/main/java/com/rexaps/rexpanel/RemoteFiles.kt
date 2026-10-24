package com.rexaps.rexpanel

import android.graphics.Bitmap
import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import android.util.LruCache
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import java.io.ByteArrayOutputStream
import java.io.File

/* ------------------------------- model ---------------------------------- */

enum class FileKind { Image, Video, Audio, Text, Archive, Other }

enum class SortBy(val label: String) { Name("Nama"), Size("Ukuran"), Date("Terbaru") }

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

internal val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
internal val VIDEO_EXT = setOf("mp4", "mkv", "webm", "3gp", "mov", "m4v", "avi", "ts")
internal val AUDIO_EXT = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
internal val ARCHIVE_EXT = setOf("zip", "tar", "gz", "tgz", "bz2", "xz", "7z", "rar")
internal val TEXT_EXT = setOf(
    "txt", "md", "log", "conf", "cfg", "ini", "json", "xml", "yml", "yaml", "sh", "py", "kt",
    "java", "js", "html", "css", "csv", "toml", "env", "properties", "service", "c", "h", "cpp",
    "rs", "go", "sql", "gradle", "kts", "php", "rb", "lua"
)
internal val TEXT_NAMES = setOf("makefile", "dockerfile", "readme", "license", "authorized_keys")

fun kindOf(name: String, isDir: Boolean): FileKind {
        return __splitImpl_kindOf(name, isDir)
    }

fun shq(s: String): String = "'" + s.replace("'", "'\\''") + "'"

fun sftpMessage(e: Exception): String {
        return __splitImpl_sftpMessage(e)
    }

/* ------------------------------- bitmap --------------------------------- */

object ThumbCache {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16).toInt().coerceAtLeast(4 shl 20)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bmp: Bitmap) { cache.put(key, bmp) }
    fun clear() = cache.evictAll()
}

internal fun sampleFor(w: Int, h: Int, maxSide: Int): Int {
    var s = 1
    while (maxOf(w, h) / (s * 2) >= maxSide) s *= 2
    return s
}

internal fun orient(bmp: Bitmap, o: Int): Bitmap {
        return __splitImpl_orient(bmp, o)
    }

fun decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
        return __splitImpl_decodeSampled(bytes, maxSide)
    }

fun decodeFileSampled(file: File, maxSide: Int): Bitmap? {
        return __splitImpl_decodeFileSampled(file, maxSide)
    }

/* -------------------------------- SFTP ---------------------------------- */

/** Satu channel SFTP + mutex. ChannelSftp tidak aman dipakai paralel, jadi dibuat beberapa "lane". */
internal class Lane(internal val opener: () -> ChannelSftp) {
    internal val lock = Mutex()
    internal var ch: ChannelSftp? = null

    suspend fun <T> use(block: (ChannelSftp, () -> Boolean) -> T): T = this.__splitImpl_use(block)

    fun close() { runCatching { ch?.disconnect() }; ch = null }
}

internal fun monitor(alive: () -> Boolean, onCount: (Long) -> Unit = {}) = object : SftpProgressMonitor {
    private var total = 0L
    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
    override fun count(count: Long): Boolean { total += count; onCount(total); return alive() }
    override fun end() {}
}

class SftpClient(opener: () -> ChannelSftp) {
    internal val browse = Lane(opener)   // list, mkdir, rename, delete
    private val thumbs = Lane(opener)   // thumbnail kecil
    internal val xfer = Lane(opener)     // unduhan untuk pratinjau
    private val dlLane = Lane(opener)   // unduhan ke /sdcard/Download
    private val upLane = Lane(opener)   // unggahan

    suspend fun home(): String = browse.use { c, _ -> c.home }

    suspend fun list(path: String): List<RemoteFile> = this.__splitImpl_list(path)

    suspend fun readThumb(path: String): ByteArray = thumbs.use { c, alive ->
        val out = ByteArrayOutputStream()
        c.get(path, out, monitor(alive))
        out.toByteArray()
    }

    suspend fun download(remote: String, dest: File, onProgress: (Long) -> Unit) {
        return this.__splitImpl_download(remote, dest, onProgress)
    }

    /** Unduh langsung ke OutputStream (dipakai untuk menyimpan ke folder Download). */
    suspend fun downloadTo(remote: String, out: OutputStream, onProgress: (Long) -> Unit) {
        dlLane.use { c, alive -> c.get(remote, out, monitor(alive, onProgress)) }
    }

    /** Unggah dari InputStream ke path server. */
    suspend fun upload(input: InputStream, remote: String, onProgress: (Long) -> Unit) {
        upLane.use { c, alive -> c.put(input, remote, monitor(alive, onProgress), ChannelSftp.OVERWRITE) }
    }

    suspend fun exists(path: String): Boolean =
        upLane.use { c, _ -> runCatching { c.stat(path) }.isSuccess }

    suspend fun removeQuiet(path: String) {
        upLane.use { c, _ -> runCatching { c.rm(path) }; Unit }
    }

    suspend fun mkdir(path: String) = browse.use { c, _ -> c.mkdir(path) }
    suspend fun rename(from: String, to: String) = browse.use { c, _ -> c.rename(from, to) }
    suspend fun remove(f: RemoteFile) = browse.use { c, _ ->
        if (f.isDir && !f.isLink) c.rmdir(f.path) else c.rm(f.path)
    }

    fun close() {
        browse.close(); thumbs.close(); xfer.close(); dlLane.close(); upLane.close()
    }
}

/* --------------------------- state file browser -------------------------- */

@Stable
class FileBrowserState(
    internal val sftp: SftpClient,
    internal val scope: CoroutineScope,
    private val exec: suspend (String) -> String,
    internal val cacheDir: File,
    appContext: Context
) {
    var path by mutableStateOf("")
        internal set
    var entries by mutableStateOf<List<RemoteFile>>(emptyList())
        internal set
    var loading by mutableStateOf(false)
        internal set
    var error by mutableStateOf<String?>(null)
        internal set
    var notice by mutableStateOf<String?>(null)
    var showHidden by mutableStateOf(false)
    var grid by mutableStateOf(false)
    var sort by mutableStateOf(SortBy.Name)
    var preview by mutableStateOf<RemoteFile?>(null)

    /** Path berkas yang sedang dipilih (multi-select). */
    var selected by mutableStateOf<Set<String>>(emptySet())
        internal set
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

    fun start() {
        return this.__splitImpl_start()
    }

    fun go(target: String) {
        return this.__splitImpl_go(target)
    }

    fun refresh() = go(path.ifEmpty { home })
    fun goHome() { if (home.isNotEmpty()) go(home) }
    fun up() {
        if (!canGoUp) return
        val p = path.trimEnd('/').substringBeforeLast('/', "")
        go(if (p.isEmpty()) "/" else p)
    }

    fun open(f: RemoteFile) { if (f.isDir) go(f.path) else preview = f }

    private fun join(dir: String, name: String) = if (dir.endsWith("/")) dir + name else "$dir/$name"

    internal fun mutate(ok: String, block: suspend () -> Unit) {
        return this.__splitImpl_mutate(ok, block)
    }

    fun mkdir(name: String) = mutate("Folder dibuat") { sftp.mkdir(join(path, name)) }
    fun rename(f: RemoteFile, newName: String) = mutate("Nama diubah") { sftp.rename(f.path, join(path, newName)) }
    fun delete(f: RemoteFile) = mutate("Dihapus") { sftp.remove(f) }

    fun deleteAll(list: List<RemoteFile>) {
        return this.__splitImpl_deleteAll(list)
    }

    internal fun thumbKey(f: RemoteFile) = "${f.path}|${f.mtimeSec}"

    fun cachedThumb(f: RemoteFile): Bitmap? = ThumbCache.get(thumbKey(f))

    suspend fun thumbnail(f: RemoteFile): Bitmap? {
        return this.__splitImpl_thumbnail(f)
    }

    /** Unduh ke cache (dipakai ulang jika sudah ada). */
    suspend fun fetch(f: RemoteFile, onProgress: (Long) -> Unit): File {
        return this.__splitImpl_fetch(f, onProgress)
    }

    suspend fun readText(f: RemoteFile): String = exec("head -c 65536 -- ${shq(f.path)} 2>&1")

    fun close() {
        job?.cancel()
        transfers.cancel()
        sftp.close()
        ThumbCache.clear()
        runCatching { File(cacheDir, "rexpanel").deleteRecursively() }
    }
}