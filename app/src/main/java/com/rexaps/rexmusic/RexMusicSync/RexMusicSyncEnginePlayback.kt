package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.max

internal suspend fun RexMusicSyncEngine.syncLocalTrack(id: String, pos: Long, playing: Boolean, startAt: Long) {
    RexPlayerController.init(appContextRef ?: return)
    val track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
    if (track == null) {
        _state.value = _state.value.copy(status = "Lagu belum ada di HP ini", error = "Lagu '$id' belum diterima atau belum terindeks di library offline; tunggu transfer cache selesai.")
        return
    }
    withContext(Dispatchers.Main) { RexPlayerController.prepareFromOfflineForSync(track) }
    val deadline = SystemClock.elapsedRealtime() + 12000
    while (SystemClock.elapsedRealtime() < deadline) {
        val s = RexPlayerController.state.value
        if (s.nowPlaying?.id == id && s.durationMs > 0L && s.phase == LoadPhase.Idle) break
        delay(100)
    }
    val actual = RexPlayerController.state.value
    if (actual.nowPlaying?.id != id) return
    val targetHostElapsed = if (startAt > 0L) startAt else SystemClock.elapsedRealtime() + clockOffsetMs
    val targetLocalElapsed = targetHostElapsed - clockOffsetMs
    delay(max(0L, targetLocalElapsed - SystemClock.elapsedRealtime()))
    val targetPos = max(0L, pos + max(0L, SystemClock.elapsedRealtime() + clockOffsetMs - targetHostElapsed))
    withContext(Dispatchers.Main) {
        if (playing) RexPlayerController.startSyncPlayback(targetPos)
        else { RexPlayerController.seekToPositionImmediate(pos); RexPlayerController.pause() }
    }
}

internal suspend fun RexMusicSyncEngine.reconcileLocalTrack(id: String, targetPosition: Long, shouldPlay: Boolean) {
    val context = appContextRef ?: return
    RexPlayerController.init(context)
    val current = RexPlayerController.state.value
    if (current.nowPlaying?.id != id) {
        val track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
        if (track == null) {
            _state.value = _state.value.copy(error = "Lagu belum tersedia offline di HP ini.")
            return
        }
        withContext(Dispatchers.Main) { RexPlayerController.playFromOffline(track) }
        val deadline = SystemClock.elapsedRealtime() + 10000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val s = RexPlayerController.state.value
            if (s.nowPlaying?.id == id && s.durationMs > 0L) break
            delay(100L)
        }
    }
    val now = RexPlayerController.state.value
    if (now.nowPlaying?.id != id) return
    if (kotlin.math.abs(now.positionMs - targetPosition) > DRIFT_CORRECTION_MS) withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(targetPosition) }
    if (shouldPlay && !now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.play() }
    if (!shouldPlay && now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.pause() }
}

internal fun RexMusicSyncEngine.playTrackImpl(track: RexTrack) {
    if (!_state.value.hosting) return
    val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
    if (local == null) { _state.value = _state.value.copy(error = "Pilih lagu yang sudah tersedia offline di HP host."); return }
    hostTrack = local.track
    playAnchorPositionMs = 0L
    hostPreparing = true
    hostPlaying = false
    _state.value = _state.value.copy(trackId = track.id, trackTitle = track.title, positionMs = 0L, playing = false, status = "Menyiapkan semua perangkat…", error = null)
    playbackJob?.cancel()
    playbackJob = scope.launch {
        // Prepare the host player before announcing the future start; do not play early.
        withContext(Dispatchers.Main) { RexPlayerController.prepareFromOfflineForSync(local.track) }
        val deadline = SystemClock.elapsedRealtime() + 15000L
        while (isActive && SystemClock.elapsedRealtime() < deadline) {
            val s = RexPlayerController.state.value
            if (s.nowPlaying?.id == local.track.id && s.durationMs > 0L && s.phase == LoadPhase.Idle) break
            delay(40L)
        }
        val ready = RexPlayerController.state.value
        if (ready.nowPlaying?.id != local.track.id || ready.durationMs <= 0L) {
            hostPreparing = false
            hostPlaying = false
            _state.value = _state.value.copy(playing = false, status = "Lagu gagal disiapkan", error = "Player tidak siap sebelum batas waktu.")
            broadcastState()
            return@launch
        }
        hostPreparing = false
        hostPlaying = true
        playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
        _state.value = _state.value.copy(playing = true, status = "Mulai serentak · room clock")
        broadcastState()
        delay(max(0L, playAnchorWallMs - SystemClock.elapsedRealtime()))
        withContext(Dispatchers.Main) { RexPlayerController.startSyncPlayback(0L) }
    }
}

private fun RexMusicSyncEngine.resumeRoomAtCurrentPosition() {
    val track = hostTrack ?: return
    playAnchorPositionMs = RexPlayerController.state.value.positionMs.coerceAtLeast(playAnchorPositionMs)
    playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
    hostPlaying = true
    _state.value = _state.value.copy(playing = true, positionMs = playAnchorPositionMs, status = "Anggota kembali · sinkronisasi ulang", error = null)
    broadcastState()
    playbackJob?.cancel()
    playbackJob = scope.launch {
        delay(max(0L, playAnchorWallMs - SystemClock.elapsedRealtime()))
        withContext(Dispatchers.Main) {
            if (RexPlayerController.state.value.nowPlaying?.id != track.id) RexPlayerController.playFromOffline(track)
            delay(150L)
            RexPlayerController.seekToPositionImmediate(playAnchorPositionMs)
            RexPlayerController.startSyncPlayback(playAnchorPositionMs)
        }
    }
}

internal fun RexMusicSyncEngine.togglePlaybackImpl() {
    if (!_state.value.hosting) return
    hostPlaying = !hostPlaying
    if (hostPlaying) {
        playAnchorPositionMs = RexPlayerController.state.value.positionMs
        playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
        broadcastState()
        playbackJob?.cancel(); playbackJob = scope.launch { delay(max(0L, playAnchorWallMs-SystemClock.elapsedRealtime())); withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(playAnchorPositionMs); RexPlayerController.startSyncPlayback(playAnchorPositionMs) } }
    } else {
        scope.launch { withContextMain { RexPlayerController.pause() } }
        _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = "Dijeda host")
        broadcastState()
    }
}

internal fun RexMusicSyncEngine.sendState(p: Peer) {
    val track = hostTrack
    val now = SystemClock.elapsedRealtime()
    val startAt = if (hostPlaying) max(now + START_LEAD_MS, playAnchorWallMs) else 0L
    val currentPosition = if (hostPlaying) playAnchorPositionMs + max(0L, now - playAnchorWallMs) else RexPlayerController.state.value.positionMs
    // If playback is already underway, project to the future start; if the anchor is still ahead, keep its start position.
    val pos = if (hostPlaying) {
        if (now < playAnchorWallMs) playAnchorPositionMs else currentPosition + max(0L, startAt - now)
    } else currentPosition
    send(p.writer, JSONObject().put("type", "state").put("track", track?.let { JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist) })
        .put("position", pos).put("playing", hostPlaying).put("hostElapsed", now).put("roomElapsedMs", (now - roomStartedElapsedMs).coerceAtLeast(0L)).put("startAt", startAt))
}

private fun RexMusicSyncEngine.broadcastState() { peers.filter { it.clockSynced }.forEach { sendState(it) } }

internal fun RexMusicSyncEngine.broadcastTick() {
    val now = SystemClock.elapsedRealtime()
    val roomElapsed = (now - roomStartedElapsedMs).coerceAtLeast(0L)
    _state.value = _state.value.copy(roomElapsedMs = roomElapsed)
    val roomMessage = JSONObject().put("type", "room_time").put("roomElapsedMs", roomElapsed)
    peers.filter { it.clockSynced }.forEach { runCatching { send(it.writer, roomMessage) } }
    if (hostPreparing) return
    val track = hostTrack ?: return
    val current = RexPlayerController.state.value
    // RexPlayerController has its own autoplay. If it advances away from the shared track,
    // hand control back to the room queue so every member gets the same next track.
    if (hostPlaying && SystemClock.elapsedRealtime() > playAnchorWallMs + 2500L &&
        current.isPlaying && current.nowPlaying?.id != null && current.nowPlaying?.id != track.id) {
        val next = sharedQueue.firstOrNull()
        if (next != null) { playQueued(next); return }
        stopAll("Lagu selesai · antrean bersama kosong")
        return
    }
    val position = if (hostPlaying) {
        if (current.nowPlaying?.id == track.id && current.positionMs > 0L) current.positionMs
        else playAnchorPositionMs + max(0L, SystemClock.elapsedRealtime() - playAnchorWallMs)
    } else current.positionMs
    val hostReference = if (hostPlaying) max(now, playAnchorWallMs) else now
    val msg = JSONObject().put("type", "sync").put("trackId", track.id).put("title", track.title)
        .put("position", position).put("playing", hostPlaying).put("hostElapsed", hostReference).put("roomElapsedMs", (now - roomStartedElapsedMs).coerceAtLeast(0L))
    peers.filter { it.clockSynced }.forEach { send(it.writer, msg) }
    _state.value = _state.value.copy(positionMs = position, playing = hostPlaying, roomElapsedMs = (SystemClock.elapsedRealtime() - roomStartedElapsedMs).coerceAtLeast(0L))
}
