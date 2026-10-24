//EditorArea.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.DocState
import com.rexaps.rexcoder.model.EditorGroup
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

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
internal fun FontStepperContent(
    label: String, value: Int, min: Int, max: Int, presets: List<Int>, onChange: (Int) -> Unit
) {
        return __splitImpl_FontStepperContent(label, value, min, max, presets, onChange)
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
        return __splitImpl_ResizeHandle(vertical, desc, onDelta, onReset)
    }

// ───────────── Editor groups (multi-editor, resizable) ─────────────

/** Horizontal = groups berdampingan (landscape/tablet), else bertumpuk (portrait). Dividers are draggable. */
@Composable
fun EditorArea(s: RexCoderState, horizontal: Boolean, modifier: Modifier = Modifier) {
        return __splitImpl_EditorArea(s, horizontal, modifier)
    }

@Composable
internal fun EditorGroupView(s: RexCoderState, g: EditorGroup, index: Int, modifier: Modifier) {
        return __splitImpl_EditorGroupView(s, g, index, modifier)
    }

@Composable
internal fun TabBar(s: RexCoderState, g: EditorGroup, index: Int, focused: Boolean) {
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
internal fun FontSizeChip(label: String, group: EditorGroup, focused: Boolean, onChange: (Int) -> Unit) {
        return __splitImpl_FontSizeChip(label, group, focused, onChange)
    }

@Composable
internal fun Tab(s: RexCoderState, g: EditorGroup, doc: DocState, active: Boolean, groupFocused: Boolean) {
        return __splitImpl_Tab(s, g, doc, active, groupFocused)
    }

@Composable
internal fun Breadcrumbs(path: String) {
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
internal fun Welcome(modifier: Modifier) {
        return __splitImpl_Welcome(modifier)
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
        return __splitImpl_CodeEditor(doc, focused, fontSize, modifier, onFontChange)
    }
