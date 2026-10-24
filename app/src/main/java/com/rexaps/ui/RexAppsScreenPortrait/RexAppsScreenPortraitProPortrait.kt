package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ProNavItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
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

/* ═════════════════════ Home portrait ═════════════════════ */

@Composable
internal fun ProPortraitHome(shell: RexShell) {
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

/* ═════════════════════ Profile portrait ═════════════════════ */

@Composable
internal fun ProPortraitProfile(appCount: Int, themeLabel: String, isActive: Boolean) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 130.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProGlass(Modifier.fillMaxWidth().entrance(0), radius = 32.dp) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SpinningAvatar(isActive = isActive, size = 124.dp)
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(label = "Aplikasi", value = "$appCount", modifier = Modifier.weight(1f))
                    StatCard(label = "Tema", value = themeLabel, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Column(Modifier.fillMaxWidth().entrance(1)) {
            SectionHeader(
                title = "Ikuti & Dukung Rex",
                subtitle = "Temukan aku di platform favoritmu"
            )
        }

        Spacer(Modifier.height(14.dp))

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            socialLinks.forEachIndexed { index, link ->
                SocialLinkCard(link = link, modifier = Modifier.entrance(2 + index))
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Dibuat dengan ❤ oleh Rex",
            style = MaterialTheme.typography.labelSmall,
            color = c.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
