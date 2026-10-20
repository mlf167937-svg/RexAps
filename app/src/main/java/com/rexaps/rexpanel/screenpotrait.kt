package com.rexaps.rexpanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import android.app.Activity

@Composable
internal fun RexPanelScreenPortrait(
    activity: Activity, onExit: () -> Unit
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.PORTRAIT) {
        RexPanelScreenContent(activity, onExit)
    }
}
