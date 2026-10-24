// TiktokStalkerScreen.kt
package com.rexaps.rextools.tiktok

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rexaps.rextools.ui.*

internal const val SCRAPE_JS = """
(function() {
    try {
        var el = document.getElementById('__UNIVERSAL_DATA_FOR_REHYDRATION__');
        if (el) { window.RexBridge.onResult(el.textContent); return; }
        var scripts = document.getElementsByTagName('script');
        for (var i = 0; i < scripts.length; i++) {
            if (scripts[i].id === '__UNIVERSAL_DATA_FOR_REHYDRATION__') {
                window.RexBridge.onResult(scripts[i].textContent);
                return;
            }
        }
        window.RexBridge.onError("Data TikTok tidak ditemukan.");
    } catch (e) {
        window.RexBridge.onError(e.message);
    }
})();
"""

internal const val SCRAPE_TIMEOUT_MS = 25_000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun HiddenScraper(url: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
    val resultCb by rememberUpdatedState(onResult)
    val errorCb by rememberUpdatedState(onError)

    Box(Modifier.size(1.dp).alpha(0.01f)) {
        AndroidView(
            factory = { c ->
                val main = Handler(Looper.getMainLooper())
                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        private var injected = false
                        override fun onPageFinished(view: WebView?, u: String?) {
                            super.onPageFinished(view, u)
                            if (injected) return
                            injected = true
                            view?.postDelayed({ view.evaluateJavascript(SCRAPE_JS, null) }, 4000)
                        }
                    }
                    // Callback jembatan JS datang dari thread lain -> pindahkan ke main thread
                    addJavascriptInterface(object : Any() {
                        @JavascriptInterface
                        fun onResult(json: String) { main.post { resultCb(json) } }

                        @JavascriptInterface
                        fun onError(msg: String) { main.post { errorCb(msg) } }
                    }, "RexBridge")
                    loadUrl(url)
                }
            },
            onRelease = {
                it.stopLoading()
                it.removeJavascriptInterface("RexBridge")
                it.destroy()
            }
        )
    }
}

/* ═════════════════════ MODE SWITCH ═════════════════════ */

@Composable
internal fun ModeSwitch(full: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RexMd)
            .background(s.surfaceContainerLow.copy(alpha = 0.85f))
            .border(hairline(), RexMd)
            .padding(4.dp)
    ) {
        val segW = (maxWidth - 8.dp) / 2
        val x by animateDpAsState(if (full) segW else 0.dp, spring(0.75f, Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segW)
                .fillMaxHeight()
                .clip(RexSm)
                .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.30f), s.tertiary.copy(alpha = 0.18f))))
                .border(1.dp, s.primary.copy(alpha = 0.5f), RexSm)
        )
        Row(Modifier.fillMaxSize()) {
            ModeOption("Biasa", "Profil & statistik", Icons.Default.Bolt, !full, enabled, { onChange(false) }, Modifier.weight(1f))
            ModeOption("Full", "Story & video", Icons.Default.Public, full, enabled, { onChange(true) }, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun ModeOption(
    title: String, hint: String, icon: ImageVector,
    selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier
) {
    val s = MaterialTheme.colorScheme
    val fg by animateColorAsState(if (selected) s.onSurface else s.onSurfaceVariant, tween(250), label = "fg")
    val tint by animateColorAsState(if (selected) s.primary else s.onSurfaceVariant, tween(250), label = "tint")
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, spring(0.6f), label = "sc")
    Row(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, enabled = enabled, onClick = onClick
            )
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fg)
            Text(hint, style = MaterialTheme.typography.labelSmall, color = s.onSurfaceVariant, maxLines = 1)
        }
    }
}

/* ═════════════════════ STATUS LOADING ═════════════════════ */

@Composable
internal fun VideoTile(label: String, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(116.dp)
            .entrance(index.coerceAtMost(9), 45, 16.dp)
            .pressScale(source, 0.94f)
            .clip(RexMd)
            .background(
                Brush.linearGradient(
                    listOf(s.primary.copy(alpha = 0.22f), s.surfaceContainerHigh, s.tertiary.copy(alpha = 0.16f))
                )
            )
            .border(hairline(), RexMd)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = s.onSurface, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = s.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
