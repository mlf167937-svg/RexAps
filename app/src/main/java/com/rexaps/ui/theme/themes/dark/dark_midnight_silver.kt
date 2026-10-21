package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Midnight Silver: brushed-metal reflection follows scrolling. */
val dark_midnight_silver = RexDesignTokens(
    id = "dark_midnight_silver", name = "Dark Midnight Silver", category = "Dark",
    primary = argb("C0C8D8"), primaryHover = blend(argb("C0C8D8"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("C0C8D8"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("8F9BB3"), accent = argb("E3E8F2"),
    background = argb("070A10"), backgroundAlt = argb("111622"),
    surface = argb("FF111622"), surfaceElevated = argb("FF1F242F"), surfaceHover = argb("FF292D38"),
    text = argb("EDF1F8"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("232B3D"), borderStrong = blend(argb("232B3D"), argb("E3E8F2"), .45f),
    glow = argb("C0C8D8"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("C0C8D8"), argb("070A10"), .35f), scrollbar = argb("232B3D"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("C0C8D8"), argb("8F9BB3"), argb("E3E8F2")),
    gradientSecondary = listOf(argb("070A10"), argb("111622")),
    gradientBorder = listOf(argb("232B3D"), argb("E3E8F2"), argb("C0C8D8")),
    signature = "brushed-metal reflection follows scrolling",
    backgroundMotion = "dark ambient: brushed-metal reflection follows scrolling",
    transitionMotion = "Dark transition: brushed-metal reflection follows scrolling",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "dark shimmer and gradient progress with moving highlight",
    notificationMotion = "dark toast with c0c8d8 accent and brief signature pulse"
)
