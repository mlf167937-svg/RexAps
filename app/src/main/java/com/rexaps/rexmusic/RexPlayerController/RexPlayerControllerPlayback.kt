package com.rexaps.rexmusic

import android.content.Context
import android.media.AudioManager
import kotlin.random.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Synchronized internal
    fun RexPlayerController.__split_init_1(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        store = RexHistoryStore(appContext)
        queueStore = RexQueueStore(appContext)
        taste = RexTasteStore(appContext)
        playlistStore = RexPlaylistStore(appContext)
        history.addAll(store.load())
        userQueue.addAll(queueStore.load())
        autoplay = taste.autoplay
        _state.update { it.copy(popularCount = RexPopularSongs.tracks.size, playlists = playlistStore.list()) }

        RexOfflineManager.init(appContext)
        scope.launch { RexOfflineManager.state.collect { onOfflineChanged(it) } }
        scope.launch { RexOfflineManager.messages.collect { postNotice(it) } }

        publishQueueInfo()
        publishTaste()
    }

/**
     * Kembali:
     * - Lagu sudah jalan > 3 detik (dan tidak force) -> ulang dari awal.
     * - Ada riwayat -> putar lagu sebelumnya (nama disimpan, audio diambil ulang lewat API / dari file offline).
     * - Tidak ada riwayat -> mundur di daftar asal.
     */
    fun RexPlayerController.__split_prev_11(force: Boolean = false) {
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
    internal fun RexPlayerController.__split_advance_12() {
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

internal fun RexPlayerController.__split_playContextNext_13() {
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

/** Set state "lagu baru" (reset ulangi, posisi, dsb). Tidak menyentuh player. */
    internal fun RexPlayerController.__split_markTrack_15(track: RexTrack) {
        tasteCounted = false
        resolving = true
        resolveText = when {
            RexOfflineManager.isDownloaded(track) -> "membuka dari penyimpanan..."
            track.spotifyUrl.isBlank() -> "mencari ${track.title}..."
            else -> "menyiapkan audio..."
        }
        _state.update {
            it.copy(
                nowPlaying = track, currentIndex = queueIndex,
                isPlaying = false, positionMs = 0L, durationMs = 0L,
                bufferedPercent = 0, seekVersion = it.seekVersion + 1, error = null,
                repeatTotal = 0, repeatLeft = 0, nowPlayingOffline = false
            )
        }
        publishLoading()
        publishQueueInfo()
        RexMusicService.start(appContext)
    }

/**
     * Paksa widget re-render tiap kali state penting berubah (lirik selesai dimuat, lagu ganti, dll).
     * Service akan mengirim cover bitmap-nya sendiri saat siap.
     */
    internal fun RexPlayerController.__split_pushWidgetUpdate_18() {
        if (!initialized) return
        runCatching {
            com.rexaps.rexmusic.widget.RexMusicWidget.updateAll(
                appContext,
                _state.value,
                force = true
            )
        }
    }

/** Update track di queue + semua list berdasarkan id (bukan index, jadi anti out-of-bounds). */
    internal fun RexPlayerController.__split_replaceTrack_19(fresh: RexTrack) {
        queue = queue.replaceById(fresh)
        _state.update {
            it.copy(
                nowPlaying = fresh,
                tracks = it.tracks.replaceById(fresh),
                searchResults = it.searchResults.replaceById(fresh),
                history = it.history.replaceById(fresh)
            )
        }
    }

/** Pilih lagu berikutnya sendiri; coba sampai [SMART_RETRIES] kali kalau audio gagal dimuat. */
    internal fun RexPlayerController.__split_playSmart_20() {
        stopPlayer()
        resolving = true
        resolveText = "memilih lagu untukmu..."
        _state.update {
            it.copy(
                isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0,
                seekVersion = it.seekVersion + 1, error = null, repeatTotal = 0, repeatLeft = 0,
                nowPlayingOffline = false
            )
        }
        publishLoading()
        RexMusicService.start(appContext)

        val job = scope.launch(start = CoroutineStart.LAZY) {
            val tried = mutableSetOf<String>()
            var attempts = 0
            while (attempts < SMART_RETRIES) {
                val pick = smartPick(tried) ?: break
                tried += songKey(pick)
                markTrack(pick)
                if (resolveAndStart(pick)) return@launch
                attempts++
            }
            if (attempts == 0) {
                resolving = false
                publishLoading()
                when {
                    offlineMode() && RexOfflineManager.state.value.entries.isEmpty() ->
                        _state.update { it.copy(error = "belum ada lagu offline. Unduh lagu dulu saat online") }
                    queue.isNotEmpty() -> playContextNext()
                    else -> _state.update {
                        it.copy(error = "belum ada lagu berikutnya. Isi RexPopularSongs atau putar dari daftar")
                    }
                }
            }
        }
        playJob = job
        job.start()
    }

/**
     * Online: ~65% dari artis favorit (kalau selera sudah terbaca), selain itu acak dari populer.
     * Offline: sama, tapi hanya dari lagu yang sudah diunduh (tanpa internet).
     * Lagu yang baru diputar dihindari.
     */
    internal suspend fun RexPlayerController.__split_smartPick_21(exclude: Set<String>): RexTrack? {
        val avoid = recentKeys() + exclude
        if (offlineMode()) return offlinePick(avoid)

        val favorite = taste.pickFavorite()
        if (favorite != null && Random.nextFloat() < TASTE_CHANCE) {
            pickFromArtist(favorite, avoid)?.let { return it }
        }
        popularPick(avoid)?.let { return it }
        if (favorite != null) pickFromArtist(favorite, avoid)?.let { return it }
        return null
    }

internal suspend fun RexPlayerController.__split_pickFromArtist_23(artist: String, avoid: Set<String>): RexTrack? {
        val list = RexMusicApi.search(artist).getOrNull() ?: return null
        val key = artist.lowercase()
        return list
            .filter { it.artist.lowercase().contains(key) && songKey(it) !in avoid }
            .randomOrNull()
    }

internal fun RexPlayerController.__split_popularPick_24(avoid: Set<String>): RexTrack? {
        val all = RexPopularSongs.tracks
        if (all.isEmpty()) return null
        val nowKey = _state.value.nowPlaying?.let { songKey(it) }
        return all.filter { songKey(it) !in avoid }.randomOrNull()
            ?: all.filter { songKey(it) != nowKey }.randomOrNull()
    }

internal fun RexPlayerController.__split_penalizeEarlySkip_25() {
        val now = _state.value.nowPlaying ?: return
        if (prepared && !tasteCounted && _state.value.positionMs < EARLY_SKIP_MS) {
            taste.recordSkip(now.artist)
            publishTaste()
        }
    }

/** Lagu tepat sebelum lagu yang sedang diputar di riwayat (saat offline: hanya yang sudah diunduh). */
    internal fun RexPlayerController.__split_previousTarget_27(now: RexTrack?): RexTrack? {
        val last = history.lastOrNull()
        val list = if (now != null && last != null && sameSong(last, now)) {
            history.dropLast(1)
        } else {
            history
        }
        val offline = offlineMode()
        return list.lastOrNull { !offline || RexOfflineManager.isDownloaded(it) }
    }
