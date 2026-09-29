package com.rexaps.rexmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexmusic.player.MusicPlayerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexMusicScreen(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RexMusic") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showPlayer) { showPlayer = false } else { onBack() }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            if (showPlayer) {
                val track = vm.currentTrack()
                if (track != null) {
                    MusicPlayerCard(
                        track = track,
                        isPlaying = state.isPlaying,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        isLiked = isLiked,
                        onPlayPause = { vm.togglePlay() },
                        onNext = { vm.next() },
                        onPrev = { vm.prev() },
                        onSeek = { vm.seekTo(it) },
                        onToggleLike = { isLiked = !isLiked },
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = { showPlayer = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("tutup player")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.isBlank()) vm.clearSearch() else vm.search(it)
                        },
                        label = { Text("cari lagu di spotify") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                    vm.clearSearch()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )

                    if (state.loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = state.loadingText.ifBlank { "loading..." },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    if (state.error != null) {
                        Text(
                            text = "error: ${state.error}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (state.searchResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Hasil Pencarian",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            itemsIndexed(state.searchResults, key = { _, t -> "s-${t.id}" }) { idx, track ->
                                TrackRow(
                                    track = track,
                                    onClick = {
                                        vm.playFromSearch(idx)
                                        showPlayer = true
                                    }
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }

                        item {
                            Text(
                                text = "Rekomendasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        itemsIndexed(state.tracks, key = { _, t -> "m-${t.id}" }) { idx, track ->
                            TrackRow(
                                track = track,
                                onClick = {
                                    vm.playFromMain(idx)
                                    showPlayer = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: RexTrack,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(track.title, maxLines = 1)
        },
        supportingContent = {
            Text(track.artist, maxLines = 1)
        },
        leadingContent = {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
