package com.rexaps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    COFFEE("Coffee")
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

/* -------------------------------- PALETTES -------------------------------- */

private fun palette(
    dark: Boolean,
    primary: Long,
    onPrimary: Long,
    container: Long,
    onContainer: Long,
    tertiary: Long,
    background: Long,
    card: Long,
    muted: Long,
    outline: Long,
    text: Long
): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = Color(primary),
            onPrimary = Color(onPrimary),
            primaryContainer = Color(container),
            onPrimaryContainer = Color(onContainer),
            secondary = Color(muted),
            tertiary = Color(tertiary),
            background = Color(background),
            onBackground = Color(text),
            surface = Color(background),
            onSurface = Color(text),
            surfaceVariant = Color(card),
            onSurfaceVariant = Color(muted),
            outlineVariant = Color(outline)
        )
    } else {
        lightColorScheme(
            primary = Color(primary),
            onPrimary = Color(onPrimary),
            primaryContainer = Color(container),
            onPrimaryContainer = Color(onContainer),
            secondary = Color(muted),
            tertiary = Color(tertiary),
            background = Color(background),
            onBackground = Color(text),
            surface = Color(background),
            onSurface = Color(text),
            surfaceVariant = Color(card),
            onSurfaceVariant = Color(muted),
            outlineVariant = Color(outline)
        )
    }

private val LightScheme = palette(false, 0xFF111111, 0xFFFFFFFF, 0xFFE6E6EB, 0xFF111111, 0xFF4A4A55, 0xFFF7F7F9, 0xFFFFFFFF, 0xFF66666E, 0xFFE0E0E6, 0xFF121214)
private val DarkScheme = palette(true, 0xFFE6E6EA, 0xFF121214, 0xFF2A2A30, 0xFFE6E6EA, 0xFFB8B8C2, 0xFF0F0F12, 0xFF1B1B20, 0xFF9A9AA3, 0xFF2C2C33, 0xFFE8E8EC)
private val AmoledScheme = palette(true, 0xFFFFFFFF, 0xFF000000, 0xFF1E1E22, 0xFFFFFFFF, 0xFF9A9AA3, 0xFF000000, 0xFF121214, 0xFF9A9AA3, 0xFF26262B, 0xFFFFFFFF)
private val OceanScheme = palette(true, 0xFF4FC3F7, 0xFF00263A, 0xFF0F3A55, 0xFFCDEBFF, 0xFF7C9CFF, 0xFF0A1622, 0xFF122536, 0xFF8FA9BC, 0xFF1F3A50, 0xFFE3F1FB)
private val ForestScheme = palette(false, 0xFF2E7D4F, 0xFFFFFFFF, 0xFFCDEBD6, 0xFF0B2E1A, 0xFF4CAF7A, 0xFFF3F8F4, 0xFFE3EEE6, 0xFF55675C, 0xFFCBDCD0, 0xFF13201A)
private val SunsetScheme = palette(true, 0xFFFF8A50, 0xFF3A1600, 0xFF4A2412, 0xFFFFDCC8, 0xFFFF5F7E, 0xFF1A1110, 0xFF2A1B18, 0xFFB89A90, 0xFF43302B, 0xFFF6E7E1)
private val LavenderScheme = palette(false, 0xFF6D4AFF, 0xFFFFFFFF, 0xFFE7DFFF, 0xFF23106B, 0xFFB06CFF, 0xFFF7F4FF, 0xFFFFFFFF, 0xFF6E6885, 0xFFE3DCF5, 0xFF1A1428)
private val RoseScheme = palette(false, 0xFFE0456B, 0xFFFFFFFF, 0xFFFFDDE5, 0xFF4A0F1E, 0xFFFF8FA8, 0xFFFFF5F7, 0xFFFFFFFF, 0xFF85646C, 0xFFF5DCE2, 0xFF2A1418)
private val MintScheme = palette(false, 0xFF00A88A, 0xFFFFFFFF, 0xFFCFF5EC, 0xFF002E26, 0xFF3DD6B0, 0xFFF2FBF9, 0xFFFFFFFF, 0xFF5B7A74, 0xFFD3EBE6, 0xFF0E211D)
private val SandScheme = palette(false, 0xFF9A6B3F, 0xFFFFFFFF, 0xFFF0E1CE, 0xFF3A2410, 0xFFD1A06B, 0xFFFAF6EF, 0xFFFFFDF8, 0xFF7D6E5C, 0xFFEADFCC, 0xFF261C10)
private val SkyScheme = palette(false, 0xFF1E88E5, 0xFFFFFFFF, 0xFFD6EAFF, 0xFF002B52, 0xFF5CC8FF, 0xFFF3F8FE, 0xFFFFFFFF, 0xFF60778F, 0xFFDAE7F5, 0xFF0E1A26)
private val MidnightScheme = palette(true, 0xFF8C9EFF, 0xFF0B1040, 0xFF1E2560, 0xFFDDE1FF, 0xFFB388FF, 0xFF0B0D1F, 0xFF141834, 0xFF8D93B8, 0xFF262C55, 0xFFE6E8FA)
private val CrimsonScheme = palette(true, 0xFFFF4D5E, 0xFF3A0008, 0xFF4A1016, 0xFFFFD9DC, 0xFFFF8A65, 0xFF130A0B, 0xFF201214, 0xFFB08F92, 0xFF3D2225, 0xFFF5E6E7)
private val EmeraldScheme = palette(true, 0xFF2EE59D, 0xFF00301F, 0xFF0F3D2B, 0xFFC6F7E1, 0xFF4DD0E1, 0xFF08120E, 0xFF10201A, 0xFF8DAA9E, 0xFF1D3A2E, 0xFFE3F5EC)
private val GoldScheme = palette(true, 0xFFFFC94D, 0xFF2E2000, 0xFF3A2E10, 0xFFFFEDC2, 0xFFFF9F43, 0xFF0A0A0A, 0xFF161513, 0xFFA39B87, 0xFF2E2A1F, 0xFFF3EEE0)
private val CyberScheme = palette(true, 0xFF00F0FF, 0xFF00272B, 0xFF12262E, 0xFFC2FBFF, 0xFFFF2BD6, 0xFF07070D, 0xFF111120, 0xFF8A8AB0, 0xFF25254A, 0xFFEAEAFF)
private val NordScheme = palette(true, 0xFF88C0D0, 0xFF1B2A30, 0xFF3B4252, 0xFFECEFF4, 0xFF81A1C1, 0xFF2E3440, 0xFF3B4252, 0xFFA5AEBF, 0xFF4C566A, 0xFFECEFF4)
private val DraculaScheme = palette(true, 0xFFBD93F9, 0xFF1E1633, 0xFF44475A, 0xFFF8F8F2, 0xFFFF79C6, 0xFF282A36, 0xFF343746, 0xFF9EA3BF, 0xFF44475A, 0xFFF8F8F2)
private val CoffeeScheme = palette(true, 0xFFD7A86E, 0xFF2B1A08, 0xFF3E2B1C, 0xFFF2DFC8, 0xFFB07A4F, 0xFF17110D, 0xFF241B15, 0xFFA8917C, 0xFF3A2C22, 0xFFF1E6DA)

fun RexThemeOption.colorScheme(systemDark: Boolean): ColorScheme = when (this) {
    RexThemeOption.SYSTEM -> if (systemDark) DarkScheme else LightScheme
    RexThemeOption.LIGHT -> LightScheme
    RexThemeOption.DARK -> DarkScheme
    RexThemeOption.AMOLED -> AmoledScheme
    RexThemeOption.OCEAN -> OceanScheme
    RexThemeOption.FOREST -> ForestScheme
    RexThemeOption.SUNSET -> SunsetScheme
    RexThemeOption.LAVENDER -> LavenderScheme
    RexThemeOption.ROSE -> RoseScheme
    RexThemeOption.MINT -> MintScheme
    RexThemeOption.SAND -> SandScheme
    RexThemeOption.SKY -> SkyScheme
    RexThemeOption.MIDNIGHT -> MidnightScheme
    RexThemeOption.CRIMSON -> CrimsonScheme
    RexThemeOption.EMERALD -> EmeraldScheme
    RexThemeOption.GOLD -> GoldScheme
    RexThemeOption.CYBER -> CyberScheme
    RexThemeOption.NORD -> NordScheme
    RexThemeOption.DRACULA -> DraculaScheme
    RexThemeOption.COFFEE -> CoffeeScheme
}

/* -------------------------------- TYPOGRAPHY ------------------------------ */

private val baseTypography = Typography()

private val RexTypography = Typography(
    headlineLarge = baseTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
)

/* ---------------------------------- THEME --------------------------------- */

/** Warna berpindah dengan transisi halus saat tema diganti. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = 450)

    @Composable
    fun anim(color: Color): Color =
        animateColorAsState(color, spec, label = "themeColor").value

    return copy(
        primary = anim(primary),
        onPrimary = anim(onPrimary),
        primaryContainer = anim(primaryContainer),
        onPrimaryContainer = anim(onPrimaryContainer),
        secondary = anim(secondary),
        tertiary = anim(tertiary),
        background = anim(background),
        onBackground = anim(onBackground),
        surface = anim(surface),
        onSurface = anim(onSurface),
        surfaceVariant = anim(surfaceVariant),
        onSurfaceVariant = anim(onSurfaceVariant),
        outlineVariant = anim(outlineVariant)
    )
}

@Composable
fun RexTheme(
    option: RexThemeOption,
    content: @Composable () -> Unit
) {
    val target = option.colorScheme(isSystemInDarkTheme())

    MaterialTheme(
        colorScheme = target.animated(),
        typography = RexTypography,
        content = content
    )
}
