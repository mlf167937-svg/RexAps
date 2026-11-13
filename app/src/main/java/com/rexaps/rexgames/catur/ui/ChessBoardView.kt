package com.rexaps.rexgames.catur.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexgames.catur.engine.ChessColor
import com.rexaps.rexgames.catur.engine.ChessMove
import com.rexaps.rexgames.catur.engine.ChessPiece
import com.rexaps.rexgames.catur.engine.ChessPieceType
import com.rexaps.rexgames.catur.engine.ChessPosition
import com.rexaps.rexgames.catur.engine.ChessRules
import com.rexaps.rexgames.catur.engine.squareName

@Composable
fun ChessBoardView(
    position: ChessPosition,
    selectedSquare: Int?,
    legalMoves: List<ChessMove>,
    lastMove: ChessMove?,
    flipped: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSquareClick: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val checkedKing = if (ChessRules.isInCheck(position, position.sideToMove)) {
        position.board.indexOfFirst { piece -> piece == ChessPiece(position.sideToMove, ChessPieceType.KING) }
    } else -1
    val legalTargets = legalMoves.map { it.to }.toSet()
    val lastSquares = setOfNotNull(lastMove?.from, lastMove?.to)
    val squares = if (flipped) (63 downTo 0).toList() else (0..63).toList()

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        squares.chunked(8).forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { square ->
                    val chessRow = square / 8
                    val chessCol = square % 8
                    val piece = position.board[square]
                    val selected = selectedSquare == square
                    val isLegalTarget = square in legalTargets
                    val isLastMove = square in lastSquares
                    val isChecked = square == checkedKing
                    val base = if ((chessRow + chessCol) % 2 == 0) scheme.surfaceVariant else scheme.secondaryContainer
                    val background = when {
                        isChecked -> scheme.errorContainer
                        selected -> scheme.tertiaryContainer
                        isLastMove -> scheme.primaryContainer.copy(alpha = 0.82f)
                        else -> base
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(background)
                            .then(
                                if (selected) Modifier.border(2.dp, scheme.primary)
                                else Modifier
                            )
                            .semantics {
                                contentDescription = buildString {
                                    append(squareName(square))
                                    append(if (piece == null) ", kosong" else ", ${pieceDescription(piece)}")
                                    if (selected) append(", dipilih")
                                    if (isLegalTarget) append(", tujuan legal")
                                    if (isChecked) append(", raja sedang check")
                                }
                            }
                            .clickable(enabled = enabled, role = Role.Button) { onSquareClick(square) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (piece != null) {
                            Text(
                                text = pieceGlyph(piece),
                                color = if (piece.color == ChessColor.WHITE) scheme.onSurface else scheme.onPrimaryContainer,
                                fontSize = 31.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (isLegalTarget) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary.copy(alpha = 0.72f))
                            )
                        }
                        if (piece != null && isLegalTarget) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.88f)
                                    .border(2.dp, scheme.primary.copy(alpha = 0.78f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun pieceGlyph(piece: ChessPiece): String = when (piece.color) {
    ChessColor.WHITE -> when (piece.type) {
        ChessPieceType.KING -> "♔"
        ChessPieceType.QUEEN -> "♕"
        ChessPieceType.ROOK -> "♖"
        ChessPieceType.BISHOP -> "♗"
        ChessPieceType.KNIGHT -> "♘"
        ChessPieceType.PAWN -> "♙"
    }
    ChessColor.BLACK -> when (piece.type) {
        ChessPieceType.KING -> "♚"
        ChessPieceType.QUEEN -> "♛"
        ChessPieceType.ROOK -> "♜"
        ChessPieceType.BISHOP -> "♝"
        ChessPieceType.KNIGHT -> "♞"
        ChessPieceType.PAWN -> "♟"
    }
}

private fun pieceDescription(piece: ChessPiece): String {
    val color = if (piece.color == ChessColor.WHITE) "putih" else "hitam"
    val name = when (piece.type) {
        ChessPieceType.KING -> "raja"
        ChessPieceType.QUEEN -> "menteri"
        ChessPieceType.ROOK -> "benteng"
        ChessPieceType.BISHOP -> "gajah"
        ChessPieceType.KNIGHT -> "kuda"
        ChessPieceType.PAWN -> "pion"
    }
    return "$name $color"
}
