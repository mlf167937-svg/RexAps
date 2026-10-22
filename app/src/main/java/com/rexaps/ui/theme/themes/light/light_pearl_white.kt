package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Pearl White: pearl iridescence shifts slowly along the border. */
val light_pearl_white = RexDesignTokens(
    id = "light_pearl_white", name = "Light Pearl White", category = "Light",
    primary = argb("5B6B8C"), primaryHover = blend(argb("5B6B8C"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("5B6B8C"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("8E9BB8"), accent = argb("E8ECF5"),
    background = argb("FBFBFD"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("1C2233"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("E3E6EE"), borderStrong = blend(argb("E3E6EE"), argb("E8ECF5"), .45f),
    glow = argb("5B6B8C"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("5B6B8C"), argb("FBFBFD"), .35f), scrollbar = argb("E3E6EE"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    radiusXs = 5, radiusSm = 9, radiusMd = 13, radiusLg = 18, radiusXl = 24,
    blurSm = 5, blurMd = 10, blurLg = 16,
    shadowSm = 1f, shadowMd = 4f, shadowLg = 10f, shadowGlow = 7f,
    borderHairline = 0.3f, borderThin = 0.7f, borderThick = 1.25f,
    fontFamilyUI = "serif", fontFamilyMono = "monospace",
    fontDisplay = 34, fontTitle = 24, fontHeading = 19, fontBody = 15, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.3f, lineHeightNormal = 1.58f, lineHeightLoose = 1.8f,
    motionInstant = 80, motionFast = 160, motionNormal = 280, motionSlow = 420, motionSlower = 620, motionAmbient = 18000,
    easingStandard = "cubic-bezier(0.20,0.00,0.00,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("5B6B8C"), argb("8E9BB8"), argb("E8ECF5")),
    gradientSecondary = listOf(argb("FBFBFD"), argb("FFFFFF")),
    gradientBorder = listOf(argb("E3E6EE"), argb("E8ECF5"), argb("5B6B8C")),
    signature = "pearl iridescence shifts slowly along the border",
    backgroundMotion = "light ambient: pearl iridescence shifts slowly along the border",
    transitionMotion = "Light transition: pearl iridescence shifts slowly along the border; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "fine pearl highlight, discreet elevation on hover, and a polished low-motion press",
    loadingMotion = "Light loading: soft pearl shimmer, staggered content fade-in, and a calm gradient progress line",
    notificationMotion = "light notification in #e8ecf5: quick fade-slide, soft surface bloom, then a signature pulse"
)
