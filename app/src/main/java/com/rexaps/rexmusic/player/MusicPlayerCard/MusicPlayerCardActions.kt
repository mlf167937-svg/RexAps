package com.rexaps.rexmusic.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*

@Composable
internal fun LikeButton(isLiked: Boolean, onToggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            scale.snapTo(0.7f)
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }
    RexIconButton(
        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        description = if (isLiked) "Hapus dari favorit" else "Tambah ke favorit",
        onClick = onToggle,
        size = 48.dp, iconSize = 28.dp,
        tint = if (isLiked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
    )
}

@Composable
internal fun PlayPauseButton(isPlaying: Boolean, busy: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, 0.92f)
    Box(
        Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = source, indication = LocalIndication.current,
                role = Role.Button, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = when {
                busy -> 0
                isPlaying -> 1
                else -> 2
            },
            label = "playPause"
        ) { s ->
            when (s) {
                0 -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp)
                )
                1 -> Icon(
                    Icons.Default.Pause, contentDescription = "Jeda",
                    tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp)
                )
                else -> Icon(
                    Icons.Default.PlayArrow, contentDescription = "Putar",
                    tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
internal fun BottomActions(
    state: RexMusicUiState,
    downloaded: Boolean,
    status: DownloadStatus?,
    actions: PlayerActions
) {
    val queueCount = state.userQueue.size
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            downloaded -> ActionPill(Icons.Default.DownloadDone, "Tersimpan", true, actions.onDelete)
            status != null -> ActionPill(
                Icons.Default.Close,
                if (status.stage == DownloadStage.Downloading && status.percent >= 0)
                    "Mengunduh ${status.percent}%" else "Menyiapkan...",
                true, actions.onCancelDownload
            )
            else -> ActionPill(Icons.Default.Download, "Unduh", false, actions.onDownload)
        }
        ActionPill(
            Icons.AutoMirrored.Filled.QueueMusic,
            if (queueCount > 0) "Antrian $queueCount" else "Antrian",
            queueCount > 0, actions.onQueue
        )
    }
}
