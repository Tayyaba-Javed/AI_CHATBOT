package com.example.aichatbot.database

import android.content.Context
import androidx.room.Room

object DbHandler {
    fun getDb(context: Context): ChatDb {
        return Room.databaseBuilder(
            context,
            ChatDb::class.java,
            "ChatDb"
        )
            .allowMainThreadQueries()
            .build()
    }
}
