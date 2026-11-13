package com.rexaps.rexgames.catur.engine

object PgnManager {
    fun export(state: ChessGameState, white: String = "White", black: String = "Black", event: String = "RexGames Chess"): String {
        val result = when (state.status) {
            ChessGameStatus.CHECKMATE -> if (state.winner == ChessColor.WHITE) "1-0" else "0-1"
            ChessGameStatus.STALEMATE, ChessGameStatus.DRAW_THREEFOLD,
            ChessGameStatus.DRAW_FIFTY_MOVE, ChessGameStatus.DRAW_INSUFFICIENT_MATERIAL -> "1/2-1/2"
            ChessGameStatus.RESIGNED -> if (state.winner == ChessColor.WHITE) "1-0" else if (state.winner == ChessColor.BLACK) "0-1" else "*"
            else -> "*"
        }
        val headers = listOf(
            "[Event \"${escape(event)}\"]",
            "[White \"${escape(white)}\"]",
            "[Black \"${escape(black)}\"]",
            "[Result \"$result\"]",
            "[SetUp \"1\"]",
            "[FEN \"${ChessFen.INITIAL}\"]"
        )
        val notation = buildList {
            state.sanMoves.forEachIndexed { index, san ->
                if (index % 2 == 0) add("${index / 2 + 1}.")
                add(san)
            }
            add(result)
        }.joinToString(" ")
        return headers.joinToString("\n") + "\n\n" + notation
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace('\n', ' ')
}
