package com.rexaps.ui

internal fun RexThemeStore.__splitImpl_save(option: RexThemeOption) {
        prefs.edit().putString(KEY_THEME, option.name).apply()
        if (option in RexThemeOption.curatedOptions) engine.setTheme(option.labelToThemeId())
    }
