package com.rexaps.rexmusic

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.max

/** Changes the Android media stream (hardware/system volume), not only MediaPlayer gain. */
internal fun readAndroidMusicVolume(context: Context): Float {
    return runCatching {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVolume <= 0) 1f else audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
    }.getOrDefault(-1f)
}

internal fun setAndroidMusicVolume(context: Context, value: Float): Float {
    return runCatching {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVolume <= 0) return@runCatching -1f
        val requested = (value.coerceIn(0f, 1f) * maxVolume).toInt().coerceIn(0, maxVolume)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, requested, 0)
        audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
    }.getOrDefault(-1f)
}

/** Host-only hard resync: freeze, seek to one captured position, then use one shared deadline. */
internal fun RexMusicSyncEngine.__splitImpl_refreshPlaybackSync() {
    if (!_state.value.hosting || hostPreparing || _state.value.resyncing) return
    val track = hostTrack ?: return
    val playerState = RexPlayerController.state.value
    if (playerState.nowPlaying?.id != track.id || playerState.durationMs <= 0L) {
        _state.value = _state.value.copy(status = "Refresh belum siap", error = "Lagu host belum siap untuk disinkronkan ulang.")
        return
    }

    val now = SystemClock.elapsedRealtime()
    val capturedMusicPosition = playerState.positionMs.coerceIn(0L, playerState.durationMs)
    val capturedRoomTime = (now - roomStartedElapsedMs).coerceAtLeast(0L)
    val sharedStartAt = now + MANUAL_RESYNC_LEAD_MS

    playbackJob?.cancel()
    hostPreparing = true // pauses periodic state/tick broadcasts during the refresh window
    hostPlaying = true
    playAnchorPositionMs = capturedMusicPosition
    playAnchorWallMs = sharedStartAt
    _state.value = _state.value.copy(
        trackId = track.id,
        trackTitle = track.title,
        positionMs = capturedMusicPosition,
        playing = false,
        roomElapsedMs = capturedRoomTime,
        resyncing = true,
        lastResyncAtMs = now,
        status = "Menyelaraskan semua perangkat…",
        error = null
    )

    val refreshMessage = JSONObject()
        .put("type", "refresh_sync")
        .put("trackId", track.id)
        .put("title", track.title)
        .put("position", capturedMusicPosition)
        .put("startAt", sharedStartAt)
        .put("roomElapsedMs", capturedRoomTime)
    peers.toList().forEach { peer -> runCatching { send(peer.writer, refreshMessage) } }

    playbackJob = scope.launch {
        withContext(Dispatchers.Main) {
            RexPlayerController.pause()
            RexPlayerController.prepareSyncSeek(capturedMusicPosition)
        }
        delay(max(0L, sharedStartAt - SystemClock.elapsedRealtime()))
        withContext(Dispatchers.Main) {
            if (RexPlayerController.state.value.nowPlaying?.id == track.id) {
                RexPlayerController.startSyncPlayback(capturedMusicPosition)
            }
        }
        hostPreparing = false
        hostPlaying = true
        _state.value = _state.value.copy(
            playing = true,
            positionMs = capturedMusicPosition,
            roomElapsedMs = (SystemClock.elapsedRealtime() - roomStartedElapsedMs).coerceAtLeast(capturedRoomTime),
            resyncing = false,
            status = "Sinkronisasi diperbarui · semua perangkat berjalan"
        )
        broadcastState()
    }
}

/** Enables a host-only periodic refresh. It never interrupts a paused room. */
internal fun RexMusicSyncEngine.__splitImpl_setAutoRefresh(enabled: Boolean) {
    if (!_state.value.hosting) return
    autoRefreshJob?.cancel()
    autoRefreshJob = null
    _state.value = _state.value.copy(autoRefreshEnabled = enabled)
    if (!enabled) return

    autoRefreshJob = scope.launch {
        while (isActive && _state.value.hosting && _state.value.autoRefreshEnabled) {
            delay(AUTO_RESYNC_INTERVAL_MS)
            if (_state.value.hosting && hostPlaying && hostTrack != null && !hostPreparing && !_state.value.resyncing) {
                refreshPlaybackSync()
            }
        }
    }
}

/** Client-side half of the refresh handshake. The host-provided monotonic deadline is
 * translated through the latest clock-offset estimate before the local player starts. */
internal suspend fun RexMusicSyncEngine.__splitImpl_applyRemoteRefresh(
    trackId: String,
    positionMs: Long,
    hostStartAtMs: Long,
    roomElapsedMs: Long
) {
    val context = appContextRef ?: return
    RexPlayerController.init(context)
    val local = RexPlayerController.state.value
    if (local.nowPlaying?.id != trackId || local.durationMs <= 0L) {
        syncLocalTrack(trackId, positionMs, true, hostStartAtMs)
        _state.value = _state.value.copy(
            positionMs = positionMs,
            playing = true,
            roomElapsedMs = roomElapsedMs,
            resyncing = false,
            status = "Terhubung kembali setelah refresh"
        )
        return
    }

    withContext(Dispatchers.Main) {
        RexPlayerController.pause()
        RexPlayerController.prepareSyncSeek(positionMs)
    }

    val localStartAt = hostStartAtMs - clockOffsetMs
    delay(max(0L, localStartAt - SystemClock.elapsedRealtime()))
    val lateBy = max(0L, SystemClock.elapsedRealtime() + clockOffsetMs - hostStartAtMs)
    withContext(Dispatchers.Main) {
        if (RexPlayerController.state.value.nowPlaying?.id == trackId) {
            // Keep the pre-seek target unchanged so startSyncPlayback can wait for its
            // matching seek-complete callback instead of racing a second late seek.
            RexPlayerController.startSyncPlayback(positionMs)
        }
    }
    _state.value = _state.value.copy(
        positionMs = positionMs,
        playing = true,
        roomElapsedMs = roomElapsedMs + max(0L, SystemClock.elapsedRealtime() + clockOffsetMs - hostStartAtMs),
        resyncing = false,
        status = "Sinkronisasi room diperbarui"
    )
}
