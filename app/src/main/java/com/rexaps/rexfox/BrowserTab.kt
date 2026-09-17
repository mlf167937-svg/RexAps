package com.rexaps.rexfox

import android.webkit.WebView

data class BrowserTab(
    val id: Int,
    val title: String = "Tab Baru",
    val url: String = "https://www.google.com",
    val webView: WebView
)
