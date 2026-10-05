package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpDailyUsage
import com.rexaps.rexwarp.RexWarpFormat

/** Bar bertumpuk per hari: upload (bawah) + download (atas); tinggi total = total trafik. */
@Composable
fun RexWarpGraph(series: List<RexWarpDailyUsage>, modifier: Modifier = Modifier) {
    val dlColor = MaterialTheme.colorScheme.primary
    val ulColor = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val max = series.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
    val peak = series.maxByOrNull { it.totalBytes }
    val summary = "Daily traffic graph, ${series.size} days. " +
        (peak?.let { "Peak ${RexWarpFormat.bytes(it.totalBytes)} on ${it.date}." } ?: "")

    Column(modifier.fillMaxWidth().semantics { contentDescription = summary }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Peak ${RexWarpFormat.bytes(max)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            repeat(3) { i ->
                val y = size.height * i / 2f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val n = series.size.coerceAtLeast(1)
            val gap = if (n > 45) 0f else 2.dp.toPx()
            val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
            series.forEachIndexed { i, d ->
                val x = i * (barW + gap)
                val hUp = size.height * d.uploadBytes.toFloat() / max
                val hDl = size.height * d.downloadBytes.toFloat() / max
                bar(x, size.height - hUp, barW, hUp, ulColor)
                bar(x, size.height - hUp - hDl, barW, hDl, dlColor)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(series.firstOrNull()?.date?.toString().orEmpty(), style = MaterialTheme.typography.labelSmall)
            Text(series.lastOrNull()?.date?.toString().orEmpty(), style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(dlColor, "Download (top)"); LegendDot(ulColor, "Upload (bottom)")
        }
    }
}

private fun DrawScope.bar(x: Float, y: Float, w: Float, h: Float, color: androidx.compose.ui.graphics.Color) {
    if (h > 0f) drawRect(color, Offset(x, y), Size(w, h))
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) { drawRect(color) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}