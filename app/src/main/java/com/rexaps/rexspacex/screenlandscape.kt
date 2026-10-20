package com.rexaps.rexspacex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode

@Composable
internal fun RexSpaceXScreenLandscape(
    onExit: () -> Unit = {}
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexSpaceXScreenContent(onExit)
    }
}
