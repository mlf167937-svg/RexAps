package com.rexaps.rexwarp.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpSpeedTestUiState

@Composable
internal fun TestProgressPanel(state: RexWarpSpeedTestUiState) {
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
