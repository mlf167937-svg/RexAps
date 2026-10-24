package com.rexaps.rexwarp.ui.components

import android.os.SystemClock
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal fun testAddressFamily(label: String, address: InetAddress?): DiagnosticCheck {
    if (address == null) {
        return DiagnosticCheck(label, DiagnosticStatus.NOT_TESTED, "DNS tidak mengembalikan alamat untuk keluarga IP ini; konektivitas keluarga IP tidak dapat disimpulkan.")
    }
    val started = SystemClock.elapsedRealtime()
    return try {
        Socket().use { raw ->
            raw.connect(InetSocketAddress(address, 443), SOCKET_TIMEOUT_MS)
            raw.soTimeout = SOCKET_TIMEOUT_MS
            val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(raw, CHECK_HOST, 443, true) as SSLSocket
            ssl.use {
                it.soTimeout = SOCKET_TIMEOUT_MS
                val params = it.sslParameters
                params.endpointIdentificationAlgorithm = "HTTPS"
                it.sslParameters = params
                it.startHandshake()
            }
        }
        DiagnosticCheck(label, DiagnosticStatus.PASS, "TCP/TLS handshake ke alamat terpilih berhasil.", SystemClock.elapsedRealtime() - started)
    } catch (e: SocketTimeoutException) {
        DiagnosticCheck(label, DiagnosticStatus.FAIL, "Koneksi TLS melewati batas waktu ${SOCKET_TIMEOUT_MS} ms.", SystemClock.elapsedRealtime() - started)
    } catch (e: Exception) {
        DiagnosticCheck(label, DiagnosticStatus.FAIL, "Koneksi ke alamat terpilih gagal: ${safeMessage(e)}", SystemClock.elapsedRealtime() - started)
    }
}

internal fun safeMessage(error: Throwable): String = when (error) {
    is SocketTimeoutException -> "timeout"
    else -> error.localizedMessage.orEmpty().take(160).ifBlank { error.javaClass.simpleName }
        .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "[url]")
        .replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "[ip]")
}

internal const val TRACE_URL = "https://www.cloudflare.com/cdn-cgi/trace"
