package com.example.pettysms

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SmsWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SmsWorker"
        
        // Function to schedule the worker with proper constraints
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresStorageNotLow(false)
                .build()
                
            val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("sms_processing")
                .build()
                
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "sms_worker",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            
            Log.d(TAG, "SmsWorker scheduled with sleep-friendly constraints")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val isMpesaLaunched = prefs.getBoolean("mpesa_first_launch", false)
        Log.d(TAG, "isFirstLaunch: $isMpesaLaunched")

        if (isMpesaLaunched) {
            // Skip processing if it's the first launch
            Log.d(TAG, "Skipping SMS processing for the first launch")
            return@withContext Result.success()
        }

        try {
            Log.d(TAG, "Notifying that SMS processing is starting")

            val sms = ArrayList<MutableList<String>>()
            val lstSms: MutableList<String> = ArrayList()
            val lstRcvr: MutableList<String> = ArrayList()
            val lstDate: MutableList<String> = ArrayList()
            val lstId: MutableList<String> = ArrayList()
            val smsServiceHelper = SmsServiceHelper()

            val cr: ContentResolver = applicationContext.contentResolver
            if (ContextCompat.checkSelfPermission(applicationContext, "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
                val cursor = cr.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox._ID),
                    Telephony.Sms.ADDRESS + " = ?",
                    arrayOf("MPESA"),
                    Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
                )

                val dbHelper = DbHelper(applicationContext)
                val db = dbHelper.writableDatabase

                cursor?.use {
                    while (it.moveToNext()) {
                        if (!dbHelper.isSmsExists(db, it.getString(0), it.getString(2).toLong())) {
                            lstSms.add(it.getString(0))
                            lstRcvr.add(it.getString(1))
                            lstDate.add(it.getString(2))
                            lstId.add(it.getString(3))
                        }
                    }
                }

                sms.add(lstSms)
                sms.add(lstRcvr)
                sms.add(lstDate)
                sms.add(lstId)

                if (sms.all { it.isNotEmpty() }) {
                    smsServiceHelper.processSms(sms, applicationContext)
                }

                db.close()
            } else {
                Log.e(TAG, "SMS read permission not granted")
            }

            Result.success() // Indicate successful completion
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            // Return retry for transient errors, but success for permission issues
            if (e is SecurityException || e is IllegalStateException) {
                return@withContext Result.failure() // Don't retry for permission issues
            }
            return@withContext Result.retry() // Retry for other errors
        }
    }
}
