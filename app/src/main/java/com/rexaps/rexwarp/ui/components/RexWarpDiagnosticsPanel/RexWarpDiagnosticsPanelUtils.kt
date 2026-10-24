package com.rexaps.rexwarp.ui.components

import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal suspend fun runNetworkChecks(tunnelActive: Boolean): List<DiagnosticCheck> {
    val checks = mutableListOf<DiagnosticCheck>()
    val dnsStarted = SystemClock.elapsedRealtime()
    val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "RexWarp-DNS-check").apply { isDaemon = true } }
    val addresses = try {
        val future = executor.submit(Callable { InetAddress.getAllByName(CHECK_HOST).toList() })
        try {
            future.get(DNS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            future.cancel(true)
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    } finally {
        executor.shutdownNow()
    }
    val dnsDuration = SystemClock.elapsedRealtime() - dnsStarted
    checks += if (addresses.isNotEmpty()) {
        DiagnosticCheck("DNS lookup ($CHECK_HOST)", DiagnosticStatus.PASS, "Resolver mengembalikan ${addresses.size} alamat.", dnsDuration)
    } else {
        DiagnosticCheck("DNS lookup ($CHECK_HOST)", DiagnosticStatus.FAIL, "Tidak ada alamat yang diperoleh dalam batas waktu ${DNS_TIMEOUT_MS} ms.", dnsDuration)
    }

    currentCoroutineContext().ensureActive()
    val httpsStarted = SystemClock.elapsedRealtime()
    var traceWarp: String? = null
    val httpsDetail = try {
        val connection = URL(TRACE_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = SOCKET_TIMEOUT_MS
        connection.readTimeout = SOCKET_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.useCaches = false
        try {
            val code = connection.responseCode
            if (code !in 200..299) "Endpoint HTTPS membalas HTTP $code."
            else {
                val body = connection.inputStream.bufferedReader().use { it.readText().take(12_000) }
                traceWarp = body.lineSequence().firstOrNull { it.startsWith("warp=") }?.substringAfter('=')
                if (tunnelActive && traceWarp !in setOf("on", "plus")) {
                    "HTTPS berhasil, tetapi trace tidak mengonfirmasi warp=on/plus (nilai=${traceWarp ?: "tidak tersedia"})."
                } else {
                    "HTTPS berhasil (HTTP $code)${traceWarp?.let { "; trace warp=$it" }.orEmpty()}."
                }
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        "Permintaan HTTPS gagal: ${safeMessage(e)}"
    }
    val httpsDuration = SystemClock.elapsedRealtime() - httpsStarted
    val httpsPassed = httpsDetail.startsWith("HTTPS berhasil") && (!tunnelActive || traceWarp in setOf("on", "plus"))
    checks += DiagnosticCheck(
        "HTTPS ke Cloudflare trace",
        if (httpsPassed) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
        httpsDetail,
        httpsDuration
    )

    currentCoroutineContext().ensureActive()
    checks += testAddressFamily("IPv4 HTTPS/TLS", addresses.filterIsInstance<Inet4Address>().firstOrNull())
    currentCoroutineContext().ensureActive()
    checks += testAddressFamily("IPv6 HTTPS/TLS", addresses.filterIsInstance<Inet6Address>().firstOrNull())
    checks += DiagnosticCheck(
        "Packet loss / ICMP ping",
        DiagnosticStatus.NOT_TESTED,
        "Tidak diukur. Pemeriksaan ini memakai DNS dan HTTPS/TLS, bukan ICMP ping atau pengukuran packet loss."
    )
    return checks
}

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
