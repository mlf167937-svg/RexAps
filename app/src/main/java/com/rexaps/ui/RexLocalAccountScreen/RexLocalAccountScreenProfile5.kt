// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.graphics.Bitmap
import android.net.Uri
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

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

fun RexProfileController.__split_addStoryVideo_3(uri: Uri?) {
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

fun RexProfileController.__split_clearAvatar_7() {
        val a = acct.copy(picturePath = "", pictureVideo = "")
        acct = a
        rev++
        scope.launch(Dispatchers.IO) {
            File(RexPaths.account, "profile-pictures.png").delete()
            File(RexPaths.account, "profile-video.mp4").delete()
            writeAccount(a)
        }
    }

fun RexProfileController.__split_postStory_9(text: String, gradient: Int) {
        scope.launch(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            File(RexPaths.story, "${System.currentTimeMillis()}.json")
                .writeText(JSONObject().put("text", text).put("gradient", gradient).toString())
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }

fun RexProfileController.__split_deleteStory_10(s: RexStory) {
        scope.launch(Dispatchers.IO) {
            s.file.delete()
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }
