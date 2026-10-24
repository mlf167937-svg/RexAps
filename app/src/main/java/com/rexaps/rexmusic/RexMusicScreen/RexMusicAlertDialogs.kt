package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
internal fun RexMusicAlertDialogs(
    state: RexMusicUiState,
    actions: RexActions,
    vm: RexMusicViewModel,
    showCreatePlaylist: Boolean,
    onShowCreatePlaylist: (Boolean) -> Unit,
    pendingDeletePlaylistName: String?,
    onPendingDeletePlaylistName: (String?) -> Unit,
    selectedPlaylistName: String?,
    onSelectedPlaylistName: (String?) -> Unit,
    showStorageDialog: Boolean,
    onShowStorageDialog: (Boolean) -> Unit,
    onPendingDownload: (RexTrack?) -> Unit,
    deleteTarget: RexTrack?,
    onDeleteTarget: (RexTrack?) -> Unit,
    showDeleteAll: Boolean,
    onShowDeleteAll: (Boolean) -> Unit,
    openStorageSettings: () -> Unit
) {
    if (showCreatePlaylist) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { onShowCreatePlaylist(false) },
            title = { Text("Buat playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nama playlist") },
                    placeholder = { Text("Favorit, Santai, Workout...") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.trim().isNotEmpty(),
                    onClick = {
                        vm.createPlaylist(name)
                        playlistTarget?.let { vm.addToPlaylist(name.trim(), it) }
                        playlistTarget = null
                        onShowCreatePlaylist(false)
                    }
                ) { Text("Buat & simpan") }
            },
            dismissButton = { TextButton(onClick = { onShowCreatePlaylist(false) }) { Text("Batal") } }
        )
    }

    pendingDeletePlaylistName?.let { playlistName ->
        AlertDialog(
            onDismissRequest = { onPendingDeletePlaylistName(null) },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("Hapus playlist?") },
            text = { Text("Playlist \"$playlistName\" akan dihapus. Lagu di dalamnya tidak akan dihapus dari koleksi atau unduhan.") },
            confirmButton = {
                TextButton(onClick = {
                    actions.deletePlaylist(playlistName)
                    if (selectedPlaylistName == playlistName) onSelectedPlaylistName(null)
                    onPendingDeletePlaylistName(null)
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onPendingDeletePlaylistName(null) }) { Text("Batal") }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = {
                onShowStorageDialog(false)
                onPendingDownload(null)
            },

            icon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null
                )
            },

            title = {
                Text("Izinkan akses penyimpanan")
            },

            text = {
                Text(
                    "Lagu yang diunduh disimpan di " +
                        "${RexOfflineManager.DISPLAY_PATH}/ " +
                        "supaya mudah kamu buka dan hapus lewat aplikasi File. " +
                        "Aplikasi butuh izin akses penyimpanan untuk itu."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        openStorageSettings()
                    }
                ) {
                    Text("Buka pengaturan")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        onShowStorageDialog(false)
                        onPendingDownload(null)
                    }
                ) {
                    Text("Nanti")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                onDeleteTarget(null)
            },

            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )
            },

            title = {
                Text("Hapus dari offline?")
            },

            text = {
                Text(
                    "\"${target.title}\" akan dihapus dari " +
                        "${RexOfflineManager.DISPLAY_PATH}/. " +
                        "Kamu tetap bisa memutarnya lagi saat online."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteDownload(target)
                        onDeleteTarget(null)
                    }
                ) {
                    Text(
                        "Hapus",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteTarget(null)
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = {
                onShowDeleteAll(false)
            },

            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )
            },

            title = {
                Text("Hapus semua lagu offline?")
            },

            text = {
                Text(
                    "${state.offline.entries.size} lagu " +
                        "(${formatBytes(state.offline.totalBytes)}) " +
                        "akan dihapus dari perangkat."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAllDownloads()
                        onShowDeleteAll(false)
                    }
                ) {
                    Text(
                        "Hapus semua",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        onShowDeleteAll(false)
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
