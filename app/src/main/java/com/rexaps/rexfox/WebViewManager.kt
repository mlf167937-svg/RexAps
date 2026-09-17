package com.rexaps.rexfox

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat

class WebViewManager(
    private val activity: Activity,
    private val settingsProvider: () -> BrowserSettings,
    private val onState: (url: String, title: String, progress: Int, loading: Boolean, error: String?) -> Unit,
    private val onDownload: (String, String?, String?, String?) -> Unit,
    private val onNewWindow: (String) -> Unit
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView, incognito: Boolean) {
        val s = settingsProvider()
        webView.settings.apply {
            javaScriptEnabled = s.javaScriptEnabled
            domStorageEnabled = s.domStorageEnabled
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = true
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            safeBrowsingEnabled = true
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, s.thirdPartyCookiesEnabled)
        }

        if (incognito) {
            webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            webView.clearHistory()
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val scheme = request.url.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") return false
                openExternal(request.url)
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                onState(url, view.title.orEmpty(), 0, true, null)
            }

            override fun onPageFinished(view: WebView, url: String) {
                onState(url, view.title.orEmpty(), 100, false, null)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    onState(
                        request.url.toString(),
                        view.title.orEmpty(),
                        0,
                        false,
                        error.description?.toString() ?: "Unable to load this page."
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: android.webkit.SslErrorHandler,
                error: android.net.http.SslError
            ) {
                handler.cancel()
                onState(
                    view.url.orEmpty(),
                    view.title.orEmpty(),
                    0,
                    false,
                    "Secure connection could not be verified."
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                onState(view.url.orEmpty(), view.title.orEmpty(), progress, progress < 100, null)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                onState(view.url.orEmpty(), title, 100, false, null)
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                // target=_blank is intentionally supported through a new RexFox tab.
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                val child = WebView(activity)
                configure(child, false)
                child.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) {
                        onNewWindow(url)
                    }
                }
                transport.webView = child
                resultMsg.sendToTarget()
                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            onDownload(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun openExternal(uri: Uri) {
        runCatching {
            val intent = if (uri.scheme.equals("intent", true)) {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            ContextCompat.startActivity(activity, intent, null)
        }
    }

    fun destroy(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.webChromeClient = null
        webView.webViewClient = null
        webView.destroy()
    }
}
