//EditorArea.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.theme.Rex

@Composable
internal fun Welcome(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Code, null, tint = Rex.Accent.copy(alpha = 0.35f), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(6.dp))
        Text("REX//CODER", fontSize = 20.sp, fontWeight = FontWeight.Light, letterSpacing = 4.sp, color = Rex.TextDim)
        Spacer(Modifier.height(12.dp))
        listOf(
            "Open a file from the Explorer",
            "Long-press a file to open it to the side",
            "Drag a divider to resize · double-tap it to reset",
            "Pinch with two fingers in the editor to zoom the font",
            "Tap E1 / E2 / E3 to set that editor's font size (2–22px)"
        ).forEach { Text(it, fontSize = 12.sp, color = Rex.TextDim, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp)) }
    }
}

// ───────────── The code editor ─────────────
