package com.example.cpuschedgame

import androidx.room.*

@Dao
interface UserDao {

    // Called during signup — inserts a new user
    @Insert
    suspend fun insertUser(user: User)

    // Called during login — finds user by username only
    // We check password manually after hashing
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    // Check if a username is already taken during signup
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun usernameExists(username: String): Int
}