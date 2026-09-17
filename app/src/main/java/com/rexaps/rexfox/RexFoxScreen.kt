package com.rexaps.rexfox

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RexFoxScreen(
    onSettings: () -> Unit
) {
    var query by remember {
        mutableStateOf("")
    }

    var showBrowser by remember {
        mutableStateOf(false)
    }

    if (!showBrowser) {
        RexFoxHome(
            onSearch = { input ->
                query = input.trim()

                if (query.isNotEmpty()) {
                    showBrowser = true
                }
            },
            onSettings = onSettings
        )
    } else {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    loadUrl(
                        if (
                            query.startsWith("http://") ||
                            query.startsWith("https://")
                        ) {
                            query
                        } else {
                            "https://www.google.com/search?q=$query"
                        }
                    )
                }
            }
        )
    }
}
