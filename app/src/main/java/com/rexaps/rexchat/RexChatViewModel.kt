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
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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

private const val HOME_URL =
    "https://web.whatsapp.com/"

class RexChatViewModel(
    private val activity: Activity,
    private val sessionId: Int
) : ViewModel() {

    /*
     * ---------------------------------------------------------
     * STATE
     * ---------------------------------------------------------
     */

    var progress by mutableIntStateOf(0)
        private set

    var loading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var pageTitle by mutableStateOf(
        "WhatsApp Web"
    )
        private set

    var currentUrl by mutableStateOf(
        HOME_URL
    )
        private set

    /*
     * ---------------------------------------------------------
     * CALLBACK
     * ---------------------------------------------------------
     */

    var onPickFiles:
            (() -> Unit)? = null

    var onNeedPermissions:
            ((Array<String>) -> Unit)? = null

    /*
     * ---------------------------------------------------------
     * PENDING CALLBACKS
     * ---------------------------------------------------------
     */

    private var pendingFileCallback:
            ValueCallback<Array<Uri>>? = null

    private var pendingPermission:
            PermissionRequest? = null

    /*
     * ---------------------------------------------------------
     * HANDLER
     * ---------------------------------------------------------
     */

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    /*
     * ---------------------------------------------------------
     * WEBVIEW
     * ---------------------------------------------------------
     */

    val webView: WebView =
        createWebView(
            activity
        )

    // =====================================================================
    // CREATE WEBVIEW
    // =====================================================================

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(
        activity: Activity
    ): WebView {

        /*
         * Debugging.
         *
         * Sebaiknya false untuk release.
         */
        WebView.setWebContentsDebuggingEnabled(
            true
        )

        val wv =
            WebView(activity)

        /*
         * =====================================================
         * SETTINGS
         * =====================================================
         */

        wv.settings.apply {

            javaScriptEnabled = true

            javaScriptCanOpenWindowsAutomatically =
                true

            domStorageEnabled =
                true

            databaseEnabled =
                true

            allowContentAccess =
                true

            allowFileAccess =
                false

            cacheMode =
                WebSettings.LOAD_DEFAULT

            useWideViewPort =
                true

            loadWithOverviewMode =
                true

            mediaPlaybackRequiresUserGesture =
                false

            setSupportZoom(
                false
            )

            builtInZoomControls =
                false

            displayZoomControls =
                false

            loadsImagesAutomatically =
                true

            /*
             * Jangan spoof User-Agent.
             */

            /*
             * HTTPS compatibility.
             */
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP
            ) {

                mixedContentMode =
                    WebSettings
                        .MIXED_CONTENT_COMPATIBILITY_MODE
            }

            /*
             * Safe Browsing.
             */
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                safeBrowsingEnabled =
                    true
            }
        }

        /*
         * =====================================================
         * COOKIE
         * =====================================================
         */

        val cookieManager =
            CookieManager.getInstance()

        cookieManager.setAcceptCookie(
            true
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.LOLLIPOP
        ) {

            cookieManager
                .setAcceptThirdPartyCookies(
                    wv,
                    true
                )
        }

        /*
         * =====================================================
         * WEBVIEW CLIENT
         * =====================================================
         */

        wv.webViewClient =
            object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {

                    return handleUrl(
                        view,
                        request.url
                    )
                }

                @Deprecated(
                    "Deprecated in API 24"
                )
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

                    val scheme =
                        uri.scheme
                            ?.lowercase()

                    val host =
                        uri.host
                            ?.lowercase()

                    /*
                     * =================================================
                     * WEB
                     * =================================================
                     */

                    if (
                        scheme == "https" ||
                        scheme == "http"
                    ) {

                        if (
                            isAllowedWebHost(
                                host
                            )
                        ) {

                            return false
                        }

                        /*
                         * External website.
                         */

                        runCatching {

                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    uri
                                )

                            activity.startActivity(
                                intent
                            )
                        }

                        return true
                    }

                    /*
                     * =================================================
                     * OTHER SCHEMES
                     * =================================================
                     */

                    if (
                        scheme != null
                    ) {

                        runCatching {

                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    uri
                                )

                            activity.startActivity(
                                intent
                            )
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

                    loading =
                        true

                    error =
                        null

                    progress =
                        0

                    currentUrl =
                        url ?: HOME_URL
                }

                override fun onPageFinished(
                    view: WebView,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )

                    loading =
                        false

                    progress =
                        100

                    currentUrl =
                        url ?: currentUrl

                    CookieManager
                        .getInstance()
                        .flush()

                    injectCompatibilityCss(
                        view
                    )
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

                    if (
                        request.isForMainFrame
                    ) {

                        loading =
                            false

                        this@RexChatViewModel.error =
                            "Tidak dapat membuka " +
                                    "WhatsApp Web.\n\n" +
                                    "Periksa koneksi internet " +
                                    "lalu coba lagi."
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse
                ) {

                    super.onReceivedHttpError(
                        view,
                        request,
                        errorResponse
                    )

                    if (
                        request.isForMainFrame
                    ) {

                        val status =
                            errorResponse.statusCode

                        if (
                            status >= 500
                        ) {

                            loading =
                                false

                            this@RexChatViewModel.error =
                                "Server WhatsApp Web " +
                                        "sedang bermasalah.\n\n" +
                                        "HTTP $status"
                        }
                    }
                }

                override fun onReceivedSslError(
                    view: WebView,
                    handler: SslErrorHandler,
                    error: android.net.http.SslError
                ) {

                    /*
                     * JANGAN bypass SSL.
                     */

                    handler.cancel()

                    loading =
                        false

                    this@RexChatViewModel.error =
                        "Koneksi HTTPS tidak aman.\n\n" +
                                "Periksa tanggal, waktu, " +
                                "dan koneksi perangkat."
                }

                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail
                ): Boolean {

                    loading =
                        false

                    error =
                        "Mesin WebView berhenti.\n\n" +
                                "Ketuk Coba lagi untuk " +
                                "memuat ulang."

                    return true
                }
            }

        /*
         * =====================================================
         * CHROME CLIENT
         * =====================================================
         */

        wv.webChromeClient =
            object : WebChromeClient() {

                override fun onProgressChanged(
                    view: WebView,
                    newProgress: Int
                ) {

                    progress =
                        newProgress

                    if (
                        newProgress >= 100
                    ) {

                        loading =
                            false
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

                    if (
                        !title.isNullOrBlank()
                    ) {

                        pageTitle =
                            title
                    }
                }

                /*
                 * =================================================
                 * FILE UPLOAD
                 * =================================================
                 */

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback:
                        ValueCallback<Array<Uri>>,
                    fileChooserParams:
                        FileChooserParams
                ): Boolean {

                    pendingFileCallback
                        ?.onReceiveValue(
                            null
                        )

                    pendingFileCallback =
                        filePathCallback

                    onPickFiles?.invoke()

                    return true
                }

                /*
                 * =================================================
                 * CAMERA / MICROPHONE
                 * =================================================
                 */

                override fun onPermissionRequest(
                    request: PermissionRequest
                ) {

                    val needed =
                        request.resources
                            .mapNotNull { resource ->

                                when (resource) {

                                    PermissionRequest
                                        .RESOURCE_VIDEO_CAPTURE -> {

                                        Manifest.permission
                                            .CAMERA
                                    }

                                    PermissionRequest
                                        .RESOURCE_AUDIO_CAPTURE -> {

                                        Manifest.permission
                                            .RECORD_AUDIO
                                    }

                                    else -> {
                                        null
                                    }
                                }
                            }
                            .distinct()

                    if (
                        needed.isEmpty()
                    ) {

                        request.deny()

                        return
                    }

                    pendingPermission
                        ?.deny()

                    pendingPermission =
                        request

                    onNeedPermissions?.invoke(
                        needed.toTypedArray()
                    )
                }

                override fun onPermissionRequestCanceled(
                    request: PermissionRequest
                ) {

                    if (
                        pendingPermission ===
                        request
                    ) {

                        pendingPermission =
                            null
                    }

                    super
                        .onPermissionRequestCanceled(
                            request
                        )
                }
            }

        /*
         * =====================================================
         * LOAD
         * =====================================================
         */

        wv.loadUrl(
            HOME_URL
        )

        return wv
    }

    // =====================================================================
    // HOST
    // =====================================================================

    private fun isAllowedWebHost(
        host: String?
    ): Boolean {

        if (
            host == null
        ) {
            return false
        }

        return host == "whatsapp.com" ||
                host.endsWith(
                    ".whatsapp.com"
                ) ||

                host == "whatsapp.net" ||
                host.endsWith(
                    ".whatsapp.net"
                ) ||

                host == "facebook.com" ||
                host.endsWith(
                    ".facebook.com"
                ) ||

                host == "fbcdn.net" ||
                host.endsWith(
                    ".fbcdn.net"
                )
    }

    // =====================================================================
    // FILE
    // =====================================================================

    fun onFilesPicked(
        uris: List<Uri>
    ) {

        val callback =
            pendingFileCallback

        pendingFileCallback =
            null

        callback?.onReceiveValue(

            if (
                uris.isEmpty()
            ) {

                null

            } else {

                uris.toTypedArray()
            }
        )
    }

    fun cancelFilePicker() {

        pendingFileCallback
            ?.onReceiveValue(
                null
            )

        pendingFileCallback =
            null
    }

    // =====================================================================
    // PERMISSION
    // =====================================================================

    fun onPermissionsResult(
        granted: Boolean
    ) {

        val request =
            pendingPermission
                ?: return

        pendingPermission =
            null

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

        pendingPermission =
            null
    }

    // =====================================================================
    // NAVIGATION
    // =====================================================================

    fun canGoBack(): Boolean {

        return webView.canGoBack()
    }

    fun goBack() {

        if (
            webView.canGoBack()
        ) {

            webView.goBack()
        }
    }

    // =====================================================================
    // RELOAD
    // =====================================================================

    fun reload() {

        error =
            null

        loading =
            true

        progress =
            0

        webView.stopLoading()

        if (
            webView.url.isNullOrBlank()
        ) {

            webView.loadUrl(
                HOME_URL
            )

        } else {

            webView.reload()
        }
    }

    // =====================================================================
    // CLEAR SESSION
    // =====================================================================

    fun clearSession() {

        /*
         * Hentikan halaman.
         */
        webView.stopLoading()

        /*
         * Callback.
         */
        cancelFilePicker()
        cancelPermissionRequest()

        /*
         * ------------------------------------------------------
         * COOKIE
         * ------------------------------------------------------
         *
         * PERHATIAN:
         *
         * CookieManager pada process ini menggunakan data
         * directory suffix session aktif.
         *
         * Jadi ini hanya session ini.
         */

        val cookies =
            CookieManager.getInstance()

        cookies.removeAllCookies {

            cookies.flush()
        }

        /*
         * ------------------------------------------------------
         * WEB STORAGE
         * ------------------------------------------------------
         */

        WebStorage
            .getInstance()
            .deleteAllData()

        /*
         * ------------------------------------------------------
         * CACHE
         * ------------------------------------------------------
         */

        webView.clearCache(
            true
        )

        webView.clearHistory()

        webView.clearFormData()

        /*
         * ------------------------------------------------------
         * METADATA
         * ------------------------------------------------------
         */

        SessionManager
            .deleteSession(
                activity,
                sessionId
            )

        error =
            null

        loading =
            false

        progress =
            0
    }

    // =====================================================================
    // CSS
    // =====================================================================

    private fun injectCompatibilityCss(
        view: WebView
    ) {

        val js =
            """
            javascript:(function() {
                try {
                    var meta =
                        document.querySelector(
                            'meta[name="viewport"]'
                        );

                    if (!meta) {
                        meta =
                            document.createElement(
                                'meta'
                            );

                        meta.name =
                            'viewport';

                        meta.content =
                            'width=device-width, initial-scale=1.0, maximum-scale=1.0';

                        document.head.appendChild(
                            meta
                        );
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

    // =====================================================================
    // COOKIE FLUSH
    // =====================================================================

    fun flush() {

        runCatching {

            CookieManager
                .getInstance()
                .flush()
        }
    }

    // =====================================================================
    // CLEANUP
    // =====================================================================

    override fun onCleared() {

        mainHandler
            .removeCallbacksAndMessages(
                null
            )

        cancelFilePicker()

        cancelPermissionRequest()

        flush()

        runCatching {

            webView.stopLoading()

            webView.removeAllViews()

            webView.destroy()
        }

        super.onCleared()
    }

    // =====================================================================
    // FACTORY
    // =====================================================================

    class Factory(
        private val activity: Activity,
        private val sessionId: Int
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
                    activity =
                        activity,
                    sessionId =
                        sessionId
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class: " +
                        modelClass.name
            )
        }
    }
}
