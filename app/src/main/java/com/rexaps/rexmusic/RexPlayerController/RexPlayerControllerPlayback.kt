package com.rexaps.rexmusic

import android.media.MediaPlayer
import android.os.PowerManager
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun RexPlayerController.postNotice(message: String) = _state.update { it.copy(notice = message) }

internal fun RexPlayerController.cancelPending() {
    playJob?.cancel()
    playJob = null
}

//  Begin / resolve 

internal fun RexPlayerController.beginTrack(track: RexTrack) {
    stopPlayer()
    markTrack(track)
    loadLyrics(track)
    playJob = scope.launch { resolveAndStart(track) }
}

/** Set state "lagu baru" (reset ulangi, posisi, dsb). Tidak menyentuh player. */
internal fun RexPlayerController.markTrack(track: RexTrack) {
    tasteCounted = false
    resolving = true
    resolveText = when {
        RexOfflineManager.isDownloaded(track) -> "membuka dari penyimpanan..."
        track.spotifyUrl.isBlank() -> "mencari ${track.title}..."
        else -> "menyiapkan audio..."
    }
    _state.update {
        it.copy(
            nowPlaying = track, currentIndex = queueIndex,
            isPlaying = false, positionMs = 0L, durationMs = 0L,
            bufferedPercent = 0, seekVersion = it.seekVersion + 1, error = null,
            repeatTotal = 0, repeatLeft = 0, nowPlayingOffline = false
        )
    }
    publishLoading()
    publishQueueInfo()
    RexMusicService.start(appContext)
}

/**
 * File offline dipakai kalau ada (tanpa internet). Kalau belum diunduh: saat mode offline gagal
 * dengan pesan jelas, saat online diambil lewat API. true = pemutaran dimulai.
 */
internal suspend fun RexPlayerController.resolveAndStart(track: RexTrack): Boolean {
    val local = RexOfflineManager.audioFile(track)
    if (local != null) {
        resolving = false
        _state.update { it.copy(nowPlayingOffline = true) }
        publishLoading()
        startPlayer(local.absolutePath, local = true)
        return true
    }
    if (offlineMode()) {
        resolving = false
        _state.update { it.copy(error = "Mode offline: \"${track.title}\" belum diunduh") }
        publishLoading()
        return false
    }

    val result = RexTrackResolver.resolve(track)
    resolving = false
    val fresh = result.getOrNull()
    if (fresh == null) {
        _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "gagal memuat audio") }
        publishLoading()
        return false
    }
    replaceTrack(fresh)
    publishLoading()
    startPlayer(fresh.audioUrl)
    return true
}

/** Update track di queue + semua list berdasarkan id (bukan index, jadi anti out-of-bounds). */
private fun RexPlayerController.replaceTrack(fresh: RexTrack) {
    queue = queue.replaceById(fresh)
    _state.update {
        it.copy(
            nowPlaying = fresh,
            tracks = it.tracks.replaceById(fresh),
            searchResults = it.searchResults.replaceById(fresh),
            history = it.history.replaceById(fresh)
        )
    }
}

//  Player 

/** [source] = URL audio (online) atau path file (local = true). */
private fun RexPlayerController.startPlayer(source: String, local: Boolean = false) {
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

private fun RexPlayerController.setBuffering(value: Boolean) {
    if (buffering == value) return
    buffering = value
    publishLoading()
}

internal fun RexPlayerController.stopPlayer() {
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
internal fun RexPlayerController.stopImpl() {
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
private fun RexPlayerController.startProgressLoop(mp: MediaPlayer) {
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
internal fun RexPlayerController.togglePlayImpl() {
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
