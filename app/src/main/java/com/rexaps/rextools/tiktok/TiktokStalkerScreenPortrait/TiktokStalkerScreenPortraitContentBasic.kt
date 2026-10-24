package com.rexaps.rextools.tiktok

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.TiktokStalkResult
import com.rexaps.rextools.ui.*

/* ═════════════════════ KARTU BASIC ═════════════════════ */

@Composable
internal fun BasicProfileCard(r: TiktokStalkResult, onOpen: (String) -> Unit) {
    RexPanel(padding = 0.dp, spacing = 0.dp, glow = true) {
        ProfileHeader(
            avatar = r.avatar, name = r.name, username = r.username, bio = r.bio,
            verified = r.verified?.equals("Verified", ignoreCase = true) == true,
            region = r.region
        )
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            r.stats?.let { st ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RexStat("Followers", st.followers ?: "-", Modifier.weight(1f), Icons.Default.Group, 0)
                    RexStat("Following", st.following ?: "-", Modifier.weight(1f), Icons.Default.PersonAdd, 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RexStat("Likes", st.likes ?: "-", Modifier.weight(1f), Icons.Default.FavoriteBorder, 2)
                    RexStat("Videos", st.videos ?: "-", Modifier.weight(1f), Icons.Default.Videocam, 3)
                }
            }
            r.link?.takeIf { it.isNotBlank() }?.let { link ->
                RexTonalButton("Buka di TikTok", Icons.Default.OpenInNew, { onOpen(link) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun StoryRow(label: String, index: Int, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    RexPanel(
        modifier = Modifier.entrance(index.coerceAtMost(8), 50, 14.dp),
        onClick = onClick, shape = RexMd, padding = 12.dp, spacing = 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexIconTile(Icons.Default.Image, size = 42.dp)
            Text(
                label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.OpenInNew, contentDescription = "Buka", tint = s.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun VideoTile(label: String, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(116.dp)
            .entrance(index.coerceAtMost(9), 45, 16.dp)
            .pressScale(source, 0.94f)
            .clip(RexMd)
            .background(
                Brush.linearGradient(
                    listOf(s.primary.copy(alpha = 0.22f), s.surfaceContainerHigh, s.tertiary.copy(alpha = 0.16f))
                )
            )
            .border(hairline(), RexMd)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = s.onSurface, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = s.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
