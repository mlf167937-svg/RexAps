package com.rexaps.rextools.ssweb

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel

/** Router orientasi; state dan aksi tetap dimiliki SsWebViewModel. */
@Composable
fun SsWebScreen(onBack: () -> Unit = {}, vm: SsWebViewModel = viewModel()) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) SsWebScreenLandscape(onBack, vm) else SsWebScreenPortrait(onBack, vm)
}
