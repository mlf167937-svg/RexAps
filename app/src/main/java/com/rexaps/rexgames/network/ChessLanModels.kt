package com.rexaps.rexgames.network

enum class ChessLanRole { HOST, CLIENT }
enum class ChessLanStatus { IDLE, HOSTING, CONNECTING, WAITING, CONNECTED, DISCONNECTED, ERROR }

data class ChessLanUiState(
    val status: ChessLanStatus = ChessLanStatus.IDLE,
    val role: ChessLanRole? = null,
    val hostAddress: String = "",
    val port: Int = ChessLanTransport.DEFAULT_PORT,
    val roomName: String = "",
    val peerName: String = "",
    val sessionId: String = "",
    val message: String = ""
)
