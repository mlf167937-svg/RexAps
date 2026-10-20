package com.rexaps.rextools

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexToolsScreen(
    onToolClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RexToolsViewModel = viewModel()
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexToolsScreenLandscape(onToolClick, onBack, viewModel)
    } else {
        RexToolsScreenPortrait(onToolClick, onBack, viewModel)
    }
}
