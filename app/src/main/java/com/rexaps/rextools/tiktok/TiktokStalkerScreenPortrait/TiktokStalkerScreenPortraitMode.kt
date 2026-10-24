package com.rexaps.rextools.tiktok

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*

/* ═════════════════════ MODE SWITCH ═════════════════════ */

@Composable
internal fun ModeSwitch(full: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RexMd)
            .background(s.surfaceContainerLow.copy(alpha = 0.85f))
            .border(hairline(), RexMd)
            .padding(4.dp)
    ) {
        val segW = (maxWidth - 8.dp) / 2
        val x by animateDpAsState(if (full) segW else 0.dp, spring(0.75f, Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segW)
                .fillMaxHeight()
                .clip(RexSm)
                .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.30f), s.tertiary.copy(alpha = 0.18f))))
                .border(1.dp, s.primary.copy(alpha = 0.5f), RexSm)
        )
        Row(Modifier.fillMaxSize()) {
            ModeOption("Biasa", "Profil & statistik", Icons.Default.Bolt, !full, enabled, { onChange(false) }, Modifier.weight(1f))
            ModeOption("Full", "Story & video", Icons.Default.Public, full, enabled, { onChange(true) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeOption(
    title: String, hint: String, icon: ImageVector,
    selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier
) {
    val s = MaterialTheme.colorScheme
    val fg by animateColorAsState(if (selected) s.onSurface else s.onSurfaceVariant, tween(250), label = "fg")
    val tint by animateColorAsState(if (selected) s.primary else s.onSurfaceVariant, tween(250), label = "tint")
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, spring(0.6f), label = "sc")
    Row(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, enabled = enabled, onClick = onClick
            )
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fg)
            Text(hint, style = MaterialTheme.typography.labelSmall, color = s.onSurfaceVariant, maxLines = 1)
        }
    }
}
