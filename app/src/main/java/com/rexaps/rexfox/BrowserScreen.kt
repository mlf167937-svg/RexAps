package com.rexaps.rexfox

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BrowserScreen(
    initialUrl: String,
    tabManager: BrowserTabManager,
    history: BrowserHistory,
    bookmarkManager: BookmarkManager,
    onNewTab: (String) -> Unit,
    onCloseTab: (Int) -> Unit,
    onSwitchTab: (Int) -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val tab = tabManager.getActiveTab() ?: return

    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("Loading…") }
    var urlBarText by remember { mutableStateOf(initialUrl) }
    var loadProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showDevTools by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var devHtml by remember { mutableStateOf("") }
    var devInfo by remember { mutableStateOf(PageInfo()) }
    var isBookmarked by remember { mutableStateOf(bookmarkManager.isBookmarked(initialUrl)) }
    var editingUrl by remember { mutableStateOf(false) }

    if (showDevTools) {
        DevToolsScreen(
            html = devHtml,
            pageInfo = devInfo,
            onDismiss = { showDevTools = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // ── Tab strip ────────────────────────────────────────────
        AnimatedVisibility(visible = showTabs) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tabManager.getTabs()) { t ->
                    val active = t.id == tabManager.activeTabId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) SurfaceElevated else SurfaceCard)
                            .clickable { onSwitchTab(t.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            t.title.take(16),
                            fontSize = 12.sp,
                            color = if (active) OnSurfacePrimary else OnSurfaceMuted,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Close, null,
                            tint = OnSurfaceMuted,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCloseTab(t.id) }
                        )
                    }
                }
                item {
                    IconButton(
                        onClick = { onNewTab(currentUrl) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Cyan)
                    }
                }
            }
        }

        // ── Progress bar ─────────────────────────────────────────
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Cyan,
                trackColor = Color.Transparent
            )
        }

        // ── URL / Navigation bar ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            IconButton(onClick = {
                if (tab.webView.canGoBack()) tab.webView.goBack()
            }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }

            // Forward
            IconButton(onClick = {
                if (tab.webView.canGoForward()) tab.webView.goForward()
            }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowForward, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }

            // Reload / Stop
            IconButton(onClick = {
                if (isLoading) tab.webView.stopLoading() else tab.webView.reload()
            }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp)
                )
            }

            // URL bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .clickable { editingUrl = true }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                if (editingUrl) {
                    BasicUrlField(
                        value = urlBarText,
                        onDone = { input ->
                            editingUrl = false
                            val url = normalizeUrl(input)
                            urlBarText = url
                            tab.webView.loadUrl(url)
                        },
                        onDismiss = { editingUrl = false }
                    )
                } else {
                    Text(
                        pageTitle.ifBlank { currentUrl },
                        fontSize = 13.sp,
                        color = OnSurfacePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bookmark toggle
            IconButton(onClick = {
                if (isBookmarked) {
                    bookmarkManager.remove(currentUrl)
                } else {
                    bookmarkManager.add(currentUrl, pageTitle)
                }
                isBookmarked = !isBookmarked
            }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    null,
                    tint = if (isBookmarked) VioletLight else OnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Tabs button
            IconButton(onClick = { showTabs = !showTabs }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Tab, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }

            // DevTools
            IconButton(onClick = {
                DevTools.extractPageSource(tab.webView) { html ->
                    devHtml = html
                    DevTools.extractPageInfo(tab.webView) { info ->
                        devInfo = info
                        showDevTools = true
                    }
                }
            }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Code, null, tint = Cyan, modifier = Modifier.size(18.dp))
            }
        }

        // ── WebView ───────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = {
                tab.webView.apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!url.isNullOrEmpty()) {
                                currentUrl = url
                                urlBarText = url
                                isLoading = false
                                history.add(url, view?.title ?: url)
                                tabManager.updateTab(tab.id, title = view?.title ?: url, url = url)
                                isBookmarked = bookmarkManager.isBookmarked(url)
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            pageTitle = title ?: ""
                        }
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadProgress = newProgress
                            isLoading = newProgress < 100
                        }
                    }
                    loadUrl(initialUrl)
                }
            },
            update = { /* state-driven via webView directly */ }
        )

        // ── Bottom bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHome) {
                Icon(Icons.Default.Home, "Home", tint = OnSurfaceMuted)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = OnSurfaceMuted)
            }
        }
    }
}

@Composable
fun BasicUrlField(
    value: String,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(value) }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = OnSurfacePrimary,
            unfocusedTextColor = OnSurfacePrimary
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { onDone(text) }
        )
    )
}

fun normalizeUrl(input: String): String {
    val url = input.trim()
    if (url.isEmpty()) return "https://www.google.com"
    return if (url.startsWith("http://") || url.startsWith("https://")) {
        url
    } else if (url.contains(".") && !url.contains(" ")) {
        "https://$url"
    } else {
        "https://www.google.com/search?q=${url.replace(" ", "+")}"
    }
}
