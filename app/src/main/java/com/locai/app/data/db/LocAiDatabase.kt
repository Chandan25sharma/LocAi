package com.locai.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LocAiDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var instance: LocAiDatabase? = null

        /** Adds the nullable column that lets a message carry an attached/generated image. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN imagePath TEXT")
            }
        }

        fun getInstance(context: Context): LocAiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocAiDatabase::class.java,
                    "locai.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
