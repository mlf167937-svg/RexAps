package com.rexaps.rexmusic

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Buka koneksi dan ikuti redirect sendiri (termasuk http -> https). */
    internal fun RexOfflineManager.__split_openConnection_14(url: String): HttpURLConnection {
        var current = url
        for (i in 0 until 6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", USER_AGENT)
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) throw IOException("redirect tanpa tujuan")
                current = URL(URL(current), location).toString()
                continue
            }
            return conn
        }
        throw IOException("terlalu banyak redirect")
    }
