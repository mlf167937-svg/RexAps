package com.rexaps.rextools.tiktok

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

/** Router orientasi TikTok Downloader. */
@Composable
fun TiktokDownloaderScreen(onBack: () -> Unit = {}, vm: TiktokDownloaderViewModel = viewModel()) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) TiktokDownloaderScreenLandscape(onBack, vm) else TiktokDownloaderScreenPortrait(onBack, vm)
}
