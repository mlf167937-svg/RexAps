package com.rexaps.rexgames

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexgames.catur.bot.ChessDifficulty
import com.rexaps.rexgames.catur.engine.*
import com.rexaps.rexgames.catur.ui.ChessBoardView
import com.rexaps.rexgames.catur.ui.pieceGlyph
import com.rexaps.rexgames.network.ChessLanRole
import com.rexaps.rexgames.network.ChessLanStatus
import com.rexaps.rexgames.network.ChessLanTransport
import com.rexaps.rexmusic.RexLocalProfileBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RexGamesScreen(
    onExit: () -> Unit = {},
    viewModel: RexGamesViewModel = viewModel()
) {
    RexGamesTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var playerName by remember { mutableStateOf("Pemain") }
        var secondPlayerName by rememberSaveable { mutableStateOf("Pemain 2") }
        var showResignDialog by rememberSaveable { mutableStateOf(false) }
        var showNewGameDialog by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            playerName = withContext(Dispatchers.IO) {
                runCatching { RexLocalProfileBridge.read().name }.getOrDefault("Pemain")
            }
        }

        BackHandler {
            if (state.game != null) viewModel.newGame()
            else { viewModel.leaveLan(); onExit() }
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (state.game == null) {
                RexGamesHome(
                    state = state,
                    playerName = playerName,
                    onPlayerName = { playerName = it },
                    secondPlayerName = secondPlayerName,
                    onSecondPlayerName = { secondPlayerName = it },
                    onDifficulty = viewModel::setDifficulty,
                    onSolo = { viewModel.startSolo(playerName) },
                    onPassAndPlay = { viewModel.startPassAndPlay(playerName, secondPlayerName) },
                    onHostLan = { room, password -> viewModel.startLanHost(playerName, room, password) },
                    onJoinLan = { address, port, password -> viewModel.joinLan(address, port, playerName, password) },
                    onStartLan = viewModel::startLanMatch,
                    onLeaveLan = viewModel::leaveLan,
                    onExit = { viewModel.leaveLan(); onExit() }
                )
            } else {
                ChessMatchScreen(
                    state = state,
                    onBack = viewModel::newGame,
                    onSquare = viewModel::selectSquare,
                    onPromotion = viewModel::choosePromotion,
                    onDismissPromotion = viewModel::dismissPromotion,
                    onFlip = viewModel::toggleBoard,
                    onResign = { showResignDialog = true },
                    onNewGame = { showNewGameDialog = true },
                    onReconnectLan = viewModel::reconnectLan
                )
            }
        }

        if (state.error != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissError,
                title = { Text("RexGames") },
                text = { Text(state.error.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissError) { Text("Tutup") }
                }
            )
        }
        if (showResignDialog) {
            AlertDialog(
                onDismissRequest = { showResignDialog = false },
                title = { Text("Menyerah dari pertandingan?") },
                text = { Text("Hasil pertandingan akan disimpan ke riwayat.") },
                confirmButton = {
                    TextButton(onClick = { showResignDialog = false; viewModel.resign() }) { Text("Menyerah") }
                },
                dismissButton = { TextButton(onClick = { showResignDialog = false }) { Text("Lanjut bermain") } }
            )
        }
        if (showNewGameDialog) {
            AlertDialog(
                onDismissRequest = { showNewGameDialog = false },
                title = { Text("Mulai pertandingan baru?") },
                text = { Text("Posisi saat ini akan ditinggalkan. Riwayat pertandingan yang selesai tetap tersimpan.") },
                confirmButton = {
                    TextButton(onClick = { showNewGameDialog = false; viewModel.newGame() }) { Text("Pertandingan baru") }
                },
                dismissButton = { TextButton(onClick = { showNewGameDialog = false }) { Text("Batal") } }
            )
        }
    }
}

@Composable
private fun RexGamesHome(
    state: RexGamesUiState,
    playerName: String,
    onPlayerName: (String) -> Unit,
    secondPlayerName: String,
    onSecondPlayerName: (String) -> Unit,
    onDifficulty: (ChessDifficulty) -> Unit,
    onSolo: () -> Unit,
    onPassAndPlay: () -> Unit,
    onHostLan: (String, String) -> Unit,
    onJoinLan: (String, Int, String) -> Unit,
    onStartLan: () -> Unit,
    onLeaveLan: () -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var roomName by rememberSaveable { mutableStateOf("Rex Chess Room") }
    var lanAddress by rememberSaveable { mutableStateOf("") }
    var lanPort by rememberSaveable { mutableStateOf(ChessLanTransport.DEFAULT_PORT.toString()) }
    var lanPassword by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Column(Modifier.weight(1f)) {
                Text("RexGames", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Offline Chess & Local Multiplayer", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Icon(Icons.Default.Star, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = colors.primary, modifier = Modifier.size(36.dp))
                    Column {
                        Text("Chess", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Permainan strategi klasik", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                Text(
                    "Main offline dengan bot lokal atau bergantian pada satu perangkat. Seluruh langkah divalidasi oleh aturan catur.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text("Solo melawan bot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = playerName,
            onValueChange = onPlayerName,
            label = { Text("Nama kamu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Tingkat kesulitan", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChessDifficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = state.difficulty == difficulty,
                    onClick = { onDifficulty(difficulty) },
                    label = { Text(difficulty.label) }
                )
            }
        }
        Button(onClick = onSolo, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Mulai solo")
        }

        HorizontalDivider()
        Text("Dua pemain · satu perangkat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = playerName,
            onValueChange = onPlayerName,
            label = { Text("Pemain 1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = secondPlayerName,
            onValueChange = onSecondPlayerName,
            label = { Text("Pemain 2") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(onClick = onPassAndPlay, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Mulai pass-and-play")
        }

        HorizontalDivider()
        Text("Multiplayer LAN · jaringan lokal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Gunakan Wi-Fi atau hotspot yang sama. Host membagikan alamat IP dan port; tidak memakai server matchmaking cloud.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it.take(32) },
            label = { Text("Nama room (host)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lanPassword,
            onValueChange = { lanPassword = it.take(64) },
            label = { Text("Password room (opsional)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onHostLan(roomName, lanPassword); lanPassword = "" },
            enabled = state.lan.status == ChessLanStatus.IDLE || state.lan.status == ChessLanStatus.ERROR || state.lan.status == ChessLanStatus.DISCONNECTED,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Buat room sebagai host") }
        OutlinedTextField(
            value = lanAddress,
            onValueChange = { lanAddress = it.take(128) },
            label = { Text("IP host untuk bergabung") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lanPort,
            onValueChange = { lanPort = it.filter(Char::isDigit).take(5) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = {
                val port = lanPort.toIntOrNull() ?: -1
                onJoinLan(lanAddress, port, lanPassword)
                lanPassword = ""
            },
            enabled = lanAddress.isNotBlank() && (lanPort.toIntOrNull()?.let { it in 1..65535 } == true) &&
                state.lan.status != ChessLanStatus.CONNECTING,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Gabung room") }

        if (state.lan.status != ChessLanStatus.IDLE) {
            Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.lan.roomName.ifBlank { "Status LAN" }, style = MaterialTheme.typography.titleMedium)
                    Text(state.lan.message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    if (state.lan.role == ChessLanRole.HOST) {
                        Text(
                            "Alamat untuk lawan: ${state.lan.hostAddress.ifBlank { "IP belum terdeteksi" }}:${state.lan.port}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (state.lan.status == ChessLanStatus.CONNECTED && state.lan.role == ChessLanRole.HOST) {
                        Button(onClick = onStartLan, modifier = Modifier.fillMaxWidth()) { Text("Mulai pertandingan") }
                    }
                    if (state.lan.role != null) {
                        TextButton(onClick = onLeaveLan) { Text("Tutup / keluar room") }
                    }
                    Text("Koneksi LAN lokal tidak dienkripsi. Jangan gunakan password penting.", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }

        if (state.recentMatches.isNotEmpty()) {
            HorizontalDivider()
            Text("Pertandingan terakhir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            state.recentMatches.take(5).forEach { match ->
                ListItem(
                    headlineContent = { Text(match.opponent, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(match.result) },
                    leadingContent = {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = colors.onSecondaryContainer)
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ChessMatchScreen(
    state: RexGamesUiState,
    onBack: () -> Unit,
    onSquare: (Int) -> Unit,
    onPromotion: (ChessPieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onNewGame: () -> Unit,
    onReconnectLan: (String) -> Unit
) {
    val game = state.game ?: return
    val colors = MaterialTheme.colorScheme
    val currentColor = game.position.sideToMove
    val currentName = if (currentColor == ChessColor.WHITE) state.whiteName else state.blackName
    val oppositeName = if (currentColor == ChessColor.WHITE) state.blackName else state.whiteName
    val lastMove = game.moves.lastOrNull()
    val isLocalTurn = when (state.mode) {
        ChessPlayMode.SOLO_BOT -> currentColor == state.playerColor
        ChessPlayMode.PASS_AND_PLAY -> true
        ChessPlayMode.LAN_HOST -> currentColor == ChessColor.WHITE
        ChessPlayMode.LAN_CLIENT -> currentColor == ChessColor.BLACK
        null -> false
    }
    val boardEnabled = !state.thinking && !game.isFinished && isLocalTurn
    var reconnectPassword by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke RexGames") }
            Column(Modifier.weight(1f)) {
                Text("Chess", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(matchStatus(game), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            IconButton(onClick = onFlip) { Icon(Icons.Default.SwapVert, contentDescription = "Balik papan") }
            IconButton(onClick = onNewGame) { Icon(Icons.Default.Refresh, contentDescription = "Pertandingan baru") }
        }
        if (state.mode == ChessPlayMode.LAN_HOST || state.mode == ChessPlayMode.LAN_CLIENT) {
            Text(
                "LAN · ${state.lan.status.name.lowercase()} · ${state.lan.message}",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.lan.status == ChessLanStatus.CONNECTED) colors.primary else colors.error,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            if (state.mode == ChessPlayMode.LAN_CLIENT && state.lan.status == ChessLanStatus.DISCONNECTED) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = reconnectPassword,
                        onValueChange = { reconnectPassword = it.take(64) },
                        label = { Text("Password room untuk sambung ulang") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { onReconnectLan(reconnectPassword); reconnectPassword = "" }) { Text("Sambung") }
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PlayerCard(oppositeName, currentColor.opposite(),
                            if (currentColor == ChessColor.WHITE) game.capturedByBlack else game.capturedByWhite)
                        ChessBoardView(
                            position = game.position,
                            selectedSquare = state.selectedSquare,
                            legalMoves = state.legalTargets,
                            lastMove = lastMove,
                            flipped = state.boardFlipped,
                            enabled = boardEnabled,
                            modifier = Modifier.fillMaxHeight(0.78f).aspectRatio(1f),
                            onSquareClick = onSquare
                        )
                        PlayerCard(currentName, currentColor,
                            if (currentColor == ChessColor.WHITE) game.capturedByWhite else game.capturedByBlack)
                    }
                    Column(
                        Modifier.widthIn(min = 190.dp, max = 260.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MatchSidePanel(game, state.thinking, onResign, onNewGame)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerCard(oppositeName, currentColor.opposite(),
                        if (currentColor == ChessColor.WHITE) game.capturedByBlack else game.capturedByWhite)
                    ChessBoardView(
                        position = game.position,
                        selectedSquare = state.selectedSquare,
                        legalMoves = state.legalTargets,
                        lastMove = lastMove,
                        flipped = state.boardFlipped,
                        enabled = boardEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        onSquareClick = onSquare
                    )
                    PlayerCard(currentName, currentColor,
                        if (currentColor == ChessColor.WHITE) game.capturedByWhite else game.capturedByBlack)
                    MatchSidePanel(game, state.thinking, onResign, onNewGame)
                }
            }
        }
    }

    if (state.promotionFrom != null && state.promotionTo != null) {
        AlertDialog(
            onDismissRequest = onDismissPromotion,
            title = { Text("Pilih promosi pion") },
            text = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT).forEach { type ->
                        TextButton(onClick = { onPromotion(type) }) {
                            Text(
                                pieceGlyph(ChessPiece(game.position.sideToMove, type)),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissPromotion) { Text("Batal") } }
        )
    }
}

@Composable
private fun PlayerCard(name: String, color: ChessColor, captured: List<ChessPiece>) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(scheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(if (color == ChessColor.WHITE) "♔" else "♚", color = scheme.onSecondaryContainer, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (color == ChessColor.WHITE) "Putih" else "Hitam", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            if (captured.isNotEmpty()) {
                Text(
                    captured.sortedByDescending { it.type.value }.joinToString("") { pieceGlyph(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MatchSidePanel(
    game: ChessGameState,
    thinking: Boolean,
    onResign: () -> Unit,
    onNewGame: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    if (thinking) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("Bot sedang berpikir…", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (game.isFinished) {
        Card(colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer)) {
            Column(Modifier.padding(14.dp)) {
                Text(matchStatus(game), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    when (game.status) {
                        ChessGameStatus.CHECKMATE, ChessGameStatus.RESIGNED -> if (game.winner == ChessColor.WHITE) "Putih menang" else "Hitam menang"
                        else -> "Pertandingan berakhir seri"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) { Text("Kembali ke RexGames") }
    } else {
        Text("Langkah", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(10.dp)) {
                if (game.sanMoves.isEmpty()) {
                    Text("Belum ada langkah", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    game.sanMoves.chunked(2).forEachIndexed { index, moves ->
                        Text(
                            buildString {
                                append("${index + 1}.  ")
                                append(moves.getOrNull(0).orEmpty())
                                if (moves.size > 1) append("     ${moves[1]}")
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        OutlinedButton(onClick = onResign, modifier = Modifier.fillMaxWidth()) { Text("Menyerah") }
    }
}

private fun matchStatus(game: ChessGameState): String = when (game.status) {
    ChessGameStatus.CHECK -> "Check · giliran ${if (game.position.sideToMove == ChessColor.WHITE) "Putih" else "Hitam"}"
    ChessGameStatus.CHECKMATE -> "Checkmate"
    ChessGameStatus.STALEMATE -> "Stalemate · seri"
    ChessGameStatus.DRAW_THREEFOLD -> "Draw · threefold repetition"
    ChessGameStatus.DRAW_FIFTY_MOVE -> "Draw · fifty-move rule"
    ChessGameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Draw · insufficient material"
    ChessGameStatus.RESIGNED -> "Menyerah"
    ChessGameStatus.ONGOING -> "Giliran ${if (game.position.sideToMove == ChessColor.WHITE) "Putih" else "Hitam"}"
}
