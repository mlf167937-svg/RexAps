package com.rexaps.rexmonitor.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.telephony.TelephonyManager

data class RexNetworkInfo(
    val isConnected: Boolean? = null,
    /** true = internet tervalidasi, false = tersambung tapi terbatas, null = tidak diketahui. */
    val validated: Boolean? = null,
    val wifiConnected: Boolean = false,
    val wifiRssiDbm: Int? = null,
    val cellularConnected: Boolean = false,
    val cellularType: String? = null,
    val operatorName: String? = null,
    val cellularSignalDbm: Int? = null,
    val downloadBps: Double? = null,
    val uploadBps: Double? = null,
    /** Traffic berasal dari TrafficStats seluruh perangkat, BUKAN traffic tunnel RexWARP. */
    val trafficScope: String = "Device Network"
)

class RexNetworkReader(context: Context) {
    private val app = context.applicationContext
    private val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifi = app.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val tm = app.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private var prevRx = -1L
    private var prevTx = -1L
    private var prevAt = 0L

    @Suppress("DEPRECATION")
    fun read(): RexNetworkInfo {
        val (down, up) = sampleSpeed()
        val manager = cm ?: return RexNetworkInfo(downloadBps = down, uploadBps = up)

        var wifiUp = false
        var cellUp = false
        var cellDbm: Int? = null
        for (n in manager.allNetworks) {
            val caps = manager.getNetworkCapabilities(n) ?: continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifiUp = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                cellUp = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val s = caps.signalStrength
                    if (s != Int.MIN_VALUE) cellDbm = s
                }
            }
        }
        val active = manager.activeNetwork
        val activeCaps = active?.let { manager.getNetworkCapabilities(it) }
        val rssi = if (wifiUp) {
            runCatching { wifi?.connectionInfo?.rssi }.getOrNull()?.takeIf { it in -126..-1 }
        } else null

        return RexNetworkInfo(
            isConnected = active != null && activeCaps != null,
            validated = activeCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            wifiConnected = wifiUp,
            wifiRssiDbm = rssi,
            cellularConnected = cellUp,
            cellularType = if (cellUp) cellularType() else null,
            operatorName = if (cellUp) tm?.networkOperatorName?.takeIf { it.isNotBlank() } else null,
            cellularSignalDbm = cellDbm,
            downloadBps = down,
            uploadBps = up
        )
    }

    private fun sampleSpeed(): Pair<Double?, Double?> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
            return null to null
        }
        val now = SystemClock.elapsedRealtime()
        var down: Double? = null
        var up: Double? = null
        if (prevRx >= 0 && now > prevAt) {
            val seconds = (now - prevAt) / 1000.0
            down = ((rx - prevRx).coerceAtLeast(0) * 8.0) / seconds
            up = ((tx - prevTx).coerceAtLeast(0) * 8.0) / seconds
        }
        prevRx = rx
        prevTx = tx
        prevAt = now
        return down to up
    }

    /** Pada Android 11+ butuh READ_PHONE_STATE; bila ditolak sistem hasilnya null (tidak tersedia). */
    private fun cellularType(): String? = try {
        when (tm?.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
            else -> null
        }
    } catch (_: SecurityException) {
        null
    }
}
