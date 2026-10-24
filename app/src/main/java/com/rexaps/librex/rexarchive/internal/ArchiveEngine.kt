package com.rexaps.librex.rexarchive.internal

import com.rexaps.librex.rexarchive.*
import java.io.*
import java.util.zip.*

internal class ArchiveEngine {
    fun capabilities(format: ArchiveFormat) = this.__split_capabilities_1(format)

    suspend fun list(input: InputStream, name: String, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> = this.__split_list_2(input, name, options, progress)

    internal suspend fun listZip(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> = this.__split_listZip_3(input, options, progress)

    internal suspend fun listTar(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> = this.__split_listTar_4(input, options, progress)

    suspend fun extract(input: InputStream, name: String, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> =
        extractInternal(input, name, destination, null, options, progress)

    suspend fun extractSelected(input: InputStream, name: String, destination: File, ids: Set<Int>, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> =
        extractInternal(input, name, destination, ids, options, progress)

    private suspend fun extractInternal(input: InputStream, name: String, destination: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> = this.__split_extractInternal_5(input, name, destination, selected, options, progress)

    internal suspend fun extractZip(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> = this.__split_extractZip_6(input, root, selected, options, progress)

    internal suspend fun extractTar(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> = this.__split_extractTar_7(input, root, selected, options, progress)

    suspend fun createZip(sources: List<File>, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) = this.__split_createZip_8(sources, destination, options, progress)

    suspend fun createTar(sources: List<File>, destination: File, format: ArchiveFormat, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) = this.__split_createTar_9(sources, destination, format, options, progress)

    internal fun wrapTarInput(input: InputStream, format: ArchiveFormat): InputStream = this.__split_wrapTarInput_10(input, format)

    internal fun collectFiles(sources: List<File>, options: ArchiveOptions): List<Pair<File, String>> = this.__split_collectFiles_11(sources, options)

    internal fun prepareDestination(destination: File, options: ArchiveOptions) = this.__split_prepareDestination_12(destination, options)

    internal fun ensureParent(file: File) { val p = file.parentFile ?: throw ArchiveError.DestinationUnavailable(file.path); if (!p.exists() && !p.mkdirs()) throw ArchiveError.DestinationUnavailable(p.path) }
    internal fun chooseTarget(target: File, policy: OverwritePolicy): File? = this.__split_chooseTarget_13(target, policy)
    internal fun drain(input: InputStream, bufferSize: Int) { val b = ByteArray(bufferSize); while (input.read(b) >= 0) { /* consume */ } }
}
