package com.github.irmin.chess.model

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
)

