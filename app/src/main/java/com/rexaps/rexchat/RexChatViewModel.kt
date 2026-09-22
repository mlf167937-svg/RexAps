package com.rexaps.rexchat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
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

class RexChatViewModel(
    private val activity: Activity
) : ViewModel() {

    companion object {
        private const val TAG = "RexChat"

        /*
         * WhatsApp Web membutuhkan JavaScript modern.
         * Jangan memalsukan Chrome version secara manual.
         *
         * WebView akan menggunakan User-Agent bawaannya sendiri.
         */
    }

    var progress by mutableIntStateOf(0)
        private set

    var loading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var pageTitle by mutableStateOf("WhatsApp Web")
        private set

    var currentUrl by mutableStateOf(HOME_URL)
        private set

    /**
     * Callback yang diisi oleh RexChatScreen.
     */
    var onPickFiles: (() -> Unit)? = null

    var onNeedPermissions: ((Array<String>) -> Unit)? = null

    /**
     * File picker callback.
     */
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    /**
     * WebView permission callback.
     */
    private var pendingPermission: PermissionRequest? = null

    /**
     * Handler utama.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * WebView.
     *
     * WebView tetap dipertahankan di ViewModel supaya session
     * tidak hilang setiap Compose recomposition.
     */
    val webView: WebView = createWebView(activity)

    // -------------------------------------------------------------------------
    // WEBVIEW
    // -------------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(activity: Activity): WebView {

        /*
         * Aktifkan debugging.
         *
         * Bisa dihapus untuk release.
         */
        WebView.setWebContentsDebuggingEnabled(true)

        val wv = WebView(activity)

        /*
         * ---------------------------------------------------------
         * SETTINGS
         * ---------------------------------------------------------
         */

        wv.settings.apply {

            javaScriptEnabled = true

            javaScriptCanOpenWindowsAutomatically = true

            domStorageEnabled = true

            databaseEnabled = true

            allowContentAccess = true

            /*
             * Jangan gunakan allowFileAccess=true.
             */
            allowFileAccess = false

            /*
             * Cache normal.
             */
            cacheMode = WebSettings.LOAD_DEFAULT

            /*
             * Tampilan desktop-ish.
             */
            useWideViewPort = true
            loadWithOverviewMode = true

            /*
             * WhatsApp Web menggunakan media.
             */
            mediaPlaybackRequiresUserGesture = false

            /*
             * Zoom tidak dibutuhkan.
             */
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false

            /*
             * Jangan override User-Agent.
             *
             * Ini sengaja dibiarkan default.
             *
             * Custom UA sebelumnya bisa membuat WebView
             * mengaku sebagai Chrome desktop yang sebenarnya
             * tidak sama dengan Chromium yang tersedia.
             */

            /*
             * Mixed content.
             *
             * WhatsApp seharusnya HTTPS, tetapi beberapa asset/
             * redirect lama bisa membutuhkan compatibility.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode =
                    WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            /*
             * Text rendering.
             */
            loadsImagesAutomatically = true

            /*
             * API tersedia pada WebView modern.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        /*
         * ---------------------------------------------------------
         * COOKIE
         * ---------------------------------------------------------
         */

        val cookieManager = CookieManager.getInstance()

        cookieManager.setAcceptCookie(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(
                wv,
                true
            )
        }

        /*
         * ---------------------------------------------------------
         * WEBVIEW CLIENT
         * ---------------------------------------------------------
         */

        wv.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                return handleUrl(
                    view,
                    request.url
                )
            }

            @Deprecated("Deprecated in API 24")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {

                return handleUrl(
                    view,
                    Uri.parse(url)
                )
            }

            private fun handleUrl(
                view: WebView,
                uri: Uri
            ): Boolean {

                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase()

                /*
                 * Web URL normal.
                 */
                if (scheme == "https" || scheme == "http") {

                    if (isAllowedWebHost(host)) {
                        return false
                    }

                    /*
                     * Link eksternal dibuka menggunakan aplikasi
                     * Android jika tersedia.
                     */
                    runCatching {

                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            uri
                        )

                        activity.startActivity(intent)

                    }.onFailure {
                        /*
                         * Kalau tidak ada aplikasi handler,
                         * jangan crash.
                         */
                    }

                    return true
                }

                /*
                 * whatsapp://, tel:, mailto:, dll.
                 */
                if (scheme != null) {

                    runCatching {

                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            uri
                        )

                        activity.startActivity(intent)

                    }

                    return true
                }

                return true
            }

            override fun onPageStarted(
                view: WebView,
                url: String?,
                favicon: Bitmap?
            ) {

                super.onPageStarted(
                    view,
                    url,
                    favicon
                )

                loading = true

                error = null

                progress = 0

                currentUrl = url ?: HOME_URL
            }

            override fun onPageFinished(
                view: WebView,
                url: String?
            ) {

                super.onPageFinished(
                    view,
                    url
                )

                loading = false

                progress = 100

                currentUrl = url ?: currentUrl

                CookieManager
                    .getInstance()
                    .flush()

                /*
                 * Pastikan halaman berada pada ukuran
                 * yang nyaman untuk perangkat kecil.
                 */
                injectCompatibilityCss(view)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {

                super.onReceivedError(
                    view,
                    request,
                    error
                )

                /*
                 * Jangan tampilkan error untuk asset kecil.
                 *
                 * Hanya main frame.
                 */
                if (request.isForMainFrame) {

                    loading = false

                    this@RexChatViewModel.error =
                        "Tidak dapat membuka WhatsApp Web.\n\n" +
                        "Periksa koneksi internet lalu coba lagi."
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {

                super.onReceivedHttpError(
                    view,
                    request,
                    errorResponse
                )

                if (request.isForMainFrame) {

                    val status =
                        errorResponse.statusCode

                    /*
                     * Jangan langsung menampilkan error untuk
                     * semua HTTP error.
                     *
                     * Hanya error besar.
                     */
                    if (status >= 500) {

                        loading = false

                        this@RexChatViewModel.error =
                            "Server WhatsApp Web sedang bermasalah.\n\n" +
                            "HTTP $status"
                    }
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: android.webkit.SslErrorHandler,
                error: android.net.http.SslError
            ) {

                /*
                 * JANGAN bypass SSL.
                 *
                 * Kalau certificate invalid, cancel.
                 */
                handler.cancel()

                loading = false

                this@RexChatViewModel.error =
                    "Koneksi HTTPS tidak aman.\n\n" +
                    "Periksa tanggal, waktu, dan koneksi perangkat."
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {

                /*
                 * Renderer Chromium mati.
                 *
                 * Kita tandai error agar user bisa melakukan
                 * recovery.
                 */
                loading = false

                error =
                    "Mesin WebView berhenti.\n\n" +
                    "Ketuk Coba lagi untuk memuat ulang."

                return true
            }
        }

        /*
         * ---------------------------------------------------------
         * CHROME CLIENT
         * ---------------------------------------------------------
         */

        wv.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(
                view: WebView,
                newProgress: Int
            ) {

                progress = newProgress

                if (newProgress >= 100) {
                    loading = false
                }
            }

            override fun onReceivedTitle(
                view: WebView,
                title: String?
            ) {

                super.onReceivedTitle(
                    view,
                    title
                )

                if (!title.isNullOrBlank()) {
                    pageTitle = title
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {

                /*
                 * Callback lama harus dibatalkan.
                 */
                pendingFileCallback
                    ?.onReceiveValue(null)

                pendingFileCallback =
                    filePathCallback

                onPickFiles?.invoke()

                return true
            }

            override fun onPermissionRequest(
                request: PermissionRequest
            ) {

                val needed =
                    request.resources
                        .mapNotNull { resource ->

                            when (resource) {

                                PermissionRequest
                                    .RESOURCE_VIDEO_CAPTURE -> {
                                    Manifest.permission.CAMERA
                                }

                                PermissionRequest
                                    .RESOURCE_AUDIO_CAPTURE -> {
                                    Manifest.permission.RECORD_AUDIO
                                }

                                else -> null
                            }
                        }
                        .distinct()

                /*
                 * Tidak ada permission yang kita dukung.
                 */
                if (needed.isEmpty()) {

                    request.deny()

                    return
                }

                /*
                 * Batalkan request sebelumnya.
                 */
                pendingPermission
                    ?.deny()

                pendingPermission = request

                onNeedPermissions?.invoke(
                    needed.toTypedArray()
                )
            }

            override fun onPermissionRequestCanceled(
                request: PermissionRequest
            ) {

                if (pendingPermission === request) {
                    pendingPermission = null
                }

                super.onPermissionRequestCanceled(
                    request
                )
            }
        }

        /*
         * ---------------------------------------------------------
         * LOAD
         * ---------------------------------------------------------
         */

        wv.loadUrl(HOME_URL)

        return wv
    }

    // -------------------------------------------------------------------------
    // URL
    // -------------------------------------------------------------------------

    private fun isAllowedWebHost(
        host: String?
    ): Boolean {

        if (host == null) {
            return false
        }

        return host == "whatsapp.com" ||
                host.endsWith(".whatsapp.com") ||

                host == "whatsapp.net" ||
                host.endsWith(".whatsapp.net") ||

                host == "facebook.com" ||
                host.endsWith(".facebook.com") ||

                host == "fbcdn.net" ||
                host.endsWith(".fbcdn.net")
    }

    // -------------------------------------------------------------------------
    // FILE PICKER
    // -------------------------------------------------------------------------

    fun onFilesPicked(
        uris: List<Uri>
    ) {

        val callback =
            pendingFileCallback

        pendingFileCallback = null

        callback?.onReceiveValue(
            if (uris.isEmpty()) {
                null
            } else {
                uris.toTypedArray()
            }
        )
    }

    fun cancelFilePicker() {

        pendingFileCallback
            ?.onReceiveValue(null)

        pendingFileCallback = null
    }

    // -------------------------------------------------------------------------
    // PERMISSIONS
    // -------------------------------------------------------------------------

    fun onPermissionsResult(
        granted: Boolean
    ) {

        val request =
            pendingPermission
                ?: return

        pendingPermission = null

        if (granted) {
            request.grant(
                request.resources
            )
        } else {
            request.deny()
        }
    }

    fun cancelPermissionRequest() {

        pendingPermission
            ?.deny()

        pendingPermission = null
    }

    // -------------------------------------------------------------------------
    // NAVIGATION
    // -------------------------------------------------------------------------

    fun canGoBack(): Boolean {
        return webView.canGoBack()
    }

    fun goBack() {

        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    // -------------------------------------------------------------------------
    // RELOAD
    // -------------------------------------------------------------------------

    fun reload() {

        error = null
        loading = true
        progress = 0

        /*
         * Stop request lama terlebih dahulu.
         */
        webView.stopLoading()

        /*
         * Kalau URL hilang, load dari awal.
         */
        if (webView.url.isNullOrBlank()) {

            webView.loadUrl(
                HOME_URL
            )

        } else {

            webView.reload()
        }
    }

    // -------------------------------------------------------------------------
    // SESSION
    // -------------------------------------------------------------------------

    fun clearSession() {

        /*
         * Hentikan halaman sekarang.
         */
        webView.stopLoading()

        /*
         * Clear callback.
         */
        cancelFilePicker()
        cancelPermissionRequest()

        /*
         * Cookies.
         */
        val cookies =
            CookieManager.getInstance()

        cookies.removeAllCookies {
            cookies.flush()
        }

        /*
         * Web storage.
         */
        WebStorage
            .getInstance()
            .deleteAllData()

        /*
         * Cache.
         */
        webView.clearCache(true)

        /*
         * History.
         */
        webView.clearHistory()

        /*
         * Form data.
         */
        webView.clearFormData()

        /*
         * Load kembali.
         */
        error = null
        loading = true
        progress = 0

        webView.loadUrl(
            HOME_URL
        )
    }

    // -------------------------------------------------------------------------
    // CSS COMPATIBILITY
    // -------------------------------------------------------------------------

    private fun injectCompatibilityCss(
        view: WebView
    ) {

        /*
         * Jangan mengubah struktur WhatsApp.
         *
         * Ini hanya menghindari beberapa masalah viewport
         * pada WebView kecil.
         */
        val js = """
            javascript:(function() {
                try {
                    var meta = document.querySelector(
                        'meta[name="viewport"]'
                    );

                    if (!meta) {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content =
                            'width=device-width, initial-scale=1.0, maximum-scale=1.0';
                        document.head.appendChild(meta);
                    }
                } catch(e) {}
            })();
        """.trimIndent()

        runCatching {
            view.evaluateJavascript(
                js,
                null
            )
        }
    }

    // -------------------------------------------------------------------------
    // COOKIE
    // -------------------------------------------------------------------------

    fun flush() {

        runCatching {
            CookieManager
                .getInstance()
                .flush()
        }
    }

    // -------------------------------------------------------------------------
    // CLEANUP
    // -------------------------------------------------------------------------

    override fun onCleared() {

        mainHandler.removeCallbacksAndMessages(
            null
        )

        cancelFilePicker()

        cancelPermissionRequest()

        flush()

        /*
         * Destroy WebView.
         */
        runCatching {
            webView.stopLoading()
            webView.destroy()
        }

        super.onCleared()
    }

    // -------------------------------------------------------------------------
    // FACTORY
    // -------------------------------------------------------------------------

    class Factory(
        private val activity: Activity
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {

            if (
                modelClass.isAssignableFrom(
                    RexChatViewModel::class.java
                )
            ) {
                return RexChatViewModel(
                    activity
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}
