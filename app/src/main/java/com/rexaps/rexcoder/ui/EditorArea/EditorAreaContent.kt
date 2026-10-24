package com.rexaps.rexcoder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.EditorGroup
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

// ───────────── Shared small widgets ─────────────

@Composable fun HDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Rex.Border))

@Composable fun VDivider() = Box(Modifier.fillMaxHeight().width(1.dp).background(Rex.Border))

@Composable
fun SmallIconButton(
    icon: ImageVector, desc: String, size: Dp = 32.dp, tint: Color = Rex.TextDim, onClick: () -> Unit
) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp)) }
}

/** Tombol ikon "Aa" yang membuka menu ukuran font. */
@Composable
fun FontMenuButton(
    title: String, value: Int, min: Int, max: Int, presets: List<Int>,
    size: Dp = 32.dp, tint: Color = Rex.TextDim, onChange: (Int) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SmallIconButton(Icons.Outlined.TextFields, "Ukuran font $title", size, tint) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Rex.Overlay)) {
            FontStepperContent(title, value, min, max, presets, onChange)
        }
    }
}

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

@Composable
internal fun EditorGroupView(s: RexCoderState, g: EditorGroup, index: Int, modifier: Modifier) {
    val focused = s.focusedGroupId == g.id
    Column(
        modifier
            .clipToBounds()
            .background(Rex.EditorBg)
            .pointerInput(g.id) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type == PointerEventType.Press) s.focusedGroupId = g.id
                    }
                }
            }
    ) {
        TabBar(s, g, index, focused)
        val doc = g.active?.let { s.docs[it] }
        if (doc == null) Welcome(Modifier.weight(1f).fillMaxWidth())
        else {
            Breadcrumbs(doc.path)
            key(doc.path) {
                CodeEditor(
                    doc, focused, g.fontSize, Modifier.weight(1f).fillMaxWidth(),
                    onFontChange = { s.setFontSize(g, it) }
                )
            }
        }
    }
}

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
