package com.rexaps.ui

import android.graphics.Bitmap
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
import kotlinx.coroutines.withContext
import java.io.File

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
