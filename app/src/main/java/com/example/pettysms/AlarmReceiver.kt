package com.example.pettysms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlarmReceiver", "Alarm triggered")

        // Start the foreground service
        if(SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Schedule the SmsWorker to run in the background with proper constraints
            SmsWorker.schedule(context)
        }else{
            val serviceIntent = Intent(context, SmsForegroundService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }

    }
}