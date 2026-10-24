package com.rexaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
internal fun __splitImpl_rememberRexProfileController(): RexProfileController {
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

internal fun __splitImpl_buildCropReq(ctx: Context, uri: Uri, target: CropTarget): CropReq? {
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

@Composable
internal fun __splitImpl_ProfileStats(ctl: RexProfileController) {
    val colors = MaterialTheme.colorScheme
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(ctl.stories.size, "Story", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(colors.outlineVariant.copy(.6f)))
            StatItem(ctl.videos.size, "Video", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(colors.outlineVariant.copy(.6f)))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text("100% Lokal", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

internal fun __splitImpl_readAccount(): RexLocalAccount? = try {
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

@Composable
internal fun __splitImpl_RexProfileRoot(padding: PaddingValues) {
    val ctl = rememberRexProfileController()
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(Modifier.fillMaxSize().padding(padding)) {
        if (isLandscape) {
            RexAccountScreenLandscape(ctl)
        } else {
            RexAccountScreenPortrait(ctl)
        }

        Surface(
            Modifier.align(Alignment.TopEnd).padding(14.dp),
            shape = CircleShape,
            color = Color.Black.copy(.35f),
            border = BorderStroke(1.dp, Color.White.copy(.18f))
        ) {
            IconButton(onClick = { ctl.showEdit = true }) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
            }
        }
    }

    RexProfileOverlays(ctl)
}

@Composable
internal fun __splitImpl_ProfileActions(ctl: RexProfileController) {
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

internal fun RexProfileController.__splitImpl_addStoryVideo(uri: Uri?) {
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

internal fun __splitImpl_writeAccount(a: RexLocalAccount) {
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

internal fun __splitImpl_renderCrop(src: Bitmap, aspect: Float, outW: Int, s: Float, ox: Float, oy: Float): Bitmap {
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
