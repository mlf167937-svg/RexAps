package com.rexaps.rexmusic

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RexMusicViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        RexMusicUiState(tracks = RexMusicRegistry.defaultTracks)
    )
    val state: StateFlow<RexMusicUiState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null

    fun search(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }
        _state.value = _state.value.copy(loading = true, loadingText = "mencari...")
        viewModelScope.launch {
            RexMusicApi.search(query)
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        loading = false,
                        loadingText = "",
                        searchResults = list
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        loadingText = "",
                        error = e.message ?: "search gagal"
                    )
                }
        }
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchResults = emptyList())
    }

    fun playFromMain(index: Int) {
        val track = _state.value.tracks.getOrNull(index) ?: return
        playTrack(track, index, isSearch = false)
    }

    fun playFromSearch(index: Int) {
        val track = _state.value.searchResults.getOrNull(index) ?: return
        playTrack(track, index, isSearch = true)
    }

    private fun playTrack(track: RexTrack, index: Int, isSearch: Boolean) {
        _state.value = _state.value.copy(
            loading = true,
            loadingText = "menyiapkan audio...",
            currentIndex = index,
            error = null
        )

        viewModelScope.launch {
            RexMusicApi.resolveAudio(track)
                .onSuccess { fresh ->
                    if (isSearch) {
                        val updated = _state.value.searchResults.toMutableList().also { it[index] = fresh }
                        _state.value = _state.value.copy(
                            loading = false,
                            loadingText = "",
                            searchResults = updated,
                            isPlaying = false,
                            positionMs = 0L,
                            durationMs = 0L
                        )
                    } else {
                        val updated = _state.value.tracks.toMutableList().also { it[index] = fresh }
                        _state.value = _state.value.copy(
                            loading = false,
                            loadingText = "",
                            tracks = updated,
                            isPlaying = false,
                            positionMs = 0L,
                            durationMs = 0L
                        )
                    }
                    startPlayer(fresh.audioUrl)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        loadingText = "",
                        error = e.message ?: "gagal resolve audio"
                    )
                }
        }
    }

    private fun startPlayer(url: String) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    _state.value = _state.value.copy(
                        durationMs = mp.duration.toLong(),
                        isPlaying = true
                    )
                    mp.start()
                    startProgressLoop()
                }
                setOnCompletionListener {
                    _state.value = _state.value.copy(isPlaying = false)
                    next()
                }
                setOnErrorListener { _, _, _ ->
                    _state.value = _state.value.copy(isPlaying = false, error = "media error")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val mp = player ?: break
                try {
                    if (mp.isPlaying) {
                        _state.value = _state.value.copy(positionMs = mp.currentPosition.toLong())
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }

    fun togglePlay() {
        val mp = player ?: return
        if (mp.isPlaying) {
            mp.pause()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            mp.start()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    fun seekTo(ratio: Float) {
        val mp = player ?: return
        val d = mp.duration
        if (d > 0) {
            val target = (d * ratio.coerceIn(0f, 1f)).toInt()
            mp.seekTo(target)
            _state.value = _state.value.copy(positionMs = target.toLong())
        }
    }

    fun next() {
        val s = _state.value
        if (s.tracks.isEmpty()) return
        playFromMain((s.currentIndex + 1) % s.tracks.size)
    }

    fun prev() {
        val s = _state.value
        if (s.tracks.isEmpty()) return
        playFromMain(if (s.currentIndex - 1 < 0) s.tracks.size - 1 else s.currentIndex - 1)
    }

    fun currentTrack(): RexTrack? =
        _state.value.tracks.getOrNull(_state.value.currentIndex)

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        player?.release()
        player = null
    }
}
