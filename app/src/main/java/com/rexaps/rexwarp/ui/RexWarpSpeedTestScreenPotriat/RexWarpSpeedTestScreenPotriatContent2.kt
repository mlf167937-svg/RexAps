package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.speedtest.CloudflareSpeedTestWebView
import com.rexaps.rexwarp.ui.components.RexWarpSpeedTestHistorySection

@Composable
internal fun __splitImpl_TestProgressPanel(state: RexWarpSpeedTestUiState) {
    val c = MaterialTheme.colorScheme
    val target = when { state.completed -> 1f; state.running && state.phaseCount > 0 -> (state.phaseIndex.toFloat() / state.phaseCount).coerceIn(.02f, .98f); state.running -> .06f; else -> 0f }
    val progress by animateFloatAsState(target, tween(500, easing = FastOutSlowInEasing), label = "phase-progress")
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(c.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Bolt, null, tint = c.primary, modifier = Modifier.size(19.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Progress pengujian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (state.running) "Mohon tunggu, jangan tutup halaman" else "Ringkasan tahapan tes koneksi", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                Text(when { state.completed -> "100%"; state.running && state.phaseCount > 0 -> "${(state.phaseIndex * 100 / state.phaseCount).coerceIn(1, 99)}%"; state.running -> "LIVE"; else -> "—" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = c.primary)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(c.primary, c.tertiary))))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.running) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp) else Icon(if (state.completed) Icons.Filled.CheckCircle else Icons.Filled.Info, null, tint = if (state.completed) c.primary else c.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(state.phase, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun __splitImpl_RexWarpSpeedTestScreenPortrait(
    state: RexWarpSpeedTestUiState,
    webView: WebView?,
    viewModel: RexWarpSpeedTestViewModel,
    modifier: Modifier = Modifier,
    vpnConnected: Boolean? = null
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SpeedTestHero(state)
        SpeedTestGauge(state)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(Modifier.weight(1f), "Download", state.downloadMbps?.let(::formatMbps) ?: "—", "Mbps", MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Download, null) }
            MetricTile(Modifier.weight(1f), "Upload", state.uploadMbps?.let(::formatMbps) ?: "—", "Mbps", MaterialTheme.colorScheme.tertiary) { Icon(Icons.Filled.Upload, null) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(Modifier.weight(1f), "Latency", state.latencyMs?.let(::formatDecimal) ?: "—", "ms", MaterialTheme.colorScheme.secondary) { Icon(Icons.Filled.Bolt, null) }
            MetricTile(Modifier.weight(1f), "Jitter", state.jitterMs?.let(::formatDecimal) ?: "—", "ms", MaterialTheme.colorScheme.primary) { Icon(Icons.Outlined.Timelapse, null) }
        }
        TestProgressPanel(state)
        PrimaryTestAction(state) {
            when {
                state.running -> { CloudflareSpeedTestWebView.cancel(webView); viewModel.onCancelRequested() }
                state.error != null && !state.engineReady -> { viewModel.onRetryLoading(); webView?.reload() }
                else -> { viewModel.onStartRequested(vpnConnected); CloudflareSpeedTestWebView.start(webView) }
            }
        }
        AnimatedVisibility(state.error != null, enter = fadeIn(tween(220)) + scaleIn(initialScale = .97f, animationSpec = tween(220)), exit = fadeOut(tween(160))) {
            state.error?.let { ErrorPanel(it) }
        }
        PrivacyPanel()
        RexWarpSpeedTestHistorySection(viewModel)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun __splitImpl_PrivacyPanel() {
    val c = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(c.secondary.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Shield, null, tint = c.secondary) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Transparansi & privasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Ketahui cara hasil tes diproses", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge, color = c.primary)
            }
            AnimatedVisibility(expanded, enter = fadeIn(tween(180)), exit = fadeOut(tween(140))) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tes memakai mesin pengukuran Cloudflare yang dimuat melalui internet. Penggunaan data bisa mencapai puluhan MB, tergantung konfigurasi tes dan kondisi jaringan.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    Text("Jika RexWarp VPN aktif, hasil dapat mencerminkan rute koneksi melalui VPN. Jalankan tes beberapa kali untuk membandingkan hasil dengan lebih adil.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
        }
    }
}
