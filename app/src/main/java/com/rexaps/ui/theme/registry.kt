package com.rexaps.ui.theme

import com.rexaps.ui.theme.themes.glass.glass_blue_moon

/** Ordered source of truth. Add one theme file and one entry here to register a theme. */
object RexThemeRegistry {
    val all: List<RexDesignTokens> = listOf(
        glass_blue_moon
    )
    val default: RexDesignTokens = all.first()
    private val byId = all.associateBy { it.id }
    fun get(id: String): RexDesignTokens = byId[id] ?: default
    fun byCategory(): Map<String, List<RexDesignTokens>> = all.groupBy { it.category }
    fun validateAll(): Map<String, List<String>> = all.associate { it.id to it.validate() }
    fun contrastRatio(foreground: Long, background: Long): Double {
        fun channel(value: Long, shift: Int): Double {
            val c = ((value shr shift) and 255L) / 255.0
            return if (c <= .04045) c / 12.92 else Math.pow((c + .055) / 1.055, 2.4)
        }
        val l = .2126 * channel(foreground,16) + .7152 * channel(foreground,8) + .0722 * channel(foreground,0)
        val r = .2126 * channel(background,16) + .7152 * channel(background,8) + .0722 * channel(background,0)
        return (maxOf(l,r)+.05)/(minOf(l,r)+.05)
    }
    fun contrastFailures(): Map<String, Double> = all.mapNotNull { theme ->
        val ratio = contrastRatio(theme.text, theme.background)
        if (ratio < 4.5) theme.id to ratio else null
    }.toMap()
}
