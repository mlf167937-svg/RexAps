package com.rexaps.rexfox

import android.content.Context
import android.content.Intent
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
    val activeUrl: String = "about:blank",
    val activeTitle: String = "New Tab",
    val activeIncognito: Boolean = false,
    val progress: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val privacyStatus: String = "No tracker blocking is active."
)

enum class Screen { Home, Browser, Tabs, Bookmarks, History, Downloads, Settings, DevTools }

class RexFoxBrowserViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val store = BrowserStore(appContext)
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
        context = appContext,
        settingsProvider = { _state.value.settings },
        onChanged = ::refreshTabs,
        onDownload = { url, userAgent, disposition, mimeType ->
            downloadManager.enqueue(url, userAgent, disposition, mimeType)
        }
    ).also { manager ->
        manager.pageStateListener = { tabId, url, title, progress, loading, error, incognito ->
            onPageState(tabId, url, title, progress, loading, error, incognito)
        }
    }

    fun navigate(input: String) {
        when (val target = NavigationResolver.resolve(input, _state.value.settings.searchEngine)) {
            NavigationTarget.Empty -> Unit
            is NavigationTarget.Web -> load(target.url)
            is NavigationTarget.Search -> load(target.url)
            is NavigationTarget.External -> runCatching {
                appContext.startActivity(Intent(Intent.ACTION_VIEW, target.uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun load(url: String) {
        tabManager.navigateActive(url)
        openScreen(Screen.Browser)
    }

    fun newTab() {
        tabManager.createTab()
        openScreen(Screen.Browser)
    }

    fun newIncognitoTab() {
        tabManager.createTab(incognito = true)
        openScreen(Screen.Browser)
    }

    fun switchTab(id: Int) {
        tabManager.switchTab(id)
        openScreen(Screen.Browser)
    }

    fun closeTab(id: Int) {
        tabManager.closeTab(id)
        openScreen(if (tabManager.count() == 0) Screen.Home else Screen.Browser)
    }

    fun closeAllTabs() {
        tabManager.closeAllTabs()
        openScreen(Screen.Home)
    }

    fun shareActiveUrlToRexAdm() {
        val url = tabManager.getActiveTab()?.url ?: return
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return
        val intent = Intent(appContext, com.rexaps.rexadm.MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    fun back() = tabManager.goBack()
    fun forward() = tabManager.goForward()
    fun reload() = tabManager.reload()
    fun stop() = tabManager.stop()

    fun toggleBookmark() {
        val tab = tabManager.getActiveTab() ?: return
        if (bookmarksRepo.isBookmarked(tab.url)) bookmarksRepo.remove(tab.url)
        else bookmarksRepo.add(tab.url, tab.title)
        _state.value = _state.value.copy(bookmarks = bookmarksRepo.all())
    }

    fun removeBookmark(url: String) {
        bookmarksRepo.remove(url)
        _state.value = _state.value.copy(bookmarks = bookmarksRepo.all())
    }

    fun deleteHistory(timestamp: Long) {
        historyRepo.delete(timestamp)
        _state.value = _state.value.copy(history = historyRepo.all())
    }

    fun clearHistory() {
        historyRepo.clear()
        _state.value = _state.value.copy(history = emptyList())
    }

    fun updateSettings(settings: BrowserSettings) {
        store.saveSettings(settings)
        tabManager.applySettings(settings)
        _state.value = _state.value.copy(
            settings = settings,
            privacyStatus = privacy.status(settings)
        )
    }

    private fun onPageState(
        tabId: Int,
        url: String,
        title: String,
        progress: Int,
        loading: Boolean,
        error: String?,
        incognito: Boolean
    ) {
        if (!loading && progress >= 100) historyRepo.add(url, title, incognito)
        val active = tabManager.activeTabId == tabId
        _state.value = _state.value.copy(
            progress = if (active) progress else _state.value.progress,
            loading = if (active) loading else _state.value.loading,
            error = if (active) error else _state.value.error,
            activeUrl = if (active) url else _state.value.activeUrl,
            activeTitle = if (active) title else _state.value.activeTitle,
            activeIncognito = if (active) incognito else _state.value.activeIncognito,
            history = historyRepo.all(),
            tabs = tabManager.snapshots(),
            activeTabId = tabManager.activeTabId
        )
    }

    private fun refreshTabs() {
        val active = tabManager.getActiveTab()
        _state.value = _state.value.copy(
            tabs = tabManager.snapshots(),
            activeTabId = tabManager.activeTabId,
            activeUrl = active?.url ?: "about:blank",
            activeTitle = active?.title ?: "New Tab",
            activeIncognito = active?.isIncognito ?: false,
            progress = active?.progress ?: 0,
            loading = active?.isLoading ?: false,
            error = active?.error
        )
    }

    fun openScreen(screen: Screen) {
        tabManager.setBrowserVisible(screen == Screen.Browser)
        _state.value = _state.value.copy(screen = screen, error = null)
    }

    override fun onCleared() {
        tabManager.destroyAll()
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RexFoxBrowserViewModel(context) as T
    }
}
