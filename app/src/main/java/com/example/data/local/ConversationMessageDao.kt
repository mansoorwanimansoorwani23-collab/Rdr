package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_context WHERE conversationKey = :key ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRecentContext(key: String, limit: Int = 5): List<ConversationMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationMessageEntity)

    @Query("DELETE FROM conversation_context WHERE conversationKey = :key")
    suspend fun clearContextForConversation(key: String)

    @Query("DELETE FROM conversation_context")
    suspend fun clearAll()

    @Query("DELETE FROM conversation_context WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
