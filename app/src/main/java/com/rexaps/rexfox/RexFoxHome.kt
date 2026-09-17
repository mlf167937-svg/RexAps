package com.rexaps.rexfox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RexFoxHome(
    onSearch: (String) -> Unit,
    onSettings: () -> Unit,
    historyEntries: List<HistoryEntry> = emptyList(),
    bookmarks: List<Bookmark> = emptyList()
) {
    var query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DeepBlack, Color(0xFF0D0D1A)))
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(Modifier.height(52.dp))
                // ── Brand Row ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(0xFF1E1030)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("R", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold, color = VioletLight)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("RexFox", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold, color = OnSurfacePrimary)
                            Text("Private · Fast · Dev-Ready", fontSize = 11.sp,
                                color = OnSurfaceMuted)
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = OnSurfaceMuted)
                    }
                }

                Spacer(Modifier.height(44.dp))

                Text("Good to have you back.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
                Spacer(Modifier.height(4.dp))
                Text("Where are we going today?", fontSize = 14.sp, color = OnSurfaceMuted)
                Spacer(Modifier.height(24.dp))
            }

            item {
                // ── Search bar ──
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    singleLine = true,
                    placeholder = { Text("Search or enter URL", color = OnSurfaceMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = OnSurfaceMuted)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onSearch(query) }) {
                                Icon(Icons.Default.ArrowForward, "Go", tint = Cyan)
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = OnSurfacePrimary,
                        unfocusedTextColor = OnSurfacePrimary
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { onSearch(query) }
                    )
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                // ── Privacy badge ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0A2030)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, null, tint = Cyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("RexFox Protection", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = OnSurfacePrimary)
                            Text("Tracking protection is active.", fontSize = 11.sp,
                                color = OnSurfaceMuted)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── Bookmarks ──
            if (bookmarks.isNotEmpty()) {
                item {
                    SectionLabel("Bookmarks")
                    Spacer(Modifier.height(10.dp))
                }
                items(bookmarks.take(5)) { bm ->
                    BookmarkRow(bm) { onSearch(bm.url) }
                    Spacer(Modifier.height(6.dp))
                }
                item { Spacer(Modifier.height(20.dp)) }
            }

            // ── History ──
            if (historyEntries.isNotEmpty()) {
                item {
                    SectionLabel("Recent")
                    Spacer(Modifier.height(10.dp))
                }
                items(historyEntries.take(8)) { entry ->
                    HistoryRow(entry) { onSearch(entry.url) }
                    Spacer(Modifier.height(6.dp))
                }
            }

            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        color = OnSurfaceMuted, letterSpacing = 1.sp)
}

@Composable
fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, fontSize = 13.sp, color = OnSurfacePrimary,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(entry.formattedTime(), fontSize = 10.sp, color = OnSurfaceMuted)
        }
    }
}

@Composable
fun BookmarkRow(bm: Bookmark, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Bookmark, null, tint = VioletLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(bm.title, fontSize = 13.sp, color = OnSurfacePrimary,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(bm.url, fontSize = 10.sp, color = OnSurfaceMuted,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}
