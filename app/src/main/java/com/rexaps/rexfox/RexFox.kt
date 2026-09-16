package com.rexaps.rexfox

import android.app.Activity
import android.graphics.Color
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

        // Address bar
        val addressRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        addressBar = EditText(activity).apply {
            hint = "Masukkan alamat..."
            setSingleLine(true)
            setText("https://www.google.com")
        }

        val goButton = Button(activity).apply {
            text = "GO"
        }

        addressRow.addView(
            addressBar,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        addressRow.addView(
            goButton,
            LinearLayout.LayoutParams(-2, -2)
        )

        // Navigation buttons
        val navigationRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val backButton = Button(activity).apply {
            text = "←"
        }

        val forwardButton = Button(activity).apply {
            text = "→"
        }

        val reloadButton = Button(activity).apply {
            text = "↻"
        }

        navigationRow.addView(
            backButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        navigationRow.addView(
            forwardButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        navigationRow.addView(
            reloadButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        // WebView
        webView = WebView(activity).apply {
            webViewClient = WebViewClient()

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            loadUrl("https://www.google.com")
        }

        root.addView(addressRow)
        root.addView(navigationRow)

        root.addView(
            webView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // GO
        goButton.setOnClickListener {
            loadAddress()
        }

        // Enter pada address bar
        addressBar.setOnEditorActionListener { _, _, _ ->
            loadAddress()
            true
        }

        // Back
        backButton.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        // Forward
        forwardButton.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        // Reload
        reloadButton.setOnClickListener {
            webView.reload()
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