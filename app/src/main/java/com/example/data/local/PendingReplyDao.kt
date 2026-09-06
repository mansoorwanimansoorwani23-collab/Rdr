package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingReplyDao {
    @Query("SELECT * FROM pending_replies WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRepliesFlow(): Flow<List<PendingReplyEntity>>

    @Query("SELECT * FROM pending_replies ORDER BY timestamp DESC LIMIT 50")
    fun getAllRepliesFlow(): Flow<List<PendingReplyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingReply: PendingReplyEntity): Long

    @Update
    suspend fun update(pendingReply: PendingReplyEntity)

    @Query("UPDATE pending_replies SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE pending_replies SET suggestedReply = :newReply, status = :status WHERE id = :id")
    suspend fun updateReplyText(id: Long, newReply: String, status: String)

    @Query("DELETE FROM pending_replies WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_replies")
    suspend fun clearAll()
}
