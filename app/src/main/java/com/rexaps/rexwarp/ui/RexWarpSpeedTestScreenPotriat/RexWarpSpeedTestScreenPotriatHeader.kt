package com.rexaps.rexwarp.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
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
