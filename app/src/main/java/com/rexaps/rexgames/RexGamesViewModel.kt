package com.rexaps.rexgames

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rexgames.catur.bot.AlphaBetaChessBot
import com.rexaps.rexgames.catur.bot.ChessDifficulty
import com.rexaps.rexgames.catur.engine.ChessColor
import com.rexaps.rexgames.catur.engine.ChessGameState
import com.rexaps.rexgames.catur.engine.ChessGameStatus
import com.rexaps.rexgames.catur.engine.ChessFen
import com.rexaps.rexgames.catur.engine.ChessMove
import com.rexaps.rexgames.catur.engine.ChessPieceType
import com.rexaps.rexgames.catur.engine.ChessRules
import com.rexaps.rexgames.catur.engine.PgnManager
import com.rexaps.rexgames.catur.engine.parseSquare
import com.rexaps.rexgames.data.ChessHistoryStore
import com.rexaps.rexgames.data.ChessMatchSummary
import com.rexaps.rexgames.data.ChessSettingsStore
import com.rexaps.rexgames.network.ChessLanRole
import com.rexaps.rexgames.network.ChessLanStatus
import com.rexaps.rexgames.network.ChessLanTransport
import com.rexaps.rexgames.network.ChessLanUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

enum class ChessPlayMode { SOLO_BOT, PASS_AND_PLAY, LAN_HOST, LAN_CLIENT }

data class RexGamesUiState(
    val game: ChessGameState? = null,
    val mode: ChessPlayMode? = null,
    val difficulty: ChessDifficulty = ChessDifficulty.MEDIUM,
    val whiteName: String = "Pemain Putih",
    val blackName: String = "Pemain Hitam",
    val thinking: Boolean = false,
    val playerColor: ChessColor = ChessColor.WHITE,
    val error: String? = null,
    val selectedSquare: Int? = null,
    val legalTargets: List<ChessMove> = emptyList(),
    val promotionFrom: Int? = null,
    val promotionTo: Int? = null,
    val boardFlipped: Boolean = false,
    val matchId: String = UUID.randomUUID().toString(),
    val recentMatches: List<ChessMatchSummary> = emptyList(),
    val lan: ChessLanUiState = ChessLanUiState(),
    val revision: Long = 0L
)

class RexGamesViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = ChessSettingsStore(application)
    private val history = ChessHistoryStore(application)
    private val bot = AlphaBetaChessBot()
    private var botJob: Job? = null
    private var lanTransport: ChessLanTransport? = null
    private var lanGeneration = 0L
    private val handledLanRequests = LinkedHashSet<String>()
    private val _state = MutableStateFlow(
        RexGamesUiState(difficulty = settings.difficulty(), recentMatches = history.load())
    )
    val state: StateFlow<RexGamesUiState> = _state.asStateFlow()

    fun setDifficulty(difficulty: ChessDifficulty) {
        settings.saveDifficulty(difficulty)
        _state.value = _state.value.copy(difficulty = difficulty)
    }

    fun startSolo(playerName: String) {
        closeLanTransport()
        botJob?.cancel()
        val whiteName = playerName.trim().ifBlank { "Pemain" }
        _state.value = RexGamesUiState(
            game = ChessGameState(), mode = ChessPlayMode.SOLO_BOT,
            difficulty = settings.difficulty(), whiteName = whiteName, blackName = "Rex Bot",
            playerColor = ChessColor.WHITE,
            recentMatches = history.load()
        )
    }

    fun startPassAndPlay(firstName: String, secondName: String) {
        closeLanTransport()
        botJob?.cancel()
        // Assignment happens once per match creation and is stored in the ViewModel state.
        val firstColor = if (Random.nextBoolean()) ChessColor.WHITE else ChessColor.BLACK
        val first = firstName.trim().ifBlank { "Pemain 1" }
        val second = secondName.trim().ifBlank { "Pemain 2" }
        val whiteName = if (firstColor == ChessColor.WHITE) first else second
        val blackName = if (firstColor == ChessColor.BLACK) first else second
        _state.value = RexGamesUiState(
            game = ChessGameState(), mode = ChessPlayMode.PASS_AND_PLAY,
            whiteName = whiteName, blackName = blackName,
            playerColor = firstColor,
            recentMatches = history.load()
        )
    }

    fun selectSquare(square: Int) {
        val current = _state.value
        val game = current.game ?: return
        if (game.isFinished || current.thinking || square !in 0..63) return
        if (current.mode == ChessPlayMode.SOLO_BOT && game.position.sideToMove != current.playerColor) return
        if (current.mode == ChessPlayMode.LAN_HOST && game.position.sideToMove != ChessColor.WHITE) return
        if (current.mode == ChessPlayMode.LAN_CLIENT && game.position.sideToMove != ChessColor.BLACK) return
        val selected = current.selectedSquare
        if (selected != null) {
            val destinations = current.legalTargets.filter { it.to == square }
            if (destinations.isNotEmpty()) {
                if (destinations.any { it.promotion != null }) {
                    _state.value = current.copy(
                        selectedSquare = null,
                        promotionFrom = selected,
                        promotionTo = square
                    )
                } else {
                    makeMove(destinations.first())
                }
                return
            }
        }
        val piece = game.position.board[square]
        if (piece?.color == game.position.sideToMove) {
            _state.value = current.copy(
                selectedSquare = square,
                legalTargets = ChessRules.legalMovesFrom(game.position, square),
                promotionFrom = null,
                promotionTo = null
            )
        } else {
            _state.value = current.copy(selectedSquare = null, legalTargets = emptyList())
        }
    }

    fun makeMove(move: ChessMove) {
        val current = _state.value
        val game = current.game ?: return
        if (current.thinking || game.isFinished) return
        if (current.mode == ChessPlayMode.LAN_CLIENT) {
            if (game.position.sideToMove != ChessColor.BLACK) return
            if (lanTransport?.send(
                    "move_request",
                    JSONObject().put("revision", current.revision).put("uci", move.toUci())
                ) != true
            ) _state.value = current.copy(error = "Koneksi LAN tidak tersedia")
            return
        }
        if (current.mode == ChessPlayMode.LAN_HOST && game.position.sideToMove != ChessColor.WHITE) return
        val result = ChessRules.play(game, move)
        val next = result.getOrNull()
        if (next == null) {
            _state.value = current.copy(
                error = result.exceptionOrNull()?.message ?: "Langkah tidak legal",
                selectedSquare = null, legalTargets = emptyList(), promotionFrom = null, promotionTo = null
            )
            return
        }
        _state.value = current.copy(
            game = next, selectedSquare = null, legalTargets = emptyList(),
            promotionFrom = null, promotionTo = null, error = null,
            boardFlipped = when (current.mode) {
                ChessPlayMode.PASS_AND_PLAY -> next.position.sideToMove == ChessColor.BLACK
                ChessPlayMode.LAN_CLIENT -> true
                else -> current.boardFlipped
            },
            revision = if (current.mode == ChessPlayMode.LAN_HOST) current.revision + 1 else current.revision
        )
        if (current.mode == ChessPlayMode.LAN_HOST) publishLanSnapshot()
        if (next.isFinished) persistFinished(current, next)
        if (current.mode == ChessPlayMode.SOLO_BOT && !next.isFinished && next.position.sideToMove != current.playerColor) {
            runBotMove(next)
        }
    }

    fun choosePromotion(type: ChessPieceType) {
        val current = _state.value
        val from = current.promotionFrom ?: return
        val to = current.promotionTo ?: return
        makeMove(ChessMove(from, to, type))
    }

    fun dismissPromotion() {
        _state.value = _state.value.copy(promotionFrom = null, promotionTo = null)
    }

    private fun runBotMove(game: ChessGameState) {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            _state.value = _state.value.copy(thinking = true)
            try {
                val move = bot.chooseMove(game.position, _state.value.difficulty)
                val latest = _state.value
                if (latest.game?.position != game.position) return@launch
                if (move == null) {
                    _state.value = latest.copy(thinking = false, error = "Bot tidak menemukan langkah legal")
                    return@launch
                }
                val next = ChessRules.play(game, move).getOrNull()
                if (next == null) {
                    _state.value = latest.copy(thinking = false, error = "Engine menghasilkan langkah yang ditolak aturan catur")
                } else {
                    _state.value = latest.copy(game = next, thinking = false)
                    if (next.isFinished) persistFinished(latest, next)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = _state.value.copy(thinking = false, error = error.message ?: "Bot gagal berpikir")
            }
        }
    }

    fun toggleBoard() {
        _state.value = _state.value.copy(boardFlipped = !_state.value.boardFlipped)
    }

    fun resign() {
        val current = _state.value
        val game = current.game ?: return
        if (game.isFinished) return
        if (current.mode == ChessPlayMode.LAN_CLIENT) {
            lanTransport?.send("resign_request", JSONObject().put("revision", current.revision))
            return
        }
        botJob?.cancel()
        val winner = when (current.mode) {
            ChessPlayMode.SOLO_BOT, ChessPlayMode.LAN_HOST -> ChessColor.BLACK
            else -> game.position.sideToMove.opposite()
        }
        val finished = game.copy(status = ChessGameStatus.RESIGNED, winner = winner)
        _state.value = current.copy(
            game = finished, thinking = false, selectedSquare = null, legalTargets = emptyList(),
            revision = if (current.mode == ChessPlayMode.LAN_HOST) current.revision + 1 else current.revision
        )
        if (current.mode == ChessPlayMode.LAN_HOST) publishLanSnapshot()
        persistFinished(current, finished)
    }

    private fun persistFinished(previous: RexGamesUiState, game: ChessGameState) {
        if (previous.mode == null) return
        val opponent = if (previous.mode == ChessPlayMode.SOLO_BOT) previous.blackName else "${previous.whiteName} & ${previous.blackName}"
        val result = when (game.status) {
            ChessGameStatus.CHECKMATE -> if (game.winner == ChessColor.WHITE) "Putih menang" else "Hitam menang"
            ChessGameStatus.RESIGNED -> if (game.winner == ChessColor.WHITE) "Putih menang · menyerah" else "Hitam menang · menyerah"
            ChessGameStatus.STALEMATE -> "Seri · stalemate"
            ChessGameStatus.DRAW_THREEFOLD -> "Seri · repetition"
            ChessGameStatus.DRAW_FIFTY_MOVE -> "Seri · 50-move rule"
            ChessGameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Seri · material tidak cukup"
            else -> "Selesai"
        }
        history.add(
            ChessMatchSummary(
                id = previous.matchId,
                opponent = opponent,
                result = result,
                pgn = PgnManager.export(game, previous.whiteName, previous.blackName),
                finishedAt = System.currentTimeMillis()
            )
        )
        _state.value = _state.value.copy(recentMatches = history.load())
    }

    fun newGame() {
        closeLanTransport()
        botJob?.cancel()
        _state.value = RexGamesUiState(
            difficulty = settings.difficulty(), recentMatches = history.load()
        )
    }

    fun startLanHost(playerName: String, roomName: String, password: String) {
        closeLanTransport()
        botJob?.cancel()
        val hostName = playerName.trim().ifBlank { "Host" }
        handledLanRequests.clear()
        _state.value = RexGamesUiState(
            mode = ChessPlayMode.LAN_HOST,
            whiteName = hostName,
            blackName = "Menunggu lawan",
            playerColor = ChessColor.WHITE,
            recentMatches = history.load(),
            lan = ChessLanUiState(status = ChessLanStatus.HOSTING, role = ChessLanRole.HOST, roomName = roomName)
        )
        val transport = createLanTransport()
        lanTransport = transport
        transport.host(roomName, hostName, password)
    }

    fun joinLan(address: String, port: Int, playerName: String, password: String) {
        closeLanTransport()
        botJob?.cancel()
        val clientName = playerName.trim().ifBlank { "Pemain" }
        handledLanRequests.clear()
        _state.value = RexGamesUiState(
            mode = ChessPlayMode.LAN_CLIENT,
            whiteName = "Host",
            blackName = clientName,
            playerColor = ChessColor.BLACK,
            recentMatches = history.load(),
            lan = ChessLanUiState(status = ChessLanStatus.CONNECTING, role = ChessLanRole.CLIENT, hostAddress = address, port = port)
        )
        val transport = createLanTransport()
        lanTransport = transport
        transport.join(address, port, clientName, password)
    }

    fun startLanMatch() {
        val current = _state.value
        if (current.mode != ChessPlayMode.LAN_HOST || current.lan.status != ChessLanStatus.CONNECTED) return
        val next = current.copy(
            game = ChessGameState(),
            whiteName = current.whiteName,
            blackName = current.lan.peerName.ifBlank { "Lawan" },
            playerColor = ChessColor.WHITE,
            boardFlipped = false,
            revision = 0L,
            selectedSquare = null,
            legalTargets = emptyList(),
            error = null
        )
        _state.value = next
        lanTransport?.send("color_assignment", JSONObject().put("white", next.whiteName).put("black", next.blackName))
        lanTransport?.send("match_start", JSONObject().put("revision", next.revision))
        publishLanSnapshot()
    }

    fun reconnectLan(password: String) {
        val current = _state.value
        if (current.mode != ChessPlayMode.LAN_CLIENT) return
        val address = current.lan.hostAddress
        val port = current.lan.port
        closeLanTransport()
        val transport = createLanTransport()
        lanTransport = transport
        _state.value = current.copy(
            lan = current.lan.copy(status = ChessLanStatus.CONNECTING, message = "Menyambungkan ulang…")
        )
        transport.join(address, port, current.blackName, password)
    }

    fun leaveLan() = newGame()

    private fun createLanTransport(): ChessLanTransport {
        val generation = ++lanGeneration
        return ChessLanTransport(
            scope = viewModelScope,
            onState = { lanState ->
                if (generation == lanGeneration) {
                    val previous = _state.value
                    _state.value = previous.copy(
                        lan = lanState,
                        blackName = if (previous.mode == ChessPlayMode.LAN_HOST && lanState.peerName.isNotBlank()) lanState.peerName else previous.blackName
                    )
                    if (lanState.status == ChessLanStatus.CONNECTED && lanState.role == ChessLanRole.HOST && previous.game != null) {
                        publishLanSnapshot()
                    }
                }
            },
            onMessage = { message ->
                if (generation == lanGeneration) {
                    viewModelScope.launch {
                        if (generation == lanGeneration) handleLanMessage(message)
                    }
                }
            }
        )
    }

    private suspend fun handleLanMessage(message: JSONObject) {
        val current = _state.value
        when (message.optString("type")) {
            "move_request" -> {
                if (current.mode != ChessPlayMode.LAN_HOST) return
                val requestId = message.optString("requestId")
                if (requestId.isBlank()) return
                if (!handledLanRequests.add(requestId)) {
                    publishLanSnapshot()
                    return
                }
                if (handledLanRequests.size > 256) handledLanRequests.remove(handledLanRequests.first())
                val game = current.game
                val move = parseLanMove(message.optString("uci"))
                val validRequest = game != null && !game.isFinished &&
                    message.optLong("revision", -1L) == current.revision &&
                    game.position.sideToMove == ChessColor.BLACK && move != null
                if (!validRequest) {
                    lanTransport?.send("move_rejected", JSONObject().put("reason", "Giliran atau revisi tidak valid").put("revision", current.revision))
                    publishLanSnapshot()
                    return
                }
                val next = ChessRules.play(game!!, move!!).getOrNull()
                if (next == null) {
                    lanTransport?.send("move_rejected", JSONObject().put("reason", "Langkah tidak legal").put("revision", current.revision))
                    return
                }
                _state.value = current.copy(
                    game = next,
                    revision = current.revision + 1,
                    boardFlipped = false,
                    selectedSquare = null,
                    legalTargets = emptyList(),
                    error = null
                )
                lanTransport?.send("move_accepted", JSONObject().put("revision", current.revision + 1))
                publishLanSnapshot()
                if (next.isFinished) persistFinished(current, next)
            }
            "resign_request" -> {
                if (current.mode != ChessPlayMode.LAN_HOST || current.game?.isFinished != false ||
                    message.optLong("revision", -1L) != current.revision
                ) return
                val currentGame = current.game
                val finished = currentGame.copy(status = ChessGameStatus.RESIGNED, winner = ChessColor.WHITE)
                _state.value = current.copy(game = finished, revision = current.revision + 1)
                publishLanSnapshot()
                persistFinished(current, finished)
            }
            "game_state_snapshot" -> {
                if (current.mode != ChessPlayMode.LAN_CLIENT) return
                val revision = message.optLong("revision", -1L)
                if (revision < 0L || (current.game != null && revision < current.revision)) return
                val decoded = withContext(Dispatchers.Default) {
                    val moves = message.optJSONArray("moves") ?: return@withContext null
                    if (moves.length() > MAX_NETWORK_MOVES) return@withContext null
                    var replay = ChessGameState()
                    for (index in 0 until moves.length()) {
                        val move = parseLanMove(moves.optString(index)) ?: return@withContext null
                        replay = ChessRules.play(replay, move).getOrNull() ?: return@withContext null
                    }
                    val fen = message.optString("fen")
                    if (fen.isBlank() || ChessFen.export(replay.position) != fen) return@withContext null
                    val remoteStatus = runCatching { ChessGameStatus.valueOf(message.optString("status")) }.getOrNull() ?: return@withContext null
                    if (remoteStatus == ChessGameStatus.RESIGNED) {
                        val winner = runCatching { ChessColor.valueOf(message.optString("winner")) }.getOrNull() ?: return@withContext null
                        replay = replay.copy(status = remoteStatus, winner = winner)
                    } else if (replay.status != remoteStatus) return@withContext null
                    replay
                } ?: return
                val replay = decoded
                val previous = _state.value
                if (previous.mode != ChessPlayMode.LAN_CLIENT || revision < previous.revision) return
                _state.value = previous.copy(
                    game = replay,
                    mode = ChessPlayMode.LAN_CLIENT,
                    whiteName = message.optString("whiteName", "Host").take(32),
                    blackName = message.optString("blackName", previous.blackName).take(32),
                    playerColor = ChessColor.BLACK,
                    boardFlipped = true,
                    revision = revision,
                    selectedSquare = null,
                    legalTargets = emptyList(),
                    promotionFrom = null,
                    promotionTo = null,
                    error = null
                )
                if (replay.isFinished) persistFinished(previous, replay)
            }
            "move_rejected" -> {
                _state.value = current.copy(error = message.optString("reason", "Langkah ditolak host"))
            }
        }
    }

    private fun publishLanSnapshot() {
        val current = _state.value
        val game = current.game ?: return
        if (current.mode != ChessPlayMode.LAN_HOST) return
        val moves = JSONArray().apply { game.moves.forEach { put(it.toUci()) } }
        lanTransport?.send(
            "game_state_snapshot",
            JSONObject()
                .put("revision", current.revision)
                .put("fen", ChessFen.export(game.position))
                .put("moves", moves)
                .put("whiteName", current.whiteName)
                .put("blackName", current.blackName)
                .put("status", game.status.name)
                .put("winner", game.winner?.name ?: JSONObject.NULL)
        )
    }

    private fun parseLanMove(uci: String): ChessMove? {
        if (uci.length !in 4..5) return null
        val from = parseSquare(uci.substring(0, 2)) ?: return null
        val to = parseSquare(uci.substring(2, 4)) ?: return null
        val promotion = if (uci.length == 5) ChessPieceType.fromFen(uci[4]) ?: return null else null
        return runCatching { ChessMove(from, to, promotion) }.getOrNull()
    }

    private fun closeLanTransport() {
        lanGeneration++
        lanTransport?.close()
        lanTransport = null
    }

    fun dismissError() { _state.value = _state.value.copy(error = null) }

    override fun onCleared() {
        botJob?.cancel()
        closeLanTransport()
        super.onCleared()
    }

    private companion object { const val MAX_NETWORK_MOVES = 2_000 }
}
