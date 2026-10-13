package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.speedtest.CloudflareSpeedTestWebView
import kotlinx.coroutines.delay

@Composable
fun RexWarpSpeedTestScreen(
    modifier: Modifier = Modifier,
    viewModel: RexWarpSpeedTestViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0f),
        factory = { ctx ->
            CloudflareSpeedTestWebView.create(ctx, viewModel).also { webView = it }
        },
        update = { if (webView !== it) webView = it }
    )
    DisposableEffect(Unit) {
        onDispose {
            CloudflareSpeedTestWebView.cancel(webView)
            webView?.stopLoading()
            webView?.removeJavascriptInterface("RexWarpBridge")
            webView?.destroy()
            webView = null
            viewModel.onScreenDisposed()
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderCard()
        SpeedGaugeCard(state)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Download",
                value = state.downloadMbps?.let(::formatMbps) ?: "--",
                unit = "Mbps",
                icon = { Icon(Icons.Filled.Download, contentDescription = null) }
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Upload",
                value = state.uploadMbps?.let(::formatMbps) ?: "--",
                unit = "Mbps",
                icon = { Icon(Icons.Filled.Upload, contentDescription = null) }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f), title = "Latency",
                value = state.latencyMs?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "--",
                unit = "ms", icon = { Icon(Icons.Filled.Bolt, contentDescription = null) }
            )
            MetricCard(
                modifier = Modifier.weight(1f), title = "Jitter",
                value = state.jitterMs?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "--",
                unit = "ms", icon = { Icon(Icons.Filled.Speed, contentDescription = null) }
            )
        }
        ProgressCard(state)
        Button(
            onClick = {
                if (state.error != null && !state.engineReady) {
                    viewModel.onRetryLoading()
                    webView?.reload()
                } else if (state.running) {
                    CloudflareSpeedTestWebView.cancel(webView)
                    viewModel.onCancelRequested()
                } else {
                    viewModel.onStartRequested()
                    CloudflareSpeedTestWebView.start(webView)
                }
            },
            enabled = state.engineReady || state.running || state.error != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            AnimatedContent(targetState = state.running, label = "speed-test-action") { running ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(if (running) Icons.Filled.Stop else if (state.completed || (state.error != null && !state.engineReady)) Icons.Filled.Refresh else Icons.Filled.Speed, contentDescription = null)
                    Text(
                        when {
                            running -> "Batalkan Pengujian"
                            state.error != null && !state.engineReady -> "Coba Muat Ulang Mesin"
                            state.completed -> "Ulangi Speed Test"
                            else -> "Mulai Speed Test"
                        }, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        state.error?.let { ErrorCard(it) }
        InfoCard()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeaderCard() {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Cloud, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(29.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("RexWarp Speed Test", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Powered by Cloudflare", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = colors.secondaryContainer) {
                Text("LIVE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpeedGaugeCard(state: RexWarpSpeedTestUiState) {
    val colors = MaterialTheme.colorScheme
    val gaugeValue = (state.downloadMbps ?: 0.0).let { (it / 500.0).coerceIn(0.0, 1.0).toFloat() }
    val animated by animateFloatAsState(gaugeValue, animationSpec = tween(650, easing = FastOutSlowInEasing), label = "gauge-progress")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (state.running) "TESTING CONNECTION" else if (state.completed) "DOWNLOAD SPEED" else "READY TO TEST", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, letterSpacing = 1.4.sp)
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 13.dp.toPx()
                    drawArc(colors.surfaceVariant, 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(colors.primary, 135f, 270f * animated, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    val dotAngle = Math.toRadians((135.0 + 270.0 * animated).toDouble())
                    val radius = size.minDimension / 2f - stroke / 2f
                    val cx = center.x + kotlin.math.cos(dotAngle).toFloat() * radius
                    val cy = center.y + kotlin.math.sin(dotAngle).toFloat() * radius
                    drawCircle(colors.primary, radius = stroke * 0.72f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(state.downloadMbps?.let(::formatMbps) ?: "--.--", fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp)
                    Text("Mbps", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                    if (state.running) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                            Text("Mengukur…", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                        }
                    }
                }
            }
            Text(
                state.phase,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.error != null) colors.error else colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    unit: String,
    icon: @Composable () -> Unit
) {
    Card(modifier, shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) { icon() }
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun ProgressCard(state: RexWarpSpeedTestUiState) {
    val colors = MaterialTheme.colorScheme
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = colors.primary)
                Text("Status pengujian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    when { state.completed -> "100%"; state.running && state.phaseCount > 0 -> "${(state.phaseIndex * 100 / state.phaseCount).coerceIn(0, 99)}%"; else -> "—" },
                    style = MaterialTheme.typography.labelLarge, color = colors.primary
                )
            }
            val progress = when {
                state.completed -> 1f
                state.running && state.phaseCount > 0 -> (state.phaseIndex.toFloat() / state.phaseCount).coerceIn(0f, 0.99f)
                else -> 0f
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
            Text(state.phase, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            state.durationMs?.takeIf { state.completed }?.let {
                Text("Durasi: ${it / 1000.0}s", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun InfoCard() {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Tentang pengukuran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tes menggunakan mesin resmi open-source Cloudflare. Pemakaian data dapat mencapai puluhan MB, tergantung kondisi koneksi. Cloudflare menyatakan hasil tes dikumpulkan saat selesai untuk insight agregat kualitas koneksi.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Jika RexWarp VPN sedang aktif, hasil dapat mencerminkan rute koneksi melalui VPN.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatMbps(value: Double): String = when {
    !value.isFinite() || value < 0 -> "--.--"
    value >= 1000 -> String.format(java.util.Locale.US, "%.0f", value)
    value >= 100 -> String.format(java.util.Locale.US, "%.1f", value)
    else -> String.format(java.util.Locale.US, "%.2f", value)
}
