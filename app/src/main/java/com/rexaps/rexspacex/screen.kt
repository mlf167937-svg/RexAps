package com.rexaps.rexspacex

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun RexSpaceXScreen(
    onExit: () -> Unit = {}
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexSpaceXScreenLandscape(onExit)
    } else {
        RexSpaceXScreenPortrait(onExit)
    }
}
