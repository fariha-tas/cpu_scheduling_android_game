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

    /** Best score the given user achieved on a specific level. */
    @Query("SELECT MAX(score) FROM player_scores WHERE username = :username AND level = :level")
    fun getBestScoreForUserAndLevel(username: String, level: String): Flow<Int?>

    /** Total number of games played by this user. */
    @Query("SELECT COUNT(*) FROM player_scores WHERE username = :username")
    fun getGameCountForUser(username: String): Flow<Int>

    /** All scores for a user, newest first. */
    @Query("SELECT * FROM player_scores WHERE username = :username ORDER BY timestamp DESC LIMIT 20")
    fun getScoresForUser(username: String): Flow<List<PlayerScore>>

    @Query("DELETE FROM player_scores")
    suspend fun clearAll()
}