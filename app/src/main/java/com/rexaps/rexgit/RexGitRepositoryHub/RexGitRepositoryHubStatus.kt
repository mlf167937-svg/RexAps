@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rexaps.rexgit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun StatusDot(status: String) {
    val normalized = status.lowercase()
    val color = when {
        "success" in normalized || "completed" in normalized || "active" in normalized -> Color(0xFF22A06B)
        "failure" in normalized || "failed" in normalized || "cancel" in normalized -> MaterialTheme.colorScheme.error
        "queued" in normalized || "progress" in normalized || "waiting" in normalized -> Color(0xFFE0A020)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.14f), shape = CircleShape, modifier = Modifier.size(28.dp)) {
        Box(contentAlignment = Alignment.Center) { Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {} }
    }
}
