package com.rexaps.rexfox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/** Lightweight browser tabs backed by Android's installed System WebView. */
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
        lateinit var tab: BrowserTab
        val webView = WebView(activity).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            settings.apply {
                javaScriptEnabled = settingsProvider().javaScriptEnabled
                domStorageEnabled = settingsProvider().domStorageEnabled
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = false
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                if (settingsProvider().desktopSite) userAgentString = userAgentString.replace("Mobile", "X11; Linux x86_64")
                if (incognito) cacheMode = WebSettings.LOAD_NO_CACHE
            }
            if (incognito) {
                clearCache(true)
                clearHistory()
            }
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, settingsProvider().thirdPartyCookiesEnabled)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    tab.url = url
                    tab.title = view.title?.ifBlank { null } ?: "Loading…"
                    onState(tab, 0, true, null)
                }
                override fun onPageFinished(view: WebView, url: String) {
                    tab.url = url
                    tab.title = view.title?.ifBlank { null } ?: url
                    tab.canGoBack = view.canGoBack()
                    tab.canGoForward = view.canGoForward()
                    onState(tab, 100, false, null)
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) onState(tab, 0, false, "Unable to load this page (${error.errorCode}). Check your connection and try again.")
                }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    val scheme = uri.scheme?.lowercase()
                    if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob" || scheme == "javascript") return false
                    openExternal(uri)
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    tab.url = view.url ?: tab.url
                    tab.title = view.title?.ifBlank { null } ?: tab.title
                    tab.canGoBack = view.canGoBack()
                    tab.canGoForward = view.canGoForward()
                    onState(tab, newProgress, newProgress < 100, null)
                }
                override fun onReceivedTitle(view: WebView, title: String?) {
                    tab.title = title?.ifBlank { null } ?: tab.title
                    onChanged()
                }
            }
            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
                // WebView supplies contentLength as the fifth argument; the download manager
                // currently determines the file size from the response itself.
                onDownload(downloadUrl, userAgent, contentDisposition, mimeType)
            }
        }
        tab = BrowserTab(nextId++, title = "New Tab", url = url, webView = webView, isIncognito = incognito)
        tabs += tab
        activeTabId = tab.id
        if (loadInitialUrl && url != "about:blank") webView.loadUrl(url)
        onChanged()
        return tab
    }

    private fun onState(tab: BrowserTab, progress: Int, loading: Boolean, error: String?) {
        tab.url = tab.webView.url ?: tab.url
        tab.title = tab.webView.title?.ifBlank { null } ?: tab.title
        tab.canGoBack = tab.webView.canGoBack()
        tab.canGoForward = tab.webView.canGoForward()
        onChanged()
        if (tab.id == activeTabId) pageStateListener?.invoke(tab.url, tab.title, progress, loading, error, tab.isIncognito)
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
        val webView = tabs.removeAt(index).webView
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.apply { stopLoading(); loadUrl("about:blank"); clearHistory(); removeAllViews(); destroy() }
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
        tabs.toList().forEach { closeTab(it.id) }
        tabs.clear()
        activeTabId = null
    }
}
