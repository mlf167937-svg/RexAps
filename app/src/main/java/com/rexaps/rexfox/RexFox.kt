
package com.rexaps.rexfox

import android.app.Activity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class RexFox(private val activity: Activity) {

    private lateinit var webView: WebView
    private lateinit var addressBar: EditText
    private lateinit var navigation: BrowserNavigation

    private val history = BrowserHistory()

    fun start() {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

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

        webView = WebView(activity).apply {
            webViewClient = object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    if (!url.isNullOrEmpty()) {
                        addressBar.setText(url)
                        history.add(url)
                    }
                }
            }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }

        navigation = BrowserNavigation(webView)

        root.addView(addressRow)
        root.addView(navigationRow)

        root.addView(
            webView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        goButton.setOnClickListener {
            loadAddress()
        }

        addressBar.setOnEditorActionListener { _, _, _ ->
            loadAddress()
            true
        }

        backButton.setOnClickListener {
            navigation.back()
        }

        forwardButton.setOnClickListener {
            navigation.forward()
        }

        reloadButton.setOnClickListener {
            navigation.reload()
        }

        activity.setContentView(root)

        webView.loadUrl("https://www.google.com")
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
        return navigation.back()
    }

    fun getHistory(): List<String> {
        return history.getAll()
    }

    fun clearHistory() {
        history.clear()
    }
}
