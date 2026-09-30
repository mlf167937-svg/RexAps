package com.rexaps.rexmanager.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rexaps.rexmanager.SortMode
import com.rexaps.rexmanager.SortOrder
import com.rexaps.rexmanager.ViewMode

data class RexManagerSettings(
    val viewMode: ViewMode = ViewMode.LIST,
    val foldersFirst: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val showFileExtensions: Boolean = true,
    /** Kept for completeness; deleting always asks for confirmation (see SettingsScreen). */
    val confirmDelete: Boolean = true,
    val confirmOverwrite: Boolean = true,
    val confirmHideUnhide: Boolean = true,
    val sortMode: SortMode = SortMode.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexManagerSettingsScreen(
    settings: RexManagerSettings,
    onChange: ((RexManagerSettings) -> RexManagerSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("RexManager settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionTitle("Appearance")
            ChoiceRow(
                title = "View mode",
                valueLabel = if (settings.viewMode == ViewMode.LIST) "List" else "Grid",
                options = listOf("List" to ViewMode.LIST, "Grid" to ViewMode.GRID),
                onSelect = { mode -> onChange { it.copy(viewMode = mode) } }
            )
            SwitchRow(
                title = "Folders first",
                checked = settings.foldersFirst,
                onCheckedChange = { v -> onChange { it.copy(foldersFirst = v) } }
            )

            SettingsSectionTitle("File display")
            SwitchRow(
                title = "Show hidden files",
                subtitle = "Files whose name starts with a dot",
                checked = settings.showHiddenFiles,
                onCheckedChange = { v -> onChange { it.copy(showHiddenFiles = v) } }
            )
            SwitchRow(
                title = "Show file extensions",
                checked = settings.showFileExtensions,
                onCheckedChange = { v -> onChange { it.copy(showFileExtensions = v) } }
            )

            SettingsSectionTitle("Behavior")
            SwitchRow(
                title = "Confirm delete",
                subtitle = "Always on. Deleting is permanent, so it always asks first.",
                checked = true,
                enabled = false,
                onCheckedChange = {}
            )
            SwitchRow(
                title = "Confirm overwrite",
                subtitle = "When off, name conflicts keep both items (never overwrites silently)",
                checked = settings.confirmOverwrite,
                onCheckedChange = { v -> onChange { it.copy(confirmOverwrite = v) } }
            )
            SwitchRow(
                title = "Confirm hide / unhide",
                checked = settings.confirmHideUnhide,
                onCheckedChange = { v -> onChange { it.copy(confirmHideUnhide = v) } }
            )

            SettingsSectionTitle("Sorting")
            ChoiceRow(
                title = "Sort by",
                valueLabel = when (settings.sortMode) {
                    SortMode.NAME -> "Name"
                    SortMode.SIZE -> "Size"
                    SortMode.MODIFIED -> "Modified"
                    SortMode.TYPE -> "Type"
                },
                options = listOf(
                    "Name" to SortMode.NAME,
                    "Size" to SortMode.SIZE,
                    "Modified" to SortMode.MODIFIED,
                    "Type" to SortMode.TYPE
                ),
                onSelect = { mode -> onChange { it.copy(sortMode = mode) } }
            )
            ChoiceRow(
                title = "Sort order",
                valueLabel = if (settings.sortOrder == SortOrder.ASCENDING) "Ascending" else "Descending",
                options = listOf("Ascending" to SortOrder.ASCENDING, "Descending" to SortOrder.DESCENDING),
                onSelect = { order -> onChange { it.copy(sortOrder = order) } }
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) }
    )
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    valueLabel: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Row {
                TextButton(onClick = { expanded = true }) { Text(valueLabel) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (label, value) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                onSelect(value)
                            }
                        )
                    }
                }
            }
        }
    )
}
