package com.rexaps.rexmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RexMusic") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            if (state.error != null) {
                Text(
                    text = "error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Rekomendasi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
                        ListItem(
                            headlineContent = { Text(track.title) },
                            supportingContent = { Text(track.artist) },
                            leadingContent = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                vm.play(index)
                                showPlayer = true
                            }
                        )
                    }
                }
            }
        }
    }
}
