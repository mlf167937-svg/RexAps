package com.rexaps.rexmusic

fun OfflineState.isDownloaded(track: RexTrack, key: String = offlineKey(track)): Boolean =
    keyFor(track, key) != null
