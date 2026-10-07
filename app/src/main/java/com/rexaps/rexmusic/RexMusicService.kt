package com.rexaps.rexmusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * RexMusic media notification.
 *
 * Important: RexMusic stays inside the RexAps APK, so Android will still show the
 * application header as "RexAps". What this service controls is the actual music
 * notification card: artwork, title, artist, lyric and transport controls.
 */
class RexMusicService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession
    private lateinit var nm: NotificationManager

    private var artJob: Job? = null
    private var coverUrl = ""
    private var coverBitmap: Bitmap? = null
    private var foreground = false
    private var closed = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                RexPlayerController.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        RexPlayerController.init(applicationContext)
        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannel()

        session = MediaSession(this, "RexMusic").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = RexPlayerController.play()
                override fun onPause() = RexPlayerController.pause()
                override fun onSkipToNext() = RexPlayerController.next()
                override fun onSkipToPrevious() = RexPlayerController.prev(force = true)
                override fun onStop() = shutdown()
                override fun onSeekTo(pos: Long) {
                    val d = RexPlayerController.state.value.durationMs
                    if (d > 0) RexPlayerController.seekTo(pos.toFloat() / d)
                }
            })
            isActive = true
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(noisyReceiver, filter)
        }

        startForegroundCompat(buildNotification(RexPlayerController.state.value))
        observeState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> RexPlayerController.togglePlay()
            ACTION_NEXT -> RexPlayerController.next()
            ACTION_PREV -> RexPlayerController.prev(force = true)
            ACTION_CLOSE -> shutdown()
            else -> if (!closed) {
                runCatching { startForegroundCompat(buildNotification(RexPlayerController.state.value)) }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        closed = true
        artJob?.cancel()
        scope.cancel()
        runCatching { unregisterReceiver(noisyReceiver) }
        runCatching { session.isActive = false }
        runCatching { session.release() }
        super.onDestroy()
    }

    private data class NotifKey(
        val id: String?,
        val title: String?,
        val artist: String?,
        val album: String?,
        val cover: String?,
        val playing: Boolean,
        val busy: Boolean,
        val duration: Long,
        val position: Long,
        val seekVersion: Int,
        val loadingText: String
    )

    private fun observeState() {
        scope.launch {
            RexPlayerController.state
                .map {
                    NotifKey(
                        id = it.nowPlaying?.id,
                        title = it.nowPlaying?.title,
                        artist = it.nowPlaying?.artist,
                        album = it.nowPlaying?.album,
                        cover = it.nowPlaying?.cover,
                        playing = it.isPlaying,
                        busy = it.phase.isPlayerBusy,
                        duration = it.durationMs,
                        position = it.positionMs,
                        seekVersion = it.seekVersion,
                        loadingText = it.loadingText
                    )
                }
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    private fun refresh() {
        if (closed) return
        val s = RexPlayerController.state.value
        loadCover(s.nowPlaying?.cover.orEmpty())
        updateSession(s)
        postNotification(s)
    }

    private fun loadCover(url: String) {
        if (url == coverUrl) return
        coverUrl = url
        coverBitmap = null
        artJob?.cancel()
        if (url.isBlank()) return

        artJob = scope.launch {
            val request = ImageRequest.Builder(applicationContext)
                .data(url)
                .size(512)
                .allowHardware(false)
                .build()
            val drawable = (applicationContext.imageLoader.execute(request) as? SuccessResult)?.drawable
            val bmp = runCatching { drawable?.toBitmap() }.getOrNull()
            if (bmp != null && coverUrl == url && !closed) {
                coverBitmap = bmp
                refresh()
            }
        }
    }

    private fun updateSession(s: RexMusicUiState) {
        val track = s.nowPlaying
        val meta = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track?.title ?: "RexMusic")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track?.artist.orEmpty())
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track?.album.orEmpty())
        if (s.durationMs > 0) meta.putLong(MediaMetadata.METADATA_KEY_DURATION, s.durationMs)
        coverBitmap?.let { meta.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) }
        session.setMetadata(meta.build())

        val code = when {
            s.phase.isPlayerBusy -> PlaybackState.STATE_BUFFERING
            s.isPlaying -> PlaybackState.STATE_PLAYING
            track != null -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_STOPPED
        }
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_STOP
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(code, s.positionMs, if (code == PlaybackState.STATE_PLAYING) 1f else 0f)
                .build()
        )
    }

    private fun postNotification(s: RexMusicUiState) {
        val n = buildNotification(s)
        val active = s.isPlaying || s.phase.isPlayerBusy
        if (active) {
            if (!foreground) {
                runCatching { startForegroundCompat(n) }
                    .onFailure { nm.notify(NOTIF_ID, n) }
            } else {
                nm.notify(NOTIF_ID, n)
            }
        } else {
            if (foreground) detachForeground()
            nm.notify(NOTIF_ID, n)
        }
    }

    private fun buildNotification(s: RexMusicUiState): Notification {
        val track = s.nowPlaying
        val busy = s.phase.isPlayerBusy
        val active = s.isPlaying || busy
        val views = RemoteViews(packageName, R.layout.notification_rexmusic_player).apply {
            setImageViewResource(R.id.rexmusic_notif_icon, android.R.drawable.ic_media_play)
            setTextViewText(R.id.rexmusic_notif_title, track?.title ?: "RexMusic")
            setTextViewText(
                R.id.rexmusic_notif_artist,
                when {
                    busy -> s.loadingText.ifBlank { "Memuat..." }
                    else -> track?.artist ?: "Siap memutar"
                }
            )
            setTextViewText(R.id.rexmusic_notif_lyric, currentLyricText(s))

            val duration = s.durationMs.coerceAtLeast(0L)
            val position = s.positionMs.coerceIn(0L, duration.coerceAtLeast(1L))
            val progress = if (duration > 0) ((position * 1000L) / duration).toInt() else 0
            setProgressBar(R.id.rexmusic_notif_progress, 1000, progress, false)
            setTextViewText(R.id.rexmusic_notif_time, formatTime(position))
            setTextViewText(R.id.rexmusic_notif_duration, formatTime(duration))
            setImageViewResource(
                R.id.rexmusic_notif_play,
                if (s.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            coverBitmap?.let { setImageViewBitmap(R.id.rexmusic_notif_cover, it) }
            setOnClickPendingIntent(R.id.rexmusic_notif_prev, servicePending(ACTION_PREV))
            setOnClickPendingIntent(R.id.rexmusic_notif_play, servicePending(ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.rexmusic_notif_next, servicePending(ACTION_NEXT))
            setOnClickPendingIntent(R.id.rexmusic_notif_close, servicePending(ACTION_CLOSE))
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setPriority(Notification.PRIORITY_LOW)
        }

        builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(active)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setDeleteIntent(servicePending(ACTION_CLOSE))
            .setContentTitle(track?.title ?: "RexMusic")
            .setContentText(track?.artist ?: "")

        contentIntent()?.let { builder.setContentIntent(it) }
        return builder.build()
    }

    /**
     * Uses the already parsed API/offline LyricLine timing. No timing is invented here.
     * The exact lyrics provider/model is owned by the player layer; this notification only
     * displays the current line when that information is available.
     */
    private fun currentLyricText(s: RexMusicUiState): String {
        val lines = s.lyrics.synced
        if (lines.isEmpty()) {
            return if (s.lyricsLoading) "♪ Memuat lirik..." else "♪ Lirik tidak tersedia"
        }

        // Same source of truth as the lyrics screen: the API/offline LyricLine timeMs.
        var lo = 0
        var hi = lines.lastIndex
        var active = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= s.positionMs) {
                active = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return active.takeIf { it >= 0 }?.let { "♪ ${lines[it].text}" } ?: "♪ ${lines.first().text}"
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val total = ms / 1000L
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    private fun servicePending(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, RexMusicService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun contentIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RexMusic",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Pemutar musik RexMusic"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    private fun startForegroundCompat(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }
        foreground = true
    }

    private fun detachForeground() {
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        foreground = false
    }

    private fun shutdown() {
        closed = true
        RexPlayerController.stop()
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foreground = false
        nm.cancel(NOTIF_ID)
        stopSelf()
    }

    companion object {
        private const val CHANNEL_ID = "rexmusic_playback_v2"
        private const val NOTIF_ID = 4711

        const val ACTION_START = "com.rexaps.rexmusic.action.START"
        const val ACTION_TOGGLE = "com.rexaps.rexmusic.action.TOGGLE"
        const val ACTION_NEXT = "com.rexaps.rexmusic.action.NEXT"
        const val ACTION_PREV = "com.rexaps.rexmusic.action.PREV"
        const val ACTION_CLOSE = "com.rexaps.rexmusic.action.CLOSE"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, RexMusicService::class.java).setAction(ACTION_START)
                )
            }
        }
    }
}
