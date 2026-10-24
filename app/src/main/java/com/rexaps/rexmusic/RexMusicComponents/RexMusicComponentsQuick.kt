package com.rexaps.rexmusic

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.player.EqualizerBars

//  Quick grid (Home) 

@Composable
fun QuickGrid(tiles: List<QuickTile>, nowId: String?, playing: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { tile ->
                    QuickTileCard(tile, tile.id == nowId, playing, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTileCard(tile: QuickTile, active: Boolean, playing: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceVariant)
            .clickable(onClick = tile.onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tile.special) {
            Box(
                Modifier.size(56.dp).background(RexOfflineBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.OfflinePin, contentDescription = null, tint = Color.White)
            }
        } else {
            CoverImage(tile.cover, Modifier.size(56.dp), RoundedCornerShape(0.dp))
        }
        Text(
            tile.title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) scheme.primary else scheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        )
        if (active && playing) {
            EqualizerBars(
                Modifier.padding(end = 10.dp), color = scheme.primary,
                barWidth = 3.dp, height = 14.dp
            )
        }
    }
}
