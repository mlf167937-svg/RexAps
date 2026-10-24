package com.rexaps.librex.rexsmartmanager

import android.content.Context
import android.os.Environment
import android.webkit.URLUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Lightweight HTTP downloader. Partial files are kept in the app's Downloads directory.
 * This first version keeps task metadata in memory; persist it (Room) before production use.
 */
class OkHttpRexSmartManager(context: Context) : RexSmartManager {
    private val appContext = context.applicationContext
    private val outputDir: File = (appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: File(appContext.filesDir, "downloads")).apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val records = ConcurrentHashMap<DownloadId, Record>()
    private val pauseRequested = ConcurrentHashMap.newKeySet<DownloadId>()
    private val _downloads = MutableStateFlow<List<DownloadProgress>>(emptyList())
    override val downloads: StateFlow<List<DownloadProgress>> = _downloads.asStateFlow()

    override fun enqueue(request: DownloadRequest): DownloadId {
        val parsed = runCatching { java.net.URI(request.url) }.getOrNull()
        require(parsed?.scheme.equals("https", true) || parsed?.scheme.equals("http", true)) {
            "URL harus menggunakan HTTP atau HTTPS."
        }
        val name = safeFileName(request.fileName ?: URLUtil.guessFileName(request.url, null, null))
        val id = newDownloadId()
        val record = Record(id, request, name)
        records[id] = record
        publish(record.progress.copy(status = DownloadStatus.QUEUED))
        start(record)
        return id
    }

    override fun pause(id: DownloadId) {
        val record = records[id] ?: return
        pauseRequested.add(id)
        record.job?.cancel()
    }

    override fun resume(id: DownloadId) {
        val record = records[id] ?: return
        if (record.progress.status !in setOf(DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.ACTION_REQUIRED)) return
        pauseRequested.remove(id)
        start(record)
    }

    override fun cancel(id: DownloadId) {
        val record = records[id] ?: return
        pauseRequested.remove(id)
        record.cancelRequested = true
        record.job?.cancel()
        File(outputDir, "${record.id}.part").delete()
        update(record, record.progress.copy(status = DownloadStatus.CANCELLED, message = "Dibatalkan"))
    }

    override fun openActionRequired(id: DownloadId, message: String) {
        val record = records[id] ?: return
        record.job?.cancel()
        update(record, record.progress.copy(status = DownloadStatus.ACTION_REQUIRED, message = message))
    }

    private fun start(record: Record) {
        if (record.job?.isActive == true) return
        record.cancelRequested = false
        record.job = scope.launch {
            try {
                download(record)
            } catch (cancelled: CancellationException) {
                if (record.cancelRequested) {
                    update(record, record.progress.copy(status = DownloadStatus.CANCELLED, message = "Dibatalkan"))
                } else if (pauseRequested.remove(record.id)) {
                    update(record, record.progress.copy(status = DownloadStatus.PAUSED, message = "Dijeda"))
                } else {
                    update(record, record.progress.copy(status = DownloadStatus.PAUSED, message = "Dijeda"))
                }
                throw cancelled
            } catch (error: Exception) {
                update(record, record.progress.copy(status = DownloadStatus.FAILED, message = error.message ?: "Unduhan gagal"))
            }
        }
    }

    private suspend fun download(record: Record) = withContext(Dispatchers.IO) {
        val part = File(outputDir, "${record.id}.part")
        val final = uniqueDestination(File(outputDir, record.fileName))
        var offset = if (record.request.allowResume) part.takeIf { it.exists() }?.length() ?: 0L else 0L
        if (!record.request.allowResume) part.delete()
        val headers = record.request.headers.toMutableMap()
        if (offset > 0L) headers["Range"] = "bytes=$offset-"
        val builder = Request.Builder().url(record.request.url).get()
        headers.forEach { (key, value) -> if (!key.equals("Range", true) || offset > 0L) builder.header(key, value) }
        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 401 || response.code == 403 || response.code == 429) {
                update(record, record.progress.copy(status = DownloadStatus.ACTION_REQUIRED,
                    message = "Server meminta autentikasi atau menolak permintaan (HTTP ${response.code}). Buka halaman di RexFox, selesaikan langkah yang diminta, lalu coba lagi."))
                return@withContext
            }
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} ${response.message}")
            val body = response.body ?: throw IOException("Server tidak mengirim isi file")
            val append = offset > 0L && response.code == 206 &&
                response.header("Content-Range")?.startsWith("bytes $offset-") == true
            if (offset > 0L && response.code == 206 && !append) {
                throw IOException("Range response tidak cocok; coba ulangi unduhan dari awal")
            }
            if (offset > 0L && response.code != 206) {
                // A full 200 response starts at byte zero; never append it to a partial file.
                part.delete()
                offset = 0L
            }
            val total = response.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                ?: body.contentLength().takeIf { it >= 0L }?.let { it + offset }
            update(record, record.progress.copy(status = DownloadStatus.DOWNLOADING,
                downloadedBytes = offset, totalBytes = total, message = null))
            body.byteStream().use { input ->
                java.io.FileOutputStream(part, append).buffered().use { sink ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                    var downloaded = offset
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        sink.write(buffer, 0, count)
                        downloaded += count
                        update(record, record.progress.copy(status = DownloadStatus.DOWNLOADING,
                            downloadedBytes = downloaded, totalBytes = total))
                    }
                    sink.flush()
                }
            }
        }
        if (!part.exists()) throw IOException("File sementara tidak ditemukan")
        if (final.exists()) final.delete()
        if (!part.renameTo(final)) throw IOException("Tidak dapat menyelesaikan file: ${final.absolutePath}")
        update(record, record.progress.copy(status = DownloadStatus.COMPLETED,
            downloadedBytes = final.length(), totalBytes = final.length(), filePath = final.absolutePath, message = "Selesai"))
    }

    private fun publish(progress: DownloadProgress) {
        val record = records[progress.id]
        if (record != null) record.progress = progress
        _downloads.value = records.values.map { it.progress }.sortedByDescending { it.id }
    }

    private fun update(record: Record, progress: DownloadProgress) {
        record.progress = progress
        _downloads.value = records.values.map { it.progress }.sortedByDescending { it.id }
    }

    private fun safeFileName(input: String): String {
        val cleaned = input.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(180)
        return cleaned.ifBlank { "download.bin" }.let { if (it == "." || it == "..") "download.bin" else it }
    }

    private fun uniqueDestination(file: File): File {
        if (!file.exists()) return file
        val base = file.nameWithoutExtension
        val ext = file.extension.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        var index = 1
        while (true) {
            val candidate = File(file.parentFile, "$base ($index)$ext")
            if (!candidate.exists()) return candidate
            index++
        }
    }

    fun close() { scope.cancel() }

    private class Record(val id: DownloadId, val request: DownloadRequest, val fileName: String) {
        @Volatile var job: Job? = null
        @Volatile var cancelRequested: Boolean = false
        @Volatile var progress = DownloadProgress(id, request.url, fileName, DownloadStatus.QUEUED)
    }
}

