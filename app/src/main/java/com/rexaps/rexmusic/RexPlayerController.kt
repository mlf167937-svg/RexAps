package com.rexaps.rexmusic

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pemutar musik tingkat aplikasi. Hidup di luar ViewModel supaya musik tetap jalan
 * saat layar ditutup dan bisa dikontrol dari notifikasi / lock screen.
 * Semua fungsi dipanggil dari main thread.
 */
object RexPlayerController {

    private lateinit var appContext: Context
    private lateinit var audioManager: AudioManager
    private lateinit var store: RexHistoryStore
    private lateinit var queueStore: RexQueueStore
    private lateinit var taste: RexTasteStore
    private var initialized = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(
        RexMusicUiState(tracks = RexMusicRegistry.defaultTracks)
    )
    val state: StateFlow<RexMusicUiState> = _state.asStateFlow()

    // player
    private var player: MediaPlayer? = null
    private var prepared = false
    private var progressJob: Job? = null
    private var seekJob: Job? = null
    private var seekPending = false
    private var tasteCounted = false

    // search / resolve / prepare / buffering flags
    private var searchJob: Job? = null
    private var playJob: Job? = null
    private var searching = false
    private var resolving = false
    private var preparing = false
    private var buffering = false
    private var resolveText = ""

    // konteks: daftar asal lagu (search / rekomendasi / riwayat), dipakai saat autoplay mati
    private var queue: List<RexTrack> = emptyList()
    private var queueIndex = 0

    // antrian buatan pengguna (metadata saja) + autoplay pintar
    private val userQueue = mutableListOf<RexTrack>()
    private var autoplay = true

    // riwayat kronologis (terlama di depan, terbaru di belakang). Hanya metadata.
    private val history = mutableListOf<RexTrack>()

    // audio focus
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnGain = false
    private val audioAttrs = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        store = RexHistoryStore(appContext)
        queueStore = RexQueueStore(appContext)
        taste = RexTasteStore(appContext)
        history.addAll(store.load())
        userQueue.addAll(queueStore.load())
        autoplay = taste.autoplay
        _state.update { it.copy(popularCount = RexPopularSongs.tracks.size) }
        publishQueueInfo()
        publishTaste()
    }

    // ───────────────────────── Search ─────────────────────────

    /** Debounced: aman dipanggil di tiap ketikan. */
    fun search(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            clearSearch()
            return
        }
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searching = true
            _state.update { it.copy(error = null) }
            publishLoading()
            RexMusicApi.search(q)
                .onSuccess { list ->
                    searching = false
                    _state.update { it.copy(searchResults = list) }
                }
                .onFailure { e ->
                    searching = false
                    _state.update { it.copy(error = e.message ?: "search gagal") }
                }
            publishLoading()
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        searching = false
        _state.update { it.copy(searchResults = emptyList()) }
        publishLoading()
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun consumeNotice() = _state.update { it.copy(notice = null) }

    private fun postNotice(message: String) = _state.update { it.copy(notice = message) }

    fun clearHistory() {
        history.clear()
        store.save(history)
        publishQueueInfo()
    }

    // ───────────────────────── Play from list ─────────────────────────

    fun playFromMain(index: Int) {
        val list = _state.value.tracks
        if (index !in list.indices) return
        startQueue(list, index)
    }

    fun playFromSearch(index: Int) {
        val list = _state.value.searchResults
        if (index !in list.indices) return
        startQueue(list, index)
    }

    /** index mengacu ke state.history (terbaru di depan). Audio diambil ulang lewat API. */
    fun playFromHistory(index: Int) {
        val list = _state.value.history
        if (index !in list.indices) return
        startQueue(list, index)
    }

    private fun startQueue(list: List<RexTrack>, index: Int) {
        cancelPending()
        queue = list
        queueIndex = index
        beginTrack(list[index])
    }

    /** Mix pintar: langsung pilih lagu dari artis favorit / populer. */
    fun playMix() {
        cancelPending()
        playSmart()
    }

    // ───────────────────────── User queue ─────────────────────────

    fun addToQueue(track: RexTrack) {
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

    fun removeFromQueue(index: Int) {
        if (index !in userQueue.indices) return
        userQueue.removeAt(index)
        persistQueue()
        publishQueueInfo()
    }

    fun moveQueueToTop(index: Int) {
        if (index !in 1 until userQueue.size) return
        val track = userQueue.removeAt(index)
        userQueue.add(0, track)
        persistQueue()
        publishQueueInfo()
    }

    fun clearQueue() {
        if (userQueue.isEmpty()) return
        userQueue.clear()
        persistQueue()
        publishQueueInfo()
        postNotice("Antrian dikosongkan")
    }

    /** Putar lagu dari antrian sekarang juga (lagunya keluar dari antrian). */
    fun playFromQueue(index: Int) {
        if (index !in userQueue.indices) return
        val track = userQueue.removeAt(index)
        persistQueue()
        cancelPending()
        beginTrack(track)
    }

    private fun persistQueue() = queueStore.save(userQueue.toList())

    // ───────────────────────── Repeat & autoplay ─────────────────────────

    /** Ulangi lagu yang sedang diputar sebanyak [times] kali lagi (0 = mati, maks 50). */
    fun setRepeat(times: Int) {
        val n = times.coerceIn(0, MAX_REPEAT)
        _state.update { it.copy(repeatTotal = n, repeatLeft = n) }
    }

    fun setAutoplay(on: Boolean) {
        autoplay = on
        taste.autoplay = on
        publishQueueInfo()
        postNotice(if (on) "Autoplay pintar aktif" else "Mengikuti urutan daftar")
    }

    // ───────────────────────── Next / previous ─────────────────────────

    /** Tombol next dari pengguna. */
    fun next() {
        penalizeEarlySkip()
        cancelPending()
        advance()
    }

    /**
     * Kembali:
     * - Lagu sudah jalan > 3 detik (dan tidak force) -> ulang dari awal.
     * - Ada riwayat -> putar lagu sebelumnya (nama disimpan, audio diambil ulang lewat API).
     * - Tidak ada riwayat -> mundur di daftar asal.
     */
    fun prev(force: Boolean = false) {
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

    fun currentTrack(): RexTrack? = _state.value.nowPlaying

    /** Urutan: antrian pengguna -> autoplay pintar -> daftar asal. */
    private fun advance() {
        if (userQueue.isNotEmpty()) {
            val track = userQueue.removeAt(0)
            persistQueue()
            beginTrack(track)
            return
        }
        if (autoplay) {
            playSmart()
            return
        }
        playContextNext()
    }

    private fun playContextNext() {
        if (queue.isEmpty()) return
        queueIndex = (queueIndex + 1) % queue.size
        queue.getOrNull(queueIndex)?.let { beginTrack(it) }
    }

    /** Pastikan target ada di antrian konteks & queueIndex menunjuk ke sana. */
    private fun syncQueueTo(target: RexTrack) {
        val idx = queue.indexOfFirst { sameSong(it, target) }
        if (idx >= 0) {
            queueIndex = idx
            return
        }
        val at = queueIndex.coerceIn(0, queue.size)
        queue = queue.toMutableList().apply { add(at, target) }
        queueIndex = at
    }

    private fun cancelPending() {
        playJob?.cancel()
        playJob = null
    }

    // ───────────────────────── Begin / resolve ─────────────────────────

    private fun beginTrack(track: RexTrack) {
        stopPlayer()
        markTrack(track)
        playJob = scope.launch { resolveAndStart(track) }
    }

    /** Set state "lagu baru" (reset ulangi, posisi, dsb). Tidak menyentuh player. */
    private fun markTrack(track: RexTrack) {
        tasteCounted = false
        resolving = true
        resolveText = if (track.spotifyUrl.isBlank()) "mencari ${track.title}..." else "menyiapkan audio..."
        _state.update {
            it.copy(
                nowPlaying = track, currentIndex = queueIndex,
                isPlaying = false, positionMs = 0L, durationMs = 0L,
                bufferedPercent = 0, seekVersion = it.seekVersion + 1, error = null,
                repeatTotal = 0, repeatLeft = 0
            )
        }
        publishLoading()
        publishQueueInfo()
        RexMusicService.start(appContext)
    }

    /** true = audio berhasil di-resolve dan player dimulai. */
    private suspend fun resolveAndStart(track: RexTrack): Boolean {
        val result = resolveTrack(track)
        resolving = false
        val fresh = result.getOrNull()
        if (fresh == null) {
            _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "gagal memuat audio") }
            publishLoading()
            return false
        }
        replaceTrack(fresh)
        publishLoading()
        startPlayer(fresh.audioUrl)
        return true
    }

    /**
     * 1) spotifyUrl ada -> langsung resolve audio.
     * 2) Gagal / kosong -> search "artist + title", ambil hasil pertama, resolve.
     * id/title/artist/album/cover asli dipertahankan.
     */
    private suspend fun resolveTrack(track: RexTrack): Result<RexTrack> {
        if (track.spotifyUrl.isNotBlank()) {
            val direct = RexMusicApi.resolveAudio(track)
            if (direct.isSuccess) return direct
        }

        val list = RexMusicApi.search("${track.artist} ${track.title}".trim())
            .getOrElse { return Result.failure(it) }
        val first = list.firstOrNull()
            ?: return Result.failure(Exception("lagu \"${track.title}\" tidak ditemukan"))

        return RexMusicApi.resolveAudio(
            first.copy(
                id = track.id, title = track.title, artist = track.artist,
                album = track.album, cover = track.cover.ifBlank { first.cover }
            )
        )
    }

    /** Update track di queue + semua list berdasarkan id (bukan index, jadi anti out-of-bounds). */
    private fun replaceTrack(fresh: RexTrack) {
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

    // ───────────────────────── Smart autoplay ─────────────────────────

    /** Pilih lagu berikutnya sendiri; coba sampai [SMART_RETRIES] kali kalau audio gagal dimuat. */
    private fun playSmart() {
        stopPlayer()
        resolving = true
        resolveText = "memilih lagu untukmu..."
        _state.update {
            it.copy(
                isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0,
                seekVersion = it.seekVersion + 1, error = null, repeatTotal = 0, repeatLeft = 0
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
                if (queue.isNotEmpty()) {
                    playContextNext()
                } else {
                    _state.update {
                        it.copy(error = "belum ada lagu berikutnya. Isi RexPopularSongs atau putar dari daftar")
                    }
                }
            }
        }
        playJob = job
        job.start()
    }

    /**
     * ~65% dari artis favorit (kalau sudah ada selera terbaca), selain itu acak dari populer.
     * Lagu yang baru diputar dihindari.
     */
    private suspend fun smartPick(exclude: Set<String>): RexTrack? {
        val avoid = recentKeys() + exclude
        val favorite = taste.pickFavorite()
        if (favorite != null && Random.nextFloat() < TASTE_CHANCE) {
            pickFromArtist(favorite, avoid)?.let { return it }
        }
        popularPick(avoid)?.let { return it }
        if (favorite != null) pickFromArtist(favorite, avoid)?.let { return it }
        return null
    }

    private suspend fun pickFromArtist(artist: String, avoid: Set<String>): RexTrack? {
        val list = RexMusicApi.search(artist).getOrNull() ?: return null
        val key = artist.lowercase()
        return list
            .filter { it.artist.lowercase().contains(key) && songKey(it) !in avoid }
            .randomOrNull()
    }

    private fun popularPick(avoid: Set<String>): RexTrack? {
        val all = RexPopularSongs.tracks
        if (all.isEmpty()) return null
        val nowKey = _state.value.nowPlaying?.let { songKey(it) }
        return all.filter { songKey(it) !in avoid }.randomOrNull()
            ?: all.filter { songKey(it) != nowKey }.randomOrNull()
    }

    private fun recentKeys(): Set<String> {
        val keys = history.takeLast(RECENT_WINDOW).map { songKey(it) }.toMutableSet()
        _state.value.nowPlaying?.let { keys += songKey(it) }
        return keys
    }

    private fun penalizeEarlySkip() {
        val now = _state.value.nowPlaying ?: return
        if (prepared && !tasteCounted && _state.value.positionMs < EARLY_SKIP_MS) {
            taste.recordSkip(now.artist)
            publishTaste()
        }
    }

    private fun publishTaste() {
        _state.update { it.copy(favoriteArtists = taste.favorites(3)) }
    }

    // ───────────────────────── History ─────────────────────────

    private fun sameSong(a: RexTrack, b: RexTrack): Boolean =
        a.id == b.id ||
            (a.title.equals(b.title, ignoreCase = true) && a.artist.equals(b.artist, ignoreCase = true))

    /** Catat lagu ke riwayat (tanpa audioUrl). Dipanggil setelah lagu benar-benar mulai diputar. */
    private fun pushHistory(track: RexTrack) {
        val entry = track.copy(audioUrl = "")
        history.removeAll { sameSong(it, entry) }
        history.add(entry)
        while (history.size > MAX_HISTORY) history.removeAt(0)
        store.save(history)
        publishQueueInfo()
    }

    /** Lagu tepat sebelum lagu yang sedang diputar di riwayat. */
    private fun previousTarget(now: RexTrack?): RexTrack? {
        val last = history.lastOrNull()
        val list = if (now != null && last != null && sameSong(last, now)) {
            history.dropLast(1)
        } else {
            history
        }
        return list.lastOrNull()
    }

    private fun publishQueueInfo() {
        val now = _state.value.nowPlaying
        val hist = history.asReversed().filter { now == null || !sameSong(it, now) }
        val up = userQueue.firstOrNull()
            ?: if (!autoplay && queue.size > 1) queue[(queueIndex + 1) % queue.size] else null
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

    // ───────────────────────── Player ─────────────────────────

    private fun startPlayer(url: String) {
        if (url.isBlank()) return fail("URL audio kosong")
        stopPlayer()
        if (!requestFocus()) return fail("audio sedang dipakai aplikasi lain")
        try {
            val mp = MediaPlayer()
            player = mp
            preparing = true
            publishLoading()
            mp.setAudioAttributes(audioAttrs)
            runCatching { mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK) }
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                prepared = true
                preparing = false
                buffering = false
                _state.update { s -> s.copy(durationMs = mp.duration.toLong(), isPlaying = true) }
                publishLoading()
                mp.start()
                startProgressLoop(mp)
                _state.value.nowPlaying?.let { pushHistory(it) }
            }
            mp.setOnBufferingUpdateListener { _, percent ->
                if (player !== mp) return@setOnBufferingUpdateListener
                _state.update { s -> s.copy(bufferedPercent = percent.coerceIn(0, 100)) }
            }
            mp.setOnInfoListener { _, what, _ ->
                if (player === mp) {
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> setBuffering(true)
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> setBuffering(false)
                    }
                }
                false
            }
            mp.setOnCompletionListener {
                if (player !== mp) return@setOnCompletionListener
                val left = _state.value.repeatLeft
                if (left > 0) {
                    // ulangi lagu yang sama tanpa memanggil API lagi
                    _state.value.nowPlaying?.let { taste.recordPlay(it.artist, REPEAT_TASTE_WEIGHT) }
                    _state.update { s ->
                        s.copy(
                            repeatLeft = left - 1, positionMs = 0L, isPlaying = true,
                            seekVersion = s.seekVersion + 1
                        )
                    }
                    runCatching {
                        mp.seekTo(0)
                        mp.start()
                    }
                } else {
                    _state.update { s -> s.copy(isPlaying = false, repeatTotal = 0, repeatLeft = 0) }
                    cancelPending()
                    advance()
                }
            }
            mp.setOnErrorListener { _, what, _ ->
                queue.getOrNull(queueIndex)?.let { RexMusicApi.invalidateAudio(it) }
                _state.value.nowPlaying?.let { RexMusicApi.invalidateAudio(it) }
                stopPlayer() // supaya tombol play bisa mencoba ulang lewat API
                fail("gagal memutar audio (kode $what)")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stopPlayer()
            fail(e.message ?: "gagal memutar audio")
        }
    }

    private fun setBuffering(value: Boolean) {
        if (buffering == value) return
        buffering = value
        publishLoading()
    }

    private fun stopPlayer() {
        progressJob?.cancel()
        seekJob?.cancel()
        seekPending = false
        prepared = false
        preparing = false
        buffering = false
        runCatching { player?.release() }
        player = null
        abandonFocus()
    }

    /** Hentikan total (tombol tutup di notifikasi). Lagu tetap tercatat, bisa diputar ulang. */
    fun stop() {
        cancelPending()
        resolving = false
        stopPlayer()
        _state.update {
            it.copy(
                isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0,
                repeatTotal = 0, repeatLeft = 0
            )
        }
        publishLoading()
    }

    /** Progress + deteksi lag + pencatatan selera (>= 15 detik dianggap benar-benar didengar). */
    private fun startProgressLoop(mp: MediaPlayer) {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastPos = -1L
            var stalledTicks = 0
            while (isActive && player === mp) {
                if (!seekPending) {
                    runCatching {
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition.toLong()
                            _state.update { it.copy(positionMs = pos) }
                            if (pos == lastPos) {
                                if (++stalledTicks >= STALL_TICKS) setBuffering(true)
                            } else {
                                stalledTicks = 0
                                lastPos = pos
                                setBuffering(false)
                            }
                            if (!tasteCounted && pos >= TASTE_LISTEN_MS) {
                                tasteCounted = true
                                _state.value.nowPlaying?.let { taste.recordPlay(it.artist) }
                                publishTaste()
                            }
                        } else {
                            stalledTicks = 0
                        }
                    }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    /** Play/pause. Kalau player sudah mati (error / ditutup) -> putar ulang lagu ini lewat API. */
    fun togglePlay() {
        val mp = player
        if (mp == null) {
            val now = _state.value.nowPlaying
            if (!resolving && now != null) {
                cancelPending()
                beginTrack(now)
            }
            return
        }
        if (!prepared) return
        if (runCatching { mp.isPlaying }.getOrDefault(false)) {
            pauseInternal()
        } else if (requestFocus()) {
            runCatching { mp.start() }
            _state.update { it.copy(isPlaying = true) }
        }
    }

    fun play() {
        if (!_state.value.isPlaying) togglePlay()
    }

    fun pause() {
        if (_state.value.isPlaying) togglePlay()
    }

    /** Posisi UI langsung update; seek asli didebounce supaya drag tidak membebani MediaPlayer. */
    fun seekTo(ratio: Float) {
        val mp = player ?: return
        val d = _state.value.durationMs
        if (!prepared || d <= 0) return
        val target = (d * ratio.coerceIn(0f, 1f)).toLong()
        seekPending = true
        _state.update { it.copy(positionMs = target, seekVersion = it.seekVersion + 1) }
        seekJob?.cancel()
        seekJob = scope.launch {
            delay(SEEK_DEBOUNCE_MS)
            runCatching { mp.seekTo(target.toInt()) }
            delay(300)
            seekPending = false
        }
    }

    private fun pauseInternal() {
        if (prepared) runCatching { player?.pause() }
        buffering = false
        _state.update { it.copy(isPlaying = false) }
        publishLoading()
    }

    private fun fail(message: String) {
        preparing = false
        buffering = false
        _state.update { it.copy(error = message, isPlaying = false) }
        publishLoading()
    }

    // ───────────────────────── Audio focus ─────────────────────────

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnGain = false
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnGain = _state.value.isPlaying
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                player?.setVolume(1f, 1f)
                if (resumeOnGain && prepared) {
                    resumeOnGain = false
                    runCatching { player?.start() }
                    _state.update { it.copy(isPlaying = true) }
                }
            }
        }
    }

    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttrs)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
                .also { focusRequest = it }
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    // ───────────────────────── Misc ─────────────────────────

    /**
     * loading = search / resolve / prepare.
     * Buffering (lag saat memutar) hanya lewat [LoadPhase] supaya halaman browse tidak ikut loading.
     */
    private fun publishLoading() = _state.update {
        val phase = when {
            resolving -> LoadPhase.Resolving
            preparing -> LoadPhase.Preparing
            buffering -> LoadPhase.Buffering
            searching -> LoadPhase.Searching
            else -> LoadPhase.Idle
        }
        it.copy(
            loading = searching || resolving || preparing,
            phase = phase,
            loadingText = when (phase) {
                LoadPhase.Resolving -> resolveText
                LoadPhase.Preparing -> "menyiapkan audio..."
                LoadPhase.Buffering -> "buffering..."
                LoadPhase.Searching -> "mencari..."
                LoadPhase.Idle -> ""
            }
        )
    }

    private fun List<RexTrack>.replaceById(t: RexTrack) = map { if (it.id == t.id) t else it }

    private const val SEARCH_DEBOUNCE_MS = 400L
    private const val SEEK_DEBOUNCE_MS = 80L
    private const val PROGRESS_INTERVAL_MS = 500L
    private const val RESTART_THRESHOLD_MS = 3000L
    private const val STALL_TICKS = 2
    private const val MAX_HISTORY = 50
    private const val MAX_QUEUE = 100
    private const val MAX_REPEAT = 50

    // smart autoplay (silakan ubah sesuai selera)
    private const val TASTE_CHANCE = 0.65f        // peluang lagu berikutnya dari artis favorit
    private const val TASTE_LISTEN_MS = 15_000L   // didengar segini lama = dihitung "suka"
    private const val EARLY_SKIP_MS = 10_000L     // di-skip sebelum ini = dianggap kurang suka
    private const val REPEAT_TASTE_WEIGHT = 0.5
    private const val RECENT_WINDOW = 8           // hindari lagu yang baru diputar
    private const val SMART_RETRIES = 3
}
