package com.rexaps.rexai

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun RexAIWebViewScreen(
    providerId: String,
    title: String,
    startUrl: String,
    holder: RexAIWebStateHolder,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var pageProgress by rememberSaveable(providerId) { mutableIntStateOf(0) }
    var isLoading by rememberSaveable(providerId) { mutableStateOf(true) }
    var pageError by rememberSaveable(providerId) { mutableStateOf<String?>(null) }
    var canGoBack by remember(providerId) { mutableStateOf(false) }

    val webView = remember(providerId) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                builtInZoomControls = false
                displayZoomControls = false
                setSupportZoom(false)
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccess = false
                allowContentAccess = false
                safeBrowsingEnabled = true
            }
            val currentWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(currentWebView, true)
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    pageProgress = newProgress.coerceIn(0, 100)
                    isLoading = newProgress < 100
                    canGoBack = view.canGoBack()
                    if (newProgress >= 100) pageError = null
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val target = request.url
                    val scheme = target.scheme.orEmpty().lowercase()
                    val host = target.host.orEmpty().lowercase()
                    if (scheme !in setOf("http", "https")) {
                        openExternal(context, target)
                        return true
                    }
                    // Google blocks embedded WebView sign-in. Hand account authorization to the user's browser.
                    if (host == "accounts.google.com" || host.endsWith(".accounts.google.com")) {
                        openExternal(context, target)
                        return true
                    }
                    if (isProviderHost(providerId, host)) return false
                    openExternal(context, target)
                    return true
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    isLoading = true
                    pageError = null
                    canGoBack = view.canGoBack()
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    isLoading = false
                    pageProgress = 100
                    canGoBack = view.canGoBack()
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) {
                        pageError = error.description?.toString() ?: "Unable to load this page"
                        isLoading = false
                    }
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    // Never bypass a certificate error. The user can retry or open the provider in a browser.
                    handler.cancel()
                    pageError = "Secure connection could not be verified. Check your connection and try again."
                    isLoading = false
                }
            }
            val saved = holder.takeState(providerId)
            val restored = saved?.let { restoreState(it) }
            if (restored == null) loadUrl(startUrl)
            else {
                canGoBack = canGoBack()
                isLoading = progress < 100
            }
        }
    }

    DisposableEffect(webView, providerId) {
        onDispose {
            runCatching {
                val state = Bundle()
                webView.saveState(state)
                holder.saveState(providerId, state)
                CookieManager.getInstance().flush()
                webView.stopLoading()
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            }
        }
    }

    BackHandler(enabled = canGoBack) {
        if (webView.canGoBack()) {
            webView.goBack()
            canGoBack = webView.canGoBack()
        } else onBack()
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = colors.surface, tonalElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = {
                    if (webView.canGoBack()) {
                        webView.goBack()
                        canGoBack = webView.canGoBack()
                    } else onBack()
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = if (canGoBack) "Back in page" else "Back to RexAI")
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Official website", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                IconButton(onClick = { webView.reload(); pageError = null }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
                }
                IconButton(onClick = { openExternal(context, Uri.parse(webView.url ?: startUrl)) }) {
                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Open current page in browser")
                }
            }
        }
        if (isLoading) {
            LinearProgressIndicator(
                progress = pageProgress.coerceIn(0, 100) / 100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
                update = { view -> canGoBack = view.canGoBack() }
            )
            if (pageError != null) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(22.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Page unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(pageError.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Surface(onClick = { pageError = null; webView.reload() }, color = colors.primaryContainer, shape = CircleShape) {
                            Text("Try again", modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = colors.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (isLoading && pageProgress == 0) {
                CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), strokeWidth = 2.dp)
            }
        }
    }
}

private fun isProviderHost(providerId: String, host: String): Boolean = when (providerId) {
    "chatgpt" -> host == "chatgpt.com" || host.endsWith(".chatgpt.com") || host == "openai.com" || host.endsWith(".openai.com") || host.endsWith(".oaistatic.com") || host.endsWith(".oaiusercontent.com")
    "claude" -> host == "claude.ai" || host.endsWith(".claude.ai") || host == "anthropic.com" || host.endsWith(".anthropic.com")
    "gemini" -> host == "gemini.google.com" || host == "google.com" || host.endsWith(".google.com") || host.endsWith(".googleusercontent.com") || host.endsWith(".gstatic.com")
    else -> false
}

private fun openExternal(context: android.content.Context, uri: Uri) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { error ->
        if (error !is ActivityNotFoundException) throw error
    }
}
