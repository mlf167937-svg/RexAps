package com.rexaps.rexgames.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/** Manual-IP TCP transport. It intentionally does not claim encryption; use it on a trusted LAN. */
internal class ChessLanTransport(
    private val scope: CoroutineScope,
    private val onState: (ChessLanUiState) -> Unit,
    private val onMessage: (JSONObject) -> Unit
) : Closeable {
    @Volatile private var server: ServerSocket? = null
    @Volatile private var socket: Socket? = null
    @Volatile private var activeSessionId: String = ""
    @Volatile private var closed = false
    private var worker: Job? = null
    private val writeLock = Any()
    private val secureRandom = SecureRandom()

    fun host(roomName: String, hostName: String, password: String) {
        stopWorker()
        closed = false
        worker = scope.launch(Dispatchers.IO) {
            var roomServer: ServerSocket? = null
            try {
                roomServer = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DEFAULT_PORT))
                }
                server = roomServer
                val address = localIpv4Address()
                onState(
                    ChessLanUiState(
                        status = ChessLanStatus.HOSTING,
                        role = ChessLanRole.HOST,
                        hostAddress = address,
                        port = DEFAULT_PORT,
                        roomName = roomName.trim().ifBlank { "Room Catur" },
                        message = if (address.isBlank()) "Room aktif; alamat IP belum dapat dideteksi. Masukkan IP perangkat secara manual." else "Menunggu lawan di jaringan lokal"
                    )
                )

                while (currentCoroutineContext().isActive && !closed) {
                    val peer = roomServer.accept()
                    try {
                        peer.tcpNoDelay = true
                        peer.keepAlive = true
                        peer.soTimeout = HANDSHAKE_TIMEOUT_MS
                        val sessionId = UUID.randomUUID().toString()
                        val nonce = randomNonce()
                        writeRaw(
                            peer,
                            JSONObject()
                                .put("type", "hello")
                                .put("protocolVersion", PROTOCOL_VERSION)
                                .put("sessionId", sessionId)
                                .put("roomName", roomName.trim().ifBlank { "Room Catur" })
                                .put("hostName", hostName.trim().ifBlank { "Host" })
                                .put("nonce", nonce)
                                .put("passwordRequired", password.isNotEmpty())
                        )
                        val join = readFrame(peer) ?: throw IOException("Permintaan bergabung kosong")
                        if (join.optString("type") != "join_request") throw IOException("Handshake tidak valid")
                        if (join.optInt("protocolVersion", -1) != PROTOCOL_VERSION) {
                            writeRaw(peer, JSONObject().put("type", "join_reject").put("reason", "Versi protocol tidak didukung"))
                            throw IOException("Versi protocol lawan tidak didukung")
                        }
                        val suppliedProof = join.optString("proof")
                        val expectedProof = passwordProof(nonce, password)
                        if (!constantTimeEquals(suppliedProof, expectedProof)) {
                            writeRaw(peer, JSONObject().put("type", "join_reject").put("reason", "Password room salah"))
                            throw IOException("Password room salah")
                        }

                        val peerName = join.optString("displayName").trim().take(32).ifBlank { "Pemain" }
                        activeSessionId = sessionId
                        socket = peer
                        peer.soTimeout = READ_TIMEOUT_MS
                        writeRaw(
                            peer,
                            JSONObject()
                                .put("type", "join_accept")
                                .put("protocolVersion", PROTOCOL_VERSION)
                                .put("sessionId", sessionId)
                                .put("hostName", hostName.trim().ifBlank { "Host" })
                                .put("peerName", peerName)
                                .put("roomName", roomName.trim().ifBlank { "Room Catur" })
                        )
                        onState(
                            ChessLanUiState(
                                status = ChessLanStatus.CONNECTED,
                                role = ChessLanRole.HOST,
                                hostAddress = address,
                                port = DEFAULT_PORT,
                                roomName = roomName.trim().ifBlank { "Room Catur" },
                                peerName = peerName,
                                sessionId = sessionId,
                                message = "Terhubung dengan $peerName"
                            )
                        )
                        readMessages(peer, sessionId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        runCatching { peer.close() }
                        if (!closed) {
                            socket = null
                            activeSessionId = ""
                            onState(
                                ChessLanUiState(
                                    status = ChessLanStatus.WAITING,
                                    role = ChessLanRole.HOST,
                                    hostAddress = localIpv4Address(),
                                    port = DEFAULT_PORT,
                                    roomName = roomName.trim().ifBlank { "Room Catur" },
                                    message = error.message ?: "Koneksi lawan berakhir; menunggu koneksi ulang"
                                )
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!closed) onState(
                    ChessLanUiState(
                        status = ChessLanStatus.ERROR,
                        role = ChessLanRole.HOST,
                        port = DEFAULT_PORT,
                        roomName = roomName,
                        message = error.message ?: "Tidak dapat membuka room LAN"
                    )
                )
            } finally {
                runCatching { roomServer?.close() }
                if (server === roomServer) server = null
                closePeer()
                if (closed) onState(ChessLanUiState())
            }
        }
    }

    fun join(address: String, port: Int, playerName: String, password: String) {
        stopWorker()
        closed = false
        worker = scope.launch(Dispatchers.IO) {
            var peer: Socket? = null
            var connectionEstablished = false
            try {
                require(address.isNotBlank()) { "Masukkan alamat IP host" }
                require(port in 1..65535) { "Port harus 1–65535" }
                onState(
                    ChessLanUiState(
                        status = ChessLanStatus.CONNECTING,
                        role = ChessLanRole.CLIENT,
                        hostAddress = address.trim(),
                        port = port,
                        message = "Menghubungkan ke host…"
                    )
                )
                peer = Socket().apply {
                    tcpNoDelay = true
                    keepAlive = true
                    connect(InetSocketAddress(address.trim(), port), CONNECT_TIMEOUT_MS)
                    soTimeout = HANDSHAKE_TIMEOUT_MS
                }
                val hello = readFrame(peer) ?: throw IOException("Host tidak mengirim handshake")
                if (hello.optString("type") != "hello") throw IOException("Respons host tidak valid")
                if (hello.optInt("protocolVersion", -1) != PROTOCOL_VERSION) throw IOException("Versi protocol host tidak didukung")
                val sessionId = hello.optString("sessionId").takeIf { it.isNotBlank() } ?: throw IOException("Session ID host tidak valid")
                val nonce = hello.optString("nonce").takeIf { it.isNotBlank() } ?: throw IOException("Challenge host tidak valid")
                writeRaw(
                    peer,
                    JSONObject()
                        .put("type", "join_request")
                        .put("protocolVersion", PROTOCOL_VERSION)
                        .put("displayName", playerName.trim().take(32).ifBlank { "Pemain" })
                        .put("proof", passwordProof(nonce, password))
                )
                val accepted = readFrame(peer) ?: throw IOException("Host tidak menjawab permintaan bergabung")
                if (accepted.optString("type") != "join_accept") {
                    throw IOException(accepted.optString("reason").ifBlank { "Host menolak koneksi" })
                }
                if (accepted.optInt("protocolVersion", -1) != PROTOCOL_VERSION || accepted.optString("sessionId") != sessionId) {
                    throw IOException("Handshake session tidak cocok")
                }
                activeSessionId = sessionId
                connectionEstablished = true
                socket = peer
                peer.soTimeout = READ_TIMEOUT_MS
                onState(
                    ChessLanUiState(
                        status = ChessLanStatus.CONNECTED,
                        role = ChessLanRole.CLIENT,
                        hostAddress = address.trim(),
                        port = port,
                        roomName = accepted.optString("roomName", hello.optString("roomName")),
                        peerName = accepted.optString("hostName", hello.optString("hostName")),
                        sessionId = sessionId,
                        message = "Terhubung dengan ${accepted.optString("hostName", "Host")}"
                    )
                )
                readMessages(peer, sessionId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!closed) onState(
                    ChessLanUiState(
                        status = if (connectionEstablished) ChessLanStatus.DISCONNECTED else ChessLanStatus.ERROR,
                        role = ChessLanRole.CLIENT,
                        hostAddress = address.trim(),
                        port = port,
                        message = if (connectionEstablished) "Koneksi ke host terputus. Bergabung kembali secara manual untuk sinkronisasi."
                            else (error.message ?: "Koneksi LAN gagal")
                    )
                )
            } finally {
                runCatching { peer?.close() }
                if (socket === peer) closePeer()
                if (closed) onState(ChessLanUiState())
            }
        }
    }

    fun send(type: String, fields: JSONObject = JSONObject()): Boolean {
        val peer = socket ?: return false
        val message = JSONObject()
            .put("type", type)
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("sessionId", activeSessionId)
            .put("requestId", UUID.randomUUID().toString())
        val keys = fields.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            message.put(key, fields.opt(key))
        }
        return runCatching { writeRaw(peer, message) }.isSuccess
    }

    private suspend fun readMessages(peer: Socket, sessionId: String) {
        var lastMessageAt = System.currentTimeMillis()
        val heartbeat = scope.launch(Dispatchers.IO) {
            while (isActive && peer.isConnected && !peer.isClosed) {
                delay(HEARTBEAT_INTERVAL_MS)
                val sent = runCatching {
                    writeRaw(
                        peer,
                        JSONObject()
                            .put("type", "heartbeat_ping")
                            .put("protocolVersion", PROTOCOL_VERSION)
                            .put("sessionId", sessionId)
                            .put("requestId", UUID.randomUUID().toString())
                    )
                }.isSuccess
                if (!sent) break
            }
        }
        try {
            while (currentCoroutineContext().isActive && !closed && !peer.isClosed) {
                val message = try {
                    readFrame(peer)
                } catch (_: SocketTimeoutException) {
                    if (System.currentTimeMillis() - lastMessageAt > HEARTBEAT_TIMEOUT_MS) {
                        throw IOException("Heartbeat lawan terputus")
                    }
                    continue
                } ?: throw IOException("Peer menutup koneksi")
                lastMessageAt = System.currentTimeMillis()
                if (message.optInt("protocolVersion", -1) != PROTOCOL_VERSION || message.optString("sessionId") != sessionId) {
                    throw IOException("Pesan protocol/session tidak valid")
                }
                when (message.optString("type")) {
                    "heartbeat_ping" -> send("heartbeat_pong")
                    "heartbeat_pong" -> Unit
                    else -> onMessage(message)
                }
            }
        } finally {
            heartbeat.cancel()
            if (socket === peer) {
                socket = null
                activeSessionId = ""
            }
            runCatching { peer.close() }
        }
    }

    private fun writeRaw(peer: Socket, json: JSONObject) {
        val payload = (json.toString() + "\n").toByteArray(Charsets.UTF_8)
        if (payload.size > MAX_FRAME_BYTES) throw IOException("Pesan LAN terlalu besar")
        synchronized(writeLock) {
            peer.getOutputStream().write(payload)
            peer.getOutputStream().flush()
        }
    }

    private fun readFrame(peer: Socket): JSONObject? {
        val output = ByteArrayOutputStream()
        val input = peer.getInputStream()
        while (true) {
            val byte = input.read()
            if (byte < 0) {
                if (output.size() == 0) return null
                throw IOException("Frame JSON terpotong")
            }
            if (byte == '\n'.code) break
            if (output.size() >= MAX_FRAME_BYTES) throw IOException("Frame LAN melebihi batas")
            if (byte != '\r'.code) output.write(byte)
        }
        return try {
            JSONObject(output.toByteArray().toString(Charsets.UTF_8))
        } catch (error: Exception) {
            throw IOException("JSON LAN tidak valid", error)
        }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun passwordProof(nonce: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$nonce|$password".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(left.toByteArray(Charsets.US_ASCII), right.toByteArray(Charsets.US_ASCII))

    private fun localIpv4Address(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLoopbackAddress && it.isSiteLocalAddress }
            .map { it.hostAddress.orEmpty() }
            .firstOrNull()
            .orEmpty()
    }.getOrDefault("")

    private fun stopWorker() {
        closed = true
        runCatching { socket?.close() }
        runCatching { server?.close() }
        socket = null
        server = null
        activeSessionId = ""
        worker?.cancel()
        worker = null
    }

    private fun closePeer() {
        runCatching { socket?.close() }
        socket = null
        activeSessionId = ""
    }

    override fun close() {
        if (socket != null) send("disconnect")
        closed = true
        stopWorker()
        onState(ChessLanUiState())
    }

    companion object {
        const val DEFAULT_PORT = 39873
        const val PROTOCOL_VERSION = 1
        private const val CONNECT_TIMEOUT_MS = 6_000
        private const val HANDSHAKE_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val HEARTBEAT_INTERVAL_MS = 4_000L
        private const val HEARTBEAT_TIMEOUT_MS = 18_000L
        private const val MAX_FRAME_BYTES = 64 * 1024
    }
}
