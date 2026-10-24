package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.rexaps.rexwarp.RexWarpBucket

@Composable
internal fun RexWarpGraphCanvas(
    series: List<RexWarpBucket>,
    metric: HistoryMetric,
    selectedIndex: Int,
    canvasWidthPx: Float,
    description: String,
    onCanvasWidthChange: (Float) -> Unit,
    onSelectedIndexChange: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(184.dp)
                .onSizeChanged { onCanvasWidthChange(it.width.toFloat()) }
                .pointerInput(series, metric, canvasWidthPx) {
                    detectTapGestures { tap ->
                        if (series.isNotEmpty() && canvasWidthPx > 0f) {
                            onSelectedIndexChange(if (metric == HistoryMetric.BREAKDOWN) {
                                (tap.x / canvasWidthPx * series.size).toInt().coerceIn(0, series.lastIndex)
                            } else {
                                (tap.x / canvasWidthPx * (series.size - 1)).roundToInt().coerceIn(0, series.lastIndex)
                            })
                        }
                    }
                }
                .semantics { contentDescription = "$description Ketuk grafik untuk memilih interval." }
        ) {
            val width = size.width
            val top = 10.dp.toPx()
            val bottom = size.height - 12.dp.toPx()
            val plotHeight = (bottom - top).coerceAtLeast(1f)
            val grid = colors.outlineVariant.copy(alpha = .48f)
            repeat(4) { i ->
                val y = top + plotHeight * i / 3f
                drawLine(grid, Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
            }

            if (series.isEmpty()) return@Canvas
            val count = series.size
            val step = if (count <= 1) width else width / (count - 1).toFloat()
            val barGap = if (count > 45) 0f else 2.dp.toPx()
            val barWidth = (width / count.toFloat() - barGap).coerceAtLeast(1f)

            fun valueAt(bucket: RexWarpBucket): Long = when (metric) {
                HistoryMetric.TOTAL, HistoryMetric.BREAKDOWN -> bucket.totalBytes.coerceAtLeast(0L)
                HistoryMetric.DOWNLOAD -> bucket.downloadBytes.coerceAtLeast(0L)
                HistoryMetric.UPLOAD -> bucket.uploadBytes.coerceAtLeast(0L)
            }

            val maximum = series.filter { it.hasData }.maxOfOrNull { valueAt(it) }?.coerceAtLeast(1L) ?: 1L
            fun yFor(value: Long): Float = bottom - (value.toDouble() / maximum.toDouble()).toFloat().coerceIn(0f, 1f) * plotHeight
            fun xFor(index: Int): Float = if (count == 1) width / 2f else step * index

            if (metric == HistoryMetric.BREAKDOWN) {
                series.forEachIndexed { index, bucket ->
                    if (!bucket.hasData) return@forEachIndexed
                    val x = index * (width / count.toFloat()) + barGap / 2f
                    val down = (bucket.downloadBytes.coerceAtLeast(0L).toDouble() / maximum.toDouble()).toFloat() * plotHeight
                    val up = (bucket.uploadBytes.coerceAtLeast(0L).toDouble() / maximum.toDouble()).toFloat() * plotHeight
                    if (down > 0f) drawRect(colors.primary, Offset(x, bottom - down), androidx.compose.ui.geometry.Size(barWidth, down))
                    if (up > 0f) drawRect(colors.tertiary, Offset(x, bottom - down - up), androidx.compose.ui.geometry.Size(barWidth, up))
                }
            } else {
                // Draw each contiguous run separately so missing samples do not become interpolated data.
                var start = 0
                while (start < count) {
                    while (start < count && !series[start].hasData) start++
                    if (start >= count) break
                    var end = start
                    while (end + 1 < count && series[end + 1].hasData) end++
                    val line = Path()
                    val area = Path()
                    val firstX = xFor(start)
                    val firstY = yFor(valueAt(series[start]))
                    line.moveTo(firstX, firstY)
                    area.moveTo(firstX, bottom)
                    area.lineTo(firstX, firstY)
                    var previousX = firstX
                    var previousY = firstY
                    for (index in (start + 1)..end) {
                        val x = xFor(index)
                        val y = yFor(valueAt(series[index]))
                        val midX = (previousX + x) / 2f
                        line.cubicTo(midX, previousY, midX, y, x, y)
                        area.cubicTo(midX, previousY, midX, y, x, y)
                        previousX = x
                        previousY = y
                    }
                    area.lineTo(xFor(end), bottom)
                    area.close()
                    val accent = if (metric == HistoryMetric.UPLOAD) colors.tertiary else colors.primary
                    drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = .28f), accent.copy(alpha = .015f)), top, bottom))
                    drawPath(line, accent, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
                    if (start == end) drawCircle(accent, 3.dp.toPx(), Offset(firstX, firstY))
                    start = end + 1
                }
            }

            val activeIndex = selectedIndex.coerceIn(0, count - 1)
            val active = series[activeIndex]
            val activeX = if (metric == HistoryMetric.BREAKDOWN) {
                activeIndex * (width / count.toFloat()) + (width / count.toFloat()) / 2f
            } else xFor(activeIndex)
            drawLine(colors.onSurface.copy(alpha = .48f), Offset(activeX, top), Offset(activeX, bottom), strokeWidth = 1.dp.toPx())
            if (active.hasData) {
                val activeValue = valueAt(active)
                val dotY = yFor(activeValue)
                val accent = when (metric) {
                    HistoryMetric.UPLOAD -> colors.tertiary
                    else -> colors.primary
                }
                drawCircle(accent.copy(alpha = .18f), 8.dp.toPx(), Offset(activeX, dotY))
                drawCircle(accent, 4.dp.toPx(), Offset(activeX, dotY))
            }
        }
}
