package com.rexaps.rexwarp.speedtest

import android.webkit.JavascriptInterface
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel

/** Bridge terbatas pada callback pengukuran; tidak mengekspos API native umum ke JavaScript. */
class CloudflareSpeedTestBridge(private val viewModel: RexWarpSpeedTestViewModel) {
    @JavascriptInterface fun onEngineReady() = viewModel.onEngineReady()
    @JavascriptInterface fun onEngineError(message: String) = viewModel.onEngineError(message)
    @JavascriptInterface fun onRunning(running: Boolean) = viewModel.onRunning(running)
    @JavascriptInterface fun onPhase(payload: String) = viewModel.onPhase(payload)
    @JavascriptInterface fun onResults(payload: String) = viewModel.onResults(payload)
    @JavascriptInterface fun onFinished(payload: String) = viewModel.onResults(payload, completed = true)
    @JavascriptInterface fun onTestError(message: String) = viewModel.onTestError(message)
}
