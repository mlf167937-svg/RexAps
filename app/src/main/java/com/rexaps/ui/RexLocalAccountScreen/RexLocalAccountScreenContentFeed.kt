package com.rexaps.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun FeedItem(file: File, acct: RexLocalAccount, rev: Int, active: Boolean) {
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
