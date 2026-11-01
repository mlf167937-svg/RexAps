package com.rexaps.rexai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import android.net.http.SslError
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun RexAIWebViewScreen(
    provider: RexAIProvider,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    var lastUrl by rememberSaveable(provider.id) { mutableStateOf(provider.homeUrl) }
    var webView by remember(provider.id) { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    BackHandler {
        val current = webView
        if (current?.canGoBack() == true) {
            current.goBack()
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.surface,
                            colors.surfaceVariant.copy(alpha = 0.72f)
                        )
                    )
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Kembali ke RexAI"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Official web interface",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    pageError = null
                    webView?.reload()
                },
                enabled = webView != null
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Muat ulang"
                )
            }

            IconButton(
                onClick = {
                    openExternalBrowser(context, lastUrl)
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = "Buka di browser"
                )
            }
        }

        if (notice != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = colors.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = notice.orEmpty(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSecondaryContainer
                )
            }
        }

        if (loading) {
            LinearProgressIndicator(
                progress = progress.coerceIn(0, 100) / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.surfaceVariant
            )
        } else {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colors.surfaceVariant.copy(alpha = 0.45f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = true
                            }
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress.coerceIn(0, 100)
                                loading = newProgress < 100
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest
                            ): Boolean {
                                return handleRexAINavigation(
                                    context = view?.context ?: viewContext,
                                    uri = request.url,
                                    provider = provider,
                                    onExternalAuth = {
                                        notice = "Login Google dibuka di browser eksternal. Sesi browser mungkin tidak tersinkron ke WebView; gunakan tombol browser bila login tidak kembali otomatis."
                                    }
                                )
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                loading = true
                                progress = 0
                                pageError = null
                                if (url != null && provider.allowsHost(Uri.parse(url).host)) {
                                    lastUrl = url
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                progress = 100
                                if (url != null && provider.allowsHost(Uri.parse(url).host)) {
                                    lastUrl = url
                                }
                                // Persist the provider's WebView session across Activity recreation.
                                CookieManager.getInstance().flush()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
                                    pageError = error?.description?.toString()
                                        ?: "Halaman gagal dimuat. Periksa koneksi internet."
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true &&
                                    errorResponse != null &&
                                    errorResponse.statusCode >= 500
                                ) {
                                    loading = false
                                    pageError = "Server ${provider.title} sedang bermasalah (${errorResponse.statusCode})."
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.cancel()
                                loading = false
                                pageError = "Sertifikat keamanan situs tidak dapat diverifikasi. Koneksi dibatalkan."
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                loading = false
                                pageError = "Proses WebView berhenti. Tekan Muat Ulang untuk mencoba lagi."
                                return true
                            }
                        }

                        setDownloadListener { url, _, _, _, _ ->
                            openExternalBrowser(context, url)
                        }

                        webView = this
                        val startUrl = lastUrl.takeIf {
                            runCatching { provider.allowsHost(Uri.parse(it).host) }.getOrDefault(false)
                        } ?: provider.homeUrl
                        loadUrl(startUrl)
                    }
                },
                update = { view ->
                    if (view.url.isNullOrBlank()) {
                        view.loadUrl(provider.homeUrl)
                    }
                }
            )

            if (pageError != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    color = colors.errorContainer,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            text = "Tidak dapat memuat halaman",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pageError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onErrorContainer
                        )
                        Button(
                            onClick = {
                                pageError = null
                                webView?.reload()
                            }
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Coba lagi")
                        }
                    }
                }
            }

            if (webView == null && loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }
        }
    }

    DisposableEffect(provider.id) {
        onDispose {
            webView?.let { view ->
                runCatching {
                    view.stopLoading()
                    view.onPause()
                    view.removeAllViews()
                    view.webChromeClient = null
                    view.webViewClient = WebViewClient()
                    view.destroy()
                }
            }
            webView = null
        }
    }
}

private fun handleRexAINavigation(
    context: Context,
    uri: Uri,
    provider: RexAIProvider,
    onExternalAuth: () -> Unit
): Boolean {
    val scheme = uri.scheme?.lowercase().orEmpty()

    if (scheme == "about") return false

    if (scheme == "intent") {
        return runCatching {
            val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val fallback = intent.getStringExtra("browser_fallback_url")
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else if (!fallback.isNullOrBlank()) {
                openExternalBrowser(context, fallback)
            }
            true
        }.getOrDefault(true)
    }

    if (scheme == "https" || scheme == "http") {
        val host = uri.host?.lowercase().orEmpty()

        // Google's OAuth flow must use a browser, not an embedded WebView.
        if (host == "accounts.google.com" ||
            host == "consent.google.com" ||
            host == "oauth2.googleapis.com"
        ) {
            onExternalAuth()
            openExternalBrowser(context, uri.toString())
            return true
        }

        if (provider.allowsHost(host)) return false

        openExternalBrowser(context, uri.toString())
        return true
    }

    if (scheme in setOf("mailto", "tel", "sms", "market")) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        return true
    }

    return true
}

private fun openExternalBrowser(context: Context, url: String) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    if (uri.scheme !in setOf("http", "https")) return

    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        // The WebView remains available if the device has no browser handler.
    }
}
