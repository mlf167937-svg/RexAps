package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
internal fun SyncMemberCard(
    member: SyncMember,
    state: SyncUiState,
    onVolumeChange: (Float) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = scheme.secondaryContainer
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = scheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        if (member.id == "host") "Host device" else "Connected · ${member.ip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (member.id == "host") {
                    Text("YOU", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                }
            }
            if (state.hosting && member.id != "host") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RexSync gain", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${((state.volumes[member.id] ?: member.volume).coerceIn(0f, 1f) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary
                    )
                }
                Slider(
                    value = (state.volumes[member.id] ?: member.volume).coerceIn(0f, 1f),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f
                )
                Text(
                    if (member.deviceVolume >= 0f) {
                        "Media volume: ${(member.deviceVolume * 100).toInt()}% · app gain is separate"
                    } else {
                        "Waiting for this device to report media volume…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            } else if (member.id != "host" && member.deviceVolume >= 0f) {
                Text(
                    "Media volume: ${(member.deviceVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

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

@Composable
internal fun SyncQueueTrackCard(track: RexTrack, onPlay: () -> Unit) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            androidx.compose.material3.Icon(Icons.Default.PlayArrow, contentDescription = "Putar dari antrean", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
