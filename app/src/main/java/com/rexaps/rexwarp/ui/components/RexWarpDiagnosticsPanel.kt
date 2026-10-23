package com.rexaps.rexwarp.ui.components

import android.net.VpnService
import android.os.SystemClock
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

private enum class DiagnosticStatus(val label: String) { PASS("PASS"), WARNING("WARNING"), FAIL("FAIL"), NOT_TESTED("NOT TESTED") }
private data class DiagnosticCheck(val name: String, val status: DiagnosticStatus, val detail: String, val durationMs: Long? = null)

/** Explicit, user-triggered network diagnostics. No settings or tunnel state are changed here. */
@Composable
fun RexWarpDiagnosticsPanel(
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

@Composable
private fun CheckRow(check: DiagnosticCheck) {
    val colors = MaterialTheme.colorScheme
    val statusColor = when (check.status) {
        DiagnosticStatus.PASS -> colors.primary
        DiagnosticStatus.WARNING -> colors.tertiary
        DiagnosticStatus.FAIL -> colors.error
        DiagnosticStatus.NOT_TESTED -> colors.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(check.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(check.status.label, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
            Text(check.detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            check.durationMs?.let { Text("Waktu: ${it} ms", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant) }
        }
    }
}

private suspend fun runNetworkChecks(tunnelActive: Boolean): List<DiagnosticCheck> {
    val checks = mutableListOf<DiagnosticCheck>()
    val dnsStarted = SystemClock.elapsedRealtime()
    val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "RexWarp-DNS-check").apply { isDaemon = true } }
    val addresses = try {
        val future = executor.submit(Callable { InetAddress.getAllByName(CHECK_HOST).toList() })
        try {
            future.get(DNS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            future.cancel(true)
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    } finally {
        executor.shutdownNow()
    }
    val dnsDuration = SystemClock.elapsedRealtime() - dnsStarted
    checks += if (addresses.isNotEmpty()) {
        DiagnosticCheck("DNS lookup ($CHECK_HOST)", DiagnosticStatus.PASS, "Resolver mengembalikan ${addresses.size} alamat.", dnsDuration)
    } else {
        DiagnosticCheck("DNS lookup ($CHECK_HOST)", DiagnosticStatus.FAIL, "Tidak ada alamat yang diperoleh dalam batas waktu ${DNS_TIMEOUT_MS} ms.", dnsDuration)
    }

    currentCoroutineContext().ensureActive()
    val httpsStarted = SystemClock.elapsedRealtime()
    var traceWarp: String? = null
    val httpsDetail = try {
        val connection = URL(TRACE_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = SOCKET_TIMEOUT_MS
        connection.readTimeout = SOCKET_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.useCaches = false
        try {
            val code = connection.responseCode
            if (code !in 200..299) "Endpoint HTTPS membalas HTTP $code."
            else {
                val body = connection.inputStream.bufferedReader().use { it.readText().take(12_000) }
                traceWarp = body.lineSequence().firstOrNull { it.startsWith("warp=") }?.substringAfter('=')
                if (tunnelActive && traceWarp !in setOf("on", "plus")) {
                    "HTTPS berhasil, tetapi trace tidak mengonfirmasi warp=on/plus (nilai=${traceWarp ?: "tidak tersedia"})."
                } else {
                    "HTTPS berhasil (HTTP $code)${traceWarp?.let { "; trace warp=$it" }.orEmpty()}."
                }
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        "Permintaan HTTPS gagal: ${safeMessage(e)}"
    }
    val httpsDuration = SystemClock.elapsedRealtime() - httpsStarted
    val httpsPassed = httpsDetail.startsWith("HTTPS berhasil") && (!tunnelActive || traceWarp in setOf("on", "plus"))
    checks += DiagnosticCheck(
        "HTTPS ke Cloudflare trace",
        if (httpsPassed) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
        httpsDetail,
        httpsDuration
    )

    currentCoroutineContext().ensureActive()
    checks += testAddressFamily("IPv4 HTTPS/TLS", addresses.filterIsInstance<Inet4Address>().firstOrNull())
    currentCoroutineContext().ensureActive()
    checks += testAddressFamily("IPv6 HTTPS/TLS", addresses.filterIsInstance<Inet6Address>().firstOrNull())
    checks += DiagnosticCheck(
        "Packet loss / ICMP ping",
        DiagnosticStatus.NOT_TESTED,
        "Tidak diukur. Pemeriksaan ini memakai DNS dan HTTPS/TLS, bukan ICMP ping atau pengukuran packet loss."
    )
    return checks
}

private fun testAddressFamily(label: String, address: InetAddress?): DiagnosticCheck {
    if (address == null) {
        return DiagnosticCheck(label, DiagnosticStatus.NOT_TESTED, "DNS tidak mengembalikan alamat untuk keluarga IP ini; konektivitas keluarga IP tidak dapat disimpulkan.")
    }
    val started = SystemClock.elapsedRealtime()
    return try {
        Socket().use { raw ->
            raw.connect(InetSocketAddress(address, 443), SOCKET_TIMEOUT_MS)
            raw.soTimeout = SOCKET_TIMEOUT_MS
            val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(raw, CHECK_HOST, 443, true) as SSLSocket
            ssl.use {
                it.soTimeout = SOCKET_TIMEOUT_MS
                val params = it.sslParameters
                params.endpointIdentificationAlgorithm = "HTTPS"
                it.sslParameters = params
                it.startHandshake()
            }
        }
        DiagnosticCheck(label, DiagnosticStatus.PASS, "TCP/TLS handshake ke alamat terpilih berhasil.", SystemClock.elapsedRealtime() - started)
    } catch (e: SocketTimeoutException) {
        DiagnosticCheck(label, DiagnosticStatus.FAIL, "Koneksi TLS melewati batas waktu ${SOCKET_TIMEOUT_MS} ms.", SystemClock.elapsedRealtime() - started)
    } catch (e: Exception) {
        DiagnosticCheck(label, DiagnosticStatus.FAIL, "Koneksi ke alamat terpilih gagal: ${safeMessage(e)}", SystemClock.elapsedRealtime() - started)
    }
}

private fun safeMessage(error: Throwable): String = when (error) {
    is SocketTimeoutException -> "timeout"
    else -> error.localizedMessage.orEmpty().take(160).ifBlank { error.javaClass.simpleName }
        .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "[url]")
        .replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "[ip]")
}

private const val CHECK_HOST = "www.cloudflare.com"
private const val TRACE_URL = "https://www.cloudflare.com/cdn-cgi/trace"
private const val DNS_TIMEOUT_MS = 4_000L
private const val SOCKET_TIMEOUT_MS = 4_000
