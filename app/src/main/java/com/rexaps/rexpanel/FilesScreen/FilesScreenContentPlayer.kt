package com.rexaps.rexpanel

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun StateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
        if (secondaryLabel != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

@Composable
internal fun ImagePreview(b: FileBrowserState, f: RemoteFile) {
    var progress by remember { mutableLongStateOf(0L) }
    var attempt by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ImgLoad?>(null) }

    LaunchedEffect(f.path, attempt) {
        result = null
        progress = 0
        result = try {
            val local = b.fetch(f) { progress = it }
            val bmp = withContext(Dispatchers.Default) { decodeFileSampled(local, 2048) }
            if (bmp != null) ImgLoad.Ready(bmp.asImageBitmap()) else ImgLoad.Failed("Format gambar ini tidak bisa ditampilkan.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ImgLoad.Failed(sftpMessage(e))
        } catch (e: OutOfMemoryError) {
            ImgLoad.Failed("Gambar terlalu besar untuk ditampilkan.")
        }
    }

    when (val r = result) {
        null -> DownloadProgress(progress, f.size, "Mengunduh gambar")
        is ImgLoad.Ready -> ZoomableImage(r.bmp, f.name)
        is ImgLoad.Failed -> PreviewError(r.msg) { attempt++ }
    }
}

@Composable
internal fun ZoomableImage(bmp: ImageBitmap, description: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val mx = box.width * (s - 1f) / 2f
        val my = box.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val s = (scale * zoom).coerceIn(1f, 6f)
                    scale = s
                    offset = if (s == 1f) Offset.Zero else clamp(offset + pan, s)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                })
            }
    ) {
        Image(
            bitmap = bmp,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = offset.x; translationY = offset.y
                }
        )
    }
}

@Composable
internal fun PlayerPreview(b: FileBrowserState, f: RemoteFile) {
    var started by remember { mutableStateOf(f.size <= AUTO_DOWNLOAD) }
    var attempt by remember { mutableIntStateOf(0) }
    var progress by remember { mutableLongStateOf(0L) }
    var local by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(started, attempt) {
        if (!started) return@LaunchedEffect
        local = null; error = null; progress = 0
        try {
            local = b.fetch(f) { progress = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = sftpMessage(e)
        }
    }

    val err = error
    val ready = local
    when {
        err != null -> PreviewError(err) { attempt++ }
        ready != null -> VideoPlayer(ready, audio = f.kind == FileKind.Audio) {
            error = "Format ini tidak didukung pemutar bawaan Android."
        }
        !started -> Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (f.kind == FileKind.Audio) Icons.Default.Audiotrack else Icons.Default.Movie,
                null, Modifier.size(56.dp), tint = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Berkas besar (${fmtBytes(f.size)})",
                style = MaterialTheme.typography.titleMedium, color = Color.White
            )
            Text(
                "Berkas diunduh dulu ke cache lalu diputar. Pakai Wi-Fi jika bisa.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { started = true }) { Text("Unduh dan putar") }
        }
        else -> DownloadProgress(progress, f.size, "Mengunduh untuk diputar")
    }
}

@Composable
internal fun VideoPlayer(file: File, audio: Boolean, onFail: () -> Unit) {
    val holder = remember { arrayOfNulls<VideoView>(1) }
    DisposableEffect(Unit) { onDispose { holder[0]?.stopPlayback() } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val vv = VideoView(ctx)
                val mc = MediaController(ctx)
                mc.setAnchorView(vv)
                vv.setMediaController(mc)
                vv.setOnPreparedListener { it.start() }
                vv.setOnErrorListener { _, _, _ -> onFail(); true }
                vv.setVideoPath(file.absolutePath)
                holder[0] = vv
                FrameLayout(ctx).apply {
                    addView(
                        vv,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER
                        )
                    )
                }
            }
        )
        if (audio) {
            Icon(
                Icons.Default.Audiotrack, null,
                tint = CAudio,
                modifier = Modifier.align(Alignment.Center).size(96.dp)
            )
        }
    }
}
