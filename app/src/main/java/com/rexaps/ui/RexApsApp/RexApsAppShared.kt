// RexApsApp.kt
package com.rexaps.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** State & aksi yang dibagikan ke shell portrait maupun landscape. */
internal class RexShell(
    val selectedTab: Int,
    val themeOption: RexThemeOption,
    val onSelectTab: (Int) -> Unit,
    val onAppClick: (RexModule) -> Unit,
    val onOpenTheme: () -> Unit
)

/* ═════════════════════ ROOT ═════════════════════ */

@Composable
internal fun Modifier.entrance(index: Int): Modifier {

    var shown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!shown) delay(index.coerceAtMost(8) * 60L)
        shown = true
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "entrance"
    )

    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 48.dp.toPx()
        val s = 0.94f + 0.06f * progress
        scaleX = s
        scaleY = s
    }
}

@Composable
internal fun accentBrush(): Brush =
    Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

/* ═════════════════════ Komponen bersama app ═════════════════════ */

@Composable
internal fun AppIcon(letter: String, size: Dp, modifier: Modifier = Modifier) {

    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.32f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = 0.5f),
                spotColor = colors.primary.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(accentBrush()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onPrimary
        )
    }
}

internal data class SocialLink(
    val title: String,
    val subtitle: String,
    val symbol: String,
    val color: Color,
    val url: String
)

internal val socialLinks = listOf(
    SocialLink(
        title = "YouTube",
        subtitle = "@RexRuinCraft",
        symbol = "▶",
        color = Color(0xFFFF0000),
        url = "https://www.youtube.com/@RexRuinCraft"
    ),
    SocialLink(
        title = "TikTok",
        subtitle = "@rexstudio4",
        symbol = "♪",
        color = Color(0xFF111111),
        url = "https://www.tiktok.com/@rexstudio4"
    ),
    SocialLink(
        title = "Saweria",
        subtitle = "Dukung lewat donasi",
        symbol = "☕",
        color = Color(0xFFF5A623),
        url = "https://saweria.co/rexruincraft"
    ),
    SocialLink(
        title = "Saluran WhatsApp",
        subtitle = "Gabung untuk update terbaru",
        symbol = "✆",
        color = Color(0xFF25D366),
        url = "https://whatsapp.com/channel/0029Vb8oqCTDOQIYrRuWic33"
    )
)
