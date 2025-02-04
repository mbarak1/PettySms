package com.example.pettysms

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val isMpesaLaunched = prefs.getBoolean("mpesa_first_launch", false)
        Log.d("SmsWorker", "isFirstLaunch: $isMpesaLaunched")

        if (!isMpesaLaunched) {
            // Skip processing if it's the first launch
            Log.d("SmsWorker", "Skipping SMS processing for the first launch")
            return@withContext Result.success()
        }

        try {
            Log.d("SmsWorker", "Notifying that SMS processing is starting")

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
                Log.e("SmsWorker", "SMS read permission not granted")
            }

            Result.success() // Indicate successful completion
        } catch (e: Exception) {
            Log.e("SmsWorker", "Error processing SMS", e)
            Result.failure() // Indicate a failure occurred
        }
    }
}
