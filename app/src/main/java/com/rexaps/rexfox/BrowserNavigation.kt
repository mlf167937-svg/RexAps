package com.rexaps.rexfox

import android.webkit.WebView

class BrowserNavigation(
    private val webView: WebView
) {

    fun back(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun forward(): Boolean {
        return if (webView.canGoForward()) {
            webView.goForward()
            true
        } else {
            false
        }
    }

    fun reload() {
        webView.reload()
    }
}
