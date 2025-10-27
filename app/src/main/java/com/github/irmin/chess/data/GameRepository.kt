package com.github.irmin.chess.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: GameStatisticsDao) {
    
    val statistics: Flow<GameStatistics?> = dao.getStatistics()
    
    suspend fun getStatisticsOnce(): GameStatistics? {
        return dao.getStatisticsOnce()
    }
    
    suspend fun updateGameWon(winner: String, gameTime: Long, movesCount: Int) {
        val current = dao.getStatisticsOnce() ?: GameStatistics()
        
        val updated = when (winner.lowercase()) {
            "white" -> current.copy(
                gamesWonWhite = current.gamesWonWhite + 1,
                totalPlayTime = current.totalPlayTime + gameTime,
                totalMoves = current.totalMoves + movesCount,
                longestGame = maxOf(current.longestGame, gameTime),
                shortestGame = if (current.shortestGame == 0L) gameTime else minOf(current.shortestGame, gameTime),
                lastPlayed = System.currentTimeMillis()
            )
            "black" -> current.copy(
                gamesWonBlack = current.gamesWonBlack + 1,
                totalPlayTime = current.totalPlayTime + gameTime,
                totalMoves = current.totalMoves + movesCount,
                longestGame = maxOf(current.longestGame, gameTime),
                shortestGame = if (current.shortestGame == 0L) gameTime else minOf(current.shortestGame, gameTime),
                lastPlayed = System.currentTimeMillis()
            )
            else -> current.copy(
                gamesDrawn = current.gamesDrawn + 1,
                totalPlayTime = current.totalPlayTime + gameTime,
                totalMoves = current.totalMoves + movesCount,
                longestGame = maxOf(current.longestGame, gameTime),
                shortestGame = if (current.shortestGame == 0L) gameTime else minOf(current.shortestGame, gameTime),
                lastPlayed = System.currentTimeMillis()
            )
        }
        
        if (current.id == 0L) {
            dao.insertStatistics(updated)
        } else {
            dao.updateStatistics(updated.copy(id = current.id))
        }
    }
    
    suspend fun resetStatistics() {
        dao.deleteAllStatistics()
    }
}
