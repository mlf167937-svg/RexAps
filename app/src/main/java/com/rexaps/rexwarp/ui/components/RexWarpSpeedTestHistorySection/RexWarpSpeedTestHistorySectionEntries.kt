package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.data.RexWarpSpeedTestEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun RexWarpSpeedTestHistoryEntries(
    history: List<RexWarpSpeedTestEntity>?,
    visibleHistory: List<RexWarpSpeedTestEntity>,
    dateTimeFormat: DateTimeFormatter,
    compareIds: MutableList<String>,
    comparison: List<RexWarpSpeedTestEntity>,
    onDetail: (RexWarpSpeedTestEntity) -> Unit,
    onDelete: (RexWarpSpeedTestEntity) -> Unit
) {
        Text("Riwayat terbaru", style = MaterialTheme.typography.titleMedium)
        when {
            history == null -> Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Memuat riwayat…")
            }
            visibleHistory.isEmpty() -> Text(
                "Tidak ada pengujian dalam periode ini.",
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> visibleHistory.take(12).forEach { item ->
                val date = runCatching {
                    Instant.ofEpochMilli(item.completedAtEpochMs).atZone(ZoneId.systemDefault()).format(dateTimeFormat)
                }.getOrDefault("Waktu tidak tersedia")
                Card(
                    Modifier.fillMaxWidth().clickable { onDetail(item) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(date, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (item.isSuccessful) "Sukses · Cloudflare" else "Gagal · tidak dihitung ke benchmark",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isSuccessful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    when (item.vpnConnected) {
                                        true -> "WARP aktif saat tes dimulai"
                                        false -> "WARP nonaktif saat tes dimulai"
                                        null -> "Kondisi VPN tidak tercatat"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onDelete(item) }) { Text("Hapus") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            SmallMetric("Download", formatMbps(item.downloadMbps), Modifier.weight(1f))
                            SmallMetric("Upload", formatMbps(item.uploadMbps), Modifier.weight(1f))
                            SmallMetric("Latency", formatMs(item.latencyMs), Modifier.weight(1f))
                            SmallMetric("Jitter", formatMs(item.jitterMs), Modifier.weight(1f))
                        }
                        if (!item.isSuccessful && !item.errorSummary.isNullOrBlank()) {
                            Text(item.errorSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (item.isSuccessful) {
                            OutlinedButton(
                                onClick = {
                                    if (item.id in compareIds) compareIds.remove(item.id)
                                    else if (compareIds.size < 2) compareIds.add(item.id)
                                    else { compareIds.removeAt(0); compareIds.add(item.id) }
                                }
                            ) { Text(if (item.id in compareIds) "Batalkan pilihan" else "Bandingkan") }
                        }
                    }
                }
            }
        }

        if (comparison.size == 2) ComparisonCard(comparison[0], comparison[1])
        if ((history?.size ?: 0) > 12) {
            Text("Menampilkan maksimal 12 hasil dari ${history?.size ?: 0} hasil terbaru yang dimuat (batas 50) pada periode pilihan.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
}
