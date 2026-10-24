package com.rexaps.rexwarp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rexaps.rexwarp.RexWarpState

/** Explicit, user-triggered network diagnostics. No settings or tunnel state are changed here. */
@Composable
fun RexWarpDiagnosticsPanel(
    state: RexWarpState,
    onConnectClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) { __RexWarpDiagnosticsPanelContent(state, onConnectClick, modifier) }
