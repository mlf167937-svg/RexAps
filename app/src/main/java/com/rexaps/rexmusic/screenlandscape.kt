package com.rexaps.rexmusic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun RexMusicScreenLandscape(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel()
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexMusicScreenContent(onBack, vm)
    }
}
