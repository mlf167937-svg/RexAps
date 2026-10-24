package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.data.RexWarpSpeedTestEntity

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
