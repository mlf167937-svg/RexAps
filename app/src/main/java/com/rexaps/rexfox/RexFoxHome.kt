package com.rexaps.rexfox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RexFoxHome(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onTabs: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().background(DeepBlack).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 34.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("R", color = VioletLight, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("RexFox", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("Fast. Focused. Dev-ready.", color = OnSurfaceMuted, fontSize = 11.sp)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Search or enter address") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton({ onNavigate(query) }) {
                        Icon(Icons.Default.ArrowForward, "Go")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { onNavigate(query) })
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("Bookmarks", Icons.Default.Bookmark, onBookmarks)
                QuickAction("History", Icons.Default.History, onHistory)
                QuickAction("Downloads", Icons.Default.Download, onDownloads)
                QuickAction("Tabs", Icons.Default.Tab, onTabs)
            }
        }

        item {
            Surface(
                Modifier.fillMaxWidth(),
                color = SurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = Cyan)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Privacy status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(state.privacyStatus, color = OnSurfaceMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        if (state.bookmarks.isNotEmpty()) {
            item { Label("BOOKMARKS") }
            items(state.bookmarks.take(6), key = { it.id }) { b ->
                Entry(b.title, b.url, Icons.Default.Bookmark) { onNavigate(b.url) }
            }
        }

        if (state.history.isNotEmpty()) {
            item { Label("RECENT") }
            items(state.history.take(8), key = { it.timestamp }) { h ->
                Entry(h.title, h.url, Icons.Default.History) { onNavigate(h.url) }
            }
        }
    }
}

@Composable
private fun RowScope.QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        Modifier.weight(1f).clickable(onClick = onClick),
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable private fun Label(text: String) =
    Text(text, color = OnSurfaceMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

@Composable
private fun Entry(title: String, url: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title.ifBlank { url }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp, color = OnSurfaceMuted)
        }
    }
}
