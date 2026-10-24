// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RexPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RexLg,
    padding: Dp = 16.dp,
    spacing: Dp = 16.dp,
    glow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = MaterialTheme.colorScheme
    val border = BorderStroke(
        1.dp,
        Brush.linearGradient(
            listOf(s.outline.copy(alpha = 0.55f), s.outlineVariant.copy(alpha = 0.25f), s.primary.copy(alpha = 0.20f))
        )
    )
    val color = s.surfaceContainerLow.copy(alpha = 0.80f)
    val glowMod = if (glow) {
        Modifier.shadow(26.dp, shape, ambientColor = s.primary.copy(alpha = 0.35f), spotColor = s.primary.copy(alpha = 0.35f))
    } else Modifier
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(padding), Arrangement.spacedBy(spacing), content = content)
    }
    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth().then(glowMod),
            shape = shape, color = color, border = border, content = body
        )
    } else {
        val src = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().pressScale(src, 0.97f).then(glowMod),
            shape = shape, color = color, border = border,
            interactionSource = src, content = body
        )
    }
}

/* ═════════════════════ ICON TILE & HERO ═════════════════════ */

@Composable
fun RexSkeletonPanel(media: Boolean = true, modifier: Modifier = Modifier) {
    RexPanel(modifier.shimmer()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexSkeleton(Modifier.size(46.dp), CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RexSkeleton(Modifier.width(140.dp).height(13.dp))
                RexSkeleton(Modifier.width(90.dp).height(11.dp))
            }
        }
        RexSkeleton(Modifier.fillMaxWidth().height(13.dp))
        if (media) RexSkeleton(Modifier.fillMaxWidth().height(190.dp), RexMd)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RexSkeleton(Modifier.weight(1f).height(50.dp), RexMd)
            RexSkeleton(Modifier.weight(1f).height(50.dp), RexMd)
        }
    }
}

/* ═════════════════════ MEDIA ═════════════════════ */
