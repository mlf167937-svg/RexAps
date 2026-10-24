package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun hairline(alpha: Float = 0.7f) =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))

/* ═════════════════════ THEME (opsional) ═════════════════════ */

@Composable
fun RexTheme(content: @Composable () -> Unit) {
    // Explicitly forward all parent tokens: MaterialTheme's default colorScheme is
    // lightColorScheme(), which previously silently reset the app palette in RexTools.
    val inheritedColors = MaterialTheme.colorScheme
    val inheritedShapes = MaterialTheme.shapes
    val baseTypography = MaterialTheme.typography
    val rexTypography = baseTypography.copy(
        displaySmall = baseTypography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
        headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        labelSmall = baseTypography.labelSmall.copy(letterSpacing = 0.8.sp)
    )
    MaterialTheme(
        colorScheme = inheritedColors,
        typography = rexTypography,
        shapes = inheritedShapes,
        content = content
    )
}

/* ═════════════════════ BACKGROUND & SCREEN ═════════════════════ */

/** Latar: dua cahaya aksen yang melayang pelan + grid tipis. Animasi dibaca di fase draw (tanpa recomposition). */
@Composable
fun RexBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val s = MaterialTheme.colorScheme
    val p = rememberInfiniteTransition(label = "aurora")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val grid = s.onSurface.copy(alpha = 0.03f)
    Box(
        modifier
            .fillMaxSize()
            .background(s.background)
            .drawBehind {
                val w = size.width
                val h = size.height
                val v = p.value
                drawRect(
                    Brush.radialGradient(
                        listOf(s.primary.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(w * (0.95f - 0.35f * v), h * (0.02f + 0.10f * v)),
                        radius = w * 1.05f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(s.tertiary.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w * (0.05f + 0.30f * v), h * (0.82f - 0.12f * v)),
                        radius = w * 0.95f
                    )
                )
                val step = 36.dp.toPx()
                var gx = 0f
                while (gx < w) { drawLine(grid, Offset(gx, 0f), Offset(gx, h), 1f); gx += step }
                var gy = 0f
                while (gy < h) { drawLine(grid, Offset(0f, gy), Offset(w, gy), 1f); gy += step }
            },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexScreen(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    scroll: Boolean = true,
    padded: Boolean = true,
    toast: RexToastState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scroll && scrollState.value > 8 } }
    val barColor by animateColorAsState(
        if (scrolled) s.background.copy(alpha = 0.88f) else Color.Transparent, tween(250), label = "bar"
    )
    val lineAlpha by animateFloatAsState(if (scrolled) 0.7f else 0f, tween(250), label = "line")

    RexBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            AnimatedContent(
                                targetState = title to subtitle,
                                transitionSpec = {
                                    (fadeIn(tween(240, delayMillis = 80)) + slideInVertically(tween(300, easing = RexEase)) { it / 3 }) togetherWith
                                        fadeOut(tween(120))
                                },
                                label = "title"
                            ) { (t, sub) ->
                                Column {
                                    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (sub != null) {
                                        Text(sub, style = MaterialTheme.typography.labelMedium, color = s.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        navigationIcon = { RexBackButton(onBack) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barColor,
                            scrolledContainerColor = barColor
                        )
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(s.outlineVariant.copy(alpha = lineAlpha)))
                }
            }
        ) { pad ->
            var m = Modifier.fillMaxSize().padding(pad)
            if (scroll) m = m.verticalScroll(scrollState)
            m = m.entrance(0, offset = 16.dp)
            if (padded) m = m.padding(horizontal = 20.dp, vertical = 8.dp)
            Column(
                modifier = m,
                verticalArrangement = if (padded) Arrangement.spacedBy(16.dp) else Arrangement.Top,
                content = content
            )
        }
        if (toast != null) RexToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }
}

/* ═════════════════════ PANEL ═════════════════════ */

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
