@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rexaps.rexgit

internal fun runLogsApiUrl(repo: RexGitRepository?, runId: Long): String? {
    if (repo == null) return null
    val remote = repo.remoteUrl.orEmpty()
    val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
    val owner = match?.groupValues?.getOrNull(1) ?: repo.globalOwner ?: repo.accountNickname ?: return null
    val name = match?.groupValues?.getOrNull(2) ?: repo.name
    return "https://api.github.com/repos/$owner/$name/actions/runs/$runId/logs"
}

internal fun formatRunDuration(start: String, end: String): String {
    return try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = true
        }
        val startDate = formatter.parse(start.take(20)) ?: return "duration unavailable"
        val endDate = formatter.parse(end.take(20)) ?: return "duration unavailable"
        val seconds = ((endDate.time - startDate.time) / 1000L).coerceAtLeast(0L)
        when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    } catch (_: Throwable) {
        "duration unavailable"
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
