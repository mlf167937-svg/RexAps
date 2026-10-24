package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.EditorGroup
import com.rexaps.rexcoder.model.MAX_EDITOR_FONT
import com.rexaps.rexcoder.model.MIN_EDITOR_FONT
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

/** Isi menu: judul, stepper − / + dan deretan preset. */
@Composable
internal fun __splitImpl_FontStepperContent(
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

/** Horizontal = groups berdampingan (landscape/tablet), else bertumpuk (portrait). Dividers are draggable. */
@Composable
internal fun __splitImpl_EditorArea(s: RexCoderState, horizontal: Boolean, modifier: Modifier = Modifier) {
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

/** "E2 12px" chip. Tap → stepper + presets for this editor's own font size. */
@Composable
internal fun __splitImpl_FontSizeChip(label: String, group: EditorGroup, focused: Boolean, onChange: (Int) -> Unit) {
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
internal fun __splitImpl_EditorGroupView(s: RexCoderState, g: EditorGroup, index: Int, modifier: Modifier) {
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
internal fun __splitImpl_Welcome(modifier: Modifier) {
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
