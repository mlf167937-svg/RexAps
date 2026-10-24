package com.rexaps.rexmusic

import android.media.MediaPlayer
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun RexPlayerController.__splitExtra_dismissError_8() = _state.update { it.copy(error = null) }

internal fun RexPlayerController.__splitExtra_consumeNotice_9() = _state.update { it.copy(notice = null) }

internal fun RexPlayerController.__splitExtra_postNotice_10(message: String) = _state.update { it.copy(notice = message) }

internal fun RexPlayerController.__splitExtra_playFromMain_12(index: Int) {
        val list = _state.value.tracks
        if (index !in list.indices) return
        startQueue(list, index)
    }

/** Ulangi lagu yang sedang diputar sebanyak [times] kali lagi (0 = mati, maks 50). */
    internal fun RexPlayerController.__splitExtra_setRepeat_21(times: Int) {
        val n = times.coerceIn(0, MAX_REPEAT)
        _state.update { it.copy(repeatTotal = n, repeatLeft = n) }
    }

/** Tombol next dari pengguna. */
    internal fun RexPlayerController.__splitExtra_next_23() {
        penalizeEarlySkip()
        cancelPending()
        advance()
    }

internal fun RexPlayerController.__splitExtra_currentTrack_24(): RexTrack? = _state.value.nowPlaying

internal fun RexPlayerController.__splitExtra_cancelPending_25() {
        playJob?.cancel()
        playJob = null
    }

internal fun RexPlayerController.__splitExtra_beginTrack_26(track: RexTrack) {
        stopPlayer()
        markTrack(track)
        loadLyrics(track)
        playJob = scope.launch { resolveAndStart(track) }
    }

internal fun RexPlayerController.__splitExtra_recentKeys_29(): Set<String> {
        val keys = history.takeLast(RECENT_WINDOW).map { songKey(it) }.toMutableSet()
        _state.value.nowPlaying?.let { keys += songKey(it) }
        return keys
    }

internal fun RexPlayerController.__splitExtra_sameSong_31(a: RexTrack, b: RexTrack): Boolean =
        a.id == b.id ||
            (a.title.equals(b.title, ignoreCase = true) && a.artist.equals(b.artist, ignoreCase = true))

internal fun RexPlayerController.__splitExtra_setBuffering_32(value: Boolean) {
        if (buffering == value) return
        buffering = value
        publishLoading()
    }

internal fun RexPlayerController.__splitExtra_play_33() {
        if (!_state.value.isPlaying) togglePlay()
    }

internal fun RexPlayerController.__splitExtra_pause_34() {
        if (_state.value.isPlaying) togglePlay()
    }

/** Posisi UI langsung update; seek asli didebounce supaya drag tidak membebani MediaPlayer. */
    internal fun RexPlayerController.__splitExtra_seekTo_36(ratio: Float) {
        val d = _state.value.durationMs
        if (d <= 0L) return
        seekToPosition((d * ratio.coerceIn(0f, 1f)).toLong())
    }

internal fun RexPlayerController.__splitExtra_pauseInternal_37() {
        if (prepared) runCatching { player?.pause() }
        buffering = false
        _state.update { it.copy(isPlaying = false) }
        publishLoading()
    }

internal fun RexPlayerController.__splitExtra_fail_38(message: String) {
        preparing = false
        buffering = false
        _state.update { it.copy(error = message, isPlaying = false) }
        publishLoading()
    }
