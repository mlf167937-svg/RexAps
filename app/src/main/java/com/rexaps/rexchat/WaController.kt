package com.rexaps.rexchat

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.ProfileStore

enum class Login { LOADING, LOGGED_OUT, CHATS }

internal const val HOME_URL = "https://web.whatsapp.com/"

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
    private var destroyed = false

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
    internal fun createWebView(): WebView {
        return this.__splitImpl_createWebView()
    }

    internal fun isAllowedHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return listOf("whatsapp.com", "whatsapp.net", "facebook.com", "fbcdn.net")
            .any { h == it || h.endsWith(".$it") }
    }

    internal fun cookies(): CookieManager {
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

    internal fun checkBlank() {
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

    internal fun applyViewport() {
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