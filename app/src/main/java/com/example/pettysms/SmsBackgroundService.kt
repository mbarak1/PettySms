package com.example.pettysms

import android.app.*
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class SmsBackgroundService : Service() {

    private lateinit var serviceJob: Job

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceJob = Job()
        val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

        serviceScope.launch {
            checkAndInsertNewSms()
        }

        return START_STICKY // Adjust based on your needs
    }

    private fun checkAndInsertNewSms() {
        val sms = ArrayList<MutableList<String>>()
        val lstSms: MutableList<String> = ArrayList()
        val lstRcvr: MutableList<String> = ArrayList()
        val lstDate: MutableList<String> = ArrayList()
        val lstId: MutableList<String> = ArrayList()
        val smsServiceHelper = SmsServiceHelper()

        val cr: ContentResolver = contentResolver
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            val cursor = cr.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox._ID),
                Telephony.Sms.ADDRESS + " = ?",
                arrayOf("MPESA"),
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
            )

            val dbHelper = DbHelper(this)
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
                smsServiceHelper.processSms(sms, this)
            }

            db.close()
        } else {
            Log.e("SmsBackgroundService", "SMS read permission not granted")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel any ongoing coroutines
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}