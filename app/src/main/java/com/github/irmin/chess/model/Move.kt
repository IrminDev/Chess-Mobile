package com.github.irmin.chess.model

data class Move(
    val from: Position,
    val to: Position,
    val capturedPiece: ChessPiece? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val rookMove: Move? = null
)

