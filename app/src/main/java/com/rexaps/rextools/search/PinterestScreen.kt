package com.rexaps.rextools.search

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

/** Router orientasi untuk Pinterest Search. */
@Composable
fun PinterestScreen(onBack: () -> Unit = {}, vm: PinterestViewModel = viewModel()) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) PinterestScreenLandscape(onBack, vm) else PinterestScreenPortrait(onBack, vm)
}
