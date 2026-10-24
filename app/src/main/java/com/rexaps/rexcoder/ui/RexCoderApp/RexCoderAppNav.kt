//RexCoderApp.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

@Composable
fun LayoutMenu(s: RexCoderState, tint: Color = Rex.TextDim, size: Dp = 32.dp) {
    var open by remember { mutableStateOf(false) }
    Box {
        SmallIconButton(Icons.Outlined.ViewColumn, "Editor layout", size, tint) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Rex.Overlay)) {
            (1..3).forEach { n ->
                DropdownMenuItem(
                    text = { Text(if (n == 1) "Single editor" else "$n editors", color = Rex.Text) },
                    leadingIcon = { if (s.groups.size == n) Icon(Icons.Outlined.Check, null, tint = Rex.Accent) },
                    onClick = { s.setLayout(n); open = false }
                )
            }
            if (s.groups.size > 1) DropdownMenuItem(
                text = { Text("Samakan ukuran editor", color = Rex.Text) },
                leadingIcon = { Icon(Icons.Outlined.ViewColumn, null, tint = Rex.TextDim) },
                onClick = { s.equalizeGroups(); open = false }
            )
        }
    }
}

// ───────────── Command palette ─────────────
