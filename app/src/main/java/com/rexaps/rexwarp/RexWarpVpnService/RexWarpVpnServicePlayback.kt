package com.rexaps.rexwarp

import android.net.*
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*

internal fun RexWarpVpnService.__split_pauseReason_11(): String? {
        val caps = physical.values.firstOrNull { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
            ?: physical.values.firstOrNull()
            ?: cm.activeNetwork?.let(cm::getNetworkCapabilities) ?: return null
        val s = settings
        return when {
            s.pauseOnWifi && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Paused on Wi-Fi"
            s.pauseOnMobile && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Paused on mobile data"
            s.pauseOnMetered && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> "Paused on metered network"
            else -> null
        }
    }
