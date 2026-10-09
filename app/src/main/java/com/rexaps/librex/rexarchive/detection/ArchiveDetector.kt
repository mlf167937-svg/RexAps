package com.rexaps.librex.rexarchive.detection

import com.rexaps.librex.rexarchive.ArchiveFormat
import java.io.File
import java.io.InputStream

object ArchiveDetector {
    fun detect(file: File): ArchiveFormat {
        if (file.isFile) {
            runCatching { file.inputStream().buffered().use { detect(it, file.name) } }.getOrNull()?.let { if (it != ArchiveFormat.UNKNOWN) return it }
        }
        return ArchiveFormat.fromFileName(file.name)
    }

    /** Reads a small prefix and does not close the supplied stream. */
    fun detect(input: InputStream, nameHint: String? = null): ArchiveFormat {
        require(input.markSupported()) { "A mark-supported stream is required for non-consuming signature detection" }
        val buffered = input
        buffered.mark(16)
        val h = ByteArray(8)
        val n = buffered.read(h)
        buffered.reset()
        if (n >= 4 && h[0] == 'P'.code.toByte() && h[1] == 'K'.code.toByte() && h[2] in byteArrayOf(3,5,7) && h[3] in byteArrayOf(4,6,8)) return ArchiveFormat.ZIP
        if (n >= 6 && h.copyOfRange(0,6).contentEquals(byteArrayOf(0x37,0x7A,0xBC.toByte(),0xAF.toByte(),0x27,0x1C))) return ArchiveFormat.SEVEN_ZIP
        if (n >= 4 && h.copyOfRange(0,4).contentEquals(byteArrayOf(0x52,0x61,0x72,0x21))) return ArchiveFormat.RAR
        if (n >= 4 && h.copyOfRange(0,4).contentEquals(byteArrayOf(0x25,0x50,0x44,0x46))) return ArchiveFormat.PDF
        if (n >= 2 && h[0] == 0x1F.toByte() && h[1] == 0x8B.toByte()) return if (nameHint?.lowercase()?.let { it.endsWith(".tar.gz") || it.endsWith(".tgz") } == true) ArchiveFormat.TAR_GZIP else ArchiveFormat.GZIP
        if (n >= 6 && h.copyOfRange(0,6).contentEquals(byteArrayOf(0xFD.toByte(),0x37,0x7A,0x58,0x5A,0x00))) return if (nameHint?.lowercase()?.let { it.endsWith(".tar.xz") || it.endsWith(".txz") } == true) ArchiveFormat.TAR_XZ else ArchiveFormat.XZ
        if (n >= 3 && h.copyOfRange(0,3).contentEquals(byteArrayOf('B'.code.toByte(),'Z'.code.toByte(),'h'.code.toByte()))) return if (nameHint?.lowercase()?.let { it.endsWith(".tar.bz2") || it.endsWith(".tbz2") || it.endsWith(".tbz") } == true) ArchiveFormat.TAR_BZIP2 else ArchiveFormat.BZIP2
        return ArchiveFormat.fromFileName(nameHint.orEmpty())
    }
}
