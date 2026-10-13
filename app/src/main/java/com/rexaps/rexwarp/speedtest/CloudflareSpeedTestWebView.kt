package com.rexaps.rexwarp.speedtest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.rexaps.rexwarp.RexWarpSpeedTestViewModel

/** WebView tersembunyi menjadi runtime JS. Semua UI yang dilihat pengguna dibuat dengan Compose. */
object CloudflareSpeedTestWebView {
    private const val BASE_HOST = "appassets.androidplatform.net"

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, viewModel: RexWarpSpeedTestViewModel): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 0f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            addJavascriptInterface(CloudflareSpeedTestBridge(viewModel), "RexWarpBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Dokumen utama hanya boleh berada pada origin internal; CDN tetap dapat dimuat sebagai module.
                    return request.url.host != BASE_HOST
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame) {
                        viewModel.onEngineError("Dokumen mesin pengukuran gagal dimuat. Periksa koneksi internet.")
                    }
                }
            }
            loadDataWithBaseURL("https://$BASE_HOST/", HTML, "text/html", "UTF-8", null)
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
          const phaseNames = {
            latency: 'Mengukur latency', download: 'Mengukur download', upload: 'Mengukur upload',
            packetLoss: 'Mengukur packet loss', latencyUnderLoad: 'Mengukur latency saat beban'
          };
          const safeCall = (obj, name) => {
            try { return obj && typeof obj[name] === 'function' ? obj[name]() : undefined; }
            catch (_) { return undefined; }
          };
          const toPayload = (results) => {
            const downloadBps = safeCall(results, 'getDownloadBandwidth');
            const uploadBps = safeCall(results, 'getUploadBandwidth');
            const summary = safeCall(results, 'getSummary') || {};
            const latency = safeCall(results, 'getUnloadedLatency');
            const jitter = safeCall(results, 'getUnloadedJitter');
            const duration = safeCall(results, 'getTotalDurationMs');
            return {
              downloadBps: Number.isFinite(downloadBps) ? downloadBps : (Number.isFinite(summary.download) ? summary.download * 1000000 : null),
              uploadBps: Number.isFinite(uploadBps) ? uploadBps : (Number.isFinite(summary.upload) ? summary.upload * 1000000 : null),
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
              engine.onResultsChange = ({ type } = {}) => {
                const order = ['latency', 'download', 'latency', 'download', 'upload', 'upload', 'download', 'upload'];
                const index = Math.max(0, order.indexOf(type));
                send('onPhase', { index: index + 1, count: order.length, phase: phaseNames[type] || 'Mengukur kualitas koneksi…' });
                send('onResults', toPayload(engine.results));
              };
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
