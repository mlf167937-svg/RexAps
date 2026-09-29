package com.rexaps.rexmusic

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
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

    // search / resolve / prepare / buffering flags
    private var searchJob: Job? = null
    private var playJob: Job? = null
    private var searching = false
    private var resolving = false
    private var preparing = false
    private var buffering = false
    private var resolveText = ""

    // queue: snapshot list yang sedang diputar (search / rekomendasi / riwayat)
    private var queue: List<RexTrack> = emptyList()
    private var queueIndex = 0

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
        history.addAll(store.load())
        publishQueueInfo()
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

    fun clearHistory() {
        history.clear()
        store.save(history)
        publishQueueInfo()
    }

    // ───────────────────────── Queue & play ─────────────────────────

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
        queue = list
        queueIndex = index
        playCurrent()
    }

    private fun playCurrent() {
        val track = queue.getOrNull(queueIndex) ?: return
        playJob?.cancel()
        stopPlayer()
        resolving = true
        resolveText = if (track.spotifyUrl.isBlank()) "mencari ${track.title}..." else "menyiapkan audio..."
        _state.update {
            it.copy(
                nowPlaying = track, currentIndex = queueIndex,
                isPlaying = false, positionMs = 0L, durationMs = 0L,
                bufferedPercent = 0, seekVersion = it.seekVersion + 1, error = null
            )
        }
        publishLoading()
        publishQueueInfo()
        RexMusicService.start(appContext)

        playJob = scope.launch {
            resolveTrack(track)
                .onSuccess { fresh ->
                    resolving = false
                    replaceTrack(fresh)
                    publishLoading()
                    startPlayer(fresh.audioUrl)
                }
                .onFailure { e ->
                    resolving = false
                    _state.update { it.copy(error = e.message ?: "gagal memuat audio") }
                    publishLoading()
                }
        }
    }

    /**
     * 1) spotifyUrl ada -> langsung resolve audio.
     * 2) Kalau gagal / spotifyUrl kosong -> search "artist + title", ambil hasil pertama, resolve.
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

    fun next() {
        if (queue.isEmpty()) return
        queueIndex = (queueIndex + 1) % queue.size
        playCurrent()
    }

    /**
     * Kembali:
     * - Kalau lagu sudah jalan > 3 detik (dan tidak force) -> ulang dari awal.
     * - Kalau ada riwayat -> putar lagu sebelumnya (nama disimpan, audio diambil ulang lewat API).
     * - Kalau tidak ada riwayat -> mundur di antrian.
     */
    fun prev(force: Boolean = false) {
        if (!force && prepared && _state.value.positionMs > RESTART_THRESHOLD_MS) {
            seekTo(0f)
            return
        }
        val now = _state.value.nowPlaying
        val target = previousTarget(now)
        if (target != null) {
            // buang lagu sekarang + target dari riwayat; target dicatat ulang saat mulai diputar
            history.removeAll { sameSong(it, target) || (now != null && sameSong(it, now)) }
            store.save(history)
            syncQueueTo(target)
            playCurrent()
            return
        }
        if (queue.isEmpty()) {
            seekTo(0f)
            return
        }
        queueIndex = if (queueIndex - 1 < 0) queue.size - 1 else queueIndex - 1
        playCurrent()
    }

    fun currentTrack(): RexTrack? = _state.value.nowPlaying

    /** Pastikan target ada di antrian & queueIndex menunjuk ke sana (disisipkan sebelum posisi sekarang). */
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
        val up = if (queue.size > 1) queue[(queueIndex + 1) % queue.size] else null
        val prev = previousTarget(now)
        _state.update { it.copy(history = hist, upNext = up, previous = prev) }
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
                _state.update { s -> s.copy(isPlaying = false) }
                next()
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

    /** Hentikan total (dipakai tombol tutup di notifikasi). Lagu tetap tercatat, bisa diputar ulang. */
    fun stop() {
        playJob?.cancel()
        resolving = false
        stopPlayer()
        _state.update {
            it.copy(isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0)
        }
        publishLoading()
    }

    /** Progress + deteksi lag (fallback kalau device tidak mengirim MEDIA_INFO_BUFFERING_*). */
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
            if (!resolving && queue.isNotEmpty() && _state.value.nowPlaying != null) playCurrent()
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
}
