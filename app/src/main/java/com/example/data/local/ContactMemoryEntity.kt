package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_memories")
data class ContactMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactKey: String, // Contact Name or normalized phone
    val memoryFact: String, // e.g. "Lives in Bangalore, prefers coffee meetings on weekends"
    val timestamp: Long = System.currentTimeMillis()
)
