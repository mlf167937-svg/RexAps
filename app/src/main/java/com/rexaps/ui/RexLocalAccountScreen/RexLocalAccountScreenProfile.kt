// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

internal data class RexLocalAccount(
    val name: String = "Rex User",
    val username: String = "@rexuser",
    val bio: String = "Halo! Aku menggunakan RexAps.",
    val bannerPath: String = "",
    val bannerVideo: String = "",
    val bannerScale: Float = 1f,
    val bannerX: Float = 0f,
    val bannerY: Float = 0f,
    val picturePath: String = "",
    val pictureVideo: String = "",
    val pictureScale: Float = 1f,
    val pictureX: Float = 0f,
    val pictureY: Float = 0f
)

internal data class RexStory(
    val file: File,
    val time: Long,
    val isVideo: Boolean,
    val text: String,
    val gradient: Int
)

internal val storyGradients = listOf(
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF232526), Color(0xFF414345)),
    listOf(Color(0xFFEE0979), Color(0xFFFF6A00))
)

/* ═════════════════════ File & Media Helpers ═════════════════════ */

internal fun readAccount(): RexLocalAccount? = try {
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

internal fun writeAccount(a: RexLocalAccount) {
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

internal class RexProfileController(
    internal val context: Context,
    internal val scope: CoroutineScope
) {
    var acct by mutableStateOf(RexLocalAccount())
    var rev by mutableIntStateOf(0)
    var stories by mutableStateOf<List<RexStory>>(emptyList())
    var videos by mutableStateOf<List<File>>(emptyList())
    var tab by mutableIntStateOf(0)
    var visible by mutableStateOf(false)
    var showEdit by mutableStateOf(false)
    var showStoryCompose by mutableStateOf(false)
    var storyIndex by mutableStateOf<Int?>(null)
    var feedIndex by mutableStateOf<Int?>(null)
    var crop by mutableStateOf<CropReq?>(null)
    var busy by mutableStateOf<String?>(null)

    var onPickBanner: () -> Unit = {}
    var onPickAvatar: () -> Unit = {}
    var onPickStoryVideo: () -> Unit = {}
    var onPickVideo: () -> Unit = {}

    internal fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    suspend fun load() = this.__split_load_1()

    fun handlePick(uri: Uri?, target: CropTarget) = this.__split_handlePick_2(uri, target)

    fun addStoryVideo(uri: Uri?) = this.__split_addStoryVideo_3(uri)

    fun addVideo(uri: Uri?) = this.__split_addVideo_4(uri)

    fun applyCrop(req: CropReq, bmp: Bitmap, s: Float, ox: Float, oy: Float, startMs: Long) = this.__split_applyCrop_5(req, bmp, s, ox, oy, startMs)

    fun clearBanner() = this.__split_clearBanner_6()

    fun clearAvatar() = this.__split_clearAvatar_7()

    fun saveProfile(n: String, u: String, b: String) = this.__split_saveProfile_8(n, u, b)

    fun postStory(text: String, gradient: Int) = this.__split_postStory_9(text, gradient)

    fun deleteStory(s: RexStory) = this.__split_deleteStory_10(s)

    fun deleteVideo(f: File) {
        scope.launch(Dispatchers.IO) {
            f.delete()
            val list = loadVideos()
            withContext(Dispatchers.Main) { videos = list }
        }
    }
}

@Composable
internal fun rememberRexProfileController(): RexProfileController {
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

/* ═════════════════════ Entry Point & Permission ═════════════════════ */

@Composable
internal fun RexProfileRoot(padding: PaddingValues) {
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
