package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Blue Moon: moonlight reflection sweeps across cards. */
val glass_blue_moon = RexDesignTokens(
    id = "glass_blue_moon", name = "Glass Blue Moon", category = "Glass",
    primary = argb("4F8CFF"), primaryHover = blend(argb("4F8CFF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("4F8CFF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("7AA7FF"), accent = argb("A5C8FF"),
    background = argb("0B1220"), backgroundAlt = argb("101C36"),
    surface = argb("FF101C36"), surfaceElevated = argb("FF1E2A42"), surfaceHover = argb("FF28334A"),
    text = argb("EAF2FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("A5C8FF"), borderStrong = blend(argb("A5C8FF"), argb("A5C8FF"), .45f),
    glow = argb("4F8CFF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("4F8CFF"), argb("0B1220"), .35f), scrollbar = argb("A5C8FF"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    radiusXs = 13, radiusSm = 17, radiusMd = 22, radiusLg = 28, radiusXl = 36,
    blurSm = 12, blurMd = 22, blurLg = 32,
    shadowSm = 3f, shadowMd = 9f, shadowLg = 20f, shadowGlow = 22f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-light", fontFamilyMono = "monospace",
    fontDisplay = 35, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 300, weightMedium = 400, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.24f, lineHeightNormal = 1.54f, lineHeightLoose = 1.75f,
    motionInstant = 85, motionFast = 170, motionNormal = 300, motionSlow = 450, motionSlower = 660, motionAmbient = 19000,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("4F8CFF"), argb("7AA7FF"), argb("A5C8FF")),
    gradientSecondary = listOf(argb("0B1220"), argb("101C36")),
    gradientBorder = listOf(argb("A5C8FF"), argb("A5C8FF"), argb("4F8CFF")),
    signature = "moonlight reflection sweeps across cards",
    backgroundMotion = "glass ambient: moonlight reflection sweeps across cards",
    transitionMotion = "Glass transition: moonlight reflection sweeps across cards; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "gloss highlight sweeps across the surface, floating hover lift, and a soft blue refraction ripple",
    loadingMotion = "Glass loading: moving specular highlight across a translucent-looking skeleton and a layered gradient progress track",
    notificationMotion = "glass notification in #a5c8ff: quick fade-slide, soft surface bloom, then a signature pulse"
)
