package com.rexaps.ui.theme.themes.elemental

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Inferno Magma Red Moon: lava fissures glow in a slow breathing rhythm. */
val inferno_magma_red_moon = RexDesignTokens(
    id = "inferno_magma_red_moon", name = "Inferno Magma Red Moon", category = "Elemental",
    primary = argb("FF3B1F"), primaryHover = blend(argb("FF3B1F"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FF3B1F"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF7A1A"), accent = argb("FFB347"),
    background = argb("120505"), backgroundAlt = argb("1F0A07"),
    surface = argb("FF1F0A07"), surfaceElevated = argb("FF2C1916"), surfaceHover = argb("FF352220"),
    text = argb("FFEFE6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FF7A1A"), borderStrong = blend(argb("FF7A1A"), argb("FFB347"), .45f),
    glow = argb("FF3B1F"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FF3B1F"), argb("120505"), .35f), scrollbar = argb("FF7A1A"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    radiusXs = 4, radiusSm = 7, radiusMd = 11, radiusLg = 15, radiusXl = 21,
    blurSm = 6, blurMd = 13, blurLg = 20,
    shadowSm = 2f, shadowMd = 6f, shadowLg = 15f, shadowGlow = 27f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif-condensed", fontFamilyMono = "monospace",
    fontDisplay = 33, fontTitle = 23, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.2f, lineHeightNormal = 1.48f, lineHeightLoose = 1.66f,
    motionInstant = 55, motionFast = 120, motionNormal = 205, motionSlow = 315, motionSlower = 470, motionAmbient = 13500,
    easingStandard = "cubic-bezier(0.12,0.80,0.20,1.00)", easingEnter = "cubic-bezier(0.12,0.80,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("FF3B1F"), argb("FF7A1A"), argb("FFB347")),
    gradientSecondary = listOf(argb("120505"), argb("1F0A07")),
    gradientBorder = listOf(argb("FF7A1A"), argb("FFB347"), argb("FF3B1F")),
    signature = "lava fissures glow in a slow breathing rhythm",
    backgroundMotion = "elemental ambient: lava fissures glow in a slow breathing rhythm",
    transitionMotion = "Elemental transition: lava fissures glow in a slow breathing rhythm; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "magma seam brightens on focus, ember ripple expands on tap, and hover warmth fades naturally",
    loadingMotion = "Elemental loading: theme-colored energy sweep, directional progress motion, and a restrained particle pulse",
    notificationMotion = "elemental notification in #ffb347: quick fade-slide, soft surface bloom, then a signature pulse"
)
