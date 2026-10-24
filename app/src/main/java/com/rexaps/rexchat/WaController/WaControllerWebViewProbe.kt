package com.rexaps.rexchat

// =====================================================================
// Deteksi login
// =====================================================================

internal fun WaController.probe() {
    if (error != null) return
    val url = webView.url ?: return
    if (!url.contains("whatsapp")) return

    webView.evaluateJavascript(
        "(function(){return document.querySelector('#pane-side')?'chats':'out';})()"
    ) { raw ->
        if (destroyed) return@evaluateJavascript
        if (raw != null && raw.contains("chats")) {
            if (login != Login.CHATS) {
                login = Login.CHATS
                SessionManager.markLinked(activity, sessionId, true)
            }
        } else {
            login = if (loading) Login.LOADING else Login.LOGGED_OUT
        }
    }
}

internal fun WaController.applyViewport() {
    val content = if (desktopLayout) "width=1100" else "width=device-width, initial-scale=1"
    webView.evaluateJavascript(
        "(function(){var m=document.querySelector('meta[name=viewport]');" +
                "if(!m){m=document.createElement('meta');m.name='viewport';" +
                "document.head.appendChild(m);}m.content='$content';})()",
        null
    )
}
