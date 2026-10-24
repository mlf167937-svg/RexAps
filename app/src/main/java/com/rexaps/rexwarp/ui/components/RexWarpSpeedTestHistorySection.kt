package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.data.RexWarpSpeedTestAggregate
import com.rexaps.rexwarp.data.RexWarpSpeedTestEntity
import java.util.Locale

@Composable
fun RexWarpSpeedTestHistorySection(viewModel: RexWarpSpeedTestViewModel, modifier: Modifier = Modifier) {
        return __splitImpl_RexWarpSpeedTestHistorySection(viewModel, modifier)
    }

@Composable
internal fun BenchmarkCard(stats: RexWarpSpeedTestAggregate, periodLabel: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ringkasan $periodLabel", style = MaterialTheme.typography.titleSmall)
                    Text("${stats.testCount} tes sukses · rata-rata dihitung per metrik yang tersedia", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallMetric("Rata-rata DL", formatMbps(stats.avgDownloadMbps), Modifier.weight(1f))
                SmallMetric("Rata-rata UL", formatMbps(stats.avgUploadMbps), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallMetric("Latency rata-rata", formatMs(stats.avgLatencyMs), Modifier.weight(1f))
                SmallMetric("Jitter rata-rata", formatMs(stats.avgJitterMs), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallMetric("Download tertinggi", formatMbps(stats.maxDownloadMbps), Modifier.weight(1f))
                SmallMetric("Latency terendah", formatMs(stats.minLatencyMs), Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun ComparisonCard(first: RexWarpSpeedTestEntity, second: RexWarpSpeedTestEntity) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Perbandingan dua tes", style = MaterialTheme.typography.titleSmall)
            Text("A = tes lebih lama · B = tes lebih baru. Kondisi jaringan dapat berbeda antarwaktu.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            ComparisonMetric("Download", first.downloadMbps, second.downloadMbps, "Mbps")
            ComparisonMetric("Upload", first.uploadMbps, second.uploadMbps, "Mbps")
            ComparisonMetric("Latency", first.latencyMs, second.latencyMs, "ms")
            ComparisonMetric("Jitter", first.jitterMs, second.jitterMs, "ms")
        }
    }
}

@Composable
private fun ComparisonMetric(label: String, a: Double?, b: Double?, unit: String) {
    val delta = if (a != null && b != null) b - a else null
    val percent = if (a != null && b != null && a > 0.0) (b - a) * 100.0 / a else null
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("${formatNumber(a)} → ${formatNumber(b)} $unit", style = MaterialTheme.typography.bodyMedium)
        Text(
            delta?.let { " (${if (it > 0) "+" else ""}${formatNumber(it)}${percent?.let { p -> ", ${if (p > 0) "+" else ""}${formatNumber(p)}%" }.orEmpty()})" } ?: " —",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
internal fun SmallMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

internal fun formatMbps(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0.0 }
    ?.let { String.format(Locale.getDefault(), "%.2f Mbps", it) } ?: "—"

internal fun formatMs(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0.0 }
    ?.let { String.format(Locale.getDefault(), "%.1f ms", it) } ?: "—"

private fun formatNumber(value: Double?): String = value?.takeIf { it.isFinite() }
    ?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—"
