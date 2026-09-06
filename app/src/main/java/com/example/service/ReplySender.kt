package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.util.concurrent.ConcurrentHashMap

object ReplySender {

    // Cache active reply action per conversationKey
    private val actionCache = ConcurrentHashMap<String, Pair<Notification.Action, RemoteInput>>()

    fun cacheAction(conversationKey: String, action: Notification.Action, remoteInput: RemoteInput) {
        actionCache[conversationKey] = Pair(action, remoteInput)
    }

    fun hasCachedAction(conversationKey: String): Boolean {
        return actionCache.containsKey(conversationKey)
    }

    fun removeAction(conversationKey: String) {
        actionCache.remove(conversationKey)
    }

    fun sendReply(
        context: Context,
        conversationKey: String,
        replyText: String,
        action: Notification.Action? = null,
        remoteInput: RemoteInput? = null
    ): Boolean {
        val targetPair = if (action != null && remoteInput != null) {
            Pair(action, remoteInput)
        } else {
            actionCache[conversationKey]
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
