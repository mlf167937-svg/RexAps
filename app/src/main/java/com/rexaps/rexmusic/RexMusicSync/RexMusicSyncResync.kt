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
    val sharedStartAt = now + ROOM_RESYNC_LEAD_MS

    playbackJob?.cancel()
    hostPreparing = true // pauses periodic state/tick broadcasts during the refresh window
    hostPlaying = true
    playAnchorPositionMs = capturedMusicPosition
    playAnchorWallMs = sharedStartAt
    roomPlaybackRevision += 1L
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
        .put("type", "room_resync")
        .put("trackId", track.id)
        .put("title", track.title)
        .put("position", capturedMusicPosition)
        .put("startAt", sharedStartAt)
        .put("roomElapsedMs", capturedRoomTime)
    peers.filter { it.clockSynced && it.assetsReady }.forEach { peer -> runCatching { send(peer.writer, refreshMessage) } }

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
            status = "Refresh manual selesai · semua perangkat berjalan"
        )
        broadcastTick()
    }
}

/** Compare the host player with all recent member reports after translating each
 * report into host elapsedRealtime. The two-second heartbeat is telemetry only. */
internal fun RexMusicSyncEngine.evaluateRoomDrift(source: RexMusicSyncEngine.Peer) {
    val now = SystemClock.elapsedRealtime()
    val track = hostTrack ?: return
    if (!_state.value.hosting || hostPreparing || _state.value.resyncing || !hostPlaying) return
    if (source.reportedTrackId != track.id) return
    if (now - playAnchorWallMs < START_LEAD_MS + 3500L) return

    val hostPlayer = RexPlayerController.state.value
    if (hostPlayer.nowPlaying?.id != track.id || hostPlayer.durationMs <= 0L) return
    val hostPosition = hostPlayer.positionMs.coerceAtLeast(0L)
    val reports = peers.filter { peer ->
        peer.reportedTrackId == track.id && peer.reportedAtHostMs > 0L &&
            now - peer.reportedAtHostMs <= CLIENT_TIMEOUT_MS
    }
    if (reports.isEmpty()) return

    val positions = ArrayList<Long>(reports.size + 1)
    positions += hostPosition
    reports.forEach { peer ->
        val elapsedSinceSample = (now - peer.reportedSampleHostMs).coerceAtLeast(0L)
        val projected = if (peer.reportedPlaying) peer.reportedPositionMs + elapsedSinceSample else peer.reportedPositionMs
        peer.driftMs = kotlin.math.abs(projected - hostPosition)
        positions += projected
    }
    val spread = (positions.maxOrNull() ?: hostPosition) - (positions.minOrNull() ?: hostPosition)
    _state.value = _state.value.copy(syncDriftMs = spread)
    if (spread >= AUTO_ROOM_RESYNC_THRESHOLD_MS) autoRoomResync(spread)
}

/** Automatic group correction is event-driven by 2-second member reports.
 * A >1s spread rewinds the entire room by one second and gives every player
 * one shared monotonic start deadline. Cooldown prevents correction loops. */
internal fun RexMusicSyncEngine.autoRoomResync(driftMs: Long) {
    val now = SystemClock.elapsedRealtime()
    if (!_state.value.hosting || hostPreparing || _state.value.resyncing || !hostPlaying) return
    if (driftMs < AUTO_ROOM_RESYNC_THRESHOLD_MS) return
    if (lastAutoRoomResyncAtMs > 0L && now - lastAutoRoomResyncAtMs < AUTO_ROOM_RESYNC_COOLDOWN_MS) return
    val track = hostTrack ?: return
    val local = RexPlayerController.state.value
    if (local.nowPlaying?.id != track.id || local.durationMs <= 0L) return

    val target = (local.positionMs.coerceAtLeast(0L) - 1000L).coerceAtLeast(0L)
        .coerceAtMost(local.durationMs)
    val startAt = now + ROOM_RESYNC_LEAD_MS
    val roomElapsed = (now - roomStartedElapsedMs).coerceAtLeast(0L)
    lastAutoRoomResyncAtMs = now
    roomPlaybackRevision += 1L
    hostPreparing = true
    hostPlaying = true
    playAnchorPositionMs = target
    playAnchorWallMs = startAt
    playbackJob?.cancel()
    _state.value = _state.value.copy(
        playing = false, positionMs = target, roomElapsedMs = roomElapsed,
        resyncing = true, lastResyncAtMs = now, syncDriftMs = driftMs,
        status = "Selisih ${driftMs} ms · room mengulang 1 detik bersama…", error = null
    )

    val command = JSONObject().put("type", "room_resync")
        .put("trackId", track.id).put("title", track.title)
        .put("position", target).put("startAt", startAt)
        .put("roomElapsedMs", roomElapsed).put("revision", roomPlaybackRevision)
        .put("reason", "automatic_drift")
    peers.filter { it.clockSynced && it.assetsReady }.forEach { send(it.writer, command) }

    playbackJob = scope.launch {
        withContext(Dispatchers.Main) {
            RexPlayerController.pause()
            RexPlayerController.prepareSyncSeek(target)
        }
        delay(max(0L, startAt - SystemClock.elapsedRealtime()))
        val startPosition = target
        withContext(Dispatchers.Main) {
            if (RexPlayerController.state.value.nowPlaying?.id == track.id) {
                RexPlayerController.startSyncPlayback(startPosition)
            }
        }
        hostPreparing = false
        hostPlaying = true
        _state.value = _state.value.copy(
            playing = true, positionMs = startPosition,
            roomElapsedMs = (SystemClock.elapsedRealtime() - roomStartedElapsedMs).coerceAtLeast(roomElapsed),
            resyncing = false, syncDriftMs = 0L,
            status = "Room kembali sinkron · rewind 1 detik selesai"
        )
        broadcastTick()
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

    val projectedTarget = (positionMs + (SystemClock.elapsedRealtime() + clockOffsetMs - hostStartAtMs).coerceAtLeast(0L))
        .coerceAtMost(local.durationMs)
    withContext(Dispatchers.Main) {
        RexPlayerController.pause()
        RexPlayerController.prepareSyncSeek(projectedTarget)
    }

    val localStartAt = hostStartAtMs - clockOffsetMs
    delay(max(0L, localStartAt - SystemClock.elapsedRealtime()))
    withContext(Dispatchers.Main) {
        if (RexPlayerController.state.value.nowPlaying?.id == trackId) {
            // Pre-seek during the lead-in; this only releases playback at the shared deadline.
            RexPlayerController.startSyncPlayback(projectedTarget)
        }
    }
    _state.value = _state.value.copy(
        positionMs = projectedTarget,
        playing = true,
        roomElapsedMs = roomElapsedMs + max(0L, SystemClock.elapsedRealtime() + clockOffsetMs - hostStartAtMs),
        resyncing = false,
        status = "Room kembali berjalan bersama"
    )
}
