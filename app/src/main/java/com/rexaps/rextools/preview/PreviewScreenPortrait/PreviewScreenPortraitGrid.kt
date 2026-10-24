package com.rexaps.rextools.preview

import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*

/* ═════════════════════ GRID ═════════════════════ */

@Composable
internal fun GridView(items: List<PreviewItem>, onOpen: (Int) -> Unit) {
    val images = remember(items) { items.count { it.type == MediaType.IMAGE } }
    val videos = remember(items) { items.count { it.type == MediaType.VIDEO } }
    val others = items.size - images - videos

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(bottom = 4.dp).entrance(0), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (images > 0) RexPill("$images foto", icon = Icons.Default.Image)
                if (videos > 0) RexPill("$videos video", icon = Icons.Default.Videocam)
                if (others > 0) RexPill("$others link", icon = Icons.Default.Link)
            }
        }
        itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
            GridTile(item, number = idx + 1, index = idx, onClick = { onOpen(idx) })
        }
    }
}

@Composable
private fun GridTile(item: PreviewItem, number: Int, index: Int, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val desc = when (item.type) {
        MediaType.IMAGE -> "Gambar $number"
        MediaType.VIDEO -> "Video $number"
        MediaType.UNKNOWN -> "Link $number"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .entrance(index.coerceAtMost(12), 45, 18.dp)
            .pressScale(interaction, 0.95f)
            .clip(RexMd)
            .background(s.surfaceContainerHigh)
            .border(hairline(), RexMd)
            .semantics { contentDescription = desc }
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    ) {
        if (item.type == MediaType.UNKNOWN) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RexIconTile(Icons.Default.Link, size = 48.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = s.onSurfaceVariant
                )
            }
        } else {
            RexImage(item.url, null, Modifier.fillMaxSize(), ContentScale.Crop)

            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
            )

            if (item.type == MediaType.VIDEO) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Text(
                item.fileName,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val (ic, label) = when (item.type) {
                MediaType.IMAGE -> Icons.Default.Image to "IMG"
                MediaType.VIDEO -> Icons.Default.Videocam to "VID"
                MediaType.UNKNOWN -> Icons.Default.Link to "URL"
            }
            Icon(ic, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
