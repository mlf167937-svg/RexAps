package com.rexaps.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.rexaps.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/* ───────────────────────── Konstanta & model ───────────────────────── */

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val BANNER_ASPECT = 16f / 9f

private object RexPaths {
    val base get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RexAps/.config")
    val account get() = File(base, "account")
    val video get() = File(account, "video")
    val story get() = File(base, "story")
    val config get() = File(base, "account.json")
}

private data class RexLocalAccount(
    val name: String = "Rex User",
    val username: String = "@rexuser",
    val bio: String = "Halo! Aku menggunakan RexAps.",
    val bannerType: String = "none", // none | image | video
    val bannerPath: String = "",
    val bannerScale: Float = 1f,
    val bannerX: Float = 0f,
    val bannerY: Float = 0f,
    val picturePath: String = ""
)

private data class RexStory(val file: File, val time: Long, val isVideo: Boolean, val text: String, val gradient: Int)

private enum class CropTarget { AVATAR, BANNER_IMAGE, BANNER_VIDEO }
private class CropReq(val target: CropTarget, val bitmap: Bitmap, val uri: Uri)

private val storyGradients = listOf(
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF232526), Color(0xFF414345)),
    listOf(Color(0xFFEE0979), Color(0xFFFF6A00))
)

/* ───────────────────────── File helpers ───────────────────────── */

private fun hasStorageAccess(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

private fun readAccount(): RexLocalAccount? = try {
    val f = RexPaths.config
    if (!f.exists()) null else JSONObject(f.readText()).let { j ->
        RexLocalAccount(
            j.optString("name", "Rex User"), j.optString("username", "@rexuser"), j.optString("bio", ""),
            j.optString("bannerType", "none"), j.optString("banner", ""),
            j.optDouble("bannerScale", 1.0).toFloat(), j.optDouble("bannerX", 0.0).toFloat(),
            j.optDouble("bannerY", 0.0).toFloat(), j.optString("profilePicture", "")
        )
    }
} catch (e: Exception) { null }

private fun writeAccount(a: RexLocalAccount) {
    RexPaths.config.parentFile?.mkdirs()
    val j = JSONObject().put("name", a.name).put("username", a.username).put("bio", a.bio)
        .put("bannerType", a.bannerType).put("banner", a.bannerPath)
        .put("bannerScale", a.bannerScale.toDouble()).put("bannerX", a.bannerX.toDouble())
        .put("bannerY", a.bannerY.toDouble()).put("profilePicture", a.picturePath)
        .put("updatedAt", System.currentTimeMillis())
    RexPaths.config.writeText(j.toString(2))
}

/** Hapus otomatis story > 24 jam (berdasarkan timestamp di nama file). */
private fun loadStories(): List<RexStory> {
    val now = System.currentTimeMillis()
    val files = RexPaths.story.listFiles() ?: return emptyList()
    return files.mapNotNull { f ->
        val t = f.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null
        if (now - t > DAY_MS) { f.delete(); return@mapNotNull null }
        when (f.extension.lowercase()) {
            "mp4" -> RexStory(f, t, true, "", 0)
            "json" -> try {
                val j = JSONObject(f.readText()); RexStory(f, t, false, j.optString("text"), j.optInt("gradient"))
            } catch (e: Exception) { null }
            else -> null
        }
    }.sortedBy { it.time }
}

private fun loadVideos(): List<File> =
    RexPaths.video.listFiles()?.filter { it.extension.lowercase() in setOf("mp4", "png", "jpg", "jpeg") }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()

private fun decodeSampled(ctx: Context, uri: Uri, maxDim: Int = 2048): Bitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1
    while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
    ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
    }
} catch (e: Exception) { null }

private fun decodeFileSampled(path: String, maxDim: Int): Bitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, o)
    var s = 1
    while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
} catch (e: Exception) { null }

private fun videoFrame(setup: MediaMetadataRetriever.() -> Unit): Bitmap? {
    val r = MediaMetadataRetriever()
    return try { r.setup(); r.getFrameAtTime(0) } catch (e: Exception) { null }
    finally { try { r.release() } catch (_: Exception) {} }
}

private fun videoDuration(path: String): Long {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(path); r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 15000L
    } catch (e: Exception) { 15000L } finally { try { r.release() } catch (_: Exception) {} }
}

/* ───────────────────────── Engine crop ───────────────────────── */

/** Batasi geseran supaya frame crop selalu penuh (tidak ada area kosong). */
private fun clampOffset(bw: Int, bh: Int, fw: Float, fh: Float, s: Float, ox: Float, oy: Float): Pair<Float, Float> {
    val base = max(fw / bw, fh / bh) * s
    val mx = max(0f, (bw * base - fw) / 2f) / fw
    val my = max(0f, (bh * base - fh) / 2f) / fh
    return ox.coerceIn(-mx, mx) to oy.coerceIn(-my, my)
}

/** Hasil crop gambar sebenarnya (disimpan sebagai PNG). */
private fun renderCrop(src: Bitmap, aspect: Float, outW: Int, s: Float, ox: Float, oy: Float): Bitmap {
    val outH = (outW / aspect).toInt()
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val base = max(outW.toFloat() / src.width, outH.toFloat() / src.height) * s
    val m = android.graphics.Matrix().apply {
        postTranslate(-src.width / 2f, -src.height / 2f)
        postScale(base, base)
        postTranslate(outW / 2f + ox * outW, outH / 2f + oy * outH)
    }
    android.graphics.Canvas(out).drawBitmap(src, m, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
    return out
}

@Composable
private fun CropDialog(
    bitmap: Bitmap, aspect: Float, circle: Boolean, title: String,
    onCancel: () -> Unit, onConfirm: (Float, Float, Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    var fw by remember { mutableFloatStateOf(1f) }
    var fh by remember { mutableFloatStateOf(1f) }
    val img = remember(bitmap) { bitmap.asImageBitmap() }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Cubit untuk zoom, geser untuk atur posisi", color = Color.White.copy(.7f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(22.dp))
                Box(
                    Modifier.fillMaxWidth().aspectRatio(aspect)
                        .clip(if (circle) CircleShape else RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                        .onSizeChanged { fw = it.width.toFloat(); fh = it.height.toFloat() }
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                val (cx, cy) = clampOffset(bitmap.width, bitmap.height, fw, fh, scale, ox + pan.x / fw, oy + pan.y / fh)
                                ox = cx; oy = cy
                            }
                        }
                ) {
                    Image(
                        img, null,
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale; scaleY = scale; translationX = ox * fw; translationY = oy * fh
                        },
                        contentScale = ContentScale.Crop
                    )
                }
                Slider(
                    value = scale, valueRange = 1f..6f,
                    onValueChange = {
                        scale = it
                        val (cx, cy) = clampOffset(bitmap.width, bitmap.height, fw, fh, scale, ox, oy); ox = cx; oy = cy
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, Modifier.weight(1f)) { Text("Batal") }
                    Button(onClick = { onConfirm(scale, ox, oy) }, Modifier.weight(1f)) { Text("Terapkan") }
                }
            }
        }
    }
}

/* ───────────────────────── Player video ───────────────────────── */

@Composable
private fun RexVideo(
    path: String, modifier: Modifier = Modifier, key: Any = path,
    loop: Boolean = true, muted: Boolean = true, playing: Boolean = true, fit: Boolean = false
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
            (LayoutInflater.from(c).inflate(R.layout.rex_player_texture, null) as PlayerView).apply { this.player = player }
        },
        update = {
            it.player = player
            it.resizeMode = if (fit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    )
}

/* ───────────────────────── Entry point + izin ───────────────────────── */

@Composable
fun RexLocalAccountScreen(padding: PaddingValues) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasStorageAccess(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val ob = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) granted = hasStorageAccess(context) }
        lifecycle.addObserver(ob)
        onDispose { lifecycle.removeObserver(ob) }
    }
    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasStorageAccess(context)
    }
    if (!granted) {
        PermissionGate(padding) {
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")))
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                legacyLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        }
    } else {
        RexProfileHome(padding)
    }
}

@Composable
private fun PermissionGate(padding: PaddingValues, onAllow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "gate")
    val pulse by inf.animateFloat(1f, 1.12f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")
    Column(
        Modifier.fillMaxSize().padding(padding).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(110.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Lock, null, tint = colors.onPrimary, modifier = Modifier.size(48.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("Izin penyimpanan diperlukan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "RexAps menyimpan profil, story, dan video kamu langsung di folder Download/RexAps/.config/ agar tetap lokal tanpa server. " +
                "Tanpa izin ini, aplikasi tidak bisa membaca atau menyimpan file tersebut.",
            color = colors.onSurfaceVariant, textAlign = TextAlign.Center
        )
        if (Build.VERSION.SDK_INT >= 30) {
            Spacer(Modifier.height(8.dp))
            Text("Kamu akan dibawa ke Pengaturan. Aktifkan \"Izinkan akses ke semua file\", lalu kembali ke aplikasi.",
                style = MaterialTheme.typography.bodySmall, color = colors.primary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAllow, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) { Text("Izinkan akses") }
    }
}

/* ───────────────────────── Halaman utama (aesthetic) ───────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RexProfileHome(padding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    var acct by remember { mutableStateOf(RexLocalAccount()) }
    var rev by remember { mutableIntStateOf(0) }
    var stories by remember { mutableStateOf(listOf<RexStory>()) }
    var videos by remember { mutableStateOf(listOf<File>()) }
    var showEdit by remember { mutableStateOf(false) }
    var showStoryCompose by remember { mutableStateOf(false) }
    var storyIndex by remember { mutableStateOf<Int?>(null) }
    var feedIndex by remember { mutableStateOf<Int?>(null) }
    var crop by remember { mutableStateOf<CropReq?>(null) }
    var visible by remember { mutableStateOf(false) }

    fun refreshStories() { scope.launch { stories = withContext(Dispatchers.IO) { loadStories() } } }
    fun refreshVideos() { scope.launch { videos = withContext(Dispatchers.IO) { loadVideos() } } }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { RexPaths.story.mkdirs(); RexPaths.video.mkdirs() }
        withContext(Dispatchers.IO) { readAccount() }?.let { acct = it; rev++ }
        videos = withContext(Dispatchers.IO) { loadVideos() }
        visible = true
        while (true) { stories = withContext(Dispatchers.IO) { loadStories() }; delay(60_000) }
    }

    /* ── pickers ── */
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val isVid = (context.contentResolver.getType(uri) ?: "").startsWith("video")
            val bmp = withContext(Dispatchers.IO) {
                if (isVid) videoFrame { setDataSource(context, uri) } else decodeSampled(context, uri)
            }
            if (bmp == null) toast("Media tidak bisa dibaca")
            else crop = CropReq(if (isVid) CropTarget.BANNER_VIDEO else CropTarget.BANNER_IMAGE, bmp, uri)
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val bmp = withContext(Dispatchers.IO) { decodeSampled(context, uri) }
            if (bmp == null) toast("Gambar tidak bisa dibaca") else crop = CropReq(CropTarget.AVATAR, bmp, uri)
        }
    }
    val storyVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            try {
                RexPaths.story.mkdirs()
                val f = File(RexPaths.story, "${System.currentTimeMillis()}.mp4")
                context.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(f).use { i.copyTo(it) } }
                withContext(Dispatchers.Main) { toast("Story diposting (hilang dalam 24 jam)") }
                stories = loadStories()
            } catch (e: Exception) { withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") } }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            try {
                RexPaths.video.mkdirs()
                val mime = context.contentResolver.getType(uri) ?: ""
                val ext = when { mime.startsWith("video") -> "mp4"; mime.contains("png") -> "png"; else -> "jpg" }
                val f = File(RexPaths.video, "${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(f).use { i.copyTo(it) } }
                videos = loadVideos()
            } catch (e: Exception) { withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") } }
        }
    }

    fun applyCrop(req: CropReq, s: Float, ox: Float, oy: Float) {
        crop = null
        scope.launch(Dispatchers.IO) {
            try {
                RexPaths.account.mkdirs()
                var a = acct
                when (req.target) {
                    CropTarget.AVATAR -> {
                        val out = renderCrop(req.bitmap, 1f, 768, s, ox, oy)
                        val f = File(RexPaths.account, "profile-pictures.png")
                        FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        a = a.copy(picturePath = f.absolutePath)
                    }
                    CropTarget.BANNER_IMAGE -> {
                        val out = renderCrop(req.bitmap, BANNER_ASPECT, 1600, s, ox, oy)
                        val f = File(RexPaths.account, "banner.png")
                        FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        File(RexPaths.account, "banner.mp4").delete()
                        a = a.copy(bannerType = "image", bannerPath = f.absolutePath, bannerScale = 1f, bannerX = 0f, bannerY = 0f)
                    }
                    CropTarget.BANNER_VIDEO -> {
                        val f = File(RexPaths.account, "banner.mp4")
                        context.contentResolver.openInputStream(req.uri)?.use { i -> FileOutputStream(f).use { i.copyTo(it) } }
                        File(RexPaths.account, "banner.png").delete()
                        a = a.copy(bannerType = "video", bannerPath = f.absolutePath, bannerScale = s, bannerX = ox, bannerY = oy)
                    }
                }
                writeAccount(a)
                withContext(Dispatchers.Main) { acct = a; rev++ }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal menyimpan: ${e.message ?: "izin ditolak"}") }
            }
        }
    }

    /* ── konten ── */
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(scroll)) {
        // Banner + avatar (avatar menempel di banner)
        Box(Modifier.fillMaxWidth()) {
            BannerView(acct, rev, Modifier.fillMaxWidth().aspectRatio(BANNER_ASPECT)) { scroll.value * 0.5f }
            AnimatedAvatar(
                acct.picturePath, rev,
                Modifier.align(Alignment.BottomStart).offset(x = 20.dp, y = 52.dp)
            ) { showEdit = true }
        }

        Column(Modifier.padding(horizontal = 20.dp).padding(top = 62.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            AnimatedVisibility(visible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 3 }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(acct.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(acct.username, color = colors.primary)
                        }
                        FilledTonalButton(onClick = { showEdit = true }) {
                            Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit")
                        }
                    }
                    if (acct.bio.isNotBlank()) Text(acct.bio, color = colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        AssistChip(onClick = {}, label = { Text("${stories.size} story aktif") })
                        AssistChip(onClick = {}, label = { Text("${videos.size} video") })
                    }
                }
            }

            // Story
            Text("Story • hilang dalam 24 jam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(colors.surfaceVariant)
                            .border(2.dp, colors.outline, CircleShape).clickable { showStoryCompose = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, null, tint = colors.primary) }
                }
                items(stories.size) { i ->
                    val st = stories[i]
                    Box(
                        Modifier.size(72.dp)
                            .border(3.dp, Brush.sweepGradient(listOf(colors.primary, colors.tertiary, colors.primary)), CircleShape)
                            .padding(5.dp).clip(CircleShape).clickable { storyIndex = i },
                        contentAlignment = Alignment.Center
                    ) {
                        if (st.isVideo) MediaThumb(st.file, Modifier.fillMaxSize())
                        else Box(
                            Modifier.fillMaxSize().background(Brush.linearGradient(storyGradients[st.gradient.mod(storyGradients.size)])),
                            contentAlignment = Alignment.Center
                        ) { Text("Aa", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // Video saya
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Video saya", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { videoPicker.launch(arrayOf("video/mp4", "image/png", "image/jpeg")) }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Tambah")
                }
            }
            if (videos.isEmpty()) {
                Text("Belum ada. Tambahkan .mp4, .png, atau .jpg lalu geser seperti TikTok.", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                videos.chunked(3).forEachIndexed { rowIdx, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { colIdx, f ->
                            MediaThumb(
                                f, Modifier.weight(1f).aspectRatio(9f / 16f).clip(RoundedCornerShape(14.dp))
                                    .clickable { feedIndex = rowIdx * 3 + colIdx }
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Text("Data: ${RexPaths.base.absolutePath}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }

    /* ── overlay / dialog ── */
    if (showEdit) {
        var n by remember { mutableStateOf(acct.name) }
        var u by remember { mutableStateOf(acct.username) }
        var b by remember { mutableStateOf(acct.bio) }
        ModalBottomSheet(onDismissRequest = { showEdit = false }) {
            Column(
                Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit profil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(n, { n = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(u, { u = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { bannerPicker.launch(arrayOf("image/*", "video/*")) }, Modifier.weight(1f)) {
                        Icon(Icons.Default.AddAPhoto, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Banner")
                    }
                    OutlinedButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }, Modifier.weight(1f)) {
                        Icon(Icons.Default.AccountCircle, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Foto profil")
                    }
                }
                if (acct.bannerType != "none") {
                    TextButton(onClick = {
                        val a = acct.copy(bannerType = "none", bannerPath = "")
                        acct = a; rev++
                        scope.launch(Dispatchers.IO) {
                            File(RexPaths.account, "banner.png").delete(); File(RexPaths.account, "banner.mp4").delete(); writeAccount(a)
                        }
                    }) { Text("Hapus banner") }
                }
                Button(
                    onClick = {
                        val a = acct.copy(name = n.trim().ifBlank { "Rex User" }, username = u.trim().ifBlank { "@rexuser" }, bio = b.trim())
                        acct = a
                        scope.launch(Dispatchers.IO) {
                            try { writeAccount(a); withContext(Dispatchers.Main) { toast("Profil tersimpan") } }
                            catch (e: Exception) { withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") } }
                        }
                        showEdit = false
                    },
                    Modifier.fillMaxWidth()
                ) { Text("Simpan profil") }
            }
        }
    }

    if (showStoryCompose) {
        StoryComposeDialog(
            onDismiss = { showStoryCompose = false },
            onText = { text, g ->
                showStoryCompose = false
                scope.launch(Dispatchers.IO) {
                    RexPaths.story.mkdirs()
                    File(RexPaths.story, "${System.currentTimeMillis()}.json")
                        .writeText(JSONObject().put("text", text).put("gradient", g).toString())
                    stories = loadStories()
                }
            },
            onVideo = { showStoryCompose = false; storyVideoPicker.launch("video/*") }
        )
    }

    storyIndex?.let { idx ->
        StoryViewer(
            stories, idx,
            onDelete = { s -> scope.launch(Dispatchers.IO) { s.file.delete(); stories = loadStories() } },
            onClose = { storyIndex = null }
        )
    }

    feedIndex?.let { idx ->
        VideoFeed(
            videos, idx,
            onDelete = { f -> scope.launch(Dispatchers.IO) { f.delete(); videos = loadVideos() } },
            onClose = { feedIndex = null }
        )
    }

    crop?.let { req ->
        CropDialog(
            bitmap = req.bitmap,
            aspect = if (req.target == CropTarget.AVATAR) 1f else BANNER_ASPECT,
            circle = req.target == CropTarget.AVATAR,
            title = if (req.target == CropTarget.AVATAR) "Crop foto profil" else "Crop banner",
            onCancel = { crop = null },
            onConfirm = { s, ox, oy -> applyCrop(req, s, ox, oy) }
        )
    }
}

/* ───────────────────────── Komponen visual ───────────────────────── */

@Composable
private fun BannerView(acct: RexLocalAccount, rev: Int, modifier: Modifier, parallax: () -> Float) {
    val colors = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "banner")
    val kb by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "kb")
    val shift by inf.animateFloat(0f, 900f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    var w by remember { mutableFloatStateOf(1f) }
    var h by remember { mutableFloatStateOf(1f) }
    val bmp by produceState<Bitmap?>(null, acct.bannerPath, acct.bannerType, rev) {
        value = if (acct.bannerType == "image") withContext(Dispatchers.IO) { decodeFileSampled(acct.bannerPath, 2048) } else null
    }
    Box(modifier.clipToBounds().onSizeChanged { w = it.width.toFloat(); h = it.height.toFloat() }) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = parallax() }) {
            val b = bmp
            when {
                acct.bannerType == "video" && File(acct.bannerPath).exists() -> RexVideo(
                    acct.bannerPath, Modifier.fillMaxSize().graphicsLayer {
                        scaleX = acct.bannerScale; scaleY = acct.bannerScale
                        translationX = acct.bannerX * w; translationY = acct.bannerY * h
                    }, key = rev
                )
                b != null -> Image(
                    b.asImageBitmap(), null,
                    Modifier.fillMaxSize().graphicsLayer {
                        val z = 1.06f + 0.08f * kb          // efek Ken Burns pelan
                        scaleX = z; scaleY = z
                        translationX = (kb - .5f) * 24f; translationY = (.5f - kb) * 12f
                    },
                    contentScale = ContentScale.Crop
                )
                else -> Box(Modifier.fillMaxSize().drawBehind {
                    drawRect(Brush.linearGradient(
                        listOf(colors.primary, colors.tertiary, colors.secondary),
                        start = Offset(shift, 0f), end = Offset(shift + 900f, 900f), tileMode = TileMode.Mirror
                    ))
                })
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.35f)))))
    }
}

@Composable
private fun AnimatedAvatar(path: String, rev: Int, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "avatar")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ring")
    val bob by inf.animateFloat(-4f, 4f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val pop by animateFloatAsState(if (appear) 1f else 0.2f, spring(Spring.DampingRatioMediumBouncy), label = "pop")
    val bmp by produceState<Bitmap?>(null, path, rev) {
        value = if (path.isBlank()) null else withContext(Dispatchers.IO) { decodeFileSampled(path, 768) }
    }
    Box(
        modifier.size(104.dp)
            .graphicsLayer { translationY = bob * density; scaleX = pop; scaleY = pop }
            .drawBehind {
                rotate(angle) {
                    drawCircle(
                        Brush.sweepGradient(listOf(colors.primary, colors.tertiary, colors.secondary, colors.primary)),
                        radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
            .padding(7.dp).clip(CircleShape).background(colors.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.AccountCircle, null, Modifier.size(72.dp), tint = colors.primary)
    }
}

@Composable
private fun MediaThumb(file: File, modifier: Modifier) {
    val isVideo = file.extension.lowercase() == "mp4"
    val bmp by produceState<Bitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) {
            if (isVideo) videoFrame { setDataSource(file.path) } else decodeFileSampled(file.path, 480)
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (isVideo) Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(.9f))
    }
}

/* ───────────────────────── Story ───────────────────────── */

@Composable
private fun StoryComposeDialog(onDismiss: () -> Unit, onText: (String, Int) -> Unit, onVideo: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var g by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat story") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(storyGradients[g])),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text.ifBlank { "Tulis sesuatu…" }, color = Color.White, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp), maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
                OutlinedTextField(text, { if (it.length <= 200) text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Teks story") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    storyGradients.forEachIndexed { i, c ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(c))
                                .border(if (i == g) 3.dp else 0.dp, Color.White, CircleShape).clickable { g = i }
                        )
                    }
                }
                TextButton(onClick = onVideo) { Text("Atau pilih video .mp4") }
            }
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onText(text.trim(), g) }) { Text("Posting teks") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun StoryViewer(stories: List<RexStory>, start: Int, onDelete: (RexStory) -> Unit, onClose: () -> Unit) {
    var index by remember { mutableIntStateOf(start) }
    val progress = remember { Animatable(0f) }
    val story = stories.getOrNull(index)
    if (story == null) { LaunchedEffect(Unit) { onClose() }; return }

    val next = { if (index < stories.lastIndex) index++ else onClose() }
    LaunchedEffect(index) {
        val d = if (story.isVideo) withContext(Dispatchers.IO) { videoDuration(story.file.path) } else 5000L
        progress.snapTo(0f)
        progress.animateTo(1f, tween(d.toInt().coerceIn(2000, 60000), easing = LinearEasing))
        next()
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).pointerInput(index) {
                detectTapGestures { o -> if (o.x < size.width / 3f) { if (index > 0) index-- } else next() }
            }
        ) {
            if (story.isVideo) RexVideo(story.file.path, Modifier.fillMaxSize(), loop = false, muted = false, fit = true)
            else Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(storyGradients[story.gradient.mod(storyGradients.size)])),
                contentAlignment = Alignment.Center
            ) {
                Text(story.text, color = Color.White, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            }
            Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    stories.forEachIndexed { i, _ ->
                        val frac = when { i < index -> 1f; i == index -> progress.value; else -> 0f }
                        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(.3f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Color.White))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val left = DAY_MS - (System.currentTimeMillis() - story.time)
                    Text("Hilang ${left / 3_600_000}j ${left / 60_000 % 60}m lagi", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDelete(story); onClose() }) { Icon(Icons.Default.Delete, null, tint = Color.White) }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}

/* ───────────────────────── Feed ala TikTok ───────────────────────── */

@Composable
private fun VideoFeed(files: List<File>, start: Int, onDelete: (File) -> Unit, onClose: () -> Unit) {
    if (files.isEmpty()) { LaunchedEffect(Unit) { onClose() }; return }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, files.lastIndex)) { files.size }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(pager, Modifier.fillMaxSize()) { page ->
                FeedItem(files[page], active = pager.currentPage == page)
            }
            Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${pager.currentPage + 1}/${files.size}", color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDelete(files[pager.currentPage]) }) { Icon(Icons.Default.Delete, null, tint = Color.White) }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }
}

@Composable
private fun FeedItem(file: File, active: Boolean) {
    val isVideo = file.extension.lowercase() == "mp4"
    var userPlaying by remember(active) { mutableStateOf(true) }
    Box(
        Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { userPlaying = !userPlaying },
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            RexVideo(file.path, Modifier.fillMaxSize(), loop = true, muted = false, playing = active && userPlaying, fit = true)
            if (active && !userPlaying) Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(.85f), modifier = Modifier.size(88.dp))
        } else {
            val bmp by produceState<Bitmap?>(null, file.path) { value = withContext(Dispatchers.IO) { decodeFileSampled(file.path, 2000) } }
            bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
        Text(file.name, color = Color.White.copy(.8f), style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(16.dp))
    }
}

/* ───────────────────────── Tab tema (tidak berubah) ───────────────────────── */

@Composable
fun ThemeTabContent(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onOpenTheme: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tema", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personalisasikan tampilan RexAps dengan 30 pilihan tema.", color = colors.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTheme),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(
                    Brush.linearGradient(listOf(colors.primary, colors.tertiary))
                ), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ColorLens, null, tint = colors.onPrimary, modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Tema aktif", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(selectedTheme.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ketuk untuk memilih tema", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}