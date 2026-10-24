package com.rexaps.rexchat

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class WaController(
    internal val activity: Activity,
    val sessionId: Int,
    internal val legacy: Boolean,
    @Suppress("unused") private val initialMethod: String
) {

    // ---------- UI state ----------
    var progress by mutableIntStateOf(0)
        internal set

    var loading by mutableStateOf(true)
        internal set

    var error by mutableStateOf<String?>(null)
        internal set

    var login by mutableStateOf(Login.LOADING)
        internal set

    var desktopLayout by mutableStateOf(false)
        internal set

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


    fun toggleDesktopLayout() = toggleDesktopLayoutImpl()

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

    fun onPermissionsResult(granted: Boolean) = onPermissionsResultImpl(granted)

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

    fun destroy() = destroyImpl()
}
