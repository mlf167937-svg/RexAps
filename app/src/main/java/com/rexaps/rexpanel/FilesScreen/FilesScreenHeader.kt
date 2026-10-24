package com.rexaps.rexpanel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_BrowserToolbar(
    b: FileBrowserState,
    searching: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onNewFolder: () -> Unit,
    onCopyPath: () -> Unit,
    onUpload: () -> Unit,
    onSelectAll: () -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searching) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp).focusRequester(focus),
                singleLine = true,
                placeholder = { Text("Cari di folder ini") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Close, "Tutup pencarian") }
        } else {
            IconButton(onClick = { b.up() }, enabled = b.canGoUp) {
                Icon(Icons.Default.ArrowUpward, "Naik satu folder")
            }
            IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Search, "Cari") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUpload) { Icon(Icons.Default.Upload, "Unggah berkas ke folder ini") }
            IconButton(onClick = { b.grid = !b.grid }) {
                Icon(
                    if (b.grid) Icons.Default.ViewList else Icons.Default.GridView,
                    if (b.grid) "Tampilan daftar" else "Tampilan kotak"
                )
            }

            var moreOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { moreOpen = true }) { Icon(Icons.Default.MoreVert, "Menu lainnya") }
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Pilih semua") },
                        leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                        onClick = { moreOpen = false; onSelectAll() }
                    )
                    SortBy.values().forEach { s ->
                        DropdownMenuItem(
                            text = { Text("Urutkan: ${s.label}") },
                            onClick = { b.sort = s; moreOpen = false },
                            trailingIcon = { if (b.sort == s) Icon(Icons.Default.Check, "Dipilih") }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Tampilkan tersembunyi") },
                        onClick = { b.showHidden = !b.showHidden; moreOpen = false },
                        trailingIcon = { if (b.showHidden) Icon(Icons.Default.Check, "Aktif") }
                    )
                    DropdownMenuItem(
                        text = { Text("Folder baru") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                        onClick = { moreOpen = false; onNewFolder() }
                    )
                    DropdownMenuItem(
                        text = { Text("Salin path folder") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { moreOpen = false; onCopyPath() }
                    )
                    DropdownMenuItem(
                        text = { Text("Muat ulang") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = { moreOpen = false; b.refresh() }
                    )
                }
            }
        }
    }
}
