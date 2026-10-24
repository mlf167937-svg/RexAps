package com.rexaps.rexcoder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexcoder.editor.RexCodeSyntaxTransformation
import com.rexaps.rexcoder.model.DocState
import com.rexaps.rexcoder.theme.Rex
import com.rexaps.rexcoder.theme.codeStyle

@Composable
internal fun __splitImpl_CodeEditor(
    doc: DocState,
    focused: Boolean,
    fontSize: Int,
    modifier: Modifier = Modifier,
    onFontChange: (Int) -> Unit = {}
) {
    val density = LocalDensity.current
    val style = codeStyle(fontSize)
    val lh = with(density) { style.lineHeight.toPx() }
    val padTop = with(density) { 8.dp.toPx() }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val transformation = remember(doc.path) { RexCodeSyntaxTransformation(doc.name) }

    val text = doc.value.text
    val cursor = doc.value.selection.start
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    val curLine = remember(text, cursor) { text.take(cursor).count { it == '\n' } }
    val numbers = remember(lineCount) { (1..lineCount).joinToString("\n") }

    // Pinch dua jari = zoom font (tiap ±12% jarak = ±1px). Satu jari tetap scroll / pilih teks seperti biasa.
    val currentFont by rememberUpdatedState(fontSize)
    val currentChange by rememberUpdatedState(onFontChange)
    val pinch = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var font = currentFont
            var lastDist = 0f
            var acc = 1f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.filter { it.pressed }
                if (down.size >= 2) {
                    val dist = (down[0].position - down[1].position).getDistance()
                    if (lastDist > 0f && dist > 0f) {
                        acc *= dist / lastDist
                        if (acc > 1.12f) { font += 1; currentChange(font); acc = 1f }
                        else if (acc < 0.89f) { font -= 1; currentChange(font); acc = 1f }
                    }
                    lastDist = dist
                    event.changes.forEach { it.consume() }
                } else {
                    lastDist = 0f
                    acc = 1f
                }
            } while (event.changes.any { it.pressed })
        }
    }

    BoxWithConstraints(modifier.background(Rex.EditorBg).then(pinch)) {
        val minH = maxHeight
        Row(
            Modifier
                .verticalScroll(vScroll)
                .fillMaxWidth()
                .heightIn(min = minH)
                .drawBehind {
                    if (focused) {
                        drawRect(Rex.LineHighlight, Offset(0f, padTop + curLine * lh), Size(size.width, lh))
                        drawRect(Rex.Accent.copy(alpha = 0.55f), Offset(0f, padTop + curLine * lh), Size(2.dp.toPx(), lh))
                    }
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    focusRequester.requestFocus()
                }
        ) {
            Text(
                numbers,
                style = style.copy(color = Rex.TextDim, textAlign = TextAlign.End),
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 12.dp).widthIn(min = 24.dp)
            )
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(Rex.Accent, Rex.SelectionEditor.copy(alpha = 0.8f))
            ) {
                Box(Modifier.weight(1f).horizontalScroll(hScroll).padding(top = 8.dp, bottom = 64.dp, end = 32.dp)) {
                    BasicTextField(
                        value = doc.value,
                        onValueChange = { doc.value = it },
                        textStyle = style,
                        cursorBrush = SolidColor(Rex.Accent),
                        visualTransformation = transformation,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.focusRequester(focusRequester).defaultMinSize(minWidth = 160.dp)
                    )
                }
            }
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
internal fun __splitImpl_ResizeHandle(
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
