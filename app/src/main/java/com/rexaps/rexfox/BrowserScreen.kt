package com.rexaps.rexfox

import android.view.ViewGroup
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
    val focusRequester = remember { FocusRequester() }
    val isBookmarked = state.bookmarks.any { it.url == tab.url }

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    Column(Modifier.fillMaxSize().background(DeepBlack)) {

        /* ------------------------------ TOP BAR ------------------------------ */
        Column(
            Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        placeholder = { Text("Search or enter address") },
                        trailingIcon = {
                            IconButton(onClick = { editing = false }) {
                                Icon(Icons.Default.Close, "Cancel")
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
                            onNavigate(address)
                        })
                    )
                } else {
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                            .rexPressable(onClick = { editing = true })
                            .padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = VioletLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            tab.url.removePrefix("https://").removePrefix("http://"),
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = { if (state.loading) onStop() else onReload() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                if (state.loading) Icons.Default.Close else Icons.Default.Refresh,
                                if (state.loading) "Stop" else "Reload",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onBookmark) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        "Bookmark",
                        tint = VioletLight
                    )
                }
                IconButton(onClick = onDevTools) {
                    Icon(Icons.Default.Code, "Developer tools", tint = Cyan)
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

        /* ------------------------------- ERROR ------------------------------- */
        state.error?.let {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .animateContentSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WifiOff, null, tint = Cyan)
                Spacer(Modifier.width(10.dp))
                Text(it, color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }

        /* ------------------------------- WEBVIEW ----------------------------- */
        key(tab.id) {
            AndroidView(
                factory = {
                    // Cegah crash "child already has a parent" saat berpindah layar/tab.
                    (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
                    tab.webView
                },
                modifier = Modifier.weight(1f),
                update = { }
            )
        }

        /* ----------------------------- BOTTOM BAR ---------------------------- */
        Row(
            Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = tab.webView.canGoBack()) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            IconButton(onClick = onForward, enabled = tab.webView.canGoForward()) {
                Icon(Icons.Default.ArrowForward, "Forward")
            }

            Box(
                Modifier
                    .size(50.dp)
                    .rexPressable(onClick = onNewTab)
                    .clip(CircleShape)
                    .background(RexFoxAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "New tab", tint = Color.White)
            }

            IconButton(onClick = onTabs) {
                Box(
                    Modifier
                        .size(26.dp)
                        .border(2.dp, OnSurfacePrimary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${state.tabs.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home") }
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
