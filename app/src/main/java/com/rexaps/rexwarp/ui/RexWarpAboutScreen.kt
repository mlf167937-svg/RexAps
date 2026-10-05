package com.rexaps.rexwarp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RexWarpAboutScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Block("Privacy", listOf(
            "Traffic statistics are stored locally on the device.",
            "RexWARP does not need a RexAps account for local usage statistics.",
            "RexWARP does not collect analytics. No passwords or secrets are stored in plain text.",
            "RexWARP does not inspect, intercept or modify the content of your traffic."
        ))
        Block("About", listOf(
            "RexWARP is an independent RexAps integration.",
            "Cloudflare and WARP are trademarks of Cloudflare, Inc.",
            "RexAps is not affiliated with, endorsed by, or sponsored by Cloudflare."
        ))
    }
}

@Composable
private fun Block(title: String, lines: List<String>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}