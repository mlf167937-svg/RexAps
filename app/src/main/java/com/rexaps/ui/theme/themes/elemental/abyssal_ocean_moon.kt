package com.rexaps.ui.theme.themes.elemental

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Abyssal Ocean Moon: a bioluminescent jellyfish drifts through empty corners. */
val abyssal_ocean_moon = RexDesignTokens(
    id = "abyssal_ocean_moon", name = "Abyssal Ocean Moon", category = "Elemental",
    primary = argb("0EA5B7"), primaryHover = blend(argb("0EA5B7"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("0EA5B7"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("1D4ED8"), accent = argb("5EEAD4"),
    background = argb("020B14"), backgroundAlt = argb("06283C"),
    surface = argb("FF06283C"), surfaceElevated = argb("FF153548"), surfaceHover = argb("FF1F3E50"),
    text = argb("E4FBFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("0EA5B7"), borderStrong = blend(argb("0EA5B7"), argb("5EEAD4"), .45f),
    glow = argb("0EA5B7"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("0EA5B7"), argb("020B14"), .35f), scrollbar = argb("0EA5B7"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    radiusXs = 13, radiusSm = 17, radiusMd = 22, radiusLg = 28, radiusXl = 36,
    blurSm = 11, blurMd = 21, blurLg = 32,
    shadowSm = 3f, shadowMd = 9f, shadowLg = 20f, shadowGlow = 24f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 1.75f,
    fontFamilyUI = "sans-serif", fontFamilyMono = "monospace",
    fontDisplay = 35, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.24f, lineHeightNormal = 1.54f, lineHeightLoose = 1.75f,
    motionInstant = 82, motionFast = 165, motionNormal = 295, motionSlow = 450, motionSlower = 660, motionAmbient = 20500,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("0EA5B7"), argb("1D4ED8"), argb("5EEAD4")),
    gradientSecondary = listOf(argb("020B14"), argb("06283C")),
    gradientBorder = listOf(argb("0EA5B7"), argb("5EEAD4"), argb("0EA5B7")),
    signature = "a bioluminescent jellyfish drifts through empty corners",
    backgroundMotion = "elemental ambient: a bioluminescent jellyfish drifts through empty corners",
    transitionMotion = "Elemental transition: a bioluminescent jellyfish drifts through empty corners; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "wave-like highlight rolls across focus, buttons sink then rebound, and cards rise like a calm swell",
    loadingMotion = "Elemental loading: theme-colored energy sweep, directional progress motion, and a restrained particle pulse",
    notificationMotion = "elemental notification in #5eead4: quick fade-slide, soft surface bloom, then a signature pulse"
)
