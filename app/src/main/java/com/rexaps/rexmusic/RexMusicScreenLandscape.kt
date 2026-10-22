package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Landscape shell: a deliberately narrow navigation rail and a centered content/player
 * stage. It uses the existing Rex palette and keeps playback/state ownership in the controller.
 */
@Composable
internal fun RexMusicScreenLandscape(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize().background(scheme.background)) {
        val showNavigationLabels = maxWidth >= 900.dp
        val railWidth = if (showNavigationLabels) 112.dp else 84.dp

        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = selectedTab != TAB_SYNC,
                enter = fadeIn(tween(180)) + slideInHorizontally(tween(230)) { -it / 3 },
                exit = fadeOut(tween(130)) + slideOutHorizontally(tween(180)) { -it / 4 }
            ) {
                Column(
                    Modifier
                        .fillMaxHeight()
                        .width(railWidth)
                        .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(scheme.surface, scheme.surface.copy(alpha = 0.94f))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = scheme.outlineVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(scheme.primary, scheme.tertiary)
                                )
                            )
                            .size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "R",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onPrimary,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        "Rex",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        modifier = Modifier.padding(top = 5.dp, bottom = 14.dp)
                    )

                    val railColors = NavigationRailItemDefaults.colors(
                        selectedIconColor = scheme.primary,
                        selectedTextColor = scheme.primary,
                        indicatorColor = scheme.primary.copy(alpha = 0.14f),
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant
                    )
                    NavigationRail(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        containerColor = Color.Transparent
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == TAB_HOME,
                            onClick = { onTabSelected(TAB_HOME) },
                            icon = { LandscapeNavigationIcon(Icons.Default.Home, "Beranda", selectedTab == TAB_HOME) },
                            label = { Text("Beranda", maxLines = 1) },
                            alwaysShowLabel = showNavigationLabels,
                            colors = railColors
                        )
                        NavigationRailItem(
                            selected = selectedTab == TAB_SEARCH,
                            onClick = { onTabSelected(TAB_SEARCH) },
                            icon = { LandscapeNavigationIcon(Icons.Default.Search, "Cari", selectedTab == TAB_SEARCH) },
                            label = { Text("Cari", maxLines = 1) },
                            alwaysShowLabel = showNavigationLabels,
                            colors = railColors
                        )
                        NavigationRailItem(
                            selected = selectedTab == TAB_PLAYLIST,
                            onClick = { onTabSelected(TAB_PLAYLIST) },
                            icon = { LandscapeNavigationIcon(Icons.Default.QueueMusic, "Playlist", selectedTab == TAB_PLAYLIST) },
                            label = { Text("Playlist", maxLines = 1) },
                            alwaysShowLabel = showNavigationLabels,
                            colors = railColors
                        )
                        NavigationRailItem(
                            selected = selectedTab == TAB_LIBRARY,
                            onClick = { onTabSelected(TAB_LIBRARY) },
                            icon = { LandscapeNavigationIcon(Icons.Default.LibraryMusic, "Koleksi", selectedTab == TAB_LIBRARY) },
                            label = { Text("Koleksi", maxLines = 1) },
                            alwaysShowLabel = showNavigationLabels,
                            colors = railColors
                        )
                        NavigationRailItem(
                            selected = selectedTab == TAB_SYNC,
                            onClick = { onTabSelected(TAB_SYNC) },
                            icon = { LandscapeNavigationIcon(Icons.Default.Groups, "Sync", selectedTab == TAB_SYNC) },
                            label = { Text("Sync", maxLines = 1) },
                            alwaysShowLabel = showNavigationLabels,
                            colors = railColors
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = Color.Transparent,
                snackbarHost = snackbarHost,
                bottomBar = {
                    AnimatedVisibility(
                        visible = selectedTab != TAB_SYNC,
                        enter = fadeIn(tween(180)) + slideInHorizontally(tween(240)) { it / 5 },
                        exit = fadeOut(tween(120)) + slideOutHorizontally(tween(150)) { it / 8 }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(scheme.background.copy(alpha = 0.96f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .widthIn(max = 1000.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
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
}

@Composable
private fun LandscapeNavigationIcon(
    image: ImageVector,
    description: String,
    selected: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.09f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "landscapeNavigationIconScale"
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
