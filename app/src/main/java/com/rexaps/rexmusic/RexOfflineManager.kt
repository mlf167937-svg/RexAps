package com.rexaps.rexmusic

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.File
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

/**
 * Mengelola lagu offline di /storage/emulated/0/Download/RexAps/Music/<Judul-Artis>/
 *   music.mp3, thumbnail.jpg|png, meta.json
 * Unduhan berjalan paralel maks 2, lewat file .part dulu supaya tidak ada file setengah jadi.
 */
object RexOfflineManager {
    internal lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private var initialized = false
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


    fun hasAccess(context: Context): Boolean = hasAccessImpl(context)

    fun setOfflineMode(on: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE, on).apply()
        _state.update { it.copy(enabled = on) }
    }

    //  Library 

    fun refresh() {
        scope.launch { refreshNow() }
    }

    fun isDownloaded(track: RexTrack): Boolean = _state.value.isDownloaded(track)
    fun importSyncTrack(track: RexTrack, sourceDir: File): Boolean = importSyncTrackImpl(track, sourceDir)
    fun audioFile(track: RexTrack): File? = audioFileImpl(track)
    fun lyricsFile(track: RexTrack): File? = lyricsFileImpl(track)
    fun readOfflineLyrics(track: RexTrack): Lyrics? = readOfflineLyricsImpl(track)

    fun download(track: RexTrack) = downloadImpl(track)

    fun cancel(track: RexTrack) {
        jobs[offlineKey(track)]?.cancel()
    }

    fun delete(track: RexTrack) = deleteImpl(track)
    fun deleteAll() = deleteAllImpl()

    internal data class DownloadProgress(
        val percent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long
    )
}
