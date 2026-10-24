//RexCoderApp.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.runtime.RexRuntime
import com.rexaps.rexcoder.model.rememberRexCoderState
import com.rexaps.rexcoder.theme.Rex
import com.rexaps.rexcoder.theme.RexTheme

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
