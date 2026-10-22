package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Red Moon: rounded pink petals drift gently downward. */
val light_red_moon = RexDesignTokens(
    id = "light_red_moon", name = "Light Red Moon", category = "Light",
    primary = argb("DC2626"), primaryHover = blend(argb("DC2626"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("DC2626"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("F87171"), accent = argb("FECACA"),
    background = argb("FFF6F6"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("2A0E0E"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("F6D4D4"), borderStrong = blend(argb("F6D4D4"), argb("FECACA"), .45f),
    glow = argb("DC2626"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("DC2626"), argb("FFF6F6"), .35f), scrollbar = argb("F6D4D4"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    radiusXs = 7, radiusSm = 11, radiusMd = 15, radiusLg = 20, radiusXl = 26,
    blurSm = 6, blurMd = 12, blurLg = 18,
    shadowSm = 2f, shadowMd = 5f, shadowLg = 12f, shadowGlow = 11f,
    borderHairline = 0.4f, borderThin = 0.9f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif", fontFamilyMono = "monospace",
    fontDisplay = 32, fontTitle = 22, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.21f, lineHeightNormal = 1.48f, lineHeightLoose = 1.66f,
    motionInstant = 68, motionFast = 140, motionNormal = 235, motionSlow = 360, motionSlower = 525, motionAmbient = 15500,
    easingStandard = "cubic-bezier(0.20,0.00,0.00,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("DC2626"), argb("F87171"), argb("FECACA")),
    gradientSecondary = listOf(argb("FFF6F6"), argb("FFFFFF")),
    gradientBorder = listOf(argb("F6D4D4"), argb("FECACA"), argb("DC2626")),
    signature = "rounded pink petals drift gently downward",
    backgroundMotion = "light ambient: rounded pink petals drift gently downward",
    transitionMotion = "Light transition: rounded pink petals drift gently downward; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "coral ripple, confident button press, and warm focus border with no harsh flash",
    loadingMotion = "Light loading: soft pearl shimmer, staggered content fade-in, and a calm gradient progress line",
    notificationMotion = "light notification in #fecaca: quick fade-slide, soft surface bloom, then a signature pulse"
)
