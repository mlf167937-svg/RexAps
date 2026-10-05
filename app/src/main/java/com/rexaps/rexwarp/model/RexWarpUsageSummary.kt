package com.rexaps.rexwarp.model
data class RexWarpUsageSummary(val downloadBytes: Long = 0, val uploadBytes: Long = 0) {
    val totalBytes: Long get() = downloadBytes + uploadBytes
    operator fun plus(o: RexWarpUsageSummary) =
        RexWarpUsageSummary(downloadBytes + o.downloadBytes, uploadBytes + o.uploadBytes)
}
