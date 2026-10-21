package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel
import com.rexaps.rexwarp.speedtest.CloudflareSpeedTestWebView

/** Owns the WebView and test lifecycle so rotating the device does not cancel a running test. */
@Composable
fun RexWarpSpeedTestScreen(
    modifier: Modifier = Modifier,
    viewModel: RexWarpSpeedTestViewModel = viewModel()
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var webView by remember(viewModel) { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0f),
        factory = { context -> CloudflareSpeedTestWebView.create(context, viewModel).also { webView = it } },
        update = { if (webView !== it) webView = it }
    )
    DisposableEffect(viewModel) {
        onDispose {
            CloudflareSpeedTestWebView.cancel(webView)
            webView?.stopLoading()
            webView?.removeJavascriptInterface("RexWarpBridge")
            webView?.destroy()
            webView = null
            viewModel.onScreenDisposed()
        }
    }

    if (landscape) RexWarpSpeedTestScreenLandscape(state, webView, viewModel, modifier)
    else RexWarpSpeedTestScreenPortrait(state, webView, viewModel, modifier)
}
