package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.speedtest.CloudflareSpeedTestWebView
import com.rexaps.rexwarp.ui.components.RexWarpSpeedTestHistorySection
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun RexWarpSpeedTestScreenPortrait(
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
internal fun SpeedTestGauge(state: RexWarpSpeedTestUiState) {
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
