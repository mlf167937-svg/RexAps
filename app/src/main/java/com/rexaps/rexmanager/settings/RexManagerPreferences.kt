package com.rexaps.rexmanager.settings

import android.content.Context
import android.content.SharedPreferences
import com.rexaps.rexmanager.SortMode
import com.rexaps.rexmanager.SortOrder
import com.rexaps.rexmanager.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists RexManager settings with SharedPreferences (no extra dependency).
 * Can be swapped for DataStore later without touching the ViewModel contract.
 */
class RexManagerPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("rexmanager_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<RexManagerSettings> = _settings.asStateFlow()

    fun update(transform: (RexManagerSettings) -> RexManagerSettings) {
        val updated = transform(_settings.value)
        write(updated)
        _settings.value = updated
    }

    private fun read(): RexManagerSettings {
        val d = RexManagerSettings()
        return RexManagerSettings(
            viewMode = prefs.enumValue(KEY_VIEW_MODE, d.viewMode),
            foldersFirst = prefs.getBoolean(KEY_FOLDERS_FIRST, d.foldersFirst),
            showHiddenFiles = prefs.getBoolean(KEY_SHOW_HIDDEN, d.showHiddenFiles),
            showFileExtensions = prefs.getBoolean(KEY_SHOW_EXTENSIONS, d.showFileExtensions),
            confirmDelete = true,
            confirmOverwrite = prefs.getBoolean(KEY_CONFIRM_OVERWRITE, d.confirmOverwrite),
            confirmHideUnhide = prefs.getBoolean(KEY_CONFIRM_HIDE, d.confirmHideUnhide),
            sortMode = prefs.enumValue(KEY_SORT_MODE, d.sortMode),
            sortOrder = prefs.enumValue(KEY_SORT_ORDER, d.sortOrder)
        )
    }

    private fun write(s: RexManagerSettings) {
        prefs.edit()
            .putString(KEY_VIEW_MODE, s.viewMode.name)
            .putBoolean(KEY_FOLDERS_FIRST, s.foldersFirst)
            .putBoolean(KEY_SHOW_HIDDEN, s.showHiddenFiles)
            .putBoolean(KEY_SHOW_EXTENSIONS, s.showFileExtensions)
            .putBoolean(KEY_CONFIRM_OVERWRITE, s.confirmOverwrite)
            .putBoolean(KEY_CONFIRM_HIDE, s.confirmHideUnhide)
            .putString(KEY_SORT_MODE, s.sortMode.name)
            .putString(KEY_SORT_ORDER, s.sortOrder.name)
            .apply()
    }

    private companion object {
        const val KEY_VIEW_MODE = "view_mode"
        const val KEY_FOLDERS_FIRST = "folders_first"
        const val KEY_SHOW_HIDDEN = "show_hidden"
        const val KEY_SHOW_EXTENSIONS = "show_extensions"
        const val KEY_CONFIRM_OVERWRITE = "confirm_overwrite"
        const val KEY_CONFIRM_HIDE = "confirm_hide"
        const val KEY_SORT_MODE = "sort_mode"
        const val KEY_SORT_ORDER = "sort_order"
    }
}

private inline fun <reified E : Enum<E>> SharedPreferences.enumValue(key: String, default: E): E =
    getString(key, null)?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
