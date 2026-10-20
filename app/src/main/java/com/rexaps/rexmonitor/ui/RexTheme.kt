package com.rexaps.rexmonitor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RexPalette(
    val cyan: Color,
    val blue: Color,
    val purple: Color,
    val magenta: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

private val DefaultPalette = RexPalette(
    cyan = Color(0xFF18E0FF), blue = Color(0xFF2F6BFF), purple = Color(0xFF8A4DFF),
    magenta = Color(0xFFFF3DB8), green = Color(0xFF21B879), orange = Color(0xFFBD7800),
    red = Color(0xFFD93449), cardTop = Color(0xFF1A1A1A), cardBottom = Color(0xFF101010),
    bgTop = Color(0xFF080808), bgBottom = Color(0xFF080808),
    textPrimary = Color.White, textSecondary = Color(0xFFB7B7B7)
)

val LocalRexPalette = staticCompositionLocalOf { DefaultPalette }

object RexTheme {
    val palette: RexPalette
        @Composable get() = LocalRexPalette.current
}

@Composable
fun RexMonitorTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val p = RexPalette(
        cyan = colors.primary,
        blue = colors.secondary,
        purple = colors.tertiary,
        magenta = colors.tertiary,
        green = Color(0xFF36C98F),
        orange = Color(0xFFFFB454),
        red = colors.error,
        cardTop = colors.surfaceVariant,
        cardBottom = colors.surface,
        bgTop = colors.background,
        bgBottom = colors.background,
        textPrimary = colors.onSurface,
        textSecondary = colors.onSurfaceVariant
    )
    CompositionLocalProvider(LocalRexPalette provides p, content = content)
}
