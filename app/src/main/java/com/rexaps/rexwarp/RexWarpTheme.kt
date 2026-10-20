package com.rexaps.rexwarp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Wrapper untuk typography/standalone usage; warna tetap diwarisi dari tema utama RexAps. */
@Composable
fun RexWarpTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
