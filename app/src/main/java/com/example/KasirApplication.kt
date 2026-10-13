package com.example

import android.app.Application
import com.example.data.local.KasirDatabase
import com.example.data.repository.KasirRepository
import com.example.data.sync.CloudSyncManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KasirApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy {
        KasirDatabase.getDatabase(this, applicationScope)
    }

    val repository by lazy {
        KasirRepository(database)
    }

    val cloudSyncManager by lazy {
        CloudSyncManager(this, repository, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:abcdef")
                    .setApiKey("fake_api_key")
                    .setProjectId("fake-project-id")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            // Ignore if unable to initialize mock FirebaseApp
        }
        NotificationHelper.initNotificationChannel(this)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
            }
        } catch (_: Exception) {}
        repository.syncManager = cloudSyncManager
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    cloudSyncManager.startRealtimeSync(user.uid)
                }
            }
        } catch (e: Exception) {
            // Safeguard for testing environments where Firebase is not initialized
        }
        cloudSyncManager.triggerAutoSync()
    }
}
