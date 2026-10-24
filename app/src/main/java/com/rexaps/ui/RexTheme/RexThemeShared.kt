package com.rexaps.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val baseTypography = Typography()

internal val rexTypography = Typography(
    displayLarge=baseTypography.displayLarge.copy(fontWeight=FontWeight.Bold, letterSpacing=(-1.0).sp),
    headlineLarge=baseTypography.headlineLarge.copy(fontWeight=FontWeight.Bold, letterSpacing=(-0.65).sp),
    headlineMedium=baseTypography.headlineMedium.copy(fontWeight=FontWeight.Bold, letterSpacing=(-0.4).sp),
    headlineSmall=baseTypography.headlineSmall.copy(fontWeight=FontWeight.SemiBold, letterSpacing=(-0.25).sp),
    titleLarge=baseTypography.titleLarge.copy(fontWeight=FontWeight.SemiBold, letterSpacing=(-0.2).sp),
    titleMedium=baseTypography.titleMedium.copy(fontWeight=FontWeight.SemiBold),
    titleSmall=baseTypography.titleSmall.copy(fontWeight=FontWeight.SemiBold),
    bodyLarge=baseTypography.bodyLarge.copy(lineHeight=24.sp), bodyMedium=baseTypography.bodyMedium.copy(lineHeight=21.sp),
    bodySmall=baseTypography.bodySmall.copy(lineHeight=18.sp), labelLarge=baseTypography.labelLarge.copy(fontWeight=FontWeight.SemiBold),
    labelMedium=baseTypography.labelMedium.copy(fontWeight=FontWeight.Medium), labelSmall=baseTypography.labelSmall.copy(fontWeight=FontWeight.Medium)
)
