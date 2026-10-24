//RexCoderApp.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.model.SideView
import com.rexaps.rexcoder.theme.Rex
import kotlinx.coroutines.launch

@Composable
internal fun WideLayout(s: RexCoderState, roomy: Boolean, widthDp: Float, heightDp: Float) {
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

@Composable
internal fun CompactLayout(s: RexCoderState, heightDp: Float) {
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
internal fun CompactBottomBar(s: RexCoderState, onSelect: (SideView) -> Unit) {
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

internal class Cmd(val title: String, val hint: String, val run: () -> Unit)
