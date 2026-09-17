package com.rexaps.rexfox

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrowserUiState(
    val screen: Screen = Screen.Home,
    val settings: BrowserSettings = BrowserSettings(),
    val history: List<HistoryEntry> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val tabs: List<TabSnapshot> = emptyList(),
    val activeTabId: Int? = null,
    val progress: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val privacyStatus: String = "No tracker blocking is active."
)

enum class Screen { Home, Browser, Tabs, Bookmarks, History, Downloads, Settings, DevTools }

class RexFoxBrowserViewModel(
    context: Context,
    private val activity: Activity
) : ViewModel() {
    private val store = BrowserStore(context.applicationContext)
    private val bookmarksRepo = BookmarkRepository(store)
    private val historyRepo = HistoryRepository(store)
    private val privacy = PrivacyManager()
    private val downloadManager = BrowserDownloadManager(context.applicationContext)

    private val _state = MutableStateFlow(
        BrowserUiState(
            settings = store.settings(),
            history = historyRepo.all(),
            bookmarks = bookmarksRepo.all(),
            privacyStatus = privacy.status(store.settings())
        )
    )
    val state = _state.asStateFlow()

    val tabManager = BrowserTabManager(
        activity = activity,
        settingsProvider = { _state.value.settings },
        onChanged = ::refreshTabs,
        onDownload = { url, ua, cd, mime ->
            downloadManager.enqueue(url, ua, cd, mime)
        }
    ).also { manager ->
        manager.pageStateListener = { url, title, progress, loading, error, incognito ->
            onPageState(url, title, progress, loading, error, incognito)
        }
    }

    fun navigate(input: String) {
        when (val target = NavigationResolver.resolve(input, _state.value.settings.searchEngine)) {
            NavigationTarget.Empty -> Unit
            is NavigationTarget.Web -> load(target.url)
            is NavigationTarget.Search -> load(target.url)
            is NavigationTarget.External -> runCatching {
                activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, target.uri))
            }
        }
    }

    fun load(url: String) {
        val tab = tabManager.getActiveTab()
        if (tab == null) tabManager.createTab(false, url)
        else tab.webView.loadUrl(url)
        _state.value = _state.value.copy(screen = Screen.Browser, error = null)
    }

    fun newTab() {
        tabManager.createTab()
        _state.value = _state.value.copy(screen = Screen.Browser)
    }

    fun newIncognitoTab() {
        tabManager.createTab(true)
        _state.value = _state.value.copy(screen = Screen.Browser)
    }

    fun switchTab(id: Int) {
        tabManager.switchTab(id)
        _state.value = _state.value.copy(screen = Screen.Browser)
    }

    fun closeTab(id: Int) {
        tabManager.closeTab(id)
        _state.value = _state.value.copy(
            screen = if (tabManager.count() == 0) Screen.Home else Screen.Browser
        )
    }

    fun back() {
        tabManager.getActiveTab()?.webView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun forward() {
        tabManager.getActiveTab()?.webView?.let { if (it.canGoForward()) it.goForward() }
    }

    fun reload() = tabManager.getActiveTab()?.webView?.reload()
    fun stop() = tabManager.getActiveTab()?.webView?.stopLoading()

    fun toggleBookmark() {
        val tab = tabManager.getActiveTab() ?: return
        if (bookmarksRepo.isBookmarked(tab.url)) bookmarksRepo.remove(tab.url)
        else bookmarksRepo.add(tab.url, tab.title)
        _state.value = _state.value.copy(bookmarks = bookmarksRepo.all())
    }

    fun clearHistory() {
        historyRepo.clear()
        _state.value = _state.value.copy(history = emptyList())
    }

    fun updateSettings(settings: BrowserSettings) {
        store.saveSettings(settings)
        _state.value = _state.value.copy(
            settings = settings,
            privacyStatus = privacy.status(settings)
        )
    }

    fun onPageState(
        url: String,
        title: String,
        progress: Int,
        loading: Boolean,
        error: String?,
        incognito: Boolean
    ) {
        // Persist only completed, non-incognito navigations. Progress callbacks can fire many times.
        if (!loading && progress >= 100) {
            historyRepo.add(url, title, incognito)
        }

        _state.value = _state.value.copy(
            progress = progress,
            loading = loading,
            error = error,
            history = historyRepo.all(),
            tabs = tabManager.snapshots(),
            activeTabId = tabManager.activeTabId
        )
    }

    private fun refreshTabs() {
        _state.value = _state.value.copy(
            tabs = tabManager.snapshots(),
            activeTabId = tabManager.activeTabId
        )
    }

    fun openScreen(screen: Screen) { _state.value = _state.value.copy(screen = screen, error = null) }

    override fun onCleared() {
        tabManager.destroyAll()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val activity: Activity
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RexFoxBrowserViewModel(context, activity) as T
    }
}

