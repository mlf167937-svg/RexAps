//EditorArea.kt
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.editor.RexCodeSyntaxTransformation
import com.rexaps.rexcoder.model.DocState
import com.rexaps.rexcoder.model.EditorGroup
import com.rexaps.rexcoder.model.MAX_EDITOR_FONT
import com.rexaps.rexcoder.model.MIN_EDITOR_FONT
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex
import com.rexaps.rexcoder.theme.codeStyle

// ───────────── Shared small widgets ─────────────

@Composable fun HDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Rex.Border))
@Composable fun VDivider() = Box(Modifier.fillMaxHeight().width(1.dp).background(Rex.Border))

fun fileStyle(name: String): Pair<ImageVector, Color> = when (name.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> Icons.Outlined.Code to Color(0xFFB380FF)
    "html", "htm" -> Icons.Outlined.Html to Color(0xFFE37933)
    "css", "scss", "sass", "less" -> Icons.Outlined.Css to Color(0xFF519ABA)
    "json" -> Icons.Outlined.DataObject to Color(0xFFCBCB41)
    "js", "mjs", "cjs", "ts", "tsx", "jsx" -> Icons.Outlined.Code to Color(0xFFF7DF1E)
    "py" -> Icons.Outlined.Code to Color(0xFFFFD54F)
    "java" -> Icons.Outlined.Code to Color(0xFFE76F51)
    "md" -> Icons.Outlined.Description to Color(0xFF42A5F5)
    else -> Icons.Outlined.Description to Rex.TextDim
}

@Composable
fun SmallIconButton(
    icon: ImageVector, desc: String, size: Dp = 32.dp, tint: Color = Rex.TextDim, onClick: () -> Unit
) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp)) }
}

// ───────────── Font size menu (dipakai editor, tree, terminal) ─────────────

/** Isi menu: judul, stepper − / + dan deretan preset. */
@Composable
private fun FontStepperContent(
    label: String, value: Int, min: Int, max: Int, presets: List<Int>, onChange: (Int) -> Unit
) {
    Column {
        Text(
            "FONT $label  ($min–$max)", fontSize = 10.sp, letterSpacing = 1.2.sp, color = Rex.TextDim,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
        )
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallIconButton(Icons.Outlined.Remove, "Kecilkan font $label", 40.dp, Rex.Text) {
                if (value > min) onChange(value - 1)
            }
            Text(
                "${value}px", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = Rex.TextBright,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 64.dp)
            )
            SmallIconButton(Icons.Outlined.Add, "Besarkan font $label", 40.dp, Rex.Text) {
                if (value < max) onChange(value + 1)
            }
        }
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { p ->
                val sel = p == value
                Text(
                    "$p", fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    color = if (sel) Rex.OnAccent else Rex.Text,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (sel) Rex.Accent else Rex.Field)
                        .clickable(role = Role.Button, onClickLabel = "Set $p px") { onChange(p) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
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

// ───────────── Resize handle (drag to resize, double-tap to reset) ─────────────

/** Thickness of the touch area of every draggable divider. */
val HANDLE_THICKNESS: Dp = 12.dp

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

/** Horizontal = groups berdampingan (landscape/tablet), else bertumpuk (portrait). Dividers are draggable. */
@Composable
fun EditorArea(s: RexCoderState, horizontal: Boolean, modifier: Modifier = Modifier) {
    val groups = s.groups.toList()
    var areaPx by remember { mutableFloatStateOf(0f) }
    val handlePx = with(LocalDensity.current) { HANDLE_THICKNESS.toPx() }
    val availablePx = (areaPx - handlePx * (groups.size - 1)).coerceAtLeast(1f)
    val sized = modifier.onSizeChanged { areaPx = (if (horizontal) it.width else it.height).toFloat() }

    if (horizontal) Row(sized) {
        groups.forEachIndexed { i, g ->
            key(g.id) {
                if (i > 0) ResizeHandle(
                    vertical = true, desc = "Ubah lebar editor ${i} dan ${i + 1}",
                    onDelta = { s.resizeGroups(i - 1, it, availablePx) }, onReset = { s.equalizeGroups() }
                )
                EditorGroupView(s, g, i, Modifier.weight(g.weight.coerceAtLeast(0.01f)).fillMaxHeight())
            }
        }
    } else Column(sized) {
        groups.forEachIndexed { i, g ->
            key(g.id) {
                if (i > 0) ResizeHandle(
                    vertical = false, desc = "Ubah tinggi editor ${i} dan ${i + 1}",
                    onDelta = { s.resizeGroups(i - 1, it, availablePx) }, onReset = { s.equalizeGroups() }
                )
                EditorGroupView(s, g, i, Modifier.weight(g.weight.coerceAtLeast(0.01f)).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EditorGroupView(s: RexCoderState, g: EditorGroup, index: Int, modifier: Modifier) {
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
private fun TabBar(s: RexCoderState, g: EditorGroup, index: Int, focused: Boolean) {
    val listState = rememberLazyListState()
    LaunchedEffect(g.active) {
        val i = g.tabs.indexOf(g.active)
        if (i >= 0) listState.animateScrollToItem(i)
    }
    Row(Modifier.fillMaxWidth().height(36.dp).background(Rex.TabBar), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(Modifier.weight(1f), state = listState) {
            items(g.tabs.toList(), key = { it }) { path ->
                s.docs[path]?.let { Tab(s, g, it, active = path == g.active, groupFocused = focused) }
            }
        }
        FontSizeChip(label = "E${index + 1}", group = g, focused = focused) { s.setFontSize(g, it) }
        if (s.groups.size < 3) SmallIconButton(Icons.Outlined.ViewColumn, "Split editor") {
            s.focusedGroupId = g.id; s.splitEditor()
        }
        if (s.groups.size > 1) SmallIconButton(Icons.Outlined.Close, "Close editor group") { s.removeGroup(g.id) }
    }
}

/** "E2 12px" chip. Tap → stepper + presets for this editor's own font size. */
@Composable
private fun FontSizeChip(label: String, group: EditorGroup, focused: Boolean, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Box {
        Row(
            Modifier
                .padding(horizontal = 2.dp)
                .height(26.dp)
                .clip(shape)
                .border(1.dp, if (focused) Rex.Accent.copy(alpha = 0.7f) else Rex.Border, shape)
                .clickable(role = Role.Button, onClickLabel = "Ubah ukuran font $label") { open = true }
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Rex.Accent)
            Spacer(Modifier.width(5.dp))
            Text("${group.fontSize}px", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Rex.Text, maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Rex.Overlay)) {
            FontStepperContent(
                label, group.fontSize, MIN_EDITOR_FONT, MAX_EDITOR_FONT,
                listOf(2, 4, 6, 8, 10, 12, 13, 14, 16, 18, 20, 22), onChange
            )
        }
    }
}

@Composable
private fun Tab(s: RexCoderState, g: EditorGroup, doc: DocState, active: Boolean, groupFocused: Boolean) {
    val (icon, tint) = fileStyle(doc.name)
    Row(
        Modifier
            .fillMaxHeight()
            .background(if (active) Rex.EditorBg else Rex.TabInactive)
            .drawBehind {
                if (active) {
                    if (groupFocused) drawRect(Rex.accentBrush(), size = Size(size.width, 2.dp.toPx()))
                    else drawRect(Rex.TextDim.copy(alpha = 0.5f), size = Size(size.width, 2.dp.toPx()))
                }
                drawRect(Rex.Border, Offset(size.width - 1.dp.toPx(), 0f), Size(1.dp.toPx(), size.height))
            }
            .clickable { g.active = doc.path; s.focusedGroupId = g.id }
            .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            doc.name, fontSize = 13.sp, maxLines = 1,
            color = if (active) Rex.TextBright else Rex.TextDim,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
        Box(
            Modifier.size(32.dp).clip(CircleShape).clickable { s.closeTab(g, doc.path) },
            contentAlignment = Alignment.Center
        ) {
            if (doc.modified) Box(Modifier.size(8.dp).clip(CircleShape).background(Rex.Modified))
            else Icon(Icons.Outlined.Close, "Close ${doc.name}", tint = Rex.TextDim, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun Breadcrumbs(path: String) {
    val parts = path.substringAfter("/Download/RexAps/", path).split('/').filter { it.isNotEmpty() }
    Row(
        Modifier.fillMaxWidth().height(24.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        parts.forEachIndexed { i, p ->
            if (i > 0) Icon(Icons.Outlined.ChevronRight, null, tint = Rex.TextDim, modifier = Modifier.size(14.dp))
            Text(p, fontSize = 12.sp, maxLines = 1, color = if (i == parts.lastIndex) Rex.Text else Rex.TextDim)
        }
    }
}

@Composable
private fun Welcome(modifier: Modifier) {
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

@Composable
fun CodeEditor(
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
