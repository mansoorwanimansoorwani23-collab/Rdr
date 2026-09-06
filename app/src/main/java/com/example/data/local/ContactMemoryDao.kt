package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactMemoryDao {
    @Query("SELECT * FROM contact_memories WHERE contactKey = :contactKey ORDER BY timestamp DESC LIMIT 10")
    suspend fun getMemoriesForContact(contactKey: String): List<ContactMemoryEntity>

    @Query("SELECT * FROM contact_memories WHERE contactKey = :contactKey ORDER BY timestamp DESC")
    fun observeMemoriesForContact(contactKey: String): Flow<List<ContactMemoryEntity>>

    @Query("SELECT * FROM contact_memories ORDER BY timestamp DESC")
    fun observeAllMemories(): Flow<List<ContactMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: ContactMemoryEntity): Long

    @Query("DELETE FROM contact_memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM contact_memories WHERE contactKey = :contactKey")
    suspend fun clearMemoriesForContact(contactKey: String)

    @Query("DELETE FROM contact_memories")
    suspend fun clearAllMemories()
}
