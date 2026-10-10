package com.rexaps.rexfox

import android.app.Activity
import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class BrowserTabManager(
    private val activity: Activity,
    private val settingsProvider: () -> BrowserSettings,
    private val onChanged: () -> Unit,
    private val onDownload: (String, String?, String?, String?) -> Unit
) {
    private val runtime: GeckoRuntime = GeckoRuntimeHolder.get(activity.applicationContext)
    private val tabs = mutableListOf<BrowserTab>()
    private var nextId = 1
    var activeTabId: Int? = null
        private set

    fun createTab(incognito: Boolean = false, url: String = "about:blank", loadInitialUrl: Boolean = true): BrowserTab {
        var tabRef: BrowserTab? = null
        val engine = GeckoViewManager(
            activity = activity,
            runtime = runtime,
            settingsProvider = settingsProvider,
            onState = { currentUrl, title, progress, loading, error ->
                tabRef?.let { tab ->
                    if (currentUrl.isNotBlank()) tab.url = currentUrl
                    if (title.isNotBlank()) tab.title = title
                }
                onChanged()
                pageStateListener?.invoke(currentUrl, title, progress, loading, error, incognito)
            },
            onNewWindow = { newUrl -> createTab(incognito, newUrl, loadInitialUrl = false).session },
            onNavigationCapabilities = { canBack, canForward ->
                tabRef?.let { tab ->
                    if (canBack != null) tab.canGoBack = canBack
                    if (canForward != null) tab.canGoForward = canForward
                    onChanged()
                }
            },
            onDownload = onDownload
        )
        val session = engine.createSession(incognito)
        val tab = BrowserTab(nextId++, url = url, session = session, isIncognito = incognito)
        tabRef = tab
        tabs += tab
        activeTabId = tab.id
        updateActiveSessions()
        if (loadInitialUrl && url != "about:blank") session.loadUri(url)
        onChanged()
        return tab
    }

    fun switchTab(id: Int): BrowserTab? {
        if (tabs.none { it.id == id }) return null
        activeTabId = id
        updateActiveSessions()
        onChanged()
        return getActiveTab()
    }

    fun closeTab(id: Int) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs.removeAt(index).session.close()
        activeTabId = when {
            tabs.isEmpty() -> null
            activeTabId != id -> activeTabId
            else -> tabs[index.coerceAtMost(tabs.lastIndex)].id
        }
        updateActiveSessions()
        onChanged()
    }

    private fun updateActiveSessions() {
        tabs.forEach { tab ->
            val active = tab.id == activeTabId
            tab.session.setActive(active)
            tab.session.setFocused(active)
        }
    }

    fun getActiveTab(): BrowserTab? = tabs.firstOrNull { it.id == activeTabId }
    fun getTabs(): List<BrowserTab> = tabs.toList()
    fun count() = tabs.size
    fun snapshots() = tabs.map { TabSnapshot(it.id, it.title, it.url, it.isIncognito) }
    var pageStateListener: ((String, String, Int, Boolean, String?, Boolean) -> Unit)? = null

    fun destroyAll() {
        tabs.forEach { runCatching { it.session.close() } }
        tabs.clear()
        activeTabId = null
    }
}

/** GeckoRuntime must outlive individual screens and tabs. */
private object GeckoRuntimeHolder {
    @Volatile private var instance: GeckoRuntime? = null
    fun get(context: Context): GeckoRuntime = instance ?: synchronized(this) {
        instance ?: GeckoRuntime.create(context.applicationContext).also { instance = it }
    }
}
