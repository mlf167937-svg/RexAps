package com.rexaps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rexaps.ui.theme.RexThemeMaterial
import com.rexaps.ui.theme.RexThemeRegistry
import com.rexaps.ui.theme.RexThemeVisuals
import com.rexaps.ui.theme.RexThemeVisualSpec

/** Purpose-drawn module artwork. IDs choose a visual grammar; theme tokens control its material. */
@Composable
internal fun currentThemeVisualSpec(): RexThemeVisualSpec {
    val primary = MaterialTheme.colorScheme.primary
    val activeId = remember(primary) {
        RexThemeRegistry.all.firstOrNull { Color(it.primary) == primary }?.id ?: "dark_blue_moon"
    }
    return remember(activeId) { RexThemeVisuals.forTheme(activeId) }
}

@Composable
internal fun ModuleArtwork(moduleId: String, size: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val visual = currentThemeVisualSpec()
    val shape = RoundedCornerShape(size * .30f)
    val glass = visual.material == RexThemeMaterial.GLASS
    Box(
        modifier = modifier.size(size)
            .shadow(
                elevation = if (glass) 8.dp else 12.dp,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = if (glass) .20f else .30f),
                spotColor = colors.primary.copy(alpha = if (glass) .18f else .24f)
            )
            .clip(shape)
            .background(colors.surface.copy(alpha = if (glass) visual.surfaceAlpha else 1f))
            .border(
                width = 1.dp,
                color = if (glass) Color.White.copy(alpha = .28f) else colors.primary.copy(alpha = .55f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!glass) {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(colors.primary.copy(alpha = .86f), colors.tertiary.copy(alpha = .62f)))))
        }
        RexThemeMotifOverlay(visual, colors.primary, colors.tertiary, Modifier.matchParentSize().alpha(if (glass) .85f else .45f))
        Canvas(Modifier.fillMaxSize(.80f)) {
            drawModuleGlyph(moduleId.lowercase(), if (glass) colors.onSurface else colors.onPrimary, if (glass) colors.primary else Color.White.copy(alpha = .90f), colors.tertiary)
        }
        // Glass gets an explicit specular streak instead of a solid-color badge.
        if (glass) {
            Box(
                Modifier.align(Alignment.TopStart).fillMaxSize(.94f).alpha((visual.highlightAlpha + .10f).coerceIn(0f, .70f))
                    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = visual.highlightAlpha), Color.Transparent, Color.Transparent)))
            )
        }
    }
}

private fun DrawScope.drawModuleGlyph(id: String, main: Color, light: Color, accent: Color) {
    val w = size.width
    val h = size.height
    val s = minOf(w, h)
    val stroke = (s * .055f).coerceAtLeast(1.8f)
    fun p(x: Float, y: Float) = Offset(w * x / 100f, h * y / 100f)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = main, width: Float = stroke) =
        drawLine(color, p(x1, y1), p(x2, y2), width)
    fun circle(x: Float, y: Float, r: Float, color: Color = main, filled: Boolean = true, width: Float = stroke) =
        drawCircle(color, s * r / 100f, p(x, y), style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width))
    fun path(points: List<Pair<Float, Float>>, color: Color = main, filled: Boolean = false, width: Float = stroke, close: Boolean = false) {
        val shape = Path().apply {
            if (points.isNotEmpty()) moveTo(p(points.first().first, points.first().second).x, p(points.first().first, points.first().second).y)
            points.drop(1).forEach { (x, y) -> lineTo(p(x, y).x, p(x, y).y) }
            if (close) close()
        }
        drawPath(shape, color, style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width))
    }
    when (id) {
        "rexfox" -> {
            path(listOf(18f to 20f, 40f to 32f, 50f to 24f, 60f to 32f, 82f to 20f, 76f to 61f, 62f to 79f, 50f to 84f, 38f to 79f, 24f to 61f), main, true, close = true)
            path(listOf(26f to 28f, 40f to 37f, 37f to 53f), light, false, stroke * .75f)
            path(listOf(74f to 28f, 60f to 37f, 63f to 53f), accent, false, stroke * .75f)
            path(listOf(37f to 59f, 50f to 70f, 63f to 59f, 57f to 76f, 50f to 80f, 43f to 76f), light, true, close = true)
            circle(39f, 52f, 2.5f, light); circle(61f, 52f, 2.5f, light)
        }
        "rexgit" -> {
            line(30f, 20f, 30f, 80f, light, stroke * 1.2f)
            line(30f, 40f, 65f, 40f, main, stroke * 1.2f)
            line(65f, 40f, 65f, 67f, accent, stroke * 1.2f)
            line(30f, 64f, 48f, 64f, main, stroke * 1.2f)
            listOf(20f to 20f, 30f to 40f, 30f to 64f, 30f to 80f, 65f to 40f, 65f to 67f).forEachIndexed { i, (x, y) -> circle(x, y, if (i == 1) 7f else 5f, if (i % 2 == 0) accent else light) }
        }
        "rexmusic" -> {
            circle(50f, 50f, 37f, main, false, stroke * .9f); circle(50f, 50f, 27f, light.copy(alpha = .8f), false, stroke * .75f)
            circle(50f, 50f, 7f, accent); circle(50f, 50f, 2.5f, main)
            val heights = listOf(20f, 37f, 52f, 32f, 44f, 26f)
            heights.forEachIndexed { i, bar ->
                val x = 20f + i * 12f
                drawRoundRect(accent.copy(alpha = if (i % 2 == 0) 1f else .72f), p(x, 50f - bar / 2), Size(w * .065f, h * bar / 100f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .03f))
            }
        }
        "rexcoder" -> {
            path(listOf(35f to 25f, 15f to 50f, 35f to 75f), main, false, stroke * 1.5f)
            path(listOf(65f to 25f, 85f to 50f, 65f to 75f), main, false, stroke * 1.5f)
            line(58f, 18f, 42f, 82f, accent, stroke * 1.6f)
            line(43f, 82f, 58f, 18f, light.copy(alpha = .55f), stroke * .7f)
        }
        "rextools" -> {
            listOf(32f to 32f, 68f to 32f, 32f to 68f, 68f to 68f).forEachIndexed { i, (x, y) ->
                drawRoundRect(if (i % 2 == 0) main else accent, p(x - 13f, y - 13f), Size(w * .26f, h * .26f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .06f), style = Stroke(stroke * 1.2f))
            }
            circle(50f, 50f, 8f, light)
        }
        "rexmonitor" -> {
            drawRoundRect(main, p(12f, 18f), Size(w * .76f, h * .58f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .07f), style = Stroke(stroke * 1.25f))
            path(listOf(20f to 55f, 34f to 45f, 46f to 58f, 60f to 34f, 78f to 42f), accent, false, stroke * 1.35f)
            line(50f, 76f, 50f, 84f, light); line(36f, 84f, 64f, 84f, light)
        }
        "rexspacex" -> {
            path(listOf(50f to 12f, 67f to 37f, 64f to 61f, 50f to 79f, 36f to 61f, 33f to 37f), main, false, stroke * 1.3f, close = true)
            circle(50f, 43f, 6f, light, false, stroke)
            path(listOf(36f to 54f, 23f to 65f, 31f to 66f, 39f to 61f), accent, true, close = true)
            path(listOf(64f to 54f, 77f to 65f, 69f to 66f, 61f to 61f), accent, true, close = true)
            path(listOf(44f to 78f, 50f to 91f, 56f to 78f), light, true, close = true)
        }
        "rexmanager", "rexalbum" -> {
            drawRoundRect(main, p(13f, 26f), Size(w * .74f, h * .55f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .07f), style = Stroke(stroke * 1.25f))
            path(listOf(14f to 28f, 14f to 20f, 43f to 20f, 49f to 28f), accent, false, stroke * 1.25f)
            if (id == "rexalbum") { circle(62f, 47f, 13f, accent, false, stroke); circle(62f, 47f, 4f, light) }
            else { line(26f, 43f, 60f, 43f, light); line(26f, 55f, 51f, 55f, light); line(26f, 67f, 66f, 67f, accent) }
        }
        "rexpanel", "rexwarp" -> {
            if (id == "rexwarp") {
                val cloud = Path().apply { moveTo(w * .23f, h * .67f); cubicTo(w * .05f, h * .64f, w * .14f, h * .39f, w * .33f, h * .42f); cubicTo(w * .40f, h * .15f, w * .74f, h * .22f, w * .75f, h * .44f); cubicTo(w * .97f, h * .42f, w * .98f, h * .68f, w * .77f, h * .70f); close() }
                drawPath(cloud, main, style = Stroke(stroke * 1.4f)); line(25f, 54f, 44f, 54f, accent); line(56f, 54f, 75f, 54f, accent)
            } else {
                repeat(3) { i -> val y = 29f + i * 21f; drawRoundRect(main, p(20f, y), Size(w * .60f, h * .12f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .05f), style = Stroke(stroke)); circle(33f + i * 13f, y + 6f, 2.5f, accent) }
            }
        }
        "rexchat" -> {
            val bubble = Path().apply { moveTo(w * .18f, h * .22f); quadraticTo(w * .18f, h * .14f, w * .28f, h * .14f); lineTo(w * .73f, h * .14f); quadraticTo(w * .83f, h * .14f, w * .83f, h * .25f); lineTo(w * .83f, h * .60f); quadraticTo(w * .83f, h * .70f, w * .72f, h * .70f); lineTo(w * .42f, h * .70f); lineTo(w * .27f, h * .84f); lineTo(w * .29f, h * .70f); quadraticTo(w * .18f, h * .70f, w * .18f, h * .60f); close() }
            drawPath(bubble, main, style = Stroke(stroke * 1.4f)); listOf(38f, 50f, 62f).forEach { circle(it, 43f, 3f, accent) }
        }
        "rexnux" -> {
            path(listOf(39f to 26f, 20f to 50f, 39f to 73f), accent, false, stroke * 1.6f)
            line(46f, 72f, 62f, 28f, main, stroke * 1.6f)
            line(65f, 73f, 82f, 73f, light, stroke * 1.6f)
            line(65f, 82f, 82f, 82f, light, stroke * 1.6f)
        }
        "rextube", "rextok" -> {
            val shape = Path().apply { moveTo(w * .25f, h * .16f); quadraticTo(w * .15f, h * .16f, w * .15f, h * .28f); lineTo(w * .15f, h * .72f); quadraticTo(w * .15f, h * .84f, w * .27f, h * .84f); lineTo(w * .73f, h * .84f); quadraticTo(w * .85f, h * .84f, w * .85f, h * .72f); lineTo(w * .85f, h * .28f); quadraticTo(w * .85f, h * .16f, w * .73f, h * .16f); close() }
            drawPath(shape, main, style = Stroke(stroke * 1.4f))
            path(listOf(43f to 35f, 43f to 65f, 66f to 50f), accent, true, close = true)
            if (id == "rextok") { circle(24f, 23f, 4f, light); circle(76f, 77f, 4f, light) }
        }
        "rexcalc" -> {
            drawRoundRect(main, p(22f, 12f), Size(w * .56f, h * .76f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .08f), style = Stroke(stroke * 1.35f))
            drawRoundRect(accent, p(31f, 22f), Size(w * .38f, h * .14f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .025f))
            listOf(39f to 49f, 60f to 49f, 39f to 67f, 60f to 67f).forEach { (x, y) -> circle(x, y, 4f, light) }
        }
        "rexai" -> {
            circle(50f, 50f, 15f, main, false, stroke * 1.3f)
            val nodes = listOf(50f to 15f, 78f to 30f, 82f to 62f, 50f to 84f, 20f to 63f, 22f to 30f)
            nodes.forEach { (x, y) -> line(50f, 50f, x, y, accent, stroke * .7f); circle(x, y, 4.5f, light) }
            circle(50f, 50f, 5f, accent)
        }
        else -> {
            // Distinct fallback glyph if a future module has no dedicated grammar yet.
            circle(50f, 50f, 30f, main, false, stroke * 1.4f)
            line(25f, 50f, 75f, 50f, accent, stroke * 1.4f)
            line(50f, 25f, 50f, 75f, light, stroke * 1.4f)
        }
    }
}
