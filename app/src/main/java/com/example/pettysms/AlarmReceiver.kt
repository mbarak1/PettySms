package com.example.pettysms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Start your foreground service when the alarm is triggered
        val serviceIntent = Intent(context, SmsForegroundService::class.java)
        context.startService(serviceIntent)
    }
}