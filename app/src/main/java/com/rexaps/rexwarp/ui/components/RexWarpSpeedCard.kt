package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpFormat
import com.rexaps.rexwarp.RexWarpSpeed

@Composable
fun RexWarpSpeedCard(speed: RexWarpSpeed, connected: Boolean, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Network speed", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SpeedTile("↓", "Download", if (connected) RexWarpFormat.speed(speed.downBytesPerSec) else "N/A", Modifier.weight(1f))
                SpeedTile("↑", "Upload", if (connected) RexWarpFormat.speed(speed.upBytesPerSec) else "N/A", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpeedTile(arrow: String, name: String, value: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$name speed $value" }) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$arrow $value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}
