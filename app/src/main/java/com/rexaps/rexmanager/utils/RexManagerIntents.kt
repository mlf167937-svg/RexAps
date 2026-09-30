package com.rexaps.rexmanager.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Share / open helpers using FileProvider. Both return an error message or null on success. */
object RexManagerIntents {

    fun authority(context: Context): String = "${context.packageName}.rexmanager.fileprovider"

    private fun uriFor(context: Context, path: String): Uri =
        FileProvider.getUriForFile(context, authority(context), File(path))

    fun share(context: Context, paths: List<String>): String? {
        return try {
            val uris = ArrayList(paths.map { uriFor(context, it) })
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = MimeTypeUtils.fromExtension(MimeTypeUtils.extensionOf(File(paths[0]).name)) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(intent, "Share")
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            null
        } catch (e: IllegalArgumentException) {
            "This file can't be shared from its current location."
        } catch (e: ActivityNotFoundException) {
            "No app available to share with."
        } catch (e: SecurityException) {
            "Sharing was blocked by the system."
        }
    }

    fun open(context: Context, path: String, mimeType: String?): String? {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriFor(context, path), mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            null
        } catch (e: ActivityNotFoundException) {
            "No app can open this file."
        } catch (e: IllegalArgumentException) {
            "This file can't be opened from its current location."
        } catch (e: SecurityException) {
            "Opening was blocked by the system."
        }
    }
}
