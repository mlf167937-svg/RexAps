package com.rexaps.rexgit

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun RexGitScreen(
    viewModel: RexGitViewModel
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexGitScreenLandscape(viewModel)
    } else {
        RexGitScreenPortrait(viewModel)
    }
}
