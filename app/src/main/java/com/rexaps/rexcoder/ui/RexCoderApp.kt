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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.model.SideView
import com.rexaps.rexcoder.model.flatFiles
import com.rexaps.rexcoder.model.rememberRexCoderState
import com.rexaps.rexcoder.theme.Rex
import com.rexaps.rexcoder.theme.RexTheme
import kotlinx.coroutines.launch

/**
 * Entry point module RexCoder.
 *  - lebar >= 600dp (landscape / tablet): Activity bar + Sidebar + editor berdampingan
 *  - lebar <  600dp (portrait phone)    : Drawer + editor bertumpuk + bottom bar + extra keys
 */
@Composable
fun RexCoderApp(modifier: Modifier = Modifier, state: RexCoderState = rememberRexCoderState()) {
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    RexTheme(darkTheme = darkTheme) {
        BoxWithConstraints(modifier.fillMaxSize().background(Rex.EditorBg)) {
            if (maxWidth >= 600.dp) WideLayout(state, roomy = maxHeight >= 480.dp, darkTheme = darkTheme, onToggleTheme = { darkTheme = !darkTheme }) else CompactLayout(state, darkTheme = darkTheme, onToggleTheme = { darkTheme = !darkTheme })
            if (state.paletteVisible) CommandPalette(state)
        }
    }
}

// ───────────── Landscape / tablet ─────────────

@Composable
private fun WideLayout(s: RexCoderState, roomy: Boolean, darkTheme: Boolean, onToggleTheme: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (roomy) TitleBar(s, darkTheme, onToggleTheme)
        Row(Modifier.weight(1f)) {
            VerticalActivityBar(s, roomy)
            if (s.sidebarVisible) {
                Sidebar(s, Modifier.fillMaxHeight().width(if (roomy) 260.dp else 210.dp), rowHeight = if (roomy) 30.dp else 28.dp)
                VDivider()
            }
            Column(Modifier.weight(1f)) {
                EditorArea(s, horizontal = true, modifier = Modifier.weight(1f))
                if (s.panelVisible) { HDivider(); BottomPanel(s, Modifier.fillMaxWidth().height(if (roomy) 190.dp else 110.dp)) }
            }
        }
        StatusBar(s, compact = false)
    }
}

@Composable
private fun TitleBar(s: RexCoderState, darkTheme: Boolean, onToggleTheme: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(36.dp).background(Rex.TitleBar).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Code, null, tint = Rex.Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        listOf("File", "Edit", "View", "Run", "Terminal").forEach { m ->
            Text(
                m, fontSize = 12.sp, color = Rex.Text,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                    when (m) {
                        "View" -> s.sidebarVisible = !s.sidebarVisible
                        "Terminal", "Run" -> s.panelVisible = !s.panelVisible
                        "File" -> s.save()
                        else -> s.paletteVisible = true
                    }
                }.padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.weight(1f).widthIn(max = 320.dp).height(26.dp).clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2B2B2B)).border(1.dp, Rex.Border, RoundedCornerShape(6.dp))
                .clickable { s.paletteVisible = true }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = Rex.TextDim, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("RexCoder Project", fontSize = 12.sp, color = Rex.TextDim, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        SmallIconButton(Icons.Outlined.Save, "Save", 32.dp) { s.save() }
        SmallIconButton(if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Toggle theme", 32.dp) { onToggleTheme() }
        SmallIconButton(Icons.Outlined.PlayArrow, "Run", 32.dp, Rex.Success) { s.panelVisible = true }
    }
}

// ───────────── Portrait phone ─────────────

@Composable
private fun CompactLayout(s: RexCoderState, darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = drawer.isOpen,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Rex.SideBar, modifier = Modifier.width(310.dp)) {
                Sidebar(s, Modifier.fillMaxSize(), rowHeight = 40.dp) { scope.launch { drawer.close() } }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            CompactTopBar(s, darkTheme, onToggleTheme) { scope.launch { drawer.open() } }
            EditorArea(s, horizontal = false, modifier = Modifier.weight(1f))
            if (s.panelVisible) { HDivider(); BottomPanel(s, Modifier.fillMaxWidth().height(170.dp)) }
            if (imeVisible) ExtraKeysBar(s) else {
                StatusBar(s, compact = true)
                CompactBottomBar(s) { v -> s.view = v; scope.launch { drawer.open() } }
            }
        }
    }
}

@Composable
private fun CompactTopBar(s: RexCoderState, darkTheme: Boolean, onToggleTheme: () -> Unit, onMenu: () -> Unit) {
    val doc = s.focusedDoc
    Row(
        Modifier.fillMaxWidth().height(48.dp).background(Rex.TitleBar).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallIconButton(Icons.Outlined.Menu, "Open explorer", 44.dp, Rex.Text, onMenu)
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
                (doc?.name ?: "RexCoder") + if (doc?.modified == true) " ●" else "",
                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Rex.TextBright, maxLines = 1
            )
            Text("${s.groups.size} editor" + if (s.groups.size > 1) "s" else "", fontSize = 11.sp, color = Rex.TextDim)
        }
        SmallIconButton(Icons.Outlined.Search, "Command palette", 40.dp, Rex.Text) { s.paletteVisible = true }
        LayoutMenu(s, tint = Rex.Text, size = 40.dp)
        SmallIconButton(if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Toggle theme", 40.dp, Rex.Text) { onToggleTheme() }
        SmallIconButton(Icons.Outlined.Save, "Save", 40.dp, Rex.Text) { s.save() }
        SmallIconButton(Icons.Outlined.PlayArrow, "Run", 40.dp, Rex.Success) { s.panelVisible = true }
    }
}

@Composable
private fun CompactBottomBar(s: RexCoderState, onSelect: (SideView) -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp).background(Rex.ActivityBar)) {
        SideView.entries.forEach { v ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable { onSelect(v) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                val sel = s.view == v
                Icon(v.icon, v.title, tint = if (sel) Color.White else Rex.TextDim, modifier = Modifier.size(22.dp))
                Text(v.title.substringBefore(' '), fontSize = 10.sp, color = if (sel) Color.White else Rex.TextDim, maxLines = 1)
            }
        }
    }
}

// ───────────── Shared: layout switcher ─────────────

@Composable
fun LayoutMenu(s: RexCoderState, tint: Color = Rex.TextDim, size: Dp = 32.dp) {
    var open by remember { mutableStateOf(false) }
    Box {
        SmallIconButton(Icons.Outlined.ViewColumn, "Editor layout", size, tint) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (1..3).forEach { n ->
                DropdownMenuItem(
                    text = { Text(if (n == 1) "Single editor" else "$n editors") },
                    leadingIcon = { if (s.groups.size == n) Icon(Icons.Outlined.Check, null) },
                    onClick = { s.setLayout(n); open = false }
                )
            }
        }
    }
}

// ───────────── Command palette ─────────────

private class Cmd(val title: String, val hint: String, val run: () -> Unit)

@Composable
private fun CommandPalette(s: RexCoderState) {
    var q by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    val close = { s.paletteVisible = false }
    val cmds = remember {
        listOf(
            Cmd("Split Editor Right", "") { s.splitEditor() },
            Cmd("Layout: Single Editor", "") { s.setLayout(1) },
            Cmd("Layout: Two Editors", "") { s.setLayout(2) },
            Cmd("Layout: Three Editors", "") { s.setLayout(3) },
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

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp).heightIn(max = 420.dp)
                .clip(RoundedCornerShape(8.dp)).background(Color(0xFF252526))
                .border(1.dp, Rex.Border, RoundedCornerShape(8.dp))
        ) {
            BasicTextField(
                q, { q = it }, singleLine = true,
                textStyle = TextStyle(color = Rex.Text, fontSize = 15.sp),
                cursorBrush = SolidColor(Rex.Text),
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
