package com.example.aichatbot.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insertMessage(chatTable: ChatTable)

    @Query("SELECT * FROM userChat ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<ChatTable>

    @Query("SELECT * FROM userChat ORDER BY timestamp ASC")
    fun getAllMessagesAsFlow(): Flow<List<ChatTable>>

    @Query("DELETE FROM userChat")
    suspend fun clearMessages()
}