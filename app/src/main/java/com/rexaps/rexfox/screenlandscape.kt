package com.rexaps.rexfox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import android.app.Activity

@Composable
internal fun RexFoxScreenLandscape(
    activity: Activity
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexFoxScreenContent(activity)
    }
}
