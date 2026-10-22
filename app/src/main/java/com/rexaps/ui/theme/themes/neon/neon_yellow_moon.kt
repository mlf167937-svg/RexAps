package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Yellow Moon: electric current travels along panel dividers. */
val neon_yellow_moon = RexDesignTokens(
    id = "neon_yellow_moon", name = "Neon Yellow Moon", category = "Neon",
    primary = argb("FFEE00"), primaryHover = blend(argb("FFEE00"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FFEE00"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FFC400"), accent = argb("FFF59D"),
    background = argb("0A0900"), backgroundAlt = argb("15130A"),
    surface = argb("FF15130A"), surfaceElevated = argb("FF232119"), surfaceHover = argb("FF2C2B22"),
    text = argb("FFFBD6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFEE00"), borderStrong = blend(argb("FFEE00"), argb("FFF59D"), .45f),
    glow = argb("FFEE00"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FFEE00"), argb("0A0900"), .35f), scrollbar = argb("FFEE00"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 2, radiusSm = 5, radiusMd = 8, radiusLg = 12, radiusXl = 16,
    blurSm = 3, blurMd = 7, blurLg = 11,
    shadowSm = 1f, shadowMd = 3f, shadowLg = 9f, shadowGlow = 25f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "monospace", fontFamilyMono = "monospace",
    fontDisplay = 30, fontTitle = 21, fontHeading = 17, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.18f, lineHeightNormal = 1.44f, lineHeightLoose = 1.6f,
    motionInstant = 42, motionFast = 100, motionNormal = 170, motionSlow = 250, motionSlower = 370, motionAmbient = 11500,
    easingStandard = "cubic-bezier(0.10,0.80,0.20,1.00)", easingEnter = "cubic-bezier(0.10,0.80,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("FFEE00"), argb("FFC400"), argb("FFF59D")),
    gradientSecondary = listOf(argb("0A0900"), argb("15130A")),
    gradientBorder = listOf(argb("FFEE00"), argb("FFF59D"), argb("FFEE00")),
    signature = "electric current travels along panel dividers",
    backgroundMotion = "neon ambient: electric current travels along panel dividers",
    transitionMotion = "Neon transition: electric current travels along panel dividers; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "amber status-light blink on focus, high-clarity hover outline, and a quick mechanical press",
    loadingMotion = "Neon loading: high-contrast scan sweep, pulsing tube segment, and a fast animated progress rail",
    notificationMotion = "neon notification in #fff59d: quick fade-slide, soft surface bloom, then a signature pulse"
)
