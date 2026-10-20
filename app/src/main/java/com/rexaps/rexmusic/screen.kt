package com.rexaps.rexmusic

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexMusicScreen(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel()
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexMusicScreenLandscape(onBack, vm)
    } else {
        RexMusicScreenPortrait(onBack, vm)
    }
}
