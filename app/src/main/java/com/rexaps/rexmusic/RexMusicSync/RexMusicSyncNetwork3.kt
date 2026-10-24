package com.rexaps.rexmusic

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.math.max

internal fun RexMusicSyncEngine.__split_handleClientMessage_6(m: JSONObject) {
        when (m.optString("type")) {
            "room_time" -> _state.value = _state.value.copy(roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs))
            "pong" -> {
                val c0 = m.optLong("clientElapsed", 0L)
                val h1 = m.optLong("hostReceiveElapsed", 0L)
                val h2 = m.optLong("hostSendElapsed", 0L)
                val c3 = SystemClock.elapsedRealtime()
                if (c0 > 0L && h1 > 0L && h2 >= h1) {
                    val rtt = (c3 - c0 - (h2 - h1)).coerceAtLeast(0L)
                    val offset = (((h1 - c0) + (h2 - c3)) / 2L)
                    // Prefer low-RTT samples; they are less affected by queued network traffic.
                    if (lastClockRttMs < 0L || rtt <= lastClockRttMs + 40L || rtt < lastClockRttMs) {
                        clockOffsetMs = offset
                        lastClockRttMs = rtt
                    }
                }
            }
            "members" -> {
                val a = m.optJSONArray("members")
                val list = mutableListOf<SyncMember>()
                if (a != null) for (i in 0 until a.length()) { val o = a.optJSONObject(i) ?: continue; list += SyncMember(o.optString("id"), o.optString("name"), volume = o.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f), deviceVolume = o.optDouble("deviceVolume", -1.0).toFloat().coerceIn(-1f, 1f)) }
                _state.value = _state.value.copy(members = list)
            }
            "sync" -> {
                val id = m.optString("trackId")
                val pos = m.optLong("position", 0L)
                val hostAt = m.optLong("hostElapsed", 0L)
                val shouldPlay = m.optBoolean("playing", false)
                val local = RexPlayerController.state.value
                val projected = if (shouldPlay && hostAt > 0L) pos + (SystemClock.elapsedRealtime() + clockOffsetMs - hostAt).coerceAtLeast(0L) else pos
                val drift = kotlin.math.abs(local.positionMs - projected)
                val needsTrack = id.isNotBlank() && local.nowPlaying?.id != id
                val needsPlayState = id.isNotBlank() && local.nowPlaying?.id == id && local.isPlaying != shouldPlay
                if (needsTrack && playbackJob?.isActive != true) playbackJob = scope.launch { syncLocalTrack(id, pos, shouldPlay, m.optLong("startAt", 0L)) }
                else if (needsPlayState && playbackJob?.isActive != true) playbackJob = scope.launch { reconcileLocalTrack(id, projected, shouldPlay) }
                else if (shouldPlay && drift > DRIFT_CORRECTION_MS && playbackJob?.isActive != true) {
                    playbackJob = scope.launch { withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(projected) } }
                }
                _state.value = _state.value.copy(trackId = id, trackTitle = m.optString("title", "Belum ada lagu"), positionMs = projected, playing = shouldPlay, roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs))
            }
            "state" -> {
                val track = m.optJSONObject("track")
                val id = track?.optString("id").orEmpty()
                val title = track?.optString("title").orEmpty().ifBlank { "Belum ada lagu" }
                val pos = m.optLong("position", 0L)
                val playing = m.optBoolean("playing", false)
                val startAt = m.optLong("startAt", 0L)
                _state.value = _state.value.copy(trackId = id, trackTitle = title, positionMs = pos, playing = playing, roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs), status = "Sinkronisasi · $title")

                // A state snapshot may arrive after the initial sync packet. Never restart
                // preparation for a track that is already loaded, and don't cancel an
                // in-flight preparation for the same track: doing so can keep members silent.
                if (id.isNotBlank()) {
                    val local = RexPlayerController.state.value
                    val sameTrackLoaded = local.nowPlaying?.id == id && local.durationMs > 0L
                    val preparationForSameTrack = playbackJob?.isActive == true && local.nowPlaying?.id == id
                    if (!sameTrackLoaded && !preparationForSameTrack) {
                        playbackJob?.cancel()
                        playbackJob = scope.launch { syncLocalTrack(id, pos, playing, startAt) }
                    } else if (sameTrackLoaded && local.isPlaying != playing && playbackJob?.isActive != true) {
                        playbackJob = scope.launch { reconcileLocalTrack(id, pos, playing) }
                    }
                }
            }
            "stop" -> { playbackJob?.cancel(); stopLocalPlayback(); _state.value = _state.value.copy(playing = false, status = m.optString("reason", "Pemutaran dihentikan")) }
            "queue" -> {
                val arr = m.optJSONArray("items"); val list = mutableListOf<RexTrack>()
                if (arr != null) for (i in 0 until arr.length()) trackFromJson(arr.optJSONObject(i))?.let { list += it }
                _state.value = _state.value.copy(queue = list)
            }
            "queue_item" -> trackFromJson(m.optJSONObject("track"))?.let { t -> if (_state.value.queue.none { it.id == t.id }) _state.value = _state.value.copy(queue = _state.value.queue + t) }
            "volume" -> { val v = m.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f); scope.launch(Dispatchers.Main) { RexPlayerController.setSyncVolume(v) } }
            "reject" -> _state.value = _state.value.copy(error = m.optString("reason"), status = "Akses ditolak")
        }
    }

fun RexMusicSyncEngine.__split_bindContext_9(context: Context) {
        appContextRef = context.applicationContext
        RexMusicService.start(context.applicationContext); RexOfflineManager.init(context.applicationContext); RexPlayerController.init(context.applicationContext)
        if (transferJob?.isActive != true) transferJob = scope.launch {
            try {
                val ss = ServerSocket(TRANSFER_PORT, 16, InetAddress.getByName("0.0.0.0")); transferServer = ss
                while (isActive && !ss.isClosed) {
                    val socket = ss.accept()
                    launch(Dispatchers.IO) { receiveBundle(socket) }
                }
            } catch (e: Exception) { if (transferServer != null) _state.value = _state.value.copy(cacheStatus = "Transfer: ${e.message}") }
        }
    }

fun RexMusicSyncEngine.__split_stopAll_13(reason: String = "Host menghentikan pemutaran") {
        hostPreparing = false; hostPlaying = false; playbackJob?.cancel()
        scope.launch { withContextMain { RexPlayerController.pause() } }
        _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = reason)
        peers.forEach { runCatching { send(it.writer, JSONObject().put("type", "stop").put("reason", reason)) } }
    }

internal fun RexMusicSyncEngine.__split_sendState_14(p: Peer) {
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
