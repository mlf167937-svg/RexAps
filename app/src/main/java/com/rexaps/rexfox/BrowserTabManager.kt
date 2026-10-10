package com.rexaps.rexfox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/** Owns WebView instances and browser state for RexFox tabs. */
class BrowserTabManager(
    private val activity: Activity,
    private val settingsProvider: () -> BrowserSettings,
    private val onChanged: () -> Unit,
    private val onDownload: (String, String?, String?, String?) -> Unit
) {
    private val tabs = mutableListOf<BrowserTab>()
    private var nextId = 1
    var activeTabId: Int? = null
        private set

    var pageStateListener: ((String, String, Int, Boolean, String?, Boolean) -> Unit)? = null

    fun createTab(incognito: Boolean = false, url: String = "about:blank", loadInitialUrl: Boolean = true): BrowserTab {
        val webView = WebView(activity).apply {
            val settings = settingsProvider()
            this.settings.javaScriptEnabled = settings.javaScriptEnabled
            this.settings.domStorageEnabled = settings.domStorageEnabled
            this.settings.javaScriptCanOpenWindowsAutomatically = true
            this.settings.setSupportMultipleWindows(false)
            this.settings.loadsImagesAutomatically = true
            this.settings.useWideViewPort = true
            this.settings.loadWithOverviewMode = settings.desktopSite
            if (settings.desktopSite) {
                this.settings.userAgentString = this.settings.userAgentString.replace("; wv", "")
            }
            if (incognito) {
                this.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                clearCache(true)
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, settings.thirdPartyCookiesEnabled && !incognito)
        }
        var tabRef: BrowserTab? = null
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob") return false
                openExternal(uri)
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                tabRef?.let { it.url = url; it.title = view.title ?: it.title }
                onState(url, view.title.orEmpty(), 0, true, null, incognito)
            }

            override fun onPageFinished(view: WebView, url: String) {
                tabRef?.let {
                    it.url = url
                    it.title = view.title?.takeIf(String::isNotBlank) ?: it.title
                    it.canGoBack = view.canGoBack()
                    it.canGoForward = view.canGoForward()
                }
                onState(url, view.title.orEmpty(), 100, false, null, incognito)
                onChanged()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    onState(request.url.toString(), view.title.orEmpty(), 0, false,
                        "Halaman gagal dimuat (${error.errorCode}).", incognito)
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                val current = view.url.orEmpty()
                onState(current, view.title.orEmpty(), newProgress, newProgress < 100, null, incognito)
                tabRef?.let { it.url = current.ifBlank { it.url }; it.title = view.title?.takeIf(String::isNotBlank) ?: it.title }
            }
            override fun onReceivedTitle(view: WebView, title: String?) {
                tabRef?.let { if (!title.isNullOrBlank()) it.title = title }
                onChanged()
            }
        }
        webView.setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
            onDownload(downloadUrl, userAgent, contentDisposition, mimeType)
        }
        val tab = BrowserTab(nextId++, url = url, webView = webView, isIncognito = incognito)
        tabRef = tab
        tabs += tab
        activeTabId = tab.id
        if (loadInitialUrl && url != "about:blank") webView.loadUrl(url)
        onChanged()
        return tab
    }

    private fun onState(url: String, title: String, progress: Int, loading: Boolean, error: String?, incognito: Boolean) {
        onChanged()
        pageStateListener?.invoke(url, title, progress, loading, error, incognito)
    }

    private fun openExternal(uri: Uri) {
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    fun switchTab(id: Int): BrowserTab? {
        if (tabs.none { it.id == id }) return null
        activeTabId = id
        onChanged()
        return getActiveTab()
    }

    fun closeTab(id: Int) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs.removeAt(index).webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        activeTabId = when {
            tabs.isEmpty() -> null
            activeTabId != id -> activeTabId
            else -> tabs[index.coerceAtMost(tabs.lastIndex)].id
        }
        onChanged()
    }

    fun getActiveTab(): BrowserTab? = tabs.firstOrNull { it.id == activeTabId }
    fun getTabs(): List<BrowserTab> = tabs.toList()
    fun count() = tabs.size
    fun snapshots() = tabs.map { TabSnapshot(it.id, it.title, it.url, it.isIncognito) }

    fun destroyAll() {
        tabs.toList().forEach { tab ->
            runCatching {
                tab.webView.stopLoading()
                tab.webView.loadUrl("about:blank")
                tab.webView.removeAllViews()
                tab.webView.destroy()
            }
        }
        tabs.clear()
        activeTabId = null
    }
}
