package com.rexaps.rexwarp.speedtest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel

/** WebView hanya menjadi runtime mesin JS; antarmuka yang terlihat tetap Compose native. */
object CloudflareSpeedTestWebView {
    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, viewModel: RexWarpSpeedTestViewModel): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 0f
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            addJavascriptInterface(CloudflareSpeedTestBridge(viewModel), "RexWarpBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Dokumen pengukuran tidak boleh menavigasi ke situs eksternal.
                    return request.url.host != "appassets.androidplatform.net"
                }
            }
            loadDataWithBaseURL(
                "https://appassets.androidplatform.net/",
                HTML,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    fun start(webView: WebView?) {
        webView?.evaluateJavascript("window.rexWarpCloudflareTest && window.rexWarpCloudflareTest.start()", null)
    }

    fun cancel(webView: WebView?) {
        webView?.evaluateJavascript("window.rexWarpCloudflareTest && window.rexWarpCloudflareTest.cancel()", null)
    }

    private val HTML = """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <meta http-equiv="Content-Security-Policy" content="default-src 'self' https: data:; script-src 'self' https://esm.sh 'unsafe-inline'; connect-src https: wss:; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'">
          <title>RexWarp Cloudflare Measurement Engine</title>
        </head>
        <body>
        <script type="module">
          const send = (name, value) => {
            try {
              const bridge = window.RexWarpBridge;
              if (!bridge || typeof bridge[name] !== 'function') return;
              if (value === undefined) bridge[name]();
              else if (typeof value === 'string' || typeof value === 'boolean' || typeof value === 'number') bridge[name](value);
              else bridge[name](JSON.stringify(value));
            } catch (_) {}
          };
          let engine = null;
          let phaseIndex = 0;
          const phaseNames = {
            latency: 'Mengukur latency', download: 'Mengukur download', upload: 'Mengukur upload',
            packetLoss: 'Mengukur packet loss', latencyUnderLoad: 'Mengukur latency saat beban'
          };
          const safeCall = (obj, name) => {
            try { return typeof obj[name] === 'function' ? obj[name]() : undefined; }
            catch (_) { return undefined; }
          };
          const toPayload = (results) => {
            const summary = safeCall(results, 'getSummary') || {};
            const latency = safeCall(results, 'getUnloadedLatency');
            const jitter = safeCall(results, 'getUnloadedJitter');
            const duration = safeCall(results, 'getTotalDurationMs');
            return {
              downloadBps: Number.isFinite(summary.download) ? summary.download : null,
              uploadBps: Number.isFinite(summary.upload) ? summary.upload : null,
              latencyMs: Number.isFinite(latency) ? latency : (Number.isFinite(summary.latency) ? summary.latency : null),
              jitterMs: Number.isFinite(jitter) ? jitter : (Number.isFinite(summary.jitter) ? summary.jitter : null),
              durationMs: Number.isFinite(duration) ? duration : null
            };
          };
          async function createEngine() {
            try {
              const module = await import('https://esm.sh/@cloudflare/speedtest@1.14.1');
              const SpeedTest = module.default;
              if (typeof SpeedTest !== 'function') throw new Error('Modul Speed Test Cloudflare tidak tersedia.');
              engine = new SpeedTest({
                autoStart: false,
                // Profil mobile yang lebih hemat data dibanding konfigurasi default.
                // Packet loss tidak diaktifkan karena layanan TURN publik Cloudflare telah ditandai deprecated.
                measurements: [
                  { type: 'latency', numPackets: 1 },
                  { type: 'download', bytes: 100000, count: 1, bypassMinDuration: true },
                  { type: 'latency', numPackets: 10 },
                  { type: 'download', bytes: 1000000, count: 4 },
                  { type: 'upload', bytes: 100000, count: 3 },
                  { type: 'upload', bytes: 1000000, count: 3 },
                  { type: 'download', bytes: 10000000, count: 2 },
                  { type: 'upload', bytes: 10000000, count: 2 }
                ]
              });
              engine.onRunningChange = running => send('onRunning', running);
              engine.onPhaseChange = payload => {
                phaseIndex = Number(payload.measurementId || 0) + 1;
                const type = payload.measurement && payload.measurement.type;
                send('onPhase', { index: phaseIndex, count: 8, phase: phaseNames[type] || 'Mengukur kualitas koneksi…' });
              };
              engine.onResultsChange = () => send('onResults', toPayload(engine.results));
              engine.onFinish = results => send('onFinished', toPayload(results));
              engine.onError = error => send('onTestError', String(error || 'Pengukuran gagal.'));
              send('onEngineReady');
            } catch (error) {
              send('onEngineError', String(error && error.message ? error.message : error));
            }
          }
          window.rexWarpCloudflareTest = {
            start() {
              if (!engine) { send('onEngineError', 'Mesin Cloudflare belum siap. Periksa koneksi internet.'); return; }
              try { engine.restart(); engine.play(); }
              catch (error) { send('onTestError', String(error)); }
            },
            cancel() {
              if (!engine) return;
              try { engine.pause(); send('onRunning', false); }
              catch (_) {}
            }
          };
          createEngine();
        </script>
        </body>
        </html>
    """.trimIndent()
}
