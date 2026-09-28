package com.rexaps.rexmusic

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private var progressJob: kotlinx.coroutines.Job? = null

    fun play(index: Int) {
        val s = _state.value
        val track = s.tracks.getOrNull(index) ?: return
        _state.value = s.copy(loading = true, currentIndex = index, error = null)

        viewModelScope.launch {
            RexMusicApi.play(track.title)
                .onSuccess { fresh ->
                    val updated = s.tracks.toMutableList().also { it[index] = fresh }
                    _state.value = _state.value.copy(
                        loading = false,
                        tracks = updated,
                        isPlaying = false,
                        positionMs = 0L,
                        durationMs = (fresh.durationSec * 1000L)
                    )
                    startPlayer(fresh.audioUrl)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "gagal load audio"
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
                        _state.value = _state.value.copy(
                            positionMs = mp.currentPosition.toLong()
                        )
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(500)
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
        play((s.currentIndex + 1) % s.tracks.size)
    }

    fun prev() {
        val s = _state.value
        if (s.tracks.isEmpty()) return
        play(if (s.currentIndex - 1 < 0) s.tracks.size - 1 else s.currentIndex - 1)
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
