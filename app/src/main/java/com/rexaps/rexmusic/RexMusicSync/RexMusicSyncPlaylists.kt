package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
internal fun SyncPlaylistShareCard(
    playlist: RexPlaylist,
    availableCount: Int,
    onShare: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val total = playlist.tracks.distinctBy { it.id }.size
    val ready = availableCount.coerceIn(0, total)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(scheme.surface, scheme.surfaceVariant.copy(alpha = 0.42f))))
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(66.dp).clip(RoundedCornerShape(16.dp))
                    .background(scheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val cover = playlist.tracks.firstOrNull { it.cover.isNotBlank() }?.cover
                if (!cover.isNullOrBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "Cover playlist ${playlist.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(29.dp))
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = scheme.surface.copy(alpha = 0.92f)
                ) {
                    Text("$total", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(playlist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$ready dari $total lagu siap dibagikan", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
                LinearProgressIndicator(
                    progress = if (total > 0) ready.toFloat() / total.toFloat() else 0f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)),
                    color = scheme.primary,
                    trackColor = scheme.surfaceVariant
                )
                if (ready < total) {
                    Text("Lagu yang belum tersedia akan diunduh otomatis saat Share.", style = MaterialTheme.typography.labelSmall, color = scheme.tertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            FilledTonalButton(
                onClick = onShare,
                enabled = total > 0,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Share")
            }
        }
    }
}

@Composable
internal fun SyncEmptyPlaylistCard() {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Belum ada playlist pribadi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Buat playlist di RexMusic. Playlist akan muncul di sini untuk dibagikan ke room.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}
