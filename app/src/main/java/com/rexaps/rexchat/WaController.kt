package com.rexaps.rexchat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat

enum class Login { LOADING, LOGGED_OUT, CHATS }

private const val HOME_URL = "https://web.whatsapp.com/"

class WaController(
    private val activity: Activity,
    val sessionId: Int,
    private val legacy: Boolean,
    @Suppress("unused") private val initialMethod: String
) {

    // ---------- UI state ----------
    var progress by mutableIntStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var login by mutableStateOf(Login.LOADING)
        private set
    var desktopLayout by mutableStateOf(false)
        private set
    var hintDismissed by mutableStateOf(false)

    // ---------- callbacks ke Activity ----------
    var onPickFiles: ((String) -> Unit)? = null
    var onNeedPermissions: ((Array<String>) -> Unit)? = null

    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermission: PermissionRequest? = null

    private val handler = Handler(Looper.getMainLooper())
    private var destroyed = false

    // Log diagnosis
    private val consoleLog = ArrayDeque<String>()
    private val netLog = ArrayDeque<String>()

    private fun addLog(list: ArrayDeque<String>, s: String) {
        list.addLast(s.take(160))
        while (list.size > 6) list.removeFirst()
    }

    // poller harus dideklarasikan SEBELUM webView dan init
    private val poller = object : Runnable {
        override fun run() {
            if (destroyed) return
            probe()
            handler.postDelayed(this, 2000)
        }
    }

    val webView: WebView = createWebView()

    init {
        handler.postDelayed(poller, 2000)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
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

    private fun isAllowedHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return listOf("whatsapp.com", "whatsapp.net", "facebook.com", "fbcdn.net")
            .any { h == it || h.endsWith(".$it") }
    }

    private fun cookies(): CookieManager {
        if (!legacy) {
            runCatching {
                ProfileStore.getInstance()
                    .getProfile(SessionManager.profileName(sessionId))
                    ?.cookieManager
            }.getOrNull()?.let { return it }
        }
        return CookieManager.getInstance()
    }

    // =====================================================================
    // Diagnosis halaman kosong
    // =====================================================================

    private fun checkBlank() {
        if (destroyed || error != null) return
        webView.evaluateJavascript(
            "(function(){var b=document.body;if(!b)return 'nobody';" +
                    "var t=(b.innerText||'').trim().length;" +
                    "var c=document.querySelectorAll('canvas,#app,#pane-side,input').length;" +
                    "return t+'|'+c+'|'+document.readyState;})()"
        ) { raw ->
            if (destroyed || error != null) return@evaluateJavascript
            val v = raw?.trim('"') ?: ""
            val parts = v.split("|")
            val text = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val nodes = parts.getOrNull(1)?.toIntOrNull() ?: 0
            if (v == "nobody" || (text < 5 && nodes == 0)) {
                val sb = StringBuilder("Halaman WhatsApp Web kosong.\n\n")
                sb.append("UA: ").append(webView.settings.userAgentString.takeLast(40)).append("\n")
                sb.append("Hasil: ").append(v).append("\n\n")
                if (consoleLog.isNotEmpty()) {
                    sb.append("Error JS:\n").append(consoleLog.joinToString("\n")).append("\n\n")
                }
                if (netLog.isNotEmpty()) {
                    sb.append("Jaringan:\n").append(netLog.joinToString("\n")).append("\n\n")
                }
                sb.append("Update Android System WebView & Chrome di Play Store, lalu coba lagi. ")
                sb.append("Kirim teks ini ke developer.")
                error = sb.toString()
            }
        }
    }

    // =====================================================================
    // Deteksi login
    // =====================================================================

    private fun probe() {
        if (error != null) return
        val url = webView.url ?: return
        if (!url.contains("whatsapp")) return

        webView.evaluateJavascript(
            "(function(){return document.querySelector('#pane-side')?'chats':'out';})()"
        ) { raw ->
            if (destroyed) return@evaluateJavascript
            if (raw != null && raw.contains("chats")) {
                if (login != Login.CHATS) {
                    login = Login.CHATS
                    SessionManager.markLinked(activity, sessionId, true)
                }
            } else {
                login = if (loading) Login.LOADING else Login.LOGGED_OUT
            }
        }
    }

    private fun applyViewport() {
        val content = if (desktopLayout) "width=1100" else "width=device-width, initial-scale=1"
        webView.evaluateJavascript(
            "(function(){var m=document.querySelector('meta[name=viewport]');" +
                    "if(!m){m=document.createElement('meta');m.name='viewport';" +
                    "document.head.appendChild(m);}m.content='$content';})()",
            null
        )
    }

    // =====================================================================
    // Actions
    // =====================================================================

    fun toggleDesktopLayout() {
        desktopLayout = !desktopLayout
        applyViewport()
    }

    fun reload() {
        error = null
        loading = true
        progress = 0
        webView.stopLoading()
        webView.loadUrl(HOME_URL)
    }

    fun canGoBack() = webView.canGoBack()
    fun goBack() = webView.goBack()

    fun onFilesPicked(uris: List<Uri>) {
        val cb = pendingFileCallback
        pendingFileCallback = null
        cb?.onReceiveValue(if (uris.isEmpty()) null else uris.toTypedArray())
    }

    fun onPermissionsResult(granted: Boolean) {
        val req = pendingPermission ?: return
        pendingPermission = null
        if (granted) {
            req.grant(
                req.resources.filter {
                    it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                }.toTypedArray()
            )
        } else {
            req.deny()
        }
    }

    fun flush() {
        runCatching { cookies().flush() }
    }

    fun onPause() {
        webView.onPause()
        flush()
    }

    fun onResume() {
        webView.onResume()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        pendingPermission?.deny()
        pendingPermission = null
        flush()
        runCatching {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}