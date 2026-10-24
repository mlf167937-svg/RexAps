package com.rexaps.rexpanel

import android.graphics.Bitmap
import java.io.InputStream
import java.io.OutputStream
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Vector

enum class SortBy(val label: String) { Name("Nama"), Size("Ukuran"), Date("Terbaru") }

internal val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")

internal val VIDEO_EXT = setOf("mp4", "mkv", "webm", "3gp", "mov", "m4v", "avi", "ts")

internal val AUDIO_EXT = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")

internal val TEXT_EXT = setOf(
    "txt", "md", "log", "conf", "cfg", "ini", "json", "xml", "yml", "yaml", "sh", "py", "kt",
    "java", "js", "html", "css", "csv", "toml", "env", "properties", "service", "c", "h", "cpp",
    "rs", "go", "sql", "gradle", "kts", "php", "rb", "lua"
)

internal val TEXT_NAMES = setOf("makefile", "dockerfile", "readme", "license", "authorized_keys")

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

internal fun sampleFor(w: Int, h: Int, maxSide: Int): Int {
    var s = 1
    while (maxOf(w, h) / (s * 2) >= maxSide) s *= 2
    return s
}

internal fun orient(bmp: Bitmap, o: Int): Bitmap {
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

/** Satu channel SFTP + mutex. ChannelSftp tidak aman dipakai paralel, jadi dibuat beberapa "lane". */
internal class Lane(private val opener: () -> ChannelSftp) {
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

internal fun monitor(alive: () -> Boolean, onCount: (Long) -> Unit = {}) = object : SftpProgressMonitor {
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

fun FileBrowserState.__split_start_1() {
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

fun FileBrowserState.__split_go_2(target: String) {
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
