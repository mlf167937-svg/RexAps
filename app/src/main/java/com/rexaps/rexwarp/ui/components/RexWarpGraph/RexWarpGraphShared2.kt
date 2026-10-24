package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpBucket
import com.rexaps.rexwarp.RexWarpFormat

/**
 * Interactive history chart. It only draws buckets marked as observed; an interval without a
 * saved sample is kept visibly unavailable instead of being presented as zero traffic.
 */
@Composable internal
fun __RexWarpGraphContent(series: List<RexWarpBucket>, modifier: Modifier = Modifier) {
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

        RexWarpGraphCanvas(
            series = series,
            metric = metric,
            selectedIndex = selectedIndex,
            canvasWidthPx = canvasWidthPx,
            description = description,
            onCanvasWidthChange = { canvasWidthPx = it },
            onSelectedIndexChange = { selectedIndex = it }
        )

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
