package com.rexaps.rexspacex

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Launch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun RexSpaceXQuickLaunch(accent: Color) {
        SectionTitle("QUICK LAUNCH", "03")
        GameLaunchCard(
            title = "Mobile Legends: Bang Bang",
            subtitle = "Launch game",
            packageName = "com.mobile.legends",
            accent = accent
        )
        Spacer(Modifier.height(9.dp))
        GameLaunchCard(
            title = "Mobile Legends (Alternative)",
            subtitle = "Try if your MLBB package differs",
            packageName = "com.mobile.legends.mi",
            accent = accent
        )
}
