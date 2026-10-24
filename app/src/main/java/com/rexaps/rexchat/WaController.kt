package com.rexaps.rexchat

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

class WaController(
    internal val activity: Activity,
    val sessionId: Int,
    internal val legacy: Boolean,
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

    internal var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    internal var pendingPermission: PermissionRequest? = null

    internal val handler = Handler(Looper.getMainLooper())
    internal var destroyed = false

    // Log diagnosis
    internal val consoleLog = ArrayDeque<String>()
    internal val netLog = ArrayDeque<String>()

    internal fun addLog(list: ArrayDeque<String>, s: String) {
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
    private fun createWebView(): WebView = this.__split_createWebView_1()

    internal fun isAllowedHost(host: String?): Boolean = this.__split_isAllowedHost_2(host)

    internal fun cookies(): CookieManager = this.__split_cookies_3()

    // =====================================================================
    // Diagnosis halaman kosong
    // =====================================================================

    internal fun checkBlank() = this.__split_checkBlank_4()

    // =====================================================================
    // Deteksi login
    // =====================================================================

    private fun probe() = this.__split_probe_5()

    internal fun applyViewport() = this.__split_applyViewport_6()

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

    fun onPermissionsResult(granted: Boolean) = this.__split_onPermissionsResult_7(granted)

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

    fun destroy() = this.__split_destroy_8()
}
