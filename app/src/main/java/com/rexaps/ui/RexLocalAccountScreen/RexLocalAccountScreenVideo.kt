package com.rexaps.ui

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.rexaps.R
import java.io.File

/* ═════════════════════ Video Player ═════════════════════ */

@OptIn(UnstableApi::class)
@Composable
internal fun RexVideo(
    path: String,
    modifier: Modifier = Modifier,
    key: Any = path,
    loop: Boolean = true,
    muted: Boolean = true,
    playing: Boolean = true,
    fit: Boolean = false
) {
    val ctx = LocalContext.current
    val player = remember(path, key) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (muted) 0f else 1f
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player, playing) { player.playWhenReady = playing }
    AndroidView(
        modifier = modifier,
        factory = { c ->
            (LayoutInflater.from(c).inflate(R.layout.rex_player_texture, null) as PlayerView).apply {
                this.player = player
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = {
            it.player = player
            it.resizeMode = if (fit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    )
}

@Composable
internal fun VideoFeed(
    acct: RexLocalAccount,
    rev: Int,
    files: List<File>,
    start: Int,
    onDelete: (File) -> Unit,
    onClose: () -> Unit
) {
    if (files.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, files.lastIndex)) { files.size }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(pager, Modifier.fillMaxSize()) { page ->
                FeedItem(files[page], acct, rev, active = pager.currentPage == page)
            }
            Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(.5f), Color.Transparent))))
            Row(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${pager.currentPage + 1} / ${files.size}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDelete(files[pager.currentPage]) }) { Icon(Icons.Default.Delete, null, tint = Color.White) }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }
}
