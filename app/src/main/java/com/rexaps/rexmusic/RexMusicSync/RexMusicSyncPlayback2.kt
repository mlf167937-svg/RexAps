package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.coroutines.*
import org.json.JSONObject

internal suspend fun RexMusicSyncEngine.__splitImpl_reconcileLocalTrack(id: String, targetPosition: Long, shouldPlay: Boolean) {
        val context = appContextRef ?: return
        RexPlayerController.setSynchronizedPlaybackMode(true)
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
        if (kotlin.math.abs(now.positionMs - targetPosition) >= AUTO_ROOM_RESYNC_THRESHOLD_MS) withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(targetPosition) }
        if (shouldPlay && !now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.play() }
        if (!shouldPlay && now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.pause() }
    }

@Composable
internal fun __splitImpl_SyncOfflineTrackCard(
    track: RexTrack,
    active: Boolean,
    onQueue: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                androidx.compose.material3.Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            TextButton(enabled = active, onClick = onQueue) { Text("+ QUEUE") }
        }
    }
}

internal fun RexMusicSyncEngine.__splitImpl_togglePlayback() {
    if (!_state.value.hosting || hostPreparing || _state.value.resyncing) return
    val track = hostTrack ?: return
    val player = RexPlayerController.state.value
    if (!hostPlaying) {
        val position = player.positionMs.coerceAtLeast(0L)
        val startAt = SystemClock.elapsedRealtime() + START_LEAD_MS
        hostPlaying = true
        playAnchorPositionMs = position
        playAnchorWallMs = startAt
        lastAutoRoomResyncAtMs = 0L
        roomPlaybackRevision += 1L
        _state.value = _state.value.copy(playing = true, positionMs = position,
            status = "Room menyiapkan resume bersama…", error = null)
        val command = JSONObject().put("type", "room_play").put("trackId", track.id)
            .put("title", track.title).put("position", position).put("startAt", startAt)
            .put("roomElapsedMs", (SystemClock.elapsedRealtime() - roomStartedElapsedMs).coerceAtLeast(0L))
            .put("revision", roomPlaybackRevision)
        peers.filter { it.clockSynced && it.assetsReady }.forEach { send(it.writer, command) }
        playbackJob?.cancel()
        playbackJob = scope.launch {
            withContext(Dispatchers.Main) {
                RexPlayerController.pause()
                RexPlayerController.prepareSyncSeek(position)
            }
            delay(max(0L, startAt - SystemClock.elapsedRealtime()))
            withContext(Dispatchers.Main) { RexPlayerController.startSyncPlayback(position) }
            _state.value = _state.value.copy(playing = true, positionMs = position, status = "Diputar bersama")
            broadcastTick()
        }
    } else {
        val position = player.positionMs.coerceAtLeast(0L)
        hostPlaying = false
        playAnchorPositionMs = position
        playAnchorWallMs = SystemClock.elapsedRealtime()
        roomPlaybackRevision += 1L
        playbackJob?.cancel()
        scope.launch { withContextMain { RexPlayerController.pause() } }
        val command = JSONObject().put("type", "room_pause").put("trackId", track.id)
            .put("position", position).put("playing", false)
            .put("roomElapsedMs", (SystemClock.elapsedRealtime() - roomStartedElapsedMs).coerceAtLeast(0L))
            .put("revision", roomPlaybackRevision)
        peers.filter { it.clockSynced }.forEach { send(it.writer, command) }
        _state.value = _state.value.copy(playing = false, positionMs = position, status = "Dijeda host")
    }
}

@Composable
internal fun __splitImpl_SyncPlaybackEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Playback synchronization", style = MaterialTheme.typography.titleMedium)
            Text("The shared playback panel appears when you host or join a room.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal suspend fun RexMusicSyncEngine.__splitImpl_distributeLocalTrack(track: RexTrack) {
        val entry = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id } ?: return
        val peersSnapshot = peers.toList()
        for (p in peersSnapshot) {
            val ok = sendBundle(p.ip, track)
            if (!ok) _state.value = _state.value.copy(cacheStatus = "Gagal mengirim ke ${p.name}; cek Wi-Fi/firewall")
        }
        _state.value = _state.value.copy(cacheStatus = "Cache lagu siap untuk anggota yang terjangkau")
    }

internal fun RexMusicSyncEngine.__splitImpl_setMemberVolume(id: String, value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (!_state.value.hosting) return
        peers.firstOrNull { it.id == id }?.let { peer ->
            peer.volume = v
            peer.volumeControlled = true
            runCatching { send(peer.writer, JSONObject().put("type", "volume").put("value", v)) }
        }
        _state.value = _state.value.copy(
            volumes = _state.value.volumes + (id to v),
            members = _state.value.members.map { if (it.id == id) it.copy(volume = v) else it }
        )
    }

internal fun RexMusicSyncEngine.__splitImpl_trackFromJson(o: JSONObject?): RexTrack? = o?.let { RexTrack(it.optString("id"), it.optString("title"), it.optString("artist"), it.optString("album", "Single"), it.optString("cover"), spotifyUrl = it.optString("spotifyUrl"), durationText = it.optString("durationText")) }?.takeIf { it.id.isNotBlank() && it.title.isNotBlank() }

internal fun RexMusicSyncEngine.__splitImpl_setHostVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        appContextRef?.let { context ->
            val actual = setAndroidMusicVolume(context, v)
            RexPlayerController.init(context)
            // MediaPlayer gain stays at unity; the slider now controls Android STREAM_MUSIC.
            RexPlayerController.setSyncVolume(1f)
            _state.value = if (actual >= 0f) {
                _state.value.copy(hostVolume = actual, error = null)
            } else {
                _state.value.copy(error = "Volume Android tidak bisa diubah. Periksa MODIFY_AUDIO_SETTINGS pada AndroidManifest.")
            }
        } ?: run {
            _state.value = _state.value.copy(error = "AudioManager belum siap. Coba masuk ulang ke room.")
        }
    }

internal fun RexMusicSyncEngine.__splitImpl_trackJson(t: RexTrack) = JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist).put("album", t.album).put("cover", t.cover).put("spotifyUrl", t.spotifyUrl).put("durationText", t.durationText)

internal fun RexMusicSyncEngine.__splitImpl_stopLocalPlayback() { appContextRef?.let { c -> scope.launch(Dispatchers.Main) { runCatching { RexPlayerController.init(c); RexPlayerController.pause() } } } }
