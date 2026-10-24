package com.rexaps.rexmusic

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI

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

@Composable
internal fun portraitReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
internal fun PortraitBottomChrome(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    clock: PortraitMotionClock
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val y = 0.75.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, scheme.primary.copy(alpha = 0.56f), scheme.tertiary.copy(alpha = 0.46f), Color.Transparent)
                    ),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .shadow(
                elevation = 19.dp,
                shape = PortraitNavigationShape,
                ambientColor = scheme.primary.copy(alpha = 0.12f),
                spotColor = scheme.tertiary.copy(alpha = 0.13f)
            )
            .clip(PortraitNavigationShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surface.copy(alpha = 0.96f),
                        scheme.surfaceVariant.copy(alpha = 0.97f),
                        scheme.surface.copy(alpha = 0.99f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(scheme.primary.copy(alpha = 0.18f), scheme.outlineVariant.copy(alpha = 0.32f))
                ),
                shape = PortraitNavigationShape
            )
    ) {
        Box(Modifier.fillMaxWidth()) {
            PortraitTopSheen(clock, Modifier.fillMaxWidth().height(3.dp))
        }

        Box(Modifier.fillMaxWidth()) {
            // The mini player remains the original composable, wrapped rather than reimplemented.
            miniPlayer()
        }

        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp)
                .drawBehind {
                    val edge = scheme.outlineVariant.copy(alpha = 0.22f)
                    drawLine(edge, Offset(12.dp.toPx(), 0f), Offset(size.width - 12.dp.toPx(), 0f), 0.7.dp.toPx())
                },
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            val itemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = scheme.primary,
                selectedTextColor = scheme.primary,
                indicatorColor = scheme.primary.copy(alpha = 0.12f),
                unselectedIconColor = scheme.onSurfaceVariant,
                unselectedTextColor = scheme.onSurfaceVariant
            )
            PortraitNavItems.forEach { item ->
                val selected = selectedTab == item.tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(item.tab) },
                    icon = {
                        PortraitNavigationIcon(
                            image = item.icon,
                            description = item.label,
                            selected = selected,
                            clock = clock
                        )
                    },
                    label = { Text(item.label, maxLines = 1, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                    colors = itemColors,
                    alwaysShowLabel = true
                )
            }
        }
        PortraitBottomAccent(clock, Modifier.fillMaxWidth().height(4.dp))
    }
}

@Composable
internal fun PortraitTopSheen(clock: PortraitMotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val center = size.width * (0.08f + 0.84f * clock.pulse.value)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, scheme.primary.copy(alpha = 0.72f), scheme.tertiary.copy(alpha = 0.48f), Color.Transparent),
                startX = center - size.width * 0.2f,
                endX = center + size.width * 0.2f
            ),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
        )
    }
}

@Composable
internal fun PortraitBottomAccent(clock: PortraitMotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val midY = size.height / 2f
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, scheme.primary.copy(alpha = 0.13f + clock.pulse.value * 0.12f), scheme.tertiary.copy(alpha = 0.12f), Color.Transparent)
            ),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
