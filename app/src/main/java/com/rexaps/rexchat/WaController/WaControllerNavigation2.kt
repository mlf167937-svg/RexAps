package com.rexaps.rexchat

import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebView

internal fun WaController.__split_checkBlank_4() {
        if (destroyed || error != null) return
        webView.evaluateJavascript(
            "(function(){var b=document.body;if(!b)return 'nobody';" +
                    "var t=(b.innerText||'').trim().length;" +
                    "var c=document.querySelectorAll('canvas,#app,#pane-side,input').length;" +
                    "return t+'|'+c+'|'+document.readyState;})()"
        ) { raw ->
            if (destroyed || error != null) return@evaluateJavascript
            val v = raw?.trim('"') ?: ""
            val parts = v.split("|")
            val text = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val nodes = parts.getOrNull(1)?.toIntOrNull() ?: 0
            if (v == "nobody" || (text < 5 && nodes == 0)) {
                val sb = StringBuilder("Halaman WhatsApp Web kosong.\n\n")
                sb.append("UA: ").append(webView.settings.userAgentString.takeLast(40)).append("\n")
                sb.append("Hasil: ").append(v).append("\n\n")
                if (consoleLog.isNotEmpty()) {
                    sb.append("Error JS:\n").append(consoleLog.joinToString("\n")).append("\n\n")
                }
                if (netLog.isNotEmpty()) {
                    sb.append("Jaringan:\n").append(netLog.joinToString("\n")).append("\n\n")
                }
                sb.append("Update Android System WebView & Chrome di Play Store, lalu coba lagi. ")
                sb.append("Kirim teks ini ke developer.")
                error = sb.toString()
            }
        }
    }

internal fun WaController.__split_probe_5() {
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

internal fun WaController.__split_applyViewport_6() {
        val content = if (desktopLayout) "width=1100" else "width=device-width, initial-scale=1"
        webView.evaluateJavascript(
            "(function(){var m=document.querySelector('meta[name=viewport]');" +
                    "if(!m){m=document.createElement('meta');m.name='viewport';" +
                    "document.head.appendChild(m);}m.content='$content';})()",
            null
        )
    }

fun WaController.__split_onPermissionsResult_7(granted: Boolean) {
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

fun WaController.__split_destroy_8() {
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
