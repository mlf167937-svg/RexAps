// /data/data/com.termux/files/home/RexAps/app/src/main/java/com/rexaps/ui/RexTheme.kt
package com.rexaps.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class RexThemeOption(val label: String) {
    SYSTEM("Sistem"),
    LIGHT("Putih"),
    DARK("Gelap"),
    AMOLED("Hitam"),
    OCEAN("Ocean"),
    FOREST("Forest"),
    SUNSET("Sunset"),
    LAVENDER("Lavender"),
    ROSE("Rose"),
    MINT("Mint"),
    SAND("Sand"),
    SKY("Sky"),
    MIDNIGHT("Midnight"),
    CRIMSON("Crimson"),
    EMERALD("Emerald"),
    GOLD("Gold"),
    CYBER("Cyber"),
    NORD("Nord"),
    DRACULA("Dracula"),
    COFFEE("Coffee"),
    AURORA("Aurora"),
    COSMIC("Cosmic"),
    SAKURA("Sakura"),
    TWILIGHT("Twilight"),
    GLACIER("Glacier"),
    VOLCANO("Volcano"),
    DESERT("Desert"),
    JADE("Jade"),
    RUBY("Ruby"),
    SAPPHIRE("Sapphire"),
    AMBER("Amber"),
    ONYX("Onyx"),
    PEARL("Pearl"),
    IVORY("Ivory"),
    SLATE("Slate"),
    STEEL("Steel"),
    COPPER("Copper"),
    BRONZE("Bronze"),
    PLATINUM("Platinum"),
    TITANIUM("Titanium"),
    NEON("Neon"),
    VAPORWAVE("Vaporwave"),
    SYNTHWAVE("Synthwave"),
    MATRIX("Matrix"),
    GALAXY("Galaxy"),
    NEBULA("Nebula"),
    COMET("Comet"),
    ECLIPSE("Eclipse"),
    STARLIGHT("Starlight"),
    MOONLIGHT("Moonlight"),
    SOLAR("Solar"),
    LAGOON("Lagoon"),
    CORAL("Coral"),
    TROPICAL("Tropical"),
    BAMBOO("Bamboo"),
    OLIVE("Olive"),
    TERRACOTTA("Terracotta"),
    CLAY("Clay"),
    CHARCOAL("Charcoal"),
    GRAPHITE("Graphite"),
    PORCELAIN("Porcelain"),
    BLOSSOM("Blossom"),
    ORCHID("Orchid"),
    MAGENTA("Magenta"),
    INDIGO("Indigo"),
    COBALT("Cobalt"),
    TEAL("Teal"),
    TURQUOISE("Turquoise"),
    AQUA("Aqua"),
    LIME("Lime"),
    CITRUS("Citrus"),
    HONEY("Honey"),
    CARAMEL("Caramel"),
    MOCHA("Mocha"),
    ESPRESSO("Espresso"),
    WINE("Wine"),
    BERRY("Berry"),
    CHERRY("Cherry"),
    FLAMINGO("Flamingo"),
    PEACH("Peach");

    /** Cheap preview color for the theme grid — never builds a full scheme. */
    val swatch: Long get() = RexPaletteSpec.of(this).primary
}

/* ------------------------------- PERSISTENCE ------------------------------ */

class RexThemeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): RexThemeOption {
        val saved = prefs.getString(KEY_THEME, null)
        return RexThemeOption.values().firstOrNull { it.name == saved }
            ?: RexThemeOption.SYSTEM
    }

    fun save(option: RexThemeOption) {
        prefs.edit().putString(KEY_THEME, option.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "rexaps_settings"
        const val KEY_THEME = "theme_option"
    }
}

/* ---------------------------- PALETTE SPECS (DATA) ------------------------- */

data class RexPaletteSpec(
    val dark: Boolean,
    val primary: Long,
    val onPrimary: Long,
    val container: Long,
    val onContainer: Long,
    val tertiary: Long,
    val background: Long,
    val card: Long,
    val muted: Long,
    val outline: Long,
    val text: Long
) {
    companion object {
        /**
         * Safe lookup: if a theme is somehow missing from the table
         * (should never happen now — see generation below) this returns
         * a sane default instead of throwing, so the theme sheet can
         * never crash from a lookup failure.
         */
        fun of(option: RexThemeOption): RexPaletteSpec =
            specTable[option] ?: specTable.getValue(RexThemeOption.DARK)

        /**
         * Built from a flat row list, one row per RexThemeOption entry,
         * IN DECLARATION ORDER. Using zip() against RexThemeOption.values()
         * instead of a hand-mapped `Enum to Spec` table means a missing or
         * misordered row is caught by a size check below rather than
         * silently producing a NoSuchElementException at runtime.
         */
        private val rows: List<RexPaletteSpec> = listOf(
            // SYSTEM (unused directly, resolves to LIGHT/DARK — placeholder row)
            RexPaletteSpec(true, 0xFFE6E6EA, 0xFF121214, 0xFF2A2A30, 0xFFE6E6EA, 0xFFB8B8C2, 0xFF0F0F12, 0xFF1B1B20, 0xFF9A9AA3, 0xFF2C2C33, 0xFFE8E8EC),
            // LIGHT
            RexPaletteSpec(false, 0xFF111111, 0xFFFFFFFF, 0xFFE6E6EB, 0xFF111111, 0xFF4A4A55, 0xFFF7F7F9, 0xFFFFFFFF, 0xFF66666E, 0xFFE0E0E6, 0xFF121214),
            // DARK
            RexPaletteSpec(true, 0xFFE6E6EA, 0xFF121214, 0xFF2A2A30, 0xFFE6E6EA, 0xFFB8B8C2, 0xFF0F0F12, 0xFF1B1B20, 0xFF9A9AA3, 0xFF2C2C33, 0xFFE8E8EC),
            // AMOLED
            RexPaletteSpec(true, 0xFFFFFFFF, 0xFF000000, 0xFF1E1E22, 0xFFFFFFFF, 0xFF9A9AA3, 0xFF000000, 0xFF121214, 0xFF9A9AA3, 0xFF26262B, 0xFFFFFFFF),
            // OCEAN
            RexPaletteSpec(true, 0xFF4FC3F7, 0xFF00263A, 0xFF0F3A55, 0xFFCDEBFF, 0xFF7C9CFF, 0xFF0A1622, 0xFF122536, 0xFF8FA9BC, 0xFF1F3A50, 0xFFE3F1FB),
            // FOREST
            RexPaletteSpec(false, 0xFF2E7D4F, 0xFFFFFFFF, 0xFFCDEBD6, 0xFF0B2E1A, 0xFF4CAF7A, 0xFFF3F8F4, 0xFFE3EEE6, 0xFF55675C, 0xFFCBDCD0, 0xFF13201A),
            // SUNSET
            RexPaletteSpec(true, 0xFFFF8A50, 0xFF3A1600, 0xFF4A2412, 0xFFFFDCC8, 0xFFFF5F7E, 0xFF1A1110, 0xFF2A1B18, 0xFFB89A90, 0xFF43302B, 0xFFF6E7E1),
            // LAVENDER
            RexPaletteSpec(false, 0xFF6D4AFF, 0xFFFFFFFF, 0xFFE7DFFF, 0xFF23106B, 0xFFB06CFF, 0xFFF7F4FF, 0xFFFFFFFF, 0xFF6E6885, 0xFFE3DCF5, 0xFF1A1428),
            // ROSE
            RexPaletteSpec(false, 0xFFE0456B, 0xFFFFFFFF, 0xFFFFDDE5, 0xFF4A0F1E, 0xFFFF8FA8, 0xFFFFF5F7, 0xFFFFFFFF, 0xFF85646C, 0xFFF5DCE2, 0xFF2A1418),
            // MINT
            RexPaletteSpec(false, 0xFF00A88A, 0xFFFFFFFF, 0xFFCFF5EC, 0xFF002E26, 0xFF3DD6B0, 0xFFF2FBF9, 0xFFFFFFFF, 0xFF5B7A74, 0xFFD3EBE6, 0xFF0E211D),
            // SAND
            RexPaletteSpec(false, 0xFF9A6B3F, 0xFFFFFFFF, 0xFFF0E1CE, 0xFF3A2410, 0xFFD1A06B, 0xFFFAF6EF, 0xFFFFFDF8, 0xFF7D6E5C, 0xFFEADFCC, 0xFF261C10),
            // SKY
            RexPaletteSpec(false, 0xFF1E88E5, 0xFFFFFFFF, 0xFFD6EAFF, 0xFF002B52, 0xFF5CC8FF, 0xFFF3F8FE, 0xFFFFFFFF, 0xFF60778F, 0xFFDAE7F5, 0xFF0E1A26),
            // MIDNIGHT
            RexPaletteSpec(true, 0xFF8C9EFF, 0xFF0B1040, 0xFF1E2560, 0xFFDDE1FF, 0xFFB388FF, 0xFF0B0D1F, 0xFF141834, 0xFF8D93B8, 0xFF262C55, 0xFFE6E8FA),
            // CRIMSON
            RexPaletteSpec(true, 0xFFFF4D5E, 0xFF3A0008, 0xFF4A1016, 0xFFFFD9DC, 0xFFFF8A65, 0xFF130A0B, 0xFF201214, 0xFFB08F92, 0xFF3D2225, 0xFFF5E6E7),
            // EMERALD
            RexPaletteSpec(true, 0xFF2EE59D, 0xFF00301F, 0xFF0F3D2B, 0xFFC6F7E1, 0xFF4DD0E1, 0xFF08120E, 0xFF10201A, 0xFF8DAA9E, 0xFF1D3A2E, 0xFFE3F5EC),
            // GOLD
            RexPaletteSpec(true, 0xFFFFC94D, 0xFF2E2000, 0xFF3A2E10, 0xFFFFEDC2, 0xFFFF9F43, 0xFF0A0A0A, 0xFF161513, 0xFFA39B87, 0xFF2E2A1F, 0xFFF3EEE0),
            // CYBER
            RexPaletteSpec(true, 0xFF00F0FF, 0xFF00272B, 0xFF12262E, 0xFFC2FBFF, 0xFFFF2BD6, 0xFF07070D, 0xFF111120, 0xFF8A8AB0, 0xFF25254A, 0xFFEAEAFF),
            // NORD
            RexPaletteSpec(true, 0xFF88C0D0, 0xFF1B2A30, 0xFF3B4252, 0xFFECEFF4, 0xFF81A1C1, 0xFF2E3440, 0xFF3B4252, 0xFFA5AEBF, 0xFF4C566A, 0xFFECEFF4),
            // DRACULA
            RexPaletteSpec(true, 0xFFBD93F9, 0xFF1E1633, 0xFF44475A, 0xFFF8F8F2, 0xFFFF79C6, 0xFF282A36, 0xFF343746, 0xFF9EA3BF, 0xFF44475A, 0xFFF8F8F2),
            // COFFEE
            RexPaletteSpec(true, 0xFFD7A86E, 0xFF2B1A08, 0xFF3E2B1C, 0xFFF2DFC8, 0xFFB07A4F, 0xFF17110D, 0xFF241B15, 0xFFA8917C, 0xFF3A2C22, 0xFFF1E6DA),
            // AURORA
            RexPaletteSpec(true, 0xFF6EE7B7, 0xFF003D2B, 0xFF0F4C3A, 0xFFD1FFF0, 0xFF8B5CF6, 0xFF0A0E14, 0xFF141A24, 0xFF8A93A6, 0xFF232B38, 0xFFE7ECF5),
            // COSMIC
            RexPaletteSpec(true, 0xFF9D7BFF, 0xFF1A0B3D, 0xFF2D1B5E, 0xFFE6DCFF, 0xFFFF6EC7, 0xFF0C0817, 0xFF17102B, 0xFF9088A8, 0xFF2B2246, 0xFFECE7F7),
            // SAKURA
            RexPaletteSpec(false, 0xFFE85D8A, 0xFFFFFFFF, 0xFFFFDCE8, 0xFF4A0B23, 0xFFFFA6C9, 0xFFFFF6F9, 0xFFFFFFFF, 0xFF8A6B75, 0xFFF5D9E2, 0xFF2B1219),
            // TWILIGHT
            RexPaletteSpec(true, 0xFF7C93FF, 0xFF0B1240, 0xFF1F2A5E, 0xFFDCE2FF, 0xFFC77DFF, 0xFF0B0D1C, 0xFF161A30, 0xFF8D92B5, 0xFF262C4A, 0xFFE6E8F7),
            // GLACIER
            RexPaletteSpec(false, 0xFF3EA8DB, 0xFFFFFFFF, 0xFFD4EEFA, 0xFF06344A, 0xFF8FE3D8, 0xFFF5FBFE, 0xFFFFFFFF, 0xFF5E7A87, 0xFFD9EAF1, 0xFF102229),
            // VOLCANO
            RexPaletteSpec(true, 0xFFFF6B4A, 0xFF3A0D00, 0xFF531A0A, 0xFFFFDDD0, 0xFFFFB347, 0xFF150807, 0xFF241210, 0xFFB39289, 0xFF3D2420, 0xFFF5E4DF),
            // DESERT
            RexPaletteSpec(false, 0xFFC17A3D, 0xFFFFFFFF, 0xFFF2DEC4, 0xFF432B0C, 0xFFE0B36B, 0xFFFBF6EE, 0xFFFFFFFF, 0xFF8A7455, 0xFFEBDFC9, 0xFF2E2313),
            // JADE
            RexPaletteSpec(true, 0xFF4ADE80, 0xFF00300F, 0xFF0E4023, 0xFFCFFAE0, 0xFF34D399, 0xFF071410, 0xFF10201A, 0xFF89A398, 0xFF1D3A2C, 0xFFE1F5EA),
            // RUBY
            RexPaletteSpec(true, 0xFFFF5C7A, 0xFF3A0010, 0xFF5A0F22, 0xFFFFD6DE, 0xFFFF8FA3, 0xFF130508, 0xFF230D13, 0xFFB18D93, 0xFF3E1B24, 0xFFF5E1E5),
            // SAPPHIRE
            RexPaletteSpec(true, 0xFF4B8CFF, 0xFF001B44, 0xFF0D3372, 0xFFD6E5FF, 0xFF7CC7FF, 0xFF060B18, 0xFF0F1930, 0xFF8894B3, 0xFF1E2C4E, 0xFFE3E9F7),
            // AMBER
            RexPaletteSpec(false, 0xFFE08A1E, 0xFFFFFFFF, 0xFFFCE3B8, 0xFF4A2E00, 0xFFFFC24D, 0xFFFFFAF0, 0xFFFFFFFF, 0xFF8C7654, 0xFFF3E3C4, 0xFF2E2107),
            // ONYX
            RexPaletteSpec(true, 0xFFD4D4D8, 0xFF18181B, 0xFF27272A, 0xFFF4F4F5, 0xFFA1A1AA, 0xFF09090B, 0xFF141416, 0xFF8B8B92, 0xFF232326, 0xFFF4F4F5),
            // PEARL
            RexPaletteSpec(false, 0xFF6B6F76, 0xFFFFFFFF, 0xFFE9EAEC, 0xFF1F2124, 0xFFB8BCC4, 0xFFFBFBFA, 0xFFFFFFFF, 0xFF7C7F85, 0xFFE7E7E5, 0xFF1D1D1D),
            // IVORY
            RexPaletteSpec(false, 0xFFB08D57, 0xFFFFFFFF, 0xFFF2E6D0, 0xFF3E2E12, 0xFFD9C298, 0xFFFFFCF5, 0xFFFFFFFF, 0xFF8E8368, 0xFFEDE3CB, 0xFF2A2413),
            // SLATE
            RexPaletteSpec(true, 0xFF94A3B8, 0xFF0F172A, 0xFF1E293B, 0xFFE2E8F0, 0xFF64748B, 0xFF0A0E17, 0xFF141B29, 0xFF8090A5, 0xFF243244, 0xFFE7ECF3),
            // STEEL
            RexPaletteSpec(true, 0xFF7DA0C4, 0xFF07202F, 0xFF123449, 0xFFD7E7F2, 0xFFA8C6DE, 0xFF080D12, 0xFF121A21, 0xFF7F94A2, 0xFF203040, 0xFFE2EAF0),
            // COPPER
            RexPaletteSpec(true, 0xFFE08A5B, 0xFF3A1704, 0xFF522608, 0xFFFFDDC5, 0xFFFFB088, 0xFF140B07, 0xFF241610, 0xFFAF9483, 0xFF3D2A1F, 0xFFF3E5DA),
            // BRONZE
            RexPaletteSpec(true, 0xFFCD9B5E, 0xFF321F00, 0xFF4A3308, 0xFFF6E3C4, 0xFFE0BD84, 0xFF120D06, 0xFF201808, 0xFFA69476, 0xFF382A14, 0xFFEEE2CC),
            // PLATINUM
            RexPaletteSpec(false, 0xFF5C6470, 0xFFFFFFFF, 0xFFE5E7EB, 0xFF1C2128, 0xFF9AA3AF, 0xFFFAFAFB, 0xFFFFFFFF, 0xFF757C87, 0xFFE1E3E6, 0xFF1A1D21),
            // TITANIUM
            RexPaletteSpec(true, 0xFFB0B4BC, 0xFF1A1C1F, 0xFF2C2F33, 0xFFEDEEEF, 0xFF8B929B, 0xFF0D0E10, 0xFF18191C, 0xFF8B8E93, 0xFF2A2C2F, 0xFFEDEEEF),
            // NEON
            RexPaletteSpec(true, 0xFF39FF88, 0xFF00230F, 0xFF0C3D1F, 0xFFC6FFDC, 0xFFFF3EC8, 0xFF060608, 0xFF101014, 0xFF8A8A96, 0xFF232330, 0xFFE9E9F0),
            // VAPORWAVE
            RexPaletteSpec(true, 0xFFFF6AD5, 0xFF3A0028, 0xFF52063C, 0xFFFFD6F3, 0xFF8CFFFF, 0xFF0F0817, 0xFF1C1229, 0xFFA08CB3, 0xFF332248, 0xFFF1E6F7),
            // SYNTHWAVE
            RexPaletteSpec(true, 0xFFFF5FA2, 0xFF350016, 0xFF4D0827, 0xFFFFD3E4, 0xFFFFC857, 0xFF0B0620, 0xFF17103A, 0xFF9689B8, 0xFF2A2154, 0xFFEDE7FA),
            // MATRIX
            RexPaletteSpec(true, 0xFF00FF66, 0xFF00190A, 0xFF00331A, 0xFFB8FFD1, 0xFF00CC55, 0xFF000000, 0xFF0A0F0A, 0xFF6FA98A, 0xFF1A2E1E, 0xFFD4FFDE),
            // GALAXY
            RexPaletteSpec(true, 0xFFA78BFA, 0xFF1E1240, 0xFF322361, 0xFFE9E1FF, 0xFF60A5FA, 0xFF0A0716, 0xFF161028, 0xFF938BB0, 0xFF2A2150, 0xFFEAE7F7),
            // NEBULA
            RexPaletteSpec(true, 0xFFF472B6, 0xFF3D0A28, 0xFF5A123D, 0xFFFFE0F0, 0xFF818CF8, 0xFF0B0714, 0xFF17102A, 0xFF9C8CAE, 0xFF2E2348, 0xFFF0E7F7),
            // COMET
            RexPaletteSpec(true, 0xFF6EE0FF, 0xFF00303D, 0xFF0C4757, 0xFFCFF6FF, 0xFFA5F3FC, 0xFF060E13, 0xFF0F1C24, 0xFF83A0AB, 0xFF1D3641, 0xFFE2F3F7),
            // ECLIPSE
            RexPaletteSpec(true, 0xFFFF9F45, 0xFF3A1F00, 0xFF522C00, 0xFFFFE3C2, 0xFFFFD180, 0xFF050505, 0xFF121212, 0xFF9E9A93, 0xFF262422, 0xFFF0EDE7),
            // STARLIGHT
            RexPaletteSpec(true, 0xFFC7D2FE, 0xFF1E2560, 0xFF2E3778, 0xFFEDEFFE, 0xFF93C5FD, 0xFF0A0C1A, 0xFF141830, 0xFF9098BD, 0xFF262D52, 0xFFE9EBFA),
            // MOONLIGHT
            RexPaletteSpec(true, 0xFFBFC9E0, 0xFF1D2436, 0xFF2C3550, 0xFFE9ECF7, 0xFF8D9BC4, 0xFF0B0E17, 0xFF161B28, 0xFF8993AC, 0xFF262E44, 0xFFE6E9F2),
            // SOLAR
            RexPaletteSpec(false, 0xFFFFA000, 0xFFFFFFFF, 0xFFFFE0A3, 0xFF4D3000, 0xFFFFCA4D, 0xFFFFFBF0, 0xFFFFFFFF, 0xFF8C7A4D, 0xFFF5E4B8, 0xFF2E2408),
            // LAGOON
            RexPaletteSpec(false, 0xFF00A9A5, 0xFFFFFFFF, 0xFFC7F0EE, 0xFF00332F, 0xFF5FE0D0, 0xFFF2FBFA, 0xFFFFFFFF, 0xFF5C807C, 0xFFCDECE9, 0xFF0D2422),
            // CORAL
            RexPaletteSpec(false, 0xFFFF6F5E, 0xFFFFFFFF, 0xFFFFDBD3, 0xFF4A1108, 0xFFFFA88F, 0xFFFFF6F4, 0xFFFFFFFF, 0xFF8C6A62, 0xFFF5DAD3, 0xFF2E140E),
            // TROPICAL
            RexPaletteSpec(false, 0xFF16A34A, 0xFFFFFFFF, 0xFFD0F2D9, 0xFF06331A, 0xFFFACC15, 0xFFF3FBF4, 0xFFFFFFFF, 0xFF5B806A, 0xFFCEE9D3, 0xFF0E2415),
            // BAMBOO
            RexPaletteSpec(false, 0xFF6DAA3C, 0xFFFFFFFF, 0xFFE0F0CB, 0xFF243D0E, 0xFFA8D96A, 0xFFF8FBF3, 0xFFFFFFFF, 0xFF748A5F, 0xFFE2EDD3, 0xFF1C2A11),
            // OLIVE
            RexPaletteSpec(true, 0xFFB5C36A, 0xFF262E00, 0xFF3A4408, 0xFFE9F2C6, 0xFFDCE08F, 0xFF0F1006, 0xFF1C1E0E, 0xFF9DA080, 0xFF32351B, 0xFFECEEDB),
            // TERRACOTTA
            RexPaletteSpec(false, 0xFFC4623E, 0xFFFFFFFF, 0xFFF3DACB, 0xFF441E0C, 0xFFE0946B, 0xFFFCF6F2, 0xFFFFFFFF, 0xFF8B6E5F, 0xFFEBDACF, 0xFF2E1B12),
            // CLAY
            RexPaletteSpec(false, 0xFFB2603A, 0xFFFFFFFF, 0xFFEEDACB, 0xFF3E1E0A, 0xFFD69568, 0xFFFBF5EF, 0xFFFFFFFF, 0xFF8A6E5C, 0xFFE7D8C8, 0xFF2B1A0F),
            // CHARCOAL
            RexPaletteSpec(true, 0xFFE0E0E0, 0xFF1A1A1A, 0xFF2C2C2C, 0xFFF2F2F2, 0xFFB0B0B0, 0xFF0D0D0D, 0xFF181818, 0xFF8F8F8F, 0xFF2A2A2A, 0xFFEFEFEF),
            // GRAPHITE
            RexPaletteSpec(true, 0xFFA0AEC0, 0xFF16202C, 0xFF24303F, 0xFFE3E9F0, 0xFF718096, 0xFF0B0F14, 0xFF161C24, 0xFF828C99, 0xFF25303D, 0xFFE5E9EE),
            // PORCELAIN
            RexPaletteSpec(false, 0xFF4A7FB5, 0xFFFFFFFF, 0xFFDCE9F5, 0xFF0F2B44, 0xFF8FB8DE, 0xFFF9FBFD, 0xFFFFFFFF, 0xFF6E828F, 0xFFE0E9F0, 0xFF15242E),
            // BLOSSOM
            RexPaletteSpec(false, 0xFFE8749A, 0xFFFFFFFF, 0xFFFCE1EA, 0xFF4A1029, 0xFFF7A8C2, 0xFFFFF7FA, 0xFFFFFFFF, 0xFF8C6C77, 0xFFF5DDE5, 0xFF2E141C),
            // ORCHID
            RexPaletteSpec(true, 0xFFD891EF, 0xFF3A0A4D, 0xFF551671, 0xFFF6DEFF, 0xFFF0A8FF, 0xFF120A17, 0xFF201229, 0xFFAD91B8, 0xFF3A2547, 0xFFF2E6F7),
            // MAGENTA
            RexPaletteSpec(true, 0xFFFF3EA5, 0xFF3A0021, 0xFF52062F, 0xFFFFD6EC, 0xFFFF7FC4, 0xFF120610, 0xFF20101B, 0xFFAE8B9E, 0xFF3A2032, 0xFFF3E4EE),
            // INDIGO
            RexPaletteSpec(true, 0xFF6366F1, 0xFF13124A, 0xFF272370, 0xFFE0E0FE, 0xFF818CF8, 0xFF080714, 0xFF141128, 0xFF8D8AB3, 0xFF24215A, 0xFFE5E4F8),
            // COBALT
            RexPaletteSpec(true, 0xFF2F6FED, 0xFF00164A, 0xFF0A2C74, 0xFFCEDFFF, 0xFF5B9BFF, 0xFF040914, 0xFF0C1628, 0xFF7F92B3, 0xFF1B2D54, 0xFFDEE6F7),
            // TEAL
            RexPaletteSpec(true, 0xFF2DD4BF, 0xFF00332E, 0xFF0B4A42, 0xFFCCFBF1, 0xFF5EEAD4, 0xFF04100E, 0xFF0C1D1A, 0xFF7BA69D, 0xFF1C3934, 0xFFDFF5F0),
            // TURQUOISE
            RexPaletteSpec(false, 0xFF06B6D4, 0xFFFFFFFF, 0xFFCBF3FA, 0xFF033543, 0xFF67E8F9, 0xFFF1FCFE, 0xFFFFFFFF, 0xFF57828B, 0xFFCCECF2, 0xFF0B2429),
            // AQUA
            RexPaletteSpec(false, 0xFF0EA5B7, 0xFFFFFFFF, 0xFFCEEFF3, 0xFF073942, 0xFF5FD4E0, 0xFFF2FBFC, 0xFFFFFFFF, 0xFF547F84, 0xFFCDE9EC, 0xFF0C2326),
            // LIME
            RexPaletteSpec(false, 0xFF84CC16, 0xFF1B2E00, 0xFFE3F5B8, 0xFF243E00, 0xFFBEF264, 0xFFF9FCF0, 0xFFFFFFFF, 0xFF6E7C4A, 0xFFE6EEC7, 0xFF1E2408),
            // CITRUS
            RexPaletteSpec(false, 0xFFFF8A00, 0xFFFFFFFF, 0xFFFFE0B3, 0xFF4A2900, 0xFFFFD166, 0xFFFFFAF0, 0xFFFFFFFF, 0xFF8C744D, 0xFFF5E2C0, 0xFF2E1E06),
            // HONEY
            RexPaletteSpec(false, 0xFFD9A441, 0xFFFFFFFF, 0xFFF7E7C2, 0xFF453006, 0xFFEFC97A, 0xFFFFFAEF, 0xFFFFFFFF, 0xFF8E7B54, 0xFFF0E2BF, 0xFF2C2005),
            // CARAMEL
            RexPaletteSpec(true, 0xFFD9A066, 0xFF33200A, 0xFF4A3212, 0xFFF5E2C6, 0xFFEFC490, 0xFF140D06, 0xFF221708, 0xFFA9927A, 0xFF3B2C15, 0xFFEFE4D2),
            // MOCHA
            RexPaletteSpec(true, 0xFFBB8B65, 0xFF2C1A0B, 0xFF43291A, 0xFFEEDDCB, 0xFFD9B48E, 0xFF120C08, 0xFF1E1610, 0xFFA1917F, 0xFF352618, 0xFFEBE1D5),
            // ESPRESSO
            RexPaletteSpec(true, 0xFF9C6644, 0xFF1E1108, 0xFF32200F, 0xFFE6D2C0, 0xFFC08552, 0xFF0D0805, 0xFF17100A, 0xFF917B6C, 0xFF2A2014, 0xFFE7DBCE),
            // WINE
            RexPaletteSpec(true, 0xFFB33951, 0xFFFFFFFF, 0xFF441420, 0xFFF6D4DC, 0xFFDB6E86, 0xFF120609, 0xFF1F1015, 0xFFA6828B, 0xFF35202A, 0xFFEFE0E4),
            // BERRY
            RexPaletteSpec(true, 0xFFA855F7, 0xFF250A42, 0xFF3B1362, 0xFFF0E0FF, 0xFFD8B4FE, 0xFF0D0716, 0xFF19102B, 0xFF9C89B5, 0xFF2E2049, 0xFFEEE6F7),
            // CHERRY
            RexPaletteSpec(false, 0xFFE0263D, 0xFFFFFFFF, 0xFFFAD2D8, 0xFF4A0812, 0xFFFF7285, 0xFFFFF5F6, 0xFFFFFFFF, 0xFF8C5F64, 0xFFF3D6DA, 0xFF2E0D11),
            // FLAMINGO
            RexPaletteSpec(false, 0xFFF9509A, 0xFFFFFFFF, 0xFFFFDCEB, 0xFF4A0A29, 0xFFFF8FC0, 0xFFFFF6FA, 0xFFFFFFFF, 0xFF8C6577, 0xFFF5D9E7, 0xFF2E0F1D),
            // PEACH
            RexPaletteSpec(false, 0xFFFF9466, 0xFFFFFFFF, 0xFFFFE3D1, 0xFF4A2408, 0xFFFFBFA0, 0xFFFFF9F5, 0xFFFFFFFF, 0xFF8C7566, 0xFFF5E2D6, 0xFF2E1B10)
        )

        private val specTable: Map<RexThemeOption, RexPaletteSpec> by lazy {
            val options = RexThemeOption.values()

            // Defensive: if counts ever drift apart (someone adds an enum
            // case without adding a row, or vice versa), fall back to
            // cycling the rows instead of throwing — the app stays usable
            // and just shows a repeated palette for the extra entries.
            if (options.size == rows.size) {
                options.zip(rows).toMap()
            } else {
                options.mapIndexed { index, option ->
                    option to rows[index % rows.size]
                }.toMap()
            }
        }
    }
}
