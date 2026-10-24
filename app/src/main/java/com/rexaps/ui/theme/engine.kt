package com.rexaps.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.CopyOnWriteArraySet

enum class RexAnimationLevel { FULL, BALANCED, SAVER, OFF }
data class RexMotionSettings(
    val level: RexAnimationLevel = RexAnimationLevel.BALANCED,
    val followSystemReduceMotion: Boolean = true,
    val effectIntensity: Int = 72
)

/** One process-wide source of truth. Subscribe for live theme updates; preview never persists until commit. */
class RexThemeManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("rexaps_theme_engine", Context.MODE_PRIVATE)
    private val listeners = CopyOnWriteArraySet<(RexDesignTokens) -> Unit>()
    private var committedId: String = RexThemeRegistry.get(prefs.getString(KEY_THEME, RexThemeRegistry.default.id).orEmpty()).id
    private var previewId: String? = null
    var current by mutableStateOf(RexThemeRegistry.get(committedId))
        private set
    var motionSettings by mutableStateOf(loadMotion())
        private set

    fun getCurrentTheme(): RexDesignTokens = current
    fun getDefaultTheme(): RexDesignTokens = RexThemeRegistry.default
    fun getThemesByCategory(): Map<String, List<RexDesignTokens>> = RexThemeRegistry.byCategory()
    fun subscribe(listener: (RexDesignTokens) -> Unit): () -> Unit {
        listeners.add(listener); listener(current)
        return { listeners.remove(listener) }
    }
    fun setTheme(themeId: String) {
        val theme = RexThemeRegistry.get(themeId)
        previewId = null; committedId = theme.id; current = theme
        prefs.edit().putString(KEY_THEME, theme.id).apply()
        // Keep the user-facing theme picker preference in sync with the engine ID.
        appContext.getSharedPreferences("rexaps_settings", Context.MODE_PRIVATE)
            .edit().putString("theme_option", theme.id.uppercase()).apply()
        listeners.forEach { it(theme) }
    }
    fun previewTheme(themeId: String) {
        val theme = RexThemeRegistry.get(themeId)
        previewId = theme.id; current = theme
        listeners.forEach { it(theme) }
    }
    fun commitPreview() { previewId?.let(::setTheme) }
    fun cancelPreview() {
        previewId = null; current = RexThemeRegistry.get(committedId)
        listeners.forEach { it(current) }
    }
    fun updateMotion(settings: RexMotionSettings) {
        motionSettings = settings
        prefs.edit().putString(KEY_LEVEL, settings.level.name)
            .putBoolean(KEY_REDUCE, settings.followSystemReduceMotion)
            .putInt(KEY_INTENSITY, settings.effectIntensity.coerceIn(0,100)).apply()
    }
    private fun loadMotion() = RexMotionSettings(
        runCatching { RexAnimationLevel.valueOf(prefs.getString(KEY_LEVEL, RexAnimationLevel.BALANCED.name)!!) }.getOrDefault(RexAnimationLevel.BALANCED),
        prefs.getBoolean(KEY_REDUCE, true),
        prefs.getInt(KEY_INTENSITY, 72).coerceIn(0,100)
    )
    companion object {
        private const val KEY_THEME = "theme_id"
        private const val KEY_LEVEL = "motion_level"
        private const val KEY_REDUCE = "follow_reduce_motion"
        private const val KEY_INTENSITY = "effect_intensity"
    }
}
