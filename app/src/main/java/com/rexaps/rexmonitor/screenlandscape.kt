package com.rexaps.rexmonitor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun RexMonitorScreenLandscape(
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
    viewModel: RexMonitorViewModel = viewModel()
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexMonitorScreenContent(modifier, onExit, viewModel)
    }
}
