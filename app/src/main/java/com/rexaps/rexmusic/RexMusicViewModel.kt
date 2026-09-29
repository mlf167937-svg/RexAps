package com.rexaps.rexmusic

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RexMusicViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(
        RexMusicUiState(tracks = RexMusicRegistry.defaultTracks)
    )
    val state: StateFlow<RexMusicUiState> = _state.asStateFlow()

    // player
    private var player: MediaPlayer? = null
    private var prepared = false
    private var progressJob: Job? = null
    private var seekJob: Job? = null
    private var seekPending = false

    // search / resolve
    private var searchJob: Job? = null
    private var playJob: Job? = null
    private var searching = false
    private var resolving = false
    private var resolveText = ""

    // queue: snapshot list yang sedang diputar (search ATAU rekomendasi)
    private var queue: List<RexTrack> = emptyList()
    private var queueIndex = 0

    // audio focus
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnGain = false
    private val audioAttrs = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    // ───────────────────────── Search ─────────────────────────

    /** Debounced: aman dipanggil di tiap ketikan. */
    fun search(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            clearSearch()
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searching = true
            _state.update { it.copy(error = null) }
            publishLoading()
            RexMusicApi.search(q)
                .onSuccess { list ->
                    searching = false
                    _state.update { it.copy(searchResults = list) }
                }
                .onFailure { e ->
                    searching = false
                    _state.update { it.copy(error = e.message ?: "search gagal") }
                }
            publishLoading()
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        searching = false
        _state.update { it.copy(searchResults = emptyList()) }
        publishLoading()
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    // ───────────────────────── Queue & play ─────────────────────────

    fun playFromMain(index: Int) {
        val list = _state.value.tracks
        if (index !in list.indices) return
        startQueue(list, index)
    }

    fun playFromSearch(index: Int) {
        val list = _state.value.searchResults
        if (index !in list.indices) return
        startQueue(list, index)
    }

    private fun startQueue(list: List<RexTrack>, index: Int) {
        queue = list
        queueIndex = index
        playCurrent()
    }

    private fun playCurrent() {
        val track = queue.getOrNull(queueIndex) ?: return
        playJob?.cancel()
        stopPlayer()
        resolving = true
        resolveText = if (track.spotifyUrl.isBlank()) "mencari ${track.title}..." else "menyiapkan audio..."
        _state.update {
            it.copy(
                nowPlaying = track, currentIndex = queueIndex,
                isPlaying = false, positionMs = 0L, durationMs = 0L, error = null
            )
        }
        publishLoading()

        playJob = viewModelScope.launch {
            resolveTrack(track)
                .onSuccess { fresh ->
                    resolving = false
                    replaceTrack(fresh)
                    publishLoading()
                    startPlayer(fresh.audioUrl)
                }
                .onFailure { e ->
                    resolving = false
                    _state.update { it.copy(error = e.message ?: "gagal memuat audio") }
                    publishLoading()
                }
        }
    }

    /**
     * spotifyUrl ada → langsung resolve audio.
     * Kosong (lagu default) → search "artist + title", ambil hasil pertama, resolve.
     * id/title/artist/album dari registry dipertahankan.
     */
    private suspend fun resolveTrack(track: RexTrack): Result<RexTrack> {
        if (track.spotifyUrl.isNotBlank()) return RexMusicApi.resolveAudio(track)

        val list = RexMusicApi.search("${track.artist} ${track.title}".trim())
            .getOrElse { return Result.failure(it) }
        val first = list.firstOrNull()
            ?: return Result.failure(Exception("lagu \"${track.title}\" tidak ditemukan"))

        return RexMusicApi.resolveAudio(
            first.copy(id = track.id, title = track.title, artist = track.artist, album = track.album)
        )
    }

    /** Update track di queue + kedua list berdasarkan id (bukan index, jadi anti out-of-bounds). */
    private fun replaceTrack(fresh: RexTrack) {
        queue = queue.replaceById(fresh)
        _state.update {
            it.copy(
                nowPlaying = fresh,
                tracks = it.tracks.replaceById(fresh),
                searchResults = it.searchResults.replaceById(fresh)
            )
        }
    }

    fun next() {
        if (queue.isEmpty()) return
        queueIndex = (queueIndex + 1) % queue.size
        playCurrent()
    }

    fun prev() {
        if (queue.isEmpty()) return
        if (prepared && _state.value.positionMs > RESTART_THRESHOLD_MS) {
            seekTo(0f)
            return
        }
        queueIndex = if (queueIndex - 1 < 0) queue.size - 1 else queueIndex - 1
        playCurrent()
    }

    fun currentTrack(): RexTrack? = _state.value.nowPlaying

    // ───────────────────────── Player ─────────────────────────

    private fun startPlayer(url: String) {
        if (url.isBlank()) return fail("URL audio kosong")
        stopPlayer()
        if (!requestFocus()) return fail("audio sedang dipakai aplikasi lain")
        try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(audioAttrs)
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                prepared = true
                _state.update { s -> s.copy(durationMs = mp.duration.toLong(), isPlaying = true) }
                mp.start()
                startProgressLoop(mp)
            }
            mp.setOnCompletionListener {
                _state.update { s -> s.copy(isPlaying = false) }
                next()
            }
            mp.setOnErrorListener { _, what, _ ->
                queue.getOrNull(queueIndex)?.let { RexMusicApi.invalidateAudio(it) }
                fail("gagal memutar audio (kode $what)")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stopPlayer()
            fail(e.message ?: "gagal memutar audio")
        }
    }

    private fun stopPlayer() {
        progressJob?.cancel()
        seekJob?.cancel()
        seekPending = false
        prepared = false
        runCatching { player?.release() }
        player = null
        abandonFocus()
    }

    private fun startProgressLoop(mp: MediaPlayer) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && player === mp) {
                if (!seekPending) {
                    runCatching {
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition.toLong()
                            _state.update { it.copy(positionMs = pos) }
                        }
                    }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    fun togglePlay() {
        val mp = player ?: return
        if (!prepared) return
        if (runCatching { mp.isPlaying }.getOrDefault(false)) {
            pauseInternal()
        } else if (requestFocus()) {
            runCatching { mp.start() }
            _state.update { it.copy(isPlaying = true) }
        }
    }

    /** Posisi UI langsung update; seek asli didebounce supaya drag tidak membebani MediaPlayer. */
    fun seekTo(ratio: Float) {
        val mp = player ?: return
        val d = _state.value.durationMs
        if (!prepared || d <= 0) return
        val target = (d * ratio.coerceIn(0f, 1f)).toLong()
        seekPending = true
        _state.update { it.copy(positionMs = target) }
        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            delay(SEEK_DEBOUNCE_MS)
            runCatching { mp.seekTo(target.toInt()) }
            delay(300)
            seekPending = false
        }
    }

    private fun pauseInternal() {
        if (prepared) runCatching { player?.pause() }
        _state.update { it.copy(isPlaying = false) }
    }

    private fun fail(message: String) =
        _state.update { it.copy(error = message, isPlaying = false) }

    // ───────────────────────── Audio focus ─────────────────────────

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
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

    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttrs)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
                .also { focusRequest = it }
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    // ───────────────────────── Misc ─────────────────────────

    /** loading = sedang search ATAU sedang resolve audio. */
    private fun publishLoading() = _state.update {
        it.copy(
            loading = searching || resolving,
            loadingText = when {
                resolving -> resolveText
                searching -> "mencari..."
                else -> ""
            }
        )
    }

    private fun List<RexTrack>.replaceById(t: RexTrack) = map { if (it.id == t.id) t else it }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        playJob?.cancel()
        stopPlayer()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
        const val SEEK_DEBOUNCE_MS = 80L
        const val PROGRESS_INTERVAL_MS = 500L
        const val RESTART_THRESHOLD_MS = 3000L
    }
}
