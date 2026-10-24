// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.rexaps.R
import java.io.File

@Composable
internal fun Aurora(modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "aurora")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    Box(modifier.drawBehind {
        drawRect(
            Brush.radialGradient(
                listOf(c.primary.copy(.26f), Color.Transparent),
                center = Offset(size.width * (.15f + .6f * t), size.height * .30f),
                radius = size.width * .95f
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(c.tertiary.copy(.22f), Color.Transparent),
                center = Offset(size.width * (.9f - .6f * t), size.height * .75f),
                radius = size.width * .9f
            )
        )
    })
}

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

/* ═════════════════════ Crop Dialog ═════════════════════ */

@Composable
internal fun PermissionGate(padding: PaddingValues, onAllow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "gate")
    val pulse by inf.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val steps = if (Build.VERSION.SDK_INT >= 30) {
        listOf(
            "Tekan tombol \"Izinkan akses\" di bawah",
            "Aktifkan \"Izinkan akses ke semua file\"",
            "Kembali ke RexAps, halaman akan terbuka otomatis"
        )
    } else {
        listOf(
            "Tekan tombol \"Izinkan akses\" di bawah",
            "Setujui izin penyimpanan yang muncul"
        )
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
        Aurora(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(136.dp).clip(CircleShape).background(colors.primary.copy(alpha = .12f)))
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = colors.onPrimary, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Izin penyimpanan diperlukan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Profil, story, dan video kamu disimpan langsung di Download/RexAps/.config/ — 100% lokal, tanpa server.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    steps.forEachIndexed { i, s -> PermissionStep(i + 1, s) }
                }
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onAllow,
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Izinkan akses", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun PermissionStep(number: Int, text: String) {
    val c = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(c.primary.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = c.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun RexGrid(
    count: Int,
    columns: Int,
    cellAspect: Float,
    gap: Dp = 8.dp,
    cell: @Composable (Int, Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until (count + columns - 1) / columns) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (c in 0 until columns) {
                    val i = r * columns + c
                    if (i < count) cell(i, Modifier.weight(1f).aspectRatio(cellAspect))
                    else Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun AddTile(modifier: Modifier, title: String, sub: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .pressable(onClick)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.primary.copy(.20f), c.tertiary.copy(.14f))))
            .border(1.dp, c.primary.copy(.35f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(c.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = c.onPrimary, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        }
    }
}
