package com.rexaps.rexmanager

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.io.File

@Composable
internal fun AudioPreview(file: File) {
    val player = remember(file) {
        runCatching { MediaPlayer().apply { setDataSource(file.path); prepare() } }.getOrNull()
    }
    if (player == null) {
        Text("Audio tidak bisa diputar", color = Rex.danger)
        return
    }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableFloatStateOf(0f) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(playing) {
        while (playing) {
            pos = player.currentPosition.toFloat()
            if (!player.isPlaying) playing = false
            delay(250)
        }
    }
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.MusicNote, null, tint = Color(0xFF3BC4F5), modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Slider(
            value = pos,
            valueRange = 0f..player.duration.toFloat().coerceAtLeast(1f),
            onValueChange = { pos = it; player.seekTo(it.toInt()) }
        )
        FilledIconButton(
            onClick = {
                if (player.isPlaying) {
                    player.pause(); playing = false
                } else {
                    player.start(); playing = true
                }
            },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Rex.primary),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playing) "Jeda" else "Putar",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
internal fun VideoPreview(file: File) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            VideoView(c).apply {
                setMediaController(MediaController(c).also { it.setAnchorView(this) })
                setVideoURI(Uri.fromFile(file))
                setOnPreparedListener { start() }
            }
        },
        onRelease = { it.stopPlayback() }
    )
}
