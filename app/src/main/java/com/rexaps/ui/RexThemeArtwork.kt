package com.rexaps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.rexaps.ui.theme.RexThemeMotif
import com.rexaps.ui.theme.RexThemeVisualSpec

/** Low-cost themed decoration shared by theme previews, module cards and ambient backgrounds. */
@Composable
internal fun RexThemeMotifOverlay(
    spec: RexThemeVisualSpec,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        drawRexMotif(spec.motif, primary, secondary, spec.highlightAlpha)
    }
}

/** Canvas-only geometry keeps theme signatures cheap enough for lists and scrolling screens. */
internal fun DrawScope.drawRexMotif(
    motif: RexThemeMotif,
    primary: Color,
    secondary: Color,
    intensity: Float
) {
    val w = size.width
    val h = size.height
    val s = minOf(w, h)
    val line = (s * .012f).coerceAtLeast(1f)
    val faint = primary.copy(alpha = (.11f + intensity * .24f).coerceIn(.08f, .38f))
    val glow = secondary.copy(alpha = (.10f + intensity * .22f).coerceIn(.08f, .34f))

    fun polygon(points: List<Offset>, color: Color, filled: Boolean = false, stroke: Float = line) {
        if (points.isEmpty()) return
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            if (filled) close()
        }
        drawPath(path, color, style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke))
    }

    when (motif) {
        RexThemeMotif.ORBIT -> {
            val c = Offset(w * .84f, h * .22f)
            drawCircle(faint, s * .34f, c, style = Stroke(line))
            drawCircle(glow, s * .23f, c, style = Stroke(line * 1.6f))
            rotate(-24f, c) { drawOval(faint, Offset(c.x - s * .42f, c.y - s * .13f), Size(s * .84f, s * .26f), style = Stroke(line * 1.25f)) }
            drawCircle(glow, s * .025f, Offset(c.x + s * .30f, c.y - s * .12f))
        }
        RexThemeMotif.BLUE_RAY -> {
            repeat(5) { i ->
                val x = w * (.54f + i * .105f)
                drawLine(faint, Offset(x, h * .02f), Offset(x - s * .16f, h * .70f), line * if (i == 2) 1.8f else .8f)
            }
            drawCircle(glow, s * .24f, Offset(w * .82f, h * .23f), style = Stroke(line * 1.8f))
            drawLine(glow, Offset(w * .55f, h * .18f), Offset(w * .98f, h * .18f), line)
        }
        RexThemeMotif.SILVER_ARC -> {
            val box = Size(s * .74f, s * .74f)
            val topLeft = Offset(w * .60f, h * .03f)
            drawArc(faint, 205f, 240f, false, topLeft, box, style = Stroke(line * 1.6f))
            drawArc(glow, 215f, 205f, false, Offset(topLeft.x + s * .08f, topLeft.y + s * .08f), Size(s * .58f, s * .58f), style = Stroke(line))
            drawLine(faint, Offset(w * .58f, h * .74f), Offset(w * .98f, h * .36f), line)
        }
        RexThemeMotif.EMBER -> {
            drawCircle(glow.copy(alpha = glow.alpha * .62f), s * .30f, Offset(w * .84f, h * .82f))
            repeat(7) { i ->
                val x = w * (.60f + (i % 4) * .095f)
                val y = h * (.62f - (i / 4) * .18f)
                drawCircle(if (i % 2 == 0) faint else glow, s * if (i % 3 == 0) .018f else .010f, Offset(x, y))
            }
            drawLine(faint, Offset(w * .73f, h * .96f), Offset(w * .84f, h * .52f), line * 1.2f)
        }
        RexThemeMotif.RED_BLADE -> {
            polygon(listOf(Offset(w * .63f, h * .02f), Offset(w * .76f, h * .02f), Offset(w * .49f, h * .70f), Offset(w * .43f, h * .70f)), faint, true)
            polygon(listOf(Offset(w * .82f, h * .15f), Offset(w * .91f, h * .15f), Offset(w * .72f, h * .59f), Offset(w * .67f, h * .59f)), glow, true)
            drawLine(Color.White.copy(alpha = .12f), Offset(w * .67f, h * .05f), Offset(w * .48f, h * .54f), line)
        }
        RexThemeMotif.MAGMA_FLOW -> {
            repeat(4) { i ->
                val y = h * (.48f + i * .12f)
                val path = Path().apply {
                    moveTo(w * .42f, y)
                    cubicTo(w * .52f, y - h * .19f, w * .62f, y + h * .17f, w * .78f, y - h * .03f)
                    cubicTo(w * .88f, y - h * .15f, w * .94f, y + h * .05f, w * 1.04f, y - h * .04f)
                }
                drawPath(path, if (i % 2 == 0) faint else glow, style = Stroke(line * (1f + i * .28f)))
            }
        }
        RexThemeMotif.LILAC_RIBBON -> {
            repeat(3) { i ->
                val path = Path().apply {
                    moveTo(w * .46f, h * (.18f + i * .13f))
                    cubicTo(w * .63f, h * (.02f + i * .13f), w * .76f, h * (.55f + i * .10f), w * 1.05f, h * (.32f + i * .13f))
                }
                drawPath(path, if (i % 2 == 0) faint else glow, style = Stroke(line * (i + 1)))
            }
        }
        RexThemeMotif.PURPLE_WAVE -> {
            repeat(4) { i ->
                val y = h * (.22f + i * .16f)
                val path = Path().apply {
                    moveTo(w * .46f, y)
                    cubicTo(w * .60f, y - h * .11f, w * .65f, y + h * .11f, w * .79f, y)
                    cubicTo(w * .89f, y - h * .08f, w * .92f, y + h * .07f, w * 1.02f, y - h * .01f)
                }
                drawPath(path, if (i % 2 == 0) faint else glow, style = Stroke(line * (1f + i * .2f)))
            }
        }
        RexThemeMotif.NEBULA -> {
            drawCircle(faint, s * .21f, Offset(w * .76f, h * .24f))
            drawCircle(glow.copy(alpha = glow.alpha * .70f), s * .28f, Offset(w * .91f, h * .34f))
            drawCircle(primary.copy(alpha = .08f), s * .33f, Offset(w * .76f, h * .24f), style = Stroke(line * 2f))
            repeat(6) { i -> drawCircle(if (i % 2 == 0) faint else glow, s * .012f, Offset(w * (.58f + (i % 3) * .12f), h * (.18f + (i / 3) * .23f))) }
        }
        RexThemeMotif.VOID_HALO -> {
            val c = Offset(w * .82f, h * .25f)
            drawCircle(faint, s * .36f, c, style = Stroke(line))
            drawCircle(glow, s * .28f, c, style = Stroke(line * 2f))
            drawCircle(faint, s * .15f, c, style = Stroke(line * 1.4f))
            drawCircle(secondary.copy(alpha = .06f), s * .12f, c)
        }
        RexThemeMotif.ONYX_RINGS -> {
            val c = Offset(w * .84f, h * .27f)
            repeat(4) { i ->
                rotate(i * 26f, c) {
                    drawOval(if (i % 2 == 0) faint else glow, Offset(c.x - s * (.19f + i * .045f), c.y - s * (.08f + i * .012f)), Size(s * (.38f + i * .09f), s * (.16f + i * .024f)), style = Stroke(line * (1f + i * .18f)))
                }
            }
        }
        RexThemeMotif.ECLIPSE -> {
            val c = Offset(w * .82f, h * .24f)
            drawCircle(primary.copy(alpha = .15f), s * .32f, c)
            drawCircle(faint, s * .32f, c, style = Stroke(line * 1.2f))
            drawCircle(Color.Black.copy(alpha = .20f), s * .25f, Offset(c.x + s * .12f, c.y - s * .06f))
            drawArc(glow, 205f, 140f, false, Offset(c.x - s * .40f, c.y - s * .40f), Size(s * .80f, s * .80f), style = Stroke(line * 1.5f))
        }
        RexThemeMotif.GLASS_REFRACTION -> {
            drawRect(Color.White.copy(alpha = .025f + intensity * .04f), Offset.Zero, Size(w, h))
            polygon(listOf(Offset(w * .56f, 0f), Offset(w * .69f, 0f), Offset(w * .30f, h), Offset(w * .17f, h)), Color.White.copy(alpha = .07f + intensity * .06f), true)
            drawLine(glow, Offset(w * .08f, h * .12f), Offset(w * .94f, h * .12f), line * 1.5f)
            drawLine(faint, Offset(w * .12f, h * .19f), Offset(w * .62f, h * .19f), line)
            drawLine(Color.White.copy(alpha = .10f), Offset(w * .46f, h), Offset(w * .90f, 0f), line)
        }
        RexThemeMotif.GLASS_ECLIPSE -> {
            val c = Offset(w * .78f, h * .34f)
            drawCircle(primary.copy(alpha = .15f), s * .26f, c)
            drawCircle(Color.White.copy(alpha = .10f), s * .26f, Offset(c.x - s * .06f, c.y - s * .04f), style = Stroke(line * 1.5f))
            drawCircle(glow, s * .18f, Offset(c.x + s * .13f, c.y - s * .02f), style = Stroke(line))
            drawLine(Color.White.copy(alpha = .12f), Offset(w * .10f, h * .10f), Offset(w * .48f, h * .10f), line * 1.5f)
        }
        RexThemeMotif.GLASS_PRISM -> {
            polygon(listOf(Offset(w * .70f, h * .08f), Offset(w * .95f, h * .49f), Offset(w * .71f, h * .88f), Offset(w * .49f, h * .49f)), primary.copy(alpha = .12f), true)
            polygon(listOf(Offset(w * .70f, h * .08f), Offset(w * .95f, h * .49f), Offset(w * .71f, h * .88f), Offset(w * .49f, h * .49f)), glow, false, line * 1.3f)
            drawLine(Color.White.copy(alpha = .16f), Offset(w * .70f, h * .08f), Offset(w * .71f, h * .88f), line)
            drawLine(faint, Offset(w * .49f, h * .49f), Offset(w * .95f, h * .49f), line)
        }
        RexThemeMotif.GLASS_VOID -> {
            val c = Offset(w * .82f, h * .28f)
            drawCircle(primary.copy(alpha = .08f), s * .34f, c)
            drawCircle(faint, s * .34f, c, style = Stroke(line))
            drawCircle(glow, s * .22f, c, style = Stroke(line * 1.8f))
            polygon(listOf(Offset(w * .54f, h * .02f), Offset(w * .62f, h * .02f), Offset(w * .94f, h * .58f), Offset(w * .86f, h * .58f)), Color.White.copy(alpha = .055f), true)
        }
        RexThemeMotif.GLASS_LEAF -> {
            val leaf = Path().apply {
                moveTo(w * .72f, h * .75f)
                quadraticTo(w * .51f, h * .38f, w * .92f, h * .27f)
                quadraticTo(w * .97f, h * .60f, w * .72f, h * .75f)
                close()
            }
            drawPath(leaf, primary.copy(alpha = .09f))
            drawPath(leaf, faint, style = Stroke(line * 1.8f))
            drawLine(glow, Offset(w * .73f, h * .72f), Offset(w * .86f, h * .40f), line)
            drawLine(faint, Offset(w * .78f, h * .57f), Offset(w * .69f, h * .49f), line * .8f)
        }
        RexThemeMotif.GLASS_FACET -> {
            polygon(listOf(Offset(w * .57f, h * .04f), Offset(w * .91f, h * .23f), Offset(w * .75f, h * .53f), Offset(w * .48f, h * .33f)), primary.copy(alpha = .10f), true)
            polygon(listOf(Offset(w * .48f, h * .33f), Offset(w * .75f, h * .53f), Offset(w * .91f, h * .23f)), glow, false, line)
            polygon(listOf(Offset(w * .48f, h * .33f), Offset(w * .75f, h * .53f), Offset(w * .61f, h * .84f), Offset(w * .36f, h * .66f)), faint.copy(alpha = .7f), true)
            drawLine(Color.White.copy(alpha = .14f), Offset(w * .57f, h * .04f), Offset(w * .36f, h * .66f), line)
        }
        RexThemeMotif.HOLLOW_RAY -> {
            polygon(listOf(Offset(w * .70f, h * .04f), Offset(w * .53f, h * .38f), Offset(w * .67f, h * .38f), Offset(w * .42f, h * .83f), Offset(w * .50f, h * .50f), Offset(w * .38f, h * .50f)), faint, false, line * 1.5f)
            drawCircle(glow, s * .28f, Offset(w * .82f, h * .24f), style = Stroke(line))
        }
        RexThemeMotif.GOLD_LIGHTNING -> {
            polygon(listOf(Offset(w * .78f, h * .02f), Offset(w * .59f, h * .39f), Offset(w * .73f, h * .39f), Offset(w * .43f, h * .92f), Offset(w * .53f, h * .52f), Offset(w * .39f, h * .52f)), glow, true)
            drawLine(Color.White.copy(alpha = .22f), Offset(w * .78f, h * .02f), Offset(w * .43f, h * .92f), line)
            drawCircle(faint, s * .26f, Offset(w * .82f, h * .22f), style = Stroke(line))
        }
        RexThemeMotif.PEARL_SHEEN -> {
            drawCircle(Color.White.copy(alpha = .12f), s * .30f, Offset(w * .22f, h * .14f))
            drawCircle(glow, s * .30f, Offset(w * .22f, h * .14f), style = Stroke(line))
            drawLine(faint, Offset.Zero, Offset(w, h * .70f), line)
            drawLine(Color.White.copy(alpha = .18f), Offset(w * .10f, h * .03f), Offset(w * .95f, h * .03f), line)
        }
        RexThemeMotif.SAKURA_PETAL -> {
            repeat(5) { i ->
                val x = w * (.62f + (i % 3) * .12f)
                val y = h * (.15f + (i / 3) * .23f)
                rotate(i * 23f, Offset(x, y)) { drawOval(glow, Offset(x - s * .035f, y - s * .07f), Size(s * .07f, s * .14f)) }
            }
        }
        RexThemeMotif.NEON_CIRCUIT -> {
            repeat(3) { i ->
                val y = h * (.24f + i * .22f)
                drawLine(faint, Offset(w * .52f, y), Offset(w * .92f, y), line * 1.5f)
                drawCircle(glow, s * .025f, Offset(w * (.58f + i * .12f), y))
                drawLine(faint, Offset(w * (.74f + i * .05f), y), Offset(w * (.74f + i * .05f), y + h * .09f), line)
            }
        }
        RexThemeMotif.NEON_MATRIX -> {
            repeat(5) { i ->
                val x = w * (.61f + i * .075f)
                drawLine(faint, Offset(x, h * .12f), Offset(x, h * .88f), line * .8f)
                repeat(3) { j -> drawCircle(if ((i + j) % 2 == 0) glow else faint, s * .018f, Offset(x, h * (.20f + j * .23f))) }
            }
        }
        RexThemeMotif.NEON_PULSE -> {
            val path = Path().apply {
                moveTo(w * .48f, h * .50f)
                lineTo(w * .60f, h * .50f)
                lineTo(w * .65f, h * .30f)
                lineTo(w * .71f, h * .70f)
                lineTo(w * .77f, h * .18f)
                lineTo(w * .83f, h * .60f)
                lineTo(w * .89f, h * .50f)
                lineTo(w * .98f, h * .50f)
            }
            drawPath(path, glow, style = Stroke(line * 1.8f))
            drawPath(path, faint.copy(alpha = .5f), style = Stroke(line * 4f))
        }
        RexThemeMotif.NEON_SUN -> {
            val c = Offset(w * .80f, h * .28f)
            drawCircle(faint, s * .17f, c, style = Stroke(line * 1.7f))
            drawCircle(glow, s * .11f, c, style = Stroke(line))
            repeat(10) { i ->
                rotate(i * 36f, c) { drawLine(faint, Offset(c.x, c.y - s * .22f), Offset(c.x, c.y - s * .29f), line * 1.2f) }
            }
        }
        RexThemeMotif.GALAXY -> {
            val c = Offset(w * .79f, h * .29f)
            repeat(3) { i ->
                val path = Path().apply {
                    moveTo(c.x - s * .10f, c.y + s * (.16f + i * .02f))
                    cubicTo(c.x - s * .32f, c.y - s * .15f, c.x + s * .06f, c.y - s * .37f, c.x + s * .24f, c.y - s * .10f)
                }
                drawPath(path, if (i % 2 == 0) faint else glow, style = Stroke(line * (1f + i * .45f)))
            }
            drawCircle(glow, s * .022f, Offset(c.x + s * .25f, c.y - s * .10f))
            drawCircle(faint, s * .018f, Offset(c.x - s * .20f, c.y + s * .16f))
        }
        RexThemeMotif.STARFIELD -> {
            val points = listOf(.12f to .18f, .32f to .30f, .70f to .16f, .86f to .45f, .55f to .72f, .92f to .86f, .18f to .76f)
            points.forEachIndexed { i, (x, y) ->
                val c = Offset(w * x, h * y)
                drawCircle(if (i % 2 == 0) faint else glow, s * if (i % 3 == 0) .018f else .010f, c)
                if (i % 3 == 0) {
                    drawLine(glow, Offset(c.x - s * .035f, c.y), Offset(c.x + s * .035f, c.y), line)
                    drawLine(glow, Offset(c.x, c.y - s * .035f), Offset(c.x, c.y + s * .035f), line)
                }
            }
        }
        RexThemeMotif.ICE_CRYSTAL -> {
            val cx = w * .78f; val cy = h * .26f
            repeat(6) { i ->
                rotate(i * 60f, Offset(cx, cy)) {
                    drawLine(faint, Offset(cx, cy), Offset(cx + s * .26f, cy), line)
                    drawLine(glow, Offset(cx + s * .16f, cy), Offset(cx + s * .21f, cy - s * .045f), line)
                    drawLine(glow, Offset(cx + s * .16f, cy), Offset(cx + s * .21f, cy + s * .045f), line)
                }
            }
        }
        RexThemeMotif.OCEAN_WAVE -> {
            repeat(4) { i ->
                val y = h * (.42f + i * .12f)
                val path = Path().apply { moveTo(w * .35f, y); cubicTo(w * .52f, y - h * .14f, w * .64f, y + h * .13f, w * 1.05f, y - h * .02f) }
                drawPath(path, if (i % 2 == 0) faint else glow, style = Stroke(line * (1f + i * .25f)))
            }
        }
    }
}
