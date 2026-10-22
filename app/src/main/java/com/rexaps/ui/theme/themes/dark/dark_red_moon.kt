package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Red Moon: main emblem has a restrained slow heartbeat pulse. */
val dark_red_moon = RexDesignTokens(
    id = "dark_red_moon", name = "Dark Red Moon", category = "Dark",
    primary = argb("EF4444"), primaryHover = blend(argb("EF4444"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("EF4444"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("F87171"), accent = argb("FCA5A5"),
    background = argb("0F0506"), backgroundAlt = argb("1A0A0C"),
    surface = argb("FF1A0A0C"), surfaceElevated = argb("FF28191B"), surfaceHover = argb("FF312224"),
    text = argb("FBEAEA"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("3A1518"), borderStrong = blend(argb("3A1518"), argb("FCA5A5"), .45f),
    glow = argb("EF4444"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("EF4444"), argb("0F0506"), .35f), scrollbar = argb("3A1518"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 6, radiusSm = 10, radiusMd = 14, radiusLg = 19, radiusXl = 25,
    blurSm = 7, blurMd = 14, blurLg = 22,
    shadowSm = 2f, shadowMd = 7f, shadowLg = 16f, shadowGlow = 20f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif-condensed", fontFamilyMono = "monospace",
    fontDisplay = 32, fontTitle = 22, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.22f, lineHeightNormal = 1.48f, lineHeightLoose = 1.65f,
    motionInstant = 64, motionFast = 135, motionNormal = 230, motionSlow = 360, motionSlower = 520, motionAmbient = 15000,
    easingStandard = "cubic-bezier(0.22,1.00,0.36,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("EF4444"), argb("F87171"), argb("FCA5A5")),
    gradientSecondary = listOf(argb("0F0506"), argb("1A0A0C")),
    gradientBorder = listOf(argb("3A1518"), argb("FCA5A5"), argb("EF4444")),
    signature = "main emblem has a restrained slow heartbeat pulse",
    backgroundMotion = "dark ambient: main emblem has a restrained slow heartbeat pulse",
    transitionMotion = "Dark transition: main emblem has a restrained slow heartbeat pulse; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "ember-red ripple, subtle 1.5dp hover rise, and a short pulse on successful actions",
    loadingMotion = "Dark loading: low-contrast metallic sweep over a breathing skeleton, with a short accent-color progress fill",
    notificationMotion = "dark notification in #fca5a5: quick fade-slide, soft surface bloom, then a signature pulse"
)
