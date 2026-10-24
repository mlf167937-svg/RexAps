// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RexHero(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RexIconTile(icon, size = 54.dp, pulse = true)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RexErrorBanner(title: String, message: String) {
    val s = MaterialTheme.colorScheme
    val shake = remember { Animatable(0f) }
    LaunchedEffect(message) {
        for (v in listOf(-12f, 10f, -7f, 4f, 0f)) shake.animateTo(v, tween(55))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shake.value }
            .clip(RexMd)
            .background(s.errorContainer)
            .border(1.dp, s.error.copy(alpha = 0.35f), RexMd)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = s.onErrorContainer)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = s.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = s.onErrorContainer)
        }
    }
}
