package com.rexaps.rexgit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode

@Composable
internal fun RexGitScreenLandscape(
    viewModel: RexGitViewModel
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexGitScreenContent(viewModel)
    }
}
