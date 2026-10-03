package com.rexaps.rexgit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RexDark = darkColorScheme(
    background = Color(0xFF0A0C10),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF12151C),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF1B1F2A),
    onSurfaceVariant = Color(0xFF9AA3B5),
    primary = Color(0xFF9B8CFF),
    onPrimary = Color(0xFF1A1240),
    primaryContainer = Color(0xFF2A2352),
    onPrimaryContainer = Color(0xFFDAD4FF),
    secondary = Color(0xFF5EE0C9),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF123A35),
    onSecondaryContainer = Color(0xFFB8F5EA),
    tertiary = Color(0xFFFFC46B),
    onTertiary = Color(0xFF3F2A00),
    tertiaryContainer = Color(0xFF3D2F12),
    onTertiaryContainer = Color(0xFFFFE2B0),
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF4A1A1A),
    onErrorContainer = Color(0xFFFFD6D6),
    outline = Color(0xFF3A4152),
    outlineVariant = Color(0xFF272C39)
)

private val RexLight = lightColorScheme(
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF14171F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14171F),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurfaceVariant = Color(0xFF5A6377),
    primary = Color(0xFF5B47E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E2FF),
    onPrimaryContainer = Color(0xFF1E1566),
    secondary = Color(0xFF0F8F7C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3F5EE),
    onSecondaryContainer = Color(0xFF00423A),
    tertiary = Color(0xFFB26B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE9C7),
    onTertiaryContainer = Color(0xFF4A2D00),
    error = Color(0xFFD93A3A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF5A0F0F),
    outline = Color(0xFF9AA3B5),
    outlineVariant = Color(0xFFDDE1EC)
)

private val base = Typography()

private val RexTypography = Typography(
    headlineSmall = base.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

private val RexShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RexGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) RexDark else RexLight,
        typography = RexTypography,
        shapes = RexShapes,
        content = content
    )
}