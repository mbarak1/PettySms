package com.example.pettysms

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.pettysms.queue.QuickBooksWorker
import com.google.android.material.color.DynamicColors
//import leakcanary.LeakCanary

class App: Application() {
    companion object {
        private const val TAG = "App"
    }
    
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        // Create notification channel
        createNotificationChannel()
        
        // Initialize QuickBooksWorker at application startup
        QuickBooksWorker.initializeAtStartup(this)
        Log.d(TAG, "QuickBooks sync worker initialized at application startup")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel_1",
                "Channel 1",
                NotificationManager.IMPORTANCE_HIGH // Use IMPORTANCE_HIGH for important notifications
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

}