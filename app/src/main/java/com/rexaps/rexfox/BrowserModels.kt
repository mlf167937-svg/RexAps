package com.rexaps.rexfox

import android.webkit.WebView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Bookmark(
    val url: String,
    val title: String,
    val id: Long = System.currentTimeMillis()
)

data class HistoryEntry(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
}

enum class SearchEngine(val label: String, val template: String?) {
    REXFOX("RexFox", null),
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BRAVE("Brave", "https://search.brave.com/search?q=%s");

    fun searchUrl(query: String): String? =
        template?.format(java.net.URLEncoder.encode(query, Charsets.UTF_8.name()))
}

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = true,
    val desktopSite: Boolean = false,
    val doNotTrack: Boolean = false,
    val trackingProtectionEnabled: Boolean = false,
    val askWhereToSaveDownloads: Boolean = false
)

data class BrowserTab(
    val id: Int,
    val title: String = "New Tab",
    val url: String = "about:blank",
    val webView: WebView,
    val isIncognito: Boolean = false
)

data class TabSnapshot(
    val id: Int,
    val title: String,
    val url: String,
    val isIncognito: Boolean
)
