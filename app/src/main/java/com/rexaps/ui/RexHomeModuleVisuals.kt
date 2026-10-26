package com.rexaps.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimal, purpose-drawn symbols for the home module tiles. Geometry stays stable across themes. */
@Composable
internal fun ProModuleGlyph(
    moduleId: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.primary.copy(alpha = 0.085f))
            .border(1.dp, colors.primary.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(size * 0.15f)) {
            drawProModuleSymbol(
                id = moduleId.lowercase(),
                main = colors.primary,
                accent = colors.tertiary,
                detail = colors.onSurface
            )
        }
    }
}

/** A subtle ambient wash plus a module-specific watermark. Runs only while this tile is composed. */
@Composable
internal fun ProModuleAmbientLayer(
    moduleId: String,
    modifier: Modifier = Modifier,
    intensity: Float = 0.10f
) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "moduleAmbient-${moduleId}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5200 + (moduleId.fold(0) { total, character -> total + character.code } % 1700),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moduleGlow-${moduleId}"
    )

    Canvas(modifier = modifier) {
        val radius = size.maxDimension * 0.74f
        // The same restrained glow has a different travel path for each module family:
        // audio/video sweeps horizontally, developer tools follow a branch-like diagonal,
        // monitors/network tools scan across the card, and mascot/AI modules drift vertically.
        val center = when (moduleId.lowercase()) {
            "rexmusic", "rextube", "rextok" -> Offset(
                x = size.width * (0.12f + phase * 0.76f),
                y = size.height * 0.30f
            )
            "rexgit", "rexcoder", "rexnux" -> Offset(
                x = size.width * (0.22f + phase * 0.50f),
                y = size.height * (0.14f + phase * 0.56f)
            )
            "rexmonitor", "rexwarp", "rexpanel" -> Offset(
                x = size.width * (0.14f + phase * 0.72f),
                y = size.height * (0.58f - phase * 0.22f)
            )
            "rexfox", "rexspacex", "rexai" -> Offset(
                x = size.width * (0.74f + phase * 0.08f),
                y = size.height * (0.12f + phase * 0.54f)
            )
            else -> Offset(
                x = size.width * (0.80f + phase * 0.07f),
                y = size.height * (0.16f + phase * 0.12f)
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.primary.copy(alpha = intensity),
                    colors.tertiary.copy(alpha = intensity * 0.34f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        // A low-contrast watermark gives each module a different visual fingerprint.
        drawProModuleSymbol(
            id = moduleId.lowercase(),
            main = colors.primary.copy(alpha = 0.075f),
            accent = colors.tertiary.copy(alpha = 0.065f),
            detail = colors.onSurface.copy(alpha = 0.045f)
        )
    }
}

private fun DrawScope.drawProModuleSymbol(
    id: String,
    main: Color,
    accent: Color,
    detail: Color
) {
    val w = size.width
    val h = size.height
    val s = minOf(w, h)
    val stroke = (s * 0.065f).coerceAtLeast(1.6f)
    fun point(x: Float, y: Float) = Offset(w * x / 100f, h * y / 100f)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = main, width: Float = stroke) {
        drawLine(color, point(x1, y1), point(x2, y2), width)
    }
    fun dot(x: Float, y: Float, r: Float, color: Color = main, filled: Boolean = true) {
        drawCircle(
            color = color,
            radius = s * r / 100f,
            center = point(x, y),
            style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke)
        )
    }
    fun poly(points: List<Pair<Float, Float>>, color: Color = main, width: Float = stroke, filled: Boolean = false, close: Boolean = false) {
        if (points.isEmpty()) return
        val path = Path().apply {
            moveTo(point(points.first().first, points.first().second).x, point(points.first().first, points.first().second).y)
            points.drop(1).forEach { (x, y) ->
                val p = point(x, y)
                lineTo(p.x, p.y)
            }
            if (close) close()
        }
        drawPath(path, color, style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width))
    }
    fun frame(x: Float, y: Float, width: Float, height: Float, color: Color = main, radius: Float = 5f, lineWidth: Float = stroke) {
        drawRoundRect(
            color = color,
            topLeft = point(x, y),
            size = Size(w * width / 100f, h * height / 100f),
            cornerRadius = CornerRadius(s * radius / 100f),
            style = Stroke(lineWidth)
        )
    }

    when (id) {
        "rexfox" -> {
            poly(listOf(16f to 18f, 39f to 31f, 50f to 25f, 61f to 31f, 84f to 18f, 77f to 60f, 62f to 80f, 50f to 88f, 38f to 80f, 23f to 60f), main, stroke, close = true)
            poly(listOf(29f to 31f, 40f to 39f, 38f to 52f), accent, stroke * 0.85f)
            poly(listOf(71f to 31f, 60f to 39f, 62f to 52f), accent, stroke * 0.85f)
            poly(listOf(37f to 61f, 50f to 71f, 63f to 61f, 57f to 76f, 50f to 81f, 43f to 76f), main, stroke * 0.8f)
            dot(39f, 52f, 2.5f, detail)
            dot(61f, 52f, 2.5f, detail)
        }
        "rexgit" -> {
            line(28f, 18f, 28f, 82f, main, stroke * 1.1f)
            line(28f, 37f, 68f, 37f, accent, stroke * 1.1f)
            line(68f, 37f, 68f, 68f, main, stroke * 1.1f)
            line(28f, 62f, 48f, 62f, accent, stroke * 1.1f)
            dot(28f, 20f, 5f, accent); dot(28f, 37f, 5f, main)
            dot(28f, 62f, 5f, main); dot(28f, 81f, 5f, accent)
            dot(68f, 37f, 5f, main); dot(68f, 68f, 5f, accent)
        }
        "rexcoder" -> {
            poly(listOf(36f to 25f, 17f to 50f, 36f to 75f), main, stroke * 1.35f)
            poly(listOf(64f to 25f, 83f to 50f, 64f to 75f), main, stroke * 1.35f)
            line(58f, 19f, 42f, 81f, accent, stroke * 1.25f)
        }
        "rextools" -> {
            line(27f, 29f, 72f, 74f, main, stroke * 1.65f)
            line(70f, 27f, 28f, 72f, accent, stroke * 1.65f)
            dot(25f, 27f, 7f, main, false); dot(74f, 75f, 7f, main, false)
            dot(73f, 26f, 7f, accent, false); dot(27f, 73f, 7f, accent, false)
        }
        "rexmusic" -> {
            dot(50f, 50f, 34f, main, false)
            dot(50f, 50f, 22f, accent, false)
            dot(50f, 50f, 5f, main)
            val bars = listOf(18f, 31f, 48f, 35f, 54f, 26f, 42f)
            bars.forEachIndexed { index, height ->
                val x = 16f + index * 11f
                drawRoundRect(
                    color = if (index % 2 == 0) accent else main,
                    topLeft = point(x, 50f - height / 2f),
                    size = Size(w * 0.055f, h * height / 100f),
                    cornerRadius = CornerRadius(s * 0.025f)
                )
            }
        }
        "rexmonitor" -> {
            frame(12f, 17f, 76f, 57f, main, 6f, stroke)
            poly(listOf(19f to 53f, 33f to 43f, 44f to 57f, 60f to 32f, 80f to 43f), accent, stroke * 1.15f)
            line(50f, 75f, 50f, 84f, main)
            line(35f, 85f, 65f, 85f, main)
        }
        "rexspacex" -> {
            poly(listOf(50f to 13f, 65f to 39f, 62f to 64f, 50f to 80f, 38f to 64f, 35f to 39f), main, stroke * 1.2f, close = true)
            dot(50f, 43f, 6f, accent, false)
            poly(listOf(37f to 57f, 20f to 69f, 33f to 68f, 42f to 63f), accent, stroke, close = true)
            poly(listOf(63f to 57f, 80f to 69f, 67f to 68f, 58f to 63f), accent, stroke, close = true)
        }
        "rexmanager" -> {
            poly(listOf(15f to 31f, 15f to 23f, 41f to 23f, 49f to 31f, 85f to 31f, 85f to 76f, 15f to 76f), main, stroke, close = true)
            line(25f, 45f, 70f, 45f, accent)
            line(25f, 57f, 62f, 57f, accent)
            line(25f, 67f, 54f, 67f, main)
        }
        "rexpanel" -> {
            listOf(31f, 50f, 69f).forEachIndexed { index, y ->
                line(18f, y, 82f, y, main, stroke * 0.8f)
                dot(listOf(40f, 65f, 49f)[index], y, 5.5f, accent)
            }
        }
        "rexchat" -> {
            poly(listOf(20f to 23f, 80f to 23f, 80f to 64f, 59f to 64f, 42f to 80f, 42f to 64f, 20f to 64f), main, stroke, close = true)
            dot(37f, 44f, 3f, accent); dot(50f, 44f, 3f, accent); dot(63f, 44f, 3f, accent)
        }
        "rexnux" -> {
            frame(13f, 20f, 74f, 60f, main, 5f, stroke)
            poly(listOf(27f to 40f, 40f to 50f, 27f to 60f), accent, stroke * 1.2f)
            line(47f, 61f, 69f, 61f, main, stroke * 1.2f)
        }
        "rexwarp" -> {
            dot(25f, 51f, 5f, main); dot(50f, 30f, 5f, accent); dot(75f, 49f, 5f, main); dot(53f, 75f, 5f, accent)
            line(25f, 51f, 50f, 30f, main); line(50f, 30f, 75f, 49f, accent)
            line(75f, 49f, 53f, 75f, main); line(53f, 75f, 25f, 51f, accent)
        }
        "rexcalc" -> {
            frame(25f, 12f, 50f, 76f, main, 6f, stroke)
            frame(33f, 22f, 34f, 14f, accent, 2f, stroke * 0.8f)
            listOf(39f to 47f, 61f to 47f, 39f to 65f, 61f to 65f).forEach { dot(it.first, it.second, 3.5f, main) }
        }
        "rexalbum" -> {
            frame(19f, 25f, 62f, 56f, main, 5f, stroke)
            poly(listOf(25f to 68f, 40f to 49f, 51f to 61f, 61f to 51f, 75f to 68f), accent, stroke, close = false)
            dot(63f, 39f, 4f, accent)
        }
        "rextube" -> {
            frame(12f, 23f, 76f, 54f, main, 9f, stroke)
            poly(listOf(43f to 36f, 43f to 64f, 65f to 50f), accent, stroke, filled = true, close = true)
        }
        "rextok" -> {
            frame(29f, 12f, 42f, 76f, main, 8f, stroke)
            line(45f, 27f, 56f, 27f, accent, stroke * 1.2f)
            poly(listOf(45f to 43f, 45f to 65f, 63f to 55f), accent, stroke, filled = true, close = true)
            dot(50f, 77f, 2.5f, main)
        }
        "rexai" -> {
            dot(50f, 50f, 12f, main, false)
            val nodes = listOf(50f to 17f, 78f to 32f, 78f to 67f, 50f to 83f, 22f to 67f, 22f to 32f)
            nodes.forEach { (x, y) -> line(50f, 50f, x, y, accent, stroke * 0.8f); dot(x, y, 3.8f, main) }
            dot(50f, 50f, 4f, accent)
        }
        else -> {
            frame(22f, 22f, 56f, 56f, main, 7f, stroke)
            line(34f, 50f, 66f, 50f, accent, stroke * 1.2f)
            line(50f, 34f, 50f, 66f, main, stroke * 1.2f)
        }
    }
}
