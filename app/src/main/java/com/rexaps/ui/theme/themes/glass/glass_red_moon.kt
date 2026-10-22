package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Red Moon: red eclipse passes behind the header every 30 seconds. */
val glass_red_moon = RexDesignTokens(
    id = "glass_red_moon", name = "Glass Red Moon", category = "Glass",
    primary = argb("FF4D6D"), primaryHover = blend(argb("FF4D6D"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FF4D6D"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF7A90"), accent = argb("FFB3C1"),
    background = argb("1A0A10"), backgroundAlt = argb("2A0F1A"),
    surface = argb("FF2A0F1A"), surfaceElevated = argb("FF371D28"), surfaceHover = argb("FF3F2731"),
    text = argb("FFEFF2"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFB3C1"), borderStrong = blend(argb("FFB3C1"), argb("FFB3C1"), .45f),
    glow = argb("FF4D6D"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FF4D6D"), argb("1A0A10"), .35f), scrollbar = argb("FFB3C1"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    radiusXs = 11, radiusSm = 15, radiusMd = 20, radiusLg = 26, radiusXl = 33,
    blurSm = 11, blurMd = 21, blurLg = 31,
    shadowSm = 3f, shadowMd = 9f, shadowLg = 20f, shadowGlow = 25f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif", fontFamilyMono = "monospace",
    fontDisplay = 33, fontTitle = 23, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.22f, lineHeightNormal = 1.52f, lineHeightLoose = 1.72f,
    motionInstant = 78, motionFast = 160, motionNormal = 285, motionSlow = 430, motionSlower = 625, motionAmbient = 18500,
    easingStandard = "cubic-bezier(0.20,0.80,0.20,1.00)", easingEnter = "cubic-bezier(0.20,0.80,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("FF4D6D"), argb("FF7A90"), argb("FFB3C1")),
    gradientSecondary = listOf(argb("1A0A10"), argb("2A0F1A")),
    gradientBorder = listOf(argb("FFB3C1"), argb("FFB3C1"), argb("FF4D6D")),
    signature = "red eclipse passes behind the header every 30 seconds",
    backgroundMotion = "glass ambient: red eclipse passes behind the header every 30 seconds",
    transitionMotion = "Glass transition: red eclipse passes behind the header every 30 seconds; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "rose reflection sweeps the surface, a restrained hover lift, and a red-tinted glass ripple",
    loadingMotion = "Glass loading: moving specular highlight across a translucent-looking skeleton and a layered gradient progress track",
    notificationMotion = "glass notification in #ffb3c1: quick fade-slide, soft surface bloom, then a signature pulse"
)
