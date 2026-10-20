package com.rexaps.rexmanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode

@Composable
internal fun RexManagerScreenLandscape(
    onExit: () -> Unit = {}
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexManagerScreenContent(onExit)
    }
}
