package com.rexaps.librex.rexarchive.internal

import com.rexaps.librex.rexarchive.*
import java.io.*
import java.util.zip.*

internal class ArchiveEngine {
    fun capabilities(format: ArchiveFormat) = capabilitiesImpl(format)
    suspend fun list(input: InputStream, name: String, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> = listImpl(input, name, options, progress)
    suspend fun extract(input: InputStream, name: String, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> = extractImpl(input, name, destination, options, progress)
    suspend fun extractSelected(input: InputStream, name: String, destination: File, ids: Set<Int>, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> = extractSelectedImpl(input, name, destination, ids, options, progress)
    suspend fun createZip(sources: List<File>, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) = createZipImpl(sources, destination, options, progress)
    suspend fun createTar(sources: List<File>, destination: File, format: ArchiveFormat, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) = createTarImpl(sources, destination, format, options, progress)
}
