package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlin.math.max

@Composable
internal fun SyncPlaybackCard(
    state: SyncUiState,
    onHostVolumeChange: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SyncSectionHeading("Playback synchronization", "Shared playback status for this room", scheme.onPrimaryContainer, scheme.onPrimaryContainer.copy(alpha = 0.8f))
            Text(
                state.trackTitle.ifBlank { "Belum ada lagu" },
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onPrimaryContainer,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (state.playing) "PLAYING" else "PAUSED",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.roomElapsedMs > 0L) formatRoomTime(state.roomElapsedMs) else "Room timer ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (state.hosting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Host volume", style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                    Text("${(state.hostVolume.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimaryContainer)
                }
                Slider(
                    value = state.hostVolume.coerceIn(0f, 1f),
                    onValueChange = onHostVolumeChange,
                    valueRange = 0f..1f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onTogglePlayback,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.playing) "Pause" else "Resume")
                    }
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                }
            } else {
                Text(
                    "Playback is controlled by the host device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

internal suspend fun RexMusicSyncEngine.__split_syncLocalTrack_7(id: String, pos: Long, playing: Boolean, startAt: Long) {
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

internal suspend fun RexMusicSyncEngine.__split_reconcileLocalTrack_8(id: String, targetPosition: Long, shouldPlay: Boolean) {
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
