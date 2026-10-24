package com.rexaps.rexfox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Desktop-like wide layout: navigation/search on the left, activity lists on the right. */
@Composable
internal fun RexFoxHomeLandscape(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onTabs: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    // Resolve theme-dependent composable colors before entering the non-composable draw scope.
    val glowPrimary = Violet
    val glowSecondary = Cyan
    Box(
        Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width * 0.18f, 0f),
                        radius = size.height * 1.1f
                    ),
                    radius = size.height * 1.1f,
                    center = Offset(size.width * 0.18f, 0f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.height * 0.9f
                    ),
                    radius = size.height * 0.9f,
                    center = Offset(size.width, size.height)
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(
                Modifier.weight(0.95f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HomeHeader(onSettings = onSettings)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari atau masukkan alamat") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = VioletLight) },
                    trailingIcon = {
                        if (query.isNotBlank()) IconButton(onClick = { onNavigate(query.trim()) }) {
                            Icon(Icons.Default.ArrowForward, "Buka alamat", tint = Cyan)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = Cyan
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onNavigate(query.trim()) })
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAction("Bookmark", Icons.Default.Bookmark, VioletLight, onBookmarks)
                    QuickAction("Riwayat", Icons.Default.History, Cyan, onHistory)
                    QuickAction("Unduhan", Icons.Default.Download, VioletLight, onDownloads)
                    QuickAction("Tab", Icons.Default.Tab, Cyan, onTabs)
                }
                PrivacyCard(status = state.privacyStatus, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                Text(
                    "REXFOX  •  FAST, FOCUSED, DEV-READY",
                    color = OnSurfaceMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp
                )
            }

            LazyColumn(
                Modifier.weight(1.05f).fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.bookmarks.isNotEmpty()) {
                    item { SectionLabel("BOOKMARKS") }
                    items(state.bookmarks.take(8), key = { it.id }) { bookmark ->
                        Entry(bookmark.title, bookmark.url, Icons.Default.Bookmark) { onNavigate(bookmark.url) }
                    }
                }
                if (state.history.isNotEmpty()) {
                    item { SectionLabel("RECENT ACTIVITY") }
                    items(state.history.take(10), key = { it.timestamp }) { history ->
                        Entry(history.title, history.url, Icons.Default.History) { onNavigate(history.url) }
                    }
                }
                if (state.bookmarks.isEmpty() && state.history.isEmpty()) {
                    item {
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Explore, null, tint = Cyan, modifier = Modifier.size(28.dp))
                                Text("Ruang jelajahmu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Bookmark dan halaman terakhir akan muncul di sini.", color = OnSurfaceMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
