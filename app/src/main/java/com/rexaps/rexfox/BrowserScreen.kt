package com.rexaps.rexfox

import android.net.Uri
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
import org.mozilla.geckoview.GeckoView

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
    onShareToRexAdm: () -> Unit,
    onNewTab: () -> Unit
) {
    val tab = tabs.getActiveTab() ?: return
    var editing by remember(tab.id) { mutableStateOf(false) }
    var address by remember(tab.id) { mutableStateOf(tab.url) }
    val focusRequester = remember { FocusRequester() }
    val isBookmarked = state.bookmarks.any { it.url == tab.url }
    var menuExpanded by remember(tab.id) { mutableStateOf(false) }
    val isSecure = Uri.parse(tab.url).scheme.equals("https", ignoreCase = true)

    LaunchedEffect(tab.url, editing) {
        if (!editing) address = tab.url
        else focusRequester.requestFocus()
    }

    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Column(
            Modifier.fillMaxWidth().background(SurfaceDark).statusBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        placeholder = { Text("Search or enter address") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = VioletLight) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { address = "" }) { Icon(Icons.Default.Close, "Clear") }
                                IconButton(onClick = { editing = false }) { Icon(Icons.Default.Cancel, "Cancel") }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            cursorColor = Cyan
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            editing = false
                            onNavigate(address.trim())
                        })
                    )
                } else {
                    Row(
                        Modifier.weight(1f).height(46.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                            .rexPressable(onClick = { address = tab.url; editing = true })
                            .padding(start = 13.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSecure) Icons.Default.Lock else Icons.Default.Info,
                            contentDescription = if (isSecure) "Secure connection" else "Connection information",
                            tint = if (isSecure) Cyan else OnSurfaceMuted,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(
                                tab.url.removePrefix("https://").removePrefix("http://").ifBlank { "Search or enter address" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (tab.isIncognito) {
                                Text("Private tab", color = Cyan, fontSize = 9.sp, lineHeight = 10.sp)
                            }
                        }
                        IconButton(onClick = { if (state.loading) onStop() else onReload() }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                if (state.loading) Icons.Default.Close else Icons.Default.Refresh,
                                if (state.loading) "Stop loading" else "Reload",
                                modifier = Modifier.size(19.dp), tint = VioletLight
                            )
                        }
                    }
                }

                IconButton(onClick = onBookmark) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        "Bookmark", tint = VioletLight
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, "Browser menu", tint = Cyan)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Send URL to RexADM") },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            onClick = { menuExpanded = false; onShareToRexAdm() }
                        )
                        DropdownMenuItem(
                            text = { Text("Developer tools") },
                            leadingIcon = { Icon(Icons.Default.Code, null) },
                            onClick = { menuExpanded = false; onDevTools() }
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp)) {
                if (state.loading) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = Cyan,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        state.error?.let { message ->
            Row(
                Modifier.fillMaxWidth().background(SurfaceCard).animateContentSize().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WifiOff, null, tint = Cyan)
                Spacer(Modifier.width(10.dp))
                Text(message, color = OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onReload) { Text("Retry") }
            }
        }

        key(tab.id) {
            AndroidView(
                factory = { context ->
                    GeckoView(context).apply {
                        setSession(tab.session)
                        tab.session.setActive(true)
                        tab.session.setFocused(true)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onRelease = { view ->
                    view.releaseSession()
                    tab.session.setFocused(false)
                    tab.session.setActive(false)
                }
            )
        }

        Row(
            Modifier.fillMaxWidth().background(SurfaceDark).navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = tab.canGoBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            IconButton(onClick = onForward, enabled = tab.canGoForward, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowForward, "Forward")
            }
            IconButton(onClick = onHome, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Home, "Home")
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(RexFoxAccent)
                    .rexPressable(onClick = onNewTab),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, "New tab", tint = Color.White) }
            IconButton(onClick = onTabs, modifier = Modifier.size(48.dp)) {
                Box(Modifier.size(26.dp).border(2.dp, OnSurfacePrimary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("${state.tabs.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }
    }
}

@Composable
fun TabOverview(
    state: BrowserUiState,
    onBack: () -> Unit,
    onNewTab: () -> Unit,
    onSwitch: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onCloseAll: () -> Unit
) {
    var confirmCloseAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Row(
            Modifier.fillMaxWidth().background(SurfaceDark).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text("Tabs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${state.tabs.size} open", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            if (state.tabs.isNotEmpty()) TextButton(onClick = { confirmCloseAll = true }) { Text("Close all") }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(RexFoxAccent).rexPressable(onClick = onNewTab),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, "New tab", tint = Color.White) }
            Spacer(Modifier.width(6.dp))
        }
        if (state.tabs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No open tabs", color = OnSurfaceMuted)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.tabs, key = { _, tab -> tab.id }) { index, tab ->
                    val active = tab.id == state.activeTabId
                    val shape = RoundedCornerShape(22.dp)
                    val letter = tab.title.ifBlank { tab.url }.firstOrNull()?.uppercase() ?: "N"
                    Row(
                        Modifier.fillMaxWidth().rexEntrance(index).rexPressable(onClick = { onSwitch(tab.id) })
                            .clip(shape).background(if (active) SurfaceElevated else SurfaceCard)
                            .border(if (active) 1.5.dp else 1.dp, if (active) Violet else BorderSubtle, shape)
                            .padding(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(RexFoxAccent), contentAlignment = Alignment.Center) {
                            Text(letter, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tab.title.ifBlank { "New Tab" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(tab.url, color = OnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (tab.isIncognito) Text("Private browsing", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            if (tab.isLoading) LinearProgressIndicator(progress = { tab.progress / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
                        }
                        IconButton(onClick = { onClose(tab.id) }) { Icon(Icons.Default.Close, "Close tab", tint = OnSurfaceMuted) }
                    }
                }
            }
        }
    }
    if (confirmCloseAll) {
        AlertDialog(
            onDismissRequest = { confirmCloseAll = false },
            title = { Text("Close all tabs?") },
            text = { Text("All regular and private tabs will be closed. Private sessions will be discarded.") },
            confirmButton = { TextButton(onClick = { confirmCloseAll = false; onCloseAll() }) { Text("Close all") } },
            dismissButton = { TextButton(onClick = { confirmCloseAll = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun BrowserEntriesScreen(
    title: String,
    entries: List<Pair<String, String>>,
    emptyMessage: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: ((Int) -> Unit)? = null
) {
    Column(Modifier.fillMaxSize().background(DeepBlack).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyMessage, color = OnSurfaceMuted) }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(entries) { index, item ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SurfaceCard)
                            .rexPressable(onClick = { onOpen(item.second) }).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.first.ifBlank { item.second }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.second, color = OnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (onRemove != null) IconButton(onClick = { onRemove(index) }) { Icon(Icons.Default.DeleteOutline, "Remove") }
                    }
                }
            }
        }
    }
}
