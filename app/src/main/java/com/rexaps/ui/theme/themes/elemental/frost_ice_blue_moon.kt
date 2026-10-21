package com.rexaps.ui.theme.themes.elemental

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Frost Ice Blue Moon: frost crystals sparkle along panel edges. */
val frost_ice_blue_moon = RexDesignTokens(
    id = "frost_ice_blue_moon", name = "Frost Ice Blue Moon", category = "Elemental",
    primary = argb("7DD3FC"), primaryHover = blend(argb("7DD3FC"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("7DD3FC"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("38BDF8"), accent = argb("E0F7FF"),
    background = argb("04121C"), backgroundAlt = argb("102632"),
    surface = argb("FF102632"), surfaceElevated = argb("FF1E333E"), surfaceHover = argb("FF283C46"),
    text = argb("EAFBFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("7DD3FC"), borderStrong = blend(argb("7DD3FC"), argb("E0F7FF"), .45f),
    glow = argb("7DD3FC"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("7DD3FC"), argb("04121C"), .35f), scrollbar = argb("7DD3FC"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("7DD3FC"), argb("38BDF8"), argb("E0F7FF")),
    gradientSecondary = listOf(argb("04121C"), argb("102632")),
    gradientBorder = listOf(argb("7DD3FC"), argb("E0F7FF"), argb("7DD3FC")),
    signature = "frost crystals sparkle along panel edges",
    backgroundMotion = "elemental ambient: frost crystals sparkle along panel edges",
    transitionMotion = "Elemental transition: frost crystals sparkle along panel edges",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "elemental shimmer and gradient progress with moving highlight",
    notificationMotion = "elemental toast with 7dd3fc accent and brief signature pulse"
)
