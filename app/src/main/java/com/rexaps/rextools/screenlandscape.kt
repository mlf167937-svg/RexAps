package com.rexaps.rextools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun RexToolsScreenLandscape(
    onToolClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RexToolsViewModel = viewModel()
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.LANDSCAPE) {
        RexToolsScreenContent(onToolClick, onBack, viewModel)
    }
}
