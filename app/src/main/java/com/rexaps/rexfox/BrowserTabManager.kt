package com.rexaps.rexfox

import android.app.Activity
import android.webkit.WebView

class BrowserTabManager(
    private val activity: Activity
) {

    private val tabs = mutableListOf<BrowserTab>()
    private var nextId = 1

    var activeTabId: Int? = null
        private set

    fun createTab(): BrowserTab {
        val webView = WebView(activity).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }

        val tab = BrowserTab(
            id = nextId++,
            webView = webView
        )

        tabs.add(tab)
        activeTabId = tab.id

        return tab
    }

    fun switchTab(id: Int): BrowserTab? {
        val tab = tabs.firstOrNull { it.id == id }

        if (tab != null) {
            activeTabId = id
        }

        return tab
    }

    fun closeTab(id: Int): BrowserTab? {
        val index = tabs.indexOfFirst { it.id == id }

        if (index == -1) {
            return null
        }

        val removed = tabs.removeAt(index)
        removed.webView.destroy()

        if (tabs.isEmpty()) {
            activeTabId = null
        } else if (activeTabId == id) {
            val newIndex = index.coerceAtMost(tabs.lastIndex)
            activeTabId = tabs[newIndex].id
        }

        return removed
    }

    fun getTabs(): List<BrowserTab> {
        return tabs.toList()
    }

    fun getActiveTab(): BrowserTab? {
        return tabs.firstOrNull { it.id == activeTabId }
    }
}
