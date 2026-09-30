package com.rexaps.rexmanager.archive

import com.rexaps.rexmanager.CollisionPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Entry point for archive work. Picks the [ArchiveProvider] for a format. */
class ArchiveManager(
    providers: List<ArchiveProvider> = listOf(
        ZipArchiveProvider(),
        SevenZipArchiveProvider(),
        TarArchiveProvider()
    ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val byFormat: Map<ArchiveFormat, ArchiveProvider> = providers.associateBy { it.format }

    fun formatFor(fileName: String): ArchiveFormat? = ArchiveFormat.fromFileName(fileName)

    suspend fun compress(
        format: ArchiveFormat,
        sources: List<File>,
        destination: File,
        onProgress: (processed: Long, total: Long) -> Unit
    ) = withContext(ioDispatcher) {
        provider(format).compress(sources, destination, onProgress)
    }

    suspend fun extract(
        archive: File,
        destinationDir: File,
        policy: CollisionPolicy,
        onProgress: (processed: Long, total: Long) -> Unit
    ): Int = withContext(ioDispatcher) {
        val format = formatFor(archive.name)
            ?: throw RexArchiveException("Unsupported archive type.")
        provider(format).extract(archive, destinationDir, policy, onProgress)
    }

    private fun provider(format: ArchiveFormat): ArchiveProvider =
        byFormat[format] ?: throw RexArchiveException("Unsupported archive type.")
}
