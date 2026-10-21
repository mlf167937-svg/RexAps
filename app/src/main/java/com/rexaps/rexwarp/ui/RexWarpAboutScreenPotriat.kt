package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RexWarpAboutScreenPortrait(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Your connection, with transparency", style = MaterialTheme.typography.headlineSmall)
        Text(
            "A quick overview of how RexWARP handles local statistics and its relationship with other services.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AnimatedVisibility(true, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 14 }) {
            AboutBlockPortrait("Privacy", rexWarpPrivacyLines)
        }
        AnimatedVisibility(true, enter = fadeIn(tween(420, delayMillis = 60)) + slideInVertically(tween(420, delayMillis = 60)) { it / 14 }) {
            AboutBlockPortrait("About", rexWarpAboutLines)
        }
    }
}

@Composable
private fun AboutBlockPortrait(title: String, lines: List<String>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            lines.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
