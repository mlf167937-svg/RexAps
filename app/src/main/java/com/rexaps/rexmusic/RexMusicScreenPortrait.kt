package com.rexaps.rexmusic

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.drawBehind
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val PortraitNavigationShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val PortraitEase = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val PortraitTwoPi = (2.0 * PI).toFloat()

private data class PortraitMotionClock(val drift: State<Float>, val pulse: State<Float>)
private data class PortraitDust(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val velocity: Float
)

/** Stable particles are calculated once, so recomposition never changes their positions. */
private val PortraitDustField = List(28) { index ->
    PortraitDust(
        x = ((index * 29 + 11) % 103) / 102f,
        y = ((index * 47 + 19) % 101) / 100f,
        radius = 0.6f + (index % 4) * 0.22f,
        phase = index * 0.83f,
        velocity = 0.35f + (index % 6) * 0.11f
    )
}

@Composable
private fun portraitReduceMotion(): Boolean {
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
private fun rememberPortraitMotionClock(reduceMotion: Boolean): PortraitMotionClock {
    if (reduceMotion) {
        return remember { PortraitMotionClock(mutableStateOf(0.18f), mutableStateOf(0.5f)) }
    }
    val transition = rememberInfiniteTransition(label = "rexPortraitAmbientClock")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(31_000, easing = LinearEasing)),
        label = "rexPortraitAmbientDrift"
    )
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(5_800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "rexPortraitAmbientPulse"
    )
    return remember(drift, pulse) { PortraitMotionClock(drift, pulse) }
}

/**
 * Portrait-first shell. It deliberately owns only visual chrome and layout; playback state,
 * tab data, and actions remain in the existing screen/controller. Every tint comes from the
 * host RexAps MaterialTheme, allowing the user's existing app palette to remain authoritative.
 */
@Composable
internal fun RexMusicScreenPortrait(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = portraitReduceMotion()
    val clock = rememberPortraitMotionClock(reduceMotion)

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        PortraitAmbientBackdrop(clock, Modifier.fillMaxSize())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            bottomBar = {
                AnimatedVisibility(
                    visible = selectedTab != TAB_SYNC,
                    enter = fadeIn(tween(220)) +
                        slideInVertically(tween(360, easing = PortraitEase)) { it / 2 } +
                        scaleIn(
                            animationSpec = tween(360, easing = PortraitEase),
                            initialScale = 0.975f,
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        ),
                    exit = fadeOut(tween(140)) +
                        slideOutVertically(tween(210, easing = PortraitEase)) { it / 3 } +
                        scaleOut(
                            animationSpec = tween(200, easing = PortraitEase),
                            targetScale = 0.985f,
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        )
                ) {
                    PortraitBottomChrome(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        miniPlayer = miniPlayer,
                        clock = clock
                    )
                }
            }
        ) { padding ->
            PortraitContentStage(
                selectedTab = selectedTab,
                reduceMotion = reduceMotion,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                content = content
            )
        }
    }
}

@Composable
private fun PortraitContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val progress = remember { Animatable(1f) }
    var settledTab by remember { mutableStateOf(selectedTab) }
    var direction by remember { mutableStateOf(1f) }
    val shiftPx = with(LocalDensity.current) { 24.dp.toPx() }

    LaunchedEffect(selectedTab) {
        if (selectedTab != settledTab) {
            direction = if (selectedTab >= settledTab) 1f else -1f
            if (reduceMotion) {
                progress.snapTo(1f)
                settledTab = selectedTab
            } else {
                progress.snapTo(0f)
                settledTab = selectedTab
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 330, easing = PortraitEase)
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            val p = if (reduceMotion) 1f else progress.value
            alpha = p
            translationX = (1f - p) * shiftPx * direction
            val scale = 0.992f + p * 0.008f
            scaleX = scale
            scaleY = scale
        }
    ) {
        content(Modifier.fillMaxSize())
    }
}

@Composable
private fun PortraitAmbientBackdrop(clock: PortraitMotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val drift = clock.drift.value
        val pulse = clock.pulse.value
        val angle = drift * PortraitTwoPi
        val maxDimension = max(w, h)

        // Layered aurora clouds blend the host palette instead of introducing new brand colors.
        val firstCenter = Offset(w * (0.12f + 0.035f * sin(angle)), h * (0.16f + 0.03f * cos(angle)))
        val firstRadius = maxDimension * (0.43f + 0.025f * pulse)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.13f), primary.copy(alpha = 0.035f), Color.Transparent),
                center = firstCenter,
                radius = firstRadius
            ),
            center = firstCenter,
            radius = firstRadius
        )

        val secondCenter = Offset(w * (0.94f - 0.025f * cos(angle * 0.8f)), h * (0.72f + 0.045f * sin(angle)))
        val secondRadius = maxDimension * (0.48f + 0.02f * (1f - pulse))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.095f), Color.Transparent),
                center = secondCenter,
                radius = secondRadius
            ),
            center = secondCenter,
            radius = secondRadius
        )

        val center = Offset(w * (0.55f + 0.025f * sin(angle + 1.4f)), h * 0.48f)
        val centerRadius = maxDimension * 0.27f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.055f), Color.Transparent),
                center = center,
                radius = centerRadius
            ),
            center = center,
            radius = centerRadius
        )

        // Hairline grid provides a crafted canvas texture and fades into the screen edges.
        val gridStep = 38.dp.toPx()
        val gridAlpha = 0.035f
        val lineColor = scheme.outlineVariant.copy(alpha = gridAlpha)
        var x = (w % gridStep) / 2f
        while (x < w) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, h), 0.5.dp.toPx())
            x += gridStep
        }
        var y = (h % gridStep) / 2f
        while (y < h) {
            drawLine(lineColor, Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
            y += gridStep
        }

        // Three orbit rings are intentionally oversized and partially clipped for depth.
        val orbitCenter = Offset(w * 0.91f, h * 0.24f)
        val orbitStroke = Stroke(width = 0.8.dp.toPx())
        drawCircle(primary.copy(alpha = 0.055f), h * 0.115f, orbitCenter, style = orbitStroke)
        drawCircle(primary.copy(alpha = 0.042f), h * 0.17f, orbitCenter, style = orbitStroke)
        drawCircle(tertiary.copy(alpha = 0.03f), h * 0.235f, orbitCenter, style = orbitStroke)
        val arcRadius = h * 0.17f
        val arcStart = drift * 720f + 26f
        drawArc(
            color = primary.copy(alpha = 0.17f),
            startAngle = arcStart,
            sweepAngle = 48f,
            useCenter = false,
            topLeft = Offset(orbitCenter.x - arcRadius, orbitCenter.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
        val dotAngle = (arcStart + 48f) * (PI / 180.0).toFloat()
        drawCircle(
            tertiary.copy(alpha = 0.52f),
            radius = 2.dp.toPx(),
            center = Offset(orbitCenter.x + arcRadius * cos(dotAngle), orbitCenter.y + arcRadius * sin(dotAngle))
        )

        PortraitDustField.forEach { dust ->
            val particleAngle = drift * PortraitTwoPi * dust.velocity + dust.phase
            val px = w * dust.x + sin(particleAngle) * w * 0.008f
            val py = h * dust.y + cos(particleAngle * 0.7f) * h * 0.009f
            val alpha = (0.025f + 0.05f * ((sin(particleAngle) + 1f) / 2f)) * (0.7f + pulse * 0.3f)
            drawCircle(
                color = if (dust.phase.toInt() % 2 == 0) primary.copy(alpha = alpha) else tertiary.copy(alpha = alpha * 0.75f),
                radius = dust.radius.dp.toPx(),
                center = Offset(px, py)
            )
        }

        // A slim reactive trace adds music identity to the negative space at the bottom.
        val baseY = h * 0.985f
        val traceWidth = w * 0.62f
        val traceStart = w * 0.19f
        val points = 52
        val gap = 2.2.dp.toPx()
        val barWidth = (traceWidth - gap * (points - 1)) / points
        for (index in 0 until points) {
            val normalized = index.toFloat() / (points - 1).coerceAtLeast(1)
            val envelope = sin(normalized * PI.toFloat()).toFloat()
            val wave = abs(sin(index * 0.43f + drift * PortraitTwoPi * 1.8f))
            val barHeight = (1.4f + envelope * (4f + 9f * wave) * (0.48f + pulse * 0.52f)).dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.10f), tertiary.copy(alpha = 0.025f))
                ),
                topLeft = Offset(traceStart + index * (barWidth + gap), baseY - barHeight),
                size = Size(barWidth.coerceAtLeast(1f), barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun PortraitBottomChrome(
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

private data class PortraitNavItem(val tab: Int, val icon: ImageVector, val label: String)
private val PortraitNavItems = listOf(
    PortraitNavItem(TAB_HOME, Icons.Default.Home, "Beranda"),
    PortraitNavItem(TAB_SEARCH, Icons.Default.Search, "Cari"),
    PortraitNavItem(TAB_PLAYLIST, Icons.Default.QueueMusic, "Playlist"),
    PortraitNavItem(TAB_LIBRARY, Icons.Default.LibraryMusic, "Koleksi"),
    PortraitNavItem(TAB_SYNC, Icons.Default.Groups, "Sync")
)

@Composable
private fun PortraitNavigationIcon(
    image: ImageVector,
    description: String,
    selected: Boolean,
    clock: PortraitMotionClock
) {
    val scheme = MaterialTheme.colorScheme
    val transition = updateTransition(targetState = selected, label = "portraitNavItemTransition")
    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) },
        label = "portraitNavScale"
    ) { active -> if (active) 1.12f else 1f }
    val halo by transition.animateFloat(
        transitionSpec = { tween(260, easing = FastOutSlowInEasing) },
        label = "portraitNavHalo"
    ) { active -> if (active) 1f else 0f }
    val tint by transition.animateColor(
        transitionSpec = { tween(230) },
        label = "portraitNavTint"
    ) { active -> if (active) scheme.primary else scheme.onSurfaceVariant }

    Box(
        Modifier
            .size(width = 43.dp, height = 31.dp)
            .drawBehind {
                if (halo > 0.005f) {
                    val radius = size.minDimension * (0.42f + clock.pulse.value * 0.04f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(scheme.primary.copy(alpha = 0.19f * halo), Color.Transparent),
                            radius = radius
                        ),
                        radius = radius
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = image,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (selected) -0.5.dp.toPx() else 0f
            }
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .size(width = 11.dp, height = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary))
                    )
            )
        }
    }
}

@Composable
private fun PortraitTopSheen(clock: PortraitMotionClock, modifier: Modifier) {
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
private fun PortraitBottomAccent(clock: PortraitMotionClock, modifier: Modifier) {
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
