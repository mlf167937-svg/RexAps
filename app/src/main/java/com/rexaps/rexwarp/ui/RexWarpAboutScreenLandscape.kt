package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RexWarpAboutScreenLandscape(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("About RexWARP", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enter = fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 16 }
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    AboutBlockLandscape("Privacy", rexWarpPrivacyLines)
                }
            }
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enter = fadeIn(tween(430, delayMillis = 70)) + slideInHorizontally(tween(430, delayMillis = 70)) { it / 18 }
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    AboutBlockLandscape("About & trademarks", rexWarpAboutLines)
                }
            }
        }
    }
}

@Composable
private fun AboutBlockLandscape(title: String, lines: List<String>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            lines.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
