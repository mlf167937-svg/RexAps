package com.rexaps.rexfox

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.MainThread
import com.rexaps.gecko.RexGeckoRuntime
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

/** Owns one GeckoSession per RexFox tab. All public methods are called on the main thread. */
class BrowserTabManager(
    context: Context,
    private val settingsProvider: () -> BrowserSettings,
    private val onChanged: () -> Unit,
    private val onDownload: (String, String?, String?, String?) -> Unit
) {
    private val appContext = context.applicationContext
    private val runtime by lazy(LazyThreadSafetyMode.NONE) { RexGeckoRuntime.get(appContext) }
    private val tabs = mutableListOf<BrowserTab>()
    private var nextId = 1
    var activeTabId: Int? = null
        private set

    /** tabId is included so a background tab can never overwrite the active tab's UI state. */
    var pageStateListener: ((Int, String, String, Int, Boolean, String?, Boolean) -> Unit)? = null
        set(value) {
            field = value
            tabs.forEach(::publish)
        }

    @MainThread
    fun createTab(
        incognito: Boolean = false,
        url: String = "about:blank",
        loadInitialUrl: Boolean = true
    ): BrowserTab {
        val currentSettings = settingsProvider()
        val builder = GeckoSessionSettings.Builder()
            .allowJavascript(currentSettings.javaScriptEnabled)
            .usePrivateMode(incognito)
            .useTrackingProtection(currentSettings.trackingProtectionEnabled)
            .userAgentMode(
                if (currentSettings.desktopSite) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            )
            .viewportMode(
                if (currentSettings.desktopSite) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            )
        if (!incognito) builder.contextId("rexfox-normal")

        val session = GeckoSession(builder.build())
        val tab = BrowserTab(
            id = nextId++,
            url = url,
            session = session,
            isIncognito = incognito
        )
        configureDelegates(tab)
        tabs += tab
        activeTabId?.let { findTab(it)?.session?.setActive(false) }
        activeTabId = tab.id
        session.open(runtime)
        session.setActive(true)
        if (loadInitialUrl && url != "about:blank") session.load(GeckoSession.Loader().uri(url))
        onChanged()
        return tab
    }

    private fun configureDelegates(tab: BrowserTab) {
        val session = tab.session
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = runCatching { Uri.parse(request.uri) }.getOrNull() ?: return null
                val scheme = uri.scheme?.lowercase().orEmpty()
                if (scheme in setOf("http", "https", "about", "data", "blob", "resource", "moz-extension")) {
                    return null
                }
                openExternal(uri)
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                url?.takeIf(String::isNotBlank)?.let { tab.url = it }
                publish(tab)
                onChanged()
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                tab.canGoBack = canGoBack
                onChanged()
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                tab.canGoForward = canGoForward
                onChanged()
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: org.mozilla.geckoview.WebRequestError
            ): GeckoResult<String>? {
                tab.isLoading = false
                tab.progress = 0
                tab.error = "Halaman gagal dimuat: ${error.message ?: "koneksi atau server bermasalah"}"
                publish(tab)
                onChanged()
                // Returning null aborts the failed load. Gecko's default certificate validation stays enabled.
                return null
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                // Popups are not silently attached to the wrong tab; the user can open the link explicitly.
                return null
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                tab.url = url
                tab.progress = 0
                tab.isLoading = true
                tab.error = null
                publish(tab)
                onChanged()
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                tab.progress = progress.coerceIn(0, 100)
                tab.isLoading = progress < 100
                publish(tab)
                onChanged()
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tab.isLoading = false
                if (success) tab.progress = 100
                publish(tab)
                onChanged()
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                title?.takeIf(String::isNotBlank)?.let { tab.title = it }
                publish(tab)
                onChanged()
            }

            override fun onExternalResponse(session: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                val headers = response.headers
                fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, true) }?.value
                val contentDisposition = header("content-disposition")
                val contentType = header("content-type")
                // The existing Android DownloadManager bridge performs the transfer. Close Gecko's
                // response stream before handing off the URL; do not call getUserAgent() here because
                // GeckoView exposes that query on its handler thread, not this UI callback.
                runCatching { response.body?.close() }
                onDownload(response.uri, null, contentDisposition, contentType)
            }

            override fun onCrash(session: GeckoSession) {
                tab.isLoading = false
                tab.canGoBack = false
                tab.canGoForward = false
                tab.error = "Proses halaman terhenti. Muat ulang untuk memulihkan sesi."
                publish(tab)
                onChanged()
            }

            override fun onKill(session: GeckoSession) {
                tab.isLoading = false
                tab.canGoBack = false
                tab.canGoForward = false
                tab.error = "Proses browser dihentikan Android. Muat ulang untuk membuka kembali halaman."
                publish(tab)
                onChanged()
            }
        }
    }

    private fun publish(tab: BrowserTab) {
        pageStateListener?.invoke(
            tab.id, tab.url, tab.title, tab.progress, tab.isLoading, tab.error, tab.isIncognito
        )
    }

    private fun openExternal(uri: Uri) {
        runCatching {
            appContext.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    @MainThread
    fun switchTab(id: Int): BrowserTab? {
        val next = findTab(id) ?: return null
        if (activeTabId != id) findTab(activeTabId)?.session?.setActive(false)
        activeTabId = id
        if (!next.session.isOpen) {
            next.session.open(runtime)
            if (next.url.isNotBlank() && next.url != "about:blank") {
                next.session.load(GeckoSession.Loader().uri(next.url))
            }
        }
        next.session.setActive(true)
        publish(next)
        onChanged()
        return next
    }

    @MainThread
    fun setBrowserVisible(visible: Boolean) {
        tabs.forEach { tab ->
            val shouldBeActive = visible && tab.id == activeTabId
            if (shouldBeActive && !tab.session.isOpen) {
                tab.session.open(runtime)
                if (tab.url.isNotBlank() && tab.url != "about:blank") {
                    tab.session.load(GeckoSession.Loader().uri(tab.url))
                }
            }
            tab.session.setActive(shouldBeActive)
        }
    }

    @MainThread
    fun closeTab(id: Int) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val tab = tabs.removeAt(index)
        runCatching {
            tab.session.stop()
            tab.session.setActive(false)
            tab.session.close()
        }
        activeTabId = when {
            tabs.isEmpty() -> null
            activeTabId != id -> activeTabId
            else -> tabs[index.coerceAtMost(tabs.lastIndex)].id
        }
        getActiveTab()?.session?.setActive(true)
        onChanged()
    }

    @MainThread
    fun closeAllTabs() {
        tabs.toList().forEach { tab ->
            runCatching {
                tab.session.stop()
                tab.session.setActive(false)
                tab.session.close()
            }
        }
        tabs.clear()
        activeTabId = null
        onChanged()
    }

    fun getActiveTab(): BrowserTab? = findTab(activeTabId)
    fun getTabs(): List<BrowserTab> = tabs.toList()
    fun count(): Int = tabs.size
    fun snapshots(): List<TabSnapshot> = tabs.map {
        TabSnapshot(
            it.id, it.title, it.url, it.isIncognito, it.progress, it.isLoading,
            it.canGoBack, it.canGoForward
        )
    }

    @MainThread
    fun navigateActive(url: String) {
        val tab = getActiveTab() ?: createTab()
        tab.url = url
        tab.error = null
        if (!tab.session.isOpen) tab.session.open(runtime)
        tab.session.load(GeckoSession.Loader().uri(url))
        onChanged()
    }

    @MainThread
    fun goBack() {
        getActiveTab()?.takeIf { it.canGoBack && it.session.isOpen }?.session?.goBack()
    }

    @MainThread
    fun goForward() {
        getActiveTab()?.takeIf { it.canGoForward && it.session.isOpen }?.session?.goForward()
    }

    @MainThread
    fun reload() {
        val tab = getActiveTab() ?: return
        if (tab.session.isOpen) {
            tab.session.reload()
        } else {
            tab.session.open(runtime)
            if (tab.url.isNotBlank() && tab.url != "about:blank") {
                tab.session.load(GeckoSession.Loader().uri(tab.url))
            }
        }
    }

    @MainThread
    fun stop() { getActiveTab()?.session?.takeIf { it.isOpen }?.stop() }

    @MainThread
    fun applySettings(settings: BrowserSettings) {
        tabs.filterNot { it.isIncognito }.forEach { tab ->
            tab.session.settings.apply {
                setAllowJavascript(settings.javaScriptEnabled)
                setUseTrackingProtection(settings.trackingProtectionEnabled)
                setUserAgentMode(
                    if (settings.desktopSite) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                )
                setViewportMode(
                    if (settings.desktopSite) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                    else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                )
            }
        }
    }

    @MainThread
    fun destroyAll() = closeAllTabs()

    private fun findTab(id: Int?): BrowserTab? = id?.let { target -> tabs.firstOrNull { it.id == target } }
}
