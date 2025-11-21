package com.github.irmin.chess.model

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    /**
     * Método para crear una copia con hasMoved modificado (usado en multijugador)
     */
    fun withHasMoved(moved: Boolean): ChessPiece {
        return copy(hasMoved = moved)
    }
}
