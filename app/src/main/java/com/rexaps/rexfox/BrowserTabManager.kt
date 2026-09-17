package com.rexaps.rexfox

import android.app.Activity
import android.webkit.WebView

class BrowserTabManager(private val activity: Activity) {

    private val tabs = mutableListOf<BrowserTab>()
    private var nextId = 1

    var activeTabId: Int? = null
        private set

    fun createTab(incognito: Boolean = false): BrowserTab {
        val webView = WebView(activity).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            if (incognito) {
                settings.savePassword = false
                settings.saveFormData = false
            }
        }
        val tab = BrowserTab(id = nextId++, webView = webView, isIncognito = incognito)
        tabs.add(tab)
        activeTabId = tab.id
        return tab
    }

    fun updateTab(id: Int, title: String? = null, url: String? = null): BrowserTab? {
        val index = tabs.indexOfFirst { it.id == id }
        if (index == -1) return null
        val old = tabs[index]
        val updated = old.copy(
            title = title ?: old.title,
            url = url ?: old.url
        )
        tabs[index] = updated
        return updated
    }

    fun switchTab(id: Int): BrowserTab? {
        val tab = tabs.firstOrNull { it.id == id }
        if (tab != null) activeTabId = id
        return tab
    }

    fun closeTab(id: Int): BrowserTab? {
        val index = tabs.indexOfFirst { it.id == id }
        if (index == -1) return null
        val removed = tabs.removeAt(index)
        removed.webView.destroy()
        activeTabId = when {
            tabs.isEmpty() -> null
            activeTabId == id -> tabs[index.coerceAtMost(tabs.lastIndex)].id
            else -> activeTabId
        }
        return removed
    }

    fun getTabs(): List<BrowserTab> = tabs.toList()

    fun getActiveTab(): BrowserTab? = tabs.firstOrNull { it.id == activeTabId }
}
