package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Hollow Purple: two pastel blobs flow together into violet. */
val light_hollow_purple = RexDesignTokens(
    id = "light_hollow_purple", name = "Light Hollow Purple", category = "Light",
    primary = argb("7C3AED"), primaryHover = blend(argb("7C3AED"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("7C3AED"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("3B82F6"), accent = argb("F43F5E"),
    background = argb("F8F5FF"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("1B1030"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("E2D8FA"), borderStrong = blend(argb("E2D8FA"), argb("F43F5E"), .45f),
    glow = argb("7C3AED"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("7C3AED"), argb("F8F5FF"), .35f), scrollbar = argb("E2D8FA"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    gradientPrimary = listOf(argb("7C3AED"), argb("3B82F6"), argb("F43F5E")),
    gradientSecondary = listOf(argb("F8F5FF"), argb("FFFFFF")),
    gradientBorder = listOf(argb("E2D8FA"), argb("F43F5E"), argb("7C3AED")),
    signature = "two pastel blobs flow together into violet",
    backgroundMotion = "light ambient: two pastel blobs flow together into violet",
    transitionMotion = "Light transition: two pastel blobs flow together into violet",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "light shimmer and gradient progress with moving highlight",
    notificationMotion = "light toast with 7c3aed accent and brief signature pulse"
)
