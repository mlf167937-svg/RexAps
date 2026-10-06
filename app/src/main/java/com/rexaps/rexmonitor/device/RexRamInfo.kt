package com.rexaps.rexmonitor.device

import android.app.ActivityManager
import android.content.Context

data class RexRamInfo(
    val totalBytes: Long? = null,
    val availableBytes: Long? = null,
    val lowMemory: Boolean = false
) {
    val usedBytes: Long? get() = if (totalBytes != null && availableBytes != null) totalBytes - availableBytes else null
    val usedPercent: Float?
        get() = usedBytes?.let { if (totalBytes!! > 0) it * 100f / totalBytes else null }
}

class RexRamReader(context: Context) {
    private val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    fun read(): RexRamInfo {
        val manager = am ?: return RexRamInfo()
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return RexRamInfo(info.totalMem, info.availMem, info.lowMemory)
    }
}
