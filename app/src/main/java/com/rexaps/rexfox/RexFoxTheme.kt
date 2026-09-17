package com.rexaps.rexfox

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Violet = Color(0xFF7C3AED)
val VioletLight = Color(0xFFA78BFA)
val Cyan = Color(0xFF06B6D4)
val CyanLight = Color(0xFF67E8F9)
val DeepBlack = Color(0xFF05050A)
val SurfaceDark = Color(0xFF0D0D15)
val SurfaceCard = Color(0xFF13131E)
val SurfaceElevated = Color(0xFF1C1C2A)
val OnSurfaceMuted = Color(0xFF9090A8)
val OnSurfacePrimary = Color(0xFFF0F0FA)

private val RexFoxColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1030),
    onPrimaryContainer = VioletLight,

    secondary = Cyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0A2030),
    onSecondaryContainer = CyanLight,

    background = DeepBlack,
    onBackground = OnSurfacePrimary,

    surface = SurfaceDark,
    onSurface = OnSurfacePrimary,

    surfaceVariant = SurfaceCard,
    onSurfaceVariant = OnSurfaceMuted,

    error = Color(0xFFFF6B6B),
    outline = Color(0xFF2A2A3F)
)

@Composable
fun RexFoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RexFoxColors,
        content = content
    )
}
