package com.rexaps.ui

import com.rexaps.ui.theme.RexMotionRuntime
import com.rexaps.ui.theme.RexAnimationLevel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_ProFeaturedCard(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    val glow by animateFloatAsState(
        targetValue = if (app.available) 1f else 0.45f,
        animationSpec = tween(when (RexMotionRuntime.settings.level) {
            RexAnimationLevel.OFF -> 0
            RexAnimationLevel.SAVER -> 180
            RexAnimationLevel.BALANCED -> 360
            RexAnimationLevel.FULL -> 520
        }, easing = FastOutSlowInEasing),
        label = "featuredModuleGlow"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .alpha(if (app.available) 1f else 0.6f)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = 0.22f),
                spotColor = colors.primary.copy(alpha = 0.24f)
            )
            .clip(shape)
            .background(colors.surfaceVariant)
            .proPress(enabled = app.available, onClick = onClick)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.20f * glow),
                            colors.tertiary.copy(alpha = 0.13f * glow),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.primary.copy(alpha = 0.16f * glow), Color.Transparent),
                        center = Offset(size.width * 0.87f, size.height * 0.18f),
                        radius = size.width * 0.52f
                    ),
                    radius = size.width * 0.52f,
                    center = Offset(size.width * 0.87f, size.height * 0.18f)
                )
            }
            .padding(22.dp)
    ) {
        ProModuleAmbientLayer(
            moduleId = app.id,
            modifier = Modifier.fillMaxSize(),
            intensity = 0.06f
        )
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f))
                        .border(1.dp, colors.primary.copy(alpha = 0.18f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Aplikasi utama",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                ProModuleGlyph(
                    moduleId = app.id,
                    size = 68.dp,
                    modifier = Modifier.alpha(0.94f)
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (app.available) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.13f))
                            .border(1.dp, colors.primary.copy(alpha = 0.22f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Buka",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                } else {
                    Text(
                        "Segera",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.surface.copy(alpha = 0.72f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** Latar gradient radial yang bergerak pelan. */
@Composable
internal fun __splitImpl_ProAmbientBackdrop(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val motion = RexMotionRuntime.settings
    val animationsEnabled = motion.level != RexAnimationLevel.OFF
    val duration = when (motion.level) {
        RexAnimationLevel.FULL -> 10_000
        RexAnimationLevel.BALANCED -> 18_500
        RexAnimationLevel.SAVER -> 30_000
        RexAnimationLevel.OFF -> 0
    }
    val driftState = if (animationsEnabled) {
        rememberInfiniteTransition(label = "rexPremiumAmbientDrift").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambientDrift"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val drift by driftState
    val sweepState = if (animationsEnabled) {
        rememberInfiniteTransition(label = "rexPremiumAmbientSweep").animateFloat(
            initialValue = -0.25f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween((duration * .78f).toInt().coerceAtLeast(1), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambientSweep"
        )
    } else {
        remember { mutableFloatStateOf(-0.25f) }
    }
    val sweep by sweepState
    val dustCount = when (motion.level) {
        RexAnimationLevel.FULL -> 30
        RexAnimationLevel.BALANCED -> 18
        RexAnimationLevel.SAVER -> 7
        RexAnimationLevel.OFF -> 0
    }
    val intensity = motion.effectIntensity / 72f
    Box(modifier.drawBehind {
        val w = size.width
        val h = size.height
        val min = size.minDimension
        drawRect(c.background)

        // Three large, low-contrast color fields produce a living aurora without
        // bitmap blur, off-screen layers, or per-frame layout work.
        drawRect(Brush.radialGradient(
            colors = listOf(c.primary.copy(alpha = 0.23f * intensity), c.primary.copy(alpha = 0.045f * intensity), Color.Transparent),
            center = Offset(w * (0.12f + 0.58f * drift), h * (0.10f + 0.08f * drift)),
            radius = min * 1.12f
        ))
        drawRect(Brush.radialGradient(
            colors = listOf(c.tertiary.copy(alpha = 0.17f * intensity), c.tertiary.copy(alpha = 0.035f * intensity), Color.Transparent),
            center = Offset(w * (0.92f - 0.47f * drift), h * (0.84f - 0.12f * drift)),
            radius = min * 0.94f
        ))
        drawRect(Brush.radialGradient(
            colors = listOf(c.secondary.copy(alpha = 0.10f * intensity), Color.Transparent),
            center = Offset(w * (0.82f - 0.2f * drift), h * (0.40f + 0.1f * drift)),
            radius = min * 0.62f
        ))
        drawRect(Brush.linearGradient(
            colors = listOf(Color.Transparent, c.primary.copy(alpha = if (animationsEnabled) 0.055f * intensity else 0f), c.tertiary.copy(alpha = if (animationsEnabled) 0.035f * intensity else 0f), Color.Transparent),
            start = Offset(w * (sweep - 0.45f), 0f),
            end = Offset(w * (sweep + 0.45f), h)
        ))

        // Fine orbital rings add depth while staying subtle beneath content.
        val orbitCenter = Offset(w * (0.88f - 0.08f * drift), h * (0.17f + 0.06f * drift))
        drawCircle(c.primary.copy(alpha = 0.10f), radius = min * 0.19f, center = orbitCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1f))
        drawCircle(c.tertiary.copy(alpha = 0.075f), radius = min * 0.27f, center = orbitCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        drawCircle(c.outlineVariant.copy(alpha = 0.045f), radius = min * 0.34f, center = orbitCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))

        // Deterministic floating dust: cheap Canvas circles, no allocations outside draw.
        repeat(dustCount) { i ->
            val direction = if (i % 2 == 0) 0.12f else -0.075f
            val x = ((i * 71f + sweep * w * direction) % w + w) % w
            val y = ((i * 67f + drift * h * (if (i % 3 == 0) 0.035f else -0.018f)) % h + h) % h
            val radius = if (i % 7 == 0) 2.0f else 1.0f
            val alpha = if (i % 5 == 0) 0.16f else 0.075f
            drawCircle(c.primary.copy(alpha = alpha), radius = radius, center = Offset(x, y))
        }

        // Almost invisible grid anchors the futuristic look without competing with text.
        val step = 48.dp.toPx()
        var gx = 0f
        while (gx < w) {
            drawLine(c.onBackground.copy(alpha = 0.018f), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
            gx += step
        }
        var gy = 0f
        while (gy < h) {
            drawLine(c.onBackground.copy(alpha = 0.018f), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            gy += step
        }
    })
}
