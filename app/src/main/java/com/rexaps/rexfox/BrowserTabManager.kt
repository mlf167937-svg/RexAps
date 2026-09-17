package com.rexaps.rexfox

import android.app.Activity
import android.webkit.WebView

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

    fun createTab(incognito: Boolean = false, url: String = "about:blank"): BrowserTab {
        val webView = WebView(activity)
        val tab = BrowserTab(nextId++, url = url, webView = webView, isIncognito = incognito)
        tabs += tab
        activeTabId = tab.id

        val manager = WebViewManager(
            activity,
            settingsProvider,
            { currentUrl, title, _, _, _ ->
                updateTab(tab.id, currentUrl, title)
                onChanged()
            },
            onDownload,
            { target -> createTab(false, target) }
        )
        manager.configure(webView, incognito)
        webView.tag = manager

        if (url != "about:blank") webView.loadUrl(url)
        onChanged()
        return tab
    }

    private fun updateTab(id: Int, url: String, title: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = tabs[index]
        tabs[index] = old.copy(
            url = url.ifBlank { old.url },
            title = title.ifBlank { old.title }
        )
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
        val removed = tabs.removeAt(index)
        (removed.webView.tag as? WebViewManager)?.destroy(removed.webView)
            ?: removed.webView.destroy()

        activeTabId = when {
            tabs.isEmpty() -> null
            activeTabId != id -> activeTabId
            else -> tabs[index.coerceAtMost(tabs.lastIndex)].id
        }
        onChanged()
    }

    fun getActiveTab() = tabs.firstOrNull { it.id == activeTabId }
    fun getTabs(): List<BrowserTab> = tabs.toList()
    fun count() = tabs.size

    fun snapshots() = tabs.map {
        TabSnapshot(it.id, it.title, it.url, it.isIncognito)
    }

    fun destroyAll() {
        tabs.forEach {
            (it.webView.tag as? WebViewManager)?.destroy(it.webView)
                ?: it.webView.destroy()
        }
        tabs.clear()
        activeTabId = null
    }
}
