package com.rexaps.rexfox

import android.app.Activity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.activity.compose.setContent

class RexFox(private val activity: Activity) {

    private lateinit var webView: WebView
    private lateinit var navigation: BrowserNavigation

    private val history = BrowserHistory()

    fun start() {
        showHome()
    }

    private fun showHome() {
        activity.setContent {
            RexFoxTheme {
                RexFoxHome(
                    onSearch = { query ->
                        openWebView(query)
                    },
                    onSettings = {
                        // Settings akan kita buat berikutnya.
                    }
                )
            }
        }
    }

    private fun openWebView(query: String) {
        val url = normalizeUrl(query)

        webView = WebView(activity).apply {
            webViewClient = object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    if (!url.isNullOrEmpty()) {
                        history.add(url)
                    }
                }
            }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }

        navigation = BrowserNavigation(webView)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(
            webView,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        activity.setContentView(root)

        webView.loadUrl(url)
    }

    private fun normalizeUrl(input: String): String {
        var url = input.trim()

        if (url.isEmpty()) {
            return "https://www.google.com"
        }

        if (!url.startsWith("http://") &&
            !url.startsWith("https://")
        ) {
            url = "https://www.google.com/search?q=$url"
        }

        return url
    }

    fun goBack(): Boolean {
        return if (::navigation.isInitialized) {
            navigation.back()
        } else {
            false
        }
    }

    fun getHistory(): List<String> {
        return history.getAll()
    }

    fun clearHistory() {
        history.clear()
    }
}
