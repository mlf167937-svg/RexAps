// RexAppsScreenLandscape.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/* ═════════════════════ Shell landscape ═════════════════════ */

@Composable
internal fun RexAppsScreenLandscape(shell: RexShell) {
    Box(Modifier.fillMaxSize()) {
        ProAmbientBackdrop(Modifier.fillMaxSize())

        Row(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            ProRail(selected = shell.selectedTab, onSelect = shell.onSelectTab)

            Crossfade(
                targetState = shell.selectedTab,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                animationSpec = tween(320),
                label = "tabLandscape"
            ) { tab ->
                val noPadding = PaddingValues(0.dp)
                when (tab) {
                    RexTab.HOME -> ProLandscapeHome(shell)

                    RexTab.PROFILE -> ProLandscapeProfile(
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

/* ═════════════════════ Rail kiri ═════════════════════ */

@Composable
private fun ProRail(selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxHeight()
            .width(92.dp)
            .background(c.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppIcon(letter = "R", size = 46.dp)
        Spacer(Modifier.height(18.dp))
        bottomTabs.forEachIndexed { index, tab ->
            ProRailItem(tab = tab, selected = index == selected, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun ProRailItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) c.primaryContainer else Color.Transparent,
        animationSpec = tween(300),
        label = "railBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) c.onPrimaryContainer else c.onSurfaceVariant,
        animationSpec = tween(300),
        label = "railFg"
    )

    Column(
        Modifier
            .width(70.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = fg,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ═════════════════════ Home landscape: dua panel ═════════════════════ */

@Composable
private fun ProLandscapeHome(shell: RexShell) {
    val apps = RexAppRegistry.modules
    val featured = apps.firstOrNull { it.id == "rexfox" }
    val others = apps.filter { it.id != "rexfox" }
    val availableCount = apps.count { it.available }

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Semua") }
    val filtered = proFilterApps(others, query, filter)

    Row(Modifier.fillMaxSize()) {

        // Panel kiri: identitas, banner, featured
        Column(
            Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ProHomeHeader(
                appCount = apps.size,
                availableCount = availableCount,
                onOpenSettings = { shell.onSelectTab(RexTab.SETTINGS) },
                modifier = Modifier.entrance(0)
            )

            RexBannerCarousel(modifier = Modifier.fillMaxWidth().entrance(1))

            if (featured != null) {
                ProFeaturedCard(
                    app = featured,
                    onClick = { shell.onAppClick(featured) },
                    modifier = Modifier.entrance(2)
                )
            }
        }

        // Panel kanan: pencarian + grid aplikasi
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 176.dp),
            modifier = Modifier.weight(0.58f).fillMaxHeight(),
            contentPadding = PaddingValues(start = 12.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().entrance(3),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProSearchBar(query = query, onQuery = { query = it })
                    ProFilterRow(selected = filter, onSelect = { filter = it })
                    ProSectionHeader(
                        title = if (query.isBlank()) "Jelajahi aplikasi" else "Hasil pencarian",
                        subtitle = when (filter) {
                            "Tersedia" -> "Aplikasi yang bisa dibuka sekarang"
                            "Segera" -> "Aplikasi yang akan hadir"
                            else -> "Semua tools dalam satu tempat"
                        },
                        count = filtered.size
                    )
                }
            }

            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { ProEmptyState() }
            } else {
                itemsIndexed(filtered, key = { _, app -> app.id }) { index, app ->
                    ProAppTile(
                        app = app,
                        onClick = { shell.onAppClick(app) },
                        modifier = Modifier.entrance(index + 4)
                    )
                }
            }
        }
    }
}

/* ═════════════════════ Profile landscape ═════════════════════ */

@Composable
private fun ProLandscapeProfile(appCount: Int, themeLabel: String, isActive: Boolean) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ProGlass(
            Modifier.weight(0.4f).fillMaxHeight().entrance(0),
            radius = 32.dp
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpinningAvatar(isActive = isActive, size = 132.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "RexAps Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Kelola aplikasi dan preferensimu",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(label = "Aplikasi", value = "$appCount", modifier = Modifier.weight(1f))
                    StatCard(label = "Tema", value = themeLabel, modifier = Modifier.weight(1f))
                }
            }
        }

        Column(
            Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "Ikuti & Dukung Rex",
                subtitle = "Temukan aku di platform favoritmu"
            )
            socialLinks.forEachIndexed { index, link ->
                SocialLinkCard(link = link, modifier = Modifier.entrance(index + 1))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Dibuat dengan ❤ oleh Rex",
                style = MaterialTheme.typography.labelSmall,
                color = c.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}