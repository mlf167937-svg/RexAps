package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Pink Moon: subtle VHS tracking line appears on screen changes. */
val neon_pink_moon = RexDesignTokens(
    id = "neon_pink_moon", name = "Neon Pink Moon", category = "Neon",
    primary = argb("FF2BD6"), primaryHover = blend(argb("FF2BD6"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FF2BD6"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF5FA2"), accent = argb("FFB0F0"),
    background = argb("0A030A"), backgroundAlt = argb("16081A"),
    surface = argb("FF16081A"), surfaceElevated = argb("FF241728"), surfaceHover = argb("FF2D2131"),
    text = argb("FFE6FA"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FF2BD6"), borderStrong = blend(argb("FF2BD6"), argb("FFB0F0"), .45f),
    glow = argb("FF2BD6"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FF2BD6"), argb("0A030A"), .35f), scrollbar = argb("FF2BD6"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 5, radiusSm = 8, radiusMd = 12, radiusLg = 17, radiusXl = 22,
    blurSm = 4, blurMd = 8, blurLg = 13,
    shadowSm = 1f, shadowMd = 4f, shadowLg = 10f, shadowGlow = 30f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif-condensed", fontFamilyMono = "monospace",
    fontDisplay = 32, fontTitle = 22, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.2f, lineHeightNormal = 1.46f, lineHeightLoose = 1.64f,
    motionInstant = 48, motionFast = 110, motionNormal = 185, motionSlow = 275, motionSlower = 410, motionAmbient = 13000,
    easingStandard = "cubic-bezier(0.10,0.80,0.20,1.00)", easingEnter = "cubic-bezier(0.10,0.80,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("FF2BD6"), argb("FF5FA2"), argb("FFB0F0")),
    gradientSecondary = listOf(argb("0A030A"), argb("16081A")),
    gradientBorder = listOf(argb("FF2BD6"), argb("FFB0F0"), argb("FF2BD6")),
    signature = "subtle VHS tracking line appears on screen changes",
    backgroundMotion = "neon ambient: subtle VHS tracking line appears on screen changes",
    transitionMotion = "Neon transition: subtle VHS tracking line appears on screen changes; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "hot-pink edge pulses once, hover glow tracks the card, and tap compresses with an elastic return",
    loadingMotion = "Neon loading: high-contrast scan sweep, pulsing tube segment, and a fast animated progress rail",
    notificationMotion = "neon notification in #ffb0f0: quick fade-slide, soft surface bloom, then a signature pulse"
)
