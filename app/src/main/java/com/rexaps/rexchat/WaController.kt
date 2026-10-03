package com.rexaps.rexchat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import org.json.JSONArray
import org.json.JSONObject

enum class Login { LOADING, QR, CODE, CHATS }

sealed interface Pairing {
    object Idle : Pairing
    object Working : Pairing
    data class Code(val code: String) : Pairing
    data class Failed(val message: String) : Pairing
}

private const val HOME_URL = "https://web.whatsapp.com/"

class WaController(
    private val activity: Activity,
    val sessionId: Int,
    private val legacy: Boolean,
    private val initialMethod: String
) {

    // ---------- UI state ----------
    var progress by mutableIntStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pageTitle by mutableStateOf("WhatsApp Web")
        private set
    var login by mutableStateOf(Login.LOADING)
        private set
    var desktopLayout by mutableStateOf(false)
        private set
    var qrBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var qrDialogOpen by mutableStateOf(false)
    var askPhone by mutableStateOf(false)
    var pairing by mutableStateOf<Pairing>(Pairing.Idle)

    // ---------- callbacks to Activity ----------
    var onPickFiles: ((String) -> Unit)? = null
    var onNeedPermissions: ((Array<String>) -> Unit)? = null

    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermission: PermissionRequest? = null

    private val handler = Handler(Looper.getMainLooper())
    private var destroyed = false
    private var methodConsumed = false
    private var pairToken = 0

    // =====================================================================
    // Login state polling
    // HARUS dideklarasikan SEBELUM webView dan init
    // =====================================================================

    private val poller = object : Runnable {
        override fun run() {
            if (destroyed) return
            if (error == null && webView.url?.contains("whatsapp") == true) probe()
            handler.postDelayed(this, 1500)
        }
    }

    // =====================================================================
    // WEBVIEW
    // =====================================================================

    val webView: WebView = createWebView()

    init {
        handler.postDelayed(poller, 1500)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        WebView.setWebContentsDebuggingEnabled(false)
        val wv = WebView(activity)

        // Data terisolasi per session (harus sebelum memuat apa pun)
        if (!legacy) {
            runCatching {
                val name = SessionManager.profileName(sessionId)
                ProfileStore.getInstance().getOrCreateProfile(name)
                WebViewCompat.setProfile(wv, name)
            }.onFailure {
                error = "Profile isolation failed: ${it.message}"
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
            useWideViewPort = true
            loadWithOverviewMode = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            safeBrowsingEnabled = true
            setSupportMultipleWindows(false)

            // WhatsApp Web menolak UA mobile -> pakai UA desktop Chrome
            val chromeMajor =
                Regex("Chrome/(\\d+)").find(userAgentString)?.groupValues?.get(1) ?: "124"
            userAgentString =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/$chromeMajor.0.0.0 Safari/537.36"
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
                if ((scheme == "https" || scheme == "http") && isAllowedHost(uri.host)) return false
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
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                err: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    loading = false
                    error = "Can't open WhatsApp Web.\n\nCheck your internet connection and try again."
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                h: SslErrorHandler,
                e: android.net.http.SslError
            ) {
                h.cancel()
                loading = false
                error = "Insecure HTTPS connection.\n\nCheck your device date, time and network."
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                loading = false
                error = "The WebView engine stopped.\n\nTap Try again to reload."
                return true
            }
        }

        wv.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress = newProgress
                if (newProgress >= 100) loading = false
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!title.isNullOrBlank()) pageTitle = title
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
                    val needed = supported.map {
                        if (it == PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                            Manifest.permission.CAMERA
                        else Manifest.permission.RECORD_AUDIO
                    }
                    val missing = needed.filter {
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
    // JS bridge
    // =====================================================================

    private fun eval(expr: String, cb: (String) -> Unit) {
        if (destroyed) return
        webView.evaluateJavascript(WaScript.JS + "\n" + expr) { raw -> cb(unquote(raw)) }
    }

    private fun unquote(raw: String?): String {
        if (raw == null || raw == "null") return ""
        return if (raw.startsWith("\"")) {
            runCatching { JSONArray("[$raw]").getString(0) }.getOrDefault("")
        } else raw
    }

    private fun applyViewport() {
        eval("__rex.viewport(${if (desktopLayout) 1100 else 0})") {}
    }

    private fun probe() {
        eval("__rex.state()") { s ->
            val state = when (s) {
                "chats" -> Login.CHATS
                "qr" -> Login.QR
                "code" -> Login.CODE
                else -> Login.LOADING
            }
            val prev = login
            login = state

            if (state == Login.CHATS && prev != Login.CHATS) {
                SessionManager.markLinked(activity, sessionId, true)
                qrDialogOpen = false
                askPhone = false
                if (pairing is Pairing.Code || pairing is Pairing.Working) {
                    pairing = Pairing.Idle
                }
            }

            if (state == Login.QR && !methodConsumed) {
                methodConsumed = true
                when (initialMethod) {
                    "qr" -> qrDialogOpen = true
                    "pair" -> askPhone = true
                }
            }

            if (qrDialogOpen && state == Login.QR) fetchQr()
            applyViewport()
        }
    }

    private fun fetchQr() {
        eval("__rex.qr()") { data ->
            if (!data.startsWith("data:image")) return@eval
            runCatching {
                val bytes = Base64.decode(data.substringAfter("base64,"), Base64.DEFAULT)
                val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@eval
                val size = 720
                val pad = 48
                val out = Bitmap.createBitmap(
                    size + pad * 2,
                    size + pad * 2,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(out)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(src, size, size, false),
                    pad.toFloat(),
                    pad.toFloat(),
                    null
                )
                qrBitmap = out
            }
        }
    }

    // =====================================================================
    // Pairing by phone number
    // =====================================================================

    fun startPairing(raw: String) {
        val digits = raw.filter { it.isDigit() }
        if (digits.length !in 8..15) {
            pairing = Pairing.Failed(
                "Enter the full number with country code, e.g. +62 812 3456 7890."
            )
            return
        }
        if (login == Login.CHATS) {
            pairing = Pairing.Failed("This session is already linked.")
            return
        }

        val token = ++pairToken
        pairing = Pairing.Working
        var attempts = 0

        fun fail() {
            if (token == pairToken) {
                pairing = Pairing.Failed(
                    "Couldn't finish automatically. Close this and continue on the " +
                            "WhatsApp Web page: tap \"Link with phone number\" and enter your number."
                )
            }
        }

        fun step(phase: Int) {
            if (token != pairToken || destroyed) return
            if (attempts++ > 45) {
                fail()
                return
            }
            when (phase) {
                0 -> eval("__rex.openPhone()") { r ->
                    if (r == "ready") step(1)
                    else handler.postDelayed({ step(0) }, 800)
                }

                1 -> eval("__rex.fill(${JSONObject.quote("+$digits")})") { r ->
                    if (r == "ok") {
                        handler.postDelayed({ eval("__rex.next()") { step(2) } }, 700)
                    } else {
                        handler.postDelayed({ step(1) }, 600)
                    }
                }

                2 -> eval("__rex.code()") { r ->
                    val clean = r.filter { it.isLetterOrDigit() }.uppercase()
                    if (clean.length == 8) {
                        if (token == pairToken) {
                            pairing = Pairing.Code(
                                clean.substring(0, 4) + "-" + clean.substring(4)
                            )
                        }
                    } else {
                        handler.postDelayed({ step(2) }, 1000)
                    }
                }
            }
        }

        step(0)
    }

    fun cancelPairing() {
        pairToken++
        pairing = Pairing.Idle
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
        if (webView.url.isNullOrBlank()) webView.loadUrl(HOME_URL) else webView.reload()
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
        pairToken++
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