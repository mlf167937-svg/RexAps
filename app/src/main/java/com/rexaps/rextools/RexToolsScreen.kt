package com.rexaps.rextools

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

/** Entry point: hanya memilih implementasi sesuai orientasi perangkat. */
@Composable
fun RexToolsScreen(
    onToolClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RexToolsViewModel = viewModel()
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) RexToolsScreenLandscape(onToolClick, onBack, viewModel)
    else RexToolsScreenPortrait(onToolClick, onBack, viewModel)
}
