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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexmusic.*

@Stable
class PlayerActions(
    val onClose: () -> Unit,
    val onLyrics: () -> Unit,
    val onMore: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrev: () -> Unit,
    val onOpenPrevious: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onToggleLike: () -> Unit,
    val onRepeat: () -> Unit,
    val onAutoplay: () -> Unit,
    val onQueue: () -> Unit,
    val onDownload: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onDelete: () -> Unit
)

//  Full-screen player 

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

//  Seek 

@Composable
internal fun PlayerControls(state: RexMusicUiState, busy: Boolean, actions: PlayerActions) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToggleControl(
            icon = Icons.Default.AutoAwesome,
            description = if (state.autoplay) "Autoplay pintar aktif" else "Autoplay pintar mati",
            active = state.autoplay, onClick = actions.onAutoplay
        )
        RexIconButton(
            Icons.Default.SkipPrevious, "Lagu sebelumnya", actions.onPrev,
            size = 52.dp, iconSize = 36.dp, tint = MaterialTheme.colorScheme.onBackground
        )
        PlayPauseButton(state.isPlaying, busy, actions.onPlayPause)
        RexIconButton(
            Icons.Default.SkipNext, "Lagu berikutnya", actions.onNext,
            size = 52.dp, iconSize = 36.dp, tint = MaterialTheme.colorScheme.onBackground
        )
        RepeatControl(state.repeatTotal, state.repeatLeft, actions.onRepeat)
    }
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
internal fun ToggleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = description,
            tint = if (active) accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
        if (active) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    .size(4.dp).background(accent, CircleShape)
            )
        }
    }
}

@Composable
internal fun RepeatControl(total: Int, left: Int, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    val on = total > 0
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (on) Icons.Default.RepeatOne else Icons.Default.Repeat,
            contentDescription = if (on) "Ulangi lagu, sisa $left dari $total kali" else "Atur ulangi lagu",
            tint = if (on) accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
        if (on) {
            Text(
                "$left", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp)
                    .background(accent, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    .size(4.dp).background(accent, CircleShape)
            )
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

@Composable
internal fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val fg = if (active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onBackground
    Row(
        Modifier.height(40.dp).clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

//  Error & neighbors 
