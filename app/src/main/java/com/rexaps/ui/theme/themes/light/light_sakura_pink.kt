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
    radiusXs = 16, radiusSm = 20, radiusMd = 25, radiusLg = 31, radiusXl = 40,
    blurSm = 9, blurMd = 17, blurLg = 25,
    shadowSm = 2f, shadowMd = 7f, shadowLg = 16f, shadowGlow = 14f,
    borderHairline = 0.4f, borderThin = 0.9f, borderThick = 1.5f,
    fontFamilyUI = "serif", fontFamilyMono = "monospace",
    fontDisplay = 36, fontTitle = 25, fontHeading = 20, fontBody = 15, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.3f, lineHeightNormal = 1.58f, lineHeightLoose = 1.8f,
    motionInstant = 90, motionFast = 180, motionNormal = 310, motionSlow = 465, motionSlower = 680, motionAmbient = 21000,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("B9366D"), argb("F5A3C0"), argb("FFD1E1")),
    gradientSecondary = listOf(argb("FFF7FA"), argb("FFFFFF")),
    gradientBorder = listOf(argb("F8D7E3"), argb("FFD1E1"), argb("B9366D")),
    signature = "a gentle sakura petal drift follows successful actions",
    backgroundMotion = "light ambient: a gentle sakura petal drift follows successful actions",
    transitionMotion = "Light transition: a gentle sakura petal drift follows successful actions; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "petal-like ripple, pillowy hover lift, and a delicate pink focus bloom",
    loadingMotion = "Light loading: soft pearl shimmer, staggered content fade-in, and a calm gradient progress line",
    notificationMotion = "light notification in #ffd1e1: quick fade-slide, soft surface bloom, then a signature pulse"
)
