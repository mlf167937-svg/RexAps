package com.rexaps.rexmusic

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** [source] = URL audio (online) atau path file (local = true). */
    internal fun RexPlayerController.__split_startPlayer_29(source: String, local: Boolean = false) {
        if (source.isBlank()) return fail("URL audio kosong")
        stopPlayer()
        if (!requestFocus()) return fail("audio sedang dipakai aplikasi lain")
        try {
            val mp = MediaPlayer()
            player = mp
            runCatching { mp.setVolume(syncVolume, syncVolume) }
            preparing = true
            publishLoading()
            mp.setAudioAttributes(audioAttrs)
            runCatching { mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK) }
            if (local) {
                // Use the path overload for legacy Android (including Android 8).
                // Keeping an FD open only until setDataSource returns can trigger
                // vendor-specific native MediaPlayer failures during prepareAsync.
                mp.setDataSource(File(source).absolutePath)
            } else {
                mp.setDataSource(source)
            }
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                prepared = true
                preparing = false
                buffering = false
                val preparePaused = syncPrepareTrackId == _state.value.nowPlaying?.id
                if (preparePaused) syncPrepareTrackId = null
                _state.update { s -> s.copy(durationMs = mp.duration.toLong(), isPlaying = !preparePaused) }
                publishLoading()
                // MediaPlayer is already stopped before its first start after onPrepared.
                // Calling pause() in this state can raise MEDIA_ERROR_INVALID_OPERATION (-38).
                // For sync preparation, leave it prepared and silent until startSyncPlayback().
                if (!preparePaused) mp.start()
                startProgressLoop(mp)
                _state.value.nowPlaying?.let { pushHistory(it) }
            }
            mp.setOnBufferingUpdateListener { _, percent ->
                if (player !== mp) return@setOnBufferingUpdateListener
                _state.update { s -> s.copy(bufferedPercent = percent.coerceIn(0, 100)) }
            }
            mp.setOnInfoListener { _, what, _ ->
                if (player === mp) {
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> setBuffering(true)
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> setBuffering(false)
                    }
                }
                false
            }
            mp.setOnCompletionListener {
                if (player !== mp) return@setOnCompletionListener
                val left = _state.value.repeatLeft
                if (left > 0) {
                    // ulangi lagu yang sama tanpa memanggil API lagi
                    _state.value.nowPlaying?.let { taste.recordPlay(it.artist, REPEAT_TASTE_WEIGHT) }
                    _state.update { s ->
                        s.copy(
                            repeatLeft = left - 1, positionMs = 0L, isPlaying = true,
                            seekVersion = s.seekVersion + 1
                        )
                    }
                    runCatching {
                        mp.seekTo(0)
                        mp.start()
                    }
                } else {
                    _state.update { s -> s.copy(isPlaying = false, repeatTotal = 0, repeatLeft = 0) }
                    cancelPending()
                    advance()
                }
            }
            mp.setOnErrorListener { _, what, extra ->
                // Only invalidate resolved network audio. A local MP3 error should
                // not evict any cached API URL for the same track.
                if (!local) {
                    queue.getOrNull(queueIndex)?.let { RexMusicApi.invalidateAudio(it) }
                    _state.value.nowPlaying?.let { RexMusicApi.invalidateAudio(it) }
                }
                stopPlayer() // supaya tombol play bisa mencoba ulang
                fail("gagal memutar audio (kode $what, detail $extra)")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stopPlayer()
            fail(e.message ?: "gagal memutar audio")
        }
    }

internal fun RexPlayerController.__split_stopPlayer_30() {
        progressJob?.cancel()
        seekJob?.cancel()
        seekPending = false
        prepared = false
        preparing = false
        buffering = false
        runCatching { player?.release() }
        player = null
        abandonFocus()
    }

/** Hentikan total (tombol tutup di notifikasi). Lagu tetap tercatat, bisa diputar ulang. */
    fun RexPlayerController.__split_stop_31() {
        cancelPending()
        resolving = false
        stopPlayer()
        _state.update {
            it.copy(
                isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0,
                repeatTotal = 0, repeatLeft = 0
            )
        }
        publishLoading()
    }

/** Progress + deteksi lag + pencatatan selera (>= 15 detik dianggap benar-benar didengar). */
    internal fun RexPlayerController.__split_startProgressLoop_32(mp: MediaPlayer) {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastPos = -1L
            var stalledTicks = 0
            while (isActive && player === mp) {
                if (!seekPending) {
                    runCatching {
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition.toLong()
                            _state.update { it.copy(positionMs = pos) }
                            if (pos == lastPos) {
                                if (++stalledTicks >= STALL_TICKS) setBuffering(true)
                            } else {
                                stalledTicks = 0
                                lastPos = pos
                                setBuffering(false)
                            }
                            if (!tasteCounted && pos >= TASTE_LISTEN_MS) {
                                tasteCounted = true
                                _state.value.nowPlaying?.let { taste.recordPlay(it.artist) }
                                publishTaste()
                            }
                        } else {
                            stalledTicks = 0
                        }
                    }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

/** Play/pause. Kalau player sudah mati (error / ditutup) -> putar ulang lagu ini. */
    fun RexPlayerController.__split_togglePlay_33() {
        val mp = player
        if (mp == null) {
            val now = _state.value.nowPlaying
            if (!resolving && now != null) {
                cancelPending()
                beginTrack(now)
            }
            return
        }
        if (!prepared) return
        if (runCatching { mp.isPlaying }.getOrDefault(false)) {
            pauseInternal()
        } else if (requestFocus()) {
            runCatching { mp.start() }
            _state.update { it.copy(isPlaying = true) }
        }
    }

/** Immediate seek for sync correction; unlike the UI drag path, this is not debounced. */
    fun RexPlayerController.__split_seekToPositionImmediate_34(positionMs: Long) {
        val mp = player ?: return
        val d = _state.value.durationMs
        if (!prepared || d <= 0L) return
        val target = positionMs.coerceIn(0L, d).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        seekJob?.cancel()
        seekPending = false
        runCatching { mp.seekTo(target) }
        _state.update { it.copy(positionMs = target.toLong(), seekVersion = it.seekVersion + 1) }
    }

/** Seek absolut, dipakai synced lyrics supaya baris yang diketuk tepat ke timestamp API. */
    fun RexPlayerController.__split_seekToPosition_36(positionMs: Long) {
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

internal fun RexPlayerController.__split_requestFocus_37(): Boolean {
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
