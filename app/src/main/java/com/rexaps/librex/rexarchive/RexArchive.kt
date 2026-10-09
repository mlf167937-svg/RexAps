package com.rexaps.librex.rexarchive

import android.content.Context
import android.net.Uri
import com.rexaps.librex.rexarchive.detection.ArchiveDetector
import com.rexaps.librex.rexarchive.internal.ArchiveEngine
import com.rexaps.librex.rexarchive.internal.RandomAccessArchiveEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/** Archive API for RexAps. UI, file pickers and password prompts stay in the app layer. */
object RexArchive {
    private val engine = ArchiveEngine()

    fun detectFormat(file: File): ArchiveFormat = ArchiveDetector.detect(file)
    fun getCapabilities(format: ArchiveFormat): ArchiveCapabilities = when (format) {
        ArchiveFormat.ZIP, ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = true)
        ArchiveFormat.SEVEN_ZIP -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = false)
        ArchiveFormat.RAR -> ArchiveCapabilities(true, true, true, true, false, supportsStreaming = false, supportsMultiVolume = true)
        ArchiveFormat.GZIP, ArchiveFormat.BZIP2, ArchiveFormat.XZ -> ArchiveCapabilities(true, false, false, false, true, supportsStreaming = true)
        else -> ArchiveCapabilities(canList = false, canExtract = false, canExtractSelected = false, canCreate = false)
    }

    suspend fun listContents(source: ArchiveSource, context: Context? = null, options: ArchiveOptions = ArchiveOptions(), onProgress: (ArchiveProgress) -> Unit = {}): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val format = sourceFormat(source, context)
        if (format == ArchiveFormat.SEVEN_ZIP || format == ArchiveFormat.RAR) {
            withRandomAccessFile(source, context) { file -> RandomAccessArchiveEngine.list(file, format, options, onProgress) }
        } else {
            openInput(source, context).use { engine.list(it, formatHint(format, source.name), options, onProgress) }
        }
    }

    suspend fun extract(source: ArchiveSource, destination: File, context: Context? = null, options: ArchiveOptions = ArchiveOptions(), onProgress: (ArchiveProgress) -> Unit = {}): List<File> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        if (!destination.exists() && !destination.mkdirs()) throw ArchiveError.DestinationUnavailable(destination.path)
        if (!destination.isDirectory) throw ArchiveError.DestinationUnavailable(destination.path)
        val format = sourceFormat(source, context)
        if (format == ArchiveFormat.SEVEN_ZIP || format == ArchiveFormat.RAR) {
            withRandomAccessFile(source, context) { file -> RandomAccessArchiveEngine.extract(file, format, destination, null, options, onProgress) }
        } else openInput(source, context).use { engine.extract(it, formatHint(format, source.name), destination, options, onProgress) }
    }

    suspend fun extractSelected(source: ArchiveSource, destination: File, entryIds: Set<Int>, context: Context? = null, options: ArchiveOptions = ArchiveOptions(), onProgress: (ArchiveProgress) -> Unit = {}): List<File> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        if (!destination.exists() && !destination.mkdirs()) throw ArchiveError.DestinationUnavailable(destination.path)
        if (!destination.isDirectory) throw ArchiveError.DestinationUnavailable(destination.path)
        val format = sourceFormat(source, context)
        if (format == ArchiveFormat.SEVEN_ZIP || format == ArchiveFormat.RAR) {
            withRandomAccessFile(source, context) { file -> RandomAccessArchiveEngine.extract(file, format, destination, entryIds, options, onProgress) }
        } else openInput(source, context).use { engine.extractSelected(it, formatHint(format, source.name), destination, entryIds, options, onProgress) }
    }

    suspend fun createZip(sources: List<File>, destination: File, options: ArchiveOptions = ArchiveOptions(), onProgress: (ArchiveProgress) -> Unit = {}) = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive(); engine.createZip(sources, destination, options, onProgress)
    }

    suspend fun createTar(sources: List<File>, destination: File, format: ArchiveFormat = ArchiveFormat.TAR, options: ArchiveOptions = ArchiveOptions(), onProgress: (ArchiveProgress) -> Unit = {}) = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive(); engine.createTar(sources, destination, format, options, onProgress)
    }

    suspend fun create7z(sources: List<File>, destination: File, options: ArchiveOptions = ArchiveOptions(), onProgress: (ArchiveProgress) -> Unit = {}) = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive(); RandomAccessArchiveEngine.create7z(sources, destination, options, onProgress)
    }

    private fun sourceFormat(source: ArchiveSource, context: Context?): ArchiveFormat {
        source.file?.let { return ArchiveDetector.detect(it) }
        val uri = source.uri ?: throw IllegalArgumentException("Missing source")
        if (context == null) return ArchiveFormat.fromFileName(source.name)
        val input = context.contentResolver.openInputStream(uri) ?: return ArchiveFormat.fromFileName(source.name)
        return input.buffered().use { ArchiveDetector.detect(it, source.name) }
    }


    private fun formatHint(format: ArchiveFormat, originalName: String): String {
        val fromName = ArchiveFormat.fromFileName(originalName)
        if (fromName == format) return originalName
        val extension = when (format) {
            ArchiveFormat.TAR_GZIP -> "tar.gz"
            ArchiveFormat.TAR_XZ -> "tar.xz"
            ArchiveFormat.TAR_BZIP2 -> "tar.bz2"
            else -> format.extensions.firstOrNull() ?: "bin"
        }
        return "archive.$extension"
    }

    private fun openInput(source: ArchiveSource, context: Context?): InputStream = when {
        source.file != null -> source.file.inputStream().buffered()
        source.uri != null && context != null -> context.contentResolver.openInputStream(source.uri) ?: throw ArchiveError.Io("Cannot open URI: ${source.uri}")
        else -> throw IllegalArgumentException("A Context is required for Uri sources")
    }

    /** Supplies a seekable temporary copy only for formats whose backends require random access. */
    private suspend fun <T> withRandomAccessFile(source: ArchiveSource, context: Context?, block: suspend (File) -> T): T {
        source.file?.let { return block(it) }
        val ctx = context ?: throw IllegalArgumentException("A Context is required for Uri sources")
        val uri = source.uri ?: throw IllegalArgumentException("Missing source")
        val temp = File.createTempFile("rexarchive-", source.name.substringAfterLast('.', "tmp").let { ".$it" }, ctx.cacheDir)
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: throw ArchiveError.Io("Cannot open URI: $uri")
            input.use { src -> temp.outputStream().buffered().use { dst ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = src.read(buffer); if (n < 0) break
                    dst.write(buffer, 0, n)
                }
            } }
            return block(temp)
        } finally { temp.delete() }
    }
}
