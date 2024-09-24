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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SmsForegroundService : Service() {

    private val NOTIFICATION_CHANNEL_ID = "SmsForegroundServiceChannel"
    // Declare a variable to hold the callback interface
    private var refreshRecyclerViewCallback: RefreshRecyclerViewCallback? = null

    // Method to set the callback interface
    fun setRefreshRecyclerViewCallback(callback: RefreshRecyclerViewCallback) {
        refreshRecyclerViewCallback = callback
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())

        GlobalScope.launch(Dispatchers.IO) {
            // Implement your logic to check for new SMS and insert into the database
            println("ndo laanza")
            checkAndInsertNewSms()
        }

        return START_STICKY
    }

    private fun checkAndInsertNewSms() {
        val sms = ArrayList<MutableList<String>>()
        val lstSms: MutableList<String> = ArrayList()
        val lstRcvr: MutableList<String> = ArrayList()
        var lstDate: MutableList<String> = ArrayList()
        var lstId: MutableList<String> = ArrayList()
        var smsServiceHelper = SmsServiceHelper()

        // Get SharedPreferences using the provided context
        val prefs = this.getSharedPreferences("YourPrefsName", Context.MODE_PRIVATE)
        val isForegroundServiceRunning = prefs.getBoolean("isForegroundServiceRunning", false)

        val cr: ContentResolver = contentResolver
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            val c = cr.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox._ID),
                Telephony.Sms.ADDRESS + " = ?",
                arrayOf("MPESA"),
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
            )

            val dbHelper = DbHelper(this)
            val db = dbHelper.writableDatabase

            try {
                if (c != null && c.moveToFirst()) {
                    do {
                        println()
                        if (!dbHelper.isSmsExists(db, c.getString(0), c.getString(2).toLong())) {
                            if (!isForegroundServiceRunning){
                                println("balaa" + c.getString(0))
                                lstSms.add(c.getString(0))
                                lstRcvr.add(c.getString(1))
                                lstDate.add(c.getString(2))
                                lstId.add(c.getString(3))
                            }
                        }
                    } while (c.moveToNext())
                }


                sms.add(lstSms)
                sms.add(lstRcvr)
                sms.add(lstDate)
                sms.add(lstId)

                println("watha ushoga" + sms.toString())

                if (!sms[0].isNullOrEmpty() && !sms[1].isNullOrEmpty() && !sms[2].isNullOrEmpty() && !sms[3].isNullOrEmpty()){
                    smsServiceHelper.processSms(sms, this)
                }

            } finally {
                c?.close()
                db.close()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE  // Add FLAG_IMMUTABLE here
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SmsForegroundService")
            .setContentText("Foreground service is running")
            .setSmallIcon(R.drawable.p_logo_cropped)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "SmsForegroundService Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

}

class SmsServiceHelper() {
    fun processSms(sms: ArrayList<MutableList<String>>, context: Context) {
        println(sms)
        val dbHelper = DbHelper(context)
        val db = dbHelper.writableDatabase
        val data = MpesaTransaction.convertSmstoMpesaTransactions(sms, null)
        var result = data.transactionsList
        var rejectedSms = data.rejectedSmsList

        println("balaa zu" + result.toString())

        addToDbTransactions(result, context)
        addtoDbRejectedSms(rejectedSms, context)

        try {
            println("Before adding to all sms")
            println(sms[0].size == sms[2].size)
            println(sms[2].size == sms[3].size)

            if (sms[0].size == sms[2].size && sms[2].size == sms[3].size) {
                println("after adding to all_sms")
                var i = 0
                while (i < sms[0].size) {
                    println("after after adding to all_sms")
                    dbHelper.insertSms(sms[3][i].toLong(), sms[0][i], sms[2][i].toLong())
                    i++
                }
            } else {
                // Log an error or handle the case where arrays have different lengths
                Log.e("InsertSms", "Arrays have different lengths")
            }
        } finally {
            db?.close()
        }

    }

    private fun addtoDbRejectedSms(rejectedSms: MutableList<MutableList<String>>, context: Context) {
        val dbHelper = DbHelper(context)
        val db = dbHelper.writableDatabase
        val dateFrmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

        if (db != null) {
            for (rejectedMessage in rejectedSms) {
                val dateObjct = Date(rejectedMessage[0].toLong())
                var msg_date = dateFrmt.format(dateObjct)
                dbHelper?.insertRejectedSMS(msg_date, rejectedMessage[1].toString())
            }
        }
    }

    private fun addToDbTransactions(result: MutableList<MpesaTransaction>, context: Context) {
        val dbHelper = DbHelper(context)
        val db = dbHelper.writableDatabase

        if (db != null) {
            for (transaction in result) {
                dbHelper.insertMpesaTransaction(transaction)

            }

            val transactors = Transactor.getTransactorsFromTransactions(result)
            addTransactorsToDb(transactors, context)

            for (transaction in result){
                updateTransactionCheck(transaction, context)
            }




        }
        // Check if the callback is set and invoke the onRefresh() method
        MpesaFragment.CallbackSingleton.refreshCallback?.onRefresh()

    }

    private fun addTransactorsToDb(transactor: List<Transactor>, context: Context) {
        val dbHelper = DbHelper(context)
        dbHelper.insertTransactors(transactor)

    }

    private fun updateTransactionCheck(mpesaTransaction: MpesaTransaction, context: Context){
        val dbHelper = DbHelper(context)
        mpesaTransaction.id?.let { dbHelper.transactorCheckUpdateTransaction(it) }
    }

}
