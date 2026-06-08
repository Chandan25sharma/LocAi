package com.locai.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LocAiDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var instance: LocAiDatabase? = null

        fun getInstance(context: Context): LocAiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocAiDatabase::class.java,
                    "locai.db"
                ).build().also { instance = it }
            }
    }
}
