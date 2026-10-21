package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.rexaps.rexwarp.DateRange
import com.rexaps.rexwarp.RexWarpGraphRange
import com.rexaps.rexwarp.RexWarpRangePreset
import com.rexaps.rexwarp.RexWarpUsageUi

@Composable
fun RexWarpUsageScreen(
    ui: RexWarpUsageUi?, onPreset: (RexWarpRangePreset) -> Unit, onGraph: (RexWarpGraphRange) -> Unit,
    onCustomRange: (DateRange) -> Unit, modifier: Modifier = Modifier
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (landscape) RexWarpUsageScreenLandscape(ui, onPreset, onGraph, onCustomRange, modifier)
    else RexWarpUsageScreenPortrait(ui, onPreset, onGraph, onCustomRange, modifier)
}
