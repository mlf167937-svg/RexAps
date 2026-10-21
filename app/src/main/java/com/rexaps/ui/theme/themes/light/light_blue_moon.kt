package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Blue Moon: soft blue light bubbles rise in the background. */
val light_blue_moon = RexDesignTokens(
    id = "light_blue_moon", name = "Light Blue Moon", category = "Light",
    primary = argb("2563EB"), primaryHover = blend(argb("2563EB"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("2563EB"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("60A5FA"), accent = argb("BFDBFE"),
    background = argb("F5F9FF"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("0F172A"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("D6E4FA"), borderStrong = blend(argb("D6E4FA"), argb("BFDBFE"), .45f),
    glow = argb("2563EB"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("2563EB"), argb("F5F9FF"), .35f), scrollbar = argb("D6E4FA"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    gradientPrimary = listOf(argb("2563EB"), argb("60A5FA"), argb("BFDBFE")),
    gradientSecondary = listOf(argb("F5F9FF"), argb("FFFFFF")),
    gradientBorder = listOf(argb("D6E4FA"), argb("BFDBFE"), argb("2563EB")),
    signature = "soft blue light bubbles rise in the background",
    backgroundMotion = "light ambient: soft blue light bubbles rise in the background",
    transitionMotion = "Light transition: soft blue light bubbles rise in the background",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "light shimmer and gradient progress with moving highlight",
    notificationMotion = "light toast with 2563eb accent and brief signature pulse"
)
