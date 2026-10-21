package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Portrait-first shell. It owns only layout chrome; content, state, and actions remain
 * in RexMusicScreen's single controller.
 */
@Composable
internal fun RexMusicScreenPortrait(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        snackbarHost = snackbarHost,
        bottomBar = {
            if (selectedTab != TAB_SYNC) {
                Column {
                    miniPlayer()
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == TAB_HOME,
                            onClick = { onTabSelected(TAB_HOME) },
                            icon = { androidx.compose.material3.Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Beranda", maxLines = 1) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_SEARCH,
                            onClick = { onTabSelected(TAB_SEARCH) },
                            icon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Cari", maxLines = 1) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_PLAYLIST,
                            onClick = { onTabSelected(TAB_PLAYLIST) },
                            icon = { androidx.compose.material3.Icon(Icons.Default.QueueMusic, contentDescription = null) },
                            label = { Text("Playlist", maxLines = 1) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_LIBRARY,
                            onClick = { onTabSelected(TAB_LIBRARY) },
                            icon = { androidx.compose.material3.Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                            label = { Text("Koleksi", maxLines = 1) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_SYNC,
                            onClick = { onTabSelected(TAB_SYNC) },
                            icon = { androidx.compose.material3.Icon(Icons.Default.Groups, contentDescription = null) },
                            label = { Text("Sync", maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        content(
            Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
