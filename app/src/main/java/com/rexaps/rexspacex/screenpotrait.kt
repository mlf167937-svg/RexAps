package com.rexaps.rexspacex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode

@Composable
internal fun RexSpaceXScreenPortrait(
    onExit: () -> Unit = {}
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.PORTRAIT) {
        RexSpaceXScreenContent(onExit)
    }
}
