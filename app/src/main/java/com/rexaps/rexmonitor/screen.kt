package com.rexaps.rexmonitor

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexMonitorScreen(
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
    viewModel: RexMonitorViewModel = viewModel()
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexMonitorScreenLandscape(modifier, onExit, viewModel)
    } else {
        RexMonitorScreenPortrait(modifier, onExit, viewModel)
    }
}
