package com.rexaps.rexfox

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexFoxScreen(activity: Activity) {
    val vm: RexFoxBrowserViewModel = viewModel(
        factory = RexFoxBrowserViewModel.Factory(activity.applicationContext, activity)
    )
    val state = vm.state.collectAsStateWithLifecycle().value

    RexFoxTheme {
        when (state.screen) {
            Screen.Home -> RexFoxHome(
                state = state,
                onNavigate = vm::navigate,
                onTabs = { vm.openScreen(Screen.Tabs) },
                onBookmarks = { vm.openScreen(Screen.Bookmarks) },
                onHistory = { vm.openScreen(Screen.History) },
                onDownloads = { vm.openScreen(Screen.Downloads) },
                onSettings = { vm.openScreen(Screen.Settings) }
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
                onNewTab = vm::newTab
            )
            Screen.Tabs -> TabOverview(
                state = state,
                onBack = { vm.openScreen(Screen.Browser) },
                onNewTab = vm::newTab,
                onSwitch = vm::switchTab,
                onClose = vm::closeTab
            )
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
