package com.rexaps.rexmusic

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pemutar musik tingkat aplikasi. Hidup di luar ViewModel supaya musik tetap jalan
 * saat layar ditutup dan bisa dikontrol dari notifikasi / lock screen.
 * Semua fungsi dipanggil dari main thread.
 */
object RexPlayerController {
    internal lateinit var appContext: Context
    internal lateinit var audioManager: AudioManager
    internal lateinit var store: RexHistoryStore
    internal lateinit var queueStore: RexQueueStore
    internal lateinit var taste: RexTasteStore
    internal lateinit var playlistStore: RexPlaylistStore
    internal var initialized = false
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    internal val _state = MutableStateFlow(
        RexMusicUiState(tracks = RexMusicRegistry.defaultTracks)
    )

    val state: StateFlow<RexMusicUiState> = _state.asStateFlow()

    // player
    internal var player: MediaPlayer? = null

    internal var syncVolume: Float = 1f
    internal var syncPrepareTrackId: String? = null
    internal var prepared = false
    internal var progressJob: Job? = null
    internal var seekJob: Job? = null
    internal var seekPending = false
    internal var tasteCounted = false

    // search / resolve / prepare / buffering flags
    internal var searchJob: Job? = null

    internal var playJob: Job? = null
    internal var searching = false
    internal var resolving = false
    internal var preparing = false
    internal var buffering = false
    internal var resolveText = ""

    // konteks: daftar asal lagu (search / rekomendasi / riwayat / perpustakaan offline)
    internal var queue: List<RexTrack> = emptyList()

    internal var queueIndex = 0

    // true = tombol next mengikuti urutan konteks (dipakai perpustakaan offline), bukan autoplay acak
    internal var followContext = false

    // antrian buatan pengguna (metadata saja) + autoplay pintar
    internal val userQueue = mutableListOf<RexTrack>()

    internal var autoplay = true

    // riwayat kronologis (terlama di depan, terbaru di belakang). Hanya metadata.
    internal val history = mutableListOf<RexTrack>()

    // audio focus
    internal var focusRequest: AudioFocusRequest? = null

    private var resumeOnGain = false

    internal val audioAttrs = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        store = RexHistoryStore(appContext)
        queueStore = RexQueueStore(appContext)
        taste = RexTasteStore(appContext)
        playlistStore = RexPlaylistStore(appContext)
        history.addAll(store.load())
        userQueue.addAll(queueStore.load())
        autoplay = taste.autoplay
        _state.update { it.copy(popularCount = RexPopularSongs.tracks.size, playlists = playlistStore.list()) }

        RexOfflineManager.init(appContext)
        scope.launch { RexOfflineManager.state.collect { onOfflineChanged(it) } }
        scope.launch { RexOfflineManager.messages.collect { postNotice(it) } }

        publishQueueInfo()
        publishTaste()
    }


    fun setOfflineMode(on: Boolean) = setOfflineModeImpl(on)

    // ---------------- Playlist JSON ----------------

    fun refreshPlaylists() {
        if (!initialized) return
        _state.update { it.copy(playlists = playlistStore.list()) }
    }

    fun createPlaylist(name: String) = createPlaylistImpl(name)
    fun addToPlaylist(name: String, track: RexTrack) = addToPlaylistImpl(name, track)

    fun removeFromPlaylist(name: String, track: RexTrack) {
        if (playlistStore.removeTrack(name, track.id)) {
            refreshPlaylists()
            postNotice("Dihapus dari $name")
        }
    }

    fun deletePlaylist(name: String) {
        if (playlistStore.delete(name)) {
            refreshPlaylists()
            postNotice("Playlist dihapus: $name")
        }
    }

    fun playPlaylist(name: String, startTrackId: String? = null) = playPlaylistImpl(name, startTrackId)
    fun playFromOffline(track: RexTrack) = playFromOfflineImpl(track)
    fun prepareFromOfflineForSync(track: RexTrack) = prepareFromOfflineForSyncImpl(track)
    fun playOfflineAll(shuffle: Boolean) = playOfflineAllImpl(shuffle)

    fun search(query: String) = searchImpl(query)

    fun clearSearch() {
        searchJob?.cancel()
        searching = false
        _state.update { it.copy(searchResults = emptyList()) }
        publishLoading()
    }

    fun dismissError() = _state.update { it.copy(error = null) }
    fun consumeNotice() = _state.update { it.copy(notice = null) }

    fun clearHistory() {
        history.clear()
        store.save(history)
        publishQueueInfo()
    }


    fun playFromMain(index: Int) = playFromMainImpl(index)

    fun playFromSearch(index: Int) {
        val list = _state.value.searchResults
        if (index !in list.indices) return
        startQueue(list, index)
    }

    fun playFromHistory(index: Int) = playFromHistoryImpl(index)

    /** Mix pintar: langsung pilih lagu dari artis favorit / populer (atau dari unduhan saat offline). */
    fun playMix() {
        cancelPending()
        followContext = false
        playSmart()
    }


    fun addToQueue(track: RexTrack) = addToQueueImpl(track)

    fun removeFromQueue(index: Int) {
        if (index !in userQueue.indices) return
        userQueue.removeAt(index)
        persistQueue()
        publishQueueInfo()
    }

    fun moveQueueToTop(index: Int) = moveQueueToTopImpl(index)

    fun clearQueue() {
        if (userQueue.isEmpty()) return
        userQueue.clear()
        persistQueue()
        publishQueueInfo()
        postNotice("Antrian dikosongkan")
    }

    fun playFromQueue(index: Int) = playFromQueueImpl(index)

    fun setRepeat(times: Int) = setRepeatImpl(times)

    fun setAutoplay(on: Boolean) {
        autoplay = on
        taste.autoplay = on
        publishQueueInfo()
        postNotice(if (on) "Autoplay pintar aktif" else "Mengikuti urutan daftar")
    }

    //  Next / previous 

    /** Tombol next dari pengguna. */
    fun next() {
        penalizeEarlySkip()
        cancelPending()
        advance()
    }

    fun prev(force: Boolean = false) = prevImpl(force)
    fun currentTrack(): RexTrack? = _state.value.nowPlaying

    //  Lyrics 

    internal var lyricsJob: Job? = null

    fun toggleLyrics() = _state.update { it.copy(lyricsVisible = !it.lyricsVisible) }
    fun closeLyrics() = _state.update { it.copy(lyricsVisible = false) }
    fun stop() = stopImpl()
    fun togglePlay() = togglePlayImpl()

    fun play() {
        if (!_state.value.isPlaying) togglePlay()
    }

    fun pause() {
        if (_state.value.isPlaying) togglePlay()
    }

    fun setSyncVolume(volume: Float) = setSyncVolumeImpl(volume)
    fun seekTo(ratio: Float) = seekToImpl(ratio)
    fun seekToPositionImmediate(positionMs: Long) = seekToPositionImmediateImpl(positionMs)
    fun startSyncPlayback(positionMs: Long) = startSyncPlaybackImpl(positionMs)
    fun seekToPosition(positionMs: Long) = seekToPositionImpl(positionMs)

    //  Audio focus 

    internal val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnGain = false
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnGain = _state.value.isPlaying
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                player?.setVolume(1f, 1f)
                if (resumeOnGain && prepared) {
                    resumeOnGain = false
                    runCatching { player?.start() }
                    _state.update { it.copy(isPlaying = true) }
                }
            }
        }
    }

    internal fun List<RexTrack>.replaceById(t: RexTrack) = map { if (it.id == t.id) t else it }
}
