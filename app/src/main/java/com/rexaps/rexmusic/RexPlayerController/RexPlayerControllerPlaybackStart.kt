package com.rexaps.rexmusic

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Local playback gain for RexMusicSync; does not change Android system volume. */
internal fun RexPlayerController.setSyncVolumeImpl(volume: Float) {
    syncVolume = volume.coerceIn(0f, 1f)
    runCatching { player?.setVolume(syncVolume, syncVolume) }
}

/** Posisi UI langsung update; seek asli didebounce supaya drag tidak membebani MediaPlayer. */
internal fun RexPlayerController.seekToImpl(ratio: Float) {
    val d = _state.value.durationMs
    if (d <= 0L) return
    seekToPosition((d * ratio.coerceIn(0f, 1f)).toLong())
}

/** Immediate seek for sync correction; unlike the UI drag path, this is not debounced. */
internal fun RexPlayerController.seekToPositionImmediateImpl(positionMs: Long) {
    val mp = player ?: return
    val d = _state.value.durationMs
    if (!prepared || d <= 0L) return
    val target = positionMs.coerceIn(0L, d).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    seekJob?.cancel()
    seekPending = false
    runCatching { mp.seekTo(target) }
    _state.update { it.copy(positionMs = target.toLong(), seekVersion = it.seekVersion + 1) }
}

/** Start the already-prepared player at the requested sync position. Must be called on main. */
internal fun RexPlayerController.startSyncPlaybackImpl(positionMs: Long) {
    val mp = player ?: return
    if (!prepared || !requestFocus()) return
    val d = _state.value.durationMs
    val target = positionMs.coerceIn(0L, d).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    seekJob?.cancel()
    seekPending = false
    if (kotlin.math.abs(_state.value.positionMs - target.toLong()) <= 25L) {
        runCatching { if (!mp.isPlaying) mp.start() }
        _state.update { it.copy(isPlaying = true) }
        return
    }
    runCatching {
        mp.setOnSeekCompleteListener { completed ->
            if (player === completed) {
                runCatching { if (!completed.isPlaying) completed.start() }
                _state.update { it.copy(positionMs = target.toLong(), isPlaying = true) }
                completed.setOnSeekCompleteListener(null)
            }
        }
        mp.seekTo(target)
    }.onFailure {
        runCatching { if (!mp.isPlaying) mp.start() }
        _state.update { it.copy(isPlaying = true) }
    }
}

/** Seek absolut, dipakai synced lyrics supaya baris yang diketuk tepat ke timestamp API. */
internal fun RexPlayerController.seekToPositionImpl(positionMs: Long) {
    val mp = player ?: return
    val d = _state.value.durationMs
    if (!prepared || d <= 0L) return
    val target = positionMs.coerceIn(0L, d)
    seekPending = true
    _state.update { it.copy(positionMs = target, seekVersion = it.seekVersion + 1) }
    seekJob?.cancel()
    seekJob = scope.launch {
        delay(SEEK_DEBOUNCE_MS)
        runCatching { mp.seekTo(target.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
        delay(180)
        seekPending = false
    }
}

internal fun RexPlayerController.pauseInternal() {
    if (prepared) runCatching { player?.pause() }
    buffering = false
    _state.update { it.copy(isPlaying = false) }
    publishLoading()
}

internal fun RexPlayerController.fail(message: String) {
    preparing = false
    buffering = false
    _state.update { it.copy(error = message, isPlaying = false) }
    publishLoading()
}

internal fun RexPlayerController.requestFocus(): Boolean {
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

internal fun RexPlayerController.abandonFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    } else {
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(focusListener)
    }
}

//  Misc 

/**
 * loading = search / resolve / prepare.
 * Buffering (lag saat memutar) hanya lewat [LoadPhase] supaya halaman browse tidak ikut loading.
 */
internal fun RexPlayerController.publishLoading() = _state.update {
    val phase = when {
        resolving -> LoadPhase.Resolving
        preparing -> LoadPhase.Preparing
        buffering -> LoadPhase.Buffering
        searching -> LoadPhase.Searching
        else -> LoadPhase.Idle
    }
    it.copy(
        loading = searching || resolving || preparing,
        phase = phase,
        loadingText = when (phase) {
            LoadPhase.Resolving -> resolveText
            LoadPhase.Preparing -> "menyiapkan audio..."
            LoadPhase.Buffering -> "buffering..."
            LoadPhase.Searching -> "mencari..."
            LoadPhase.Idle -> ""
        }
    )
}
