package com.rexaps.rexmusic

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
internal fun DockStatusSignal(clock: MotionClock) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offset = index * 0.23f
            val height = 3.5.dp + (clock.pulse.value * (5 + index % 2 * 2) + sin(clock.slow.value * TwoPi + offset) * 2f + 2f).dp
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(2.5.dp)
                    .height(height.coerceIn(3.dp, 12.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index == 1) scheme.tertiary.copy(alpha = 0.8f) else scheme.primary.copy(alpha = 0.65f))
            )
        }
    }
    Text(
        text = "AUDIO ENGINE",
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onSurfaceVariant.copy(alpha = 0.58f),
        maxLines = 1,
        modifier = Modifier.padding(top = 5.dp)
    )
}

@Composable
internal fun DockIndicator(height: Dp, clock: MotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                // Glow terkontrol di belakang indikator
                val r = this.size.width * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.20f + 0.07f * clock.pulse.value), Color.Transparent),
                        radius = r
                    ),
                    radius = r
                )
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.24f), tertiary.copy(alpha = 0.10f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(primary.copy(alpha = 0.55f), tertiary.copy(alpha = 0.25f))),
                shape
            )
            .drawWithContent {
                drawContent()
                // Kapsul aksen di sisi kiri
                val barH = this.size.height * 0.42f
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, (this.size.height - barH) / 2f),
                    size = Size(3.dp.toPx(), barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
    )
}

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
