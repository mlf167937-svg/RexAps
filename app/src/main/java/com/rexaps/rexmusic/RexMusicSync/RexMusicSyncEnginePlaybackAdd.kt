package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun RexMusicSyncEngine.broadcastMembers() {
    val a = org.json.JSONArray().put(JSONObject().put("id", "host").put("name", "Host"))
    peers.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("volume", it.volume).put("deviceVolume", it.deviceVolume)) }
    _state.value = _state.value.copy(members = listOf(SyncMember("host", "Host")) + peers.map { SyncMember(it.id, it.name, ip = it.ip, volume = it.volume, deviceVolume = it.deviceVolume) }, volumes = peers.associate { it.id to it.volume })
    val msg = JSONObject().put("type", "members").put("members", a)
    peers.forEach { send(it.writer, msg) }
}

internal fun RexMusicSyncEngine.addToQueueImpl(track: RexTrack) {
    val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
    if (local == null) { _state.value = _state.value.copy(error = "Lagu ini belum tersedia offline di HP kamu.", cacheStatus = "Pilih lagu yang sudah diunduh."); return }
    if (_state.value.hosting) {
        if (sharedQueue.none { it.id == local.track.id }) sharedQueue += local.track
        publishQueue()
        _state.value = _state.value.copy(cacheStatus = "Menyalin ${track.title} ke cache anggota…")
        scope.launch { distributeLocalTrack(local.track) }
    } else if (_state.value.connected) {
        runCatching { clientWriter?.let { send(it, JSONObject().put("type", "queue_add").put("track", trackJson(local.track))) } }
        scope.launch { sendBundle(_state.value.ip, local.track) }
        _state.value = _state.value.copy(cacheStatus = "Mengirim ${track.title} ke room…", error = null)
    }
}

internal fun RexMusicSyncEngine.playQueuedImpl(track: RexTrack) {
    if (_state.value.hosting) scope.launch {
        delay(500L)
        val offline = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
        if (offline == null) { _state.value = _state.value.copy(error = "Transfer lagu belum selesai atau izin penyimpanan belum aktif."); return@launch }
        sharedQueue.removeAll { it.id == track.id }; publishQueue(); playTrack(offline.track)
    } else if (_state.value.connected) runCatching { clientWriter?.let { send(it, JSONObject().put("type", "play_request").put("trackId", track.id)) } }
}

internal fun RexMusicSyncEngine.setHostVolumeImpl(value: Float) {
    val v = value.coerceIn(0f, 1f)
    _state.value = _state.value.copy(hostVolume = v)
    appContextRef?.let { context ->
        RexPlayerController.init(context)
        RexPlayerController.setSyncVolume(v)
    }
}

internal fun RexMusicSyncEngine.setMemberVolumeImpl(id: String, value: Float) {
    val v = value.coerceIn(0f, 1f)
    if (!_state.value.hosting) return
    peers.firstOrNull { it.id == id }?.let { p -> p.volume = v; runCatching { send(p.writer, JSONObject().put("type", "volume").put("value", v)) } }
    _state.value = _state.value.copy(volumes = _state.value.volumes + (id to v), members = _state.value.members.map { if (it.id == id) it.copy(volume = v) else it })
}

internal fun RexMusicSyncEngine.trackJson(t: RexTrack) = JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist).put("album", t.album).put("cover", t.cover).put("spotifyUrl", t.spotifyUrl).put("durationText", t.durationText)

internal fun RexMusicSyncEngine.trackFromJson(o: JSONObject?): RexTrack? = o?.let { RexTrack(it.optString("id"), it.optString("title"), it.optString("artist"), it.optString("album", "Single"), it.optString("cover"), spotifyUrl = it.optString("spotifyUrl"), durationText = it.optString("durationText")) }?.takeIf { it.id.isNotBlank() && it.title.isNotBlank() }

internal fun RexMusicSyncEngine.sendQueue(p: Peer) { val arr = org.json.JSONArray(); sharedQueue.forEach { arr.put(trackJson(it)) }; send(p.writer, JSONObject().put("type", "queue").put("items", arr)) }

internal fun RexMusicSyncEngine.publishQueue() { val arr = org.json.JSONArray(); sharedQueue.forEach { arr.put(trackJson(it)) }; val msg = JSONObject().put("type", "queue").put("items", arr); peers.forEach { send(it.writer, msg) }; _state.value = _state.value.copy(queue = sharedQueue.toList()) }

internal fun RexMusicSyncEngine.stopLocalPlayback() { appContextRef?.let { c -> scope.launch(Dispatchers.Main) { runCatching { RexPlayerController.init(c); RexPlayerController.pause() } } } }
