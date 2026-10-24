// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
internal fun MediaOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier.pressable(onClick),
        shape = RoundedCornerShape(20.dp),
        color = c.surfaceVariant.copy(.7f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(.4f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.primary.copy(.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = c.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
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

fun RexProfileController.__split_handlePick_2(uri: Uri?, target: CropTarget) {
        if (uri == null) return
        scope.launch {
            busy = "Membaca media…"
            val req = withContext(Dispatchers.IO) { buildCropReq(context, uri, target) }
            busy = null
            if (req == null) toast("Media tidak bisa dibaca") else crop = req
        }
    }

fun RexProfileController.__split_addVideo_4(uri: Uri?) {
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

fun RexProfileController.__split_applyCrop_5(req: CropReq, bmp: Bitmap, s: Float, ox: Float, oy: Float, startMs: Long) {
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
