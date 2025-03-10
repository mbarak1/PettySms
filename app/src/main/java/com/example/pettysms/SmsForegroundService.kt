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
import com.example.pettysms.queue.QuickBooksWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
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

                    val automationLogic = dbHelper.checkIsAutomated(pettyCash)

                    Log.d("SmsServiceHelper", "Automation Logic: " + automationLogic.toString())

                    if(automationLogic){
                        Log.d("SmsServiceHelper", "Automated Petty Cash")

                        pettyCash = fillAutomatedPettyCash(pettyCash, context)
                        transactionCostPettyCash = fillAutomatedPettyCashTransaction(transactionCostPettyCash,pettyCash, context)
                        
                    }

                    dbHelper.insertPettyCash(pettyCash)
                    dbHelper.insertPettyCash(transactionCostPettyCash)
                    
                    // Add to queue if the petty cash has a number
                    if (pettyCash.pettyCashNumber != null && !pettyCash.pettyCashNumber.isNullOrEmpty()) {
                        Log.d("SmsServiceHelper", "Adding to QuickBooks queue: ${pettyCash.pettyCashNumber}")
                        dbHelper.addRelatedPettyCashToQueue(pettyCash, transactionCostPettyCash)
                        // Start QuickBooks worker if not running
                        QuickBooksWorker.startWorkerIfNeeded(context)
                    }
                    
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

        if (ViewAllPettyCashActivity.isActivityVisible) {
            ViewAllPettyCashActivity.CallbackSingleton.refreshCallback?.onRefresh()
        }


    }

    private fun fillAutomatedPettyCash(pettyCash: PettyCash, context: Context): PettyCash {
        Log.d("SmsServiceHelper", "Filling automated petty cash data")
        
        try {
            // Get the database helper
            val dbHelper = DbHelper(context)
            
            // Find matching automation rule
            val matchingRule = dbHelper.findMatchingAutomationRule(pettyCash)
            
            if (matchingRule != null) {
                Log.d("SmsServiceHelper", "Found matching rule: ${matchingRule.name}")
                
                // Set account information
                if (matchingRule.accountId != null) {
                    pettyCash.account = dbHelper.getAccountById(matchingRule.accountId ?: 0)
                    Log.d("SmsServiceHelper", "Set account: ${matchingRule.accountName}")
                }
                
                // Set owner information
                if (matchingRule.ownerId != null) {
                    pettyCash.owner = matchingRule.ownerId?.let { dbHelper.getOwnerById(it) }
                    Log.d("SmsServiceHelper", "Set owner: ${matchingRule.ownerName}")
                }
                
                // Set truck information
                if (matchingRule.truckId != null) {
                    if (matchingRule.truckId == -1) {
                        // All trucks for this owner
                        val trucks = matchingRule.ownerName?.let { it ->
                            dbHelper.getOwnerByName(it)
                                ?.let { dbHelper.getLocalTrucksByOwner(it) }
                        }
                        if (trucks != null) {
                            if (trucks.isNotEmpty()) {
                                // Join truck registration numbers with commas
                                val truckNumbers = trucks.joinToString(", ") { it.truckNo ?: "" }
                                pettyCash.trucks = trucks.toMutableList()
                                Log.d("SmsServiceHelper", "Set all trucks: $truckNumbers")
                            }
                        }
                    } else {
                        // Specific truck
                        val truck = dbHelper.getTruckById(matchingRule.truckId!!)
                        if (truck != null) {
                            pettyCash.trucks = mutableListOf(truck)
                        }
                    }
                }
                
                // Generate petty cash number using the same algorithm as in AddPettyCashFragment
                val pettyCashNumber = generatePettyCashNumber(pettyCash, context)
                pettyCash.pettyCashNumber = pettyCashNumber
                Log.d("SmsServiceHelper", "Generated petty cash number: $pettyCashNumber")

                // Set description
                if (!matchingRule.descriptionPattern.isNullOrEmpty()) {
                    val description = matchingRule.descriptionPattern
                    pettyCash.description = description
                    Log.d("SmsServiceHelper", "Set description: $description")
                }


            } else {
                Log.d("SmsServiceHelper", "No matching rule found for automation")
            }
        } catch (e: Exception) {
            Log.e("SmsServiceHelper", "Error filling automated petty cash: ${e.message}", e)
        }
        
        return pettyCash
    }
    
    private fun fillAutomatedPettyCashTransaction(
        transactionPettyCash: PettyCash,
        pettyCash: PettyCash,
        context: Context
    ): PettyCash {
        Log.d("SmsServiceHelper", "Filling automated petty cash transaction cost data")
        
        try {
            // Get the database helper
            val dbHelper = DbHelper(context)
            
            // Set owner information (should be the same as the main petty cash)
            if (pettyCash.owner != null) {
                // Keep the same owner as the main petty cash
                Log.d("SmsServiceHelper", "Using same owner for transaction cost: ${pettyCash.owner?.name}")
                
                // Set account information - use transaction cost account for this owner
                val ownerCode = pettyCash.owner?.ownerCode
                transactionPettyCash.owner = pettyCash.owner
                if (ownerCode != null) {
                    val transactionCostAccount = dbHelper.getTransactionCostAccountByOwner(ownerCode)
                    if (transactionCostAccount != null) {
                        transactionPettyCash.account = transactionCostAccount
                        Log.d("SmsServiceHelper", "Set transaction cost account: ${transactionCostAccount.name}")
                    } else {
                        Log.d("SmsServiceHelper", "No transaction cost account found for owner: $ownerCode")
                    }
                }


                
                // Set truck information (same as main petty cash)
                transactionPettyCash.trucks = pettyCash.trucks
                // Trucks are already set from the main petty cash
                
                // Generate petty cash number by incrementing the main petty cash number
                if (pettyCash.pettyCashNumber != null) {
                    val latestPettyCashNo = pettyCash.pettyCashNumber
                    val parts = latestPettyCashNo!!.split("/")
                    val ownerCode = pettyCash.owner?.ownerCode
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    
                    // Validate the format of the petty cash number
                    if (parts.size == 3 && parts[0] == ownerCode && parts[2] == currentYear.toString()) {
                        val currentNumber = parts[1].toIntOrNull() ?: 0
                        val newNumber = currentNumber + 1 // Increment the petty cash number
                        val nextPettyCashNo = "$ownerCode/${String.format("%08d", newNumber)}/$currentYear"
                        transactionPettyCash.pettyCashNumber = nextPettyCashNo
                        Log.d("SmsServiceHelper", "Generated transaction cost petty cash number: $nextPettyCashNo")
                    } else {
                        // If format is unexpected, generate a new number
                        val nextPettyCashNo = generatePettyCashNumber(pettyCash, context)
                        transactionPettyCash.pettyCashNumber = nextPettyCashNo
                        Log.d("SmsServiceHelper", "Generated new transaction cost petty cash number: $nextPettyCashNo")
                    }
                } else {
                    // If no petty cash number exists, generate a new one
                    val nextPettyCashNo = generatePettyCashNumber(pettyCash, context)
                    transactionPettyCash.pettyCashNumber = nextPettyCashNo
                    Log.d("SmsServiceHelper", "Generated new transaction cost petty cash number: $nextPettyCashNo")
                }
                
                // Set description based on the mpesa transaction code
                if (pettyCash.mpesaTransaction != null) {
                    val mpesaCode = pettyCash.mpesaTransaction?.mpesa_code
                    transactionPettyCash.description = "Mpesa Transaction Cost on M-Pesa Transaction: $mpesaCode"
                    Log.d("SmsServiceHelper", "Set transaction cost description for mpesa code: $mpesaCode")
                }
            } else {
                Log.d("SmsServiceHelper", "No owner information available for transaction cost petty cash")
            }
            
        } catch (e: Exception) {
            Log.e("SmsServiceHelper", "Error filling automated petty cash transaction cost: ${e.message}", e)
        }
        
        return transactionPettyCash
    }

    private fun generatePettyCashNumber(pettyCash: PettyCash, context: Context): String {
        try {
            // Get the owner code
            val ownerCode = pettyCash.owner?.ownerCode ?: "XX"
            
            // Get the current year
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            
            // Get the database helper
            val dbHelper = DbHelper(context)
            
            // Get the latest petty cash for this owner
            val latestPettyCash = dbHelper.getLatestPettyCashByOwnerAndPettyCashNumber(ownerCode)
            
            var nextPettyCashNo: String
            
            if (latestPettyCash != null) {
                // Extract the current petty cash number and increment it
                val latestPettyCashNo = latestPettyCash.pettyCashNumber
                val parts = latestPettyCashNo!!.split("/")
                
                // Validate the format of the latest petty cash number
                if (parts.size == 3 && parts[0] == ownerCode && parts[2] == currentYear.toString()) {
                    val currentNumber = parts[1].toIntOrNull() ?: 0
                    val newNumber = currentNumber + 1 // Increment the petty cash number
                    nextPettyCashNo = "$ownerCode/${String.format("%08d", newNumber)}/$currentYear"
                    Log.d("SmsServiceHelper", "Incremented existing petty cash number: $nextPettyCashNo")
                } else {
                    // Fallback if the format is unexpected
                    nextPettyCashNo = "$ownerCode/00000001/$currentYear"
                    Log.d("SmsServiceHelper", "Created new petty cash number (format mismatch): $nextPettyCashNo")
                }
            } else {
                // No petty cash found, create the initial petty cash number
                nextPettyCashNo = "$ownerCode/00000001/$currentYear"
                Log.d("SmsServiceHelper", "Created new petty cash number (no previous): $nextPettyCashNo")
            }
            
            return nextPettyCashNo
        } catch (e: Exception) {
            Log.e("SmsServiceHelper", "Error generating petty cash number: ${e.message}", e)
            // Fallback to a default format if there's an error
            val ownerCode = pettyCash.owner?.ownerCode ?: "XX"
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return "$ownerCode/00000001/$currentYear"
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
