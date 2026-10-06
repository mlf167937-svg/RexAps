package com.rexaps.rexmonitor.device

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock

data class RexSystemInfo(
    val uptimeMs: Long? = null,
    val installedAppCount: Int? = null,
    /** true = jumlah mungkin tidak lengkap karena package visibility Android 11+. */
    val appCountLimited: Boolean = false
)

class RexSystemReader(context: Context) {
    private val app = context.applicationContext
    private var cachedCount: Int? = null
    private var cachedAt = 0L

    fun read(): RexSystemInfo {
        val now = SystemClock.elapsedRealtime()
        if (cachedCount == null || now - cachedAt > APP_COUNT_TTL_MS) {
            cachedCount = runCatching {
                @Suppress("DEPRECATION")
                app.packageManager.getInstalledApplications(0)
                    .count { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            }.getOrNull()
            cachedAt = now
        }
        val limited = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            app.packageManager.checkPermission(
                "android.permission.QUERY_ALL_PACKAGES", app.packageName
            ) != PackageManager.PERMISSION_GRANTED
        return RexSystemInfo(uptimeMs = now, installedAppCount = cachedCount, appCountLimited = limited)
    }

    private companion object {
        const val APP_COUNT_TTL_MS = 60_000L
    }
}
