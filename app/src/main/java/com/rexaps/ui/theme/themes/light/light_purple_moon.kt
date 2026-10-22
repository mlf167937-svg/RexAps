package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Purple Moon: pastel stars twinkle faintly. */
val light_purple_moon = RexDesignTokens(
    id = "light_purple_moon", name = "Light Purple Moon", category = "Light",
    primary = argb("7C3AED"), primaryHover = blend(argb("7C3AED"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("7C3AED"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("A78BFA"), accent = argb("DDD6FE"),
    background = argb("F9F6FF"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("1E1033"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("E4DAFA"), borderStrong = blend(argb("E4DAFA"), argb("DDD6FE"), .45f),
    glow = argb("7C3AED"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("7C3AED"), argb("F9F6FF"), .35f), scrollbar = argb("E4DAFA"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    radiusXs = 11, radiusSm = 15, radiusMd = 20, radiusLg = 26, radiusXl = 33,
    blurSm = 7, blurMd = 14, blurLg = 22,
    shadowSm = 2f, shadowMd = 6f, shadowLg = 14f, shadowGlow = 12f,
    borderHairline = 0.4f, borderThin = 0.9f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-rounded", fontFamilyMono = "monospace",
    fontDisplay = 34, fontTitle = 23, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.24f, lineHeightNormal = 1.52f, lineHeightLoose = 1.72f,
    motionInstant = 75, motionFast = 150, motionNormal = 265, motionSlow = 400, motionSlower = 585, motionAmbient = 17500,
    easingStandard = "cubic-bezier(0.18,0.90,0.22,1.00)", easingEnter = "cubic-bezier(0.18,0.90,0.22,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("7C3AED"), argb("A78BFA"), argb("DDD6FE")),
    gradientSecondary = listOf(argb("F9F6FF"), argb("FFFFFF")),
    gradientBorder = listOf(argb("E4DAFA"), argb("DDD6FE"), argb("7C3AED")),
    signature = "pastel stars twinkle faintly",
    backgroundMotion = "light ambient: pastel stars twinkle faintly",
    transitionMotion = "Light transition: pastel stars twinkle faintly; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "lavender underline draws on focus, card shadow rises smoothly, and tap scale rebounds naturally",
    loadingMotion = "Light loading: soft pearl shimmer, staggered content fade-in, and a calm gradient progress line",
    notificationMotion = "light notification in #ddd6fe: quick fade-slide, soft surface bloom, then a signature pulse"
)
