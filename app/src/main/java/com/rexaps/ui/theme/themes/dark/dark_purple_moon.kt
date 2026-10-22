package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Purple Moon: fine constellation lines draw into empty areas. */
val dark_purple_moon = RexDesignTokens(
    id = "dark_purple_moon", name = "Dark Purple Moon", category = "Dark",
    primary = argb("9368F7"), primaryHover = blend(argb("9368F7"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("9368F7"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("A78BFA"), accent = argb("C4B5FD"),
    background = argb("0A0614"), backgroundAlt = argb("150E26"),
    surface = argb("FF150E26"), surfaceElevated = argb("FF231C33"), surfaceHover = argb("FF2C263C"),
    text = argb("EFE9FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("2C1F4D"), borderStrong = blend(argb("2C1F4D"), argb("C4B5FD"), .45f),
    glow = argb("9368F7"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("9368F7"), argb("0A0614"), .35f), scrollbar = argb("2C1F4D"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 10, radiusSm = 14, radiusMd = 19, radiusLg = 25, radiusXl = 32,
    blurSm = 9, blurMd = 18, blurLg = 28,
    shadowSm = 2f, shadowMd = 8f, shadowLg = 18f, shadowGlow = 26f,
    borderHairline = 0.6f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif-rounded", fontFamilyMono = "monospace",
    fontDisplay = 35, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.25f, lineHeightNormal = 1.53f, lineHeightLoose = 1.72f,
    motionInstant = 78, motionFast = 160, motionNormal = 280, motionSlow = 430, motionSlower = 640, motionAmbient = 20000,
    easingStandard = "cubic-bezier(0.18,0.90,0.22,1.00)", easingEnter = "cubic-bezier(0.18,0.90,0.22,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("9368F7"), argb("A78BFA"), argb("C4B5FD")),
    gradientSecondary = listOf(argb("0A0614"), argb("150E26")),
    gradientBorder = listOf(argb("2C1F4D"), argb("C4B5FD"), argb("9368F7")),
    signature = "fine constellation lines draw into empty areas",
    backgroundMotion = "dark ambient: fine constellation lines draw into empty areas",
    transitionMotion = "Dark transition: fine constellation lines draw into empty areas; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "violet halo blooms on focus, cards float gently on hover, and taps compress then rebound",
    loadingMotion = "Dark loading: low-contrast metallic sweep over a breathing skeleton, with a short accent-color progress fill",
    notificationMotion = "dark notification in #c4b5fd: quick fade-slide, soft surface bloom, then a signature pulse"
)
