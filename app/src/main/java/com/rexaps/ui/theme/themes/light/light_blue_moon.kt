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
    radiusXs = 9, radiusSm = 13, radiusMd = 18, radiusLg = 23, radiusXl = 30,
    blurSm = 7, blurMd = 14, blurLg = 20,
    shadowSm = 2f, shadowMd = 6f, shadowLg = 14f, shadowGlow = 10f,
    borderHairline = 0.4f, borderThin = 0.9f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif", fontFamilyMono = "monospace",
    fontDisplay = 33, fontTitle = 23, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.22f, lineHeightNormal = 1.5f, lineHeightLoose = 1.68f,
    motionInstant = 72, motionFast = 145, motionNormal = 250, motionSlow = 380, motionSlower = 560, motionAmbient = 16000,
    easingStandard = "cubic-bezier(0.20,0.00,0.00,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("2563EB"), argb("60A5FA"), argb("BFDBFE")),
    gradientSecondary = listOf(argb("F5F9FF"), argb("FFFFFF")),
    gradientBorder = listOf(argb("D6E4FA"), argb("BFDBFE"), argb("2563EB")),
    signature = "soft blue light bubbles rise in the background",
    backgroundMotion = "light ambient: soft blue light bubbles rise in the background",
    transitionMotion = "Light transition: soft blue light bubbles rise in the background; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "clear blue focus outline, airy hover lift, and a clean ripple with quick tactile return",
    loadingMotion = "Light loading: soft pearl shimmer, staggered content fade-in, and a calm gradient progress line",
    notificationMotion = "light notification in #bfdbfe: quick fade-slide, soft surface bloom, then a signature pulse"
)
