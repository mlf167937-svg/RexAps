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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_ProPortraitHome(shell: RexShell) {
    val apps = RexAppRegistry.modules
    val featured = apps.firstOrNull { it.id == "rexfox" }
    val others = apps.filter { it.id != "rexfox" }
    val availableCount = apps.count { it.available }

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Semua") }
    val filtered = proFilterApps(others, query, filter)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProHomeHeader(
                appCount = apps.size,
                availableCount = availableCount,
                onOpenSettings = { shell.onSelectTab(RexTab.SETTINGS) },
                modifier = Modifier.entrance(0)
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            RexBannerCarousel(modifier = Modifier.fillMaxWidth().entrance(1))
        }

        if (featured != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProFeaturedCard(
                    app = featured,
                    onClick = { shell.onAppClick(featured) },
                    modifier = Modifier.entrance(2)
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.fillMaxWidth().entrance(3),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProSearchBar(query = query, onQuery = { query = it })
                ProFilterRow(selected = filter, onSelect = { filter = it })
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
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

@Composable
internal fun __splitImpl_RexAppsScreenPortrait(shell: RexShell) {
    Box(Modifier.fillMaxSize()) {
        ProAmbientBackdrop(Modifier.fillMaxSize())

        Crossfade(
            targetState = shell.selectedTab,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            animationSpec = tween(320),
            label = "tabPortrait"
        ) { tab ->
            // Ruang di bawah agar konten tidak tertutup navigasi mengambang.
            val bottomSafe = PaddingValues(bottom = 120.dp)
            when (tab) {
                RexTab.HOME -> ProPortraitHome(shell)

                RexTab.PROFILE -> ProPortraitProfile(
                    appCount = RexAppRegistry.modules.size,
                    themeLabel = shell.themeOption.label,
                    isActive = shell.selectedTab == RexTab.PROFILE
                )

                RexTab.THEME -> ThemeTabContent(
                    padding = bottomSafe,
                    selectedTheme = shell.themeOption,
                    onOpenTheme = shell.onOpenTheme
                )

                RexTab.SETTINGS -> RexSettingsScreen(
                    padding = bottomSafe,
                    selectedTheme = shell.themeOption,
                    onOpenTheme = shell.onOpenTheme,
                    onBackHome = { shell.onSelectTab(RexTab.HOME) }
                )

                else -> RexLocalAccountScreen(padding = bottomSafe)
            }
        }

        ProPortraitNav(
            selected = shell.selectedTab,
            onSelect = shell.onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
internal fun __splitImpl_ProNavItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) c.primary else Color.Transparent,
        animationSpec = tween(300),
        label = "pnBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) c.onPrimary else c.onSurfaceVariant,
        animationSpec = tween(300),
        label = "pnFg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pnScale"
    )

    Row(
        Modifier
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 16.dp else 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = fg,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
        )
        if (selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun __splitImpl_ProAppTile(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProGlass(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .alpha(if (app.available) 1f else 0.55f)
            .proPress(enabled = app.available, onClick = onClick),
        radius = 26.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProIcon(app.name, proGradientFor(app.name), 50.dp)
                ProStatusDot(app.available)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
