package com.github.irmin.chess.model

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    var hasMoved: Boolean = false
) {
    fun setHasMoved(moved: Boolean) {
        hasMoved = moved
    }
}

