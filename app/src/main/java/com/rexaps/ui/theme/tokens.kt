package com.rexaps.ui.theme

/** Complete design-token contract shared by every RexAps theme. Colors are ARGB longs. */
data class RexDesignTokens(
    val id: String, val name: String, val category: String,
    val primary: Long, val primaryHover: Long, val primaryPressed: Long, val onPrimary: Long,
    val secondary: Long, val accent: Long, val background: Long, val backgroundAlt: Long,
    val surface: Long, val surfaceElevated: Long, val surfaceHover: Long,
    val text: Long, val textMuted: Long, val textDisabled: Long, val border: Long,
    val borderStrong: Long, val glow: Long, val overlay: Long,
    val success: Long, val warning: Long, val error: Long, val info: Long,
    val selection: Long, val scrollbar: Long,
    val radiusXs: Int = 4, val radiusSm: Int = 8, val radiusMd: Int = 12,
    val radiusLg: Int = 16, val radiusXl: Int = 24, val radiusPill: Int = 999,
    val blurNone: Int = 0, val blurSm: Int = 8, val blurMd: Int = 16,
    val blurLg: Int = 24, val blurXl: Int = 40,
    val shadowSm: Float = 2f, val shadowMd: Float = 6f, val shadowLg: Float = 14f,
    val shadowGlow: Float = 18f, val borderHairline: Float = .5f,
    val borderThin: Float = 1f, val borderThick: Float = 2f,
    val opacitySurface: Float = .94f, val noiseIntensity: Float = .02f,
    val gradientPrimary: List<Long>, val gradientSecondary: List<Long>, val gradientBorder: List<Long>,
    val fontFamilyUI: String = "sans-serif", val fontFamilyMono: String = "monospace",
    val fontDisplay: Int = 32, val fontTitle: Int = 22, val fontHeading: Int = 18,
    val fontBody: Int = 14, val fontCaption: Int = 12, val fontCode: Int = 13,
    val weightRegular: Int = 400, val weightMedium: Int = 500, val weightSemibold: Int = 600,
    val weightBold: Int = 700, val lineHeightTight: Float = 1.25f,
    val lineHeightNormal: Float = 1.5f, val lineHeightLoose: Float = 1.7f,
    val spacing: List<Int> = listOf(4,8,12,16,20,24,32,40,48,64),
    val motionInstant: Int = 80, val motionFast: Int = 150, val motionNormal: Int = 250,
    val motionSlow: Int = 400, val motionSlower: Int = 600, val motionAmbient: Int = 16000,
    val easingStandard: String = "cubic-bezier(0.2, 0.0, 0, 1)",
    val easingEnter: String = "cubic-bezier(0.0, 0.0, 0.2, 1)",
    val easingExit: String = "cubic-bezier(0.4, 0.0, 1, 1)",
    val signature: String, val backgroundMotion: String, val transitionMotion: String,
    val interactionMotion: String, val loadingMotion: String, val notificationMotion: String
) {
    fun validate(): List<String> = buildList {
        if (id.isBlank() || name.isBlank() || category.isBlank()) add("identity")
        val colors = listOf(primary,primaryHover,primaryPressed,onPrimary,secondary,accent,background,backgroundAlt,
            surface,surfaceElevated,surfaceHover,text,textMuted,textDisabled,border,borderStrong,glow,overlay,
            success,warning,error,info,selection,scrollbar)
        if (colors.any { it == 0L }) add("color token")
        if (gradientPrimary.isEmpty() || gradientSecondary.isEmpty() || gradientBorder.isEmpty()) add("gradient")
        if (noiseIntensity !in 0f..0.08f) add("noiseIntensity")
        if (spacing != listOf(4,8,12,16,20,24,32,40,48,64)) add("spacing scale")
        if (motionInstant <= 0 || motionNormal < motionFast || motionAmbient < 8000) add("motion")
        if (signature.isBlank() || backgroundMotion.isBlank() || transitionMotion.isBlank()) add("motion signature")
    }
}

internal fun argb(hex: String): Long = (0xFF000000L or hex.removePrefix("#").toLong(16)) and 0xFFFFFFFFL
internal fun blend(color: Long, toward: Long, amount: Float): Long {
    fun ch(c: Long, shift: Int) = ((c shr shift) and 255).toFloat()
    var out = 0xFF000000L
    for (shift in listOf(16,8,0)) {
        val v = (ch(color,shift) * (1f-amount) + ch(toward,shift)*amount).toInt().coerceIn(0,255)
        out = out or (v.toLong() shl shift)
    }
    return out
}
