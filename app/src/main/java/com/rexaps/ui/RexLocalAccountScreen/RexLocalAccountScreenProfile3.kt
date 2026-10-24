// RexLocalAccountScreen.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ProfileStats(ctl: RexProfileController) {
    val colors = MaterialTheme.colorScheme
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(ctl.stories.size, "Story", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(colors.outlineVariant.copy(.6f)))
            StatItem(ctl.videos.size, "Video", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(colors.outlineVariant.copy(.6f)))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text("100% Lokal", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ProfileActions(ctl: RexProfileController) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { ctl.showEdit = true },
            Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit profil")
        }
        FilledTonalButton(
            onClick = { ctl.showStoryCompose = true },
            Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buat story")
        }
    }
}

@Composable
internal fun StoryTile(st: RexStory, acct: RexLocalAccount, rev: Int, modifier: Modifier, onClick: () -> Unit) {
    val left = (DAY_MS - (System.currentTimeMillis() - st.time)).coerceAtLeast(0)
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.pressable(onClick).clip(shape).border(1.dp, Color.White.copy(alpha = .08f), shape)) {
        if (st.isVideo) {
            MediaThumb(st.file, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(storyGradients[st.gradient.mod(storyGradients.size)])
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    st.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black.copy(.35f), .3f to Color.Transparent, .7f to Color.Transparent, 1f to Color.Black.copy(.6f))
        )))
        AvatarContent(
            acct, rev, false,
            Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).border(2.dp, Color.White, CircleShape)
        )
        Text(
            "${left / 3_600_000}j lagi",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
    }
}
