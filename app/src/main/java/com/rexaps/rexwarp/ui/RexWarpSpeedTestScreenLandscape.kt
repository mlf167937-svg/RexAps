package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.speedtest.CloudflareSpeedTestWebView
import com.rexaps.rexwarp.ui.components.RexWarpSpeedTestHistorySection

@Composable
internal fun RexWarpSpeedTestScreenLandscape(
    state: RexWarpSpeedTestUiState,
    webView: WebView?,
    viewModel: RexWarpSpeedTestViewModel,
    modifier: Modifier = Modifier,
    vpnConnected: Boolean? = null
) {
    Row(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            Modifier.weight(0.98f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SpeedTestHero(state)
            SpeedTestGauge(state)
        }
        Column(
            Modifier.weight(1.02f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(Modifier.weight(1f), "Download", state.downloadMbps?.let(::formatMbps) ?: "—", "Mbps", MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Download, null) }
                MetricTile(Modifier.weight(1f), "Upload", state.uploadMbps?.let(::formatMbps) ?: "—", "Mbps", MaterialTheme.colorScheme.tertiary) { Icon(Icons.Filled.Upload, null) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
}
