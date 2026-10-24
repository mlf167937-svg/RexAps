package com.rexaps.rexchat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat

@SuppressLint("SetJavaScriptEnabled")
    internal fun WaController.__split_createWebView_1(): WebView {
        WebView.setWebContentsDebuggingEnabled(true)
        val wv = WebView(activity)
        wv.setBackgroundColor(android.graphics.Color.WHITE)

        // Data terpisah per session, harus sebelum load
        if (!legacy) {
            runCatching {
                val name = SessionManager.profileName(sessionId)
                ProfileStore.getInstance().getOrCreateProfile(name)
                WebViewCompat.setProfile(wv, name)
            }.onFailure {
                error = "Isolasi profil gagal: ${it.message}"
            }
        }

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            setSupportMultipleWindows(false)

            // UA Chrome desktop (Windows), versi mengikuti WebView terpasang
            val major = Regex("Chrome/(\\d+)").find(userAgentString)
                ?.groupValues?.get(1) ?: "124"
            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/$major.0.0.0 Safari/537.36"
        }

        cookies().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                if (scheme == "blob" || scheme == "data" || scheme == "about") return false
                if ((scheme == "https" || scheme == "http") && isAllowedHost(uri.host)) {
                    return false
                }
                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                loading = true
                error = null
                progress = 0
            }

            override fun onPageFinished(view: WebView, url: String?) {
                loading = false
                progress = 100
                flush()
                applyViewport()
                // Cek halaman kosong setelah 8 detik
                handler.postDelayed({ checkBlank() }, 8000)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                err: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    loading = false
                    error = "Tidak bisa membuka WhatsApp Web.\n\n" +
                            "${err.description}\n\nPeriksa koneksi internet lalu coba lagi."
                } else {
                    addLog(netLog, "ERR ${err.description} ${request.url.host}")
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                resp: WebResourceResponse
            ) {
                addLog(netLog, "HTTP ${resp.statusCode} ${request.url.host}${request.url.path}")
            }

            override fun onReceivedSslError(
                view: WebView,
                h: SslErrorHandler,
                e: android.net.http.SslError
            ) {
                h.cancel()
                loading = false
                error = "Koneksi HTTPS tidak aman.\n\nPeriksa tanggal, waktu, dan jaringan."
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                loading = false
                error = "WebView berhenti (memori habis?).\n\nKetuk Coba lagi."
                return true
            }
        }

        wv.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress = newProgress
                if (newProgress >= 100) loading = false
            }

            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    addLog(consoleLog, m.message())
                }
                return true
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback
                val accept = params.acceptTypes?.firstOrNull().orEmpty()
                val mime =
                    if (accept.isBlank() || accept.contains(",") || accept.startsWith("."))
                        "*/*"
                    else accept
                onPickFiles?.invoke(mime)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                activity.runOnUiThread {
                    if (!isAllowedHost(request.origin.host)) {
                        request.deny()
                        return@runOnUiThread
                    }
                    val supported = request.resources.filter {
                        it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                                it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    }
                    if (supported.isEmpty()) {
                        request.deny()
                        return@runOnUiThread
                    }
                    val missing = supported.map {
                        if (it == PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                            Manifest.permission.CAMERA
                        else Manifest.permission.RECORD_AUDIO
                    }.filter {
                        ContextCompat.checkSelfPermission(activity, it) !=
                                PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isEmpty()) {
                        request.grant(supported.toTypedArray())
                        return@runOnUiThread
                    }
                    pendingPermission?.deny()
                    pendingPermission = request
                    onNeedPermissions?.invoke(missing.toTypedArray())
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingPermission === request) pendingPermission = null
            }
        }

        wv.loadUrl(HOME_URL)
        return wv
    }

internal fun WaController.__split_isAllowedHost_2(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return listOf("whatsapp.com", "whatsapp.net", "facebook.com", "fbcdn.net")
            .any { h == it || h.endsWith(".$it") }
    }

internal fun WaController.__split_cookies_3(): CookieManager {
        if (!legacy) {
            runCatching {
                ProfileStore.getInstance()
                    .getProfile(SessionManager.profileName(sessionId))
                    ?.cookieManager
            }.getOrNull()?.let { return it }
        }
        return CookieManager.getInstance()
    }
