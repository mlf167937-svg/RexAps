package com.rexaps.ui.theme.themes.elemental

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Thunder Gold Moon: fine electric arcs link related controls. */
val thunder_gold_moon = RexDesignTokens(
    id = "thunder_gold_moon", name = "Thunder Gold Moon", category = "Elemental",
    primary = argb("FFD400"), primaryHover = blend(argb("FFD400"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FFD400"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FFA500"), accent = argb("6EC5FF"),
    background = argb("0A0A12"), backgroundAlt = argb("14141F"),
    surface = argb("FF14141F"), surfaceElevated = argb("FF22222C"), surfaceHover = argb("FF2C2C35"),
    text = argb("FFF8D6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFD400"), borderStrong = blend(argb("FFD400"), argb("6EC5FF"), .45f),
    glow = argb("FFD400"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FFD400"), argb("0A0A12"), .35f), scrollbar = argb("FFD400"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("FFD400"), argb("FFA500"), argb("6EC5FF")),
    gradientSecondary = listOf(argb("0A0A12"), argb("14141F")),
    gradientBorder = listOf(argb("FFD400"), argb("6EC5FF"), argb("FFD400")),
    signature = "fine electric arcs link related controls",
    backgroundMotion = "elemental ambient: fine electric arcs link related controls",
    transitionMotion = "Elemental transition: fine electric arcs link related controls",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "elemental shimmer and gradient progress with moving highlight",
    notificationMotion = "elemental toast with ffd400 accent and brief signature pulse"
)
