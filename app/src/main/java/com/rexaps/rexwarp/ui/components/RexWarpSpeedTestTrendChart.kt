package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.data.RexWarpSpeedTestEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private enum class SpeedTrendMetric(val label: String, val unit: String) {
    DOWNLOAD("Download", "Mbps"), UPLOAD("Upload", "Mbps"), LATENCY("Latency", "ms"), JITTER("Jitter", "ms")
}

/** Plots measured values only. Missing metrics are gaps, never inferred zeroes. */
@Composable
fun RexWarpSpeedTestTrendChart(results: List<RexWarpSpeedTestEntity>, modifier: Modifier = Modifier) {
    var metric by remember { mutableStateOf(SpeedTrendMetric.DOWNLOAD) }
    var selectedIndex by remember(results) { mutableStateOf((results.lastIndex).coerceAtLeast(0)) }
    var widthPx by remember { mutableStateOf(0f) }
    val colors = MaterialTheme.colorScheme
    val values = remember(results, metric) { results.map { result -> metric.value(result) } }
    val valid = values.filterNotNull().filter { it.isFinite() && it >= 0.0 }
    val minimum = valid.minOrNull()
    val maximum = valid.maxOrNull()
    val selected = results.getOrNull(selectedIndex)
    val selectedValue = values.getOrNull(selectedIndex)?.takeIf { it.isFinite() && it >= 0.0 }
    val timeFormat = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    val selectedTime = selected?.let {
        runCatching { Instant.ofEpochMilli(it.completedAtEpochMs).atZone(ZoneId.systemDefault()).format(timeFormat) }.getOrNull()
    }
    val description = "Tren Speed Test berdasarkan ${metric.label}. ${valid.size} dari ${results.size} hasil memiliki metrik."

    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Tren hasil sukses", style = MaterialTheme.typography.titleMedium)
                Text("Hanya pengukuran yang tersedia ditampilkan; titik tanpa metrik tidak diinterpolasi.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SpeedTrendMetric.entries.forEach { item ->
                    FilterChip(selected = metric == item, onClick = { metric = item }, label = { Text(item.label) })
                }
            }
            if (valid.isEmpty()) {
                Text("Belum ada metrik ${metric.label.lowercase(Locale.getDefault())} yang valid pada periode ini.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 14.dp))
            } else {
                Canvas(
                    Modifier.fillMaxWidth().height(152.dp)
                        .onSizeChanged { widthPx = it.width.toFloat() }
                        .pointerInput(results, metric, widthPx) {
                            detectTapGestures { tap ->
                                if (results.isNotEmpty() && widthPx > 0f) {
                                    selectedIndex = (tap.x / widthPx * results.size).toInt().coerceIn(0, results.lastIndex)
                                }
                            }
                        }
                        .semantics { contentDescription = "$description Ketuk grafik untuk memilih tes." }
                ) {
                    if (results.isEmpty()) return@Canvas
                    val left = 4.dp.toPx()
                    val right = size.width - 4.dp.toPx()
                    val top = 8.dp.toPx()
                    val bottom = size.height - 8.dp.toPx()
                    val plotHeight = (bottom - top).coerceAtLeast(1f)
                    repeat(4) { i ->
                        val y = top + plotHeight * i / 3f
                        drawLine(colors.outlineVariant.copy(alpha = .45f), Offset(left, y), Offset(right, y), 1.dp.toPx())
                    }
                    val lo = minimum ?: 0.0
                    val hi = maximum ?: (lo + 1.0)
                    val span = hi - lo
                    fun x(index: Int): Float = if (results.size <= 1) (left + right) / 2f else left + (right - left) * index / (results.size - 1).toFloat()
                    fun y(value: Double): Float = if (span <= 0.0) (top + bottom) / 2f
                        else bottom - (((value - lo) / span).toFloat().coerceIn(0f, 1f) * plotHeight)

                    var index = 0
                    while (index < results.size) {
                        while (index < results.size && values[index]?.let { it.isFinite() && it >= 0.0 } != true) index++
                        if (index >= results.size) break
                        val start = index
                        var end = start
                        while (end + 1 < results.size && values[end + 1]?.let { it.isFinite() && it >= 0.0 } == true) end++
                        val line = Path()
                        val area = Path()
                        val firstX = x(start)
                        val firstY = y(values[start]!!)
                        line.moveTo(firstX, firstY)
                        area.moveTo(firstX, bottom)
                        area.lineTo(firstX, firstY)
                        var previousX = firstX
                        var previousY = firstY
                        for (i in (start + 1)..end) {
                            val currentX = x(i)
                            val currentY = y(values[i]!!)
                            val midX = (previousX + currentX) / 2f
                            line.cubicTo(midX, previousY, midX, currentY, currentX, currentY)
                            area.cubicTo(midX, previousY, midX, currentY, currentX, currentY)
                            previousX = currentX
                            previousY = currentY
                        }
                        area.lineTo(x(end), bottom)
                        area.close()
                        val accent = when (metric) {
                            SpeedTrendMetric.UPLOAD, SpeedTrendMetric.JITTER -> colors.tertiary
                            SpeedTrendMetric.LATENCY -> colors.secondary
                            SpeedTrendMetric.DOWNLOAD -> colors.primary
                        }
                        drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = .24f), accent.copy(alpha = .015f)), top, bottom))
                        drawPath(line, accent, style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round))
                        if (start == end) drawCircle(accent, 3.dp.toPx(), Offset(firstX, firstY))
                        index = end + 1
                    }
                    val activeX = x(selectedIndex.coerceIn(0, results.lastIndex))
                    drawLine(colors.onSurface.copy(alpha = .48f), Offset(activeX, top), Offset(activeX, bottom), 1.dp.toPx())
                    selectedValue?.let { v ->
                        val accent = when (metric) {
                            SpeedTrendMetric.UPLOAD, SpeedTrendMetric.JITTER -> colors.tertiary
                            SpeedTrendMetric.LATENCY -> colors.secondary
                            SpeedTrendMetric.DOWNLOAD -> colors.primary
                        }
                        val activeY = y(v)
                        drawCircle(accent.copy(alpha = .18f), 8.dp.toPx(), Offset(activeX, activeY))
                        drawCircle(accent, 4.dp.toPx(), Offset(activeX, activeY))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(results.firstOrNull()?.let { runCatching { Instant.ofEpochMilli(it.completedAtEpochMs).atZone(ZoneId.systemDefault()).format(timeFormat) }.getOrNull() }.orEmpty(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Text(results.lastOrNull()?.let { runCatching { Instant.ofEpochMilli(it.completedAtEpochMs).atZone(ZoneId.systemDefault()).format(timeFormat) }.getOrNull() }.orEmpty(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(selectedTime ?: "Tes tidak dipilih", style = MaterialTheme.typography.titleSmall)
                        Text("${metric.label} terukur", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Text(selectedValue?.let { String.format(Locale.getDefault(), if (metric.unit == "Mbps") "%.2f %s" else "%.1f %s", it, metric.unit) } ?: "Tidak tersedia", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                }
            }
        }
    }
}

private fun SpeedTrendMetric.value(result: RexWarpSpeedTestEntity): Double? = when (this) {
    SpeedTrendMetric.DOWNLOAD -> result.downloadMbps
    SpeedTrendMetric.UPLOAD -> result.uploadMbps
    SpeedTrendMetric.LATENCY -> result.latencyMs
    SpeedTrendMetric.JITTER -> result.jitterMs
}
