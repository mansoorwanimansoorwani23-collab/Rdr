package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class BatchedMessage(
    val conversationKey: String,
    val senderName: String,
    val messages: MutableList<String> = mutableListOf(),
    var lastReceivedTimestamp: Long = System.currentTimeMillis()
)

class SmartMessageBatcher(
    private val coroutineScope: CoroutineScope,
    private val onBatchReady: suspend (conversationKey: String, senderName: String, combinedMessage: String) -> Unit
) {
    private val batches = ConcurrentHashMap<String, BatchedMessage>()
    private val timerJobs = ConcurrentHashMap<String, Job>()

    fun addMessage(
        conversationKey: String,
        senderName: String,
        messageText: String,
        windowSeconds: Int
    ) {
        val batch = batches.getOrPut(conversationKey) {
            BatchedMessage(conversationKey = conversationKey, senderName = senderName)
        }

        synchronized(batch) {
            batch.messages.add(messageText)
            batch.lastReceivedTimestamp = System.currentTimeMillis()
        }

        // Cancel previous timer for this conversation and set a new one
        timerJobs[conversationKey]?.cancel()
        timerJobs[conversationKey] = coroutineScope.launch {
            delay(windowSeconds * 1000L)
            emitBatch(conversationKey)
        }
    }

    private suspend fun emitBatch(conversationKey: String) {
        val batch = batches.remove(conversationKey) ?: return
        timerJobs.remove(conversationKey)

        val combined = synchronized(batch) {
            batch.messages.joinToString("\n")
        }

        if (combined.isNotBlank()) {
            onBatchReady(conversationKey, batch.senderName, combined)
        }
    }
}
