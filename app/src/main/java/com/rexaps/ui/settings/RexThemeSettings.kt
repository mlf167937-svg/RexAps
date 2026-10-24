package com.rexaps.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rexaps.ui.theme.*

@Composable
fun RexThemeSettings(manager: RexThemeManager, modifier: Modifier = Modifier) {
    RexThemeSettingsContent(manager = manager, modifier = modifier)
}
