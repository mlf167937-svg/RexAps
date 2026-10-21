package com.rexaps.rexgit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

@Composable
fun RexGitTheme(content: @Composable () -> Unit) {
    // Keep the app shell's selected color scheme; this module only specializes typography.
    val inheritedColors = MaterialTheme.colorScheme
    val inheritedShapes = MaterialTheme.shapes
    MaterialTheme(
        colorScheme = inheritedColors,
        typography = RexTypography,
        shapes = inheritedShapes,
        content = content
    )
}
