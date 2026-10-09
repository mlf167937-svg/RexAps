package com.rexaps.rexmanager.bigupgrade.cloud

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** OAuth helper. The host Activity must forward onActivityResult to handleAuthorizationResult. */
class GoogleDriveAuth(private val context: Context) {
    private var pendingCallback: ((Result<String>) -> Unit)? = null

    fun authorize(activity: Activity, callback: (Result<String>) -> Unit) {
        pendingCallback = callback
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.file"), Scope("https://www.googleapis.com/auth/drive.readonly")))
            .build()
        Identity.getAuthorizationClient(activity).authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    result.pendingIntent?.let { activity.startIntentSenderForResult(it.intentSender, REQUEST_CODE, null, 0, 0, 0) }
                        ?: finish(Result.failure(IllegalStateException("Otorisasi Google Drive tidak tersedia")))
                } else {
                    val token = result.accessToken
                    if (token.isNullOrBlank()) finish(Result.failure(IllegalStateException("Token akses kosong")))
                    else finish(Result.success(token))
                }
            }
            .addOnFailureListener { finish(Result.failure(it)) }
    }

    fun handleAuthorizationResult(data: Intent?, callback: (Result<String>) -> Unit) {
        runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data) }
            .onSuccess { result -> result.accessToken?.takeIf(String::isNotBlank)?.let { callback(Result.success(it)) } ?: callback(Result.failure(IllegalStateException("Token Drive tidak diterima"))) }
            .onFailure { callback(Result.failure(it)) }
    }

    private fun finish(result: Result<String>) { pendingCallback?.invoke(result); pendingCallback = null }
    companion object { const val REQUEST_CODE = 8042 }
}

data class DriveFileItem(val id: String, val name: String, val mimeType: String, val size: Long?, val modifiedTime: String?)

/** Minimal real Google Drive REST client. Token must come from GoogleDriveAuth; never hardcode it. */
class GoogleDriveClient(private val accessToken: String) {
    suspend fun listFiles(query: String = "trashed = false", pageSize: Int = 100): List<DriveFileItem> = withContext(Dispatchers.IO) {
        val q = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
        val fields = java.net.URLEncoder.encode("files(id,name,mimeType,size,modifiedTime),nextPageToken", Charsets.UTF_8.name())
        val json = request("GET", "https://www.googleapis.com/drive/v3/files?q=$q&pageSize=${pageSize.coerceIn(1,1000)}&orderBy=name&fields=$fields")
        val arr = json.optJSONArray("files") ?: JSONArray()
        buildList { for (i in 0 until arr.length()) { val f = arr.getJSONObject(i); add(DriveFileItem(f.optString("id"), f.optString("name"), f.optString("mimeType"), f.optString("size").toLongOrNull(), f.optString("modifiedTime").takeIf(String::isNotBlank))) } }
    }

    suspend fun upload(file: File, parentId: String? = null): DriveFileItem = withContext(Dispatchers.IO) {
        require(file.isFile) { "File tidak ditemukan" }
        val boundary = "RexDrive-${UUID.randomUUID()}"
        val metadata = JSONObject().put("name", file.name).apply { if (!parentId.isNullOrBlank()) put("parents", JSONArray().put(parentId)) }
        val conn = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,size,modifiedTime").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("Authorization", "Bearer $accessToken"); conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.outputStream.use { out ->
            out.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${metadata}\r\n".toByteArray())
            out.write("--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
            file.inputStream().use { it.copyTo(out) }
            out.write("\r\n--$boundary--".toByteArray())
        }
        readResponse(conn).let { parseFile(it) }
    }

    suspend fun download(fileId: String, destination: File): File = withContext(Dispatchers.IO) {
        val conn = URL("https://www.googleapis.com/drive/v3/files/${urlPath(fileId)}?alt=media").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        if (conn.responseCode !in 200..299) throw IllegalStateException("Download Drive gagal (${conn.responseCode}): ${conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()}")
        destination.parentFile?.mkdirs(); conn.inputStream.use { input -> destination.outputStream().use(input::copyTo) }; destination
    }

    suspend fun createFolder(name: String, parentId: String? = null): DriveFileItem = withContext(Dispatchers.IO) {
        val body = JSONObject().put("name", name).put("mimeType", "application/vnd.google-apps.folder").apply { if (!parentId.isNullOrBlank()) put("parents", JSONArray().put(parentId)) }
        parseFile(request("POST", "https://www.googleapis.com/drive/v3/files?fields=id,name,mimeType,size,modifiedTime", body))
    }

    private fun request(method: String, url: String, body: JSONObject? = null): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method; c.setRequestProperty("Authorization", "Bearer $accessToken"); c.setRequestProperty("Accept", "application/json")
        if (body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); c.outputStream.use { it.write(body.toString().toByteArray()) } }
        return readResponse(c)
    }
    private fun readResponse(c: HttpURLConnection): JSONObject {
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IllegalStateException("Google Drive API ($code): $text")
        return JSONObject(text)
    }
    private fun parseFile(f: JSONObject) = DriveFileItem(f.optString("id"), f.optString("name"), f.optString("mimeType"), f.optString("size").toLongOrNull(), f.optString("modifiedTime").takeIf(String::isNotBlank))
    private fun urlPath(s: String) = java.net.URLEncoder.encode(s, Charsets.UTF_8.name())
}
