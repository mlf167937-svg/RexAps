// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ═════════════════════ Shared premium kit (dipakai portrait & landscape) ═════════════════════ */

internal val proGradients = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFFEC4899)),
    listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
    listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)),
    listOf(Color(0xFFF43F5E), Color(0xFFF97316))
)

internal fun proGradientFor(key: String): List<Color> =
    proGradients[key.hashCode().mod(proGradients.size)]

internal fun proFilterApps(apps: List<RexModule>, query: String, filter: String): List<RexModule> {
    val q = query.trim()
    return apps.filter { app ->
        val matchQuery = app.name.contains(q, ignoreCase = true) ||
            app.description.contains(q, ignoreCase = true)
        val matchFilter = when (filter) {
            "Tersedia" -> app.available
            "Segera" -> !app.available
            else -> true
        }
        matchQuery && matchFilter
    }
}

/** Efek tekan membal untuk elemen yang bisa diketuk. */
internal fun Modifier.proPress(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "proPress"
    )
    this.graphicsLayer { scaleX = s; scaleY = s }
        .clickable(
            interactionSource = src,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

/** Permukaan kaca: gradient halus dengan border tipis berwarna tema. */
@Composable
internal fun ProGlass(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(c.surfaceVariant.copy(alpha = 0.82f), c.surface.copy(alpha = 0.6f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(c.primary.copy(alpha = 0.30f), c.outlineVariant.copy(alpha = 0.20f))
                ),
                shape
            ),
        content = content
    )
}

/** Latar gradient radial yang bergerak pelan. */
@Composable
internal fun ProAmbientBackdrop(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "ambient")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ambientT"
    )
    Box(
        modifier.drawBehind {
            drawRect(c.background)
            drawRect(
                Brush.radialGradient(
                    listOf(c.primary.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * (0.2f + 0.5f * t), size.height * 0.12f),
                    radius = size.width * 0.8f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(c.tertiary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * (0.95f - 0.5f * t), size.height * 0.85f),
                    radius = size.width * 0.9f
                )
            )
        }
    )
}

@Composable
internal fun ProIcon(name: String, colors: List<Color>, dim: Dp) {
    val shape = RoundedCornerShape(dim * 0.34f)
    Box(
        Modifier
            .size(dim)
            .shadow(elevation = 12.dp, shape = shape, spotColor = colors.first().copy(alpha = 0.5f))
            .clip(shape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
internal fun ProStatusDot(available: Boolean) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (available) c.primaryContainer.copy(alpha = 0.7f) else c.outlineVariant.copy(alpha = 0.4f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (available) c.primary else c.onSurfaceVariant, CircleShape)
        )
        Text(
            text = if (available) "Tersedia" else "Segera",
            style = MaterialTheme.typography.labelSmall,
            color = if (available) c.onPrimaryContainer else c.onSurfaceVariant
        )
    }
}

@Composable
internal fun ProHomeHeader(
    appCount: Int,
    availableCount: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(7.dp).background(c.primary, CircleShape))
                Text(
                    text = "Selamat datang di",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onSurfaceVariant
                )
            }
            Text(
                text = "RexAps",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$appCount aplikasi · $availableCount siap dibuka",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant
            )
        }

        ProGlass(
            modifier = Modifier
                .size(52.dp)
                .proPress(onClick = onOpenSettings)
                .semantics { contentDescription = "Buka pengaturan" },
            radius = 26.dp
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.align(Alignment.Center).size(24.dp)
            )
        }
    }
}

@Composable
internal fun ProFeaturedCard(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grad = proGradientFor(app.name)
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .alpha(if (app.available) 1f else 0.6f)
            .shadow(elevation = 24.dp, shape = shape, spotColor = grad.first().copy(alpha = 0.45f))
            .clip(shape)
            .background(Brush.linearGradient(grad))
            .proPress(enabled = app.available, onClick = onClick)
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = size.width * 0.38f,
                    center = Offset(size.width * 0.92f, size.height * 0.05f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.width * 0.22f,
                    center = Offset(size.width * 0.78f, size.height)
                )
            }
            .padding(22.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        "Aplikasi utama",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (!app.available) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("Segera", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (app.available) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Buka",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = grad.first()
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = grad.first(),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProAppTile(
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

@Composable
internal fun ProSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    ProGlass(modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Cari", tint = c.primary)
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Cari aplikasi atau fitur…") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
internal fun ProFilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Semua", "Tersedia", "Segera").forEach { f ->
            FilterChip(
                selected = selected == f,
                onClick = { onSelect(f) },
                label = { Text(f) }
            )
        }
    }
}

@Composable
internal fun ProSectionHeader(title: String, subtitle: String, count: Int) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
        ProGlass(radius = 50.dp) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = c.primary
            )
        }
    }
}

@Composable
internal fun ProEmptyState() {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text("Aplikasi tidak ditemukan", style = MaterialTheme.typography.titleMedium)
        Text(
            "Coba kata kunci lain atau ubah filter.",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/* ═════════════════════ Shell portrait ═════════════════════ */

@Composable
internal fun RexAppsScreenPortrait(shell: RexShell) {
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

/* ═════════════════════ Navigasi bawah (pill kaca) ═════════════════════ */

@Composable
private fun ProPortraitNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = c.surfaceVariant.copy(alpha = 0.9f),
            shadowElevation = 20.dp,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, c.outline.copy(alpha = 0.25f))
        ) {
            Row(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomTabs.forEachIndexed { index, tab ->
                    ProNavItem(tab = tab, selected = index == selected, onClick = { onSelect(index) })
                }
            }
        }
    }
}

@Composable
private fun ProNavItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
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
private fun ProPortraitHome(shell: RexShell) {
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
private fun ProPortraitProfile(appCount: Int, themeLabel: String, isActive: Boolean) {
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