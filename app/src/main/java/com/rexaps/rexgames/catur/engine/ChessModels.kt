package com.rexaps.rexgames.catur.engine

enum class ChessColor {
    WHITE, BLACK;

    fun opposite(): ChessColor = if (this == WHITE) BLACK else WHITE
}

enum class ChessPieceType(val fen: Char, val san: Char, val value: Int) {
    KING('k', 'K', 0), QUEEN('q', 'Q', 900), ROOK('r', 'R', 500),
    BISHOP('b', 'B', 330), KNIGHT('n', 'N', 320), PAWN('p', ' ', 100);

    companion object {
        fun fromFen(char: Char): ChessPieceType? = entries.firstOrNull { it.fen == char.lowercaseChar() }
    }
}

data class ChessPiece(val color: ChessColor, val type: ChessPieceType)

data class ChessMove(val from: Int, val to: Int, val promotion: ChessPieceType? = null) {
    init {
        require(from in 0..63 && to in 0..63 && from != to)
        require(promotion == null || promotion in setOf(
            ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT
        ))
    }

    fun toUci(): String = squareName(from) + squareName(to) + (promotion?.fen?.toString() ?: "")
}

enum class ChessGameStatus {
    ONGOING, CHECK, CHECKMATE, STALEMATE, DRAW_THREEFOLD, DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT_MATERIAL, RESIGNED
}

data class ChessPosition(
    val board: List<ChessPiece?>,
    val sideToMove: ChessColor = ChessColor.WHITE,
    val castlingRights: String = "KQkq",
    val enPassantSquare: Int? = null,
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1,
    val positionHistory: List<String> = emptyList()
) {
    init {
        require(board.size == 64) { "A chess board must contain 64 squares" }
        require(halfmoveClock >= 0 && fullmoveNumber >= 1)
    }

    fun pieceAt(square: Int): ChessPiece? = board.getOrNull(square)

    companion object {
        fun initial(): ChessPosition {
            val rank = listOf(
                ChessPieceType.ROOK, ChessPieceType.KNIGHT, ChessPieceType.BISHOP,
                ChessPieceType.QUEEN, ChessPieceType.KING, ChessPieceType.BISHOP,
                ChessPieceType.KNIGHT, ChessPieceType.ROOK
            )
            val pieces = MutableList<ChessPiece?>(64) { null }
            for (file in 0..7) {
                pieces[file] = ChessPiece(ChessColor.BLACK, rank[file])
                pieces[8 + file] = ChessPiece(ChessColor.BLACK, ChessPieceType.PAWN)
                pieces[48 + file] = ChessPiece(ChessColor.WHITE, ChessPieceType.PAWN)
                pieces[56 + file] = ChessPiece(ChessColor.WHITE, rank[file])
            }
            val position = ChessPosition(pieces)
            return position.copy(positionHistory = listOf(ChessFen.positionKey(position)))
        }
    }
}

data class ChessGameState(
    val position: ChessPosition = ChessPosition.initial(),
    val moves: List<ChessMove> = emptyList(),
    val sanMoves: List<String> = emptyList(),
    val status: ChessGameStatus = ChessGameStatus.ONGOING,
    val winner: ChessColor? = null,
    val capturedByWhite: List<ChessPiece> = emptyList(),
    val capturedByBlack: List<ChessPiece> = emptyList()
) {
    val isFinished: Boolean
        get() = status in setOf(
            ChessGameStatus.CHECKMATE, ChessGameStatus.STALEMATE,
            ChessGameStatus.DRAW_THREEFOLD, ChessGameStatus.DRAW_FIFTY_MOVE,
            ChessGameStatus.DRAW_INSUFFICIENT_MATERIAL, ChessGameStatus.RESIGNED
        )
}

fun squareName(square: Int): String {
    require(square in 0..63)
    val file = 'a' + (square % 8)
    val rank = '8' - (square / 8)
    return "$file$rank"
}

fun parseSquare(value: String): Int? {
    if (value.length != 2) return null
    val file = value[0].lowercaseChar() - 'a'
    val rank = '8' - value[1]
    if (file !in 0..7 || rank !in 0..7) return null
    return rank * 8 + file
}
