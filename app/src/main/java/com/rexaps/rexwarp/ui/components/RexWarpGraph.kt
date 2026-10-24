package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.rexaps.rexwarp.RexWarpFormat

private enum class HistoryMetric(val label: String) {
    TOTAL("Total"), DOWNLOAD("Download"), UPLOAD("Upload"), BREAKDOWN("Download + upload")
}

/**
 * Interactive history chart. It only draws buckets marked as observed; an interval without a
 * saved sample is kept visibly unavailable instead of being presented as zero traffic.
 */
@Composable
fun RexWarpGraph(series: List<RexWarpBucket>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var metric by remember { mutableStateOf(HistoryMetric.TOTAL) }
    var selectedIndex by remember(series) {
        mutableStateOf(series.indexOfLast { it.hasData }.coerceAtLeast(0))
    }
    var canvasWidthPx by remember { mutableStateOf(0f) }

    val observed = remember(series) { series.filter { it.hasData } }
    val peakBucket = observed.maxByOrNull { it.totalBytes }
    val downloadTotal = observed.sumOf { it.downloadBytes.coerceAtLeast(0L) }
    val uploadTotal = observed.sumOf { it.uploadBytes.coerceAtLeast(0L) }
    val total = downloadTotal + uploadTotal
    val average = if (observed.isEmpty()) null else total.toDouble() / observed.size.toDouble()
    val selected = series.getOrNull(selectedIndex)
    val description = buildString {
        append("Grafik trafik interaktif, ").append(series.size).append(" interval.")
        append(" ").append(observed.size).append(" interval memiliki data tersimpan.")
        peakBucket?.let { append(" Puncak ").append(RexWarpFormat.bytes(it.totalBytes)).append(" pada ").append(it.label).append(".") }
    }

    Column(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HistoryMetric.entries.forEach { option ->
                FilterChip(
                    selected = metric == option,
                    onClick = { metric = option },
                    label = { Text(option.label, maxLines = 1) }
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(metric.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    when (metric) {
                        HistoryMetric.TOTAL -> "Volume trafik per interval"
                        HistoryMetric.DOWNLOAD -> "Trafik download yang tercatat"
                        HistoryMetric.UPLOAD -> "Trafik upload yang tercatat"
                        HistoryMetric.BREAKDOWN -> "Download dan upload ditumpuk"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Text(
                peakBucket?.let { "Puncak ${RexWarpFormat.bytes(it.totalBytes)}" } ?: "Belum ada sampel",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary
            )
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(184.dp)
                .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                .pointerInput(series, metric, canvasWidthPx) {
                    detectTapGestures { tap ->
                        if (series.isNotEmpty() && canvasWidthPx > 0f) {
                            selectedIndex = if (metric == HistoryMetric.BREAKDOWN) {
                                (tap.x / canvasWidthPx * series.size).toInt().coerceIn(0, series.lastIndex)
                            } else {
                                (tap.x / canvasWidthPx * (series.size - 1)).roundToInt().coerceIn(0, series.lastIndex)
                            }
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(series.firstOrNull()?.label.orEmpty(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(series.lastOrNull()?.label.orEmpty(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }

        selected?.let { bucket ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(bucket.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (bucket.hasData) "Data direkam" else "Tidak ada sampel pada interval ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bucket.hasData) colors.primary else colors.onSurfaceVariant
                            )
                        }
                        if (bucket.hasData) Text(RexWarpFormat.bytes(bucket.totalBytes), style = MaterialTheme.typography.titleMedium, color = colors.primary)
                    }
                    if (bucket.hasData) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChartMetric("Download", RexWarpFormat.bytes(bucket.downloadBytes), colors.primary, Modifier.weight(1f))
                            ChartMetric("Upload", RexWarpFormat.bytes(bucket.uploadBytes), colors.tertiary, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartSummary("Total", if (observed.isEmpty()) "—" else RexWarpFormat.bytes(total), Modifier.weight(1f))
            ChartSummary("Rata-rata / sampel", average?.let { RexWarpFormat.bytes(it.toLong()) } ?: "—", Modifier.weight(1f))
            ChartSummary("Sampel", observed.size.toString(), Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(colors.primary, when (metric) { HistoryMetric.BREAKDOWN -> "Download / dasar"; HistoryMetric.UPLOAD -> "Upload"; HistoryMetric.DOWNLOAD -> "Download"; HistoryMetric.TOTAL -> "Total traffic" })
            if (metric == HistoryMetric.BREAKDOWN) LegendSwatch(colors.tertiary, "Upload / tumpukan")
            Spacer(Modifier.width(2.dp))
            Text("Ketuk grafik untuk memilih", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChartSummary(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(Modifier.width(14.dp).height(4.dp)) { drawLine(color, Offset.Zero, Offset(size.width, 0f), strokeWidth = size.height, cap = StrokeCap.Round) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
