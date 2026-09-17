package com.rexaps.rexfox

import android.app.Activity
import androidx.compose.runtime.*

@Composable
fun RexFoxScreen(activity: Activity) {
    val tabManager = remember { BrowserTabManager(activity) }
    val history = remember { BrowserHistory() }
    val bookmarkManager = remember { BookmarkManager() }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    RexFoxTheme {
        when (val s = screen) {
            is Screen.Home -> RexFoxHome(
                onSearch = { query ->
                    val url = normalizeUrl(query)
                    val tab = tabManager.createTab()
                    screen = Screen.Browser(url, tab.id)
                },
                onSettings = { screen = Screen.Settings },
                historyEntries = history.getAll(),
                bookmarks = bookmarkManager.getAll()
            )

            is Screen.Browser -> BrowserScreen(
                initialUrl = s.url,
                tabManager = tabManager,
                history = history,
                bookmarkManager = bookmarkManager,
                onNewTab = { url ->
                    val tab = tabManager.createTab()
                    screen = Screen.Browser(url, tab.id)
                },
                onCloseTab = { id ->
                    tabManager.closeTab(id)
                    val active = tabManager.getActiveTab()
                    screen = if (active != null) Screen.Browser(active.url, active.id)
                    else Screen.Home
                },
                onSwitchTab = { id ->
                    tabManager.switchTab(id)
                    val tab = tabManager.getActiveTab()
                    if (tab != null) screen = Screen.Browser(tab.url, tab.id)
                },
                onHome = { screen = Screen.Home },
                onSettings = { screen = Screen.Settings }
            )

            is Screen.Settings -> SettingsScreen(
                onBack = { screen = Screen.Home },
                onClearHistory = { history.clear() }
            )
        }
    }
}

sealed class Screen {
    object Home : Screen()
    data class Browser(val url: String, val tabId: Int) : Screen()
    object Settings : Screen()
}
