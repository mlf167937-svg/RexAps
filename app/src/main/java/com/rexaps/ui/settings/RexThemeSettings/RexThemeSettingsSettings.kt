package com.rexaps.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rexaps.ui.theme.*

@Composable internal
fun __RexThemeSettingsContent(manager: RexThemeManager, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var selectedId by remember { mutableStateOf(manager.getCurrentTheme().id) }
    var showDetail by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(manager.motionSettings) }
    val theme = manager.getCurrentTheme()
    val animationsEnabled = settings.level != RexAnimationLevel.OFF
    val backgroundTop by animateColorAsState(Color(theme.background), tween(if (animationsEnabled) theme.motionNormal else 0), label = "theme-settings-bg-top")
    val backgroundBottom by animateColorAsState(Color(theme.backgroundAlt), tween(if (animationsEnabled) theme.motionSlow else 0), label = "theme-settings-bg-bottom")
    val categories = listOf("Semua", "Glass", "Dark", "Light", "Neon", "Cosmic", "Elemental")
    val filtered = manager.getThemesByCategory().values.flatten().filter {
        (category == "Semua" || it.category.equals(category, true)) &&
            (it.name.contains(query, true) || it.category.contains(query, true))
    }
    Column(
        modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundTop, backgroundBottom)))
            .animateContentSize(tween(if (animationsEnabled) theme.motionSlow else 0, easing = FastOutSlowInEasing))
            .padding(16.dp)
    ) {
        RexThemeSettingsFilters(
            query = query,
            category = category,
            categories = categories,
            theme = theme,
            onQueryChange = { query = it },
            onCategoryChange = { category = it }
        )
        Spacer(Modifier.height(12.dp))
        RexThemeSettingsGrid(
            filtered = filtered,
            theme = theme,
            animationsEnabled = animationsEnabled,
            selectedId = selectedId,
            onSelect = { id -> selectedId = id; manager.previewTheme(id) },
            onDetail = { id -> selectedId = id; manager.previewTheme(id); showDetail = true },
            modifier = Modifier.weight(1f)
        )

        RexThemeSettingsDetail(
            visible = showDetail,
            selectedId = selectedId,
            theme = theme,
            animationsEnabled = animationsEnabled
        )
        RexThemeSettingsMotionControls(
            theme = theme,
            settings = settings,
            onSettingsChange = { updated -> settings = updated; manager.updateMotion(updated) },
            onCancelPreview = { manager.cancelPreview(); selectedId = manager.getCurrentTheme().id },
            onCommitPreview = { manager.commitPreview(); selectedId = manager.getCurrentTheme().id },
            onReset = {
                manager.setTheme(RexThemeRegistry.default.id)
                selectedId = RexThemeRegistry.default.id
                settings = RexMotionSettings()
                manager.updateMotion(settings)
            }
        )
}
}
