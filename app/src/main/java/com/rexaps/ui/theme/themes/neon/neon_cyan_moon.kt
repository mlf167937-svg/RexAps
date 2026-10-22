package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Cyan Moon: neon tube lettering lights up in stages. */
val neon_cyan_moon = RexDesignTokens(
    id = "neon_cyan_moon", name = "Neon Cyan Moon", category = "Neon",
    primary = argb("00F0FF"), primaryHover = blend(argb("00F0FF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("00F0FF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("00B8D4"), accent = argb("7DF9FF"),
    background = argb("03070D"), backgroundAlt = argb("08121C"),
    surface = argb("FF08121C"), surfaceElevated = argb("FF17202A"), surfaceHover = argb("FF212A33"),
    text = argb("E0FCFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("00F0FF"), borderStrong = blend(argb("00F0FF"), argb("7DF9FF"), .45f),
    glow = argb("00F0FF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("00F0FF"), argb("03070D"), .35f), scrollbar = argb("00F0FF"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 2, radiusSm = 4, radiusMd = 7, radiusLg = 10, radiusXl = 14,
    blurSm = 3, blurMd = 6, blurLg = 10,
    shadowSm = 1f, shadowMd = 3f, shadowLg = 8f, shadowGlow = 24f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "monospace", fontFamilyMono = "monospace",
    fontDisplay = 30, fontTitle = 21, fontHeading = 17, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.18f, lineHeightNormal = 1.44f, lineHeightLoose = 1.6f,
    motionInstant = 45, motionFast = 105, motionNormal = 175, motionSlow = 260, motionSlower = 390, motionAmbient = 12000,
    easingStandard = "cubic-bezier(0.10,0.80,0.20,1.00)", easingEnter = "cubic-bezier(0.10,0.80,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("00F0FF"), argb("00B8D4"), argb("7DF9FF")),
    gradientSecondary = listOf(argb("03070D"), argb("08121C")),
    gradientBorder = listOf(argb("00F0FF"), argb("7DF9FF"), argb("00F0FF")),
    signature = "neon tube lettering lights up in stages",
    backgroundMotion = "neon ambient: neon tube lettering lights up in stages",
    transitionMotion = "Neon transition: neon tube lettering lights up in stages; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "cyan tube ignites on focus, sharp scanline hover, 0.94 press snap, and a short electric afterglow",
    loadingMotion = "Neon loading: high-contrast scan sweep, pulsing tube segment, and a fast animated progress rail",
    notificationMotion = "neon notification in #7df9ff: quick fade-slide, soft surface bloom, then a signature pulse"
)
