package com.example.cpuschedgame

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_scores")
data class PlayerScore(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,           // which player this score belongs to
    val score: Int,
    val algorithm: String,
    val completedProcesses: Int,
    val timeElapsed: Float,
    val rating: String,
    val timestamp: Long = System.currentTimeMillis()
)