package com.rexaps.ui.theme.themes.elemental

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Abyssal Ocean Moon: a bioluminescent jellyfish drifts through empty corners. */
val abyssal_ocean_moon = RexDesignTokens(
    id = "abyssal_ocean_moon", name = "Abyssal Ocean Moon", category = "Elemental",
    primary = argb("0EA5B7"), primaryHover = blend(argb("0EA5B7"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("0EA5B7"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
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
    gradientPrimary = listOf(argb("0EA5B7"), argb("1D4ED8"), argb("5EEAD4")),
    gradientSecondary = listOf(argb("020B14"), argb("06283C")),
    gradientBorder = listOf(argb("0EA5B7"), argb("5EEAD4"), argb("0EA5B7")),
    signature = "a bioluminescent jellyfish drifts through empty corners",
    backgroundMotion = "elemental ambient: a bioluminescent jellyfish drifts through empty corners",
    transitionMotion = "Elemental transition: a bioluminescent jellyfish drifts through empty corners",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "elemental shimmer and gradient progress with moving highlight",
    notificationMotion = "elemental toast with 0ea5b7 accent and brief signature pulse"
)
