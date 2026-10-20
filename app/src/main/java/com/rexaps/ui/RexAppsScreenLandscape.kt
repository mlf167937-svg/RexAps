// RexAppsScreenLandscape.kt
package com.rexaps.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/* ═════════════════════ Shell landscape ═════════════════════ */

@Composable
internal fun RexAppsScreenLandscape(shell: RexShell) {

    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.20f),
                            colors.tertiary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            LandscapeRail(
                selected = shell.selectedTab,
                onSelect = shell.onSelectTab
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Crossfade(
                    targetState = shell.selectedTab,
                    animationSpec = tween(320),
                    label = "tabLandscape"
                ) { tab ->
                    val noPadding = PaddingValues(0.dp)
                    when (tab) {
                        RexTab.HOME -> LandscapeHome(shell = shell)

                        RexTab.PROFILE -> LandscapeProfile(
                            appCount = RexAppRegistry.modules.size,
                            themeLabel = shell.themeOption.label,
                            isActive = shell.selectedTab == RexTab.PROFILE
                        )

                        RexTab.THEME -> ThemeTabContent(
                            padding = noPadding,
                            selectedTheme = shell.themeOption,
                            onOpenTheme = shell.onOpenTheme
                        )

                        RexTab.SETTINGS -> RexSettingsScreen(
                            padding = noPadding,
                            selectedTheme = shell.themeOption,
                            onOpenTheme = shell.onOpenTheme,
                            onBackHome = { shell.onSelectTab(RexTab.HOME) }
                        )

                        else -> RexLocalAccountScreen(padding = noPadding)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeRail(selected: Int, onSelect: (Int) -> Unit) {

    val colors = MaterialTheme.colorScheme

    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = colors.surfaceVariant.copy(alpha = 0.55f),
        header = {
            AppIcon(
                letter = "R",
                size = 44.dp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    ) {
        bottomTabs.forEachIndexed { index, tab ->
            NavigationRailItem(
                selected = index == selected,
                onClick = { onSelect(index) },
                icon = {
                    Icon(imageVector = tab.icon, contentDescription = tab.label)
                },
                label = { Text(tab.label) },
                alwaysShowLabel = false
            )
        }
    }
}

/* ═════════════════════ Home landscape ═════════════════════ */

@Composable
private fun LandscapeHome(shell: RexShell) {

    val colors = MaterialTheme.colorScheme
    val apps = RexAppRegistry.modules
    val featured = apps.firstOrNull { it.id == "rexfox" }
    val others = apps.filter { it.id != "rexfox" }
    val availableCount = apps.count { it.available }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var appFilter by rememberSaveable { mutableStateOf("Semua") }

    val query = searchQuery.trim()
    val filteredApps = others.filter { app ->
        val matchesQuery = app.name.contains(query, ignoreCase = true) ||
            app.description.contains(query, ignoreCase = true)
        val matchesFilter = when (appFilter) {
            "Tersedia" -> app.available
            "Segera" -> !app.available
            else -> true
        }
        matchesQuery && matchesFilter
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHeader(
                onOpenSettings = { shell.onSelectTab(RexTab.SETTINGS) },
                modifier = Modifier.entrance(0)
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(1),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                HeroCard(
                    appCount = apps.size,
                    availableCount = availableCount,
                    themeLabel = shell.themeOption.label,
                    isActive = shell.selectedTab == RexTab.HOME,
                    modifier = Modifier.weight(1f)
                )
                if (featured != null) {
                    FeaturedCard(
                        app = featured,
                        isActive = shell.selectedTab == RexTab.HOME,
                        onClick = { shell.onAppClick(featured) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (searchQuery.isBlank()) "Jelajahi aplikasi" else "Hasil pencarian",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = when (appFilter) {
                                "Tersedia" -> "Aplikasi yang bisa dibuka sekarang"
                                "Segera" -> "Aplikasi yang akan hadir"
                                else -> "Semua tools dalam satu tempat"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Semua", "Tersedia", "Segera").forEach { filter ->
                            FilterChip(
                                selected = appFilter == filter,
                                onClick = { appFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(3),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Cari aplikasi atau fitur...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari aplikasi",
                            tint = colors.primary
                        )
                    }
                )
            }
        }

        if (filteredApps.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptySearchState()
            }
        } else {
            itemsIndexed(filteredApps, key = { _, app -> app.id }) { index, app ->
                AppCard(
                    app = app,
                    onClick = { shell.onAppClick(app) },
                    modifier = Modifier.entrance(index + 4)
                )
            }
        }
    }
}

/* ═════════════════════ Profile landscape ═════════════════════ */

@Composable
private fun LandscapeProfile(appCount: Int, themeLabel: String, isActive: Boolean) {

    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(colors.primary.copy(alpha = 0.12f), colors.background)
                )
            )
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SpinningAvatar(isActive = isActive, size = 128.dp)

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Profile RexAps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.entrance(0)
            )
            Text(
                text = "Kelola aplikasi dan preferensimu",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.entrance(1)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Aplikasi", value = "$appCount", modifier = Modifier.weight(1f))
                StatCard(label = "Tema", value = themeLabel, modifier = Modifier.weight(1f))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Ikuti & Dukung Rex",
                subtitle = "Temukan aku di platform favoritmu"
            )
            Spacer(Modifier.height(6.dp))

            socialLinks.forEachIndexed { index, link ->
                SocialLinkCard(link = link, modifier = Modifier.entrance(3 + index))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Dibuat dengan ❤ oleh Rex",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}