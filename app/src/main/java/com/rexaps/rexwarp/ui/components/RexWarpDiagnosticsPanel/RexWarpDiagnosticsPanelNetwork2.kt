package com.rexaps.rexwarp.ui.components

import android.net.VpnService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rexaps.rexwarp.RexWarpConnectionState
import com.rexaps.rexwarp.RexWarpState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Explicit, user-triggered network diagnostics. No settings or tunnel state are changed here. */
@Composable internal
fun __RexWarpDiagnosticsPanelContent(
    state: RexWarpState,
    onConnectClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<DiagnosticCheck>?>(null) }
    var completedAt by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var runSequence by remember { mutableStateOf(0) }
    val isTunnelActive = state.connection == RexWarpConnectionState.CONNECTED

    val report = remember(results, completedAt) {
        buildString {
            appendLine("RexWARP connection diagnostics")
            appendLine("Completed: ${completedAt ?: "not run"}")
            results.orEmpty().forEach { check ->
                appendLine("[${check.status.label}] ${check.name}: ${check.detail}${check.durationMs?.let { " (${it} ms)" }.orEmpty()}")
            }
            appendLine("Note: checks are point-in-time observations, not a guarantee against future disconnections or leaks.")
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Connection diagnostics", style = MaterialTheme.typography.titleLarge)
                Text("Pemeriksaan jaringan nyata; jalankan manual saat dibutuhkan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (running) CircularProgressIndicator()
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status saat ini", style = MaterialTheme.typography.titleSmall)
                CheckRow(
                    DiagnosticCheck(
                        "Tunnel WARP",
                        when (state.connection) {
                            RexWarpConnectionState.CONNECTED -> DiagnosticStatus.PASS
                            RexWarpConnectionState.ERROR -> DiagnosticStatus.FAIL
                            RexWarpConnectionState.CONNECTING, RexWarpConnectionState.DISCONNECTING -> DiagnosticStatus.WARNING
                            RexWarpConnectionState.DISCONNECTED -> DiagnosticStatus.WARNING
                        },
                        when (state.connection) {
                            RexWarpConnectionState.CONNECTED -> "UI melaporkan Connected setelah validasi Cloudflare pada jalur tunnel."
                            RexWarpConnectionState.ERROR -> state.error?.userMessage ?: "Engine melaporkan error."
                            RexWarpConnectionState.CONNECTING -> "Koneksi sedang disiapkan; status akhir belum terverifikasi."
                            RexWarpConnectionState.DISCONNECTING -> "Tunnel sedang dihentikan."
                            RexWarpConnectionState.DISCONNECTED -> "Tunnel sedang tidak aktif; koneksi normal perangkat tidak diuji sebagai tunnel."
                        }
                    )
                )
                val permissionGranted = runCatching { VpnService.prepare(context) == null }.getOrDefault(false)
                CheckRow(
                    DiagnosticCheck(
                        "Izin VPN Android",
                        if (permissionGranted) DiagnosticStatus.PASS else DiagnosticStatus.WARNING,
                        if (permissionGranted) "Android tidak meminta persetujuan VPN ulang saat ini." else "Persetujuan VPN perlu diberikan sebelum koneksi dapat dimulai."
                    )
                )
                CheckRow(
                    DiagnosticCheck(
                        "WireGuard handshake detail",
                        DiagnosticStatus.NOT_TESTED,
                        "Provider tidak mengekspos timestamp handshake ke layar diagnosis ini. Status Connected tetap mengikuti validasi tunnel yang ada."
                    )
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (running) {
                        runSequence += 1
                        job?.cancel()
                        results = listOf(DiagnosticCheck("Network checks", DiagnosticStatus.NOT_TESTED, "Pemeriksaan dibatalkan oleh pengguna."))
                        running = false
                    } else {
                        running = true
                        results = null
                        runSequence += 1
                        val thisRun = runSequence
                        job = scope.launch {
                            try {
                                val outcome = withContext(Dispatchers.IO) { runNetworkChecks(isTunnelActive) }
                                if (thisRun == runSequence) {
                                    results = outcome
                                    completedAt = java.time.Instant.now().atZone(java.time.ZoneId.systemDefault())
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()))
                                }
                            } catch (cancel: CancellationException) {
                                if (thisRun == runSequence) {
                                    results = listOf(DiagnosticCheck("Network checks", DiagnosticStatus.NOT_TESTED, "Pemeriksaan dibatalkan oleh pengguna."))
                                }
                                throw cancel
                            } catch (e: Exception) {
                                if (thisRun == runSequence) results = listOf(DiagnosticCheck("Network checks", DiagnosticStatus.FAIL, "Pemeriksaan berhenti: ${safeMessage(e)}"))
                            } finally {
                                if (thisRun == runSequence) running = false
                            }
                        }
                    }
                },
                enabled = true,
                modifier = Modifier.weight(1f)
            ) { Text(if (running) "Batalkan pemeriksaan" else "Jalankan pemeriksaan") }
            if (!results.isNullOrEmpty()) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(report)) }, modifier = Modifier.weight(1f)) {
                    Text("Salin laporan")
                }
            }
        }

        results?.let { checks ->
            Text("Hasil pemeriksaan", style = MaterialTheme.typography.titleMedium)
            checks.forEach { CheckRow(it) }
        }

        if (!isTunnelActive && onConnectClick != null) {
            OutlinedButton(
                onClick = onConnectClick,
                enabled = !running && state.connection != RexWarpConnectionState.CONNECTING && state.connection != RexWarpConnectionState.DISCONNECTING,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.connection == RexWarpConnectionState.ERROR) "Coba sambungkan kembali" else "Sambungkan RexWARP") }
        }

        Text(
            "Perlindungan kebocoran bergantung pada versi Android dan konfigurasi sistem. Backend RexWARP pada source ini tidak menyediakan kill switch internal yang dapat dijamin. Untuk pemblokiran saat VPN terputus, periksa opsi Always-on VPN dan ‘Block connections without VPN’ di pengaturan Android jika tersedia.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = "Batasan perlindungan VPN dan panduan konfigurasi Android" }
        )
    }
}
