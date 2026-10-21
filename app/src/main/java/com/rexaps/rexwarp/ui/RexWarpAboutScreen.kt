package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun RexWarpAboutScreen(modifier: Modifier = Modifier) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (landscape) RexWarpAboutScreenLandscape(modifier) else RexWarpAboutScreenPortrait(modifier)
}

internal val rexWarpPrivacyLines = listOf(
    "Traffic statistics are stored locally on the device.",
    "RexWARP does not need a RexAps account for local usage statistics.",
    "RexWARP does not collect analytics. No passwords or secrets are stored in plain text.",
    "RexWARP does not inspect, intercept or modify the content of your traffic."
)

internal val rexWarpAboutLines = listOf(
    "RexWARP is an independent RexAps integration.",
    "Cloudflare and WARP are trademarks of Cloudflare, Inc.",
    "RexAps is not affiliated with, endorsed by, or sponsored by Cloudflare."
)
