package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Purple Moon: tiny stars appear around interaction points. */
val glass_purple_moon = RexDesignTokens(
    id = "glass_purple_moon", name = "Glass Purple Moon", category = "Glass",
    primary = argb("9B6BFF"), primaryHover = blend(argb("9B6BFF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("9B6BFF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("B794FF"), accent = argb("D6C2FF"),
    background = argb("120B22"), backgroundAlt = argb("1E1238"),
    surface = argb("FF1E1238"), surfaceElevated = argb("FF2C2044"), surfaceHover = argb("FF342A4C"),
    text = argb("F3EDFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("D6C2FF"), borderStrong = blend(argb("D6C2FF"), argb("D6C2FF"), .45f),
    glow = argb("9B6BFF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("9B6BFF"), argb("120B22"), .35f), scrollbar = argb("D6C2FF"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    radiusXs = 14, radiusSm = 18, radiusMd = 23, radiusLg = 29, radiusXl = 37,
    blurSm = 13, blurMd = 23, blurLg = 34,
    shadowSm = 3f, shadowMd = 10f, shadowLg = 22f, shadowGlow = 26f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-light", fontFamilyMono = "monospace",
    fontDisplay = 35, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 300, weightMedium = 400, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.25f, lineHeightNormal = 1.55f, lineHeightLoose = 1.76f,
    motionInstant = 88, motionFast = 175, motionNormal = 310, motionSlow = 465, motionSlower = 680, motionAmbient = 20500,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("9B6BFF"), argb("B794FF"), argb("D6C2FF")),
    gradientSecondary = listOf(argb("120B22"), argb("1E1238")),
    gradientBorder = listOf(argb("D6C2FF"), argb("D6C2FF"), argb("9B6BFF")),
    signature = "tiny stars appear around interaction points",
    backgroundMotion = "glass ambient: tiny stars appear around interaction points",
    transitionMotion = "Glass transition: tiny stars appear around interaction points; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "lilac light glides along rounded edges, gentle hover elevation, and a springy tap release",
    loadingMotion = "Glass loading: moving specular highlight across a translucent-looking skeleton and a layered gradient progress track",
    notificationMotion = "glass notification in #d6c2ff: quick fade-slide, soft surface bloom, then a signature pulse"
)
