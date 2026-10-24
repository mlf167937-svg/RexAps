package com.rexaps.rexmusic.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Build
import android.os.PowerManager
import android.widget.RemoteViews
import com.rexaps.R
import com.rexaps.rexmusic.LoadPhase
import com.rexaps.rexmusic.LyricLine
import com.rexaps.rexmusic.RexMusicUiState
import com.rexaps.rexmusic.isPlayerBusy
import kotlin.math.abs

internal fun RexMusicWidget.Companion.__split_updateAll_1(
            context: Context,
            state: RexMusicUiState,
            cover: Bitmap? = null,
            coverUrl: String? = null,
            force: Boolean = false
        ) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, RexMusicWidget::class.java))
            if (ids.isEmpty()) return

            if (cover != null) WidgetArt.put(coverUrl ?: state.nowPlaying?.cover.orEmpty(), cover)
            val art = WidgetArt.forTrack(state.nowPlaying?.cover)
            val lyr = lyricLines(state)
            if (!shouldRender(app, state, art, lyr, force)) return
            renderInto(app, manager, ids, state, art, lyr)
        }

internal fun RexMusicWidget.Companion.__split_updateIds_2(
            context: Context, manager: AppWidgetManager, ids: IntArray, state: RexMusicUiState
        ) {
            val art = WidgetArt.forTrack(state.nowPlaying?.cover)
            val lyr = lyricLines(state)
            lastSignature = signature(state, art, lyr)
            lastPositionMs = state.positionMs
            renderInto(context, manager, ids, state, art, lyr)
        }

internal fun RexMusicWidget.Companion.__split_renderInto_3(
            context: Context, manager: AppWidgetManager, ids: IntArray,
            state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr
        ) {
            val built = HashMap<Boolean, RemoteViews>(2)
            for (id in ids) {
                val wide = isWide(context, manager, id)
                val views = built.getOrPut(wide) { render(context, state, art, lyr, wide) }
                runCatching { manager.updateAppWidget(id, views) }
            }
        }

internal fun RexMusicWidget.Companion.__split_signature_4(state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr): String {
            val t = state.nowPlaying
            return listOf(
                t?.id.orEmpty(), t?.title.orEmpty(), t?.artist.orEmpty(),
                (art != null).toString(), state.isPlaying.toString(),
                state.nowPlayingOffline.toString(), state.phase.name,
                (state.durationMs / 1000L).toString(),
                lyr.cur, lyr.next
            ).joinToString("|")
        }

internal fun RexMusicWidget.Companion.__split_shouldRender_5(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, force: Boolean
        ): Boolean {
            val sig = signature(state, art, lyr)
            val pos = state.positionMs
            if (force || sig != lastSignature) {
                lastSignature = sig
                lastPositionMs = pos
                return true
            }
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isInteractive) return false
            if (abs(pos - lastPositionMs) < PROGRESS_STEP_MS) return false
            lastPositionMs = pos
            return true
        }

internal fun RexMusicWidget.Companion.__split_render_7(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, wide: Boolean
        ): RemoteViews {
            val views = RemoteViews(
                context.packageName,
                if (wide) R.layout.widget_rexmusic_player_wide else R.layout.widget_rexmusic_player
            )
            val track = state.nowPlaying
            val busy = track != null && state.phase.isPlayerBusy
            val accent = art?.accent ?: DEFAULT_ACCENT
            val title = track?.title?.takeIf { it.isNotBlank() } ?: "RexMusic"
            val artist = when {
                track == null -> "Pilih lagu untuk mulai"
                track.artist.isBlank() -> "Artis tidak diketahui"
                else -> track.artist
            }
            val source = sourceLabel(state, wide)

            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(R.id.widget_track_artist, artist)
            views.setTextViewText(R.id.widget_source, source.first)
            views.setTextColor(R.id.widget_source, source.second)

            views.setTextViewText(R.id.widget_lyrics, lyr.cur.ifBlank { "♪" })
            views.setTextColor(R.id.widget_lyrics, accent)
            if (wide) views.setTextViewText(R.id.widget_lyrics_next, lyr.next)

            if (track != null && art != null) {
                views.setImageViewBitmap(R.id.widget_cover, art.cover)
                views.setImageViewBitmap(R.id.widget_bg, if (wide) art.bgWide else art.bgCompact)
            } else {
                views.setImageViewResource(R.id.widget_cover, R.drawable.rexw_cover_placeholder)
                views.setImageViewResource(R.id.widget_bg, R.drawable.rexw_bg)
            }

            views.setTextViewText(R.id.widget_time_current, formatTime(state.positionMs))
            views.setTextViewText(R.id.widget_time_total, formatTime(state.durationMs))

            views.setImageViewResource(
                R.id.widget_play_pause,
                if (state.isPlaying) R.drawable.rexw_ic_pause else R.drawable.rexw_ic_play
            )
            views.setInt(R.id.widget_play_pause, "setColorFilter", ICON_DARK)
            views.setInt(R.id.widget_play_pause, "setImageAlpha", if (busy) 140 else 255)
            views.setContentDescription(
                R.id.widget_play_pause, if (state.isPlaying) "Jeda" else "Putar"
            )

            val progress = if (state.durationMs > 0L) {
                ((state.positionMs * 1000L) / state.durationMs).toInt().coerceIn(0, 1000)
            } else 0
            views.setProgressBar(R.id.widget_progress, 1000, progress, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(
                    R.id.widget_progress, "setProgressTintList", ColorStateList.valueOf(accent)
                )
            }

            val open = openAppIntent(context)
            if (open != null) views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_prev, broadcast(context, ACTION_PREV, RC_PREV))
            views.setOnClickPendingIntent(R.id.widget_next, broadcast(context, ACTION_NEXT, RC_NEXT))
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                if (track == null && open != null) open
                else broadcast(context, ACTION_TOGGLE, RC_TOGGLE)
            )

            views.setContentDescription(
                R.id.widget_root,
                if (track == null) "RexMusic. Belum ada lagu. Ketuk untuk membuka aplikasi"
                else "$title, $artist. ${source.first.removePrefix("● ").lowercase()}. " +
                    "Ketuk untuk membuka RexMusic"
            )
            return views
        }

internal fun RexMusicWidget.Companion.__split_sourceLabel_8(state: RexMusicUiState, wide: Boolean): Pair<String, Int> {
            val track = state.nowPlaying ?: return "SIAP" to COLOR_MUTED
            if (track.title.isBlank() && track.artist.isBlank()) return "SIAP" to COLOR_MUTED
            if (state.phase.isPlayerBusy) {
                val text = when (state.phase) {
                    LoadPhase.Buffering -> "BUFFERING"
                    LoadPhase.Preparing -> "MEMUAT"
                    else -> "MENYIAPKAN"
                }
                return text to COLOR_MUTED
            }
            return if (state.nowPlayingOffline) {
                (if (wide) "● OFFLINE · LOKAL" else "● OFFLINE") to COLOR_OFFLINE
            } else {
                (if (wide) "● ONLINE · STREAM" else "● ONLINE") to COLOR_ONLINE
            }
        }

internal fun RexMusicWidget.Companion.__split_lyricLines_9(state: RexMusicUiState): Lyr {
            if (state.nowPlaying == null) return Lyr("", "")
            val lyrics = state.lyrics
            return when {
                state.lyricsLoading -> Lyr("Memuat lirik…", "")
                lyrics.hasSynced -> {
                    val i = activeIndex(lyrics.synced, state.positionMs)
                    val cur = if (i >= 0) lyrics.synced[i].text.trim() else ""
                    val nxt = lyrics.synced.getOrNull(i + 1)?.text?.trim().orEmpty()
                    Lyr(if (cur.isBlank()) "♪" else "♪  $cur", nxt)
                }
                !lyrics.isEmpty -> Lyr("Lirik tanpa sinkronisasi · buka aplikasi", "")
                else -> Lyr("Lirik tidak tersedia", "")
            }
        }

internal fun RexMusicWidget.Companion.__split_activeIndex_10(list: List<LyricLine>, positionMs: Long): Int {
            var lo = 0
            var hi = list.lastIndex
            var result = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (list[mid].timeMs <= positionMs) { result = mid; lo = mid + 1 } else hi = mid - 1
            }
            return result
        }
