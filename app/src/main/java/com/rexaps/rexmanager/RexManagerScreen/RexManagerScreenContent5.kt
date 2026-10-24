package com.rexaps.rexmanager

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.io.File
import java.util.zip.ZipFile
import org.apache.commons.compress.archivers.sevenz.SevenZFile

// ───────────────────────── Media (jpg/png/jpeg/mp3/ogg/mp4) ─────────────────────────
@Composable
internal fun __splitImpl_MediaScreen(file: File, onBack: () -> Unit) {
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

internal fun __splitImpl_listArchive(f: File): List<ArcItem> = when (f.extension.lowercase()) {
    "zip" -> ZipFile(f).use { z ->
        z.entries().asSequence().map { ArcItem(it.name, it.size, it.isDirectory) }.toList()
    }
    else -> SevenZFile(f).use { sz ->
        generateSequence { sz.nextEntry }.map { ArcItem(it.name, it.size, it.isDirectory) }.toList()
    }
}

// ───────────────────────── Entry ─────────────────────────
@Composable
internal fun __splitImpl_RexManagerApp(onExit: () -> Unit = {}) {
    var splash by rememberSaveable { mutableStateOf(true) }
    if (splash) {
        RexSplashManager(onStart = { splash = false }, onExit = onExit)
    } else {
        RexManagerScreen(onExit = onExit)
    }
}
