package com.rexaps.rexmanager

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun RexManagerScreen(
    onExit: () -> Unit = {}
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexManagerScreenLandscape(onExit)
    } else {
        RexManagerScreenPortrait(onExit)
    }
}
