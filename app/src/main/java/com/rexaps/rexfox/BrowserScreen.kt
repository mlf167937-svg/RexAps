package com.rexaps.rexfox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView

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
    var address by remember(tab.id, tab.url) { mutableStateOf(tab.url.takeUnless { it == "about:blank" }.orEmpty()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val isBookmarked = state.bookmarks.any { it.url == tab.url }

    LaunchedEffect(editing) { if (editing) focusRequester.requestFocus() }

    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Column(Modifier.fillMaxWidth().background(SurfaceDark).statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 9.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(RexFoxAccent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Explore, null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("RexFox", fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 19.sp)
                    Text(if (tab.isIncognito) "Private tab" else "Fast, focused browsing", color = OnSurfaceMuted, fontSize = 10.sp)
                }
                Surface(color = SurfaceCard, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tab, null, tint = VioletLight, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("${state.tabs.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "More options", tint = OnSurfacePrimary)
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("New tab") }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { menuExpanded = false; onNewTab() })
                        DropdownMenuItem(text = { Text(if (isBookmarked) "Remove bookmark" else "Add bookmark") }, leadingIcon = { Icon(Icons.Default.BookmarkBorder, null) }, onClick = { menuExpanded = false; onBookmark() })
                        DropdownMenuItem(text = { Text("Developer tools") }, leadingIcon = { Icon(Icons.Default.Code, null) }, onClick = { menuExpanded = false; onDevTools() })
                        DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { menuExpanded = false; onSettings() })
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp),
                        placeholder = { Text("Search or enter web address") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { IconButton(onClick = { editing = false }) { Icon(Icons.Default.Close, "Cancel") } },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet, unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                            cursorColor = Cyan
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { editing = false; onNavigate(address) })
                    )
                } else {
                    Row(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(17.dp))
                            .background(SurfaceCard).border(1.dp, BorderSubtle, RoundedCornerShape(17.dp))
                            .rexPressable(onClick = { address = tab.url.takeUnless { it == "about:blank" }.orEmpty(); editing = true })
                            .padding(start = 13.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (tab.url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                            null, tint = if (tab.url.startsWith("https://")) Cyan else OnSurfaceMuted,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            tab.url.removePrefix("https://").removePrefix("http://").takeUnless { it == "about:blank" } ?: "Search or enter address",
                            Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (tab.url == "about:blank") OnSurfaceMuted else OnSurfacePrimary, fontSize = 13.sp
                        )
                        IconButton(onClick = { if (state.loading) onStop() else onReload() }, modifier = Modifier.size(38.dp)) {
                            Icon(if (state.loading) Icons.Default.Close else Icons.Default.Refresh, if (state.loading) "Stop" else "Reload", modifier = Modifier.size(19.dp))
                        }
                    }
                    IconButton(onClick = onBookmark, modifier = Modifier.size(42.dp)) {
                        Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark", tint = if (isBookmarked) VioletLight else OnSurfacePrimary)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp)) {
                if (state.loading) LinearProgressIndicator(
                    progress = { state.progress / 100f }, modifier = Modifier.fillMaxSize(),
                    color = Cyan, trackColor = Color.Transparent
                )
            }
        }

        state.error?.let {
            Row(Modifier.fillMaxWidth().background(SurfaceCard).animateContentSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WifiOff, null, tint = Cyan)
                Spacer(Modifier.width(10.dp))
                Text(it, color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }

        key(tab.id) {
            AndroidView(factory = { tab.webView }, modifier = Modifier.weight(1f))
        }

        Row(
            Modifier.fillMaxWidth().background(SurfaceDark).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationIcon(Icons.Default.ArrowBack, "Back", enabled = tab.canGoBack, onClick = onBack)
            NavigationIcon(Icons.Default.ArrowForward, "Forward", enabled = tab.canGoForward, onClick = onForward)
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home", tint = OnSurfacePrimary) }
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)).background(RexFoxAccent).rexPressable(onClick = onNewTab), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, "New tab", tint = Color.White)
            }
            IconButton(onClick = onTabs) {
                Box(Modifier.size(25.dp).border(1.7.dp, OnSurfacePrimary, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                    Text("${state.tabs.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = OnSurfacePrimary) }
        }
    }
}

@Composable
private fun NavigationIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, description, tint = if (enabled) OnSurfacePrimary else OnSurfaceMuted.copy(alpha = 0.4f))
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
        Row(
            Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text("Tabs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${state.tabs.size} terbuka", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .size(44.dp)
                    .rexPressable(onClick = onNewTab)
                    .clip(CircleShape)
                    .background(RexFoxAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "New tab", tint = Color.White)
            }
            Spacer(Modifier.width(6.dp))
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(state.tabs, key = { _, tab -> tab.id }) { index, tab ->
                val active = tab.id == state.activeTabId
                val shape = RoundedCornerShape(22.dp)
                val letter = tab.title.ifBlank { tab.url }.firstOrNull()?.uppercase() ?: "N"

                Row(
                    Modifier
                        .fillMaxWidth()
                        .rexEntrance(index)
                        .rexPressable(onClick = { onSwitch(tab.id) })
                        .clip(shape)
                        .background(if (active) SurfaceElevated else SurfaceCard)
                        .border(if (active) 1.5.dp else 1.dp, if (active) Violet else BorderSubtle, shape)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) RexFoxAccent else androidx.compose.ui.graphics.SolidColor(SurfaceElevated)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letter, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            tab.title.ifBlank { "New Tab" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            tab.url,
                            color = OnSurfaceMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tab.isIncognito) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Incognito",
                                color = Cyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = { onClose(tab.id) }) {
                        Icon(Icons.Default.Close, "Close tab", tint = OnSurfaceMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(screen: Screen, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Spacer(Modifier.height(24.dp))
        Text(screen.name, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "This route is wired for the upgraded architecture. Keep the existing DevTools implementation here when integrating it.",
            color = OnSurfaceMuted
        )
    }
}
