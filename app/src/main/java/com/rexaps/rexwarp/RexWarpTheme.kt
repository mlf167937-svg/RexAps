package com.rexaps.rexwarp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shared RexWARP shape tokens while preserving the parent application's active color palette. */
@Composable
fun RexWarpTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val rexWarpShapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(26.dp)
    )
    MaterialTheme(colorScheme = colors, typography = typography, shapes = rexWarpShapes, content = content)
}
