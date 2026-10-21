package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.rexaps.rexwarp.RexWarpSettings
import com.rexaps.rexwarp.tunnel.RexWarpTunnelCapabilities

@Composable
fun RexWarpSettingsScreen(
    settings: RexWarpSettings, capabilities: RexWarpTunnelCapabilities, engineReason: String?,
    onChange: (RexWarpSettings) -> Unit, onExport: () -> Unit, onImport: () -> Unit,
    onResetToday: () -> Unit, onResetAll: () -> Unit, onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (landscape) RexWarpSettingsScreenLandscape(settings, capabilities, engineReason, onChange, onExport, onImport, onResetToday, onResetAll, onOpenAbout, modifier)
    else RexWarpSettingsScreenPortrait(settings, capabilities, engineReason, onChange, onExport, onImport, onResetToday, onResetAll, onOpenAbout, modifier)
}
