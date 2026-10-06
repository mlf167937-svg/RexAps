package com.rexaps.rexmonitor.device

import android.os.Environment
import android.os.StatFs

data class RexStorageInfo(
    val totalBytes: Long? = null,
    val freeBytes: Long? = null
) {
    val usedBytes: Long? get() = if (totalBytes != null && freeBytes != null) totalBytes - freeBytes else null
    val usedPercent: Float?
        get() = usedBytes?.let { if (totalBytes!! > 0) it * 100f / totalBytes else null }
}

/** Membaca partisi data internal (/data), yang dilihat pengguna sebagai penyimpanan internal. */
class RexStorageReader {
    fun read(): RexStorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        return RexStorageInfo(total, free)
    }
}
