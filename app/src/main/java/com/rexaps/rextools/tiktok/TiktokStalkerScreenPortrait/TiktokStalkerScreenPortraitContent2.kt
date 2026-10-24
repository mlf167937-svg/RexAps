package com.rexaps.rextools.tiktok

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rexaps.rextools.ui.*

@Composable
internal fun __splitImpl_StatusPanel(scraping: Boolean, text: String) {
    val s = MaterialTheme.colorScheme
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(0.94f, tween(if (scraping) 9000 else 4000, easing = RexEase)) }
    val angle by rememberInfiniteTransition(label = "orbit")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "a")
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.25f, 0.8f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")

    RexPanel(glow = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .drawBehind {
                        val w = 4.dp.toPx()
                        drawCircle(
                            Brush.radialGradient(listOf(s.primary.copy(alpha = pulse * 0.5f), Color.Transparent)),
                            radius = size.minDimension
                        )
                        inset(w / 2) {
                            drawCircle(s.outlineVariant, style = Stroke(w))
                            drawArc(
                                brush = Brush.sweepGradient(listOf(s.primary, s.tertiary)),
                                startAngle = angle, sweepAngle = 110f, useCenter = false,
                                style = Stroke(w, cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = s.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RexOverline(if (scraping) "Mode Full" else "Mode Biasa")
                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        (fadeIn(tween(260, delayMillis = 80)) + slideInVertically(tween(320, easing = RexEase)) { it / 2 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically { -it / 2 })
                    },
                    label = "status"
                ) { t -> Text(t, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
            }
        }
        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(s.surfaceContainerHighest)) {
            Box(
                Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(s.primary, s.tertiary)))
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun __splitImpl_HiddenScraper(url: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
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

@Composable
internal fun __splitImpl_VideoTile(label: String, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
