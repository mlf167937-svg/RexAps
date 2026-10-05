package com.rexaps.rexwarp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.rexaps.R
import com.rexaps.rexwarp.model.RexWarpConnectionInfo
import com.rexaps.rexwarp.tunnel.RexWarpTunnelStats
import java.util.Locale

/** Riwayat kecepatan (byte/detik) untuk grafik mini di notifikasi. Elemen terakhir = terbaru. */
class RexWarpSpeedHistory(val down: List<Long>, val up: List<Long>)

object RexWarpNotification {
    const val CHANNEL_ID = "rexwarp_status"
    const val ID = 0x7257

    private const val NAVY = 0xFF0A1226.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLUE = 0xFF2D8CFF.toInt()
    private const val BLUE_LINE = 0xFF2E9BFF.toInt()
    private const val PURPLE_LINE = 0xFF8A5CFF.toInt()

    private const val GRAPH_POINTS = 40
    private const val GRAPH_W = 300
    private const val GRAPH_H = 56
    private const val MIN_PEAK = 125_000f // 1 Mb/s: grafik tidak "meledak" saat trafik kecil

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "RexWARP status", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Status dan kecepatan real-time RexWARP"; setShowBadge(false) }
            )
        }
    }

    /** Nilai Mb/s (megabit per detik) tanpa satuan, mis. "12.34". */
    private fun mbitValue(bytesPerSec: Long): String {
        val m = bytesPerSec.coerceAtLeast(0) * 8 / 1_000_000.0
        return when {
            m < 10 -> String.format(Locale.US, "%.2f", m)
            m < 100 -> String.format(Locale.US, "%.1f", m)
            else -> String.format(Locale.US, "%.0f", m)
        }
    }

    fun build(
        ctx: Context,
        connection: RexWarpConnectionState,
        speed: RexWarpSpeed?,
        sessionStartWallMs: Long?,
        pausedReason: String?,
        stats: RexWarpTunnelStats? = null,
        info: RexWarpConnectionInfo? = null,
        history: RexWarpSpeedHistory? = null
    ): Notification {
        val open = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
            PendingIntent.getActivity(ctx, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val disconnect = PendingIntent.getService(
            ctx, 1, Intent(ctx, RexWarpVpnService::class.java).setAction(RexWarpVpnService.ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE
        )

        val b = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rexwarp_notification)
            .setColor(NAVY).setColorized(true)
            .setOngoing(connection != RexWarpConnectionState.ERROR && connection != RexWarpConnectionState.DISCONNECTED)
            .setAutoCancel(connection == RexWarpConnectionState.ERROR)
            .setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)

        if (connection == RexWarpConnectionState.CONNECTED && speed != null) {
            val dlText = mbitValue(speed.downBytesPerSec)
            val ulText = mbitValue(speed.upBytesPerSec)
            val server = info?.server?.name?.substringAfter("· ", "")?.trim()?.takeIf { it.isNotEmpty() } ?: "Auto"

            // ---- tampilan ringkas ----
            val small = RemoteViews(ctx.packageName, R.layout.notif_rexwarp_small)
            small.setTextViewText(R.id.nt_s_speed, "↓ $dlText Mb/s     ↑ $ulText Mb/s")
            small.setTextViewText(R.id.nt_s_status, "Terhubung dengan server")

            // ---- tampilan lengkap (seperti desain) ----
            val big = RemoteViews(ctx.packageName, R.layout.notif_rexwarp_big)
            big.setTextViewText(R.id.nt_title, brandTitle())
            big.setTextViewText(R.id.nt_status, "Terhubung dengan server")
            big.setTextViewText(R.id.nt_dl_value, dlText)
            big.setTextViewText(R.id.nt_ul_value, ulText)
            big.setTextViewText(R.id.nt_server, server)

            val dlBmp = sparkline(history?.down.orEmpty(), BLUE_LINE)
            val ulBmp = sparkline(history?.up.orEmpty(), PURPLE_LINE)
            big.setImageViewBitmap(R.id.nt_dl_graph, dlBmp)
            big.setImageViewBitmap(R.id.nt_ul_graph, ulBmp)

            // Timer berjalan sendiri di sisi sistem (tidak perlu update tiap detik).
            val base = if (sessionStartWallMs != null)
                SystemClock.elapsedRealtime() - (System.currentTimeMillis() - sessionStartWallMs)
            else SystemClock.elapsedRealtime()
            big.setChronometer(R.id.nt_timer, base, null, true)

            b.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(small)
                .setCustomBigContentView(big)
                .setContentTitle("RexWARP")
                .setContentText("↓ $dlText Mb/s   ↑ $ulText Mb/s")
        } else {
            val status = pausedReason ?: when (connection) {
                RexWarpConnectionState.CONNECTED -> "Terhubung"
                RexWarpConnectionState.CONNECTING -> "Menghubungkan..."
                RexWarpConnectionState.DISCONNECTING -> "Memutuskan..."
                RexWarpConnectionState.DISCONNECTED -> "Terputus"
                RexWarpConnectionState.ERROR -> "Error"
            }
            b.setContentTitle("RexWARP").setContentText(status)
        }

        if (connection != RexWarpConnectionState.DISCONNECTING) b.addAction(0, "Putuskan", disconnect)
        return b.build()
    }

    /** "Rex" putih + "WARP" biru, seperti logo. */
    private fun brandTitle(): CharSequence = SpannableString("RexWARP").apply {
        setSpan(ForegroundColorSpan(WHITE), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(ForegroundColorSpan(BLUE), 3, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** Grafik garis halus dengan isi gradasi. Bitmap kecil (300x56) agar ringan dikirim ke SystemUI. */
    private fun sparkline(values: List<Long>, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(GRAPH_W, GRAPH_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val padded = if (values.size >= GRAPH_POINTS) values.takeLast(GRAPH_POINTS)
        else List(GRAPH_POINTS - values.size) { 0L } + values
        val peak = maxOf(padded.maxOrNull()?.toFloat() ?: 0f, MIN_PEAK)

        val stepX = GRAPH_W.toFloat() / (GRAPH_POINTS - 1)
        val xs = FloatArray(GRAPH_POINTS) { it * stepX }
        val ys = FloatArray(GRAPH_POINTS) { i ->
            GRAPH_H - 4f - (padded[i] / peak) * (GRAPH_H - 10f)
        }

        val line = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until GRAPH_POINTS) {
                val mid = (xs[i - 1] + xs[i]) / 2f
                cubicTo(mid, ys[i - 1], mid, ys[i], xs[i], ys[i])
            }
        }
        val fill = Path(line).apply {
            lineTo(GRAPH_W.toFloat(), GRAPH_H.toFloat())
            lineTo(0f, GRAPH_H.toFloat())
            close()
        }

        val top = (color and 0x00FFFFFF) or (0x78 shl 24)
        val bottom = color and 0x00FFFFFF
        canvas.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, 0f, 0f, GRAPH_H.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        })
        canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
        })
        return bmp
    }
}
