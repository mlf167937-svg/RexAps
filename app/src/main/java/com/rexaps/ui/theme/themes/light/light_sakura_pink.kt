package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Sakura Pink: a gentle sakura petal drift follows successful actions. */
val light_sakura_pink = RexDesignTokens(
    id = "light_sakura_pink", name = "Light Sakura Pink", category = "Light",
    primary = argb("B9366D"), primaryHover = blend(argb("B9366D"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("B9366D"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("F5A3C0"), accent = argb("FFD1E1"),
    background = argb("FFF7FA"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("3A1624"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("F8D7E3"), borderStrong = blend(argb("F8D7E3"), argb("FFD1E1"), .45f),
    glow = argb("B9366D"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("B9366D"), argb("FFF7FA"), .35f), scrollbar = argb("F8D7E3"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    gradientPrimary = listOf(argb("B9366D"), argb("F5A3C0"), argb("FFD1E1")),
    gradientSecondary = listOf(argb("FFF7FA"), argb("FFFFFF")),
    gradientBorder = listOf(argb("F8D7E3"), argb("FFD1E1"), argb("B9366D")),
    signature = "a gentle sakura petal drift follows successful actions",
    backgroundMotion = "light ambient: a gentle sakura petal drift follows successful actions",
    transitionMotion = "Light transition: a gentle sakura petal drift follows successful actions",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "light shimmer and gradient progress with moving highlight",
    notificationMotion = "light toast with ec6a9c accent and brief signature pulse"
)
