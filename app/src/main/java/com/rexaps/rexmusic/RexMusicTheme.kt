package com.rexaps.rexmusic

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Warna dasar player (selalu gelap, gaya Spotify). */
val RexPlayerBase = Color(0xFF09090D)

val RexHeroBrush: Brush = Brush.linearGradient(listOf(Color(0xFF6A4DFF), Color(0xFF0F9D8C)))
val RexOfflineBrush: Brush = Brush.linearGradient(listOf(Color(0xFF0F9D8C), Color(0xFF2B62E0)))
val RexAccentBrush: Brush = Brush.horizontalGradient(listOf(Color(0xFF9B8CFF), Color(0xFF3DE0C8)))

private val RexDark = darkColorScheme(
    primary = Color(0xFF9B8CFF),
    onPrimary = Color(0xFF130B3D),
    primaryContainer = Color(0xFF2A2160),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFFB4ADD6),
    onSecondary = Color(0xFF1B1733),
    secondaryContainer = Color(0xFF26233A),
    onSecondaryContainer = Color(0xFFE3DFF5),
    tertiary = Color(0xFF3DE0C8),
    onTertiary = Color(0xFF00201B),
    tertiaryContainer = Color(0xFF0F3B35),
    onTertiaryContainer = Color(0xFFB9F5EC),
    background = Color(0xFF0A0A0F),
    onBackground = Color(0xFFF3F3F8),
    surface = Color(0xFF111117),
    onSurface = Color(0xFFF3F3F8),
    surfaceVariant = Color(0xFF1C1C25),
    onSurfaceVariant = Color(0xFFA0A0B2),
    outline = Color(0xFF3A3A48),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF3F1519),
    onErrorContainer = Color(0xFFFFD9D6)
)

private val RexLight = lightColorScheme(
    primary = Color(0xFF5B47E0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF1A0F5C),
    secondary = Color(0xFF5F5B7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E3F5),
    onSecondaryContainer = Color(0xFF1C1933),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB9F5EC),
    onTertiaryContainer = Color(0xFF00201B),
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF14141A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14141A),
    surfaceVariant = Color(0xFFEDEDF3),
    onSurfaceVariant = Color(0xFF5F5F70),
    outline = Color(0xFFC6C6D2),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val RexTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
)

@Composable
fun RexMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme: ColorScheme = if (darkTheme) RexDark else RexLight
    MaterialTheme(colorScheme = scheme, typography = RexTypography) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
