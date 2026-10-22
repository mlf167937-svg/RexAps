package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Emerald Moon: refracted gemstone light slowly rotates behind panels. */
val glass_emerald_moon = RexDesignTokens(
    id = "glass_emerald_moon", name = "Glass Emerald Moon", category = "Glass",
    primary = argb("2ED8A3"), primaryHover = blend(argb("2ED8A3"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("2ED8A3"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("5FE8BE"), accent = argb("A8F5DC"),
    background = argb("07140F"), backgroundAlt = argb("0C2219"),
    surface = argb("FF0C2219"), surfaceElevated = argb("FF1B2F27"), surfaceHover = argb("FF243830"),
    text = argb("E8FFF7"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("A8F5DC"), borderStrong = blend(argb("A8F5DC"), argb("A8F5DC"), .45f),
    glow = argb("2ED8A3"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("2ED8A3"), argb("07140F"), .35f), scrollbar = argb("A8F5DC"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    radiusXs = 15, radiusSm = 19, radiusMd = 24, radiusLg = 30, radiusXl = 38,
    blurSm = 14, blurMd = 24, blurLg = 36,
    shadowSm = 3f, shadowMd = 10f, shadowLg = 22f, shadowGlow = 24f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-light", fontFamilyMono = "monospace",
    fontDisplay = 36, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 300, weightMedium = 400, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.25f, lineHeightNormal = 1.56f, lineHeightLoose = 1.78f,
    motionInstant = 90, motionFast = 180, motionNormal = 315, motionSlow = 470, motionSlower = 690, motionAmbient = 21000,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("2ED8A3"), argb("5FE8BE"), argb("A8F5DC")),
    gradientSecondary = listOf(argb("07140F"), argb("0C2219")),
    gradientBorder = listOf(argb("A8F5DC"), argb("A8F5DC"), argb("2ED8A3")),
    signature = "refracted gemstone light slowly rotates behind panels",
    backgroundMotion = "glass ambient: refracted gemstone light slowly rotates behind panels",
    transitionMotion = "Glass transition: refracted gemstone light slowly rotates behind panels; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "emerald edge refracts on focus, layered card lift, and a fluid low-amplitude press spring",
    loadingMotion = "Glass loading: moving specular highlight across a translucent-looking skeleton and a layered gradient progress track",
    notificationMotion = "glass notification in #a8f5dc: quick fade-slide, soft surface bloom, then a signature pulse"
)
