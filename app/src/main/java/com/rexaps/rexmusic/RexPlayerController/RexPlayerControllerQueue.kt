package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

//  Play from list 

internal fun RexPlayerController.playFromMainImpl(index: Int) {
    val list = _state.value.tracks
    if (index !in list.indices) return
    startQueue(list, index)
}

/** index mengacu ke state.history (terbaru di depan). Lagu yang sudah diunduh diputar dari file. */
internal fun RexPlayerController.playFromHistoryImpl(index: Int) {
    val list = _state.value.history
    if (index !in list.indices) return
    startQueue(list, index)
}

internal fun RexPlayerController.startQueue(list: List<RexTrack>, index: Int, follow: Boolean = false) {
    cancelPending()
    queue = list
    queueIndex = index
    followContext = follow
    beginTrack(list[index])
}

//  User queue 

internal fun RexPlayerController.addToQueueImpl(track: RexTrack) {
    val entry = track.copy(audioUrl = "")
    when {
        userQueue.any { sameSong(it, entry) } -> postNotice("Sudah ada di antrian")
        userQueue.size >= MAX_QUEUE -> postNotice("Antrian penuh (maks $MAX_QUEUE lagu)")
        else -> {
            userQueue.add(entry)
            persistQueue()
            publishQueueInfo()
            postNotice("Ditambahkan ke antrian")
        }
    }
}

internal fun RexPlayerController.moveQueueToTopImpl(index: Int) {
    if (index !in 1 until userQueue.size) return
    val track = userQueue.removeAt(index)
    userQueue.add(0, track)
    persistQueue()
    publishQueueInfo()
}

/** Putar lagu dari antrian sekarang juga (lagunya keluar dari antrian). */
internal fun RexPlayerController.playFromQueueImpl(index: Int) {
    if (index !in userQueue.indices) return
    val track = userQueue.removeAt(index)
    persistQueue()
    cancelPending()
    beginTrack(track)
}

internal fun RexPlayerController.persistQueue() = queueStore.save(userQueue.toList())

//  Repeat & autoplay 

/** Ulangi lagu yang sedang diputar sebanyak [times] kali lagi (0 = mati, maks 50). */
internal fun RexPlayerController.setRepeatImpl(times: Int) {
    val n = times.coerceIn(0, MAX_REPEAT)
    _state.update { it.copy(repeatTotal = n, repeatLeft = n) }
}

/**
 * Kembali:
 * - Lagu sudah jalan > 3 detik (dan tidak force) -> ulang dari awal.
 * - Ada riwayat -> putar lagu sebelumnya (nama disimpan, audio diambil ulang lewat API / dari file offline).
 * - Tidak ada riwayat -> mundur di daftar asal.
 */
internal fun RexPlayerController.prevImpl(force: Boolean = false) {
    if (!force && prepared && _state.value.positionMs > RESTART_THRESHOLD_MS) {
        seekTo(0f)
        return
    }
    val now = _state.value.nowPlaying
    val target = previousTarget(now)
    if (target != null) {
        cancelPending()
        history.removeAll { sameSong(it, target) || (now != null && sameSong(it, now)) }
        store.save(history)
        syncQueueTo(target)
        beginTrack(target)
        return
    }
    if (queue.isEmpty()) {
        seekTo(0f)
        return
    }
    cancelPending()
    queueIndex = if (queueIndex - 1 < 0) queue.size - 1 else queueIndex - 1
    queue.getOrNull(queueIndex)?.let { beginTrack(it) }
}

/**
 * Urutan: antrian pengguna -> autoplay pintar -> daftar asal.
 * Saat offline, lagu yang belum diunduh dilewati.
 */
internal fun RexPlayerController.advance() {
    val offline = offlineMode()
    val queued = userQueue.indexOfFirst { !offline || RexOfflineManager.isDownloaded(it) }
    if (queued >= 0) {
        val track = userQueue.removeAt(queued)
        persistQueue()
        beginTrack(track)
        return
    }
    if (autoplay && !followContext) {
        playSmart()
        return
    }
    playContextNext()
}

internal fun RexPlayerController.playContextNext() {
    if (queue.isEmpty()) return
    val offline = offlineMode()
    for (i in queue.indices) {
        queueIndex = (queueIndex + 1) % queue.size
        val track = queue[queueIndex]
        if (!offline || RexOfflineManager.isDownloaded(track)) {
            beginTrack(track)
            return
        }
    }
    _state.update { it.copy(error = "tidak ada lagu offline lain untuk diputar") }
}

/** Pastikan target ada di antrian konteks & queueIndex menunjuk ke sana. */
private fun RexPlayerController.syncQueueTo(target: RexTrack) {
    val idx = queue.indexOfFirst { sameSong(it, target) }
    if (idx >= 0) {
        queueIndex = idx
        return
    }
    val at = queueIndex.coerceIn(0, queue.size)
    queue = queue.toMutableList().apply { add(at, target) }
    queueIndex = at
}

/** Catat lagu ke riwayat (tanpa audioUrl). Dipanggil setelah lagu benar-benar mulai diputar. */
internal fun RexPlayerController.pushHistory(track: RexTrack) {
    val entry = track.copy(audioUrl = "")
    history.removeAll { sameSong(it, entry) }
    history.add(entry)
    while (history.size > MAX_HISTORY) history.removeAt(0)
    store.save(history)
    publishQueueInfo()
}

/** Lagu tepat sebelum lagu yang sedang diputar di riwayat (saat offline: hanya yang sudah diunduh). */
private fun RexPlayerController.previousTarget(now: RexTrack?): RexTrack? {
    val last = history.lastOrNull()
    val list = if (now != null && last != null && sameSong(last, now)) {
        history.dropLast(1)
    } else {
        history
    }
    val offline = offlineMode()
    return list.lastOrNull { !offline || RexOfflineManager.isDownloaded(it) }
}

internal fun RexPlayerController.publishQueueInfo() {
    val now = _state.value.nowPlaying
    val hist = history.asReversed().filter { now == null || !sameSong(it, now) }
    val contextNext = !autoplay || followContext
    val up = userQueue.firstOrNull()
        ?: if (contextNext && queue.size > 1) queue[(queueIndex + 1) % queue.size] else null
    _state.update {
        it.copy(
            history = hist,
            upNext = up,
            previous = previousTarget(now),
            userQueue = userQueue.toList(),
            autoplay = autoplay
        )
    }
}
