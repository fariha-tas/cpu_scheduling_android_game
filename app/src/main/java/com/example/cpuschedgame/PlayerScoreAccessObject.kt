package com.example.cpuschedgame

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerScoreDao {

    @Insert
    suspend fun insertScore(score: PlayerScore)

    @Query("SELECT * FROM player_scores ORDER BY score DESC LIMIT 10")
    fun getTopScores(): Flow<List<PlayerScore>>

    @Query("SELECT MAX(score) FROM player_scores")
    fun getHighScore(): Flow<Int?>

    @Query("DELETE FROM player_scores")
    suspend fun clearAll()
}