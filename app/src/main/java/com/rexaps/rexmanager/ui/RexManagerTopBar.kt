package com.rexaps.rexmanager.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexmanager.RexManagerActions
import com.rexaps.rexmanager.RexManagerState
import com.rexaps.rexmanager.SortMode
import com.rexaps.rexmanager.SortOrder
import com.rexaps.rexmanager.ViewMode

private fun SortMode.label(): String = when (this) {
    SortMode.NAME -> "Name"
    SortMode.SIZE -> "Size"
    SortMode.MODIFIED -> "Modified"
    SortMode.TYPE -> "Type"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexManagerTopBar(
    state: RexManagerState,
    actions: RexManagerActions,
    onNavigateBack: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isList = state.viewMode == ViewMode.LIST

    TopAppBar(
        title = { Text("RexManager") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { actions.onViewModeChange(if (isList) ViewMode.GRID else ViewMode.LIST) }) {
                Icon(
                    imageVector = if (isList) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = if (isList) "Switch to grid view" else "Switch to list view"
                )
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("New folder") },
                    leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onRequestCreate(true)
                    }
                )
                DropdownMenuItem(
                    text = { Text("New file") },
                    leadingIcon = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onRequestCreate(false)
                    }
                )
                HorizontalDivider()
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                SortMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label()) },
                        trailingIcon = {
                            if (state.sortMode == mode) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected")
                            }
                        },
                        onClick = {
                            menuOpen = false
                            actions.onSortModeChange(mode)
                        }
                    )
                }
                val ascending = state.sortOrder == SortOrder.ASCENDING
                DropdownMenuItem(
                    text = { Text(if (ascending) "Sort descending" else "Sort ascending") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onSortOrderChange(if (ascending) SortOrder.DESCENDING else SortOrder.ASCENDING)
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(if (state.showHiddenFiles) "Hide hidden files" else "Show hidden files") },
                    leadingIcon = {
                        Icon(
                            if (state.showHiddenFiles) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuOpen = false
                        actions.onShowHiddenChange(!state.showHiddenFiles)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Refresh") },
                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onRefresh()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onOpenSettings()
                    }
                )
            }
        }
    )
}
