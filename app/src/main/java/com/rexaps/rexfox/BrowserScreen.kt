package com.rexaps.rexfox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BrowserScreen(
    state: BrowserUiState,
    tabs: BrowserTabManager,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onBookmark: () -> Unit,
    onTabs: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onDevTools: () -> Unit,
    onNewTab: () -> Unit
) {
    val tab = tabs.getActiveTab() ?: return
    var editing by remember(tab.id) { mutableStateOf(false) }
    var address by remember(tab.id, tab.url) { mutableStateOf(tab.url) }

    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        if (state.loading) {
            LinearProgressIndicator(
                progress = { state.progress / 100f },
                Modifier.fillMaxWidth().height(2.dp),
                color = Cyan,
                trackColor = androidx.compose.ui.graphics.Color.Transparent
            )
        }

        Row(Modifier.fillMaxWidth().background(SurfaceDark).padding(5.dp)) {
            IconButton(onClick = onBack, enabled = tab.webView.canGoBack()) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            IconButton(onClick = onForward, enabled = tab.webView.canGoForward()) {
                Icon(Icons.Default.ArrowForward, "Forward")
            }
            IconButton(onClick = { if (state.loading) onStop() else onReload() }) {
                Icon(if (state.loading) Icons.Default.Close else Icons.Default.Refresh,
                    if (state.loading) "Stop" else "Reload")
            }

            if (editing) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search or enter address") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = {
                        editing = false
                        onNavigate(address)
                    })
                )
            } else {
                Surface(
                    Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp),
                    color = SurfaceCard,
                    onClick = { editing = true }
                ) {
                    Text(
                        tab.url.removePrefix("https://").removePrefix("http://"),
                        Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onBookmark) {
                Icon(
                    if (state.bookmarks.any { it.url == tab.url }) Icons.Default.Bookmark
                    else Icons.Default.BookmarkBorder,
                    "Bookmark",
                    tint = VioletLight
                )
            }
            IconButton(onClick = onTabs) { Icon(Icons.Default.Tab, "Tabs") }
            IconButton(onClick = onDevTools) { Icon(Icons.Default.Code, "Developer tools", tint = Cyan) }
        }

        state.error?.let {
            Surface(Modifier.fillMaxWidth(), color = SurfaceCard) {
                Row(Modifier.padding(10.dp)) {
                    Icon(Icons.Default.WifiOff, null)
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = OnSurfaceMuted, fontSize = 12.sp)
                }
            }
        }

        AndroidView(
            Modifier.weight(1f),
            factory = { tab.webView },
            update = { }
        )

        Row(Modifier.fillMaxWidth().background(SurfaceDark), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home") }
            IconButton(onClick = onNewTab) { Icon(Icons.Default.Add, "New tab") }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
        }
    }
}

@Composable
fun TabOverview(
    state: BrowserUiState,
    onBack: () -> Unit,
    onNewTab: () -> Unit,
    onSwitch: (Int) -> Unit,
    onClose: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Row(Modifier.fillMaxWidth().background(SurfaceDark).padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Tabs", Modifier.weight(1f))
            IconButton(onClick = onNewTab) { Icon(Icons.Default.Add, "New tab") }
        }

        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(state.tabs, key = { it.id }) { tab ->
                Surface(
                    Modifier.fillMaxWidth(),
                    color = if (tab.id == state.activeTabId) SurfaceElevated else SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (tab.id == state.activeTabId) Violet else BorderSubtle
                    ),
                    onClick = { onSwitch(tab.id) }
                ) {
                    Row(Modifier.padding(13.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(tab.title.ifBlank { "New Tab" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(tab.url, color = OnSurfaceMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (tab.isIncognito) Text("Incognito", color = Cyan, fontSize = 10.sp)
                        }
                        IconButton(onClick = { onClose(tab.id) }) {
                            Icon(Icons.Default.Close, "Close tab")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(screen: Screen, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(DeepBlack).padding(24.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Spacer(Modifier.height(24.dp))
        Text(screen.name, style = MaterialTheme.typography.headlineSmall)
        Text("This route is wired for the upgraded architecture. Keep the existing DevTools implementation here when integrating it.", color = OnSurfaceMuted)
    }
}
