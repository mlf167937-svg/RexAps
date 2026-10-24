package com.rexaps.rexwarp.ui

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rexaps.rexwarp.RexWarpSpeedTestUiState
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel

@Composable
internal fun RexWarpSpeedTestScreenLandscape(
    state: RexWarpSpeedTestUiState,
    webView: WebView?,
    viewModel: RexWarpSpeedTestViewModel,
    modifier: Modifier = Modifier,
    vpnConnected: Boolean? = null
) {
    RexWarpSpeedTestLandscapeLayout(state, webView, viewModel, modifier, vpnConnected)
}
