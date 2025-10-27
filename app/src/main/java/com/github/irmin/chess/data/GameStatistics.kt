package com.github.irmin.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_statistics")
data class GameStatistics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gamesWonWhite: Int = 0,
    val gamesWonBlack: Int = 0,
    val gamesDrawn: Int = 0,
    val totalPlayTime: Long = 0, // En milisegundos
    val longestGame: Long = 0,    // En milisegundos
    val shortestGame: Long = 0,   // En milisegundos
    val totalMoves: Int = 0,
    val lastPlayed: Long = System.currentTimeMillis()
) {
    val totalGames: Int
        get() = gamesWonWhite + gamesWonBlack + gamesDrawn
        
    val winRateWhite: Float
        get() = if (totalGames > 0) (gamesWonWhite.toFloat() / totalGames) * 100 else 0f
        
    val winRateBlack: Float
        get() = if (totalGames > 0) (gamesWonBlack.toFloat() / totalGames) * 100 else 0f
        
    val averageGameTime: Long
        get() = if (totalGames > 0) totalPlayTime / totalGames else 0
}
