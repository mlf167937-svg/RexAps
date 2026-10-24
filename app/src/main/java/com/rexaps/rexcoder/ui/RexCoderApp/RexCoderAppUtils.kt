//RexCoderApp.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.rexaps.rexcoder.model.RexCoderState

/** Horizontal handle above the bottom panel. Dragging up makes the panel taller. */
@Composable
internal fun PanelResizeHandle(s: RexCoderState, currentDp: Float, heightDp: Float, density: Float) {
    ResizeHandle(
        vertical = false, desc = "Ubah tinggi panel terminal",
        onDelta = { d -> s.panelHeight = (currentDp - d / density).coerceIn(90f, (heightDp * 0.6f).coerceAtLeast(90f)) },
        onReset = { s.panelHeight = 190f }
    )
}
