package com.rexaps.rexmanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.io.File

// ───────────────────────── Media (jpg/png/jpeg/mp3/ogg/mp4) ─────────────────────────
@Composable
internal fun MediaScreen(file: File, onBack: () -> Unit) {
    val ext = file.extension.lowercase()
    Column(Modifier.fillMaxSize()) {
        RexTopBar(file.name, formatSize(file.length()), onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (ext) {
                in IMAGE_EXT -> ImagePreview(file)
                in AUDIO_EXT -> AudioPreview(file)
                in VIDEO_EXT -> VideoPreview(file)
            }
        }
    }
}
