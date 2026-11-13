package com.rexaps.rexgames.catur.bot

import com.rexaps.rexgames.catur.engine.ChessColor
import com.rexaps.rexgames.catur.engine.ChessMove
import com.rexaps.rexgames.catur.engine.ChessPieceType
import com.rexaps.rexgames.catur.engine.ChessPosition
import com.rexaps.rexgames.catur.engine.ChessRules
import com.rexaps.rexgames.catur.engine.ChessFen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** The three user-facing levels have explicit time/depth budgets; no random legal-move fallback is used. */
enum class ChessDifficulty(val label: String, val maxDepth: Int, val thinkBudgetMs: Long) {
    EASY("Easy", 2, 300L),
    MEDIUM("Medium", 4, 1_000L),
    HARD("Hard", 64, 2_800L)
}

interface ChessBot {
    suspend fun chooseMove(position: ChessPosition, difficulty: ChessDifficulty): ChessMove?
}

/** Deterministic offline alpha-beta engine. It is deliberately not advertised as Stockfish-strength. */
class AlphaBetaChessBot : ChessBot {
    override suspend fun chooseMove(position: ChessPosition, difficulty: ChessDifficulty): ChessMove? {
        val context = currentCoroutineContext()
        return withContext(Dispatchers.Default) {
            val legal = ChessRules.legalMoves(position)
            if (legal.isEmpty()) return@withContext null
            val deadline = System.nanoTime() + difficulty.thinkBudgetMs * 1_000_000L
            val search = Search(deadline) { context.ensureActive() }
            search.bestMove(position, difficulty.maxDepth, legal)
        }
    }

    private class Search(private val deadline: Long, private val ensureActive: () -> Unit) {
        private var nodes = 0L

        fun bestMove(position: ChessPosition, maxDepth: Int, legalMoves: List<ChessMove>): ChessMove {
            var best = orderMoves(position, legalMoves).first()
            for (depth in 1..maxDepth) {
                try {
                    ensureActive()
                    checkDeadline()
                    val candidate = root(position, depth, legalMoves)
                    best = candidate
                } catch (_: SearchTimeout) {
                    break
                }
            }
            return best
        }

        private fun root(position: ChessPosition, depth: Int, moves: List<ChessMove>): ChessMove {
            var alpha = -MATE_SCORE
            var bestScore = -MATE_SCORE
            var bestMove = moves.first()
            for (move in orderMoves(position, moves)) {
                tick()
                val next = ChessRules.applyGeneratedMove(position, move) ?: continue
                val score = -negamax(next, depth - 1, -MATE_SCORE, -alpha, 1)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
                alpha = maxOf(alpha, score)
            }
            return bestMove
        }

        private fun negamax(position: ChessPosition, depth: Int, alphaInput: Int, beta: Int, ply: Int): Int {
            tick()
            if (position.halfmoveClock >= 100 || position.positionHistory.count { it == ChessFen.positionKey(position) } >= 3 ||
                ChessRules.isInsufficientMaterial(position)
            ) return 0

            val moves = ChessRules.legalMoves(position)
            if (moves.isEmpty()) return if (ChessRules.isInCheck(position, position.sideToMove)) -MATE_SCORE + ply else 0
            if (depth <= 0) return evaluate(position)

            var alpha = alphaInput
            var bestScore = -MATE_SCORE
            for (move in orderMoves(position, moves)) {
                val next = ChessRules.applyGeneratedMove(position, move) ?: continue
                val score = -negamax(next, depth - 1, -beta, -alpha, ply + 1)
                if (score > bestScore) bestScore = score
                alpha = maxOf(alpha, score)
                if (alpha >= beta) break
            }
            return bestScore
        }

        private fun evaluate(position: ChessPosition): Int {
            var score = 0
            position.board.forEachIndexed { index, piece ->
                piece ?: return@forEachIndexed
                val row = index / 8
                val col = index % 8
                val advance = if (piece.color == ChessColor.WHITE) 6 - row else row - 1
                val centerDistance = kotlin.math.abs(3.5f - col) + kotlin.math.abs(3.5f - row)
                val positional = when (piece.type) {
                    ChessPieceType.PAWN -> advance * 7 + (3.5f - kotlin.math.abs(3.5f - col)).toInt() * 2
                    ChessPieceType.KNIGHT -> (7f - centerDistance).toInt() * 8
                    ChessPieceType.BISHOP -> (7f - centerDistance).toInt() * 4
                    ChessPieceType.ROOK -> advance * 2
                    ChessPieceType.QUEEN -> (7f - centerDistance).toInt() * 2
                    ChessPieceType.KING -> if (position.fullmoveNumber < 20) -centerDistance.toInt() * 3 else (7f - centerDistance).toInt() * 3
                }
                val value = piece.type.value + positional
                score += if (piece.color == ChessColor.WHITE) value else -value
            }
            return if (position.sideToMove == ChessColor.WHITE) score else -score
        }

        private fun orderMoves(position: ChessPosition, moves: List<ChessMove>): List<ChessMove> = moves.sortedByDescending { move ->
            val attacker = position.board[move.from]
            val victim = position.board[move.to]
            (victim?.type?.value ?: 0) * 10 - (attacker?.type?.value ?: 0) +
                (move.promotion?.value ?: 0) * 3
        }

        private fun tick() {
            nodes++
            if ((nodes and 255L) == 0L) {
                ensureActive()
                checkDeadline()
            }
        }

        private fun checkDeadline() {
            if (System.nanoTime() >= deadline) throw SearchTimeout
        }

        private object SearchTimeout : RuntimeException()
        private companion object { const val MATE_SCORE = 1_000_000 }
    }
}
