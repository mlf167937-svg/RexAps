package com.rexaps.librex.rexarchive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Single-stream compression/decompression for .gz, .bz2 and .xz files (not archive containers). */
object StreamCompression {
    suspend fun decompress(source: File, destination: File, format: ArchiveFormat, bufferSize: Int = 64 * 1024): File = withContext(Dispatchers.IO) {
        require(format in setOf(ArchiveFormat.GZIP, ArchiveFormat.BZIP2, ArchiveFormat.XZ)) { "Only GZIP, BZIP2 and XZ streams are supported" }
        require(bufferSize in 4096..1_048_576)
        destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
        if (destination.exists()) throw ArchiveError.DestinationExists(destination.path)
        val input: InputStream = FileInputStream(source).buffered(bufferSize).let {
            when (format) {
                ArchiveFormat.GZIP -> GzipCompressorInputStream(it)
                ArchiveFormat.BZIP2 -> BZip2CompressorInputStream(it)
                ArchiveFormat.XZ -> XZCompressorInputStream(it)
                else -> it
            }
        }
        try {
            input.use { zin -> FileOutputStream(destination).buffered(bufferSize).use { out -> copyChecked(zin, out, bufferSize) } }
        } catch (t: Throwable) { destination.delete(); throw t }
        destination
    }

    suspend fun compress(source: File, destination: File, format: ArchiveFormat, bufferSize: Int = 64 * 1024): File = withContext(Dispatchers.IO) {
        require(format in setOf(ArchiveFormat.GZIP, ArchiveFormat.BZIP2, ArchiveFormat.XZ)) { "Only GZIP, BZIP2 and XZ streams are supported" }
        require(bufferSize in 4096..1_048_576)
        destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
        if (destination.exists()) throw ArchiveError.DestinationExists(destination.path)
        val raw = FileOutputStream(destination).buffered(bufferSize)
        val output: OutputStream = when (format) {
            ArchiveFormat.GZIP -> GzipCompressorOutputStream(raw)
            ArchiveFormat.BZIP2 -> BZip2CompressorOutputStream(raw)
            ArchiveFormat.XZ -> XZCompressorOutputStream(raw)
            else -> raw
        }
        try { output.use { zout -> FileInputStream(source).buffered(bufferSize).use { zin -> copyChecked(zin, zout, bufferSize) } } }
        catch (t: Throwable) { destination.delete(); throw t }
        destination
    }

    private suspend fun copyChecked(input: InputStream, output: OutputStream, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        while (true) {
            currentCoroutineContext().ensureActive()
            val n = input.read(buffer); if (n < 0) break
            output.write(buffer, 0, n)
        }
    }
}
