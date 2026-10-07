package com.rexaps.rexmusic

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rexaps.rexmusic.player.EqualizerBars

//  Models & actions 

/** Target menu opsi lagu. onPlay = null berarti menu "Putar" disembunyikan. */
class MenuTarget(val track: RexTrack, val onPlay: (() -> Unit)?)

class QuickTile(
    val id: String,
    val title: String,
    val cover: String,
    val special: Boolean = false,
    val onClick: () -> Unit
)

class RexActions(
    val playSearch: (Int) -> Unit,
    val playHistory: (Int) -> Unit,
    val playMain: (Int) -> Unit,
    val playOffline: (RexTrack) -> Unit,
    val playOfflineAll: (Boolean) -> Unit,
    val playMix: () -> Unit,
    val playQueue: (Int) -> Unit,
    val addQueue: (RexTrack) -> Unit,
    val removeQueue: (Int) -> Unit,
    val moveQueueTop: (Int) -> Unit,
    val clearQueue: () -> Unit,
    val download: (RexTrack) -> Unit,
    val cancelDownload: (RexTrack) -> Unit,
    val delete: (RexTrack) -> Unit,
    val deleteAll: () -> Unit,
    val clearHistory: () -> Unit,
    val requestAccess: () -> Unit,
    val setOffline: (Boolean) -> Unit,
    val toggleAutoplay: () -> Unit,
    val searchArtist: (String) -> Unit,
    val goLibrary: () -> Unit,
    val goPlaylists: () -> Unit,
    val openMenu: (MenuTarget) -> Unit,
    val openPlaylistPicker: (RexTrack) -> Unit,
    val playPlaylist: (String) -> Unit,
    val createPlaylist: (String) -> Unit,
    val deletePlaylist: (String) -> Unit,
    val dismissError: () -> Unit
)

enum class CoverOverlay { None, Loading, Playing }

fun overlayFor(active: Boolean, playing: Boolean, loading: Boolean): CoverOverlay = when {
    active && loading -> CoverOverlay.Loading
    active && playing -> CoverOverlay.Playing
    else -> CoverOverlay.None
}

//  Helpers 

/** Rata-rata warna dari bitmap 16x16 (murah, tanpa Palette). */
fun Drawable.averageColor(): Color? = runCatching {
    val bmp = toBitmap(16, 16, Bitmap.Config.ARGB_8888)
    val px = IntArray(256)
    bmp.getPixels(px, 0, 16, 0, 0, 16, 16)
    var r = 0L
    var g = 0L
    var b = 0L
    px.forEach {
        r += ((it shr 16) and 0xFF)
        g += ((it shr 8) and 0xFF)
        b += (it and 0xFF)
    }
    Color((r / 256).toInt(), (g / 256).toInt(), (b / 256).toInt())
}.getOrNull()

private val TilePalette = listOf(
    Color(0xFF7E57C2), Color(0xFF1E88E5), Color(0xFFE5533D), Color(0xFF00897B),
    Color(0xFFD81B60), Color(0xFF3949AB), Color(0xFF43A047), Color(0xFFF57C00)
)

fun rexTileColor(name: String): Color =
    TilePalette[(name.hashCode() and 0x7fffffff) % TilePalette.size]

fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.2f GB".format(bytes / 1073741824.0)
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / 1048576.0)
    bytes >= 1L shl 10 -> "%d KB".format(bytes shr 10)
    else -> "$bytes B"
}

fun greetingForHour(hour: Int): String = when (hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

@Composable
fun rememberPressScale(source: MutableInteractionSource, pressed: Float = 0.96f): State<Float> {
    val isPressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
}

//  Basic building blocks 

/** Tombol ikon bulat dengan ukuran pasti (tidak pernah melebar / bertabrakan). */
@Composable
fun RexIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    background: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (background.isSpecified) Modifier.background(background) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Gambar cover dengan placeholder ikon. Kalau [onColor] diisi, warna rata-rata dikirim balik. */
@Composable
fun CoverImage(
    url: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    onColor: ((Color) -> Unit)? = null,
    contentDescription: String? = null,
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val callback by rememberUpdatedState(onColor)
    Box(
        modifier.clip(shape).background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote, contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(iconSize)
        )
        if (url.isNotBlank()) {
            val needColor = onColor != null
            val request = remember(url, needColor) {
                ImageRequest.Builder(context).data(url)
                    .allowHardware(!needColor).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { s -> callback?.let { cb -> s.result.drawable.averageColor()?.let(cb) } }
            )
        }
    }
}

/** Thumbnail kotak dengan overlay loading / equalizer. */
@Composable
fun CoverThumb(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    overlay: CoverOverlay = CoverOverlay.None,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier.size(size)) {
        CoverImage(url, Modifier.fillMaxSize(), shape, iconSize = size / 2.5f)
        if (overlay != CoverOverlay.None) {
            Box(
                Modifier.fillMaxSize().clip(shape).background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                when (overlay) {
                    CoverOverlay.Loading -> CircularProgressIndicator(
                        color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(size / 2.4f)
                    )
                    CoverOverlay.Playing -> EqualizerBars(
                        color = Color.White, height = size / 3f, barWidth = 3.dp
                    )
                    CoverOverlay.None -> Unit
                }
            }
        }
    }
}

@Composable
fun DownloadRing(status: DownloadStatus, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.tertiary
    if (status.stage == DownloadStage.Downloading && status.percent >= 0) {
        CircularProgressIndicator(
            progress = status.percent / 100f,
            modifier = modifier.size(size), color = color, strokeWidth = 2.dp
        )
    } else {
        CircularProgressIndicator(modifier = modifier.size(size), color = color, strokeWidth = 2.dp)
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = MaterialTheme.typography.titleLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Saklar Online / Offline berbentuk pill. */
@Composable
fun ModeChip(offline: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fg by animateColorAsState(
        if (offline) scheme.tertiary else scheme.onSurfaceVariant, label = "chipFg"
    )
    val bg by animateColorAsState(
        if (offline) scheme.tertiary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.08f),
        label = "chipBg"
    )
    Row(
        modifier.height(36.dp).clip(CircleShape).background(bg)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (offline) Icons.Default.CloudOff else Icons.Default.Cloud,
            contentDescription = null, tint = fg, modifier = Modifier.size(16.dp)
        )
        Text(
            if (offline) "Offline" else "Online", color = fg,
            style = MaterialTheme.typography.labelLarge, maxLines = 1
        )
    }
}

/** Header untuk tab Cari & Koleksi. */
@Composable
fun TabHeader(title: String, offline: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = MaterialTheme.typography.headlineMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        ModeChip(offline, onToggle)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message, style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorCard(error: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = !error.isNullOrBlank(),
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut()
    ) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = scheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    error.orEmpty(), modifier = Modifier.weight(1f),
                    color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall
                )
                RexIconButton(
                    Icons.Default.Close, "Tutup pesan", onDismiss,
                    size = 40.dp, iconSize = 20.dp, tint = scheme.onErrorContainer
                )
            }
        }
    }
}

//  Skeleton 

@Composable
fun SkeletonList(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.35f), base.copy(alpha = 0.9f), base.copy(alpha = 0.35f)),
        start = Offset(x - 300f, 0f), end = Offset(x, 0f)
    )
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(7) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }
        }
    }
}

//  Track row (list) 

@Composable
fun TrackRow(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    loading: Boolean,
    downloaded: Boolean,
    status: DownloadStatus?,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverThumb(track.cover, 52.dp, overlay = overlayFor(active, playing, loading))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = if (active) scheme.primary else scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloaded) {
                    Icon(
                        Icons.Default.DownloadDone, contentDescription = "Tersimpan offline",
                        tint = scheme.tertiary, modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    subtitle ?: track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (status != null) {
            DownloadRing(status, 20.dp)
            Spacer(Modifier.width(6.dp))
        }
        RexIconButton(
            Icons.Default.MoreVert, "Opsi lagu", onMore,
            size = 40.dp, iconSize = 22.dp, tint = scheme.onSurfaceVariant
        )
    }
}

//  Quick grid (Home) 

@Composable
fun QuickGrid(tiles: List<QuickTile>, nowId: String?, playing: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { tile ->
                    QuickTileCard(tile, tile.id == nowId, playing, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTileCard(tile: QuickTile, active: Boolean, playing: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceVariant)
            .clickable(onClick = tile.onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tile.special) {
            Box(
                Modifier.size(56.dp).background(RexOfflineBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.OfflinePin, contentDescription = null, tint = Color.White)
            }
        } else {
            CoverImage(tile.cover, Modifier.size(56.dp), RoundedCornerShape(0.dp))
        }
        Text(
            tile.title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) scheme.primary else scheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        )
        if (active && playing) {
            EqualizerBars(
                Modifier.padding(end = 10.dp), color = scheme.primary,
                barWidth = 3.dp, height = 14.dp
            )
        }
    }
}

//  Shelf cards 

@Composable
fun ShelfCard(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp
) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    Column(
        modifier.width(size)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(size)) {
            CoverImage(track.cover, Modifier.fillMaxSize(), RoundedCornerShape(10.dp), iconSize = 44.dp)
            if (active && playing) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBars(color = Color.White, barWidth = 2.dp, height = 12.dp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall,
                color = if (active) scheme.primary else scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArtistCard(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = rexTileColor(name)
    Column(
        modifier.width(108.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.55f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(), color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Text(
            name, style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

//  Downloads & queue rows 

@Composable
fun DownloadRow(status: DownloadStatus, onCancel: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = when (status.stage) {
        DownloadStage.Queued -> "Menunggu giliran..."
        DownloadStage.Resolving -> "Menyiapkan..."
        DownloadStage.Downloading ->
            if (status.percent >= 0) "Mengunduh ${status.percent}%" else "Mengunduh..."
        DownloadStage.FetchingLyrics -> "Mengunduh lirik..."
        DownloadStage.Finalizing -> "Menyimpan..."
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceVariant)
            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverThumb(status.track.cover, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    status.track.title, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.tertiary)
            }
            RexIconButton(
                Icons.Default.Close, "Batalkan unduhan", onCancel,
                size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        val bar = Modifier.fillMaxWidth().padding(end = 6.dp).clip(CircleShape)
        if (status.stage == DownloadStage.Downloading && status.percent >= 0) {
            LinearProgressIndicator(
                progress = status.percent / 100f, modifier = bar, color = scheme.tertiary
            )
        } else {
            LinearProgressIndicator(modifier = bar, color = scheme.tertiary)
        }
    }
}

@Composable
fun QueueRow(
    index: Int,
    track: RexTrack,
    onPlay: () -> Unit,
    onMoveTop: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp)).clickable(onClick = onPlay)
            .padding(start = 8.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${index + 1}", style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        CoverThumb(track.cover, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (index > 0) {
            RexIconButton(
                Icons.Default.KeyboardArrowUp, "Naikkan ke paling atas", onMoveTop,
                size = 40.dp, iconSize = 22.dp, tint = scheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.size(40.dp))
        }
        RexIconButton(
            Icons.Default.Close, "Hapus dari antrian", onRemove,
            size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
        )
    }
}

@Composable
fun AutoplayCard(state: RexMusicUiState, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        !state.autoplay -> "Mati: lagu berikutnya mengikuti urutan daftar"
        state.offline.enabled -> "Dipilih dari lagu unduhanmu"
        state.favoriteArtists.isNotEmpty() ->
            "Dicampur dari ${state.favoriteArtists.joinToString(", ")} & lagu populer"
        state.popularCount > 0 -> "Dipilih acak dari lagu populer"
        else -> "Dipilih otomatis untukmu"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.primary.copy(alpha = if (state.autoplay) 0.14f else 0.06f))
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Autoplay pintar", style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = state.autoplay, onCheckedChange = { onToggle() })
    }
}
