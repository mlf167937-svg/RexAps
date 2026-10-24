package com.rexaps.rexmusic

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexmusic.player.EqualizerBars

@Composable
fun TrackRow(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    loading: Boolean,
    downloaded: Boolean,
    status: DownloadStatus?,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverThumb(track.cover, 52.dp, overlay = overlayFor(active, playing, loading))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = if (active) scheme.primary else scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloaded) {
                    Icon(
                        Icons.Default.DownloadDone, contentDescription = "Tersimpan offline",
                        tint = scheme.tertiary, modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    subtitle ?: track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (status != null) {
            DownloadRing(status, 20.dp)
            Spacer(Modifier.width(6.dp))
        }
        RexIconButton(
            Icons.Default.MoreVert, "Opsi lagu", onMore,
            size = 40.dp, iconSize = 22.dp, tint = scheme.onSurfaceVariant
        )
    }
}

//  Quick grid (Home) 

@Composable
internal fun QuickTileCard(tile: QuickTile, active: Boolean, playing: Boolean, modifier: Modifier) {
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

//  Shelf cards 

@Composable
fun ShelfCard(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp
) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    Column(
        modifier.width(size)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(size)) {
            CoverImage(track.cover, Modifier.fillMaxSize(), RoundedCornerShape(10.dp), iconSize = 44.dp)
            if (active && playing) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBars(color = Color.White, barWidth = 2.dp, height = 12.dp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall,
                color = if (active) scheme.primary else scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArtistCard(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = rexTileColor(name)
    Column(
        modifier.width(108.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.55f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(), color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Text(
            name, style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

//  Downloads & queue rows 
