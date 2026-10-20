package com.rexaps.rexnux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun RexNuxScreenPortrait(
    onExit: () -> Unit = {},
    viewModel: RexNuxViewModel = viewModel()
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.PORTRAIT) {
        RexNuxScreenContent(onExit, viewModel)
    }
}
