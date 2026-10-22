package com.rexaps.ui.theme.themes.elemental

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Frost Ice Blue Moon: frost crystals sparkle along panel edges. */
val frost_ice_blue_moon = RexDesignTokens(
    id = "frost_ice_blue_moon", name = "Frost Ice Blue Moon", category = "Elemental",
    primary = argb("7DD3FC"), primaryHover = blend(argb("7DD3FC"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("7DD3FC"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("38BDF8"), accent = argb("E0F7FF"),
    background = argb("04121C"), backgroundAlt = argb("102632"),
    surface = argb("FF102632"), surfaceElevated = argb("FF1E333E"), surfaceHover = argb("FF283C46"),
    text = argb("EAFBFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("7DD3FC"), borderStrong = blend(argb("7DD3FC"), argb("E0F7FF"), .45f),
    glow = argb("7DD3FC"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("7DD3FC"), argb("04121C"), .35f), scrollbar = argb("7DD3FC"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    radiusXs = 8, radiusSm = 12, radiusMd = 17, radiusLg = 22, radiusXl = 29,
    blurSm = 7, blurMd = 14, blurLg = 21,
    shadowSm = 2f, shadowMd = 6f, shadowLg = 14f, shadowGlow = 18f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-light", fontFamilyMono = "monospace",
    fontDisplay = 34, fontTitle = 23, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 300, weightMedium = 400, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.22f, lineHeightNormal = 1.5f, lineHeightLoose = 1.7f,
    motionInstant = 72, motionFast = 145, motionNormal = 255, motionSlow = 385, motionSlower = 565, motionAmbient = 17500,
    easingStandard = "cubic-bezier(0.20,0.00,0.00,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("7DD3FC"), argb("38BDF8"), argb("E0F7FF")),
    gradientSecondary = listOf(argb("04121C"), argb("102632")),
    gradientBorder = listOf(argb("7DD3FC"), argb("E0F7FF"), argb("7DD3FC")),
    signature = "frost crystals sparkle along panel edges",
    backgroundMotion = "elemental ambient: frost crystals sparkle along panel edges",
    transitionMotion = "Elemental transition: frost crystals sparkle along panel edges; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "icy edge catches focus, crisp hover glide, and a clean glassy press with a quick return",
    loadingMotion = "Elemental loading: theme-colored energy sweep, directional progress motion, and a restrained particle pulse",
    notificationMotion = "elemental notification in #e0f7ff: quick fade-slide, soft surface bloom, then a signature pulse"
)
