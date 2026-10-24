package com.rexaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/* ═════════════════════ Tema ═════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSheet(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pilih tema",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${RexThemeOption.curatedOptions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "30 tema premium dalam enam kategori visual, dengan preview langsung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ThemePicker(selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
internal fun ThemePicker(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryGroups = listOf(
        "Dark" to RexThemeOption.curatedOptions.filter { it.name.startsWith("DARK_") },
        "Glass" to RexThemeOption.curatedOptions.filter { it.name.startsWith("GLASS_") },
        "Light" to RexThemeOption.curatedOptions.filter { it.name.startsWith("LIGHT_") },
        "Neon" to RexThemeOption.curatedOptions.filter { it.name.startsWith("NEON_") },
        "Cosmic" to RexThemeOption.curatedOptions.filter { it.name.startsWith("ASTRAL_") || it.name.startsWith("NEBULA_") || it.name.startsWith("ECLIPSE_") || it.name.startsWith("STARLIGHT_") },
        "Elemental" to RexThemeOption.curatedOptions.filter { it.name.startsWith("FROST_") || it.name.startsWith("INFERNO_") || it.name.startsWith("THUNDER_") || it.name.startsWith("ABYSSAL_") }
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categoryGroups.forEach { (category, options) ->
            Text(category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            options.chunked(THEME_COLUMNS).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { option ->
                        ThemeTile(option = option, isSelected = option == selected,
                            onClick = { onSelect(option) }, modifier = Modifier.weight(1f))
                    }
                    repeat(THEME_COLUMNS - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
