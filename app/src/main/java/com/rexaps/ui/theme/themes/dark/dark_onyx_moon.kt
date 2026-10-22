package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Onyx Moon: white ink spreads subtly from the tap point. */
val dark_onyx_moon = RexDesignTokens(
    id = "dark_onyx_moon", name = "Dark Onyx Moon", category = "Dark",
    primary = argb("E5E7EB"), primaryHover = blend(argb("E5E7EB"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("E5E7EB"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("9CA3AF"), accent = argb("FFFFFF"),
    background = argb("000000"), backgroundAlt = argb("0B0B0C"),
    surface = argb("FF0B0B0C"), surfaceElevated = argb("FF1A1A1B"), surfaceHover = argb("FF232324"),
    text = argb("F5F5F5"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("222224"), borderStrong = blend(argb("222224"), argb("FFFFFF"), .45f),
    glow = argb("E5E7EB"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("E5E7EB"), argb("000000"), .35f), scrollbar = argb("222224"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 2, radiusSm = 5, radiusMd = 8, radiusLg = 12, radiusXl = 16,
    blurSm = 3, blurMd = 7, blurLg = 12,
    shadowSm = 1f, shadowMd = 3f, shadowLg = 8f, shadowGlow = 5f,
    borderHairline = 0.4f, borderThin = 0.8f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-condensed", fontFamilyMono = "monospace",
    fontDisplay = 29, fontTitle = 20, fontHeading = 17, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.18f, lineHeightNormal = 1.44f, lineHeightLoose = 1.6f,
    motionInstant = 55, motionFast = 120, motionNormal = 200, motionSlow = 310, motionSlower = 460, motionAmbient = 13000,
    easingStandard = "cubic-bezier(0.20,0.00,0.00,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("E5E7EB"), argb("9CA3AF"), argb("FFFFFF")),
    gradientSecondary = listOf(argb("000000"), argb("0B0B0C")),
    gradientBorder = listOf(argb("222224"), argb("FFFFFF"), argb("E5E7EB")),
    signature = "white ink spreads subtly from the tap point",
    backgroundMotion = "dark ambient: white ink spreads subtly from the tap point",
    transitionMotion = "Dark transition: white ink spreads subtly from the tap point; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "minimal monochrome ink ripple, firm pressed state, and a hairline focus outline",
    loadingMotion = "Dark loading: low-contrast metallic sweep over a breathing skeleton, with a short accent-color progress fill",
    notificationMotion = "dark notification in #ffffff: quick fade-slide, soft surface bloom, then a signature pulse"
)
