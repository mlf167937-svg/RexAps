package com.rexaps.rexnux

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexNuxScreen(
    onExit: () -> Unit = {},
    viewModel: RexNuxViewModel = viewModel()
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexNuxScreenLandscape(onExit, viewModel)
    } else {
        RexNuxScreenPortrait(onExit, viewModel)
    }
}
