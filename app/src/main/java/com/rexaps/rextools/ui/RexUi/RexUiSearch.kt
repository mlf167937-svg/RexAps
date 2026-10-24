// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RexFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val container by animateColorAsState(
        if (selected) s.primary else s.surfaceContainerHigh.copy(alpha = 0.6f), tween(260), label = "bg"
    )
    val content by animateColorAsState(if (selected) s.onPrimary else s.onSurfaceVariant, tween(260), label = "fg")
    val borderColor by animateColorAsState(if (selected) Color.Transparent else s.outlineVariant, tween(260), label = "bd")
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.pressScale(source, 0.93f),
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = source
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}
