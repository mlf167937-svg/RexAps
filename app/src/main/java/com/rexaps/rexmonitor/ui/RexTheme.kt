package com.rexaps.rexmonitor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val DarkPalette = RexPalette(
    cyan = Color(0xFF18E0FF), blue = Color(0xFF2F6BFF), purple = Color(0xFF8A4DFF),
    magenta = Color(0xFFFF3DB8), green = Color(0xFF21E6A1), orange = Color(0xFFFFA52F),
    red = Color(0xFFFF4D5E),
    cardTop = Color(0xFF0C1736), cardBottom = Color(0xFF070E24),
    bgTop = Color(0xFF0A1440), bgBottom = Color(0xFF030814),
    textPrimary = Color(0xFFF2F6FF), textSecondary = Color(0xFFA9B6D8)
)

private val LightPalette = RexPalette(
    cyan = Color(0xFF007C91), blue = Color(0xFF1C4FD8), purple = Color(0xFF6A35D6),
    magenta = Color(0xFFC2187F), green = Color(0xFF0B8F5A), orange = Color(0xFFB86200),
    red = Color(0xFFC62828),
    cardTop = Color(0xFFFFFFFF), cardBottom = Color(0xFFF1F5FF),
    bgTop = Color(0xFFE6EDFF), bgBottom = Color(0xFFF6F8FF),
    textPrimary = Color(0xFF0B1330), textSecondary = Color(0xFF47527A)
)

val LocalRexPalette = staticCompositionLocalOf { DarkPalette }

object RexTheme {
    val palette: RexPalette
        @Composable get() = LocalRexPalette.current
}

@Composable
fun RexMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val p = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = p.cyan, secondary = p.purple, tertiary = p.magenta,
            background = p.bgBottom, surface = p.cardBottom,
            onBackground = p.textPrimary, onSurface = p.textPrimary,
            onSurfaceVariant = p.textSecondary
        )
    } else {
        lightColorScheme(
            primary = p.cyan, secondary = p.purple, tertiary = p.magenta,
            background = p.bgBottom, surface = p.cardBottom,
            onBackground = p.textPrimary, onSurface = p.textPrimary,
            onSurfaceVariant = p.textSecondary
        )
    }
    CompositionLocalProvider(LocalRexPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
