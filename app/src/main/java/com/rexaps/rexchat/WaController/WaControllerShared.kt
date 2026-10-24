package com.rexaps.rexchat

import android.view.ViewGroup
import android.webkit.PermissionRequest

enum class Login { LOADING, LOGGED_OUT, CHATS }

internal const val HOME_URL = "https://web.whatsapp.com/"

internal fun WaController.addLog(list: ArrayDeque<String>, s: String) {
    list.addLast(s.take(160))
    while (list.size > 6) list.removeFirst()
}

// =====================================================================
// Actions
// =====================================================================

internal fun WaController.toggleDesktopLayoutImpl() {
    desktopLayout = !desktopLayout
    applyViewport()
}

internal fun WaController.onPermissionsResultImpl(granted: Boolean) {
    val req = pendingPermission ?: return
    pendingPermission = null
    if (granted) {
        req.grant(
            req.resources.filter {
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                        it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }.toTypedArray()
        )
    } else {
        req.deny()
    }
}

internal fun WaController.destroyImpl() {
    if (destroyed) return
    destroyed = true
    handler.removeCallbacksAndMessages(null)
    pendingFileCallback?.onReceiveValue(null)
    pendingFileCallback = null
    pendingPermission?.deny()
    pendingPermission = null
    flush()
    runCatching {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
    }
}
