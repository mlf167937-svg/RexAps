package com.rexaps.rexgames

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** RexGames inherits the currently selected RexAps Material 3 palette and shape system. */
@Composable
internal fun RexGamesTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
