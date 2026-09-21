package com.rexaps.rexchat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

private const val HOME_URL = "https://web.whatsapp.com/"
private const val DESKTOP_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

class RexChatViewModel(activity: Activity) : ViewModel() {

    var progress by mutableIntStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Diisi oleh layar: membuka pemilih file / meminta izin. */
    var onPickFiles: (() -> Unit)? = null
    var onNeedPermissions: ((Array<String>) -> Unit)? = null

    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermission: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = createWebView(activity)

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(activity: Activity): WebView {
        val wv = WebView(activity)

        // Ambil versi Chrome asli dari WebView di HP, supaya UA selalu cocok dan tidak usang.
        val chromeVersion = Regex("""Chrome/([\d.]+)""")
            .find(wv.settings.userAgentString)?.groupValues?.get(1) ?: "138.0.0.0"
        val desktopUa =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$chromeVersion Safari/537.36"

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // sesi WA Web disimpan di sini
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = desktopUa
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
        }

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(wv, true)

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                if (isAllowed(uri)) return false
                // Di luar WhatsApp: buka di aplikasi lain, bukan di dalam RexChat.
                runCatching {
                    view.context.startActivity(
                        Intent(Intent.ACTION_VIEW, uri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                loading = true
                error = null
            }

            override fun onPageFinished(view: WebView, url: String) {
                loading = false
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                err: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    error = "Tidak bisa terhubung ke WhatsApp Web. Cek koneksi internetmu."
                    loading = false
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress = newProgress
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                storeFileCallback(filePathCallback)
                onPickFiles?.invoke()
                return true
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: android.webkit.RenderProcessGoneDetail
            ): Boolean {
                error = "Tampilan WhatsApp berhenti. Ketuk Coba lagi."
                loading = false
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                val needed = request.resources.mapNotNull {
                    when (it) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                            android.Manifest.permission.CAMERA
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                            android.Manifest.permission.RECORD_AUDIO
                        else -> null
                    }
                }
                if (needed.isEmpty()) {
                    request.deny()
                    return
                }
                storePermissionRequest(request)
                onNeedPermissions?.invoke(needed.toTypedArray())
            }
        }

        wv.loadUrl(HOME_URL)
        return wv
    }

    private fun storeFileCallback(callback: ValueCallback<Array<Uri>>) {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = callback
    }

    private fun storePermissionRequest(request: PermissionRequest) {
        pendingPermission?.deny()
        pendingPermission = request
    }

    private fun isAllowed(uri: Uri): Boolean {
        if (uri.scheme != "https") return false
        val host = uri.host ?: return false
        return host == "whatsapp.com" || host.endsWith(".whatsapp.com") ||
            host.endsWith(".whatsapp.net") || host.endsWith(".facebook.com")
    }

    fun onFilesPicked(uris: List<Uri>) {
        pendingFileCallback?.onReceiveValue(
            if (uris.isEmpty()) null else uris.toTypedArray()
        )
        pendingFileCallback = null
    }

    fun onPermissionsResult(granted: Boolean) {
        val req = pendingPermission ?: return
        if (granted) req.grant(req.resources) else req.deny()
        pendingPermission = null
    }

    fun reload() {
        error = null
        if (webView.url == null) webView.loadUrl(HOME_URL) else webView.reload()
    }

    /** Logout bersih: hapus cookie dan penyimpanan WA Web. Perlu scan/pairing lagi. */
    fun clearSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)
        webView.loadUrl(HOME_URL)
    }

    fun flush() = CookieManager.getInstance().flush()

    override fun onCleared() {
        flush()
        webView.destroy()
        super.onCleared()
    }

    class Factory(private val activity: Activity) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RexChatViewModel(activity) as T
    }
}
