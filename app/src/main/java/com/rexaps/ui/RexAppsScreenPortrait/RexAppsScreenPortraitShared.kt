package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

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
