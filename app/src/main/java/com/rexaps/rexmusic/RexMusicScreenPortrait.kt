package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

private val PortraitNavigationShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

/**
 * Portrait-first shell. It owns layout chrome only; all music state and actions remain
 * in RexMusicScreen's single controller. Motion here is short, purposeful, and bounded.
 */
@Composable
internal fun RexMusicScreenPortrait(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.background,
        snackbarHost = snackbarHost,
        bottomBar = {
            AnimatedVisibility(
                visible = selectedTab != TAB_SYNC,
                enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 3 },
                exit = fadeOut(tween(130)) + slideOutVertically(tween(180)) { it / 4 }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(PortraitNavigationShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    scheme.surface.copy(alpha = 0.98f),
                                    scheme.surface
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = scheme.outlineVariant.copy(alpha = 0.22f),
                            shape = PortraitNavigationShape
                        )
                ) {
                    miniPlayer()
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = scheme.primary,
                            selectedTextColor = scheme.primary,
                            indicatorColor = scheme.primary.copy(alpha = 0.13f),
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_HOME,
                            onClick = { onTabSelected(TAB_HOME) },
                            icon = { PortraitNavigationIcon(Icons.Default.Home, "Beranda", selectedTab == TAB_HOME) },
                            label = { Text("Beranda", maxLines = 1) },
                            colors = itemColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_SEARCH,
                            onClick = { onTabSelected(TAB_SEARCH) },
                            icon = { PortraitNavigationIcon(Icons.Default.Search, "Cari", selectedTab == TAB_SEARCH) },
                            label = { Text("Cari", maxLines = 1) },
                            colors = itemColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_PLAYLIST,
                            onClick = { onTabSelected(TAB_PLAYLIST) },
                            icon = { PortraitNavigationIcon(Icons.Default.QueueMusic, "Playlist", selectedTab == TAB_PLAYLIST) },
                            label = { Text("Playlist", maxLines = 1) },
                            colors = itemColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_LIBRARY,
                            onClick = { onTabSelected(TAB_LIBRARY) },
                            icon = { PortraitNavigationIcon(Icons.Default.LibraryMusic, "Koleksi", selectedTab == TAB_LIBRARY) },
                            label = { Text("Koleksi", maxLines = 1) },
                            colors = itemColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == TAB_SYNC,
                            onClick = { onTabSelected(TAB_SYNC) },
                            icon = { PortraitNavigationIcon(Icons.Default.Groups, "Sync", selectedTab == TAB_SYNC) },
                            label = { Text("Sync", maxLines = 1) },
                            colors = itemColors
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

@Composable
private fun PortraitNavigationIcon(
    image: ImageVector,
    description: String,
    selected: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "portraitNavigationIconScale"
    )
    Icon(
        imageVector = image,
        contentDescription = description,
        modifier = Modifier
            .size(23.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}
