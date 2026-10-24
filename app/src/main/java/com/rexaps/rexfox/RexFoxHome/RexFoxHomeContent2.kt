package com.rexaps.rexfox

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable internal
fun __RexFoxHomeContent(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onTabs: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        RexFoxHomeLandscape(
            state = state,
            onNavigate = onNavigate,
            onTabs = onTabs,
            onBookmarks = onBookmarks,
            onHistory = onHistory,
            onDownloads = onDownloads,
            onSettings = onSettings
        )
        return
    }

    var query by remember { mutableStateOf("") }
    val glowPrimary = Violet
    val glowSecondary = Cyan

    Box(
        Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.85f, 0f),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.85f, 0f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(0f, size.height * 0.35f),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(0f, size.height * 0.35f)
                )
            }
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 34.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HomeHeader(
                    onSettings = onSettings,
                    modifier = Modifier.rexEntrance(0)
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .rexEntrance(1),
                    placeholder = { Text("Search or enter address") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = VioletLight) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onNavigate(query) }) {
                                Icon(Icons.Default.ArrowForward, "Go", tint = Cyan)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = Cyan
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onNavigate(query) })
                )
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .rexEntrance(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAction("Bookmarks", Icons.Default.Bookmark, VioletLight, onBookmarks)
                    QuickAction("History", Icons.Default.History, Cyan, onHistory)
                    QuickAction("Downloads", Icons.Default.Download, VioletLight, onDownloads)
                    QuickAction("Tabs", Icons.Default.Tab, Cyan, onTabs)
                }
            }

            item {
                PrivacyCard(
                    status = state.privacyStatus,
                    modifier = Modifier.rexEntrance(3)
                )
            }

            if (state.bookmarks.isNotEmpty()) {
                item { SectionLabel("BOOKMARKS", Modifier.rexEntrance(4)) }
                items(state.bookmarks.take(6), key = { it.id }) { b ->
                    Entry(b.title, b.url, Icons.Default.Bookmark) { onNavigate(b.url) }
                }
            }

            if (state.history.isNotEmpty()) {
                item { SectionLabel("RECENT", Modifier.rexEntrance(5)) }
                items(state.history.take(8), key = { it.timestamp }) { h ->
                    Entry(h.title, h.url, Icons.Default.History) { onNavigate(h.url) }
                }
            }
        }
    }
}
