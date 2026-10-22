package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Gold Moon: moving gold-foil highlight tracks pointer or tilt. */
val glass_gold_moon = RexDesignTokens(
    id = "glass_gold_moon", name = "Glass Gold Moon", category = "Glass",
    primary = argb("F5C451"), primaryHover = blend(argb("F5C451"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("F5C451"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FFD97A"), accent = argb("FFEBB0"),
    background = argb("17120A"), backgroundAlt = argb("2A200E"),
    surface = argb("FF2A200E"), surfaceElevated = argb("FF372D1C"), surfaceHover = argb("FF3F3626"),
    text = argb("FFF8E6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFEBB0"), borderStrong = blend(argb("FFEBB0"), argb("FFEBB0"), .45f),
    glow = argb("F5C451"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("F5C451"), argb("17120A"), .35f), scrollbar = argb("FFEBB0"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    radiusXs = 10, radiusSm = 14, radiusMd = 19, radiusLg = 25, radiusXl = 32,
    blurSm = 12, blurMd = 22, blurLg = 32,
    shadowSm = 3f, shadowMd = 9f, shadowLg = 20f, shadowGlow = 23f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 1.5f,
    fontFamilyUI = "serif", fontFamilyMono = "monospace",
    fontDisplay = 36, fontTitle = 25, fontHeading = 20, fontBody = 15, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.28f, lineHeightNormal = 1.56f, lineHeightLoose = 1.78f,
    motionInstant = 90, motionFast = 180, motionNormal = 320, motionSlow = 480, motionSlower = 700, motionAmbient = 22000,
    easingStandard = "cubic-bezier(0.18,1.00,0.28,1.00)", easingEnter = "cubic-bezier(0.18,1.00,0.28,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("F5C451"), argb("FFD97A"), argb("FFEBB0")),
    gradientSecondary = listOf(argb("17120A"), argb("2A200E")),
    gradientBorder = listOf(argb("FFEBB0"), argb("FFEBB0"), argb("F5C451")),
    signature = "moving gold-foil highlight tracks pointer or tilt",
    backgroundMotion = "glass ambient: moving gold-foil highlight tracks pointer or tilt",
    transitionMotion = "Glass transition: moving gold-foil highlight tracks pointer or tilt; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "warm foil glint follows focus, elegant hover lift, and a brief golden ripple on tap",
    loadingMotion = "Glass loading: moving specular highlight across a translucent-looking skeleton and a layered gradient progress track",
    notificationMotion = "glass notification in #ffebb0: quick fade-slide, soft surface bloom, then a signature pulse"
)
