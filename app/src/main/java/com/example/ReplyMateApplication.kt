package com.example

import android.app.Application
import com.example.ai.AIEngine
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.security.SecureKeyStorage
import com.example.service.MessageFilter
import com.example.service.NotificationHelper

class ReplyMateApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var secureKeyStorage: SecureKeyStorage
        private set
    lateinit var aiEngine: AIEngine
        private set
    lateinit var messageFilter: MessageFilter
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferencesRepository = PreferencesRepository(this)
        secureKeyStorage = SecureKeyStorage(this)
        aiEngine = AIEngine(secureKeyStorage, database)
        messageFilter = MessageFilter(database)

        NotificationHelper.createNotificationChannels(this)
    }

    companion object {
        lateinit var instance: ReplyMateApplication
            private set
    }
}
