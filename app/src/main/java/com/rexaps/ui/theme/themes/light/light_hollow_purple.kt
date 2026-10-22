package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Hollow Purple: two pastel blobs flow together into violet. */
val light_hollow_purple = RexDesignTokens(
    id = "light_hollow_purple", name = "Light Hollow Purple", category = "Light",
    primary = argb("7C3AED"), primaryHover = blend(argb("7C3AED"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("7C3AED"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("3B82F6"), accent = argb("F43F5E"),
    background = argb("F8F5FF"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("1B1030"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("E2D8FA"), borderStrong = blend(argb("E2D8FA"), argb("F43F5E"), .45f),
    glow = argb("7C3AED"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("7C3AED"), argb("F8F5FF"), .35f), scrollbar = argb("E2D8FA"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    radiusXs = 12, radiusSm = 16, radiusMd = 21, radiusLg = 27, radiusXl = 34,
    blurSm = 8, blurMd = 15, blurLg = 22,
    shadowSm = 2f, shadowMd = 7f, shadowLg = 15f, shadowGlow = 12f,
    borderHairline = 0.4f, borderThin = 0.9f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-rounded", fontFamilyMono = "monospace",
    fontDisplay = 35, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.25f, lineHeightNormal = 1.53f, lineHeightLoose = 1.73f,
    motionInstant = 78, motionFast = 155, motionNormal = 270, motionSlow = 410, motionSlower = 600, motionAmbient = 18000,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("7C3AED"), argb("3B82F6"), argb("F43F5E")),
    gradientSecondary = listOf(argb("F8F5FF"), argb("FFFFFF")),
    gradientBorder = listOf(argb("E2D8FA"), argb("F43F5E"), argb("7C3AED")),
    signature = "two pastel blobs flow together into violet",
    backgroundMotion = "light ambient: two pastel blobs flow together into violet",
    transitionMotion = "Light transition: two pastel blobs flow together into violet; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "soft lavender bloom on focus, floating card response, and a gentle spring release",
    loadingMotion = "Light loading: soft pearl shimmer, staggered content fade-in, and a calm gradient progress line",
    notificationMotion = "light notification in #f43f5e: quick fade-slide, soft surface bloom, then a signature pulse"
)
