package com.rexaps.rextools.tiktok

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

/** Router orientasi TikTok Stalker. */
@Composable
fun TiktokStalkerScreen(onBack: () -> Unit = {}, vm: TiktokStalkerViewModel = viewModel()) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) TiktokStalkerScreenLandscape(onBack, vm) else TiktokStalkerScreenPortrait(onBack, vm)
}
