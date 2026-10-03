package com.rexaps.rexpanel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Vector

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

private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
private val VIDEO_EXT = setOf("mp4", "mkv", "webm", "3gp", "mov", "m4v", "avi", "ts")
private val AUDIO_EXT = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
private val ARCHIVE_EXT = setOf("zip", "tar", "gz", "tgz", "bz2", "xz", "7z", "rar")
private val TEXT_EXT = setOf(
    "txt", "md", "log", "conf", "cfg", "ini", "json", "xml", "yml", "yaml", "sh", "py", "kt",
    "java", "js", "html", "css", "csv", "toml", "env", "properties", "service", "c", "h", "cpp",
    "rs", "go", "sql", "gradle", "kts", "php", "rb", "lua"
)
private val TEXT_NAMES = setOf("makefile", "dockerfile", "readme", "license", "authorized_keys")

fun kindOf(name: String, isDir: Boolean): FileKind {
    if (isDir) return FileKind.Other
    val lower = name.lowercase()
    val ext = lower.substringAfterLast('.', "")
    return when {
        ext in IMAGE_EXT -> FileKind.Image
        ext in VIDEO_EXT -> FileKind.Video
        ext in AUDIO_EXT -> FileKind.Audio
        ext in ARCHIVE_EXT -> FileKind.Archive
        ext in TEXT_EXT || lower in TEXT_NAMES || (lower.startsWith(".") && '.' !in lower.drop(1)) -> FileKind.Text
        else -> FileKind.Other
    }
}

fun shq(s: String): String = "'" + s.replace("'", "'\\''") + "'"

fun sftpMessage(e: Exception): String {
    if (e is SftpException) {
        return when (e.id) {
            ChannelSftp.SSH_FX_NO_SUCH_FILE -> "Berkas atau folder tidak ditemukan."
            ChannelSftp.SSH_FX_PERMISSION_DENIED -> "Akses ditolak. Akun ini tidak punya izin untuk lokasi tersebut."
            else -> e.message?.takeIf { it.isNotBlank() }
                ?: "Operasi gagal. Jika menghapus folder, pastikan folder kosong."
        }
    }
    return e.message?.takeIf { it.isNotBlank() } ?: "Terjadi kesalahan."
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

private fun sampleFor(w: Int, h: Int, maxSide: Int): Int {
    var s = 1
    while (maxOf(w, h) / (s * 2) >= maxSide) s *= 2
    return s
}

private fun orient(bmp: Bitmap, o: Int): Bitmap {
    val m = Matrix()
    when (o) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        else -> return bmp
    }
    return runCatching { Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true) }.getOrDefault(bmp)
}

fun decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0) return null
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, maxSide) }
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
    val o = if (Build.VERSION.SDK_INT >= 24) {
        runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    } else ExifInterface.ORIENTATION_NORMAL
    return orient(bmp, o)
}

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

/** Satu channel SFTP + mutex. ChannelSftp tidak aman dipakai paralel, jadi dibuat beberapa "lane". */
private class Lane(private val opener: () -> ChannelSftp) {
    private val lock = Mutex()
    private var ch: ChannelSftp? = null

    suspend fun <T> use(block: (ChannelSftp, () -> Boolean) -> T): T = lock.withLock {
        withContext(Dispatchers.IO) {
            val job = coroutineContext[Job]
            val alive = { job?.isActive != false }
            val cur = ch
            val chan = if (cur != null && cur.isConnected && !cur.isClosed) cur else opener().also { ch = it }
            try {
                val r = block(chan, alive)
                ensureActive()
                r
            } catch (e: Throwable) {
                // Channel yang transfernya dibatalkan atau error aneh tidak dipakai ulang.
                if (!alive() || e !is SftpException) {
                    runCatching { chan.disconnect() }
                    if (ch === chan) ch = null
                }
                throw e
            }
        }
    }

    fun close() { runCatching { ch?.disconnect() }; ch = null }
}

private fun monitor(alive: () -> Boolean, onCount: (Long) -> Unit = {}) = object : SftpProgressMonitor {
    private var total = 0L
    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
    override fun count(count: Long): Boolean { total += count; onCount(total); return alive() }
    override fun end() {}
}

class SftpClient(opener: () -> ChannelSftp) {
    private val browse = Lane(opener)   // list, mkdir, rename, delete
    private val thumbs = Lane(opener)   // thumbnail kecil
    private val xfer = Lane(opener)     // unduhan untuk pratinjau
    private val dlLane = Lane(opener)   // unduhan ke /sdcard/Download
    private val upLane = Lane(opener)   // unggahan

    suspend fun home(): String = browse.use { c, _ -> c.home }

    suspend fun list(path: String): List<RemoteFile> = browse.use { c, _ ->
        val dir = if (path.isEmpty()) "/" else path
        val base = dir.trimEnd('/')
        (c.ls(dir) as Vector<*>).filterIsInstance<ChannelSftp.LsEntry>()
            .filter { it.filename != "." && it.filename != ".." }
            .map { e ->
                val a = e.attrs
                val full = "$base/${e.filename}"
                var isDir = a.isDir
                var size = a.size
                if (a.isLink) {
                    val st = runCatching { c.stat(full) }.getOrNull()
                    isDir = st?.isDir ?: false
                    size = st?.size ?: size
                }
                RemoteFile(
                    name = e.filename, path = full, isDir = isDir, isLink = a.isLink,
                    size = size, mtimeSec = a.mTime.toLong(), perms = a.permissionsString,
                    kind = kindOf(e.filename, isDir)
                )
            }
    }

    suspend fun readThumb(path: String): ByteArray = thumbs.use { c, alive ->
        val out = ByteArrayOutputStream()
        c.get(path, out, monitor(alive))
        out.toByteArray()
    }

    suspend fun download(remote: String, dest: File, onProgress: (Long) -> Unit) {
        val tmp = File(dest.path + ".part")
        try {
            xfer.use { c, alive -> c.get(remote, tmp.absolutePath, monitor(alive, onProgress)) }
            tmp.renameTo(dest)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
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
    private val sftp: SftpClient,
    private val scope: CoroutineScope,
    private val exec: suspend (String) -> String,
    private val cacheDir: File,
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

    private var home = ""
    private var job: Job? = null
    private var req = 0
    private val failedThumbs = HashSet<String>()

    val canGoUp: Boolean get() = path.isNotEmpty() && path != "/"

    fun toggleSelect(f: RemoteFile) {
        selected = if (f.path in selected) selected - f.path else selected + f.path
    }

    fun selectAll(list: List<RemoteFile>) { selected = list.map { it.path }.toSet() }
    fun clearSelection() { selected = emptySet() }

    fun start() {
        scope.launch {
            loading = true
            try {
                home = sftp.home()
                go(home)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = sftpMessage(e)
                loading = false
            }
        }
    }

    fun go(target: String) {
        job?.cancel()
        val id = ++req
        job = scope.launch {
            loading = true
            if (path.isEmpty()) error = null
            try {
                val list = sftp.list(target)
                if (id == req) { entries = list; path = target; error = null; selected = emptySet() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (id == req) {
                    if (entries.isEmpty()) { error = sftpMessage(e); path = target }
                    else notice = sftpMessage(e)
                }
            } finally {
                if (id == req) loading = false
            }
        }
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

    private fun mutate(ok: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                notice = ok
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = sftpMessage(e)
            }
        }
    }

    fun mkdir(name: String) = mutate("Folder dibuat") { sftp.mkdir(join(path, name)) }
    fun rename(f: RemoteFile, newName: String) = mutate("Nama diubah") { sftp.rename(f.path, join(path, newName)) }
    fun delete(f: RemoteFile) = mutate("Dihapus") { sftp.remove(f) }

    fun deleteAll(list: List<RemoteFile>) {
        scope.launch {
            var ok = 0
            var fail = 0
            var last: String? = null
            for (f in list) {
                try {
                    sftp.remove(f)
                    ok++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail++
                    last = sftpMessage(e)
                }
            }
            notice = if (fail == 0) "$ok item dihapus" else "$ok dihapus, $fail gagal" + (last?.let { ": $it" } ?: "")
            refresh()
        }
    }

    private fun thumbKey(f: RemoteFile) = "${f.path}|${f.mtimeSec}"

    fun cachedThumb(f: RemoteFile): Bitmap? = ThumbCache.get(thumbKey(f))

    suspend fun thumbnail(f: RemoteFile): Bitmap? {
        val key = thumbKey(f)
        ThumbCache.get(key)?.let { return it }
        if (key in failedThumbs) return null
        return try {
            val bytes = sftp.readThumb(f.path)
            val bmp = withContext(Dispatchers.Default) { decodeSampled(bytes, 256) }
            if (bmp != null) ThumbCache.put(key, bmp) else failedThumbs.add(key)
            bmp
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failedThumbs.add(key)
            null
        }
    }

    /** Unduh ke cache (dipakai ulang jika sudah ada). */
    suspend fun fetch(f: RemoteFile, onProgress: (Long) -> Unit): File {
        val dir = File(cacheDir, "rexpanel").apply { mkdirs() }
        val tag = Integer.toHexString((f.path + f.mtimeSec).hashCode())
        val dest = File(dir, "${tag}_${f.name}".replace('/', '_'))
        if (dest.exists() && dest.length() == f.size) return dest
        sftp.download(f.path, dest, onProgress)
        return dest
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