package com.rexaps.ui.theme.themes.cosmic

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Nebula Void Moon: miniature black hole bends nearby light. */
val nebula_void_moon = RexDesignTokens(
    id = "nebula_void_moon", name = "Nebula Void Moon", category = "Cosmic",
    primary = argb("B24BF3"), primaryHover = blend(argb("B24BF3"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("B24BF3"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("FF3D8B"), accent = argb("5EEAD4"),
    background = argb("060211"), backgroundAlt = argb("180A2C"),
    surface = argb("FF180A2C"), surfaceElevated = argb("FF261939"), surfaceHover = argb("FF2F2241"),
    text = argb("F2E9FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("B24BF3"), borderStrong = blend(argb("B24BF3"), argb("5EEAD4"), .45f),
    glow = argb("B24BF3"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("B24BF3"), argb("060211"), .35f), scrollbar = argb("B24BF3"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("B24BF3"), argb("FF3D8B"), argb("5EEAD4")),
    gradientSecondary = listOf(argb("060211"), argb("180A2C")),
    gradientBorder = listOf(argb("B24BF3"), argb("5EEAD4"), argb("B24BF3")),
    signature = "miniature black hole bends nearby light",
    backgroundMotion = "cosmic ambient: miniature black hole bends nearby light",
    transitionMotion = "Cosmic transition: miniature black hole bends nearby light",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "cosmic shimmer and gradient progress with moving highlight",
    notificationMotion = "cosmic toast with b24bf3 accent and brief signature pulse"
)
