package com.rexaps.rexmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import com.rexaps.rexmusic.LyricLine
import com.rexaps.rexmusic.RexMusicUiState
import com.rexaps.rexmusic.RexPlayerController

class RexMusicWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = this.__split_onUpdate_1(context, manager, ids)

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        val app = context.applicationContext
        RexPlayerController.init(app)
        updateIds(app, manager, intArrayOf(appWidgetId), RexPlayerController.state.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val app = context.applicationContext

        // RemoteViews lama bisa masih tersimpan oleh launcher setelah APK diperbarui.
        // Render ulang seluruh instance supaya action/text target mengikuti layout terbaru.
        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            RexPlayerController.init(app)
            updateAll(app, RexPlayerController.state.value, force = true)
            return
        }

        if (action !in CONTROL_ACTIONS) return
        RexPlayerController.init(app)
        when (action) {
            ACTION_TOGGLE -> RexPlayerController.togglePlay()
            ACTION_NEXT -> RexPlayerController.next()
            ACTION_PREV -> RexPlayerController.prev(force = true)
            ACTION_OPEN -> openApp(app)
            ACTION_REFRESH -> Unit
        }
        updateAll(app, RexPlayerController.state.value, force = true)
    }

    internal data class Lyr(val cur: String, val next: String)

    companion object {
        const val ACTION_TOGGLE = "com.rexaps.rexmusic.widget.TOGGLE"
        const val ACTION_NEXT = "com.rexaps.rexmusic.widget.NEXT"
        const val ACTION_PREV = "com.rexaps.rexmusic.widget.PREV"
        const val ACTION_OPEN = "com.rexaps.rexmusic.widget.OPEN"
        const val ACTION_REFRESH = "com.rexaps.rexmusic.widget.REFRESH"

        private val CONTROL_ACTIONS =
            setOf(ACTION_TOGGLE, ACTION_NEXT, ACTION_PREV, ACTION_OPEN, ACTION_REFRESH)

        internal const val WIDE_MIN_DP = 372
        internal const val PROGRESS_STEP_MS = 1_000L

        internal const val DEFAULT_ACCENT = 0xFFA99BFF.toInt()
        internal val COLOR_ONLINE = Color.rgb(120, 200, 255)
        internal val COLOR_OFFLINE = Color.rgb(110, 235, 180)
        internal val COLOR_MUTED = Color.rgb(200, 200, 215)
        internal const val ICON_DARK = 0xFF121212.toInt()

        internal const val RC_OPEN = 10
        internal const val RC_PREV = 11
        internal const val RC_TOGGLE = 12
        internal const val RC_NEXT = 13

        internal var lastSignature = ""
        internal var lastPositionMs = -1L

        fun updateAll(
            context: Context,
            state: RexMusicUiState,
            cover: Bitmap? = null,
            coverUrl: String? = null,
            force: Boolean = false
        ) = this.__split_updateAll_1(context, state, cover, coverUrl, force)

        internal fun updateIds(
            context: Context, manager: AppWidgetManager, ids: IntArray, state: RexMusicUiState
        ) = this.__split_updateIds_2(context, manager, ids, state)

        internal fun renderInto(
            context: Context, manager: AppWidgetManager, ids: IntArray,
            state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr
        ) = this.__split_renderInto_3(context, manager, ids, state, art, lyr)

        // ---------- throttle ----------

        internal fun signature(state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr): String = this.__split_signature_4(state, art, lyr)

        internal fun shouldRender(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, force: Boolean
        ): Boolean = this.__split_shouldRender_5(context, state, art, lyr, force)

        internal fun isWide(context: Context, manager: AppWidgetManager, id: Int): Boolean = this.__split_isWide_6(context, manager, id)

        // ---------- render ----------

        internal fun render(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, wide: Boolean
        ): RemoteViews = this.__split_render_7(context, state, art, lyr, wide)

        internal fun sourceLabel(state: RexMusicUiState, wide: Boolean): Pair<String, Int> = this.__split_sourceLabel_8(state, wide)

        internal fun lyricLines(state: RexMusicUiState): Lyr = this.__split_lyricLines_9(state)

        internal fun activeIndex(list: List<LyricLine>, positionMs: Long): Int = this.__split_activeIndex_10(list, positionMs)

        internal fun formatTime(ms: Long): String = this.__split_formatTime_11(ms)

        // ---------- intents ----------

        internal fun broadcast(context: Context, action: String, request: Int): PendingIntent = this.__split_broadcast_12(context, action, request)

        internal fun openAppIntent(context: Context): PendingIntent? = this.__split_openAppIntent_13(context)

        private fun openApp(context: Context) = this.__split_openApp_14(context)
    }
}
