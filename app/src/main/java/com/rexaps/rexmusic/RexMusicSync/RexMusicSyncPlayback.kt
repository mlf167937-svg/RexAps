package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.coroutines.*

@Composable
internal fun __splitImpl_SyncPlaybackCard(
    state: SyncUiState,
    onHostVolumeChange: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit,
    onRefresh: () -> Unit,
    onRepeatChange: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var showRepeatSheet by remember { mutableStateOf(false) }
    val playerState by RexPlayerController.state.collectAsState()
    val activeTrack = playerState.nowPlaying
    val duration = playerState.durationMs.coerceAtLeast(0L)
    val position = if (activeTrack?.id == state.trackId) playerState.positionMs else state.positionMs
    val progress = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("ROOM PLAYER", style = MaterialTheme.typography.labelSmall, color = scheme.primary, fontWeight = FontWeight.Bold)
                    Text("Sinkronisasi audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(50), color = if (state.resyncing) scheme.tertiaryContainer else scheme.primaryContainer) {
                    Text(
                        when { state.resyncing -> "SYNCING"; state.playing -> "LIVE"; else -> "PAUSED" },
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.resyncing) scheme.onTertiaryContainer else scheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    Modifier.size(82.dp).clip(RoundedCornerShape(18.dp)).background(scheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!activeTrack?.cover.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = activeTrack?.cover,
                            contentDescription = "Cover ${state.trackTitle}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = scheme.primary
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        state.trackTitle.ifBlank { "Belum ada lagu" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        activeTrack?.artist?.takeIf { it.isNotBlank() } ?: "Shared room track",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        if (state.resyncing) "Menyamakan posisi semua perangkat" else if (state.playing) "Diputar bersama" else "Menunggu host",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.resyncing) scheme.tertiary else scheme.primary
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(scheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(50)).background(scheme.primary))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MUSIC", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(formatSyncClock(position), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ROOM TIME", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(formatSyncClock(state.roomElapsedMs), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            if (state.hosting) {
                Divider(color = scheme.outlineVariant.copy(alpha = 0.55f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Volume media perangkat host", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text("${(state.hostVolume.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                }
                Slider(value = state.hostVolume.coerceIn(0f, 1f), onValueChange = onHostVolumeChange, valueRange = 0f..1f)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = onTogglePlayback,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        enabled = !state.resyncing,
                        shape = RoundedCornerShape(14.dp)
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
                        enabled = !state.resyncing,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                }
                FilledTonalButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = state.trackId.isNotBlank() && state.playing && !state.resyncing,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.resyncing) {
                        CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                    } else {
                        androidx.compose.material3.Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.resyncing) "Menyelaraskan…" else "Refresh Sync · manual")
                }
            } else {
                Text(
                    if (state.resyncing) "Host sedang menyelaraskan ulang semua perangkat…" else "Kontrol pemutaran utama tersedia untuk host. Kamu tetap dapat meminta pengaturan repeat room.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { showRepeatSheet = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.trackId.isNotBlank() && !state.resyncing && !state.status.contains("Menyiapkan", ignoreCase = true),
                shape = RoundedCornerShape(14.dp)
            ) {
                androidx.compose.material3.Icon(Icons.Default.Repeat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.repeatTotal == 0) "Ulangi lagu · Mati (maks 20×)" else "Ulangi lagu · ${state.repeatTotal}× · sisa ${state.repeatLeft}")
            }
            if (state.status.isNotBlank()) {
                Text(state.status, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (state.syncDriftMs > 0L && state.syncDriftMs < AUTO_ROOM_RESYNC_THRESHOLD_MS) {
                Text("Deviasi room: ${state.syncDriftMs} ms · diperiksa tiap 2 detik", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
    if (showRepeatSheet) {
        RepeatSheet(
            total = state.repeatTotal,
            left = state.repeatLeft,
            title = state.trackTitle.ifBlank { "Room playback" },
            onSet = onRepeatChange,
            onDismiss = { showRepeatSheet = false },
            maxValue = MAX_SYNC_REPEAT
        )
    }
}

private fun formatSyncClock(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes >= 60L) {
        String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", minutes / 60L, minutes % 60L, seconds)
    } else {
        String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

internal fun RexMusicSyncEngine.__splitImpl_playTrack(track: RexTrack) {
        if (!_state.value.hosting) return
        RexPlayerController.setSynchronizedPlaybackMode(true)
        val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
        if (local == null) { _state.value = _state.value.copy(error = "Pilih lagu yang sudah tersedia offline di HP host."); return }
        hostTrack = local.track
        playAnchorPositionMs = 0L
        hostPreparing = true
        hostPlaying = false
        _state.value = _state.value.copy(
            trackId = track.id, trackTitle = track.title, positionMs = 0L, playing = false,
            repeatTotal = 0, repeatLeft = 0,
            status = "Menyiapkan semua perangkat…", error = null
        )
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
            playAnchorPositionMs = 0L
            playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
            lastAutoRoomResyncAtMs = 0L
            roomPlaybackRevision += 1L
            _state.value = _state.value.copy(playing = true, positionMs = 0L, status = "Room menyiapkan waktu mulai bersama…")
            broadcastState()
            delay(max(0L, playAnchorWallMs - SystemClock.elapsedRealtime()))
            withContext(Dispatchers.Main) { RexPlayerController.startSyncPlayback(0L) }
        }
    }

internal suspend fun RexMusicSyncEngine.__splitImpl_syncLocalTrack(id: String, pos: Long, playing: Boolean, startAt: Long) {
        RexPlayerController.init(appContextRef ?: return)
        RexPlayerController.setSynchronizedPlaybackMode(true)
        var track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
        // A transfer socket can finish a fraction earlier than the receiving library index;
        // wait briefly for the completed cache to become visible before declaring failure.
        val cacheDeadline = SystemClock.elapsedRealtime() + 15000L
        while (track == null && SystemClock.elapsedRealtime() < cacheDeadline) {
            delay(120L)
            track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
        }
        val readyTrack = track ?: run {
            _state.value = _state.value.copy(status = "Lagu belum ada di HP ini", error = "Lagu '$id' belum diterima atau belum terindeks di library offline; tunggu transfer cache selesai.")
            return
        }
        withContext(Dispatchers.Main) { RexPlayerController.prepareFromOfflineForSync(readyTrack) }
        val deadline = SystemClock.elapsedRealtime() + 12000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val s = RexPlayerController.state.value
            if (s.nowPlaying?.id == id && s.durationMs > 0L && s.phase == LoadPhase.Idle) break
            delay(80L)
        }
        val actual = RexPlayerController.state.value
        if (actual.nowPlaying?.id != id || actual.durationMs <= 0L) {
            _state.value = _state.value.copy(status = "Lagu belum siap diputar", error = "Player belum selesai menyiapkan cache lagu.")
            return
        }

        // The host's target position is the position at the shared start deadline.
        // If the command arrived after that deadline, project forward by the same elapsed time.
        val hostNow = SystemClock.elapsedRealtime() + clockOffsetMs
        val targetHostElapsed = if (playing && startAt > 0L) startAt else hostNow
        val targetPos = (pos + if (playing) (hostNow - targetHostElapsed).coerceAtLeast(0L) else 0L)
            .coerceIn(0L, actual.durationMs)
        val syncRepeat = _state.value
        withContext(Dispatchers.Main) {
            RexPlayerController.setSynchronizedRepeat(syncRepeat.repeatTotal, syncRepeat.repeatLeft)
            RexPlayerController.pause()
            RexPlayerController.prepareSyncSeek(targetPos)
        }

        if (playing) {
            val targetLocalElapsed = targetHostElapsed - clockOffsetMs
            delay(max(0L, targetLocalElapsed - SystemClock.elapsedRealtime()))
            withContext(Dispatchers.Main) {
                if (RexPlayerController.state.value.nowPlaying?.id == id) {
                    // The seek has already run during the lead-in; this only releases playback.
                    RexPlayerController.startSyncPlayback(targetPos)
                }
            }
        } else {
            _state.value = _state.value.copy(positionMs = targetPos, playing = false)
        }
    }
