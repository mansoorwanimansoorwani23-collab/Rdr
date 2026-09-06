package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PendingReplyEntity::class,
        ConversationMessageEntity::class,
        ReplyLogEntity::class,
        ContactMemoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingReplyDao(): PendingReplyDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun replyLogDao(): ReplyLogDao
    abstract fun contactMemoryDao(): ContactMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "replymate_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
