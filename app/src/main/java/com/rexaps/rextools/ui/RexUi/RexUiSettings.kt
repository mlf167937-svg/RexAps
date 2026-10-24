// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun RexTheme(content: @Composable () -> Unit) {
    // Explicitly forward all parent tokens: MaterialTheme's default colorScheme is
    // lightColorScheme(), which previously silently reset the app palette in RexTools.
    val inheritedColors = MaterialTheme.colorScheme
    val inheritedShapes = MaterialTheme.shapes
    val baseTypography = MaterialTheme.typography
    val rexTypography = baseTypography.copy(
        displaySmall = baseTypography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
        headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        labelSmall = baseTypography.labelSmall.copy(letterSpacing = 0.8.sp)
    )
    MaterialTheme(
        colorScheme = inheritedColors,
        typography = rexTypography,
        shapes = inheritedShapes,
        content = content
    )
}

/* ═════════════════════ MODIFIERS ═════════════════════ */
