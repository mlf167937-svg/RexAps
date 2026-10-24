package com.rexaps.rexmusic

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Transisi tab tanpa menduplikasi / mereset content: `content` hanya dipanggil SEKALI,
 * efek sinematik (fade + slide + scale halus) dilakukan via graphicsLayer pada container.
 */
@Composable
internal fun ContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    content: @Composable (Modifier) -> Unit
) {
    val progress = remember { Animatable(1f) }
    var settledTab by remember { mutableStateOf(selectedTab) }
    var direction by remember { mutableStateOf(1f) }
    val shiftPx = with(LocalDensity.current) { 40.dp.toPx() }

    LaunchedEffect(selectedTab) {
        if (selectedTab != settledTab) {
            direction = if (selectedTab > settledTab) 1f else -1f
            if (reduceMotion) {
                settledTab = selectedTab
            } else {
                progress.snapTo(0f)
                settledTab = selectedTab
                progress.animateTo(1f, tween(420, easing = EmphasizedDecelerate))
            }
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 1440.dp)
            .fillMaxSize()
            .graphicsLayer {
                // Frame pertama setelah tab berganti: tahan di alpha 0 agar tidak flicker.
                val switching = selectedTab != settledTab && !reduceMotion
                val p = if (switching) 0f else progress.value
                alpha = p
                translationX = (1f - p) * shiftPx * direction
                val s = 0.985f + 0.015f * p
                scaleX = s
                scaleY = s
            }
    ) {
        content(Modifier.fillMaxSize())
    }
}

// ───────────────────────── Ambient backdrop ─────────────────────────

@Composable
internal fun DockItemView(
    item: DockItem,
    selected: Boolean,
    showLabel: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val glowColor = scheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "dockPressScale"
    )
    val transition = updateTransition(targetState = selected, label = "dockItemTransition")
    val progress by transition.animateFloat(
        label = "dockItemProgress",
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) }
    ) { if (it) 1f else 0f }
    val tint by transition.animateColor(
        label = "dockItemTint",
        transitionSpec = { tween(280) }
    ) { if (it) scheme.primary else scheme.onSurfaceVariant }

    val iconModifier = Modifier
        .size(24.dp)
        .drawBehind {
            val p = progress.coerceIn(0f, 1f)
            if (p > 0.01f) {
                val r = this.size.minDimension * 1.1f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowColor.copy(alpha = 0.30f * p), Color.Transparent),
                        radius = r
                    ),
                    radius = r
                )
            }
        }
        .graphicsLayer {
            val s = 1f + 0.14f * progress.coerceIn(0f, 1f)
            scaleX = s
            scaleY = s
            if (!showLabel) translationY = -7.dp.toPx() * progress.coerceIn(0f, 1f)
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = tint, modifier = iconModifier)
                Spacer(Modifier.height(4.dp))
                Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
            }
        } else {
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = iconModifier)
            Text(
                item.label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
            )
        }
    }
}
