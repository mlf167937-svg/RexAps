package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun SpeedTestHero(state: RexWarpSpeedTestUiState) {
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
