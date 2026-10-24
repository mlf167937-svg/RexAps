package com.rexaps.rexmusic.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*

@Composable
internal fun NeighborSection(state: RexMusicUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val previous = state.previous
    val upNext = state.upNext
    if (previous == null && upNext == null && !state.autoplay) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (previous != null) {
            NeighborRow("SEBELUMNYA", previous, Icons.Default.SkipPrevious, onPrevious)
        }
        if (upNext != null) {
            NeighborRow(
                if (state.userQueue.isNotEmpty()) "DI ANTRIAN" else "SELANJUTNYA",
                upNext, Icons.Default.SkipNext, onNext
            )
        } else if (state.autoplay) {
            AutoplayRow(state.favoriteArtists, state.popularCount, state.offline.enabled, onNext)
        }
    }
}

@Composable
internal fun NeighborRow(
    label: String,
    track: RexTrack,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverThumb(track.cover, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Text(
                track.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}
