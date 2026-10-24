package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.model.flatFiles
import com.rexaps.rexcoder.theme.Rex

@Composable
internal fun __splitImpl_CommandPalette(s: RexCoderState) {
    var q by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    val close = { s.paletteVisible = false }
    val cmds = remember {
        listOf(
            Cmd("Split Editor Right", "") { s.splitEditor() },
            Cmd("Layout: Single Editor", "") { s.setLayout(1) },
            Cmd("Layout: Two Editors", "") { s.setLayout(2) },
            Cmd("Layout: Three Editors", "") { s.setLayout(3) },
            Cmd("Equalize Editor Sizes", "") { s.equalizeGroups() },
            Cmd("Editor Font: Increase", "") { s.adjustFocusedFont(1) },
            Cmd("Editor Font: Decrease", "") { s.adjustFocusedFont(-1) },
            Cmd("Toggle Sidebar", "Ctrl+B") { s.sidebarVisible = !s.sidebarVisible },
            Cmd("Toggle Terminal Panel", "Ctrl+`") { s.panelVisible = !s.panelVisible },
            Cmd("Save File", "Ctrl+S") { s.save() },
            Cmd("Save All", "Ctrl+Alt+S") { s.saveAll() },
            Cmd("Close All Editors", "") { s.closeAll() }
        )
    }
    val files = remember { s.root.flatFiles() }
    val term = q.removePrefix(">").trim()
    val showFiles = !q.startsWith(">")
    val fHits = if (showFiles) files.filter { it.name.contains(term, true) } else emptyList()
    val cHits = cmds.filter { it.title.contains(term, true) }
    val shape = RoundedCornerShape(12.dp)

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp).heightIn(max = 420.dp)
                .clip(shape).background(Rex.Overlay)
                .border(1.dp, Rex.accentBrush(), shape)
        ) {
            BasicTextField(
                q, { q = it }, singleLine = true,
                textStyle = TextStyle(color = Rex.Text, fontSize = 15.sp),
                cursorBrush = SolidColor(Rex.Accent),
                modifier = Modifier.fillMaxWidth().focusRequester(fr).padding(14.dp),
                decorationBox = { inner ->
                    if (q.isEmpty()) Text("Go to file…  or type > for commands", fontSize = 15.sp, color = Rex.TextDim)
                    inner()
                }
            )
            HDivider()
            LazyColumn {
                items(fHits) { f ->
                    val (icon, tint) = fileStyle(f.name)
                    Row(
                        Modifier.fillMaxWidth().clickable { s.openFile(f); close() }.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(f.name, fontSize = 14.sp, color = Rex.Text)
                        Spacer(Modifier.width(8.dp))
                        Text(f.path.substringBeforeLast('/'), fontSize = 12.sp, color = Rex.TextDim, maxLines = 1)
                    }
                }
                items(cHits) { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { c.run(); close() }.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(c.title, fontSize = 14.sp, color = Rex.Text, modifier = Modifier.weight(1f))
                        Text(c.hint, fontSize = 12.sp, color = Rex.TextDim)
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { fr.requestFocus() }
}

@Composable
internal fun __splitImpl_TitleBar(s: RexCoderState) {
    Row(
        Modifier.fillMaxWidth().height(36.dp).background(Rex.TitleBar)
            .drawBehind { drawRect(Rex.accentBrush(), Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()), alpha = 0.6f) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Code, null, tint = Rex.Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("REX", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Rex.Accent)
        Text("CODER", fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp, color = Rex.TextBright)
        Spacer(Modifier.width(10.dp))
        listOf("File", "Edit", "View", "Terminal").forEach { m ->
            Text(
                m, fontSize = 12.sp, color = Rex.Text,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                    when (m) {
                        "View" -> s.sidebarVisible = !s.sidebarVisible
                        "Terminal" -> s.panelVisible = !s.panelVisible
                        "File" -> s.save()
                        else -> s.paletteVisible = true
                    }
                }.padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.weight(1f).widthIn(max = 320.dp).height(26.dp).clip(RoundedCornerShape(8.dp))
                .background(Rex.Field).border(1.dp, Rex.Border, RoundedCornerShape(8.dp))
                .clickable { s.paletteVisible = true }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = Rex.TextDim, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("RexCoder Project", fontSize = 12.sp, color = Rex.TextDim, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        SmallIconButton(Icons.Outlined.Save, "Save", 32.dp) { s.save() }
    }
}
