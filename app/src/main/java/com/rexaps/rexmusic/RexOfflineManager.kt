package com.rexaps.rexmusic

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.json.JSONObject

/**
 * Mengelola lagu offline di /storage/emulated/0/Download/RexAps/Music/<Judul-Artis>/
 *   music.mp3, thumbnail.jpg|png, meta.json
 * Unduhan berjalan paralel maks 2, lewat file .part dulu supaya tidak ada file setengah jadi.
 */
object RexOfflineManager {

    const val DISPLAY_PATH = "Download/RexAps/Music"

    internal const val AUDIO_NAME = "music.mp3"
    internal const val META_NAME = "meta.json"
    internal const val LYRICS_NAME = "lyrics.json"
    internal const val PREFS = "rex_music_offline"
    internal const val KEY_OFFLINE = "offline_mode"
    internal const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) RexMusic"
    private const val MAX_PARALLEL = 2

    internal lateinit var appContext: Context
    internal lateinit var prefs: SharedPreferences
    internal var initialized = false

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val jobs = ConcurrentHashMap<String, Job>()
    internal val gate = Semaphore(MAX_PARALLEL)

    internal val _state = MutableStateFlow(OfflineState())
    val state: StateFlow<OfflineState> = _state.asStateFlow()

    internal val _messages = MutableSharedFlow<String>(
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
    fun init(context: Context) = this.__split_init_1(context)

    //  Access & mode 

    /** Android 11+: "Akses semua file". Android 10-: izin tulis penyimpanan biasa. */
    fun hasAccess(context: Context): Boolean = this.__split_hasAccess_2(context)

    fun setOfflineMode(on: Boolean) = this.__splitExtra_setOfflineMode_1(on)

    //  Library 

    fun refresh() = this.__splitExtra_refresh_2()

    internal suspend fun refreshNow() = this.__split_refreshNow_3()

    fun isDownloaded(track: RexTrack): Boolean = this.__splitExtra_isDownloaded_3(track)

    /** Import lagu yang diterima lewat RexMusicSync ke library RexMusic, tanpa menghapus cache sync. */
    fun importSyncTrack(track: RexTrack, sourceDir: File): Boolean = this.__split_importSyncTrack_4(track, sourceDir)

    /** File mp3 lokal untuk lagu ini, atau null kalau belum diunduh. */
    fun audioFile(track: RexTrack): File? = this.__splitExtra_audioFile_4(track)

    /** Lirik offline yang sudah disimpan bersama lagu. */
    fun lyricsFile(track: RexTrack): File? = this.__splitExtra_lyricsFile_5(track)

    fun readOfflineLyrics(track: RexTrack): Lyrics? = this.__split_readOfflineLyrics_5(track)

    //  Download 

    fun download(track: RexTrack) = this.__split_download_6(track)

    fun cancel(track: RexTrack) = this.__splitExtra_cancel_6(track)

    fun delete(track: RexTrack) = this.__split_delete_7(track)

    fun deleteAll() = this.__split_deleteAll_8()

    internal suspend fun doDownload(track: RexTrack, key: String) = this.__split_doDownload_9(track, key)

    internal data class DownloadProgress(
        val percent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long
    )

    internal suspend fun downloadFile(
        url: String,
        dest: File,
        onProgress: (DownloadProgress) -> Unit
    ) = this.__split_downloadFile_10(url, dest, onProgress)

    /** Thumbnail bersifat opsional: kalau gagal, lagu tetap tersimpan tanpa cover. */
    internal fun downloadThumbnail(url: String, dir: File) = this.__split_downloadThumbnail_11(url, dir)

    internal fun writeMeta(dir: File, t: RexTrack) = this.__split_writeMeta_12(dir, t)


    internal fun writeLyrics(dir: File, lyrics: Lyrics?, unavailable: Boolean) = this.__split_writeLyrics_13(dir, lyrics, unavailable)

    internal fun readMeta(dir: File): JSONObject? = this.__splitExtra_readMeta_7(dir)

    /** Buka koneksi dan ikuti redirect sendiri (termasuk http -> https). */
    internal fun openConnection(url: String): HttpURLConnection = this.__split_openConnection_14(url)

    internal fun cleanup(key: String) = this.__split_cleanup_15(key)

    //  Scan 

    internal fun scan(): List<OfflineEntry> = this.__split_scan_16()

    internal fun findThumbnail(dir: File): File? = this.__splitExtra_findThumbnail_8(dir)

    //  Status helpers 

    internal fun setStatus(key: String, status: DownloadStatus) = this.__split_setStatus_17(key, status)

    internal fun clearStatus(key: String) = this.__splitExtra_clearStatus_9(key)
}
