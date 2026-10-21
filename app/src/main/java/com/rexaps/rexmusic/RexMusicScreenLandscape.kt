package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Landscape shell: a persistent navigation rail and a separate content/player region
 * make better use of horizontal space without stretching the portrait bottom bar.
 */
@Composable
internal fun RexMusicScreenLandscape(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        if (selectedTab != TAB_SYNC) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Rex",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                NavigationRailItem(
                    selected = selectedTab == TAB_HOME,
                    onClick = { onTabSelected(TAB_HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Beranda", maxLines = 1) },
                    alwaysShowLabel = false
                )
                NavigationRailItem(
                    selected = selectedTab == TAB_SEARCH,
                    onClick = { onTabSelected(TAB_SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Cari", maxLines = 1) },
                    alwaysShowLabel = false
                )
                NavigationRailItem(
                    selected = selectedTab == TAB_PLAYLIST,
                    onClick = { onTabSelected(TAB_PLAYLIST) },
                    icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                    label = { Text("Playlist", maxLines = 1) },
                    alwaysShowLabel = false
                )
                NavigationRailItem(
                    selected = selectedTab == TAB_LIBRARY,
                    onClick = { onTabSelected(TAB_LIBRARY) },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                    label = { Text("Koleksi", maxLines = 1) },
                    alwaysShowLabel = false
                )
                NavigationRailItem(
                    selected = selectedTab == TAB_SYNC,
                    onClick = { onTabSelected(TAB_SYNC) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { Text("Sync", maxLines = 1) },
                    alwaysShowLabel = false
                )
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = snackbarHost,
            bottomBar = {
                if (selectedTab != TAB_SYNC) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.widthIn(max = 1000.dp)) {
                            miniPlayer()
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 1440.dp)
                        .fillMaxSize()
                ) {
                    content(Modifier.fillMaxSize())
                }
            }
        }
    }
}
