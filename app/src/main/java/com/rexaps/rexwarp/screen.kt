package com.rexaps.rexwarp

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexWarpScreen(
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
    viewModel: RexWarpViewModel = viewModel()
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexWarpScreenLandscape(modifier, onExit, viewModel)
    } else {
        RexWarpScreenPortrait(modifier, onExit, viewModel)
    }
}
