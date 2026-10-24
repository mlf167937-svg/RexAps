package com.rexaps.rexpanel

import android.graphics.Bitmap
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val isHome: Boolean get() = home.isNotEmpty() && path == home

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
