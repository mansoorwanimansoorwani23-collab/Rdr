package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.util.concurrent.ConcurrentHashMap

object ReplySender {

    // Cache active reply action per conversationKey and senderName
    private val actionCache = ConcurrentHashMap<String, Pair<Notification.Action, RemoteInput>>()

    fun cacheAction(
        conversationKey: String,
        action: Notification.Action,
        remoteInput: RemoteInput,
        senderName: String? = null
    ) {
        val pair = Pair(action, remoteInput)
        actionCache[conversationKey] = pair
        if (!senderName.isNullOrBlank()) {
            actionCache[senderName] = pair
        }
    }

    fun hasCachedAction(conversationKey: String, senderName: String? = null): Boolean {
        if (actionCache.containsKey(conversationKey)) return true
        if (!senderName.isNullOrBlank() && actionCache.containsKey(senderName)) return true
        return false
    }

    fun anyCachedActionAvailable(): Boolean {
        return actionCache.isNotEmpty()
    }

    fun removeAction(conversationKey: String, senderName: String? = null) {
        actionCache.remove(conversationKey)
        if (!senderName.isNullOrBlank()) {
            actionCache.remove(senderName)
        }
    }

    fun sendReply(
        context: Context,
        conversationKey: String,
        replyText: String,
        action: Notification.Action? = null,
        remoteInput: RemoteInput? = null,
        senderName: String? = null
    ): Boolean {
        val targetPair = if (action != null && remoteInput != null) {
            Pair(action, remoteInput)
        } else {
            actionCache[conversationKey] ?: if (!senderName.isNullOrBlank()) actionCache[senderName] else null
        } ?: return false

        val targetAction = targetPair.first
        val targetRemoteInput = targetPair.second

        return try {
            val intent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(targetRemoteInput.resultKey, replyText)
            RemoteInput.addResultsToIntent(arrayOf(targetRemoteInput), intent, bundle)
            targetAction.actionIntent.send(context, 0, intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
