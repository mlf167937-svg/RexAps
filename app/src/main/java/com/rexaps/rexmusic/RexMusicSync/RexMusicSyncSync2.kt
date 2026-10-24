package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
internal fun SyncOverviewCard(
    state: SyncUiState,
    active: Boolean,
    connecting: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.primaryContainer
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Music, in sync.",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Putar bareng di jaringan Wi-Fi yang sama",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                val statusColor = when {
                    state.error != null -> scheme.errorContainer
                    active -> scheme.tertiaryContainer
                    else -> scheme.surface
                }
                val statusTextColor = when {
                    state.error != null -> scheme.onErrorContainer
                    active -> scheme.onTertiaryContainer
                    else -> scheme.onSurfaceVariant
                }
                Surface(shape = RoundedCornerShape(50), color = statusColor) {
                    Text(
                        when {
                            state.error != null -> "ERROR"
                            state.hosting -> "HOST"
                            state.connected -> "CONNECTED"
                            connecting -> "CONNECTING"
                            else -> "OFFLINE"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor
                    )
                }
            }

            Text(
                state.status,
                style = MaterialTheme.typography.titleSmall,
                color = if (state.error != null) scheme.error else scheme.onSurface
            )
            if (connecting) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Mencoba menghubungkan…", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            if (active) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ROOM", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Text(state.room, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MEMBERS", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Text(
                            "${state.members.distinctBy { it.id }.size}",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.primary
                        )
                    }
                    if (state.roomElapsedMs > 0L) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ROOM TIME", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            Text(formatRoomTime(state.roomElapsedMs), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            if (state.cacheStatus.isNotBlank()) {
                Text(state.cacheStatus, style = MaterialTheme.typography.bodySmall, color = scheme.tertiary)
            }
            state.error?.let { errorText ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.errorContainer
                ) {
                    Text(
                        errorText,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = scheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
internal fun SyncSectionHeading(
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color? = null,
    subtitleColor: androidx.compose.ui.graphics.Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor ?: scheme.primary)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor ?: scheme.onSurfaceVariant
            )
        }
    }
}
