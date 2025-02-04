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
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SmsForegroundService : Service() {

    private val NOTIFICATION_CHANNEL_ID = "SmsForegroundServiceChannel"
    private val notificationId = 1
    private lateinit var serviceJob: Job

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(notificationId, createNotification())

        serviceJob = Job()
        val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
        serviceScope.launch {
            checkAndInsertNewSms()
        }

        return START_STICKY
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
            Log.e("SmsForegroundService", "SMS read permission not granted")
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SmsForegroundService")
            .setContentText("Foreground service is running")
            .setSmallIcon(R.drawable.p_logo_cropped)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "SMS Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
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

            result.forEach { transaction ->
                if (!transaction.isConvertedToPettyCash && (transaction.transaction_type == "paybill" || transaction.transaction_type == "till" || transaction.transaction_type == "send_money" || transaction.transaction_type == "topup" || transaction.transaction_type == "withdraw")) {
                    var pettyCash = transaction.let {
                        PettyCash.convertMpesaTransactionToPettyCash(
                            it,
                            context
                        )
                    }
                    var transactionCostPettyCash =
                        transaction.let { PettyCash.getTransactionCostPettyCashObject(it, context) }

                    dbHelper.insertPettyCash(pettyCash)
                    dbHelper.insertPettyCash(transactionCostPettyCash)
                    dbHelper.updateMpesaTransactionAsConverted(transaction)


                }
            }




        }

        Log.d("SmsServiceHelper", "MpesaFragment Visibility: " + MpesaFragment.FragmentVisibilityTracker.isMpesaFragmentVisible)

        if (MpesaFragment.FragmentVisibilityTracker.isMpesaFragmentVisible) {
            MpesaFragment.CallbackSingleton.refreshCallback?.onRefresh()
        }

        if (PettyCashFragment.FragmentVisibilityTracker.isPettyCashFragmentVisible){
            PettyCashFragment.CallbackSingleton.refreshCallback?.onRefresh()
        }


    }

    private fun addTransactorsToDb(transactor: List<Transactor>, context: Context) {
        val dbHelper = DbHelper(context)
        dbHelper.insertTransactors(transactor)
    }

    private fun updateTransactionCheck(mpesaTransaction: MpesaTransaction, context: Context){
        val dbHelper = DbHelper(context)
        mpesaTransaction.let { dbHelper.transactorCheckUpdateTransaction(it) }
    }

}
