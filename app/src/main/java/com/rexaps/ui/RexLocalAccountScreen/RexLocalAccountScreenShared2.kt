package com.rexaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/* ═════════════════════ Profile Controller ═════════════════════ */

internal class RexProfileController(
    private val context: Context,
    private val scope: CoroutineScope
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

    private fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    suspend fun load() {
        withContext(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            RexPaths.video.mkdirs()
        }
        withContext(Dispatchers.IO) { readAccount() }?.let { acct = it; rev++ }
        videos = withContext(Dispatchers.IO) { loadVideos() }
        visible = true
        while (true) {
            stories = withContext(Dispatchers.IO) { loadStories() }
            delay(60_000)
        }
    }

    fun handlePick(uri: Uri?, target: CropTarget) {
        if (uri == null) return
        scope.launch {
            busy = "Membaca media…"
            val req = withContext(Dispatchers.IO) { buildCropReq(context, uri, target) }
            busy = null
            if (req == null) toast("Media tidak bisa dibaca") else crop = req
        }
    }

    fun addStoryVideo(uri: Uri?) {
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

    fun addVideo(uri: Uri?) {
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

    fun applyCrop(req: CropReq, bmp: Bitmap, s: Float, ox: Float, oy: Float, startMs: Long) {
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

    fun clearBanner() {
        val a = acct.copy(bannerPath = "", bannerVideo = "")
        acct = a
        rev++
        scope.launch(Dispatchers.IO) {
            File(RexPaths.account, "banner.png").delete()
            File(RexPaths.account, "banner.mp4").delete()
            writeAccount(a)
        }
    }

    fun clearAvatar() {
        val a = acct.copy(picturePath = "", pictureVideo = "")
        acct = a
        rev++
        scope.launch(Dispatchers.IO) {
            File(RexPaths.account, "profile-pictures.png").delete()
            File(RexPaths.account, "profile-video.mp4").delete()
            writeAccount(a)
        }
    }

    fun saveProfile(n: String, u: String, b: String) {
        val a = acct.copy(
            name = n.trim().ifBlank { "Rex User" },
            username = u.trim().ifBlank { "@rexuser" },
            bio = b.trim()
        )
        acct = a
        scope.launch(Dispatchers.IO) {
            try {
                writeAccount(a)
                withContext(Dispatchers.Main) { toast("Profil tersimpan") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }

    fun postStory(text: String, gradient: Int) {
        scope.launch(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            File(RexPaths.story, "${System.currentTimeMillis()}.json")
                .writeText(JSONObject().put("text", text).put("gradient", gradient).toString())
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }

    fun deleteStory(s: RexStory) {
        scope.launch(Dispatchers.IO) {
            s.file.delete()
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }

    fun deleteVideo(f: File) {
        scope.launch(Dispatchers.IO) {
            f.delete()
            val list = loadVideos()
            withContext(Dispatchers.Main) { videos = list }
        }
    }
}
