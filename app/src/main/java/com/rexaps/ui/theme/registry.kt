package com.rexaps.ui.theme

import com.rexaps.ui.theme.themes.dark.dark_blue_moon
import com.rexaps.ui.theme.themes.glass.glass_blue_moon
import com.rexaps.ui.theme.themes.glass.glass_red_moon
import com.rexaps.ui.theme.themes.glass.glass_purple_moon
import com.rexaps.ui.theme.themes.glass.glass_hollow_purple
import com.rexaps.ui.theme.themes.glass.glass_emerald_moon
import com.rexaps.ui.theme.themes.glass.glass_gold_moon
import com.rexaps.ui.theme.themes.dark.dark_red_moon
import com.rexaps.ui.theme.themes.dark.dark_purple_moon
import com.rexaps.ui.theme.themes.dark.dark_hollow_purple
import com.rexaps.ui.theme.themes.dark.dark_onyx_moon
import com.rexaps.ui.theme.themes.dark.dark_midnight_silver
import com.rexaps.ui.theme.themes.light.light_blue_moon
import com.rexaps.ui.theme.themes.light.light_red_moon
import com.rexaps.ui.theme.themes.light.light_purple_moon
import com.rexaps.ui.theme.themes.light.light_hollow_purple
import com.rexaps.ui.theme.themes.light.light_pearl_white
import com.rexaps.ui.theme.themes.light.light_sakura_pink
import com.rexaps.ui.theme.themes.neon.neon_cyan_moon
import com.rexaps.ui.theme.themes.neon.neon_green_moon
import com.rexaps.ui.theme.themes.neon.neon_pink_moon
import com.rexaps.ui.theme.themes.neon.neon_yellow_moon
import com.rexaps.ui.theme.themes.cosmic.astral_galaxy_moon
import com.rexaps.ui.theme.themes.cosmic.nebula_void_moon
import com.rexaps.ui.theme.themes.cosmic.eclipse_black_moon
import com.rexaps.ui.theme.themes.cosmic.starlight_silver_moon
import com.rexaps.ui.theme.themes.elemental.frost_ice_blue_moon
import com.rexaps.ui.theme.themes.elemental.inferno_magma_red_moon
import com.rexaps.ui.theme.themes.elemental.thunder_gold_moon
import com.rexaps.ui.theme.themes.elemental.abyssal_ocean_moon

/**
 * The single source of truth for all 30 selectable themes.
 * Dark Blue Moon is intentionally first so first-run defaults match RexAps UI.
 */
object RexThemeRegistry {
    val all: List<RexDesignTokens> = listOf(
        dark_blue_moon,
        dark_red_moon,
        dark_purple_moon,
        dark_hollow_purple,
        dark_onyx_moon,
        dark_midnight_silver,
        glass_blue_moon,
        glass_red_moon,
        glass_purple_moon,
        glass_hollow_purple,
        glass_emerald_moon,
        glass_gold_moon,
        light_blue_moon,
        light_red_moon,
        light_purple_moon,
        light_hollow_purple,
        light_pearl_white,
        light_sakura_pink,
        neon_cyan_moon,
        neon_green_moon,
        neon_pink_moon,
        neon_yellow_moon,
        astral_galaxy_moon,
        nebula_void_moon,
        eclipse_black_moon,
        starlight_silver_moon,
        frost_ice_blue_moon,
        inferno_magma_red_moon,
        thunder_gold_moon,
        abyssal_ocean_moon
    )
    val default: RexDesignTokens = all.first()
    private val byId = all.associateBy { it.id }

    fun get(id: String): RexDesignTokens = byId[id] ?: default
    fun byCategory(): Map<String, List<RexDesignTokens>> = all.groupBy { it.category }
    fun validateAll(): Map<String, List<String>> = all.associate { it.id to it.validate() }
    /** Empty means every registered theme has an explicit material/motif specification. */
    fun validateVisualCoverage(): Set<String> = RexThemeVisuals.coverage(all.map { it.id }.toSet())

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
        val bodyRatio = contrastRatio(theme.text, theme.background)
        val actionRatio = contrastRatio(theme.onPrimary, theme.primary)
        val accentRatio = contrastRatio(theme.primary, theme.background)
        val weakest = minOf(bodyRatio, actionRatio, accentRatio)
        if (bodyRatio < 4.5 || actionRatio < 4.5 || accentRatio < 4.5) theme.id to weakest else null
    }.toMap()
}
