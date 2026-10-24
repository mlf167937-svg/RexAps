
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

internal val LandscapeRailWidth = 300.dp

internal val LandscapeRepoPaneWidth = 320.dp

/** Keeps content clear of the notch / side navigation bar in landscape. */
@Composable
internal fun Modifier.landscapeSideInsets(): Modifier {
    return this.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    )
}
