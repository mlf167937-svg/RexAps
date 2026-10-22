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
    radiusXs = 5, radiusSm = 9, radiusMd = 13, radiusLg = 18, radiusXl = 24,
    blurSm = 7, blurMd = 14, blurLg = 21,
    shadowSm = 2f, shadowMd = 7f, shadowLg = 17f, shadowGlow = 25f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif-medium", fontFamilyMono = "monospace",
    fontDisplay = 33, fontTitle = 23, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.2f, lineHeightNormal = 1.48f, lineHeightLoose = 1.66f,
    motionInstant = 50, motionFast = 110, motionNormal = 190, motionSlow = 290, motionSlower = 430, motionAmbient = 12500,
    easingStandard = "cubic-bezier(0.12,0.80,0.20,1.00)", easingEnter = "cubic-bezier(0.12,0.80,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("FFD400"), argb("FFA500"), argb("6EC5FF")),
    gradientSecondary = listOf(argb("0A0A12"), argb("14141F")),
    gradientBorder = listOf(argb("FFD400"), argb("6EC5FF"), argb("FFD400")),
    signature = "fine electric arcs link related controls",
    backgroundMotion = "elemental ambient: fine electric arcs link related controls",
    transitionMotion = "Elemental transition: fine electric arcs link related controls; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "golden charge traces the focus edge, hover snaps into place, and tap emits one short electric pulse",
    loadingMotion = "Elemental loading: theme-colored energy sweep, directional progress motion, and a restrained particle pulse",
    notificationMotion = "elemental notification in #6ec5ff: quick fade-slide, soft surface bloom, then a signature pulse"
)
