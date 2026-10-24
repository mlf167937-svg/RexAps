package com.rexaps.rexmusic

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI

internal val TwoPi = (2.0 * PI).toFloat()

internal val DegToRad = (PI / 180.0).toFloat()

internal val LandscapeParticles = List(34) { index ->
    // A deterministic low-discrepancy layout gives the canvas texture without a Random on every draw.
    val x = ((index * 37 + 13) % 101) / 100f
    val y = ((index * 61 + 29) % 97) / 96f
    AmbientParticle(x, y, 0.7f + (index % 4) * 0.24f, index * 0.71f, 0.35f + (index % 5) * 0.13f)
}

/** Dua "jam" animasi bersama (lambat + denyut) agar hanya ada satu InfiniteTransition. */
@Stable
internal class MotionClock(val slow: State<Float>, val pulse: State<Float>)

internal data class DockItem(val tab: Int, val icon: ImageVector, val label: String)

// ───────────────────────── Root ─────────────────────────

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
internal fun FloatingAudioDock(clock: MotionClock, miniPlayer: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.20f),
                spotColor = primary.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        scheme.surface.copy(alpha = 0.97f),
                        scheme.surfaceVariant.copy(alpha = 0.88f),
                        scheme.surface.copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        primary.copy(alpha = 0.38f),
                        scheme.outlineVariant.copy(alpha = 0.25f),
                        scheme.tertiary.copy(alpha = 0.38f)
                    )
                ),
                shape
            )
            .drawWithContent {
                drawContent()
                // Highlight tipis yang menyapu tepi atas dock
                val w = this.size.width
                val cx = w * (0.1f + 0.8f * clock.pulse.value)
                val y = 0.75.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, primary.copy(alpha = 0.75f), Color.Transparent),
                        startX = cx - w * 0.2f,
                        endX = cx + w * 0.2f
                    ),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
    ) {
        miniPlayer()
    }
}
