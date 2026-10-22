package com.rexaps.ui.theme.themes.cosmic

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Astral Galaxy Moon: orbital rings rotate around the hero moon. */
val astral_galaxy_moon = RexDesignTokens(
    id = "astral_galaxy_moon", name = "Astral Galaxy Moon", category = "Cosmic",
    primary = argb("8268FF"), primaryHover = blend(argb("8268FF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("8268FF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("00C2FF"), accent = argb("FF7AD9"),
    background = argb("04030F"), backgroundAlt = argb("141032"),
    surface = argb("FF141032"), surfaceElevated = argb("FF221E3E"), surfaceHover = argb("FF2C2846"),
    text = argb("ECE9FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("8268FF"), borderStrong = blend(argb("8268FF"), argb("FF7AD9"), .45f),
    glow = argb("8268FF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("8268FF"), argb("04030F"), .35f), scrollbar = argb("8268FF"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    radiusXs = 13, radiusSm = 17, radiusMd = 23, radiusLg = 29, radiusXl = 37,
    blurSm = 12, blurMd = 23, blurLg = 35,
    shadowSm = 3f, shadowMd = 9f, shadowLg = 21f, shadowGlow = 28f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif-light", fontFamilyMono = "monospace",
    fontDisplay = 37, fontTitle = 25, fontHeading = 20, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 300, weightMedium = 400, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.25f, lineHeightNormal = 1.55f, lineHeightLoose = 1.77f,
    motionInstant = 88, motionFast = 180, motionNormal = 320, motionSlow = 485, motionSlower = 710, motionAmbient = 22000,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("8268FF"), argb("00C2FF"), argb("FF7AD9")),
    gradientSecondary = listOf(argb("04030F"), argb("141032")),
    gradientBorder = listOf(argb("8268FF"), argb("FF7AD9"), argb("8268FF")),
    signature = "orbital rings rotate around the hero moon",
    backgroundMotion = "cosmic ambient: orbital rings rotate around the hero moon",
    transitionMotion = "Cosmic transition: orbital rings rotate around the hero moon; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "orbital accent rotates subtly around focus, cards drift upward, and tap sends a tiny starburst ripple",
    loadingMotion = "Cosmic loading: slow aurora shimmer, orbiting progress highlight, and softly staggered star-like item reveals",
    notificationMotion = "cosmic notification in #ff7ad9: quick fade-slide, soft surface bloom, then a signature pulse"
)
