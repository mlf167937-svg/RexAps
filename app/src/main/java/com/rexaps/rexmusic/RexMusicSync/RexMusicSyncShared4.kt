package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.BufferedWriter
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun RexMusicSyncEngine.__splitImpl_broadcastMembers() {
        val host = _state.value.members.firstOrNull { it.id == "host" }
            ?: SyncMember("host", _state.value.name.ifBlank { "Host" })
        val members = listOf(host) + peers.map { peer ->
            SyncMember(
                id = peer.id, name = peer.name, ip = peer.ip,
                volume = peer.volume, deviceVolume = peer.deviceVolume,
                username = peer.username, bio = peer.bio,
                profilePictureBase64 = peer.profilePictureBase64,
                bannerBase64 = peer.bannerBase64
            )
        }
        val array = org.json.JSONArray()
        members.forEach { member ->
            array.put(JSONObject()
                .put("id", member.id).put("name", member.name).put("username", member.username)
                .put("bio", member.bio).put("profilePicture", member.profilePictureBase64)
                .put("banner", member.bannerBase64).put("volume", member.volume)
                .put("deviceVolume", member.deviceVolume))
        }
        _state.value = _state.value.copy(
            members = members,
            volumes = peers.associate { it.id to it.volume }
        )
        val message = JSONObject().put("type", "members").put("members", array)
        peers.forEach { send(it.writer, message) }
    }

internal fun RexMusicSyncEngine.__splitImpl_closeAll() {
        hostPreparing = false; hostPlaying = false; roomStartedElapsedMs = 0L; clockOffsetMs = 0L; lastClockRttMs = -1L
        heartbeat?.cancel(); playbackJob?.cancel(); autoRefreshJob?.cancel(); autoRefreshJob = null; roomJob?.cancel(); roomJob = null
        sharedQueue.clear()
        peers.forEach { runCatching { it.socket.close() } }; peers.clear()
        runCatching { client?.close() }; client = null; clientWriter = null
        runCatching { server?.close() }; server = null
        if (_state.value.hosting) stopLocalPlayback()
        _state.value = _state.value.copy(queue = emptyList(), autoRefreshEnabled = false, resyncing = false)
    }

internal fun RexMusicSyncEngine.__splitImpl_stopAll(reason: String = "Host menghentikan pemutaran") {
        hostPreparing = false; hostPlaying = false; playbackJob?.cancel()
        scope.launch { withContextMain { RexPlayerController.pause() } }
        _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = reason)
        peers.forEach { runCatching { send(it.writer, JSONObject().put("type", "stop").put("reason", reason)) } }
    }

internal fun RexMusicSyncEngine.__splitImpl_send(w: BufferedWriter, m: JSONObject) {
        // Socket writes may fail when a phone disconnects mid-gesture. Never let an IOException
        // escape a Compose callback (especially the volume slider) and crash the whole screen.
        runCatching { synchronized(w) { w.write(m.toString()); w.newLine(); w.flush() } }
    }

internal suspend fun RexMusicSyncEngine.__splitImpl_monitorPeers() {
        while (_state.value.hosting) {
            val now = SystemClock.elapsedRealtime()
            peers.filter { now - it.lastSeen > CLIENT_TIMEOUT_MS }.forEach { p -> runCatching { p.socket.close() } }
            delay(HEARTBEAT_MS)
        }
    }

internal fun __splitImpl_formatRoomTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", total / 3600L, (total / 60L) % 60L, total % 60L)
}

internal fun RexMusicSyncEngine.__splitImpl_disconnect() { closeAll(); _state.value = _state.value.copy(hosting = false, connected = false, playing = false, members = emptyList(), status = "Terputus", error = null) }
