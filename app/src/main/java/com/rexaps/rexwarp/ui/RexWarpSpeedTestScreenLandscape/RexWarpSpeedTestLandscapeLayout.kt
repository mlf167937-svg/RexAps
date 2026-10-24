package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.speedtest.CloudflareSpeedTestWebView
import com.rexaps.rexwarp.ui.components.RexWarpSpeedTestHistorySection

@Composable
internal fun RexWarpSpeedTestLandscapeLayout(
    state: RexWarpSpeedTestUiState,
    webView: WebView?,
    viewModel: RexWarpSpeedTestViewModel,
    modifier: Modifier = Modifier,
    vpnConnected: Boolean? = null
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.98f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SpeedTestHero(state)
            SpeedTestGauge(state)
        }

        Column(
            modifier = Modifier
                .weight(1.02f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    title = "Download",
                    value = state.downloadMbps?.let(::formatMbps) ?: "—",
                    unit = "Mbps",
                    accent = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                }
                MetricTile(
                    modifier = Modifier.weight(1f),
                    title = "Upload",
                    value = state.uploadMbps?.let(::formatMbps) ?: "—",
                    unit = "Mbps",
                    accent = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    title = "Latency",
                    value = state.latencyMs?.let(::formatDecimal) ?: "—",
                    unit = "ms",
                    accent = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null)
                }
                MetricTile(
                    modifier = Modifier.weight(1f),
                    title = "Jitter",
                    value = state.jitterMs?.let(::formatDecimal) ?: "—",
                    unit = "ms",
                    accent = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Outlined.Timelapse, contentDescription = null)
                }
            }

            TestProgressPanel(state)
            PrimaryTestAction(state) {
                when {
                    state.running -> {
                        CloudflareSpeedTestWebView.cancel(webView)
                        viewModel.onCancelRequested()
                    }

                    state.error != null && !state.engineReady -> {
                        viewModel.onRetryLoading()
                        webView?.reload()
                    }

                    else -> {
                        viewModel.onStartRequested(vpnConnected)
                        CloudflareSpeedTestWebView.start(webView)
                    }
                }
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(tween(220)) + scaleIn(
                    initialScale = 0.97f,
                    animationSpec = tween(220)
                ),
                exit = fadeOut(tween(160))
            ) {
                if (state.error != null) {
                    ErrorPanel(message = state.error!!)
                }
            }

            PrivacyPanel()
            RexWarpSpeedTestHistorySection(viewModel)
            Spacer(Modifier.height(8.dp))
        }
    }
}
