package com.rexaps.rexmusic

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val KEY_CLEAN = Regex("[^\\p{L}\\p{N}]+")

/** Nama folder lagu: "Iqro" + "Raim Laode" -> "Iqro-Raim-Laode". */
fun offlineKey(track: RexTrack): String {
    val raw = KEY_CLEAN.replace("${track.title} ${track.artist}", "-").trim('-')
    return raw.take(100).trim('-').ifBlank { "Lagu" }
}

/** Key folder lagu ini kalau sudah diunduh (cocok lewat nama folder, atau lewat id). */
fun OfflineState.keyFor(track: RexTrack, key: String = offlineKey(track)): String? =
    if (key in keys) key else entries.firstOrNull { it.track.id == track.id }?.key

fun OfflineState.isDownloaded(track: RexTrack, key: String = offlineKey(track)): Boolean =
    keyFor(track, key) != null

/**
 * Mengelola lagu offline di /storage/emulated/0/Download/RexAps/Music/<Judul-Artis>/
 *   music.mp3, thumbnail.jpg|png, meta.json
 * Unduhan berjalan paralel maks 2, lewat file .part dulu supaya tidak ada file setengah jadi.
 */
object RexOfflineManager {

    const val DISPLAY_PATH = "Download/RexAps/Music"

    private const val AUDIO_NAME = "music.mp3"
    private const val META_NAME = "meta.json"
    private const val LYRICS_NAME = "lyrics.json"
    private const val PREFS = "rex_music_offline"
    private const val KEY_OFFLINE = "offline_mode"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) RexMusic"
    private const val MAX_PARALLEL = 2

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private var initialized = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val gate = Semaphore(MAX_PARALLEL)

    private val _state = MutableStateFlow(OfflineState())
    val state: StateFlow<OfflineState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val root: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "RexAps/Music"
        )

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        RexDownloadNotification.init(appContext)
        _state.update {
            it.copy(
                enabled = prefs.getBoolean(KEY_OFFLINE, false),
                hasAccess = hasAccess(appContext)
            )
        }
        refresh()
    }

    //  Access & mode 

    /** Android 11+: "Akses semua file". Android 10-: izin tulis penyimpanan biasa. */
    fun hasAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    fun setOfflineMode(on: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE, on).apply()
        _state.update { it.copy(enabled = on) }
    }

    //  Library 

    fun refresh() {
        scope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        val access = hasAccess(appContext)
        val entries = if (access) withContext(Dispatchers.IO) { scan() } else emptyList()
        _state.update {
            it.copy(
                hasAccess = access,
                entries = entries,
                keys = entries.map { e -> e.key }.toSet(),
                totalBytes = entries.sumOf { e -> e.sizeBytes }
            )
        }
    }

    fun isDownloaded(track: RexTrack): Boolean = _state.value.isDownloaded(track)

    /** File mp3 lokal untuk lagu ini, atau null kalau belum diunduh. */
    fun audioFile(track: RexTrack): File? {
        val key = _state.value.keyFor(track) ?: return null
        val file = File(root, "$key/$AUDIO_NAME")
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    /** Lirik offline yang sudah disimpan bersama lagu. */
    fun lyricsFile(track: RexTrack): File? {
        val key = _state.value.keyFor(track) ?: return null
        val file = File(root, "$key/$LYRICS_NAME")
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    fun readOfflineLyrics(track: RexTrack): Lyrics? = runCatching {
        val file = lyricsFile(track) ?: return null
        val o = JSONObject(file.readText())
        val plain = o.optString("plainLyrics")
        val arr = o.optJSONArray("syncedLyrics") ?: org.json.JSONArray()
        val synced = (0 until arr.length()).mapNotNull { i ->
            val line = arr.optJSONObject(i) ?: return@mapNotNull null
            val text = line.optString("text").trim()
            if (text.isBlank()) return@mapNotNull null
            LyricLine(line.optLong("timeMs").coerceAtLeast(0L), text)
        }.sortedBy { it.timeMs }
        Lyrics(plain = plain, synced = synced)
    }.getOrNull()

    //  Download 

    fun download(track: RexTrack) {
        if (!hasAccess(appContext)) {
            _messages.tryEmit("Izin penyimpanan belum diberikan")
            return
        }
        val key = offlineKey(track)
        if (_state.value.isDownloaded(track, key)) {
            _messages.tryEmit("Sudah ada di offline")
            return
        }
        if (jobs[key]?.isActive == true) return

        setStatus(key, DownloadStatus(track, DownloadStage.Queued))
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                gate.withPermit { doDownload(track, key) }
                refreshNow()
                val finalStatus = _state.value.downloads[key]
                    ?: DownloadStatus(track, DownloadStage.Finalizing, 100)
                RexDownloadNotification.showCompleted(appContext, key, finalStatus)
                _messages.tryEmit("Tersimpan offline: ${track.title}")
            } catch (e: CancellationException) {
                cleanup(key)
                RexDownloadNotification.showCancelled(appContext, key, track)
                throw e
            } catch (e: Exception) {
                cleanup(key)
                RexDownloadNotification.showFailed(appContext, key, track, e.message ?: "kesalahan jaringan")
                _messages.tryEmit("Gagal mengunduh ${track.title}: ${e.message ?: "kesalahan jaringan"}")
            } finally {
                clearStatus(key)
                jobs.remove(key)
            }
        }
        jobs[key] = job
        job.start()
    }

    fun cancel(track: RexTrack) {
        jobs[offlineKey(track)]?.cancel()
    }

    fun delete(track: RexTrack) {
        val key = _state.value.keyFor(track) ?: return
        jobs[key]?.cancel()
        scope.launch {
            withContext(Dispatchers.IO) { File(root, key).deleteRecursively() }
            refreshNow()
            _messages.tryEmit("Dihapus dari offline: ${track.title}")
        }
    }

    fun deleteAll() {
        val keys = _state.value.entries.map { it.key }
        scope.launch {
            withContext(Dispatchers.IO) { keys.forEach { File(root, it).deleteRecursively() } }
            refreshNow()
            _messages.tryEmit("Semua lagu offline dihapus")
        }
    }

    private suspend fun doDownload(track: RexTrack, key: String) {
        setStatus(key, DownloadStatus(track, DownloadStage.Resolving))
        val fresh = RexTrackResolver.resolve(track).getOrThrow()
        if (fresh.audioUrl.isBlank()) throw IOException("URL audio kosong")

        withContext(Dispatchers.IO) {
            val dir = File(root, key)
            if (!dir.isDirectory && !dir.mkdirs()) {
                throw IOException("tidak bisa membuat folder $DISPLAY_PATH/$key")
            }
            val part = File(dir, "$AUDIO_NAME.part")
            downloadFile(fresh.audioUrl, part) { progress ->
                setStatus(
                    key,
                    DownloadStatus(
                        track = track,
                        stage = DownloadStage.Downloading,
                        percent = progress.percent,
                        downloadedBytes = progress.downloadedBytes,
                        totalBytes = progress.totalBytes,
                        speedBytesPerSecond = progress.speedBytesPerSecond
                    )
                )
            }
            val target = File(dir, AUDIO_NAME)
            if (target.exists()) target.delete()
            if (!part.renameTo(target)) throw IOException("gagal menyimpan file")

            ensureActive()
            downloadThumbnail(fresh.cover.ifBlank { track.cover }, dir)
            writeMeta(dir, fresh)

            val audioStatus = _state.value.downloads[key]
                ?: DownloadStatus(track = track, stage = DownloadStage.Downloading, percent = 100, downloadedBytes = target.length(), totalBytes = target.length())
            setStatus(
                key,
                audioStatus.copy(
                    track = track,
                    stage = DownloadStage.FetchingLyrics,
                    percent = 100,
                    speedBytesPerSecond = 0L
                )
            )
            val lyrics = RexMusicApi.fetchLyrics("${fresh.artist} ${fresh.title}".trim()).getOrNull()
            writeLyrics(dir, lyrics, lyrics == null)

            setStatus(
                key,
                _state.value.downloads[key]?.copy(
                    stage = DownloadStage.Finalizing,
                    percent = 100,
                    speedBytesPerSecond = 0L
                ) ?: audioStatus.copy(stage = DownloadStage.Finalizing, percent = 100, speedBytesPerSecond = 0L)
            )
        }
    }

    private data class DownloadProgress(
        val percent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long
    )

    private suspend fun downloadFile(
        url: String,
        dest: File,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val conn = openConnection(url)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("server membalas kode $code")
            val type = conn.contentType.orEmpty().lowercase()
            if (type.startsWith("text/") || type.contains("json")) {
                throw IOException("server tidak mengirim file audio")
            }
            val total = conn.contentLengthLong.coerceAtLeast(0L)
            onProgress(DownloadProgress(
                percent = if (total > 0L) 0 else -1,
                downloadedBytes = 0L,
                totalBytes = total,
                speedBytesPerSecond = 0L
            ))

            val startAt = System.currentTimeMillis()
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buffer = ByteArray(32 * 1024)
                    var done = 0L
                    var lastTick = 0L
                    var lastBytes = 0L
                    var speed = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n

                        val now = System.currentTimeMillis()
                        if (done == n.toLong() || now - lastTick >= 250L || (total > 0L && done >= total)) {
                            val dt = now - lastTick
                            if (lastTick > 0L && dt > 0L) {
                                val instant = ((done - lastBytes) * 1000L / dt).coerceAtLeast(0L)
                                speed = if (speed == 0L) instant else ((speed * 0.72f) + (instant * 0.28f)).toLong()
                            }
                            lastTick = now
                            lastBytes = done
                            val pct = if (total > 0L) {
                                (done * 100L / total).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            onProgress(DownloadProgress(pct, done, total, speed))
                        }
                    }
                    if (total > 0L && done < total) throw IOException("unduhan terputus")
                    if (done > 0L) {
                        val elapsed = (System.currentTimeMillis() - startAt).coerceAtLeast(1L)
                        val finalSpeed = if (speed > 0L) speed else (done * 1000L / elapsed)
                        onProgress(DownloadProgress(
                            percent = if (total > 0L) 100 else -1,
                            downloadedBytes = done,
                            totalBytes = total,
                            speedBytesPerSecond = finalSpeed
                        ))
                    }
                }
            }
            if (dest.length() <= 0L) throw IOException("file kosong")
        } finally {
            conn.disconnect()
        }
    }

    /** Thumbnail bersifat opsional: kalau gagal, lagu tetap tersimpan tanpa cover. */
    private fun downloadThumbnail(url: String, dir: File) {
        if (!url.startsWith("http")) return
        val tmp = File(dir, "thumbnail.tmp")
        try {
            val conn = openConnection(url)
            try {
                if (conn.responseCode !in 200..299) return
                conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            } finally {
                conn.disconnect()
            }
            if (tmp.length() <= 0L) return
            val isPng = tmp.inputStream().use { s ->
                val h = ByteArray(4)
                s.read(h) == 4 && h[0] == 0x89.toByte() && h[1] == 0x50.toByte() &&
                    h[2] == 0x4E.toByte() && h[3] == 0x47.toByte()
            }
            File(dir, "thumbnail.jpg").delete()
            File(dir, "thumbnail.png").delete()
            tmp.renameTo(File(dir, if (isPng) "thumbnail.png" else "thumbnail.jpg"))
        } catch (_: Exception) {
            // abaikan
        } finally {
            tmp.delete()
        }
    }

    private fun writeMeta(dir: File, t: RexTrack) {
        runCatching {
            File(dir, META_NAME).writeText(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("artist", t.artist)
                    .put("album", t.album)
                    .put("spotifyUrl", t.spotifyUrl)
                    .put("durationText", t.durationText)
                    .put("coverUrl", t.cover)
                    .toString()
            )
        }
    }


    private fun writeLyrics(dir: File, lyrics: Lyrics?, unavailable: Boolean) {
        runCatching {
            val synced = org.json.JSONArray().apply { lyrics?.synced.orEmpty().forEach { put(org.json.JSONObject().put("timeMs", it.timeMs).put("text", it.text)) } }
            File(dir, LYRICS_NAME).writeText(
                JSONObject()
                    .put("version", 1)
                    .put("available", lyrics != null && !lyrics.isEmpty)
                    .put("offlineOnly", true)
                    .put("unavailable", unavailable)
                    .put("plainLyrics", lyrics?.plain.orEmpty())
                    .put("syncedLyrics", synced)
                    .toString()
            )
        }
    }

    private fun readMeta(dir: File): JSONObject? = runCatching {
        JSONObject(File(dir, META_NAME).readText())
    }.getOrNull()

    /** Buka koneksi dan ikuti redirect sendiri (termasuk http -> https). */
    private fun openConnection(url: String): HttpURLConnection {
        var current = url
        for (i in 0 until 6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", USER_AGENT)
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) throw IOException("redirect tanpa tujuan")
                current = URL(URL(current), location).toString()
                continue
            }
            return conn
        }
        throw IOException("terlalu banyak redirect")
    }

    private fun cleanup(key: String) {
        val dir = File(root, key)
        File(dir, "$AUDIO_NAME.part").delete()
        File(dir, "thumbnail.tmp").delete()
        if (!File(dir, AUDIO_NAME).exists()) dir.deleteRecursively()
    }

    //  Scan 

    private fun scan(): List<OfflineEntry> {
        val dirs = root.listFiles { f -> f.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val mp3 = File(dir, AUDIO_NAME)
            if (!mp3.isFile || mp3.length() <= 0L) return@mapNotNull null
            val thumb = findThumbnail(dir)
            val meta = readMeta(dir)
            val track = RexTrack(
                id = meta?.optString("id").orEmpty().ifBlank { "off-${dir.name}" },
                title = meta?.optString("title").orEmpty().ifBlank { dir.name.replace('-', ' ') },
                artist = meta?.optString("artist").orEmpty().ifBlank { "Unknown" },
                album = meta?.optString("album").orEmpty().ifBlank { "Offline" },
                cover = thumb?.let { Uri.fromFile(it).toString() }.orEmpty(),
                spotifyUrl = meta?.optString("spotifyUrl").orEmpty(),
                durationText = meta?.optString("durationText").orEmpty()
            )
            OfflineEntry(
                key = dir.name,
                track = track,
                sizeBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                savedAt = mp3.lastModified()
            )
        }.sortedByDescending { it.savedAt }
    }

    private fun findThumbnail(dir: File): File? =
        listOf("thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp")
            .map { File(dir, it) }
            .firstOrNull { it.isFile && it.length() > 0L }

    //  Status helpers 

    private fun setStatus(key: String, status: DownloadStatus) {
        _state.update { it.copy(downloads = it.downloads + (key to status)) }
        if (::appContext.isInitialized) RexDownloadNotification.showProgress(appContext, key, status)
    }

    private fun clearStatus(key: String) =
        _state.update { it.copy(downloads = it.downloads - key) }
}
