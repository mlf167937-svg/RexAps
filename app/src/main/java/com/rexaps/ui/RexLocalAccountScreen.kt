// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.rexaps.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* ═════════════════════ Konstanta & model ═════════════════════ */

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val MAX_CLIP_MS = 4000L
internal const val BANNER_ASPECT = 16f / 9f

private object RexPaths {
    val base get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RexAps/.config")
    val account get() = File(base, "account")
    val video get() = File(account, "video")
    val story get() = File(base, "story")
    val config get() = File(base, "account.json")
}

internal data class RexLocalAccount(
    val name: String = "Rex User",
    val username: String = "@rexuser",
    val bio: String = "Halo! Aku menggunakan RexAps.",
    val bannerPath: String = "",
    val bannerVideo: String = "",
    val bannerScale: Float = 1f,
    val bannerX: Float = 0f,
    val bannerY: Float = 0f,
    val picturePath: String = "",
    val pictureVideo: String = "",
    val pictureScale: Float = 1f,
    val pictureX: Float = 0f,
    val pictureY: Float = 0f
)

internal data class RexStory(
    val file: File,
    val time: Long,
    val isVideo: Boolean,
    val text: String,
    val gradient: Int
)

internal enum class CropTarget { AVATAR, BANNER }

internal class VSrc(val uri: Uri?, val file: File?) {
    fun bind(r: MediaMetadataRetriever, c: Context) {
        if (uri != null) r.setDataSource(c, uri) else r.setDataSource(file!!.absolutePath)
    }

    fun bind(e: MediaExtractor, c: Context) {
        if (uri != null) e.setDataSource(c, uri, null) else e.setDataSource(file!!.absolutePath)
    }

    fun open(c: Context): InputStream? =
        if (uri != null) c.contentResolver.openInputStream(uri) else file?.inputStream()
}

internal class CropReq(
    val target: CropTarget,
    val bitmap: Bitmap,
    val video: VSrc?,
    val durationMs: Long,
    val live: Boolean
)

internal val storyGradients = listOf(
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF232526), Color(0xFF414345)),
    listOf(Color(0xFFEE0979), Color(0xFFFF6A00))
)

/* ═════════════════════ File & media helpers ═════════════════════ */

private fun hasStorageAccess(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

private fun readAccount(): RexLocalAccount? = try {
    val f = RexPaths.config
    if (!f.exists()) null else JSONObject(f.readText()).let { j ->
        var bp = j.optString("banner", "")
        var bv = j.optString("bannerVideo", "")
        if (bp.endsWith(".mp4")) { bv = bp; bp = "" }
        RexLocalAccount(
            j.optString("name", "Rex User"), j.optString("username", "@rexuser"), j.optString("bio", ""),
            bp, bv,
            j.optDouble("bannerScale", 1.0).toFloat(), j.optDouble("bannerX", 0.0).toFloat(), j.optDouble("bannerY", 0.0).toFloat(),
            j.optString("profilePicture", ""), j.optString("profileVideo", ""),
            j.optDouble("pictureScale", 1.0).toFloat(), j.optDouble("pictureX", 0.0).toFloat(), j.optDouble("pictureY", 0.0).toFloat()
        )
    }
} catch (e: Exception) { null }

private fun writeAccount(a: RexLocalAccount) {
    RexPaths.config.parentFile?.mkdirs()
    val j = JSONObject()
        .put("name", a.name).put("username", a.username).put("bio", a.bio)
        .put("banner", a.bannerPath).put("bannerVideo", a.bannerVideo)
        .put("bannerScale", a.bannerScale.toDouble()).put("bannerX", a.bannerX.toDouble()).put("bannerY", a.bannerY.toDouble())
        .put("profilePicture", a.picturePath).put("profileVideo", a.pictureVideo)
        .put("pictureScale", a.pictureScale.toDouble()).put("pictureX", a.pictureX.toDouble()).put("pictureY", a.pictureY.toDouble())
        .put("updatedAt", System.currentTimeMillis())
    RexPaths.config.writeText(j.toString(2))
}

/** Story > 24 jam dihapus otomatis. */
private fun loadStories(): List<RexStory> {
    val now = System.currentTimeMillis()
    val files = RexPaths.story.listFiles() ?: return emptyList()
    return files.mapNotNull { f ->
        val t = f.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null
        if (now - t > DAY_MS) { f.delete(); return@mapNotNull null }
        when (f.extension.lowercase()) {
            "mp4" -> RexStory(f, t, true, "", 0)
            "json" -> try {
                val j = JSONObject(f.readText())
                RexStory(f, t, false, j.optString("text"), j.optInt("gradient"))
            } catch (e: Exception) { null }
            else -> null
        }
    }.sortedBy { it.time }
}

private fun loadVideos(): List<File> =
    RexPaths.video.listFiles()
        ?.filter { it.extension.lowercase() in setOf("mp4", "png", "jpg", "jpeg") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

private fun decodeSampled(ctx: Context, uri: Uri, maxDim: Int = 2400): Bitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1
    while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
    val raw = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
    }
    raw?.let { applyExif(ctx, uri, it) }
} catch (e: Exception) { null }

internal fun decodeFileSampled(path: String, maxDim: Int): Bitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, o)
    var s = 1
    while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
} catch (e: Exception) { null }

private fun applyExif(ctx: Context, uri: Uri, b: Bitmap): Bitmap {
    val o = try {
        ctx.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    } catch (e: Exception) { null } ?: ExifInterface.ORIENTATION_NORMAL

    val m = android.graphics.Matrix()
    when (o) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        else -> return b
    }
    return try { Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true) } catch (e: Exception) { b }
}

internal fun rotate90(b: Bitmap): Bitmap {
    val m = android.graphics.Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
}

private fun downscale(b: Bitmap, maxDim: Int): Bitmap {
    val m = max(b.width, b.height)
    if (m <= maxDim) return b
    val k = maxDim.toFloat() / m
    return Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
}

internal fun videoFrameAt(ctx: Context, src: VSrc, ms: Long): Bitmap? {
    val r = MediaMetadataRetriever()
    return try {
        src.bind(r, ctx)
        val f = r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC) ?: r.getFrameAtTime(0)
        f?.let { downscale(it, 1600) }
    } catch (e: Exception) { null } finally { try { r.release() } catch (_: Exception) {} }
}

internal fun videoDurationOf(ctx: Context, src: VSrc): Long {
    val r = MediaMetadataRetriever()
    return try {
        src.bind(r, ctx)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) { 0L } finally { try { r.release() } catch (_: Exception) {} }
}

/** Ekstrak video dari Motion Photo / Live Photo Android (mp4 di ujung file JPEG). */
private fun extractMotionVideo(ctx: Context, uri: Uri, out: File): Boolean = try {
    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    var start = -1
    if (bytes.size > 4096 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
        var i = 2048
        while (i < bytes.size - 16) {
            if (bytes[i] == 'f'.code.toByte() && bytes[i + 1] == 't'.code.toByte() &&
                bytes[i + 2] == 'y'.code.toByte() && bytes[i + 3] == 'p'.code.toByte()
            ) {
                val boxSize = ((bytes[i - 4].toInt() and 0xFF) shl 24) or
                    ((bytes[i - 3].toInt() and 0xFF) shl 16) or
                    ((bytes[i - 2].toInt() and 0xFF) shl 8) or
                    (bytes[i - 1].toInt() and 0xFF)
                if (boxSize in 16..64) { start = i - 4; break }
            }
            i++
        }
    }
    if (start > 0 && bytes.size - start > 2048) {
        FileOutputStream(out).use { it.write(bytes, start, bytes.size - start) }
        true
    } else false
} catch (e: Exception) { false }

/** Potong video maks 4 detik tanpa re-encode (remux). Audio dibuang. */
internal fun trimVideo(ctx: Context, src: VSrc, startMs: Long, out: File): Boolean {
    val dur = videoDurationOf(ctx, src)
    if (dur in 1..(MAX_CLIP_MS + 300)) {
        return try {
            src.open(ctx)?.use { i -> FileOutputStream(out).use { i.copyTo(it) } }
            true
        } catch (e: Exception) { false }
    }
    val ex = MediaExtractor()
    var mux: MediaMuxer? = null
    return try {
        src.bind(ex, ctx)
        var vt = -1
        for (i in 0 until ex.trackCount) {
            if (ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) { vt = i; break }
        }
        if (vt < 0) return false
        ex.selectTrack(vt)
        val fmt = ex.getTrackFormat(vt)
        val maxIn = if (fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0
        val buf = ByteBuffer.allocate(max(maxIn, 8 * 1024 * 1024))

        var rot = 0
        val r = MediaMetadataRetriever()
        try {
            src.bind(r, ctx)
            rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } catch (_: Exception) {
        } finally { try { r.release() } catch (_: Exception) {} }

        out.delete()
        mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val mt = mux.addTrack(fmt)
        mux.setOrientationHint(rot)
        mux.start()
        ex.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val info = MediaCodec.BufferInfo()
        var first = -1L
        while (true) {
            buf.clear()
            val size = ex.readSampleData(buf, 0)
            if (size < 0) break
            val t = ex.sampleTime
            if (first < 0) first = t
            if (t - first > MAX_CLIP_MS * 1000) break
            val flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            info.set(0, size, t - first, flags)
            mux.writeSampleData(mt, buf, info)
            ex.advance()
        }
        mux.stop()
        true
    } catch (e: Exception) { false }
    finally {
        try { mux?.release() } catch (_: Exception) {}
        try { ex.release() } catch (_: Exception) {}
    }
}

internal fun buildCropReq(ctx: Context, uri: Uri, target: CropTarget): CropReq? {
    val mime = ctx.contentResolver.getType(uri) ?: ""
    if (mime.startsWith("video")) {
        val src = VSrc(uri, null)
        val dur = videoDurationOf(ctx, src)
        val frame = videoFrameAt(ctx, src, 0) ?: return null
        return CropReq(target, frame, src, dur, false)
    }
    val photo = decodeSampled(ctx, uri) ?: return null
    if (mime == "image/jpeg") {
        val f = File(ctx.cacheDir, "live_${System.currentTimeMillis()}.mp4")
        if (extractMotionVideo(ctx, uri, f)) {
            val src = VSrc(null, f)
            val dur = videoDurationOf(ctx, src)
            val frame = videoFrameAt(ctx, src, 0)
            if (dur > 0 && frame != null) {
                val sameAspect = Math.abs(
                    photo.width.toFloat() / photo.height - frame.width.toFloat() / frame.height
                ) < 0.02f
                return CropReq(target, if (sameAspect) photo else frame, src, dur, true)
            }
        }
        f.delete()
    }
    return CropReq(target, photo, null, 0, false)
}

internal fun renderCrop(src: Bitmap, aspect: Float, outW: Int, s: Float, ox: Float, oy: Float): Bitmap {
    val outH = (outW / aspect).toInt()
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val base = max(outW.toFloat() / src.width, outH.toFloat() / src.height) * s
    val m = android.graphics.Matrix().apply {
        postTranslate(-src.width / 2f, -src.height / 2f)
        postScale(base, base)
        postTranslate(outW / 2f + ox * outW, outH / 2f + oy * outH)
    }
    android.graphics.Canvas(out).drawBitmap(
        src, m,
        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.DITHER_FLAG)
    )
    return out
}

internal fun secs(ms: Long) = String.format("%.1fs", ms / 1000f)

/* ═════════════════════ Util UI ═════════════════════ */

internal fun Modifier.pressable(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) .93f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press"
    )
    this.graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier,
        shape = RoundedCornerShape(22.dp),
        color = c.surface.copy(alpha = .55f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = .5f))
    ) { Column(content = content) }
}

@Composable
internal fun Reveal(visible: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(550, delayMs)) + slideInVertically(tween(550, delayMs)) { it / 4 }
    ) { content() }
}

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

@Composable
internal fun GlowBadge(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(.16f)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ═════════════════════ Player video ═════════════════════ */

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

/* ═════════════════════ Crop dialog ═════════════════════ */

@Composable
internal fun CropDialog(
    req: CropReq,
    aspect: Float,
    circle: Boolean,
    title: String,
    onCancel: () -> Unit,
    onConfirm: (Bitmap, Float, Float, Float, Long) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bmp by remember { mutableStateOf(req.bitmap) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var off by remember { mutableStateOf(Offset.Zero) }
    var vw by remember { mutableFloatStateOf(1f) }
    var vh by remember { mutableFloatStateOf(1f) }
    var startMs by remember { mutableFloatStateOf(0f) }

    val canTrim = req.video != null && req.durationMs > MAX_CLIP_MS
    val maxStart = (req.durationMs - MAX_CLIP_MS).coerceAtLeast(0L).toFloat()
    val fw = min(vw * .9f, vh * .9f * aspect)
    val fh = fw / aspect

    fun clamp(z: Float, o: Offset): Offset {
        val base = max(fw / bmp.width, fh / bmp.height)
        val mx = max(0f, (bmp.width * base * z - fw) / 2f)
        val my = max(0f, (bmp.height * base * z - fh) / 2f)
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }

    if (canTrim) {
        LaunchedEffect(startMs) {
            if (startMs > 0f) delay(220)
            val f = withContext(Dispatchers.IO) { videoFrameAt(ctx, req.video!!, startMs.toLong()) }
            if (f != null && f.width == bmp.width && f.height == bmp.height) bmp = f
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF09090D)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Cubit & geser • ketuk 2x untuk zoom", color = Color.White.copy(.6f), style = MaterialTheme.typography.labelSmall)
                    }
                    if (req.live) GlowBadge("LIVE") else if (req.video != null) GlowBadge("VIDEO")
                    Spacer(Modifier.width(8.dp))
                }

                val img = remember(bmp) { bmp.asImageBitmap() }
                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { vw = it.width.toFloat(); vh = it.height.toFloat() }
                        .pointerInput(fw, fh, bmp) {
                            detectTransformGestures { centroid, pan, gz, _ ->
                                val nz = (zoom * gz).coerceIn(1f, 8f)
                                val c = centroid - Offset(size.width / 2f, size.height / 2f)
                                val q = (c - off) / zoom
                                off = clamp(nz, c - q * nz + pan)
                                zoom = nz
                            }
                        }
                        .pointerInput(fw, fh, bmp) {
                            detectTapGestures(onDoubleTap = {
                                scope.launch {
                                    val from = zoom
                                    val to = if (from > 1.05f) 1f else 2.5f
                                    animate(from, to, animationSpec = tween(280)) { v, _ ->
                                        zoom = v
                                        off = clamp(v, off)
                                    }
                                }
                            })
                        }
                ) {
                    val base = max(fw / img.width, fh / img.height)
                    val s = base * zoom
                    val dw = img.width * s
                    val dh = img.height * s
                    val cx = size.width / 2f + off.x
                    val cy = size.height / 2f + off.y
                    drawImage(
                        img,
                        dstOffset = IntOffset((cx - dw / 2f).roundToInt(), (cy - dh / 2f).roundToInt()),
                        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                        filterQuality = FilterQuality.High
                    )
                    val left = (size.width - fw) / 2f
                    val top = (size.height - fh) / 2f
                    val frame = Rect(left, top, left + fw, top + fh)
                    val corner = CornerRadius(18.dp.toPx())
                    val hole = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        if (circle) addOval(frame) else addRoundRect(RoundRect(frame, corner))
                    }
                    drawPath(hole, Color.Black.copy(.72f))
                    if (circle) {
                        drawOval(Color.White, frame.topLeft, frame.size, style = Stroke(2.dp.toPx()))
                    } else {
                        drawRoundRect(Color.White, frame.topLeft, frame.size, corner, style = Stroke(2.dp.toPx()))
                        val gl = Color.White.copy(.28f)
                        for (k in 1..2) {
                            drawLine(gl, Offset(left + fw * k / 3f, top), Offset(left + fw * k / 3f, top + fh), 1.dp.toPx())
                            drawLine(gl, Offset(left, top + fh * k / 3f), Offset(left + fw, top + fh * k / 3f), 1.dp.toPx())
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (canTrim) {
                        Text(
                            "Pilih 4 detik: ${secs(startMs.toLong())} – ${secs(startMs.toLong() + MAX_CLIP_MS)}  (total ${secs(req.durationMs)})",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(value = startMs, onValueChange = { startMs = it }, valueRange = 0f..maxStart)
                    } else if (req.video != null) {
                        Text(
                            if (req.live) "Live photo • gerakan ${secs(req.durationMs)} ikut disimpan"
                            else "Video ${secs(req.durationMs)} • sudah ≤ 4 detik",
                            color = Color.White.copy(.8f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text("Zoom", color = Color.White.copy(.7f), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = zoom,
                        valueRange = 1f..8f,
                        onValueChange = { zoom = it; off = clamp(it, off) }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            enabled = req.video == null,
                            onClick = { bmp = rotate90(bmp); zoom = 1f; off = Offset.Zero }
                        ) {
                            Icon(Icons.Default.RotateRight, null, tint = Color.White.copy(if (req.video == null) 1f else .3f))
                        }
                        IconButton(onClick = { zoom = 1f; off = Offset.Zero }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = onCancel) { Text("Batal") }
                        Button(onClick = { onConfirm(bmp, zoom, off.x / fw, off.y / fh, startMs.toLong()) }) { Text("Terapkan") }
                    }
                }
            }
        }
    }
}

/* ═════════════════════ Controller state profil ═════════════════════ */

/**
 * Semua state & aksi profil. Dipakai bersama oleh layout portrait & landscape,
 * sehingga logika tidak diduplikasi.
 */
internal class RexProfileController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var acct by mutableStateOf(RexLocalAccount())
    var rev by mutableIntStateOf(0)
    var stories by mutableStateOf<List<RexStory>>(emptyList())
    var videos by mutableStateOf<List<File>>(emptyList())
    var tab by mutableIntStateOf(0)
    var visible by mutableStateOf(false)
    var showEdit by mutableStateOf(false)
    var showStoryCompose by mutableStateOf(false)
    var storyIndex by mutableStateOf<Int?>(null)
    var feedIndex by mutableStateOf<Int?>(null)
    var crop by mutableStateOf<CropReq?>(null)
    var busy by mutableStateOf<String?>(null)

    // Diisi oleh rememberRexProfileController() dari launcher Activity.
    var onPickBanner: () -> Unit = {}
    var onPickAvatar: () -> Unit = {}
    var onPickStoryVideo: () -> Unit = {}
    var onPickVideo: () -> Unit = {}

    private fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    suspend fun load() {
        withContext(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            RexPaths.video.mkdirs()
        }
        withContext(Dispatchers.IO) { readAccount() }?.let { acct = it; rev++ }
        videos = withContext(Dispatchers.IO) { loadVideos() }
        visible = true
        while (true) {
            stories = withContext(Dispatchers.IO) { loadStories() }
            delay(60_000)
        }
    }

    fun handlePick(uri: Uri?, target: CropTarget) {
        if (uri == null) return
        scope.launch {
            busy = "Membaca media…"
            val req = withContext(Dispatchers.IO) { buildCropReq(context, uri, target) }
            busy = null
            if (req == null) toast("Media tidak bisa dibaca") else crop = req
        }
    }

    fun addStoryVideo(uri: Uri?) {
        if (uri == null) return
        scope.launch(Dispatchers.IO) {
            try {
                RexPaths.story.mkdirs()
                val f = File(RexPaths.story, "${System.currentTimeMillis()}.mp4")
                context.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(f).use { i.copyTo(it) } }
                val list = loadStories()
                withContext(Dispatchers.Main) {
                    stories = list
                    toast("Story diposting (hilang dalam 24 jam)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }

    fun addVideo(uri: Uri?) {
        if (uri == null) return
        scope.launch(Dispatchers.IO) {
            try {
                RexPaths.video.mkdirs()
                val mime = context.contentResolver.getType(uri) ?: ""
                val ext = when {
                    mime.startsWith("video") -> "mp4"
                    mime.contains("png") -> "png"
                    else -> "jpg"
                }
                val f = File(RexPaths.video, "${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(f).use { i.copyTo(it) } }
                val list = loadVideos()
                withContext(Dispatchers.Main) { videos = list }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }

    fun applyCrop(req: CropReq, bmp: Bitmap, s: Float, ox: Float, oy: Float, startMs: Long) {
        crop = null
        scope.launch {
            busy = "Memproses…"
            val prev = acct
            val result = withContext(Dispatchers.IO) {
                try {
                    RexPaths.account.mkdirs()
                    val isAvatar = req.target == CropTarget.AVATAR
                    val still = if (isAvatar) renderCrop(bmp, 1f, 768, s, ox, oy)
                    else renderCrop(bmp, BANNER_ASPECT, 1600, s, ox, oy)
                    val png = File(RexPaths.account, if (isAvatar) "profile-pictures.png" else "banner.png")
                    FileOutputStream(png).use { still.compress(Bitmap.CompressFormat.PNG, 100, it) }

                    val vf = File(RexPaths.account, if (isAvatar) "profile-video.mp4" else "banner.mp4")
                    var vpath = ""
                    var videoFailed = false
                    val v = req.video
                    if (v != null) {
                        if (trimVideo(context, v, startMs, vf)) vpath = vf.absolutePath
                        else { vf.delete(); videoFailed = true }
                    } else vf.delete()

                    val a = if (isAvatar)
                        prev.copy(picturePath = png.absolutePath, pictureVideo = vpath, pictureScale = s, pictureX = ox, pictureY = oy)
                    else
                        prev.copy(bannerPath = png.absolutePath, bannerVideo = vpath, bannerScale = s, bannerX = ox, bannerY = oy)

                    writeAccount(a)
                    req.video?.file?.delete()
                    a to videoFailed
                } catch (e: Exception) { null }
            }
            busy = null
            if (result == null) {
                toast("Gagal menyimpan (cek izin)")
            } else {
                acct = result.first
                rev++
                if (result.second) toast("Video tidak bisa diproses, dipakai sebagai foto")
            }
        }
    }

    fun clearBanner() {
        val a = acct.copy(bannerPath = "", bannerVideo = "")
        acct = a
        rev++
        scope.launch(Dispatchers.IO) {
            File(RexPaths.account, "banner.png").delete()
            File(RexPaths.account, "banner.mp4").delete()
            writeAccount(a)
        }
    }

    fun clearAvatar() {
        val a = acct.copy(picturePath = "", pictureVideo = "")
        acct = a
        rev++
        scope.launch(Dispatchers.IO) {
            File(RexPaths.account, "profile-pictures.png").delete()
            File(RexPaths.account, "profile-video.mp4").delete()
            writeAccount(a)
        }
    }

    fun saveProfile(n: String, u: String, b: String) {
        val a = acct.copy(
            name = n.trim().ifBlank { "Rex User" },
            username = u.trim().ifBlank { "@rexuser" },
            bio = b.trim()
        )
        acct = a
        scope.launch(Dispatchers.IO) {
            try {
                writeAccount(a)
                withContext(Dispatchers.Main) { toast("Profil tersimpan") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }

    fun postStory(text: String, gradient: Int) {
        scope.launch(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            File(RexPaths.story, "${System.currentTimeMillis()}.json")
                .writeText(JSONObject().put("text", text).put("gradient", gradient).toString())
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }

    fun deleteStory(s: RexStory) {
        scope.launch(Dispatchers.IO) {
            s.file.delete()
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }

    fun deleteVideo(f: File) {
        scope.launch(Dispatchers.IO) {
            f.delete()
            val list = loadVideos()
            withContext(Dispatchers.Main) { videos = list }
        }
    }
}

/** Membuat controller + launcher. Dipanggil sekali dari entry point. */
@Composable
internal fun rememberRexProfileController(): RexProfileController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ctl = remember { RexProfileController(context, scope) }

    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.handlePick(it, CropTarget.BANNER)
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.handlePick(it, CropTarget.AVATAR)
    }
    val storyVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        ctl.addStoryVideo(it)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.addVideo(it)
    }

    ctl.onPickBanner = { bannerPicker.launch(arrayOf("image/*", "video/*")) }
    ctl.onPickAvatar = { avatarPicker.launch(arrayOf("image/*", "video/*")) }
    ctl.onPickStoryVideo = { storyVideoPicker.launch("video/*") }
    ctl.onPickVideo = { videoPicker.launch(arrayOf("video/mp4", "image/png", "image/jpeg")) }

    LaunchedEffect(Unit) { ctl.load() }
    return ctl
}

/* ═════════════════════ Entry point + izin ═════════════════════ */

@Composable
fun RexLocalAccountScreen(padding: PaddingValues) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasStorageAccess(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val ob = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) granted = hasStorageAccess(context)
        }
        lifecycle.addObserver(ob)
        onDispose { lifecycle.removeObserver(ob) }
    }

    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted = hasStorageAccess(context) }

    if (!granted) {
        PermissionGate(padding) {
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                legacyLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )
            }
        }
    } else {
        RexProfileRoot(padding)
    }
}

@Composable
private fun RexProfileRoot(padding: PaddingValues) {
    val ctl = rememberRexProfileController()
    val isLandscape =
        LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(Modifier.fillMaxSize().padding(padding)) {
        if (isLandscape) {
            RexAccountScreenLandscape(ctl)
        } else {
            RexAccountScreenPortrait(ctl)
        }

        // Tombol kamera melayang (ubah profil)
        Surface(
            Modifier.align(Alignment.TopEnd).padding(14.dp),
            shape = CircleShape,
            color = Color.Black.copy(.35f)
        ) {
            IconButton(onClick = { ctl.showEdit = true }) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
            }
        }
    }

    RexProfileOverlays(ctl)
}

@Composable
private fun RexProfileOverlays(ctl: RexProfileController) {
    if (ctl.showEdit) EditProfileSheet(ctl)

    if (ctl.showStoryCompose) {
        StoryComposeDialog(
            onDismiss = { ctl.showStoryCompose = false },
            onText = { text, g ->
                ctl.showStoryCompose = false
                ctl.postStory(text, g)
            },
            onVideo = {
                ctl.showStoryCompose = false
                ctl.onPickStoryVideo()
            }
        )
    }

    ctl.storyIndex?.let { idx ->
        StoryViewer(
            acct = ctl.acct,
            rev = ctl.rev,
            stories = ctl.stories,
            start = idx,
            onDelete = { ctl.deleteStory(it) },
            onClose = { ctl.storyIndex = null }
        )
    }

    ctl.feedIndex?.let { idx ->
        VideoFeed(
            acct = ctl.acct,
            rev = ctl.rev,
            files = ctl.videos,
            start = idx,
            onDelete = { ctl.deleteVideo(it) },
            onClose = { ctl.feedIndex = null }
        )
    }

    ctl.crop?.let { req ->
        CropDialog(
            req = req,
            aspect = if (req.target == CropTarget.AVATAR) 1f else BANNER_ASPECT,
            circle = req.target == CropTarget.AVATAR,
            title = if (req.target == CropTarget.AVATAR) "Crop foto profil" else "Crop banner",
            onCancel = {
                ctl.crop = null
                req.video?.file?.delete()
            },
            onConfirm = { bmp, s, ox, oy, st -> ctl.applyCrop(req, bmp, s, ox, oy, st) }
        )
    }

    ctl.busy?.let { msg ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
                Row(
                    Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text(msg)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(ctl: RexProfileController) {
    val acct = ctl.acct
    var n by remember { mutableStateOf(acct.name) }
    var u by remember { mutableStateOf(acct.username) }
    var b by remember { mutableStateOf(acct.bio) }

    ModalBottomSheet(onDismissRequest = { ctl.showEdit = false }) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit profil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MediaOption(Icons.Default.AddAPhoto, "Banner", "Foto • video 4 dtk • live", Modifier.weight(1f)) {
                    ctl.showEdit = false
                    ctl.onPickBanner()
                }
                MediaOption(Icons.Default.AccountCircle, "Foto profil", "Foto • video 4 dtk • live", Modifier.weight(1f)) {
                    ctl.showEdit = false
                    ctl.onPickAvatar()
                }
            }

            OutlinedTextField(
                value = n, onValueChange = { n = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = u, onValueChange = { u = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = b, onValueChange = { b = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (acct.bannerPath.isNotBlank() || acct.bannerVideo.isNotBlank()) {
                    TextButton(onClick = { ctl.clearBanner() }) { Text("Hapus banner") }
                }
                if (acct.picturePath.isNotBlank()) {
                    TextButton(onClick = { ctl.clearAvatar() }) { Text("Hapus foto profil") }
                }
            }

            Button(
                onClick = {
                    ctl.saveProfile(n, u, b)
                    ctl.showEdit = false
                },
                Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Simpan profil") }
        }
    }
}

/* ═════════════════════ Izin storage ═════════════════════ */

@Composable
private fun PermissionGate(padding: PaddingValues, onAllow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "gate")
    val pulse by inf.animateFloat(1f, 1.12f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")

    Box(Modifier.fillMaxSize().padding(padding)) {
        Aurora(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(110.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = colors.onPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Izin penyimpanan diperlukan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Profil, story, dan video kamu disimpan langsung di Download/RexAps/.config/ — 100% lokal, tanpa server. " +
                    "Tanpa izin ini aplikasi tidak bisa membaca atau menyimpan file tersebut.",
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (Build.VERSION.SDK_INT >= 30) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Kamu akan dibawa ke Pengaturan. Aktifkan \"Izinkan akses ke semua file\", lalu kembali ke aplikasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onAllow,
                Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Izinkan akses") }
        }
    }
}

/* ═════════════════════ Komponen bersama layout profil ═════════════════════ */

@Composable
internal fun StatItem(value: Int, label: String, modifier: Modifier) {
    val v by animateIntAsState(value, tween(900), label = "stat")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$v", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SegTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = c.surfaceVariant.copy(.55f)) {
        Row(Modifier.padding(4.dp)) {
            labels.forEachIndexed { i, l ->
                val sel = i == selected
                val bg by animateColorAsState(if (sel) c.primary else Color.Transparent, label = "tabbg")
                val fg by animateColorAsState(if (sel) c.onPrimary else c.onSurfaceVariant, label = "tabfg")
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .clickable { onSelect(i) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) { Text(l, color = fg, fontWeight = FontWeight.SemiBold) }
            }
        }
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
    Box(
        modifier
            .pressable(onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(c.primary.copy(.18f), c.tertiary.copy(.18f))))
            .border(1.dp, c.primary.copy(.4f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Add, null, tint = c.primary, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MediaOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Surface(modifier.pressable(onClick), shape = RoundedCornerShape(18.dp), color = c.surfaceVariant) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = c.primary)
            Text(title, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        }
    }
}

@Composable
internal fun BannerView(acct: RexLocalAccount, rev: Int, modifier: Modifier, parallax: () -> Float) {
    val colors = MaterialTheme.colorScheme
    val bg = colors.background
    val inf = rememberInfiniteTransition(label = "banner")
    val kb by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "kb")
    val shift by inf.animateFloat(0f, 900f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    var w by remember { mutableFloatStateOf(1f) }
    var h by remember { mutableFloatStateOf(1f) }
    val bmp by produceState<Bitmap?>(null, acct.bannerPath, rev) {
        value = if (acct.bannerPath.isNotBlank()) withContext(Dispatchers.IO) { decodeFileSampled(acct.bannerPath, 2048) } else null
    }
    val hasVideo = acct.bannerVideo.isNotBlank() && File(acct.bannerVideo).exists()

    Box(modifier.clipToBounds().onSizeChanged { w = it.width.toFloat(); h = it.height.toFloat() }) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = parallax() }) {
            val b = bmp
            if (b != null) {
                Image(
                    b.asImageBitmap(), null,
                    Modifier.fillMaxSize().graphicsLayer {
                        if (!hasVideo) {
                            val z = 1.08f + 0.08f * kb
                            scaleX = z; scaleY = z
                            translationX = (kb - .5f) * 24f
                            translationY = (.5f - kb) * 12f
                        }
                    },
                    contentScale = ContentScale.Crop
                )
            } else if (!hasVideo) {
                Box(Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            listOf(colors.primary, colors.tertiary, colors.secondary),
                            start = Offset(shift, 0f),
                            end = Offset(shift + 900f, 900f),
                            tileMode = TileMode.Mirror
                        )
                    )
                })
            }
            if (hasVideo) {
                RexVideo(
                    acct.bannerVideo,
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = acct.bannerScale; scaleY = acct.bannerScale
                        translationX = acct.bannerX * w
                        translationY = acct.bannerY * h
                    },
                    key = rev
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black.copy(.25f), .25f to Color.Transparent, .6f to Color.Transparent, 1f to bg)
        )))
    }
}

@Composable
internal fun AvatarContent(acct: RexLocalAccount, rev: Int, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var sz by remember { mutableStateOf(IntSize.Zero) }
    val bmp by produceState<Bitmap?>(null, acct.picturePath, rev) {
        value = if (acct.picturePath.isBlank()) null
        else withContext(Dispatchers.IO) { decodeFileSampled(acct.picturePath, 768) }
    }
    val hasVideo = animate && acct.pictureVideo.isNotBlank() && File(acct.pictureVideo).exists()

    Box(
        modifier.clip(CircleShape).background(colors.surfaceVariant).onSizeChanged { sz = it },
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.AccountCircle, null, Modifier.fillMaxSize(0.8f), tint = colors.primary)

        if (hasVideo) {
            RexVideo(
                acct.pictureVideo,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = acct.pictureScale; scaleY = acct.pictureScale
                    translationX = acct.pictureX * sz.width
                    translationY = acct.pictureY * sz.height
                },
                key = rev
            )
        }
    }
}

/** Avatar dengan ring story bersegmen. */
@Composable
internal fun StoryRingAvatar(
    acct: RexLocalAccount,
    rev: Int,
    storyCount: Int,
    dia: Dp,
    modifier: Modifier,
    animate: Boolean = true,
    onClick: () -> Unit,
    onAdd: (() -> Unit)? = null
) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "ring")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "angle")
    val bob by inf.animateFloat(-3f, 3f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val pop by animateFloatAsState(if (appear) 1f else .2f, spring(Spring.DampingRatioMediumBouncy), label = "pop")
    val ringColors = listOf(c.primary, c.tertiary, c.secondary, c.primary)

    Box(
        modifier
            .size(dia)
            .graphicsLayer {
                translationY = if (animate) bob * density else 0f
                scaleX = pop
                scaleY = pop
            }
            .drawBehind {
                val sw = 3.5.dp.toPx()
                val d = this.size.minDimension
                rotate(angle) {
                    val brush = Brush.sweepGradient(ringColors)
                    if (storyCount in 2..12) {
                        val seg = 360f / storyCount
                        for (i in 0 until storyCount) {
                            drawArc(
                                brush, -90f + i * seg + 5f, seg - 10f, false,
                                Offset(sw / 2, sw / 2), Size(d - sw, d - sw),
                                style = Stroke(sw, cap = StrokeCap.Round)
                            )
                        }
                    } else {
                        drawCircle(
                            brush, radius = d / 2 - sw / 2,
                            style = Stroke(sw),
                            alpha = if (storyCount == 0) .5f else 1f
                        )
                    }
                }
            }
    ) {
        AvatarContent(acct, rev, animate, Modifier.fillMaxSize().padding(7.dp).pressable(onClick))
        if (onAdd != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(c.primary)
                    .border(3.dp, c.background, CircleShape)
                    .pressable(onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun MediaThumb(file: File, modifier: Modifier) {
    val ctx = LocalContext.current
    val isVideo = file.extension.lowercase() == "mp4"
    val bmp by produceState<Bitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) {
            if (isVideo) videoFrameAt(ctx, VSrc(null, file), 0) else decodeFileSampled(file.path, 600)
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (isVideo) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(.9f), modifier = Modifier.size(34.dp))
        }
    }
}

/** Galeri (video + story) dengan tab. Dipakai portrait & landscape. */
@Composable
internal fun ProfileTabs(ctl: RexProfileController, columns: Int, cellAspect: Float) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SegTabs(listOf("Video saya", "Story 24 jam"), ctl.tab) { ctl.tab = it }

        AnimatedContent(
            ctl.tab,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
            label = "tab"
        ) { t ->
            if (t == 0) {
                RexGrid(ctl.videos.size + 1, columns, cellAspect) { i, m ->
                    if (i == 0) {
                        AddTile(m, "Tambah", "mp4 • png • jpg") { ctl.onPickVideo() }
                    } else {
                        val f = ctl.videos[i - 1]
                        Box(m.pressable { ctl.feedIndex = i - 1 }.clip(RoundedCornerShape(16.dp))) {
                            MediaThumb(f, Modifier.fillMaxSize())
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(0f to Color.Transparent, .65f to Color.Transparent, 1f to Color.Black.copy(.55f))
                                    )
                                )
                            )
                            Icon(
                                if (f.extension.lowercase() == "mp4") Icons.Default.Movie else Icons.Default.Image,
                                null,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(16.dp)
                            )
                        }
                    }
                }
            } else {
                RexGrid(ctl.stories.size + 1, columns, cellAspect) { i, m ->
                    if (i == 0) AddTile(m, "Story baru", "teks • mp4") { ctl.showStoryCompose = true }
                    else StoryTile(ctl.stories[i - 1], ctl.acct, ctl.rev, m) { ctl.storyIndex = i - 1 }
                }
            }
        }

        Text(
            "Data: ${RexPaths.base.absolutePath}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(.7f)
        )
    }
}

@Composable
internal fun ProfileIdentity(acct: RexLocalAccount, centered: Boolean) {
    val colors = MaterialTheme.colorScheme
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = align,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            acct.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = textAlign
        )
        Surface(shape = CircleShape, color = colors.primary.copy(.14f)) {
            Text(
                acct.username,
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = colors.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (acct.bio.isNotBlank()) {
            Text(
                acct.bio,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign
            )
        }
    }
}

@Composable
internal fun ProfileStats(ctl: RexProfileController) {
    val colors = MaterialTheme.colorScheme
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            StatItem(ctl.stories.size, "Story", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(28.dp).background(colors.outlineVariant))
            StatItem(ctl.videos.size, "Video", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(28.dp).background(colors.outlineVariant))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Text("100% Lokal", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ProfileActions(ctl: RexProfileController) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { ctl.showEdit = true },
            Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit profil")
        }
        FilledTonalButton(
            onClick = { ctl.showStoryCompose = true },
            Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buat story")
        }
    }
}

@Composable
private fun StoryTile(st: RexStory, acct: RexLocalAccount, rev: Int, modifier: Modifier, onClick: () -> Unit) {
    val left = (DAY_MS - (System.currentTimeMillis() - st.time)).coerceAtLeast(0)
    Box(modifier.pressable(onClick).clip(RoundedCornerShape(16.dp))) {
        if (st.isVideo) {
            MediaThumb(st.file, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(storyGradients[st.gradient.mod(storyGradients.size)])
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    st.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black.copy(.35f), .3f to Color.Transparent, .7f to Color.Transparent, 1f to Color.Black.copy(.6f))
        )))
        AvatarContent(
            acct, rev, false,
            Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).border(2.dp, Color.White, CircleShape)
        )
        Text(
            "${left / 3_600_000}j lagi",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
    }
}

@Composable
internal fun StoryComposeDialog(onDismiss: () -> Unit, onText: (String, Int) -> Unit, onVideo: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var g by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Buat story", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(storyGradients[g])),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text.ifBlank { "Tulis sesuatu…" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 200) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Teks story") },
                    shape = RoundedCornerShape(14.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    storyGradients.forEachIndexed { i, cols ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(cols))
                                .border(if (i == g) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { g = i }
                        )
                    }
                }
                OutlinedButton(onClick = onVideo, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Videocam, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih video .mp4")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onText(text.trim(), g) }) { Text("Posting teks") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
internal fun StoryViewer(
    acct: RexLocalAccount,
    rev: Int,
    stories: List<RexStory>,
    start: Int,
    onDelete: (RexStory) -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    var index by remember { mutableIntStateOf(start) }
    val progress = remember { Animatable(0f) }
    var paused by remember { mutableStateOf(false) }
    var dur by remember(index) { mutableLongStateOf(0L) }
    val story = stories.getOrNull(index)
    if (story == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    LaunchedEffect(index) {
        progress.snapTo(0f)
        dur = if (story.isVideo) {
            withContext(Dispatchers.IO) { videoDurationOf(ctx, VSrc(null, story.file)) }.takeIf { it > 0 } ?: 15000L
        } else 5000L
    }
    LaunchedEffect(index, paused, dur) {
        if (dur > 0 && !paused) {
            val remaining = ((1f - progress.value) * dur).toInt().coerceAtLeast(200)
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            if (index < stories.lastIndex) index++ else onClose()
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black)
                .pointerInput(index) {
                    detectTapGestures(
                        onPress = { paused = true; tryAwaitRelease(); paused = false },
                        onTap = { o ->
                            if (o.x < size.width / 3f) { if (index > 0) index-- }
                            else if (index < stories.lastIndex) index++ else onClose()
                        }
                    )
                }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > 160f) onClose() }
                    ) { _, d -> total += d }
                }
        ) {
            if (story.isVideo) {
                RexVideo(story.file.path, Modifier.fillMaxSize(), key = index, loop = false, muted = false, playing = !paused, fit = true)
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(storyGradients[story.gradient.mod(storyGradients.size)])
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        story.text,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(.55f), Color.Transparent))))

            Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    stories.forEachIndexed { i, _ ->
                        val frac = when {
                            i < index -> 1f
                            i == index -> progress.value
                            else -> 0f
                        }
                        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(.3f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Color.White))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AvatarContent(acct, rev, false, Modifier.size(38.dp).border(1.5.dp, Color.White, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        val left = (DAY_MS - (System.currentTimeMillis() - story.time)).coerceAtLeast(0)
                        Text(acct.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Hilang ${left / 3_600_000}j ${left / 60_000 % 60}m lagi",
                            color = Color.White.copy(.75f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(onClick = { onDelete(story); onClose() }) { Icon(Icons.Default.Delete, null, tint = Color.White) }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}

/* ═════════════════════ Feed video ═════════════════════ */

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

@Composable
private fun FeedItem(file: File, acct: RexLocalAccount, rev: Int, active: Boolean) {
    val isVideo = file.extension.lowercase() == "mp4"
    var userPlaying by remember(active) { mutableStateOf(true) }

    Box(
        Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { userPlaying = !userPlaying },
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            RexVideo(file.path, Modifier.fillMaxSize(), loop = true, muted = false, playing = active && userPlaying, fit = true)
            if (active && !userPlaying) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(.85f), modifier = Modifier.size(88.dp))
            }
        } else {
            val bmp by produceState<Bitmap?>(null, file.path) {
                value = withContext(Dispatchers.IO) { decodeFileSampled(file.path, 2000) }
            }
            bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }

        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.6f)))))

        Row(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarContent(acct, rev, false, Modifier.size(42.dp).border(2.dp, Color.White, CircleShape))
            Column {
                Text(acct.username, color = Color.White, fontWeight = FontWeight.Bold)
                Text(file.name, color = Color.White.copy(.75f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}