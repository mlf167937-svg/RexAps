// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
