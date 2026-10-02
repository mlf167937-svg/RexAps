package com.rexaps.rextools.downloader

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object MediaDownloader {

    private val client = OkHttpClient()

    /** Download gambar (jpg/png/webp) ke Pictures/RexTools */
    suspend fun downloadImage(context: Context, url: String, fileName: String? = null) =
        download(context, url, fileName, "image/jpeg", MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_PICTURES, "RexTools", ".jpg")

    /** Download video (mp4) ke Movies/RexTools */
    suspend fun downloadVideo(context: Context, url: String, fileName: String? = null) =
        download(context, url, fileName, "video/mp4", MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MOVIES, "RexTools", ".mp4")

    private suspend fun download(
        context: Context,
        url: String,
        fileName: String?,
        mimeType: String,
        collection: android.net.Uri,
        directory: String,
        subFolder: String,
        defaultExt: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("body kosong"))
            val bytes = body.bytes()

            val name = fileName ?: "rex_${System.currentTimeMillis()}$defaultExt"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/$subFolder")
                }
                val uri = context.contentResolver.insert(collection, values)
                    ?: return@withContext Result.failure(Exception("gagal insert media"))

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                Result.success(File(uri.toString()))
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(directory),
                    subFolder
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, name)
                FileOutputStream(file).use { it.write(bytes) }
                Result.success(file)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
