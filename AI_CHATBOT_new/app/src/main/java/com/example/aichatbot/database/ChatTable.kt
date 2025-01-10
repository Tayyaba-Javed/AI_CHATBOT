package com.example.aichatbot.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userChat")
data class ChatTable(
    @PrimaryKey(autoGenerate =true) val id: Int = 0,
    @ColumnInfo(name = "message") val message:String,
    @ColumnInfo(name = "is_sender") val isSender:Boolean,
    @ColumnInfo(name = "timestamp") val timestamp:Long =System.currentTimeMillis()


)