package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Pearl White: pearl iridescence shifts slowly along the border. */
val light_pearl_white = RexDesignTokens(
    id = "light_pearl_white", name = "Light Pearl White", category = "Light",
    primary = argb("5B6B8C"), primaryHover = blend(argb("5B6B8C"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("5B6B8C"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("8E9BB8"), accent = argb("E8ECF5"),
    background = argb("FBFBFD"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("1C2233"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("E3E6EE"), borderStrong = blend(argb("E3E6EE"), argb("E8ECF5"), .45f),
    glow = argb("5B6B8C"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("5B6B8C"), argb("FBFBFD"), .35f), scrollbar = argb("E3E6EE"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    gradientPrimary = listOf(argb("5B6B8C"), argb("8E9BB8"), argb("E8ECF5")),
    gradientSecondary = listOf(argb("FBFBFD"), argb("FFFFFF")),
    gradientBorder = listOf(argb("E3E6EE"), argb("E8ECF5"), argb("5B6B8C")),
    signature = "pearl iridescence shifts slowly along the border",
    backgroundMotion = "light ambient: pearl iridescence shifts slowly along the border",
    transitionMotion = "Light transition: pearl iridescence shifts slowly along the border",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "light shimmer and gradient progress with moving highlight",
    notificationMotion = "light toast with 5b6b8c accent and brief signature pulse"
)
