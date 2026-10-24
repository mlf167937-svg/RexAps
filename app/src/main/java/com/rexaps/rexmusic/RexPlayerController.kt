package com.rexaps.rexmusic

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pemutar musik tingkat aplikasi. Hidup di luar ViewModel supaya musik tetap jalan
 * saat layar ditutup dan bisa dikontrol dari notifikasi / lock screen.
 * Semua fungsi dipanggil dari main thread.
 */
object RexPlayerController {

    internal lateinit var appContext: Context
    internal lateinit var audioManager: AudioManager
    internal lateinit var store: RexHistoryStore
    internal lateinit var queueStore: RexQueueStore
    internal lateinit var taste: RexTasteStore
    internal lateinit var playlistStore: RexPlaylistStore
    internal var initialized = false

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    internal val _state = MutableStateFlow(
        RexMusicUiState(tracks = RexMusicRegistry.defaultTracks)
    )
    val state: StateFlow<RexMusicUiState> = _state.asStateFlow()

    // player
    internal var player: MediaPlayer? = null
    internal var syncVolume: Float = 1f
    internal var syncPrepareTrackId: String? = null
    internal var prepared = false
    internal var progressJob: Job? = null
    internal var seekJob: Job? = null
    internal var seekPending = false
    internal var tasteCounted = false

    internal var searchJob: Job? = null
    internal var playJob: Job? = null
    internal var searching = false
    internal var resolving = false
    internal var preparing = false
    internal var buffering = false
    internal var resolveText = ""

    internal var queue: List<RexTrack> = emptyList()
    internal var queueIndex = 0

    internal var followContext = false

    internal val userQueue = mutableListOf<RexTrack>()
    internal var autoplay = true

    internal val history = mutableListOf<RexTrack>()

    internal var focusRequest: AudioFocusRequest? = null
    private var resumeOnGain = false
    internal val audioAttrs = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    @Synchronized
    fun init(context: Context) = this.__split_init_1(context)

    internal fun offlineMode(): Boolean = this.__splitExtra_offlineMode_1()

    internal fun onOfflineChanged(o: OfflineState) = this.__splitExtra_onOfflineChanged_2(o)


    fun setOfflineMode(on: Boolean) = this.__splitExtra_setOfflineMode_3(on)


    fun refreshPlaylists() = this.__splitExtra_refreshPlaylists_4()

    fun createPlaylist(name: String) = this.__split_createPlaylist_2(name)

    fun addToPlaylist(name: String, track: RexTrack) = this.__split_addToPlaylist_3(name, track)

    fun removeFromPlaylist(name: String, track: RexTrack) = this.__splitExtra_removeFromPlaylist_5(name, track)

    fun deletePlaylist(name: String) = this.__splitExtra_deletePlaylist_6(name)

    fun playPlaylist(name: String, startTrackId: String? = null) = this.__split_playPlaylist_4(name, startTrackId)

    /** Putar lagu dari perpustakaan offline; Next mengikuti urutan perpustakaan. */
    fun playFromOffline(track: RexTrack) = this.__split_playFromOffline_5(track)

    /** Prepare a local track for synchronized playback without emitting audio before the room target time. */
    fun prepareFromOfflineForSync(track: RexTrack) = this.__split_prepareFromOfflineForSync_6(track)

    /** "Putar semua" / "Acak" di perpustakaan offline. */
    fun playOfflineAll(shuffle: Boolean) = this.__split_playOfflineAll_7(shuffle)


    /** Debounced: aman dipanggil di tiap ketikan. Tidak dipakai saat mode offline. */
    fun search(query: String) = this.__split_search_8(query)

    fun clearSearch() = this.__splitExtra_clearSearch_7()

    fun dismissError() = this.__splitExtra_dismissError_8()

    fun consumeNotice() = this.__splitExtra_consumeNotice_9()

    internal fun postNotice(message: String) = this.__splitExtra_postNotice_10(message)

    fun clearHistory() = this.__splitExtra_clearHistory_11()


    fun playFromMain(index: Int) = this.__splitExtra_playFromMain_12(index)

    fun playFromSearch(index: Int) = this.__splitExtra_playFromSearch_13(index)

    /** index mengacu ke state.history (terbaru di depan). Lagu yang sudah diunduh diputar dari file. */
    fun playFromHistory(index: Int) = this.__splitExtra_playFromHistory_14(index)

    internal fun startQueue(list: List<RexTrack>, index: Int, follow: Boolean = false) = this.__splitExtra_startQueue_15(list, index, follow)

    /** Mix pintar: langsung pilih lagu dari artis favorit / populer (atau dari unduhan saat offline). */
    fun playMix() = this.__splitExtra_playMix_16()


    fun addToQueue(track: RexTrack) = this.__split_addToQueue_9(track)

    fun removeFromQueue(index: Int) = this.__splitExtra_removeFromQueue_17(index)

    fun moveQueueToTop(index: Int) = this.__split_moveQueueToTop_10(index)

    fun clearQueue() = this.__splitExtra_clearQueue_18()

    /** Putar lagu dari antrian sekarang juga (lagunya keluar dari antrian). */
    fun playFromQueue(index: Int) = this.__splitExtra_playFromQueue_19(index)

    internal fun persistQueue() = this.__splitExtra_persistQueue_20()


    /** Ulangi lagu yang sedang diputar sebanyak [times] kali lagi (0 = mati, maks 50). */
    fun setRepeat(times: Int) = this.__splitExtra_setRepeat_21(times)

    fun setAutoplay(on: Boolean) = this.__splitExtra_setAutoplay_22(on)


    /** Tombol next dari pengguna. */
    fun next() = this.__splitExtra_next_23()

    /**
     * Kembali:
     * - Lagu sudah jalan > 3 detik (dan tidak force) -> ulang dari awal.
     * - Ada riwayat -> putar lagu sebelumnya (nama disimpan, audio diambil ulang lewat API / dari file offline).
     * - Tidak ada riwayat -> mundur di daftar asal.
     */
    fun prev(force: Boolean = false) = this.__split_prev_11(force)

    fun currentTrack(): RexTrack? = this.__splitExtra_currentTrack_24()

    /**
     * Urutan: antrian pengguna -> autoplay pintar -> daftar asal.
     * Saat offline, lagu yang belum diunduh dilewati.
     */
    internal fun advance() = this.__split_advance_12()

    internal fun playContextNext() = this.__split_playContextNext_13()

    /** Pastikan target ada di antrian konteks & queueIndex menunjuk ke sana. */
    internal fun syncQueueTo(target: RexTrack) = this.__split_syncQueueTo_14(target)

    internal fun cancelPending() = this.__splitExtra_cancelPending_25()


    internal fun beginTrack(track: RexTrack) = this.__splitExtra_beginTrack_26(track)

    /** Set state "lagu baru" (reset ulangi, posisi, dsb). Tidak menyentuh player. */
    internal fun markTrack(track: RexTrack) = this.__split_markTrack_15(track)

    /**
     * File offline dipakai kalau ada (tanpa internet). Kalau belum diunduh: saat mode offline gagal
     * dengan pesan jelas, saat online diambil lewat API. true = pemutaran dimulai.
     */
    internal suspend fun resolveAndStart(track: RexTrack): Boolean = this.__split_resolveAndStart_16(track)


    internal var lyricsJob: Job? = null

    internal fun loadLyrics(track: RexTrack) = this.__split_loadLyrics_17(track)

    /**
     * Paksa widget re-render tiap kali state penting berubah (lirik selesai dimuat, lagu ganti, dll).
     * Service akan mengirim cover bitmap-nya sendiri saat siap.
     */
    internal fun pushWidgetUpdate() = this.__split_pushWidgetUpdate_18()

    fun toggleLyrics() = this.__splitExtra_toggleLyrics_27()

    fun closeLyrics() = this.__splitExtra_closeLyrics_28()
    /** Update track di queue + semua list berdasarkan id (bukan index, jadi anti out-of-bounds). */
    internal fun replaceTrack(fresh: RexTrack) = this.__split_replaceTrack_19(fresh)


    /** Pilih lagu berikutnya sendiri; coba sampai [SMART_RETRIES] kali kalau audio gagal dimuat. */
    internal fun playSmart() = this.__split_playSmart_20()

    /**
     * Online: ~65% dari artis favorit (kalau selera sudah terbaca), selain itu acak dari populer.
     * Offline: sama, tapi hanya dari lagu yang sudah diunduh (tanpa internet).
     * Lagu yang baru diputar dihindari.
     */
    internal suspend fun smartPick(exclude: Set<String>): RexTrack? = this.__split_smartPick_21(exclude)

    internal fun offlinePick(avoid: Set<String>): RexTrack? = this.__split_offlinePick_22(avoid)

    internal suspend fun pickFromArtist(artist: String, avoid: Set<String>): RexTrack? = this.__split_pickFromArtist_23(artist, avoid)

    internal fun popularPick(avoid: Set<String>): RexTrack? = this.__split_popularPick_24(avoid)

    internal fun recentKeys(): Set<String> = this.__splitExtra_recentKeys_29()

    internal fun penalizeEarlySkip() = this.__split_penalizeEarlySkip_25()

    internal fun publishTaste() = this.__splitExtra_publishTaste_30()


    internal fun sameSong(a: RexTrack, b: RexTrack): Boolean = this.__splitExtra_sameSong_31(a, b)

    /** Catat lagu ke riwayat (tanpa audioUrl). Dipanggil setelah lagu benar-benar mulai diputar. */
    internal fun pushHistory(track: RexTrack) = this.__split_pushHistory_26(track)

    /** Lagu tepat sebelum lagu yang sedang diputar di riwayat (saat offline: hanya yang sudah diunduh). */
    internal fun previousTarget(now: RexTrack?): RexTrack? = this.__split_previousTarget_27(now)

    internal fun publishQueueInfo() = this.__split_publishQueueInfo_28()


    /** [source] = URL audio (online) atau path file (local = true). */
    internal fun startPlayer(source: String, local: Boolean = false) = this.__split_startPlayer_29(source, local)

    internal fun setBuffering(value: Boolean) = this.__splitExtra_setBuffering_32(value)

    internal fun stopPlayer() = this.__split_stopPlayer_30()

    /** Hentikan total (tombol tutup di notifikasi). Lagu tetap tercatat, bisa diputar ulang. */
    fun stop() = this.__split_stop_31()

    /** Progress + deteksi lag + pencatatan selera (>= 15 detik dianggap benar-benar didengar). */
    internal fun startProgressLoop(mp: MediaPlayer) = this.__split_startProgressLoop_32(mp)

    /** Play/pause. Kalau player sudah mati (error / ditutup) -> putar ulang lagu ini. */
    fun togglePlay() = this.__split_togglePlay_33()

    fun play() = this.__splitExtra_play_33()

    fun pause() = this.__splitExtra_pause_34()

    /** Local playback gain for RexMusicSync; does not change Android system volume. */
    fun setSyncVolume(volume: Float) = this.__splitExtra_setSyncVolume_35(volume)

    /** Posisi UI langsung update; seek asli didebounce supaya drag tidak membebani MediaPlayer. */
    fun seekTo(ratio: Float) = this.__splitExtra_seekTo_36(ratio)

    /** Immediate seek for sync correction; unlike the UI drag path, this is not debounced. */
    fun seekToPositionImmediate(positionMs: Long) = this.__split_seekToPositionImmediate_34(positionMs)

    /** Start the already-prepared player at the requested sync position. Must be called on main. */
    fun startSyncPlayback(positionMs: Long) = this.__split_startSyncPlayback_35(positionMs)

    /** Seek absolut, dipakai synced lyrics supaya baris yang diketuk tepat ke timestamp API. */
    fun seekToPosition(positionMs: Long) = this.__split_seekToPosition_36(positionMs)

    internal fun pauseInternal() = this.__splitExtra_pauseInternal_37()

    internal fun fail(message: String) = this.__splitExtra_fail_38(message)


    internal val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
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

    internal fun requestFocus(): Boolean = this.__split_requestFocus_37()

    internal fun abandonFocus() = this.__split_abandonFocus_38()


    /**
     * loading = search / resolve / prepare.
     * Buffering (lag saat memutar) hanya lewat [LoadPhase] supaya halaman browse tidak ikut loading.
     */
    internal fun publishLoading() = this.__split_publishLoading_39()

    internal fun List<RexTrack>.replaceById(t: RexTrack) = map { if (it.id == t.id) t else it }

    internal const val SEARCH_DEBOUNCE_MS = 400L
    internal const val SEEK_DEBOUNCE_MS = 80L
    internal const val PROGRESS_INTERVAL_MS = 100L
    internal const val RESTART_THRESHOLD_MS = 3000L
    internal const val STALL_TICKS = 6
    internal const val MAX_HISTORY = 50
    internal const val MAX_QUEUE = 100
    internal const val MAX_REPEAT = 50

    internal const val TASTE_CHANCE = 0.65f        // peluang lagu berikutnya dari artis favorit
    internal const val TASTE_LISTEN_MS = 15_000L   // didengar segini lama = dihitung "suka"
    internal const val EARLY_SKIP_MS = 10_000L     // di-skip sebelum ini = dianggap kurang suka
    internal const val REPEAT_TASTE_WEIGHT = 0.5
    internal const val RECENT_WINDOW = 8           // hindari lagu yang baru diputar
    internal const val SMART_RETRIES = 3
}
