package com.rexaps.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class RexThemeOption(val label: String, val caption: String) {
    SYSTEM("Sistem", "Ikuti pengaturan perangkat"),
    LIGHT("Putih", "Bersih dan terang"),
    DARK("Gelap", "Nyaman untuk malam hari"),
    AMOLED("Hitam Pekat", "Hitam murni, hemat baterai OLED"),
    OCEAN("Ocean", "Biru malam yang sejuk"),
    FOREST("Forest", "Hijau lembut yang tenang"),
    SUNSET("Sunset", "Oranye hangat di latar gelap")
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E6EB),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF5E5E66),
    tertiary = Color(0xFF4A4A55),
    background = Color(0xFFF7F7F9),
    onBackground = Color(0xFF121214),
    surface = Color(0xFFF7F7F9),
    onSurface = Color(0xFF121214),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF66666E),
    outlineVariant = Color(0xFFE0E0E6)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE6E6EA),
    onPrimary = Color(0xFF121214),
    primaryContainer = Color(0xFF2A2A30),
    onPrimaryContainer = Color(0xFFE6E6EA),
    secondary = Color(0xFFA8A8B2),
    tertiary = Color(0xFFB8B8C2),
    background = Color(0xFF0F0F12),
    onBackground = Color(0xFFE8E8EC),
    surface = Color(0xFF0F0F12),
    onSurface = Color(0xFFE8E8EC),
    surfaceVariant = Color(0xFF1B1B20),
    onSurfaceVariant = Color(0xFF9A9AA3),
    outlineVariant = Color(0xFF2C2C33)
)

private val AmoledScheme = DarkScheme.copy(
    primary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF9A9AA3),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF121214),
    primaryContainer = Color(0xFF1E1E22),
    outlineVariant = Color(0xFF26262B)
)

private val OceanScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00263A),
    primaryContainer = Color(0xFF0F3A55),
    onPrimaryContainer = Color(0xFFCDEBFF),
    secondary = Color(0xFF8FB8D0),
    tertiary = Color(0xFF7C9CFF),
    background = Color(0xFF0A1622),
    onBackground = Color(0xFFE3F1FB),
    surface = Color(0xFF0A1622),
    onSurface = Color(0xFFE3F1FB),
    surfaceVariant = Color(0xFF122536),
    onSurfaceVariant = Color(0xFF8FA9BC),
    outlineVariant = Color(0xFF1F3A50)
)

private val ForestScheme = lightColorScheme(
    primary = Color(0xFF2E7D4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBD6),
    onPrimaryContainer = Color(0xFF0B2E1A),
    secondary = Color(0xFF55675C),
    tertiary = Color(0xFF4CAF7A),
    background = Color(0xFFF3F8F4),
    onBackground = Color(0xFF13201A),
    surface = Color(0xFFF3F8F4),
    onSurface = Color(0xFF13201A),
    surfaceVariant = Color(0xFFE3EEE6),
    onSurfaceVariant = Color(0xFF55675C),
    outlineVariant = Color(0xFFCBDCD0)
)

private val SunsetScheme = darkColorScheme(
    primary = Color(0xFFFF8A50),
    onPrimary = Color(0xFF3A1600),
    primaryContainer = Color(0xFF4A2412),
    onPrimaryContainer = Color(0xFFFFDCC8),
    secondary = Color(0xFFD9A48C),
    tertiary = Color(0xFFFF5F7E),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF6E7E1),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF6E7E1),
    surfaceVariant = Color(0xFF2A1B18),
    onSurfaceVariant = Color(0xFFB89A90),
    outlineVariant = Color(0xFF43302B)
)

fun RexThemeOption.colorScheme(systemDark: Boolean): ColorScheme = when (this) {
    RexThemeOption.SYSTEM -> if (systemDark) DarkScheme else LightScheme
    RexThemeOption.LIGHT -> LightScheme
    RexThemeOption.DARK -> DarkScheme
    RexThemeOption.AMOLED -> AmoledScheme
    RexThemeOption.OCEAN -> OceanScheme
    RexThemeOption.FOREST -> ForestScheme
    RexThemeOption.SUNSET -> SunsetScheme
}

private val baseTypography = Typography()

private val RexTypography = Typography(
    headlineLarge = baseTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun RexTheme(
    option: RexThemeOption,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = option.colorScheme(isSystemInDarkTheme()),
        typography = RexTypography,
        content = content
    )
}
