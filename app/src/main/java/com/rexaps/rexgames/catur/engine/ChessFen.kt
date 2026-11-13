package com.rexaps.rexgames.catur.engine

object ChessFen {
    const val INITIAL = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun export(position: ChessPosition): String {
        val rows = (0..7).map { row ->
            buildString {
                var empty = 0
                for (col in 0..7) {
                    val piece = position.board[row * 8 + col]
                    if (piece == null) empty++ else {
                        if (empty > 0) { append(empty); empty = 0 }
                        val ch = piece.type.fen
                        append(if (piece.color == ChessColor.WHITE) ch.uppercaseChar() else ch)
                    }
                }
                if (empty > 0) append(empty)
            }
        }
        return listOf(
            rows.joinToString("/"),
            if (position.sideToMove == ChessColor.WHITE) "w" else "b",
            position.castlingRights.ifBlank { "-" },
            position.enPassantSquare?.let(::squareName) ?: "-",
            position.halfmoveClock.toString(),
            position.fullmoveNumber.toString()
        ).joinToString(" ")
    }

    fun parse(fen: String): Result<ChessPosition> = runCatching {
        val fields = fen.trim().split(Regex("\\s+"))
        require(fields.size == 6) { "FEN harus berisi enam field" }
        val ranks = fields[0].split('/')
        require(ranks.size == 8) { "FEN harus memiliki delapan rank" }
        val board = MutableList<ChessPiece?>(64) { null }
        ranks.forEachIndexed { row, text ->
            var col = 0
            text.forEach { char ->
                if (char in '1'..'8') col += char.digitToInt()
                else {
                    require(char.isLetter()) { "Karakter bidak FEN tidak valid" }
                    val type = ChessPieceType.fromFen(char) ?: error("Jenis bidak FEN tidak dikenal")
                    require(col < 8) { "Rank FEN melebihi delapan kotak" }
                    val color = if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK
                    board[row * 8 + col] = ChessPiece(color, type)
                    col++
                }
                require(col <= 8) { "Rank FEN melebihi delapan kotak" }
            }
            require(col == 8) { "Setiap rank FEN harus berisi delapan kotak" }
        }
        require(board.count { it == ChessPiece(ChessColor.WHITE, ChessPieceType.KING) } == 1) { "Raja putih harus tepat satu" }
        require(board.count { it == ChessPiece(ChessColor.BLACK, ChessPieceType.KING) } == 1) { "Raja hitam harus tepat satu" }
        require((0..7).none { board[it]?.type == ChessPieceType.PAWN || board[56 + it]?.type == ChessPieceType.PAWN }) {
            "Pion tidak boleh berada di rank promosi"
        }
        val side = when (fields[1]) {
            "w" -> ChessColor.WHITE
            "b" -> ChessColor.BLACK
            else -> error("Giliran FEN harus w atau b")
        }
        val rightsField = fields[2]
        require(rightsField == "-" || (rightsField.all { it in "KQkq" } && rightsField.toSet().size == rightsField.length && rightsField.isNotEmpty())) {
            "Hak castling FEN tidak valid"
        }
        val rights = if (rightsField == "-") "-" else "KQkq".filter { it in rightsField }.ifBlank { "-" }
        if ('K' in rights) require(board[60] == ChessPiece(ChessColor.WHITE, ChessPieceType.KING) && board[63] == ChessPiece(ChessColor.WHITE, ChessPieceType.ROOK)) { "Hak O-O putih tidak sesuai posisi raja/benteng" }
        if ('Q' in rights) require(board[60] == ChessPiece(ChessColor.WHITE, ChessPieceType.KING) && board[56] == ChessPiece(ChessColor.WHITE, ChessPieceType.ROOK)) { "Hak O-O-O putih tidak sesuai posisi raja/benteng" }
        if ('k' in rights) require(board[4] == ChessPiece(ChessColor.BLACK, ChessPieceType.KING) && board[7] == ChessPiece(ChessColor.BLACK, ChessPieceType.ROOK)) { "Hak O-O hitam tidak sesuai posisi raja/benteng" }
        if ('q' in rights) require(board[4] == ChessPiece(ChessColor.BLACK, ChessPieceType.KING) && board[0] == ChessPiece(ChessColor.BLACK, ChessPieceType.ROOK)) { "Hak O-O-O hitam tidak sesuai posisi raja/benteng" }

        val ep = if (fields[3] == "-") null else parseSquare(fields[3]) ?: error("En-passant square tidak valid")
        if (ep != null) {
            require((side == ChessColor.WHITE && ep / 8 == 2) || (side == ChessColor.BLACK && ep / 8 == 5)) {
                "Rank en-passant tidak sesuai giliran"
            }
            require(board[ep] == null) { "En-passant square harus kosong" }
            val capturedIndex = ep + if (side == ChessColor.WHITE) 8 else -8
            val pawnStartIndex = ep + if (side == ChessColor.WHITE) -8 else 8
            require(board[capturedIndex] == ChessPiece(side.opposite(), ChessPieceType.PAWN) && board[pawnStartIndex] == null) {
                "Riwayat pion en-passant tidak konsisten"
            }
        }
        val halfmove = fields[4].toInt().also { require(it >= 0) }
        val fullmove = fields[5].toInt().also { require(it >= 1) }
        val position = ChessPosition(board, side, if (rights == "-") "-" else rights, ep, halfmove, fullmove)
        require(!ChessRules.isInCheck(position, side.opposite())) { "Raja yang baru saja bergerak tidak boleh tetap dalam check" }
        position.copy(positionHistory = listOf(positionKey(position)))
    }

    /** Repetition key follows the position fields that change legal move availability. */
    fun positionKey(position: ChessPosition): String {
        val placement = export(position).substringBefore(' ')
        val side = if (position.sideToMove == ChessColor.WHITE) "w" else "b"
        val rights = position.castlingRights
        val ep = position.enPassantSquare?.takeIf { target ->
            val color = position.sideToMove
            val sourceRow = target / 8 + if (color == ChessColor.WHITE) 1 else -1
            val sourceCols = listOf(target % 8 - 1, target % 8 + 1)
            sourceCols.any { col ->
                if (sourceRow !in 0..7 || col !in 0..7) false
                else {
                    val from = sourceRow * 8 + col
                    val pawn = position.board[from]
                    if (pawn != ChessPiece(color, ChessPieceType.PAWN)) false
                    else {
                        val board = position.board.toMutableList()
                        board[from] = null
                        board[target] = pawn
                        val captured = target + if (color == ChessColor.WHITE) 8 else -8
                        board[captured] = null
                        val trial = position.copy(board = board, sideToMove = color.opposite(), enPassantSquare = null)
                        !ChessRules.isInCheck(trial, color)
                    }
                }
            }
        }
        return "$placement $side $rights ${ep?.let(::squareName) ?: "-"}"
    }
}
