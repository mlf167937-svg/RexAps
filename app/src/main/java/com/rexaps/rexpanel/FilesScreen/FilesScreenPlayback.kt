package com.rexaps.rexpanel

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

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
