package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpSpeedTestPeriod
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.RexWarpSpeedTestVpnFilter
import com.rexaps.rexwarp.data.RexWarpSpeedTestEntity
import com.rexaps.rexwarp.ui.components.RexWarpSpeedTestTrendChart
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun __splitImpl_RexWarpSpeedTestHistorySection(viewModel: RexWarpSpeedTestViewModel, modifier: Modifier = Modifier) {
    val history by viewModel.history.collectAsState()
    val benchmark by viewModel.benchmark.collectAsState()
    val period by viewModel.selectedPeriod.collectAsState()
    val vpnFilter by viewModel.selectedVpnFilter.collectAsState()
    val historyError by viewModel.historyError.collectAsState()
    val compareIds = remember { mutableStateListOf<String>() }
    var deleteTarget by remember { mutableStateOf<RexWarpSpeedTestEntity?>(null) }
    var detailTarget by remember { mutableStateOf<RexWarpSpeedTestEntity?>(null) }
    var askClear by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val cutoff = period.durationMs?.let { (now - it).coerceAtLeast(0L) } ?: 0L
    val visibleHistory = history?.filter {
        it.completedAtEpochMs >= cutoff &&
            (vpnFilter.vpnConnected == null || it.vpnConnected == vpnFilter.vpnConnected)
    }.orEmpty()
    val comparison = visibleHistory.filter { it.isSuccessful && it.id in compareIds }.sortedBy { it.completedAtEpochMs }.take(2)
    val trendHistory = visibleHistory.filter { it.isSuccessful }.sortedBy { it.completedAtEpochMs }
    val dateTimeFormat = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Speed Test benchmark", style = MaterialTheme.typography.titleLarge)
                Text("Hasil pengukuran tersimpan lokal; hanya tes sukses dihitung sebagai benchmark.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!history.isNullOrEmpty()) TextButton(onClick = { askClear = true }) { Text("Hapus semua") }
        }

        historyError?.let { message ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = viewModel::clearHistoryError) { Text("Tutup") }
                }
            }
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RexWarpSpeedTestPeriod.entries.forEach { choice ->
                FilterChip(
                    selected = period == choice,
                    onClick = {
                        viewModel.setHistoryPeriod(choice)
                        compareIds.clear()
                    },
                    label = { Text(choice.label) }
                )
            }
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RexWarpSpeedTestVpnFilter.entries.forEach { choice ->
                FilterChip(
                    selected = vpnFilter == choice,
                    onClick = {
                        viewModel.setVpnFilter(choice)
                        compareIds.clear()
                    },
                    label = { Text(choice.label) }
                )
            }
        }

        when {
            benchmark == null -> Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text("Menghitung benchmark…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            benchmark?.testCount == 0 -> Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Belum ada hasil sukses pada periode ini", style = MaterialTheme.typography.titleSmall)
                    Text("Jalankan Speed Test untuk mulai membangun riwayat. Hasil yang gagal tetap dapat dicatat, tetapi tidak masuk ke rata-rata.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> BenchmarkCard(benchmark!!, "${period.label} · ${vpnFilter.label}")
        }

        RexWarpSpeedTestTrendChart(trendHistory)

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
                    Modifier.fillMaxWidth().clickable { detailTarget = item },
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
                            TextButton(onClick = { deleteTarget = item }) { Text("Hapus") }
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

    detailTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { detailTarget = null },
            title = { Text("Detail Speed Test") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(runCatching { Instant.ofEpochMilli(target.completedAtEpochMs).atZone(ZoneId.systemDefault()).format(dateTimeFormat) }.getOrDefault("Waktu tidak tersedia"))
                    Text(if (target.isSuccessful) "Status: sukses" else "Status: gagal")
                    Text("Download: ${formatMbps(target.downloadMbps)}")
                    Text("Upload: ${formatMbps(target.uploadMbps)}")
                    Text("Latency: ${formatMs(target.latencyMs)}")
                    Text("Jitter: ${formatMs(target.jitterMs)}")
                    Text("Durasi: ${target.durationMs?.let { String.format(Locale.getDefault(), "%.1f s", it / 1000.0) } ?: "—"}")
                    Text("VPN saat mulai: ${when (target.vpnConnected) { true -> "WARP aktif"; false -> "WARP nonaktif"; null -> "tidak tercatat" }}")
                    target.errorSummary?.takeIf { it.isNotBlank() }?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = { detailTarget = null }) { Text("Tutup") } }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus hasil Speed Test?") },
            text = { Text("Hasil pada ${target.completedAtEpochMs.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(dateTimeFormat) }} akan dihapus dari riwayat lokal.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteHistoryItem(target.id); compareIds.remove(target.id); deleteTarget = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }
    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text("Hapus semua riwayat Speed Test?") },
            text = { Text("Semua hasil sukses dan gagal yang tersimpan akan dihapus. Statistik penggunaan trafik tidak terpengaruh.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteAllHistory(); compareIds.clear(); askClear = false }) { Text("Hapus semua") } },
            dismissButton = { TextButton(onClick = { askClear = false }) { Text("Batal") } }
        )
    }
}
