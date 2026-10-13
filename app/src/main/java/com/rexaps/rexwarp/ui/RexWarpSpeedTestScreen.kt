package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.speedtest.CloudflareSpeedTestWebView
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RexWarpSpeedTestScreen(
    modifier: Modifier = Modifier,
    viewModel: RexWarpSpeedTestViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0f),
        factory = { context -> CloudflareSpeedTestWebView.create(context, viewModel).also { webView = it } },
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
                else -> { viewModel.onStartRequested(); CloudflareSpeedTestWebView.start(webView) }
            }
        }
        AnimatedVisibility(state.error != null, enter = fadeIn(tween(220)) + scaleIn(initialScale = .97f, animationSpec = tween(220)), exit = fadeOut(tween(160))) {
            state.error?.let { ErrorPanel(it) }
        }
        PrivacyPanel()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SpeedTestHero(state: RexWarpSpeedTestUiState) {
    val c = MaterialTheme.colorScheme
    val orbit = rememberInfiniteTransition(label = "hero-orbit").animateFloat(0f, 360f, infiniteRepeatable(tween(18000), RepeatMode.Restart), label = "orbit").value
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(c.primary.copy(alpha = .17f), c.tertiary.copy(alpha = .08f), c.surfaceContainerHigh))).padding(20.dp)) {
            Canvas(Modifier.size(118.dp).align(Alignment.TopEnd).alpha(.7f)) {
                val r = size.minDimension * .34f
                drawCircle(c.primary.copy(alpha = .12f), r * 1.6f)
                drawCircle(c.primary.copy(alpha = .45f), r, androidx.compose.ui.geometry.Offset(center.x + cos(Math.toRadians(orbit.toDouble())).toFloat() * r * .42f, center.y + sin(Math.toRadians(orbit.toDouble())).toFloat() * r * .42f), style = Stroke(1.5.dp.toPx()))
            }
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(c.primary, c.tertiary))), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Cloud, null, tint = c.onPrimary, modifier = Modifier.size(26.dp)) }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("REXWARP / NETWORK", style = MaterialTheme.typography.labelSmall, color = c.primary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                        Text("Speed diagnostics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Kenali performa koneksi kamu secara real-time.", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.widthIn(max = 270.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = if (state.engineReady) c.primary.copy(alpha = .12f) else c.surfaceVariant, shape = CircleShape) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (state.engineReady) c.primary else c.onSurfaceVariant))
                            Text(when { state.running -> "TEST BERJALAN"; state.completed -> "SELESAI"; state.error != null -> "PERLU PERHATIAN"; state.engineReady -> "MESIN SIAP"; else -> "MENYIAPKAN MESIN" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (state.error != null) c.error else c.onSurface)
                        }
                    }
                    Text("Cloudflare engine", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SpeedTestGauge(state: RexWarpSpeedTestUiState) {
    val c = MaterialTheme.colorScheme
    val raw = ((state.downloadMbps ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    val progress by animateFloatAsState(if (state.running && raw < .08f) .08f else raw, tween(700, easing = FastOutSlowInEasing), label = "gauge")
    val pulse by rememberInfiniteTransition(label = "gauge-pulse").animateFloat(1f, 1.045f, infiniteRepeatable(tween(1050, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Filled.Speed, null, tint = c.primary, modifier = Modifier.size(17.dp))
                Text(when { state.running -> "LIVE MEASUREMENT"; state.completed -> "YOUR DOWNLOAD SPEED"; else -> "READY WHEN YOU ARE" }, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp, color = c.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().widthIn(max = 300.dp).aspectRatio(1.2f), contentAlignment = Alignment.Center) {
                val gaugeSize = maxWidth.coerceAtMost(270.dp)
                Canvas(Modifier.size(gaugeSize)) {
                    val stroke = 12.dp.toPx()
                    val start = 135f
                    drawArc(Brush.sweepGradient(listOf(c.primary.copy(alpha = .18f), c.tertiary.copy(alpha = .32f), c.primary.copy(alpha = .18f))), start, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(Brush.sweepGradient(listOf(c.primary, c.tertiary, c.primary)), start, 270f * progress, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (progress > 0f) {
                        val a = Math.toRadians((start + 270f * progress).toDouble())
                        val r = size.minDimension / 2f - stroke / 2f
                        val p = androidx.compose.ui.geometry.Offset(center.x + cos(a).toFloat() * r, center.y + sin(a).toFloat() * r)
                        drawCircle(c.primary.copy(alpha = .18f), stroke * 1.25f, p)
                        drawCircle(c.primary, stroke * .52f, p)
                    }
                }
                Column(Modifier.align(Alignment.Center).scale(if (state.running) pulse else 1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    AnimatedContent(state.downloadMbps, label = "main-speed") { value -> Text(value?.let(::formatMbps) ?: "— —", fontSize = 39.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.7).sp, maxLines = 1) }
                    Text("Mbps", style = MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.running) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp) else if (state.completed) Icon(Icons.Filled.CheckCircle, null, tint = c.primary, modifier = Modifier.size(14.dp))
                        Text(when { state.running -> "Sedang mengukur"; state.completed -> "Hasil pengujian"; state.error != null -> "Pengukuran tertunda"; else -> "Download throughput" }, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    }
                }
            }
            Text(state.phase, style = MaterialTheme.typography.bodyMedium, color = if (state.error != null) c.error else c.onSurfaceVariant, textAlign = TextAlign.Center)
            if (state.completed && state.durationMs != null) Surface(color = c.primary.copy(alpha = .10f), shape = CircleShape) {
                Text("Selesai dalam ${formatDecimal(state.durationMs / 1000.0)} detik", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = c.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MetricTile(modifier: Modifier, title: String, value: String, unit: String, accent: Color, icon: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(modifier.animateContentSize(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides accent) { icon() }
                }
                Text(title, style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                AnimatedContent(value, label = "metric-$title") { Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                Text(unit, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun TestProgressPanel(state: RexWarpSpeedTestUiState) {
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
private fun PrimaryTestAction(state: RexWarpSpeedTestUiState, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    Button(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        enabled = state.engineReady || state.running || state.error != null,
        modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (state.running) c.error else c.primary, disabledContainerColor = c.surfaceVariant, disabledContentColor = c.onSurfaceVariant),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        AnimatedContent(actionLabel(state), label = "primary-action") { label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(when { state.running -> Icons.Filled.Stop; state.completed || (state.error != null && !state.engineReady) -> Icons.Filled.Refresh; else -> Icons.Filled.Speed }, null)
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
    if (!state.engineReady && state.error == null) Text("Menghubungkan ke mesin pengukuran…", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
}

private fun actionLabel(state: RexWarpSpeedTestUiState): String = when {
    state.running -> "Batalkan pengujian"
    state.error != null && !state.engineReady -> "Muat ulang mesin"
    state.completed -> "Uji koneksi lagi"
    else -> "Mulai speed test"
}

@Composable
private fun ErrorPanel(message: String) {
    val c = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.errorContainer)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, null, tint = c.onErrorContainer)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pengujian belum berhasil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.onErrorContainer)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
            }
        }
    }
}

@Composable
private fun PrivacyPanel() {
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

private fun formatMbps(value: Double): String = when {
    !value.isFinite() || value < 0 -> "—"
    value >= 1000 -> String.format(Locale.US, "%.0f", value)
    value >= 100 -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.2f", value)
}
private fun formatDecimal(value: Double): String = if (!value.isFinite() || value < 0) "—" else String.format(Locale.US, "%.1f", value)
