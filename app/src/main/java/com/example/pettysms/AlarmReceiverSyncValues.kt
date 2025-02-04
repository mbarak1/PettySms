package com.example.pettysms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiverSyncValues : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val workRequest = OneTimeWorkRequestBuilder<SyncMainPettyCashValues>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}