package com.rexaps.rexmusic

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// ───────────────────────── Motion tokens ─────────────────────────

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val TwoPi = (2.0 * PI).toFloat()
private val DegToRad = (PI / 180.0).toFloat()

/** Seeded, stable particle coordinates prevent shimmer caused by regenerating randomness per frame. */
private data class AmbientParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float
)

private val LandscapeParticles = List(34) { index ->
    // A deterministic low-discrepancy layout gives the canvas texture without a Random on every draw.
    val x = ((index * 37 + 13) % 101) / 100f
    val y = ((index * 61 + 29) % 97) / 96f
    AmbientParticle(x, y, 0.7f + (index % 4) * 0.24f, index * 0.71f, 0.35f + (index % 5) * 0.13f)
}

/** Dua "jam" animasi bersama (lambat + denyut) agar hanya ada satu InfiniteTransition. */
@Stable
private class MotionClock(val slow: State<Float>, val pulse: State<Float>)

@Composable
private fun rememberReduceMotion(): Boolean {
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
private fun rememberMotionClock(reduceMotion: Boolean): MotionClock {
    return if (reduceMotion) {
        remember { MotionClock(mutableStateOf(0.18f), mutableStateOf(0.5f)) }
    } else {
        val transition = rememberInfiniteTransition(label = "rexLandscapeClock")
        val slow = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
            label = "rexClockSlow"
        )
        val pulse = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(7_000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "rexClockPulse"
        )
        remember(slow, pulse) { MotionClock(slow, pulse) }
    }
}

private data class DockItem(val tab: Int, val icon: ImageVector, val label: String)

// ───────────────────────── Root ─────────────────────────

/**
 * Landscape shell futuristik: floating navigation dock dengan indikator animasi,
 * ambient backdrop, content stage sinematik, dan floating audio dock.
 * Seluruh warna/typography berasal dari MaterialTheme; state & playback tetap milik parent.
 */
@Composable
internal fun RexMusicScreenLandscape(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = rememberReduceMotion()
    val clock = rememberMotionClock(reduceMotion)
    val railVisible = selectedTab != TAB_SYNC

    BoxWithConstraints(Modifier.fillMaxSize().background(scheme.background)) {
        val showNavigationLabels = maxWidth >= 900.dp
        val compactHeight = maxHeight < 420.dp
        val railWidth = if (showNavigationLabels) 104.dp else 80.dp

        AmbientBackdrop(clock, Modifier.fillMaxSize())

        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = railVisible,
                enter = fadeIn(tween(240)) +
                    slideInHorizontally(tween(340, easing = EmphasizedDecelerate)) { -it / 2 } +
                    expandHorizontally(tween(340, easing = EmphasizedDecelerate), expandFrom = Alignment.Start),
                exit = fadeOut(tween(140)) +
                    slideOutHorizontally(tween(220, easing = EmphasizedAccelerate)) { -it / 3 } +
                    shrinkHorizontally(tween(240, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Start)
            ) {
                NavigationDock(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    showLabels = showNavigationLabels,
                    compactHeight = compactHeight,
                    width = railWidth,
                    clock = clock
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = Color.Transparent,
                snackbarHost = snackbarHost,
                bottomBar = {
                    AnimatedVisibility(
                        visible = railVisible,
                        enter = fadeIn(tween(260)) +
                            slideInVertically(tween(360, easing = EmphasizedDecelerate)) { it / 2 } +
                            expandVertically(tween(320, easing = EmphasizedDecelerate), expandFrom = Alignment.Bottom) +
                            scaleIn(tween(320, easing = EmphasizedDecelerate), initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 1f)),
                        exit = fadeOut(tween(150)) +
                            slideOutVertically(tween(220, easing = EmphasizedAccelerate)) { it / 2 } +
                            shrinkVertically(tween(240, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Bottom) +
                            scaleOut(tween(220, easing = EmphasizedAccelerate), targetScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 1f))
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .widthIn(max = 1000.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                FloatingAudioDock(clock) { miniPlayer() }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ContentStage(
                        selectedTab = selectedTab,
                        reduceMotion = reduceMotion,
                        content = content
                    )
                }
            }
        }
    }
}

// ───────────────────────── Content stage ─────────────────────────

/**
 * Transisi tab tanpa menduplikasi / mereset content: `content` hanya dipanggil SEKALI,
 * efek sinematik (fade + slide + scale halus) dilakukan via graphicsLayer pada container.
 */
@Composable
private fun ContentStage(
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
private fun AmbientBackdrop(clock: MotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val slow = clock.slow.value
        val pulse = clock.pulse.value
        val a = slow * TwoPi
        val big = max(w, h)

        // Ambient glows yang melayang pelan
        val c1 = Offset(w * (0.16f + 0.05f * sin(a)), h * (0.18f + 0.06f * cos(a)))
        val r1 = big * (0.52f + 0.03f * pulse)
        drawCircle(
            brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.14f), Color.Transparent), center = c1, radius = r1),
            radius = r1,
            center = c1
        )
        val c2 = Offset(w * (0.86f + 0.04f * cos(a)), h * (0.84f + 0.05f * sin(a)))
        val r2 = big * (0.46f + 0.03f * (1f - pulse))
        drawCircle(
            brush = Brush.radialGradient(listOf(tertiary.copy(alpha = 0.11f), Color.Transparent), center = c2, radius = r2),
            radius = r2,
            center = c2
        )
        val c3 = Offset(w * (0.55f + 0.03f * sin(a + 1.7f)), h * 0.5f)
        val r3 = big * 0.28f
        drawCircle(
            brush = Brush.radialGradient(listOf(secondary.copy(alpha = 0.06f), Color.Transparent), center = c3, radius = r3),
            radius = r3,
            center = c3
        )

        // Dekorasi geometris: cincin konsentris + busur berputar + titik orbit
        val ringCenter = Offset(w * 0.84f, h * 0.26f)
        val hairline = Stroke(width = 1.dp.toPx())
        drawCircle(primary.copy(alpha = 0.06f), radius = h * 0.22f, center = ringCenter, style = hairline)
        drawCircle(primary.copy(alpha = 0.05f), radius = h * 0.34f, center = ringCenter, style = hairline)
        drawCircle(primary.copy(alpha = 0.035f), radius = h * 0.48f, center = ringCenter, style = hairline)

        val arcR = h * 0.34f
        val arcStart = slow * 720f
        drawArc(
            color = primary.copy(alpha = 0.16f),
            startAngle = arcStart,
            sweepAngle = 64f,
            useCenter = false,
            topLeft = Offset(ringCenter.x - arcR, ringCenter.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
        val dotAngle = (arcStart + 64f) * DegToRad
        drawCircle(
            color = tertiary.copy(alpha = 0.45f),
            radius = 2.5.dp.toPx(),
            center = Offset(ringCenter.x + arcR * cos(dotAngle), ringCenter.y + arcR * sin(dotAngle))
        )

        val ring2 = Offset(w * 0.1f, h * 0.92f)
        drawCircle(tertiary.copy(alpha = 0.05f), radius = h * 0.18f, center = ring2, style = hairline)
        drawCircle(tertiary.copy(alpha = 0.035f), radius = h * 0.3f, center = ring2, style = hairline)

        // A barely visible drafting grid gives the interface depth without competing with song art.
        val gridStep = 42.dp.toPx()
        val gridColor = scheme.outlineVariant.copy(alpha = 0.045f)
        var gridX = (w % gridStep) / 2f
        while (gridX < w) {
            drawLine(gridColor, Offset(gridX, 0f), Offset(gridX, h), 0.55.dp.toPx())
            gridX += gridStep
        }
        var gridY = (h % gridStep) / 2f
        while (gridY < h) {
            drawLine(gridColor, Offset(0f, gridY), Offset(w, gridY), 0.55.dp.toPx())
            gridY += gridStep
        }

        // Slow dust particles are intentionally dim; motion remains fluid and palette-aware.
        LandscapeParticles.forEach { particle ->
            val drift = slow * TwoPi * particle.speed + particle.phase
            val x = w * particle.x + sin(drift) * w * 0.009f
            val y = h * particle.y + cos(drift * 0.73f) * h * 0.012f
            val alpha = (0.035f + 0.055f * ((sin(drift) + 1f) / 2f)) * (0.7f + pulse * 0.3f)
            drawCircle(
                color = if (particle.phase.toInt() % 2 == 0) primary.copy(alpha = alpha)
                else tertiary.copy(alpha = alpha * 0.8f),
                radius = particle.radius.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // A discreet audio-spectrum signature lives near the lower edge of the backdrop.
        val spectrumBase = h * 0.94f
        val spectrumWidth = w * 0.34f
        val spectrumStart = w * 0.57f
        val bars = 48
        val barGap = 3.dp.toPx()
        val barWidth = (spectrumWidth - barGap * (bars - 1)) / bars
        for (index in 0 until bars) {
            val envelope = sin((index.toFloat() / bars) * PI.toFloat()).toFloat()
            val wave = abs(sin(index * 0.39f + slow * TwoPi * 1.5f))
            val barHeight = (2.5f + envelope * (5f + 13f * wave) * (0.45f + pulse * 0.55f)).dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.13f), tertiary.copy(alpha = 0.035f))
                ),
                topLeft = Offset(spectrumStart + index * (barWidth + barGap), spectrumBase - barHeight),
                size = Size(barWidth.coerceAtLeast(1f), barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

// ───────────────────────── Navigation dock ─────────────────────────

@Composable
private fun NavigationDock(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showLabels: Boolean,
    compactHeight: Boolean,
    width: Dp,
    clock: MotionClock
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val items = remember {
        listOf(
            DockItem(TAB_HOME, Icons.Default.Home, "Beranda"),
            DockItem(TAB_SEARCH, Icons.Default.Search, "Cari"),
            DockItem(TAB_PLAYLIST, Icons.Default.QueueMusic, "Playlist"),
            DockItem(TAB_LIBRARY, Icons.Default.LibraryMusic, "Koleksi"),
            DockItem(TAB_SYNC, Icons.Default.Groups, "Sync")
        )
    }
    val itemHeight = if (compactHeight) 52.dp else 62.dp
    val gap = 6.dp
    val density = LocalDensity.current
    val selectedIndex = items.indexOfFirst { it.tab == selectedTab }
    val indicatorTarget = with(density) { (itemHeight + gap).toPx() } * selectedIndex.coerceAtLeast(0)
    val indicatorY by animateFloatAsState(
        targetValue = indicatorTarget,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "dockIndicatorY"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = tween(200),
        label = "dockIndicatorAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
            )
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
            .width(width)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = scheme.primary.copy(alpha = 0.18f),
                spotColor = scheme.primary.copy(alpha = 0.24f)
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surface.copy(alpha = 0.93f),
                        scheme.surfaceVariant.copy(alpha = 0.62f)
                    )
                ),
                shape
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.42f),
                        scheme.outlineVariant.copy(alpha = 0.18f),
                        scheme.tertiary.copy(alpha = 0.34f)
                    )
                ),
                shape
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(if (compactHeight) 12.dp else 18.dp))
        RexBadge(size = if (compactHeight) 34.dp else 42.dp, compact = compactHeight, clock = clock)
        if (!compactHeight) {
            Text(
                "Rex",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Box(
            Modifier
                .padding(vertical = if (compactHeight) 8.dp else 12.dp)
                .width(26.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, scheme.outlineVariant.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                DockIndicator(
                    height = itemHeight,
                    clock = clock,
                    modifier = Modifier.graphicsLayer {
                        translationY = indicatorY
                        alpha = indicatorAlpha
                    }
                )
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    items.forEach { item ->
                        DockItemView(
                            item = item,
                            selected = item.tab == selectedTab,
                            showLabel = showLabels,
                            height = itemHeight,
                            onClick = { onTabSelected(item.tab) }
                        )
                    }
                }
            }
        }
        if (!compactHeight) {
            Spacer(Modifier.height(7.dp))
            DockStatusSignal(clock)
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DockStatusSignal(clock: MotionClock) {
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
private fun DockIndicator(height: Dp, clock: MotionClock, modifier: Modifier) {
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
private fun DockItemView(
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

@Composable
private fun RexBadge(size: Dp, compact: Boolean, clock: MotionClock) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    val ringBrush = remember(primary, tertiary) {
        Brush.sweepGradient(
            listOf(Color.Transparent, primary.copy(alpha = 0.9f), tertiary.copy(alpha = 0.9f), Color.Transparent)
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                val base = this.size.minDimension
                drawCircle(primary.copy(alpha = 0.08f + 0.08f * clock.pulse.value), radius = base * 0.85f)
                rotate(clock.slow.value * 360f * 3f) {
                    drawCircle(
                        brush = ringBrush,
                        radius = base / 2f + 4.dp.toPx(),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(Brush.linearGradient(listOf(primary, tertiary))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "R",
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = scheme.onPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

// ───────────────────────── Floating audio dock ─────────────────────────

@Composable
private fun FloatingAudioDock(clock: MotionClock, miniPlayer: @Composable () -> Unit) {
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