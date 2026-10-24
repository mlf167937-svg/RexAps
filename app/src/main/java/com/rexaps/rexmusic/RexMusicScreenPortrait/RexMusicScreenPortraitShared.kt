package com.rexaps.rexmusic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.PI

internal val PortraitNavigationShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

internal val PortraitEase = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

internal val PortraitTwoPi = (2.0 * PI).toFloat()

internal data class PortraitMotionClock(val drift: State<Float>, val pulse: State<Float>)

internal data class PortraitDust(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val velocity: Float
)

/** Stable particles are calculated once, so recomposition never changes their positions. */
internal val PortraitDustField = List(28) { index ->
    PortraitDust(
        x = ((index * 29 + 11) % 103) / 102f,
        y = ((index * 47 + 19) % 101) / 100f,
        radius = 0.6f + (index % 4) * 0.22f,
        phase = index * 0.83f,
        velocity = 0.35f + (index % 6) * 0.11f
    )
}

internal data class PortraitNavItem(val tab: Int, val icon: ImageVector, val label: String)

internal val PortraitNavItems = listOf(
    PortraitNavItem(TAB_HOME, Icons.Default.Home, "Beranda"),
    PortraitNavItem(TAB_SEARCH, Icons.Default.Search, "Cari"),
    PortraitNavItem(TAB_PLAYLIST, Icons.Default.QueueMusic, "Playlist"),
    PortraitNavItem(TAB_LIBRARY, Icons.Default.LibraryMusic, "Koleksi"),
    PortraitNavItem(TAB_SYNC, Icons.Default.Groups, "Sync")
)
