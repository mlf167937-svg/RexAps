package com.rexaps.rexfox

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import android.app.Activity

@Composable
fun RexFoxScreen(
    activity: Activity
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexFoxScreenLandscape(activity)
    } else {
        RexFoxScreenPortrait(activity)
    }
}
