package com.rexaps.rexnux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun RexNuxScreenLandscape(
    onExit: () -> Unit = {},
    viewModel: RexNuxViewModel = viewModel()
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexNuxScreenContent(onExit, viewModel)
    }
}
