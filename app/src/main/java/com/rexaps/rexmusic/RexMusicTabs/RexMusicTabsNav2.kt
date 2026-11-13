package com.rexaps.rexmusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
internal fun __splitImpl_HomeTab(
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val offlineMode = state.offline.enabled
    val glow by animateColorAsState(
        if (offlineMode) scheme.tertiary else scheme.primary, tween(400), label = "homeGlow"
    )
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    Box(
        modifier.fillMaxSize().drawBehind {
            val h = 320.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    listOf(glow.copy(alpha = 0.30f), Color.Transparent), startY = 0f, endY = h
                ),
                size = Size(size.width, h)
            )
        }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "home-header") {
                HomeHeader(
                    greeting = greetingForHour(hour),
                    offline = offlineMode,
                    onToggle = { actions.setOffline(!offlineMode) },
                    onBack = onBack
                )
            }
            if (offlineMode) {
                offlineSection(state, actions, query = "", full = true)
            } else {
                onlineHome(state, actions)
            }
        }
    }
}

@Composable
internal fun __splitImpl_LibraryTab(
    state: RexMusicUiState,
    libTab: Int,
    onLibTab: (Int) -> Unit,
    actions: RexActions,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val offlineMode = state.offline.enabled
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "lib-header") {
            TabHeader("Koleksi", offlineMode) { actions.setOffline(!offlineMode) }
        }
        item(key = "lib-chips") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryChip("Unduhan", state.offline.entries.size, libTab == 0) { onLibTab(0) }
                LibraryChip("Antrian", state.userQueue.size, libTab == 1) { onLibTab(1) }
                LibraryChip("Riwayat", state.history.size, libTab == 2) { onLibTab(2) }
            }
        }
        when (libTab) {
            0 -> offlineSection(state, actions, query = "", full = true)
            1 -> queueSection(state, actions, showNow = false)
            else -> historySection(state, actions)
        }
    }
}

@Composable
internal fun __splitImpl_SearchTab(
    state: RexMusicUiState,
    query: String,
    typing: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    actions: RexActions,
    listState: LazyListState,
    onAddMusic: () -> Unit = {},
    modifier: Modifier = Modifier,
    providerFilter: RexTrackSource? = null,
    onProviderFilterChange: (RexTrackSource?) -> Unit = {},
    searchHistory: List<String> = emptyList(),
    onSelectHistory: (String) -> Unit = {},
    onRemoveHistory: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val offlineMode = state.offline.enabled
    val q = query.trim()
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "search-header") {
            val scheme = MaterialTheme.colorScheme
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Cari", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                RexIconButton(
                    icon = androidx.compose.material.icons.Icons.Default.Add,
                    description = "Tambah musik",
                    onClick = onAddMusic,
                    size = 36.dp,
                    iconSize = 19.dp,
                    tint = scheme.onSurfaceVariant,
                    background = scheme.surfaceVariant.copy(alpha = 0.42f)
                )
                Spacer(Modifier.width(8.dp))
                ModeChip(offlineMode, onToggle = { actions.setOffline(!offlineMode) })
                Spacer(Modifier.width(8.dp))
                RexMusicProfileAvatarButton()
            }
        }
        item(key = "search-field") {
            SearchField(
                query, onQueryChange, onClear,
                if (offlineMode) "Cari di lagu offline" else "Lagu, artis, atau album",
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (!offlineMode) {
            item(key = "provider-filters") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = providerFilter == null, onClick = { onProviderFilterChange(null) }, label = { Text("Semua") })
                    listOf(RexTrackSource.SPOTIFY, RexTrackSource.APPLE_MUSIC, RexTrackSource.YOUTUBE).forEach { source ->
                        FilterChip(
                            selected = providerFilter == source,
                            onClick = { onProviderFilterChange(source) },
                            label = { Text(source.label) }
                        )
                    }
                }
            }
        }
        when {
            offlineMode -> offlineSection(state, actions, q, full = false)
            q.isEmpty() -> {
                if (searchHistory.isNotEmpty()) {
                    item(key = "search-history-title") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("Pencarian terbaru", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = onClearHistory) { Text("Hapus semua") }
                        }
                    }
                    items(searchHistory, key = { "query-${it.lowercase()}" }) { savedQuery ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelectHistory(savedQuery) }
                                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(savedQuery, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(onClick = { onRemoveHistory(savedQuery) }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus $savedQuery")
                            }
                        }
                    }
                }
                exploreSection(state, actions)
            }
            else -> resultsSection(state, actions, typing, onRetry)
        }
    }
}
