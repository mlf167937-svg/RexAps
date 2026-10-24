package com.rexaps.rexmusic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.PI

// ───────────────────────── Motion tokens ─────────────────────────

internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

internal val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

internal val TwoPi = (2.0 * PI).toFloat()

internal val DegToRad = (PI / 180.0).toFloat()

/** Seeded, stable particle coordinates prevent shimmer caused by regenerating randomness per frame. */
internal data class AmbientParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float
)

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
