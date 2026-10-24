package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import java.util.Locale

@Composable
internal fun RexWarpSpeedTestScreenPortrait(
    state: RexWarpSpeedTestUiState,
    webView: WebView?,
    viewModel: RexWarpSpeedTestViewModel,
    modifier: Modifier = Modifier,
    vpnConnected: Boolean? = null
) {
        return __splitImpl_RexWarpSpeedTestScreenPortrait(state, webView, viewModel, modifier, vpnConnected)
    }

@Composable
internal fun SpeedTestHero(state: RexWarpSpeedTestUiState) {
        return __splitImpl_SpeedTestHero(state)
    }

@Composable
internal fun SpeedTestGauge(state: RexWarpSpeedTestUiState) {
        return __splitImpl_SpeedTestGauge(state)
    }

@Composable
internal fun MetricTile(modifier: Modifier, title: String, value: String, unit: String, accent: Color, icon: @Composable () -> Unit) {
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
internal fun TestProgressPanel(state: RexWarpSpeedTestUiState) {
        return __splitImpl_TestProgressPanel(state)
    }

@Composable
internal fun PrimaryTestAction(state: RexWarpSpeedTestUiState, onClick: () -> Unit) {
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
internal fun ErrorPanel(message: String) {
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
internal fun PrivacyPanel() {
        return __splitImpl_PrivacyPanel()
    }

internal fun formatMbps(value: Double): String = when {
    !value.isFinite() || value < 0 -> "—"
    value >= 1000 -> String.format(Locale.US, "%.0f", value)
    value >= 100 -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.2f", value)
}
internal fun formatDecimal(value: Double): String = if (!value.isFinite() || value < 0) "—" else String.format(Locale.US, "%.1f", value)
