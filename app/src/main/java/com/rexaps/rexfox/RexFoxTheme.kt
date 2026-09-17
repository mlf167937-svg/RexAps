package com.rexaps.rexfox

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Violet = Color(0xFF7C3AED)
val VioletLight = Color(0xFFA78BFA)
val Cyan = Color(0xFF06B6D4)
val CyanLight = Color(0xFF67E8F9)
val DeepBlack = Color(0xFF050507)
val SurfaceDark = Color(0xFF0B0C10)
val SurfaceCard = Color(0xFF12141A)
val SurfaceElevated = Color(0xFF191C24)
val BorderSubtle = Color(0xFF292D38)
val OnSurfaceMuted = Color(0xFF9AA1B2)
val OnSurfacePrimary = Color(0xFFF3F5F8)

@Composable
fun RexFoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Cyan,
            background = DeepBlack,
            surface = SurfaceDark,
            surfaceVariant = SurfaceCard,
            onSurface = OnSurfacePrimary,
            onBackground = OnSurfacePrimary,
            onSurfaceVariant = OnSurfaceMuted,
            outline = BorderSubtle
        ),
        content = content
    )
}
