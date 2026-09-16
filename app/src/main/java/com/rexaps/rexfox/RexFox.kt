package com.rexaps.rexfox

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class RexFox(private val activity: Activity) {

    private lateinit var webView: WebView
    private lateinit var addressBar: EditText

    fun start() {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        val topBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        addressBar = EditText(activity).apply {
            hint = "Masukkan alamat..."
            setText("https://www.google.com")
        }

        val goButton = Button(activity).apply {
            text = "GO"
        }

        topBar.addView(
            addressBar,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        topBar.addView(
            goButton,
            LinearLayout.LayoutParams(-2, -2)
        )

        webView = WebView(activity).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl("https://www.google.com")
        }

        root.addView(topBar)
        root.addView(
            webView,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        goButton.setOnClickListener {
            loadAddress()
        }

        activity.setContentView(root)
    }

    private fun loadAddress() {
        var url = addressBar.text.toString().trim()

        if (url.isEmpty()) return

        if (!url.startsWith("http://") &&
            !url.startsWith("https://")) {
            url = "https://$url"
        }

        addressBar.setText(url)
        webView.loadUrl(url)
    }

    fun goBack(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }
}