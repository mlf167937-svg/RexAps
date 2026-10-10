//Sidebar.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.FileNode
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.model.SideView
import com.rexaps.rexcoder.theme.Rex

// ───────────── Activity bar (landscape / tablet) ─────────────

@Composable
fun VerticalActivityBar(s: RexCoderState, roomy: Boolean) {
    val item = if (roomy) 48.dp else 40.dp
    val low = if (roomy) 44.dp else 36.dp
    Column(
        Modifier.fillMaxHeight().width(item).background(Rex.ActivityBar),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SideView.entries.forEach { v ->
            val sel = s.view == v && s.sidebarVisible
            Box(
                Modifier.size(item)
                    .drawBehind {
                        if (sel) drawRect(
                            Brush.verticalGradient(listOf(Rex.Accent, Rex.Accent2)),
                            size = Size(2.dp.toPx(), size.height)
                        )
                    }
                    .clickable {
                        if (s.view == v) s.sidebarVisible = !s.sidebarVisible else { s.view = v; s.sidebarVisible = true }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(v.icon, v.title, tint = if (sel) Rex.Accent else Rex.TextDim, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        LayoutMenu(s, size = low)
        SmallIconButton(Icons.Outlined.Terminal, "Toggle terminal", low) { s.panelVisible = !s.panelVisible }
        SmallIconButton(Icons.Outlined.Settings, "Command palette", low) { s.paletteVisible = true }
    }
}

// ───────────── Sidebar ─────────────

@Composable
fun Sidebar(
    s: RexCoderState, modifier: Modifier = Modifier, rowHeight: Dp = 30.dp, onFileOpened: () -> Unit = {}
) {
    Column(modifier.background(Rex.SideBar)) {
        Text(
            s.view.title.uppercase(), fontSize = 11.sp, letterSpacing = 1.4.sp, color = Rex.TextDim,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
        )
        when (s.view) {
            SideView.Explorer -> ExplorerView(s, rowHeight, onFileOpened)
            SideView.Search -> SearchView(s, onFileOpened)
            SideView.Git -> GitView(s)
            SideView.Extensions -> InfoList(
                listOf(
                    Triple("Kotlin", "JetBrains", "Install"),
                    Triple("Material Icon Theme", "Philipp Kief", "Install"),
                    Triple("Prettier", "Prettier", "Install"),
                    Triple("GitLens", "GitKraken", "Install")
                )
            ) { }
        }
    }
}

private fun visibleRows(
    n: FileNode, depth: Int, expanded: List<String>, out: MutableList<Pair<FileNode, Int>> = mutableListOf()
): List<Pair<FileNode, Int>> {
    out += n to depth
    if (n.isDir && n.path in expanded)
        n.children.sortedWith(compareBy({ !it.isDir }, { it.name })).forEach { visibleRows(it, depth + 1, expanded, out) }
    return out
}

@Composable
private fun ExplorerView(s: RexCoderState, rowHeight: Dp, onOpened: () -> Unit) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    val rows = visibleRows(s.root, 0, s.expanded)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("WORKSPACE", fontSize = 10.sp, letterSpacing = 1.sp, color = Rex.TextDim, modifier = Modifier.weight(1f))
            SmallIconButton(Icons.Outlined.NoteAdd, "New file", 32.dp) { name = ""; dialog = "file" }
            SmallIconButton(Icons.Outlined.CreateNewFolder, "New folder", 32.dp) { name = ""; dialog = "folder" }
            SmallIconButton(Icons.Outlined.Refresh, "Refresh", 32.dp) { s.refreshWorkspace() }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(rows, key = { it.first.path }) { (node, depth) -> TreeRow(s, node, depth, rowHeight, onOpened) }
        }
    }
    dialog?.let { kind ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = Rex.Overlay,
            titleContentColor = Rex.TextBright,
            textContentColor = Rex.Text,
            title = { Text(if (kind == "file") "Create file" else "Create folder") },
            text = {
                BasicTextField(
                    name, { name = it }, singleLine = true,
                    textStyle = TextStyle(color = Rex.Text, fontSize = 14.sp),
                    cursorBrush = SolidColor(Rex.Accent),
                    modifier = Modifier.fillMaxWidth()
                        .background(Rex.Field, RoundedCornerShape(8.dp))
                        .border(1.dp, Rex.Border, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    if (name.isNotBlank()) { if (kind == "file") s.createFile(name.trim()) else s.createFolder(name.trim()); dialog = null }
                }) { Text("Create", color = Rex.Accent) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { dialog = null }) { Text("Cancel", color = Rex.TextDim) } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRow(s: RexCoderState, node: FileNode, depth: Int, rowH: Dp, onOpened: () -> Unit) {
    val open = node.path in s.expanded
    val active = !node.isDir && s.focusedGroup.active == node.path
    val (icon, tint) = if (node.isDir)
        (if (open) Icons.Outlined.FolderOpen else Icons.Outlined.Folder) to Color(0xFFC09553)
    else fileStyle(node.name)
    Row(
        Modifier.fillMaxWidth().height(rowH)
            .background(if (active) Rex.Selection else Color.Transparent)
            .drawBehind { if (active) drawRect(Rex.Accent, size = Size(2.dp.toPx(), size.height)) }
            .combinedClickable(
                onLongClick = { if (!node.isDir) { s.openToSide(node); onOpened() } }
            ) { if (node.isDir) s.toggle(node.path) else { s.openFile(node); onOpened() } }
            .padding(start = (8 + depth * 12).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDir) Icon(
            if (open) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight, null,
            tint = Rex.TextDim, modifier = Modifier.size(16.dp)
        ) else Spacer(Modifier.width(16.dp))
        Spacer(Modifier.width(4.dp))
        if (node.isDir) Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        else RexLanguageIcon(node.name, Modifier.width(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            node.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (active) Rex.TextBright else Rex.Text,
            fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SearchView(s: RexCoderState, onOpened: () -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    val hits = remember(q) { s.search(q) }
    Box(
        Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)).background(Rex.Field)
            .border(1.dp, Rex.Border, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        if (q.isEmpty()) Text("Search", fontSize = 13.sp, color = Rex.TextDim)
        BasicTextField(
            q, { q = it }, singleLine = true,
            textStyle = TextStyle(color = Rex.Text, fontSize = 13.sp),
            cursorBrush = SolidColor(Rex.Accent), modifier = Modifier.fillMaxWidth()
        )
    }
    if (q.length >= 2) Text(
        "${hits.size} results", fontSize = 11.sp, color = Rex.TextDim,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    LazyColumn(Modifier.fillMaxSize()) {
        items(hits) { h ->
            Column(
                Modifier.fillMaxWidth().clickable { s.openAt(h.path, h.line); onOpened() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("${h.name}:${h.line}", fontSize = 12.sp, color = Rex.SynVariable)
                Text(h.text, fontSize = 12.sp, color = Rex.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GitView(s: RexCoderState) {
    val changed = s.modifiedDocs()
    if (changed.isEmpty()) {
        Text("No changes detected.", fontSize = 13.sp, color = Rex.TextDim, modifier = Modifier.padding(16.dp))
        return
    }
    Text(
        "CHANGES  ${changed.size}", fontSize = 11.sp, color = Rex.TextDim,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    changed.forEach { d ->
        val (icon, tint) = fileStyle(d.name)
        Row(
            Modifier.fillMaxWidth().height(32.dp)
                .clickable { s.openAt(d.path, 1) }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(d.name, fontSize = 13.sp, color = Rex.Text, modifier = Modifier.weight(1f), maxLines = 1)
            Text("M", fontSize = 12.sp, color = Rex.Modified, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(Rex.accentBrush()).clickable { s.saveAll() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text("✓  Commit All", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Rex.OnAccent) }
}

@Composable
private fun InfoList(items: List<Triple<String, String, String>>, onAction: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(items) { (title, sub, action) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 13.sp, color = Rex.Text, fontWeight = FontWeight.Medium)
                    Text(sub, fontSize = 12.sp, color = Rex.TextDim)
                }
                Text(
                    action, fontSize = 12.sp, color = Rex.OnAccent, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Rex.Accent)
                        .clickable(onClick = onAction).padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
