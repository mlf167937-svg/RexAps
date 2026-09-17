package com.rexaps.rexfox

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RexFoxDarkColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,

    secondary = Color(0xFF22D3EE),
    onSecondary = Color.Black,

    background = Color(0xFF08080D),
    onBackground = Color(0xFFF5F5F7),

    surface = Color(0xFF111118),
    onSurface = Color(0xFFF5F5F7),

    surfaceVariant = Color(0xFF1A1A24),
    onSurfaceVariant = Color(0xFFB8B8C7),

    primaryContainer = Color(0xFF241A3D),
    onPrimaryContainer = Color(0xFFE9DDFF)
)

@Composable
fun RexFoxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RexFoxDarkColors,
        content = content
    )
}
