package com.rexaps.rexmanager.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.RexManagerActions
import com.rexaps.rexmanager.archive.ArchiveFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexManagerSelectionBar(
    selectedFiles: List<RexFile>,
    totalCount: Int,
    actions: RexManagerActions
) {
    var menuOpen by remember { mutableStateOf(false) }
    val single = selectedFiles.singleOrNull()
    val anyVisible = selectedFiles.any { !it.isHidden }
    val anyHidden = selectedFiles.any { it.isHidden }
    val isArchive = single != null && !single.isDirectory && ArchiveFormat.fromFileName(single.name) != null

    TopAppBar(
        title = { Text("${selectedFiles.size} selected", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = actions::onClearSelection) {
                Icon(Icons.Filled.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = { actions.onCopy(selectedFiles) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = { actions.onMove(selectedFiles) }) {
                Icon(Icons.Filled.ContentCut, contentDescription = "Move")
            }
            IconButton(onClick = { actions.onRequestDelete(selectedFiles) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (selectedFiles.size < totalCount) {
                    DropdownMenuItem(
                        text = { Text("Select all") },
                        leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onSelectAll()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onShare(selectedFiles)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Compress") },
                    leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onRequestCompress(selectedFiles)
                    }
                )
                if (single != null && isArchive) {
                    DropdownMenuItem(
                        text = { Text("Extract") },
                        leadingIcon = { Icon(Icons.Filled.Unarchive, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onRequestExtract(single)
                        }
                    )
                }
                if (anyVisible) {
                    DropdownMenuItem(
                        text = { Text("Hide") },
                        leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onRequestHide(selectedFiles, true)
                        }
                    )
                }
                if (anyHidden) {
                    DropdownMenuItem(
                        text = { Text("Unhide") },
                        leadingIcon = { Icon(Icons.Filled.Visibility, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onRequestHide(selectedFiles, false)
                        }
                    )
                }
                if (single != null) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onRequestRename(single)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Properties") },
                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onRequestProperties(single)
                        }
                    )
                }
            }
        }
    )
}
