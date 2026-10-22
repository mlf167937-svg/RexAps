package com.rexaps.ui.theme.themes.cosmic

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Eclipse Black Moon: slow eclipse cycle closes and opens around the logo. */
val eclipse_black_moon = RexDesignTokens(
    id = "eclipse_black_moon", name = "Eclipse Black Moon", category = "Cosmic",
    primary = argb("FFB020"), primaryHover = blend(argb("FFB020"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FFB020"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF6A00"), accent = argb("FFE08A"),
    background = argb("000000"), backgroundAlt = argb("0A0A0A"),
    surface = argb("FF0A0A0A"), surfaceElevated = argb("FF191919"), surfaceHover = argb("FF222222"),
    text = argb("F6F1E6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFB020"), borderStrong = blend(argb("FFB020"), argb("FFE08A"), .45f),
    glow = argb("FFB020"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FFB020"), argb("000000"), .35f), scrollbar = argb("FFB020"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    radiusXs = 4, radiusSm = 8, radiusMd = 12, radiusLg = 17, radiusXl = 22,
    blurSm = 8, blurMd = 16, blurLg = 24,
    shadowSm = 2f, shadowMd = 7f, shadowLg = 17f, shadowGlow = 24f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "serif", fontFamilyMono = "monospace",
    fontDisplay = 36, fontTitle = 25, fontHeading = 20, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.25f, lineHeightNormal = 1.52f, lineHeightLoose = 1.72f,
    motionInstant = 76, motionFast = 155, motionNormal = 275, motionSlow = 425, motionSlower = 620, motionAmbient = 19500,
    easingStandard = "cubic-bezier(0.18,0.90,0.22,1.00)", easingEnter = "cubic-bezier(0.18,0.90,0.22,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("FFB020"), argb("FF6A00"), argb("FFE08A")),
    gradientSecondary = listOf(argb("000000"), argb("0A0A0A")),
    gradientBorder = listOf(argb("FFB020"), argb("FFE08A"), argb("FFB020")),
    signature = "slow eclipse cycle closes and opens around the logo",
    backgroundMotion = "cosmic ambient: slow eclipse cycle closes and opens around the logo",
    transitionMotion = "Cosmic transition: slow eclipse cycle closes and opens around the logo; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "warm corona brightens around focus, surfaces contract gently on tap, and a controlled eclipse pulse returns",
    loadingMotion = "Cosmic loading: slow aurora shimmer, orbiting progress highlight, and softly staggered star-like item reveals",
    notificationMotion = "cosmic notification in #ffe08a: quick fade-slide, soft surface bloom, then a signature pulse"
)
