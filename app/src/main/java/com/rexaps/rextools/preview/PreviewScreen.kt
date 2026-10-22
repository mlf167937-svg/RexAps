package com.rexaps.rextools.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

/** Router orientasi Media Preview. */
@Composable
fun PreviewScreen(onBack: () -> Unit = {}, vm: PreviewViewModel = viewModel()) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) PreviewScreenLandscape(onBack, vm) else PreviewScreenPortrait(onBack, vm)
}
