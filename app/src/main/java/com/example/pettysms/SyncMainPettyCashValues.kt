package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pettysms.queue.QuickBooksWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.*

class SyncMainPettyCashValues(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var notCheckedTransactions:  MutableList<MpesaTransaction>? = null

    override suspend fun doWork(): Result {
        try {
            syncMainPettyCashValues()
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private suspend fun syncMainPettyCashValues() {
        Log.d("SyncMainPettyCashValues", "Starting sync...")

        dbHelper = DbHelper(applicationContext)
        db = dbHelper!!.writableDatabase

        try {
            withContext(Dispatchers.IO) {
                fetchAndInsertOwners(dbHelper!!, db!!)
                Log.d("SyncMainPettyCashValues", "Owners synced successfully.")

                fetchAndInsertTrucks()
                Log.d("SyncMainPettyCashValues", "Trucks synced successfully.")

                fetchAndInsertAccounts()
                Log.d("SyncMainPettyCashValues", "Accounts synced successfully.")

                fetchAndInsertTransactors()
                Log.d("SyncMainPettyCashValues", "Transactors synced successfully.")

                convertTransactionsToPettyCash()
                Log.d("SyncMainPettyCashValues", "Transactions converted to PettyCash successfully.")
            }
        } catch (e: Exception) {
            Log.e("SyncMainPettyCashValues", "Error during sync: ${e.message}", e)
        } finally {
            db?.close()
        }
    }


    private fun convertTransactionsToPettyCash() {
        var pettyCashList = mutableListOf<PettyCash>()
        var transactionCostPettyCashList = mutableListOf<PettyCash>()
        Log.d("SyncPettyCashValuesWorker", "Converting transactions to petty cash...")
        if (dbHelper == null) {
            dbHelper = DbHelper(applicationContext)
        }

        val mpesaTransactionsNotConverted = dbHelper?.getNotConvertedMpesaTransactions()

        Log.d("SyncPettyCashValuesWorker", "Mpesa transactions not converted: ${mpesaTransactionsNotConverted?.size}")
        if (!mpesaTransactionsNotConverted.isNullOrEmpty()) {
            for (transaction in mpesaTransactionsNotConverted) {
                if (transaction.transaction_type == "paybill" || transaction.transaction_type == "till" || transaction.transaction_type == "send_money" || transaction.transaction_type == "topup" || transaction.transaction_type == "withdraw") {
                    Log.d("SyncPettyCashValuesWorker", "Converting transaction: ${transaction.mpesa_code}")
                    val pettyCash =
                        PettyCash.convertMpesaTransactionToPettyCash(transaction, applicationContext)
                    
                    // Check if automation rule applies and fill petty cash data
                    if (dbHelper?.checkIsAutomated(pettyCash) == true) {
                        Log.d("SyncPettyCashValuesWorker", "Automated Petty Cash found for: ${transaction.mpesa_code}")
                        fillAutomatedPettyCash(pettyCash)
                    }

                    if (transaction.transactionCost!! > 0.0) {
                        val transactionCostPettyCash =
                            PettyCash.getTransactionCostPettyCashObject(
                                transaction,
                                applicationContext
                            )
                        
                        // Check if automation rule applies and fill transaction cost petty cash data
                        if (dbHelper?.checkIsAutomated(pettyCash) == true) {
                            Log.d("SyncPettyCashValuesWorker", "Applying automation for transaction cost: ${transaction.mpesa_code}")
                            fillAutomatedPettyCashTransaction(transactionCostPettyCash, pettyCash)
                        }
                        
                        transactionCostPettyCashList.add(transactionCostPettyCash)
                    }

                    pettyCashList.add(pettyCash)
                }
            }
        }

        Log.d("SyncPettyCashValuesWorker", "Petty cash list size: ${pettyCashList.size}")
        if (pettyCashList.isEmpty()) {
            Log.d("SyncPettyCashValuesWorker", "No petty cash records found.")
            return
        }

        dbHelper?.insertPettyCashList(pettyCashList)
        dbHelper?.insertPettyCashList(transactionCostPettyCashList)
        
        // Add items with petty cash numbers to the queue
        if (pettyCashList.isNotEmpty()) {
            Log.d("SyncPettyCashValuesWorker", "Adding petty cash items to QuickBooks queue")
            
            for (i in pettyCashList.indices) {
                val pettyCash = pettyCashList[i]
                
                // Only add items with valid petty cash numbers
                if (!pettyCash.pettyCashNumber.isNullOrEmpty()) {
                    // Find the corresponding transaction cost entry if it exists
                    val transactionCostPettyCash = if (i < transactionCostPettyCashList.size) {
                        transactionCostPettyCashList[i]
                    } else null
                    
                    if (transactionCostPettyCash != null) {
                        dbHelper?.addRelatedPettyCashToQueue(pettyCash, transactionCostPettyCash)
                        Log.d("SyncPettyCashValuesWorker", "Added to queue with transaction cost: ${pettyCash.pettyCashNumber}")
                        // Start QuickBooks worker if not running
                        QuickBooksWorker.startWorkerIfNeeded(applicationContext)
                    } else {
                        dbHelper?.addToQueue(pettyCash)
                        Log.d("SyncPettyCashValuesWorker", "Added to queue: ${pettyCash.pettyCashNumber}")
                        // Start QuickBooks worker if not running
                        QuickBooksWorker.startWorkerIfNeeded(applicationContext)
                    }
                }
            }
        }
        
        mpesaTransactionsNotConverted?.let { dbHelper?.updateMpesaTransactionListAsConverted(it) }
    }

    private fun fillAutomatedPettyCash(pettyCash: PettyCash): PettyCash {
        Log.d("SyncPettyCashValuesWorker", "Filling automated petty cash data")
        
        try {
            // Get the database helper
            if (dbHelper == null) {
                dbHelper = DbHelper(applicationContext)
            }
            
            // Find matching automation rule
            val matchingRule = dbHelper?.findMatchingAutomationRule(pettyCash)
            
            if (matchingRule != null) {
                Log.d("SyncPettyCashValuesWorker", "Found matching rule: ${matchingRule.name}")
                
                // Set account information
                if (matchingRule.accountId != null) {
                    pettyCash.account = dbHelper?.getAccountById(matchingRule.accountId ?: 0)
                    Log.d("SyncPettyCashValuesWorker", "Set account: ${matchingRule.accountName}")
                }
                
                // Set owner information
                if (matchingRule.ownerId != null) {
                    pettyCash.owner = matchingRule.ownerId?.let { dbHelper?.getOwnerById(it) }
                    Log.d("SyncPettyCashValuesWorker", "Set owner: ${matchingRule.ownerName}")
                }
                
                // Set truck information
                if (matchingRule.truckId != null) {
                    if (matchingRule.truckId == -1) {
                        // All trucks for this owner
                        val trucks = matchingRule.ownerName?.let { it ->
                            dbHelper?.getOwnerByName(it)
                                ?.let { dbHelper?.getLocalTrucksByOwner(it) }
                        }
                        if (trucks != null) {
                            if (trucks.isNotEmpty()) {
                                // Join truck registration numbers with commas
                                val truckNumbers = trucks.joinToString(", ") { it.truckNo ?: "" }
                                pettyCash.trucks = trucks.toMutableList()
                                Log.d("SyncPettyCashValuesWorker", "Set all trucks: $truckNumbers")
                            }
                        }
                    } else {
                        // Specific truck
                        val truck = dbHelper?.getTruckById(matchingRule.truckId!!)
                        if (truck != null) {
                            pettyCash.trucks = mutableListOf(truck)
                        }
                    }
                }
                
                // Generate petty cash number
                val pettyCashNumber = generatePettyCashNumber(pettyCash)
                pettyCash.pettyCashNumber = pettyCashNumber
                Log.d("SyncPettyCashValuesWorker", "Generated petty cash number: $pettyCashNumber")

                // Set description
                if (!matchingRule.descriptionPattern.isNullOrEmpty()) {
                    val description = matchingRule.descriptionPattern
                    pettyCash.description = description
                    Log.d("SyncPettyCashValuesWorker", "Set description: $description")
                }
            } else {
                Log.d("SyncPettyCashValuesWorker", "No matching rule found for automation")
            }
        } catch (e: Exception) {
            Log.e("SyncPettyCashValuesWorker", "Error filling automated petty cash: ${e.message}", e)
        }
        
        return pettyCash
    }
    
    private fun fillAutomatedPettyCashTransaction(
        transactionPettyCash: PettyCash,
        pettyCash: PettyCash
    ): PettyCash {
        Log.d("SyncPettyCashValuesWorker", "Filling automated petty cash transaction cost data")
        
        try {
            // Get the database helper
            if (dbHelper == null) {
                dbHelper = DbHelper(applicationContext)
            }
            
            // Set owner information (should be the same as the main petty cash)
            if (pettyCash.owner != null) {
                // Keep the same owner as the main petty cash
                Log.d("SyncPettyCashValuesWorker", "Using same owner for transaction cost: ${pettyCash.owner?.name}")
                
                // Set account information - use transaction cost account for this owner
                val ownerCode = pettyCash.owner?.ownerCode
                transactionPettyCash.owner = pettyCash.owner
                if (ownerCode != null) {
                    val transactionCostAccount = dbHelper?.getTransactionCostAccountByOwner(ownerCode)
                    if (transactionCostAccount != null) {
                        transactionPettyCash.account = transactionCostAccount
                        Log.d("SyncPettyCashValuesWorker", "Set transaction cost account: ${transactionCostAccount.name}")
                    } else {
                        Log.d("SyncPettyCashValuesWorker", "No transaction cost account found for owner: $ownerCode")
                    }
                }
                
                // Set truck information (same as main petty cash)
                transactionPettyCash.trucks = pettyCash.trucks
                
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
                        Log.d("SyncPettyCashValuesWorker", "Generated transaction cost petty cash number: $nextPettyCashNo")
                    } else {
                        // If format is unexpected, generate a new number
                        val nextPettyCashNo = generatePettyCashNumber(pettyCash)
                        transactionPettyCash.pettyCashNumber = nextPettyCashNo
                        Log.d("SyncPettyCashValuesWorker", "Generated new transaction cost petty cash number: $nextPettyCashNo")
                    }
                } else {
                    // If no petty cash number exists, generate a new one
                    val nextPettyCashNo = generatePettyCashNumber(pettyCash)
                    transactionPettyCash.pettyCashNumber = nextPettyCashNo
                    Log.d("SyncPettyCashValuesWorker", "Generated new transaction cost petty cash number: $nextPettyCashNo")
                }
                
                // Set description based on the mpesa transaction code
                if (pettyCash.mpesaTransaction != null) {
                    val mpesaCode = pettyCash.mpesaTransaction?.mpesa_code
                    transactionPettyCash.description = "Mpesa Transaction Cost on M-Pesa Transaction: $mpesaCode"
                    Log.d("SyncPettyCashValuesWorker", "Set transaction cost description for mpesa code: $mpesaCode")
                }
            } else {
                Log.d("SyncPettyCashValuesWorker", "No owner information available for transaction cost petty cash")
            }
            
        } catch (e: Exception) {
            Log.e("SyncPettyCashValuesWorker", "Error filling automated petty cash transaction cost: ${e.message}", e)
        }
        
        return transactionPettyCash
    }

    private fun generatePettyCashNumber(pettyCash: PettyCash): String {
        try {
            // Get the owner code
            val ownerCode = pettyCash.owner?.ownerCode ?: "XX"
            
            // Get the current year
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            
            // Get the database helper
            if (dbHelper == null) {
                dbHelper = DbHelper(applicationContext)
            }
            
            // Get the latest petty cash for this owner
            val latestPettyCash = dbHelper?.getLatestPettyCashByOwnerAndPettyCashNumber(ownerCode)
            
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
                    Log.d("SyncPettyCashValuesWorker", "Incremented existing petty cash number: $nextPettyCashNo")
                } else {
                    // Fallback if the format is unexpected
                    nextPettyCashNo = "$ownerCode/00000001/$currentYear"
                    Log.d("SyncPettyCashValuesWorker", "Created new petty cash number (format mismatch): $nextPettyCashNo")
                }
            } else {
                // No petty cash found, create the initial petty cash number
                nextPettyCashNo = "$ownerCode/00000001/$currentYear"
                Log.d("SyncPettyCashValuesWorker", "Created new petty cash number (no previous): $nextPettyCashNo")
            }
            
            return nextPettyCashNo
        } catch (e: Exception) {
            Log.e("SyncPettyCashValuesWorker", "Error generating petty cash number: ${e.message}", e)
            // Fallback to a default format if there's an error
            val ownerCode = pettyCash.owner?.ownerCode ?: "XX"
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return "$ownerCode/00000001/$currentYear"
        }
    }

    private fun fetchAndInsertTransactors() {
        notCheckedTransactions = dbHelper?.getTransactorNotCheckedTransactions()
        var notCheckedTransactors = Transactor.getTransactorsFromTransactions(notCheckedTransactions!!)

        syncNewTransactors(notCheckedTransactors, notCheckedTransactions!!)
    }

    private fun syncNewTransactors(notCheckedTransactors: List<Transactor>, notCheckedTransactions: MutableList<MpesaTransaction>) {
            addTransactorsToDb(notCheckedTransactors)
            for (transaction in notCheckedTransactions) {
                updateTransactionCheck(transaction)
            }

    }

    private fun addTransactorsToDb(transactor: List<Transactor>) {
        if (db?.isOpen == true) {
            dbHelper?.insertTransactors(transactor)
        } else {
            try {
                initializeDatabase()
                dbHelper?.insertTransactors(transactor)
            } catch (e: Exception) {
                // Log the error
                e.printStackTrace()
            }
        }
    }


    private fun updateTransactionCheck(mpesaTransaction: MpesaTransaction){
        val dbHelper = DbHelper(applicationContext)
        mpesaTransaction.let { dbHelper.transactorCheckUpdateTransaction(it) }
    }

    private suspend fun fetchAndInsertAccounts() {
        val remoteAccounts = fetchRemoteAccounts()

        remoteAccounts?.let { newAccounts ->
            val localAccounts = dbHelper?.getAllAccounts().orEmpty()

            val accountsToInsert = newAccounts.filterNot { newAccount ->
                localAccounts.any {
                    it.id == newAccount.id &&
                            it.name == newAccount.name &&
                            it.owner?.name == newAccount.owner?.name
                }
            }

            if (db?.isOpen == true) {
                dbHelper?.insertAccounts(accountsToInsert)
            } else {
                initializeDatabase()
                dbHelper?.insertAccounts(accountsToInsert)
            }
        }
    }

    private suspend fun fetchRemoteAccounts(): List<Account>? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("operation", "getallaccounts")
            .build()

        val request = Request.Builder()
            .url("https://$SERVER_IP/")
            .post(formBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body?.string() ?: "")
                    val accounts = mutableListOf<Account>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val accountId = jsonObject.getInt("id")
                        val accountName = jsonObject.getString("full_name")
                        val accountType = jsonObject.getString("account_type")
                        val accountCurrency = jsonObject.getString("currency")
                        val accountNumber = jsonObject.getString("account_number")
                        val ownerName = jsonObject.optString("company_name", "")

                        // Retrieve owner from the database
                        val owner = getOwnerByName(ownerName)

                        // Add account only if owner exists
                        if (owner != null) {
                            accounts.add(
                                Account(
                                    id = accountId,
                                    name = accountName,
                                    owner = owner,
                                    type = accountType,
                                    currency = accountCurrency,
                                    accountNumber = accountNumber
                                )
                            )
                        }
                    }

                    return@use accounts
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getOwnerByName(ownerName: String): Owner? {
        return if (db?.isOpen == true) {
            dbHelper?.getOwnerByName(ownerName)
        } else {
            initializeDatabase()
            dbHelper?.getOwnerByName(ownerName)
        }
    }




        private suspend fun fetchAndInsertOwners(dbHelper: DbHelper, db: SQLiteDatabase) {
        val remoteOwners = fetchRemoteOwners()
        remoteOwners?.let { newOwners ->
            val localOwners = dbHelper.getAllOwners()
            val ownersToInsert = newOwners.filterNot { newOwner ->
                localOwners.any { it.id == newOwner.id }
            }
            if (db.isOpen == true) {
                dbHelper.insertOwners(ownersToInsert)
            } else {
                initializeDatabase()
                dbHelper.insertOwners(ownersToInsert)
            }
        }
    }

    private suspend fun fetchRemoteOwners(): List<Owner>? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("operation", "getallowners")
            .build()

        val request = Request.Builder()
            .url("https://${SERVER_IP}/")
            .post(formBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body?.string() ?: "")
                    val owners = mutableListOf<Owner>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val ownerId = jsonObject.getInt("id")
                        val ownerName = jsonObject.getString("name")
                        val ownerCode = jsonObject.getString("owner_code")
                        owners.add(Owner(ownerId, ownerName, ownerCode))
                    }
                    return@use owners
                }
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchAndInsertTrucks() {
        val remoteTrucks = fetchRemoteTrucks()

        remoteTrucks?.let { newTrucks ->
            val localTrucks = getLocalTrucks()
            val trucksToInsert = newTrucks.filterNot { newTruck ->
                localTrucks.any { it == newTruck }
            }

            if (db?.isOpen == true) {
                dbHelper?.insertTrucks(trucksToInsert)
            } else {
                initializeDatabase()
                dbHelper?.insertTrucks(trucksToInsert)
            }
        }
    }

    private fun initializeDatabase() {
        if (dbHelper == null || db == null || db?.isOpen == false) {
            dbHelper = DbHelper(applicationContext)
            db = dbHelper?.writableDatabase
        }
    }

    private fun getLocalTrucks(): List<Truck> {
        if (db?.isOpen == true) {
            return dbHelper?.getLocalTrucks() ?: emptyList() // Use empty list if null
        } else {
            dbHelper = applicationContext.let { DbHelper(it) }
            db = dbHelper?.writableDatabase
            return dbHelper?.getLocalTrucks() ?: emptyList() // Use empty list if null
        }
    }


    private suspend fun fetchRemoteTrucks(): List<Truck>? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .add("operation", "getalltrucks")
                .build()

            val request = Request.Builder()
                .url("https://${SERVER_IP}/")
                .post(formBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(response.body?.string() ?: "")
                        val trucks = mutableListOf<Truck>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val truckId = jsonObject.getInt("id")
                            val truckNo = jsonObject.getString("truck_no")
                            val make = jsonObject.getString("make")
                            val ownerCode = jsonObject.getString("owner")
                            val activeStatusString = jsonObject.getString("active_status")

                            val activeStatus = activeStatusString == "active"
                            var owner: Owner? = null

                            if (db?.isOpen == true) {
                                owner = dbHelper?.getOwnerByCode(ownerCode)
                            }else{
                                dbHelper = DbHelper(applicationContext)
                                db = dbHelper?.writableDatabase
                                owner = dbHelper?.getOwnerByCode(ownerCode)
                            }


                            // Parse other truck details...
                            trucks.add(Truck(truckId, truckNo, make, owner, activeStatus))
                        }
                        trucks
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    companion object {
        const val SERVER_IP = "api.abdulcon.com"
    }
}
