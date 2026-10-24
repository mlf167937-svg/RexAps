//EditorArea.kt
package com.rexaps.rexcoder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rexaps.rexcoder.theme.Rex

/**
 * Draggable divider.
 * [vertical] = true  → a vertical bar between side-by-side panes (drag left/right).
 * [vertical] = false → a horizontal bar between stacked panes (drag up/down).
 * [onDelta] receives the finger movement in px along the drag axis.
 * Screen-reader users get "grow / shrink" custom actions instead of a drag gesture.
 */
@Composable
fun ResizeHandle(
    vertical: Boolean,
    desc: String,
    onDelta: (Float) -> Unit,
    onReset: (() -> Unit)? = null
) {
    val currentDelta by rememberUpdatedState(onDelta)
    val currentReset by rememberUpdatedState(onReset)
    var dragging by remember { mutableStateOf(false) }
    val lineColor by animateColorAsState(if (dragging) Rex.Accent else Rex.Border, tween(120), label = "resizeLine")
    val gripColor by animateColorAsState(if (dragging) Rex.Accent else Rex.TextDim.copy(alpha = 0.45f), tween(120), label = "resizeGrip")

    val area = if (vertical) Modifier.fillMaxHeight().width(HANDLE_THICKNESS) else Modifier.fillMaxWidth().height(HANDLE_THICKNESS)
    Box(
        area
            .semantics {
                contentDescription = desc
                customActions = listOf(
                    CustomAccessibilityAction("Perbesar") { currentDelta(48f); true },
                    CustomAccessibilityAction("Perkecil") { currentDelta(-48f); true }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { currentReset?.invoke() }) }
            .pointerInput(vertical) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, drag ->
                    change.consume()
                    currentDelta(if (vertical) drag.x else drag.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val thin = if (dragging) 2.dp else 1.dp
        Box((if (vertical) Modifier.fillMaxHeight().width(thin) else Modifier.fillMaxWidth().height(thin)).background(lineColor))
        Box(
            (if (vertical) Modifier.size(3.dp, 28.dp) else Modifier.size(28.dp, 3.dp))
                .clip(CircleShape).background(gripColor)
        )
    }
}

// ───────────── Editor groups (multi-editor, resizable) ─────────────
