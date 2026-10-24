//RexCoderApp.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.runtime.RexRuntime
import com.rexaps.rexcoder.model.SideView
import com.rexaps.rexcoder.model.rememberRexCoderState
import com.rexaps.rexcoder.theme.Rex
import com.rexaps.rexcoder.theme.RexTheme
import kotlinx.coroutines.launch

/**
 * Entry point module RexCoder.
 *  - lebar >= 600dp (landscape / tablet): Activity bar + Sidebar + editor berdampingan
 *  - lebar <  600dp (portrait phone)    : Drawer + editor bertumpuk + bottom bar + extra keys
 * Semua pembatas (sidebar, antar-editor, panel bawah) bisa digeser; double-tap pembatas = reset.
 */
@Composable
fun RexCoderApp(modifier: Modifier = Modifier, state: RexCoderState = rememberRexCoderState()) {
    RexRuntime.init(LocalContext.current)
    RexTheme {
        BoxWithConstraints(modifier.fillMaxSize().background(Rex.EditorBg)) {
            val widthDp = maxWidth.value
            val heightDp = maxHeight.value
            if (maxWidth >= 600.dp) WideLayout(state, roomy = maxHeight >= 480.dp, widthDp = widthDp, heightDp = heightDp)
            else CompactLayout(state, heightDp = heightDp)
            if (state.paletteVisible) CommandPalette(state)
        }
    }
}

// ───────────── Landscape / tablet ─────────────

@Composable
private fun WideLayout(s: RexCoderState, roomy: Boolean, widthDp: Float, heightDp: Float) {
    val density = LocalDensity.current.density
    val sidebarW = s.sidebarWidth.coerceIn(150f, (widthDp * 0.5f).coerceAtLeast(150f))
    val panelH = s.panelHeight.coerceIn(90f, (heightDp * 0.6f).coerceAtLeast(90f))
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (roomy) TitleBar(s)
        Row(Modifier.weight(1f)) {
            VerticalActivityBar(s, roomy)
            if (s.sidebarVisible) {
                Sidebar(s, Modifier.fillMaxHeight().width(sidebarW.dp), rowHeight = if (roomy) 30.dp else 28.dp)
                ResizeHandle(
                    vertical = true, desc = "Ubah lebar sidebar",
                    onDelta = { d -> s.sidebarWidth = (sidebarW + d / density).coerceIn(150f, (widthDp * 0.5f).coerceAtLeast(150f)) },
                    onReset = { s.sidebarWidth = 260f }
                )
            }
            Column(Modifier.weight(1f)) {
                EditorArea(s, horizontal = true, modifier = Modifier.weight(1f))
                if (s.panelVisible) {
                    PanelResizeHandle(s, panelH, heightDp, density)
                    BottomPanel(s, Modifier.fillMaxWidth().height(panelH.dp))
                }
            }
        }
        StatusBar(s, compact = false)
    }
}

/** Horizontal handle above the bottom panel. Dragging up makes the panel taller. */
@Composable
private fun PanelResizeHandle(s: RexCoderState, currentDp: Float, heightDp: Float, density: Float) {
    ResizeHandle(
        vertical = false, desc = "Ubah tinggi panel terminal",
        onDelta = { d -> s.panelHeight = (currentDp - d / density).coerceIn(90f, (heightDp * 0.6f).coerceAtLeast(90f)) },
        onReset = { s.panelHeight = 190f }
    )
}

@Composable
internal fun TitleBar(s: RexCoderState) {
        return __splitImpl_TitleBar(s)
    }

// ───────────── Portrait phone ─────────────

@Composable
private fun CompactLayout(s: RexCoderState, heightDp: Float) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val density = LocalDensity.current.density
    val panelH = s.panelHeight.coerceIn(90f, (heightDp * 0.6f).coerceAtLeast(90f))

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
            CompactTopBar(s) { scope.launch { drawer.open() } }
            EditorArea(s, horizontal = false, modifier = Modifier.weight(1f))
            if (s.panelVisible) {
                PanelResizeHandle(s, panelH, heightDp, density)
                BottomPanel(s, Modifier.fillMaxWidth().height(panelH.dp))
            }
            if (imeVisible) ExtraKeysBar(s) else {
                StatusBar(s, compact = true)
                CompactBottomBar(s) { v -> s.view = v; scope.launch { drawer.open() } }
            }
        }
    }
}

@Composable
private fun CompactTopBar(s: RexCoderState, onMenu: () -> Unit) {
    val doc = s.focusedDoc
    Row(
        Modifier.fillMaxWidth().height(48.dp).background(Rex.TitleBar)
            .drawBehind { drawRect(Rex.accentBrush(), Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()), alpha = 0.6f) }
            .padding(horizontal = 4.dp),
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
        SmallIconButton(Icons.Outlined.Save, "Save", 40.dp, Rex.Text) { s.save() }
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
                Icon(v.icon, v.title, tint = if (sel) Rex.Accent else Rex.TextDim, modifier = Modifier.size(22.dp))
                Text(v.title.substringBefore(' '), fontSize = 10.sp, color = if (sel) Rex.Accent else Rex.TextDim, maxLines = 1)
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
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Rex.Overlay)) {
            (1..3).forEach { n ->
                DropdownMenuItem(
                    text = { Text(if (n == 1) "Single editor" else "$n editors", color = Rex.Text) },
                    leadingIcon = { if (s.groups.size == n) Icon(Icons.Outlined.Check, null, tint = Rex.Accent) },
                    onClick = { s.setLayout(n); open = false }
                )
            }
            if (s.groups.size > 1) DropdownMenuItem(
                text = { Text("Samakan ukuran editor", color = Rex.Text) },
                leadingIcon = { Icon(Icons.Outlined.ViewColumn, null, tint = Rex.TextDim) },
                onClick = { s.equalizeGroups(); open = false }
            )
        }
    }
}

// ───────────── Command palette ─────────────

internal class Cmd(val title: String, val hint: String, val run: () -> Unit)

@Composable
internal fun CommandPalette(s: RexCoderState) {
        return __splitImpl_CommandPalette(s)
    }
