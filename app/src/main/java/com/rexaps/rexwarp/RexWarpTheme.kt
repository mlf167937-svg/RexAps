package com.rexaps.rexwarp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/** Hanya untuk preview/standalone. Di RexAps, pakai MaterialTheme dari shell agar warna konsisten. */
@Composable
fun RexWarpTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content)
