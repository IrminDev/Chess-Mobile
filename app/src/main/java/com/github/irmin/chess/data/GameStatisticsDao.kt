package com.github.irmin.chess.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatisticsDao {
    @Query("SELECT * FROM game_statistics ORDER BY id DESC LIMIT 1")
    fun getStatistics(): Flow<GameStatistics?>
    
    @Query("SELECT * FROM game_statistics ORDER BY id DESC LIMIT 1")
    suspend fun getStatisticsOnce(): GameStatistics?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(statistics: GameStatistics): Long
    
    @Update
    suspend fun updateStatistics(statistics: GameStatistics)
    
    @Query("DELETE FROM game_statistics")
    suspend fun deleteAllStatistics()
    
    @Query("SELECT COUNT(*) FROM game_statistics")
    suspend fun getCount(): Int
}
