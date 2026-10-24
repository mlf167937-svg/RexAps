// TiktokStalkerScreen.kt
package com.rexaps.rextools.tiktok

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.TiktokStalkResult
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

@Composable
fun TiktokStalkerScreenPortrait(
    onBack: () -> Unit = {},
    vm: TiktokStalkerViewModel = viewModel()
) {
        return __splitImpl_TiktokStalkerScreenPortrait(onBack, vm)
    }

/* ═════════════════════ WEBVIEW TERSEMBUNYI ═════════════════════ */

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun HiddenScraper(url: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        return __splitImpl_HiddenScraper(url, onResult, onError)
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
private fun ModeOption(
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
internal fun StatusPanel(scraping: Boolean, text: String) {
        return __splitImpl_StatusPanel(scraping, text)
    }

/* ═════════════════════ HEADER PROFIL (dipakai bersama) ═════════════════════ */

@Composable
internal fun ProfileHeader(
    avatar: Any?, name: String?, username: String?, bio: String?,
    verified: Boolean, region: String?
) {
        return __splitImpl_ProfileHeader(avatar, name, username, bio, verified, region)
    }

/* ═════════════════════ KARTU BASIC ═════════════════════ */

@Composable
internal fun BasicProfileCard(r: TiktokStalkResult, onOpen: (String) -> Unit) {
        return __splitImpl_BasicProfileCard(r, onOpen)
    }

/* ═════════════════════ KARTU FULL ═════════════════════ */

internal const val PREVIEW_COUNT = 6

@Composable
internal fun FullProfileCard(r: com.rexaps.rextools.TiktokFullResult, onOpen: (String) -> Unit) {
        return __splitImpl_FullProfileCard(r, onOpen)
    }

@Composable
internal fun StoryRow(label: String, index: Int, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    RexPanel(
        modifier = Modifier.entrance(index.coerceAtMost(8), 50, 14.dp),
        onClick = onClick, shape = RexMd, padding = 12.dp, spacing = 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexIconTile(Icons.Default.Image, size = 42.dp)
            Text(
                label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.OpenInNew, contentDescription = "Buka", tint = s.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun VideoTile(label: String, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_VideoTile(label, index, onClick, modifier)
    }
