package com.rexaps.rextools.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/** Latar: dua cahaya aksen yang melayang pelan + grid tipis. Animasi dibaca di fase draw (tanpa recomposition). */
@Composable
internal fun __splitImpl_RexBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
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

@Composable
internal fun __splitImpl_RexFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val container by animateColorAsState(
        if (selected) s.primary else s.surfaceContainerHigh.copy(alpha = 0.6f), tween(260), label = "bg"
    )
    val content by animateColorAsState(if (selected) s.onPrimary else s.onSurfaceVariant, tween(260), label = "fg")
    val borderColor by animateColorAsState(if (selected) Color.Transparent else s.outlineVariant, tween(260), label = "bd")
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.pressScale(source, 0.93f),
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = source
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
internal fun __splitImpl_RexPanel(
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

@Composable
internal fun __splitImpl_RexIconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 48.dp, pulse: Boolean = false) {
    val s = MaterialTheme.colorScheme
    val glow = rememberInfiniteTransition(label = "tile")
        .animateFloat(0.15f, 0.5f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    Box(
        modifier
            .size(size)
            .drawBehind {
                if (pulse) {
                    drawCircle(
                        Brush.radialGradient(listOf(s.primary.copy(alpha = glow.value), Color.Transparent)),
                        radius = this.size.minDimension * 0.95f
                    )
                }
            }
            .clip(RexSm)
            .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.24f), s.tertiary.copy(alpha = 0.14f))))
            .border(1.dp, Brush.linearGradient(listOf(s.primary.copy(alpha = 0.6f), s.tertiary.copy(alpha = 0.15f))), RexSm),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
internal fun __splitImpl_RexTheme(content: @Composable () -> Unit) {
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

@Composable
internal fun __splitImpl_RexImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val s = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val request = remember(model) { ImageRequest.Builder(ctx).data(model).crossfade(350).build() }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize().defaultMinSize(minHeight = 160.dp).background(s.surfaceContainerHigh).shimmer())
        },
        error = {
            Box(
                Modifier.fillMaxSize().defaultMinSize(minHeight = 120.dp).background(s.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BrokenImage, contentDescription = "Gagal memuat", tint = s.onSurfaceVariant)
            }
        }
    )
}
