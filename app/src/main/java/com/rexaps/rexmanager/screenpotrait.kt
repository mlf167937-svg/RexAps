package com.rexaps.rexmanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode

@Composable
internal fun RexManagerScreenPortrait(
    onExit: () -> Unit = {}
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.PORTRAIT) {
        RexManagerScreenContent(onExit)
    }
}
