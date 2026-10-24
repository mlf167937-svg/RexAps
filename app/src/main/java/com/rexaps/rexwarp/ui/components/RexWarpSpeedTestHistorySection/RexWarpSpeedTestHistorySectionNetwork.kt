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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.data.RexWarpSpeedTestAggregate

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
