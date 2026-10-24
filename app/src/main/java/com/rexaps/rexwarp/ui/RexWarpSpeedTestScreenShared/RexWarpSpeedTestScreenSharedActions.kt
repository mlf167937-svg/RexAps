package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpSpeedTestUiState

@Composable
internal fun PrimaryTestAction(state: RexWarpSpeedTestUiState, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    Button(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        enabled = state.engineReady || state.running || state.error != null,
        modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (state.running) c.error else c.primary, disabledContainerColor = c.surfaceVariant, disabledContentColor = c.onSurfaceVariant),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        AnimatedContent(actionLabel(state), label = "primary-action") { label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(when { state.running -> Icons.Filled.Stop; state.completed || (state.error != null && !state.engineReady) -> Icons.Filled.Refresh; else -> Icons.Filled.Speed }, null)
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
    if (!state.engineReady && state.error == null) Text("Menghubungkan ke mesin pengukuran…", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
}
