// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/* ═════════════════════ Shell portrait ═════════════════════ */

@Composable
internal fun RexAppsScreenPortrait(shell: RexShell) {

    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
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

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = colors.onBackground,
            bottomBar = {
                PortraitNavBar(
                    selected = shell.selectedTab,
                    onSelect = shell.onSelectTab
                )
            }
        ) { padding ->
            Crossfade(
                targetState = shell.selectedTab,
                animationSpec = tween(320),
                label = "tabPortrait"
            ) { tab ->
                when (tab) {
                    RexTab.HOME -> PortraitHome(
                        padding = padding,
                        themeLabel = shell.themeOption.label,
                        isActive = shell.selectedTab == RexTab.HOME,
                        onAppClick = shell.onAppClick,
                        onOpenSettings = { shell.onSelectTab(RexTab.SETTINGS) }
                    )

                    RexTab.PROFILE -> PortraitProfile(
                        padding = padding,
                        appCount = RexAppRegistry.modules.size,
                        themeLabel = shell.themeOption.label,
                        isActive = shell.selectedTab == RexTab.PROFILE
                    )

                    RexTab.THEME -> ThemeTabContent(
                        padding = padding,
                        selectedTheme = shell.themeOption,
                        onOpenTheme = shell.onOpenTheme
                    )

                    RexTab.SETTINGS -> RexSettingsScreen(
                        padding = padding,
                        selectedTheme = shell.themeOption,
                        onOpenTheme = shell.onOpenTheme,
                        onBackHome = { shell.onSelectTab(RexTab.HOME) }
                    )

                    else -> RexLocalAccountScreen(padding = padding)
                }
            }
        }
    }
}

/* ═════════════════════ Navigasi bawah (mengambang) ═════════════════════ */

@Composable
private fun PortraitNavBar(selected: Int, onSelect: (Int) -> Unit) {

    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = colors.surfaceVariant.copy(alpha = 0.96f),
            shadowElevation = 18.dp,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomTabs.forEachIndexed { index, tab ->
                    PortraitNavItem(
                        tab = tab,
                        selected = index == selected,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitNavItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {

    val colors = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = if (selected) colors.primary else Color.Transparent,
        animationSpec = tween(300),
        label = "navBg"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = tween(300),
        label = "navFg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIconScale"
    )

    Row(
        modifier = Modifier
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = foreground,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            }
        )
        if (selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = foreground,
                maxLines = 1
            )
        }
    }
}

/* ═════════════════════ Home portrait ═════════════════════ */

@Composable
private fun PortraitHome(
    padding: PaddingValues,
    themeLabel: String,
    isActive: Boolean,
    onAppClick: (RexModule) -> Unit,
    onOpenSettings: () -> Unit
) {
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
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHeader(onOpenSettings = onOpenSettings, modifier = Modifier.entrance(0))
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroCard(
                appCount = apps.size,
                availableCount = availableCount,
                themeLabel = themeLabel,
                isActive = isActive,
                modifier = Modifier.entrance(1)
            )
        }

        if (featured != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCard(
                    app = featured,
                    isActive = isActive,
                    onClick = { onAppClick(featured) },
                    modifier = Modifier.entrance(2)
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .entrance(5),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "${filteredApps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
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
                    onClick = { onAppClick(app) },
                    modifier = Modifier.entrance(index + 6)
                )
            }
        }
    }
}

@Composable
internal fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Aplikasi tidak ditemukan", style = MaterialTheme.typography.titleMedium)
        Text(
            "Coba kata kunci lain atau ubah filter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/* ═════════════════════ Profile portrait ═════════════════════ */

@Composable
private fun PortraitProfile(
    padding: PaddingValues,
    appCount: Int,
    themeLabel: String,
    isActive: Boolean
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.primary.copy(alpha = 0.14f), colors.background, colors.background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            SpinningAvatar(isActive = isActive, size = 132.dp)

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Profile RexAps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.entrance(1)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Kelola aplikasi dan preferensimu",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.entrance(1)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Aplikasi", value = "$appCount", modifier = Modifier.weight(1f))
                StatCard(label = "Tema", value = themeLabel, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(3)
            ) {
                SectionHeader(
                    title = "Ikuti & Dukung Rex",
                    subtitle = "Temukan aku di platform favoritmu"
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                socialLinks.forEachIndexed { index, link ->
                    SocialLinkCard(link = link, modifier = Modifier.entrance(4 + index))
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Dibuat dengan ❤ oleh Rex",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}