package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
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

/* ═════════════════════ Navigasi bawah (pill kaca) ═════════════════════ */

@Composable
internal fun ProPortraitNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
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
