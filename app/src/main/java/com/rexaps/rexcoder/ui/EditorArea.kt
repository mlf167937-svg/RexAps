package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.editor.RexCodeSyntaxTransformation
import com.rexaps.rexcoder.model.DocState
import com.rexaps.rexcoder.model.EditorGroup
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.CodeStyle
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

// ───────────── Editor groups (multi-editor) ─────────────

/** Horizontal = groups berdampingan (landscape/tablet), else bertumpuk (portrait). */
@Composable
fun EditorArea(s: RexCoderState, horizontal: Boolean, modifier: Modifier = Modifier) {
    val groups = s.groups.toList()
    if (horizontal) Row(modifier) {
        groups.forEachIndexed { i, g ->
            key(g.id) { if (i > 0) VDivider(); EditorGroupView(s, g, Modifier.weight(1f).fillMaxHeight()) }
        }
    } else Column(modifier) {
        groups.forEachIndexed { i, g ->
            key(g.id) { if (i > 0) HDivider(); EditorGroupView(s, g, Modifier.weight(1f).fillMaxWidth()) }
        }
    }
}

@Composable
private fun EditorGroupView(s: RexCoderState, g: EditorGroup, modifier: Modifier) {
    val focused = s.focusedGroupId == g.id
    Column(
        modifier
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
        TabBar(s, g, focused)
        val doc = g.active?.let { s.docs[it] }
        if (doc == null) Welcome(Modifier.weight(1f).fillMaxWidth())
        else {
            Breadcrumbs(doc.path)
            key(doc.path) { CodeEditor(doc, focused, Modifier.weight(1f).fillMaxWidth()) }
        }
    }
}

@Composable
private fun TabBar(s: RexCoderState, g: EditorGroup, focused: Boolean) {
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
        if (s.groups.size < 3) SmallIconButton(Icons.Outlined.ViewColumn, "Split editor") {
            s.focusedGroupId = g.id; s.splitEditor()
        }
        if (s.groups.size > 1) SmallIconButton(Icons.Outlined.Close, "Close editor group") { s.removeGroup(g.id) }
    }
}

@Composable
private fun Tab(s: RexCoderState, g: EditorGroup, doc: DocState, active: Boolean, groupFocused: Boolean) {
    val (icon, tint) = fileStyle(doc.name)
    val topColor = if (groupFocused) Rex.Accent else Color(0xFF6B6B6B)
    Row(
        Modifier
            .fillMaxHeight()
            .background(if (active) Rex.EditorBg else Rex.TabInactive)
            .drawBehind {
                if (active) drawRect(topColor, size = Size(size.width, 2.dp.toPx()))
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
            if (doc.modified) Box(Modifier.size(8.dp).clip(CircleShape).background(Rex.Text))
            else Icon(Icons.Outlined.Close, "Close ${doc.name}", tint = Rex.TextDim, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun Breadcrumbs(path: String) {
    val parts = path.split('/')
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
        Icon(Icons.Outlined.Code, null, tint = Color.White.copy(alpha = 0.08f), modifier = Modifier.size(64.dp))
        Text("RexCoder", fontSize = 22.sp, fontWeight = FontWeight.Light, color = Color.White.copy(alpha = 0.25f))
        Spacer(Modifier.height(10.dp))
        listOf("Open a file from the Explorer", "Long-press a file to open it to the side", "Tap the search icon for the Command Palette")
            .forEach { Text(it, fontSize = 12.sp, color = Rex.TextDim, textAlign = TextAlign.Center) }
    }
}

// ───────────── The code editor ─────────────

@Composable
fun CodeEditor(doc: DocState, focused: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val lh = with(density) { 20.sp.toPx() }
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

    BoxWithConstraints(modifier.background(Rex.EditorBg)) {
        val minH = maxHeight
        Row(
            Modifier
                .verticalScroll(vScroll)
                .fillMaxWidth()
                .heightIn(min = minH)
                .drawBehind { if (focused) drawRect(Rex.LineHighlight, Offset(0f, padTop + curLine * lh), Size(size.width, lh)) }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    focusRequester.requestFocus()
                }
        ) {
            Text(
                numbers,
                style = CodeStyle.copy(color = Rex.TextDim, textAlign = TextAlign.End),
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 12.dp).widthIn(min = 24.dp)
            )
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(Rex.Accent, Rex.SelectionEditor.copy(alpha = 0.8f))
            ) {
                Box(Modifier.weight(1f).horizontalScroll(hScroll).padding(top = 8.dp, bottom = 64.dp, end = 32.dp)) {
                    BasicTextField(
                        value = doc.value,
                        onValueChange = { doc.value = it },
                        textStyle = CodeStyle,
                        cursorBrush = SolidColor(Color.White),
                        visualTransformation = transformation,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.focusRequester(focusRequester).defaultMinSize(minWidth = 160.dp)
                    )
                }
            }
        }
    }
}
