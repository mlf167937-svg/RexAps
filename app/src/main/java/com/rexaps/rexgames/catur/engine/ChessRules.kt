package com.rexaps.rexgames.catur.engine

/** Pure Kotlin rules engine. Board index 0 is a8 and index 63 is h1. */
object ChessRules {
    private val knightOffsets = arrayOf(
        -2 to -1, -2 to 1, -1 to -2, -1 to 2,
        1 to -2, 1 to 2, 2 to -1, 2 to 1
    )
    private val kingOffsets = arrayOf(
        -1 to -1, -1 to 0, -1 to 1, 0 to -1,
        0 to 1, 1 to -1, 1 to 0, 1 to 1
    )
    private val bishopDirections = arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    private val rookDirections = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    fun legalMoves(position: ChessPosition): List<ChessMove> = pseudoMoves(position).filter { move ->
        val piece = position.board[move.from] ?: return@filter false
        val next = applyUnchecked(position, move, piece)
        !isInCheck(next, piece.color)
    }

    fun legalMovesFrom(position: ChessPosition, square: Int): List<ChessMove> =
        legalMoves(position).filter { it.from == square }

    fun isInCheck(position: ChessPosition, color: ChessColor): Boolean {
        val kingSquare = position.board.indexOfFirst {
            it?.color == color && it.type == ChessPieceType.KING
        }
        return kingSquare < 0 || isSquareAttacked(position, kingSquare, color.opposite())
    }

    fun isSquareAttacked(position: ChessPosition, square: Int, byColor: ChessColor): Boolean {
        if (square !in 0..63) return true
        val targetRow = square / 8
        val targetCol = square % 8
        for (from in 0..63) {
            val piece = position.board[from] ?: continue
            if (piece.color != byColor) continue
            val row = from / 8
            val col = from % 8
            val dr = targetRow - row
            val dc = targetCol - col
            when (piece.type) {
                ChessPieceType.PAWN -> {
                    val direction = if (byColor == ChessColor.WHITE) -1 else 1
                    if (dr == direction && kotlin.math.abs(dc) == 1) return true
                }
                ChessPieceType.KNIGHT -> if ((kotlin.math.abs(dr) to kotlin.math.abs(dc)) in listOf(1 to 2, 2 to 1)) return true
                ChessPieceType.KING -> if (kotlin.math.abs(dr) <= 1 && kotlin.math.abs(dc) <= 1) return true
                ChessPieceType.BISHOP -> if (kotlin.math.abs(dr) == kotlin.math.abs(dc) && dr != 0 && pathIsClear(position, from, square)) return true
                ChessPieceType.ROOK -> if ((dr == 0 || dc == 0) && dr + dc != 0 && pathIsClear(position, from, square)) return true
                ChessPieceType.QUEEN -> if (((kotlin.math.abs(dr) == kotlin.math.abs(dc) && dr != 0) || ((dr == 0 || dc == 0) && dr + dc != 0)) && pathIsClear(position, from, square)) return true
            }
        }
        return false
    }

    fun applyLegalMove(position: ChessPosition, move: ChessMove): ChessPosition? {
        if (move !in legalMoves(position)) return null
        return applyGeneratedMove(position, move)
    }

    /** Internal fast path for search code iterating a list returned by [legalMoves]. */
    internal fun applyGeneratedMove(position: ChessPosition, move: ChessMove): ChessPosition? {
        val piece = position.board.getOrNull(move.from) ?: return null
        if (piece.color != position.sideToMove) return null
        return applyUnchecked(position, move, piece)
    }

    fun play(state: ChessGameState, move: ChessMove): Result<ChessGameState> {
        if (state.isFinished) return Result.failure(IllegalStateException("Pertandingan sudah selesai"))
        if (move !in legalMoves(state.position)) return Result.failure(IllegalArgumentException("Langkah catur tidak legal"))
        val san = sanForMove(state.position, move)
        val movingPiece = state.position.board[move.from] ?: return Result.failure(IllegalArgumentException("Bidak tidak ditemukan"))
        val captured = state.position.board[move.to] ?: if (
            movingPiece.type == ChessPieceType.PAWN && move.from % 8 != move.to % 8 && move.to == state.position.enPassantSquare
        ) state.position.board.getOrNull(move.to + if (movingPiece.color == ChessColor.WHITE) 8 else -8) else null
        val nextPosition = applyUnchecked(state.position, move, movingPiece)
        val status = evaluateStatus(nextPosition)
        val winner = if (status == ChessGameStatus.CHECKMATE) movingPiece.color else null
        return Result.success(
            ChessGameState(
                position = nextPosition,
                moves = state.moves + move,
                sanMoves = state.sanMoves + san,
                status = status,
                winner = winner,
                capturedByWhite = if (movingPiece.color == ChessColor.WHITE && captured != null) state.capturedByWhite + captured else state.capturedByWhite,
                capturedByBlack = if (movingPiece.color == ChessColor.BLACK && captured != null) state.capturedByBlack + captured else state.capturedByBlack
            )
        )
    }

    fun evaluateStatus(position: ChessPosition): ChessGameStatus {
        val moves = legalMoves(position)
        val checked = isInCheck(position, position.sideToMove)
        if (moves.isEmpty()) return if (checked) ChessGameStatus.CHECKMATE else ChessGameStatus.STALEMATE
        if (position.positionHistory.count { it == ChessFen.positionKey(position) } >= 3) return ChessGameStatus.DRAW_THREEFOLD
        if (position.halfmoveClock >= 100) return ChessGameStatus.DRAW_FIFTY_MOVE
        if (isInsufficientMaterial(position)) return ChessGameStatus.DRAW_INSUFFICIENT_MATERIAL
        return if (checked) ChessGameStatus.CHECK else ChessGameStatus.ONGOING
    }

    fun isInsufficientMaterial(position: ChessPosition): Boolean {
        val nonKings = position.board.filterNotNull().filter { it.type != ChessPieceType.KING }
        if (nonKings.isEmpty()) return true
        if (nonKings.any { it.type == ChessPieceType.PAWN || it.type == ChessPieceType.ROOK || it.type == ChessPieceType.QUEEN }) return false
        if (nonKings.size == 1 && nonKings.single().type in setOf(ChessPieceType.BISHOP, ChessPieceType.KNIGHT)) return true
        if (nonKings.size == 2 && nonKings.all { it.type == ChessPieceType.KNIGHT } &&
            nonKings.map { it.color }.toSet().size == 1
        ) return true
        if (nonKings.all { it.type == ChessPieceType.BISHOP }) {
            val squareColors = position.board.mapIndexedNotNull { index, piece ->
                if (piece?.type == ChessPieceType.BISHOP) (index / 8 + index % 8) % 2 else null
            }.toSet()
            return squareColors.size <= 1
        }
        return false
    }

    fun sanForMove(position: ChessPosition, move: ChessMove): String {
        val piece = position.board.getOrNull(move.from) ?: return move.toUci()
        if (piece.type == ChessPieceType.KING && kotlin.math.abs(move.to % 8 - move.from % 8) == 2) {
            val base = if (move.to % 8 > move.from % 8) "O-O" else "O-O-O"
            val afterCastle = applyUnchecked(position, move, piece)
            return when (evaluateStatus(afterCastle)) {
                ChessGameStatus.CHECKMATE -> "$base#"
                ChessGameStatus.CHECK -> "$base+"
                else -> base
            }
        }
        val capture = position.board[move.to] != null ||
            (piece.type == ChessPieceType.PAWN && move.from % 8 != move.to % 8)
        val legal = legalMoves(position)
        val contenders = legal.filter { candidate ->
            candidate != move && candidate.to == move.to &&
                position.board[candidate.from]?.type == piece.type
        }
        val disambiguation = if (contenders.isEmpty()) "" else {
            val sameFile = contenders.any { it.from % 8 == move.from % 8 }
            val sameRank = contenders.any { it.from / 8 == move.from / 8 }
            when {
                !sameFile -> ('a' + move.from % 8).toString()
                !sameRank -> ('8' - move.from / 8).toString()
                else -> squareName(move.from)
            }
        }
        val prefix = if (piece.type == ChessPieceType.PAWN) {
            if (capture) ('a' + move.from % 8).toString() else ""
        } else piece.type.san.toString() + disambiguation
        val suffix = if (move.promotion != null) "=${move.promotion.san}" else ""
        val base = prefix + (if (capture) "x" else "") + squareName(move.to) + suffix
        val after = applyUnchecked(position, move, piece)
        return when (evaluateStatus(after)) {
            ChessGameStatus.CHECKMATE -> "$base#"
            ChessGameStatus.CHECK -> "$base+"
            else -> base
        }
    }

    private fun pseudoMoves(position: ChessPosition): List<ChessMove> = buildList {
        for (from in 0..63) {
            val piece = position.board[from] ?: continue
            if (piece.color != position.sideToMove) continue
            when (piece.type) {
                ChessPieceType.PAWN -> addPawnMoves(position, from, piece, this)
                ChessPieceType.KNIGHT -> addJumpMoves(position, from, piece, knightOffsets, this)
                ChessPieceType.BISHOP -> addSlidingMoves(position, from, piece, bishopDirections, this)
                ChessPieceType.ROOK -> addSlidingMoves(position, from, piece, rookDirections, this)
                ChessPieceType.QUEEN -> addSlidingMoves(position, from, piece, bishopDirections + rookDirections, this)
                ChessPieceType.KING -> {
                    addJumpMoves(position, from, piece, kingOffsets, this)
                    addCastlingMoves(position, from, piece, this)
                }
            }
        }
    }

    private fun addPawnMoves(position: ChessPosition, from: Int, piece: ChessPiece, out: MutableList<ChessMove>) {
        val row = from / 8
        val col = from % 8
        val direction = if (piece.color == ChessColor.WHITE) -1 else 1
        val startRow = if (piece.color == ChessColor.WHITE) 6 else 1
        val promotionRow = if (piece.color == ChessColor.WHITE) 0 else 7
        val nextRow = row + direction
        if (nextRow in 0..7) {
            val one = nextRow * 8 + col
            if (position.board[one] == null) {
                addPawnDestination(from, one, promotionRow, out)
                val twoRow = row + 2 * direction
                val two = twoRow * 8 + col
                if (row == startRow && position.board[two] == null) out += ChessMove(from, two)
            }
            for (dc in intArrayOf(-1, 1)) {
                val targetCol = col + dc
                if (targetCol !in 0..7) continue
                val to = nextRow * 8 + targetCol
                val target = position.board[to]
                if (target != null && target.color != piece.color && target.type != ChessPieceType.KING) {
                    addPawnDestination(from, to, promotionRow, out)
                } else if (to == position.enPassantSquare) {
                    val capturedIndex = to - direction * 8
                    val captured = position.board.getOrNull(capturedIndex)
                    if (captured?.color == piece.color.opposite() && captured.type == ChessPieceType.PAWN) out += ChessMove(from, to)
                }
            }
        }
    }

    private fun addPawnDestination(from: Int, to: Int, promotionRow: Int, out: MutableList<ChessMove>) {
        if (to / 8 == promotionRow) {
            listOf(ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT)
                .forEach { out += ChessMove(from, to, it) }
        } else out += ChessMove(from, to)
    }

    private fun addJumpMoves(
        position: ChessPosition,
        from: Int,
        piece: ChessPiece,
        offsets: Array<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        val row = from / 8
        val col = from % 8
        offsets.forEach { (dr, dc) ->
            val r = row + dr
            val c = col + dc
            if (r !in 0..7 || c !in 0..7) return@forEach
            val to = r * 8 + c
            val target = position.board[to]
            if (target == null || (target.color != piece.color && target.type != ChessPieceType.KING)) {
                out += ChessMove(from, to)
            }
        }
    }

    private fun addSlidingMoves(
        position: ChessPosition,
        from: Int,
        piece: ChessPiece,
        directions: Array<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        val row = from / 8
        val col = from % 8
        directions.forEach { (dr, dc) ->
            var r = row + dr
            var c = col + dc
            while (r in 0..7 && c in 0..7) {
                val to = r * 8 + c
                val target = position.board[to]
                if (target == null) out += ChessMove(from, to)
                else {
                    if (target.color != piece.color && target.type != ChessPieceType.KING) out += ChessMove(from, to)
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    private fun addCastlingMoves(position: ChessPosition, from: Int, piece: ChessPiece, out: MutableList<ChessMove>) {
        val home = if (piece.color == ChessColor.WHITE) 60 else 4
        if (from != home || isInCheck(position, piece.color)) return
        val enemy = piece.color.opposite()
        val rights = position.castlingRights
        if (piece.color == ChessColor.WHITE) {
            if ('K' in rights && position.board[63] == ChessPiece(ChessColor.WHITE, ChessPieceType.ROOK) &&
                position.board[61] == null && position.board[62] == null &&
                !isSquareAttacked(position, 61, enemy) && !isSquareAttacked(position, 62, enemy)
            ) out += ChessMove(60, 62)
            if ('Q' in rights && position.board[56] == ChessPiece(ChessColor.WHITE, ChessPieceType.ROOK) &&
                position.board[59] == null && position.board[58] == null && position.board[57] == null &&
                !isSquareAttacked(position, 59, enemy) && !isSquareAttacked(position, 58, enemy)
            ) out += ChessMove(60, 58)
        } else {
            if ('k' in rights && position.board[7] == ChessPiece(ChessColor.BLACK, ChessPieceType.ROOK) &&
                position.board[5] == null && position.board[6] == null &&
                !isSquareAttacked(position, 5, enemy) && !isSquareAttacked(position, 6, enemy)
            ) out += ChessMove(4, 6)
            if ('q' in rights && position.board[0] == ChessPiece(ChessColor.BLACK, ChessPieceType.ROOK) &&
                position.board[3] == null && position.board[2] == null && position.board[1] == null &&
                !isSquareAttacked(position, 3, enemy) && !isSquareAttacked(position, 2, enemy)
            ) out += ChessMove(4, 2)
        }
    }

    private fun applyUnchecked(position: ChessPosition, move: ChessMove, moving: ChessPiece): ChessPosition {
        val board = position.board.toMutableList()
        val captured = board[move.to]
        board[move.from] = null
        if (moving.type == ChessPieceType.PAWN && move.to == position.enPassantSquare && captured == null && move.from % 8 != move.to % 8) {
            val capturedIndex = move.to + if (moving.color == ChessColor.WHITE) 8 else -8
            board[capturedIndex] = null
        }
        val placed = if (move.promotion != null) ChessPiece(moving.color, move.promotion) else moving
        board[move.to] = placed

        if (moving.type == ChessPieceType.KING && kotlin.math.abs(move.to - move.from) == 2) {
            when (move.to) {
                62 -> { board[63] = null; board[61] = ChessPiece(ChessColor.WHITE, ChessPieceType.ROOK) }
                58 -> { board[56] = null; board[59] = ChessPiece(ChessColor.WHITE, ChessPieceType.ROOK) }
                6 -> { board[7] = null; board[5] = ChessPiece(ChessColor.BLACK, ChessPieceType.ROOK) }
                2 -> { board[0] = null; board[3] = ChessPiece(ChessColor.BLACK, ChessPieceType.ROOK) }
            }
        }

        var rights = position.castlingRights
        fun removeRight(char: Char) { rights = rights.replace(char.toString(), "") }
        when (move.from) {
            60 -> { removeRight('K'); removeRight('Q') }
            4 -> { removeRight('k'); removeRight('q') }
            63 -> removeRight('K')
            56 -> removeRight('Q')
            7 -> removeRight('k')
            0 -> removeRight('q')
        }
        when (move.to) {
            63 -> removeRight('K')
            56 -> removeRight('Q')
            7 -> removeRight('k')
            0 -> removeRight('q')
        }
        if (rights.isBlank()) rights = "-"

        val ep = if (moving.type == ChessPieceType.PAWN && kotlin.math.abs(move.to - move.from) == 16) {
            (move.to + move.from) / 2
        } else null
        val halfmove = if (moving.type == ChessPieceType.PAWN || captured != null ||
            (moving.type == ChessPieceType.PAWN && move.to == position.enPassantSquare)
        ) 0 else position.halfmoveClock + 1
        val next = ChessPosition(
            board = board,
            sideToMove = moving.color.opposite(),
            castlingRights = rights,
            enPassantSquare = ep,
            halfmoveClock = halfmove,
            fullmoveNumber = position.fullmoveNumber + if (moving.color == ChessColor.BLACK) 1 else 0,
            positionHistory = position.positionHistory
        )
        val key = ChessFen.positionKey(next)
        return next.copy(positionHistory = position.positionHistory + key)
    }

    private fun pathIsClear(position: ChessPosition, from: Int, to: Int): Boolean {
        val dr = (to / 8 - from / 8).compareTo(0)
        val dc = (to % 8 - from % 8).compareTo(0)
        var row = from / 8 + dr
        var col = from % 8 + dc
        while (row * 8 + col != to) {
            if (position.board[row * 8 + col] != null) return false
            row += dr
            col += dc
        }
        return true
    }
}
