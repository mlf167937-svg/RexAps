// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

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
