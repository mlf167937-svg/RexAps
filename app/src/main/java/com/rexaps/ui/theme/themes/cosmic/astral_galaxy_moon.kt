package com.rexaps.ui.theme.themes.cosmic

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Astral Galaxy Moon: orbital rings rotate around the hero moon. */
val astral_galaxy_moon = RexDesignTokens(
    id = "astral_galaxy_moon", name = "Astral Galaxy Moon", category = "Cosmic",
    primary = argb("7B5CFF"), primaryHover = blend(argb("7B5CFF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("7B5CFF"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("00C2FF"), accent = argb("FF7AD9"),
    background = argb("04030F"), backgroundAlt = argb("141032"),
    surface = argb("FF141032"), surfaceElevated = argb("FF221E3E"), surfaceHover = argb("FF2C2846"),
    text = argb("ECE9FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("7B5CFF"), borderStrong = blend(argb("7B5CFF"), argb("FF7AD9"), .45f),
    glow = argb("7B5CFF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("7B5CFF"), argb("04030F"), .35f), scrollbar = argb("7B5CFF"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("7B5CFF"), argb("00C2FF"), argb("FF7AD9")),
    gradientSecondary = listOf(argb("04030F"), argb("141032")),
    gradientBorder = listOf(argb("7B5CFF"), argb("FF7AD9"), argb("7B5CFF")),
    signature = "orbital rings rotate around the hero moon",
    backgroundMotion = "cosmic ambient: orbital rings rotate around the hero moon",
    transitionMotion = "Cosmic transition: orbital rings rotate around the hero moon",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "cosmic shimmer and gradient progress with moving highlight",
    notificationMotion = "cosmic toast with 7b5cff accent and brief signature pulse"
)
