package com.github.irmin.chess.model

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = when (this) {
        WHITE -> BLACK
        BLACK -> WHITE
    }
}

