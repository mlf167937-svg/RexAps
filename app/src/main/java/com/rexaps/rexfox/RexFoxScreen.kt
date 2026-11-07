package com.rexaps.rexfox

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.rexaps.rexadm.RexAdmScreen

@Composable
fun RexFoxScreen(activity: Activity) {
    val owner = activity as? ViewModelStoreOwner
        ?: error("RexFoxScreen requires an Activity that implements ViewModelStoreOwner")
    val vm: RexFoxBrowserViewModel = ViewModelProvider(
        owner,
        RexFoxBrowserViewModel.Factory(activity.applicationContext)
    )[RexFoxBrowserViewModel::class.java]
    val state = vm.state.collectAsState().value

    DisposableEffect(vm) {
        onDispose { vm.tabManager.setBrowserVisible(false) }
    }

    RexFoxTheme {
        key(state.screen) {
            RexFoxFade {
                when (state.screen) {
                    Screen.Home -> RexFoxHome(
                        state = state,
                        onNavigate = vm::navigate,
                        onTabs = { vm.openScreen(Screen.Tabs) },
                        onBookmarks = { vm.openScreen(Screen.Bookmarks) },
                        onHistory = { vm.openScreen(Screen.History) },
                        onDownloads = { vm.openScreen(Screen.Downloads) },
                        onSettings = { vm.openScreen(Screen.Settings) },
                        onIncognito = vm::newIncognitoTab
                    )
                    Screen.Browser -> BrowserScreen(
                        state = state,
                        tabs = vm.tabManager,
                        onNavigate = vm::navigate,
                        onBack = vm::back,
                        onForward = vm::forward,
                        onReload = vm::reload,
                        onStop = vm::stop,
                        onBookmark = vm::toggleBookmark,
                        onTabs = { vm.openScreen(Screen.Tabs) },
                        onHome = { vm.openScreen(Screen.Home) },
                        onSettings = { vm.openScreen(Screen.Settings) },
                        onDevTools = { vm.openScreen(Screen.DevTools) },
                        onShareToRexAdm = vm::shareActiveUrlToRexAdm,
                        onNewTab = vm::newTab
                    )
                    Screen.Tabs -> TabOverview(
                        state = state,
                        onBack = { vm.openScreen(if (vm.tabManager.count() == 0) Screen.Home else Screen.Browser) },
                        onNewTab = vm::newTab,
                        onSwitch = vm::switchTab,
                        onClose = vm::closeTab,
                        onCloseAll = vm::closeAllTabs
                    )
                    Screen.Bookmarks -> BrowserEntriesScreen(
                        title = "Bookmarks",
                        entries = state.bookmarks.map { it.title to it.url },
                        emptyMessage = "No bookmarks saved yet.",
                        onBack = { vm.openScreen(Screen.Home) },
                        onOpen = vm::load,
                        onRemove = { index -> state.bookmarks.getOrNull(index)?.let { vm.removeBookmark(it.url) } }
                    )
                    Screen.History -> BrowserEntriesScreen(
                        title = "History",
                        entries = state.history.map { "${it.title} · ${it.formattedTime()}" to it.url },
                        emptyMessage = "No regular browsing history yet.",
                        onBack = { vm.openScreen(Screen.Home) },
                        onOpen = vm::load,
                        onRemove = { index -> state.history.getOrNull(index)?.let { vm.deleteHistory(it.timestamp) } }
                    )
                    Screen.Downloads -> RexAdmScreen(onExit = { vm.openScreen(Screen.Home) })
                    Screen.Settings -> SettingsScreen(
                        settings = state.settings,
                        onChange = vm::updateSettings,
                        onBack = { vm.openScreen(Screen.Home) },
                        onClearHistory = vm::clearHistory
                    )
                    else -> PlaceholderScreen(state.screen) { vm.openScreen(Screen.Home) }
                }
            }
        }
    }
}
