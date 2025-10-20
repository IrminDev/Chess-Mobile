package com.github.irmin.chess.model

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7

    operator fun plus(other: Position): Position = Position(row + other.row, col + other.col)
}

