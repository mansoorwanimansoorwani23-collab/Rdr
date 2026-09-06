package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyLogDao {
    @Query("SELECT * FROM reply_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogsFlow(): Flow<List<ReplyLogEntity>>

    @Query("SELECT COUNT(*) FROM reply_logs WHERE timestamp >= :startOfDayTimestamp AND (status = 'AUTO_SENT' OR status = 'MANUAL_SENT')")
    fun getTodayReplyCountFlow(startOfDayTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM reply_logs WHERE timestamp >= :startOfDayTimestamp AND (status = 'AUTO_SENT' OR status = 'MANUAL_SENT')")
    suspend fun getTodayReplyCount(startOfDayTimestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ReplyLogEntity): Long

    @Query("DELETE FROM reply_logs")
    suspend fun clearAll()
}
