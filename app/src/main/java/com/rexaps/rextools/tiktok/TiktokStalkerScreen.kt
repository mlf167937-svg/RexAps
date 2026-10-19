package com.rexaps.rextools.tiktok

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rextools.TiktokStalkResult

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiktokStalkerScreen(
    onBack: () -> Unit = {},
    vm: TiktokStalkerViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    var webViewUrl by remember { mutableStateOf<String?>(null) }

    // WebView tersembunyi
    if (webViewUrl != null) {
        Box(modifier = Modifier.size(1.dp).alpha(0.01f)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Tunggu 3 detik biar JS TikTok selesai render
                                view?.postDelayed({
                                    view.evaluateJavascript("""
                                        (function() {
                                            try {
                                                var el = document.getElementById('__UNIVERSAL_DATA_FOR_REHYDRATION__');
                                                if(el) {
                                                    window.RexBridge.onResult(el.textContent);
                                                } else {
                                                    // Fallback kalo element gak ada, coba script lain
                                                    var scripts = document.getElementsByTagName('script');
                                                    for(var i=0; i<scripts.length; i++) {
                                                        if(scripts[i].id === '__UNIVERSAL_DATA_FOR_REHYDRATION__') {
                                                            window.RexBridge.onResult(scripts[i].textContent);
                                                            return;
                                                        }
                                                    }
                                                    window.RexBridge.onError("Data TikTok tidak ditemukan.");
                                                }
                                            } catch(e) {
                                                window.RexBridge.onError(e.message);
                                            }
                                        })();
                                    """.trimIndent(), null)
                                }, 4000)
                            }
                        }
                        addJavascriptInterface(object : Any() {
                            @JavascriptInterface
                            fun onResult(json: String) {
                                vm.onFullResultReceived(json)
                                webViewUrl = null // Tutup webview kalo udah dapet
                            }
                            @JavascriptInterface
                            fun onError(msg: String) {
                                // Handle error di VM lu kalo mau
                                webViewUrl = null
                            }
                        }, "RexBridge")
                        loadUrl(webViewUrl!!)
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TikTok Stalker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = vm::onUsernameChange,
                label = { Text("Username (tanpa @)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2 TOMBOL
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { vm.stalkBasic() },
                    enabled = !state.loading && webViewUrl == null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stalk Biasa", maxLines = 1)
                }
                Button(
                    onClick = {
                        if (state.username.isNotBlank()) {
                            webViewUrl = "https://www.tiktok.com/@${state.username.trim().removePrefix("@")}"
                        }
                    },
                    enabled = !state.loading && webViewUrl == null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stalk Full", maxLines = 1)
                }
            }

            if (state.loading || webViewUrl != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = if (webViewUrl != null) "Membuka WebView & Scraping... (Tunggu 5 detik)" else state.loadingText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            state.error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }

            // HASIL NEXRAY (BIASA)
            state.result?.let { r -> BasicProfileCard(r) }

            // HASIL WEBVIEW (FULL)
            state.fullResult?.let { r -> FullProfileCard(r) }
        }
    }
}

@Composable
private fun BasicProfileCard(r: TiktokStalkResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = r.avatar, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(40.dp)))
            Text(r.name ?: "-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("@${r.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            r.bio?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun FullProfileCard(r: com.rexaps.rextools.TiktokFullResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = r.avatar, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(r.name ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("@${r.username}", style = MaterialTheme.typography.bodySmall)
                    Text(r.bio ?: "-", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            if (r.stories.isNotEmpty()) {
                Text("Story (${r.stories.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                r.stories.forEach { (key, url) ->
                    OutlinedButton(onClick = { /* Handle open story */ }, modifier = Modifier.fillMaxWidth()) {
                        Text(key)
                    }
                }
            }

            if (r.videos.isNotEmpty()) {
                Text("Videos (${r.videos.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightIn(max = 300.dp)) {
                    items(r.videos.entries.toList()) { entry ->
                        Box(modifier = Modifier.height(100.dp).padding(4.dp).background(Color.Gray).clickable { /* Play video */ }) {
                            Text(entry.key, modifier = Modifier.align(Alignment.Center), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}