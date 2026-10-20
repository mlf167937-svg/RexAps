package com.rexaps.rexpanel

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import android.app.Activity

@Composable
fun RexPanelScreen(
    activity: Activity, onExit: () -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexPanelScreenLandscape(activity, onExit)
    } else {
        RexPanelScreenPortrait(activity, onExit)
    }
}
