package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

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
                    if (transaction.transactionCost!! > 0.0) {
                        val transactionCostPettyCash =
                            PettyCash.getTransactionCostPettyCashObject(
                                transaction,
                                applicationContext
                            )
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
        mpesaTransactionsNotConverted?.let { dbHelper?.updateMpesaTransactionListAsConverted(it) }


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
