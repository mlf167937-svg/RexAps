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

/**
 * RexGit design tokens. Components should prefer MaterialTheme.colorScheme over
 * introducing one-off hex colors so all repository screens stay consistent.
 */
private object RexGitPalette {
    // Shared brand / feedback tokens
    val blue = Color(0xFF0969DA)
    val lightBlue = Color(0xFF79C0FF)
    val green = Color(0xFF3FB950)
    val amber = Color(0xFFD29922)
    val red = Color(0xFFFF7B72)

    val darkBackground = Color(0xFF060B16)
    val darkSurface = Color(0xFF0B1220)
    val darkSurfaceVariant = Color(0xFF151F30)
    val darkOnSurface = Color(0xFFE6EDF3)
    val darkOnSurfaceVariant = Color(0xFFA8B3C5)
    val darkOutline = Color(0xFF5A667A)
    val darkOutlineVariant = Color(0xFF273449)

    val lightBackground = Color(0xFFF6F8FA)
    val lightSurface = Color(0xFFFFFFFF)
    val lightSurfaceVariant = Color(0xFFEAEEF3)
    val lightOnSurface = Color(0xFF1F2328)
    val lightOnSurfaceVariant = Color(0xFF59636E)
    val lightOutline = Color(0xFF8C959F)
    val lightOutlineVariant = Color(0xFFD0D7DE)
}

private val RexDarkColors = darkColorScheme(
    primary = RexGitPalette.lightBlue,
    onPrimary = Color(0xFF061329),
    primaryContainer = Color(0xFF173D70),
    onPrimaryContainer = Color(0xFFDCEBFF),
    secondary = Color(0xFFB7C7E0),
    onSecondary = Color(0xFF1B2A40),
    secondaryContainer = Color(0xFF253650),
    onSecondaryContainer = Color(0xFFDCE7FA),
    background = RexGitPalette.darkBackground,
    onBackground = RexGitPalette.darkOnSurface,
    surface = RexGitPalette.darkSurface,
    onSurface = RexGitPalette.darkOnSurface,
    surfaceVariant = RexGitPalette.darkSurfaceVariant,
    onSurfaceVariant = RexGitPalette.darkOnSurfaceVariant,
    outline = RexGitPalette.darkOutline,
    outlineVariant = RexGitPalette.darkOutlineVariant,
    error = RexGitPalette.red,
    onError = Color(0xFF3B0704),
    errorContainer = Color(0xFF5B1E1C),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val RexLightColors = lightColorScheme(
    primary = RexGitPalette.blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF0B376F),
    secondary = Color(0xFF53657A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E8F2),
    onSecondaryContainer = Color(0xFF1C2B40),
    background = RexGitPalette.lightBackground,
    onBackground = RexGitPalette.lightOnSurface,
    surface = RexGitPalette.lightSurface,
    onSurface = RexGitPalette.lightOnSurface,
    surfaceVariant = RexGitPalette.lightSurfaceVariant,
    onSurfaceVariant = RexGitPalette.lightOnSurfaceVariant,
    outline = RexGitPalette.lightOutline,
    outlineVariant = RexGitPalette.lightOutlineVariant,
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3E0),
    onErrorContainer = Color(0xFF7A1710)
)

private val RexBaseTypography = Typography()

private val RexTypography = Typography(
    headlineLarge = RexBaseTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.45).sp
    ),
    headlineMedium = RexBaseTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = RexBaseTypography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = RexBaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = RexBaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = RexBaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = RexBaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

private val RexShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/** Follows the host app's system theme; README preview can still choose its own theme. */
@Composable
fun RexGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) RexDarkColors else RexLightColors,
        typography = RexTypography,
        shapes = RexShapes,
        content = content
    )
}
