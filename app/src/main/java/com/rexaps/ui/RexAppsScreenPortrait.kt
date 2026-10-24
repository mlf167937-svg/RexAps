// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ═════════════════════ Shared premium kit (dipakai portrait & landscape) ═════════════════════ */

internal val proGradients = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFFEC4899)),
    listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
    listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)),
    listOf(Color(0xFFF43F5E), Color(0xFFF97316))
)

internal fun proGradientFor(key: String): List<Color> =
    proGradients[key.hashCode().mod(proGradients.size)]

internal fun proFilterApps(apps: List<RexModule>, query: String, filter: String): List<RexModule> {
    val q = query.trim()
    return apps.filter { app ->
        val matchQuery = app.name.contains(q, ignoreCase = true) ||
            app.description.contains(q, ignoreCase = true)
        val matchFilter = when (filter) {
            "Tersedia" -> app.available
            "Segera" -> !app.available
            else -> true
        }
        matchQuery && matchFilter
    }
}

/** Efek tekan membal untuk elemen yang bisa diketuk. */
internal fun Modifier.proPress(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "proPress"
    )
    this.graphicsLayer { scaleX = s; scaleY = s }
        .clickable(
            interactionSource = src,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

/** Permukaan kaca: gradient halus dengan border tipis berwarna tema. */
@Composable
internal fun ProGlass(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(c.surfaceVariant.copy(alpha = 0.82f), c.surface.copy(alpha = 0.6f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(c.primary.copy(alpha = 0.30f), c.outlineVariant.copy(alpha = 0.20f))
                ),
                shape
            ),
        content = content
    )
}

/** Latar gradient radial yang bergerak pelan. */
@Composable
internal fun ProAmbientBackdrop(modifier: Modifier = Modifier) {
        return __splitImpl_ProAmbientBackdrop(modifier)
    }

@Composable
internal fun ProIcon(name: String, colors: List<Color>, dim: Dp) {
    val shape = RoundedCornerShape(dim * 0.34f)
    Box(
        Modifier
            .size(dim)
            .shadow(elevation = 12.dp, shape = shape, spotColor = colors.first().copy(alpha = 0.5f))
            .clip(shape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
internal fun ProStatusDot(available: Boolean) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (available) c.primaryContainer.copy(alpha = 0.7f) else c.outlineVariant.copy(alpha = 0.4f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (available) c.primary else c.onSurfaceVariant, CircleShape)
        )
        Text(
            text = if (available) "Tersedia" else "Segera",
            style = MaterialTheme.typography.labelSmall,
            color = if (available) c.onPrimaryContainer else c.onSurfaceVariant
        )
    }
}

@Composable
internal fun ProHomeHeader(
    appCount: Int,
    availableCount: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_ProHomeHeader(appCount, availableCount, onOpenSettings, modifier)
    }

@Composable
internal fun ProFeaturedCard(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_ProFeaturedCard(app, onClick, modifier)
    }

@Composable
internal fun ProAppTile(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_ProAppTile(app, onClick, modifier)
    }

@Composable
internal fun ProSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_ProSearchBar(query, onQuery, modifier)
    }

@Composable
internal fun ProFilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Semua", "Tersedia", "Segera").forEach { f ->
            FilterChip(
                selected = selected == f,
                onClick = { onSelect(f) },
                label = { Text(f) }
            )
        }
    }
}

@Composable
internal fun ProSectionHeader(title: String, subtitle: String, count: Int) {
        return __splitImpl_ProSectionHeader(title, subtitle, count)
    }

@Composable
internal fun ProEmptyState() {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text("Aplikasi tidak ditemukan", style = MaterialTheme.typography.titleMedium)
        Text(
            "Coba kata kunci lain atau ubah filter.",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/* ═════════════════════ Shell portrait ═════════════════════ */

@Composable
internal fun RexAppsScreenPortrait(shell: RexShell) {
        return __splitImpl_RexAppsScreenPortrait(shell)
    }

/* ═════════════════════ Navigasi bawah (pill kaca) ═════════════════════ */

@Composable
internal fun ProPortraitNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_ProPortraitNav(selected, onSelect, modifier)
    }

@Composable
internal fun ProNavItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
        return __splitImpl_ProNavItem(tab, selected, onClick)
    }

/* ═════════════════════ Home portrait ═════════════════════ */

@Composable
internal fun ProPortraitHome(shell: RexShell) {
        return __splitImpl_ProPortraitHome(shell)
    }

/* ═════════════════════ Profile portrait ═════════════════════ */

@Composable
internal fun ProPortraitProfile(appCount: Int, themeLabel: String, isActive: Boolean) {
        return __splitImpl_ProPortraitProfile(appCount, themeLabel, isActive)
    }