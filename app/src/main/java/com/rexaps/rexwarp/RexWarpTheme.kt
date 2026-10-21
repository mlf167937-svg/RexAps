package com.rexaps.rexwarp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Wrapper tipografi yang tidak pernah mengganti warna aktif dari RexAps. */
@Composable
fun RexWarpTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
