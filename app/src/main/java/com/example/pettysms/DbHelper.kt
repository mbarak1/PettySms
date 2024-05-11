package com.example.pettysms

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION)  {
    override fun onCreate(db: SQLiteDatabase) {
        createInitialTables(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over

        val SQL_DELETE_ENTRIES =  "DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS

        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun insertMpesaTransaction(mpesaTransaction: MpesaTransaction){

        val db = writableDatabase

        val contentValues = ContentValues()

        contentValues.put(COL_TRANSACTIONS_MPESA_CODE, mpesaTransaction.mpesa_code)
        contentValues.put(COL_TRANSACTIONS_RECEPIENT_NAME, mpesaTransaction.recipient?.name)
        contentValues.put(COL_TRANSACTIONS_MESSAGE_DATE, mpesaTransaction.msg_date)
        contentValues.put(COL_TRANSACTIONS_TRANSACTION_DATE, mpesaTransaction.transaction_date)
        contentValues.put(COL_TRANSACTIONS_RECEPIENT_NO, mpesaTransaction.recipient?.phone_no)
        contentValues.put(COL_TRANSACTIONS_AMOUNT, mpesaTransaction.amount)
        contentValues.put(COL_TRANSACTIONS_ACCOUNT_NO, mpesaTransaction.account?.id)
        contentValues.put(COL_TRANSACTIONS_COMPANY_OWNER, mpesaTransaction.company_owner?.name)
        contentValues.put(COL_TRANSACTIONS_TRANSACTION_TYPE, mpesaTransaction.transaction_type)
        contentValues.put(COL_TRANSACTIONS_USER, mpesaTransaction.user?.name)
        contentValues.put(COL_TRANSACTIONS_PAYMENT_MODE, mpesaTransaction.payment_mode?.id)
        contentValues.put(COL_TRANSACTIONS_MPESA_BALANCE, mpesaTransaction.mpesaBalance)
        contentValues.put(COL_TRANSACTIONS_TRANSACTION_COST, mpesaTransaction.transactionCost)
        contentValues.put(COL_TRANSACTIONS_MPESA_DEPOSITOR, mpesaTransaction.mpesaDepositor)
        contentValues.put(COL_TRANSACTIONS_SMS_TEXT, mpesaTransaction.smsText)
        contentValues.put(COL_TRANSACTIONS_PAYBILL_ACCOUNT, mpesaTransaction.paybillAcount)
        contentValues.put(COL_TRANSACTIONS_DESCRIPTION, mpesaTransaction.description)
        contentValues.put(COL_TRANSACTIONS_SENDER_NAME, mpesaTransaction.sender?.name)
        contentValues.put(COL_TRANSACTIONS_SENDER_PHONE_NO, mpesaTransaction.sender?.phone_no)
        contentValues.put(COL_TRANSACTIONS_IS_DELETED, if (mpesaTransaction.isDeleted == true) 1 else 0)




        val newRowId = db.insert(TABLE_TRANSACTIONS, null, contentValues)

        db.close()


    }

    fun deleteTransaction(transactionId: Int){
        val db = writableDatabase

        val contentValues = ContentValues().apply {
            put(COL_TRANSACTIONS_IS_DELETED, 1)
        }
        val whereClause = "$COL_TRANSACTIONS_ID = ?"
        val whereArgs = arrayOf(transactionId.toString())
        db.update(TABLE_TRANSACTIONS, contentValues, whereClause, whereArgs)

    }

    fun insertRejectedSMS(date: String, smsBody: String) {
        val db = writableDatabase

        // Utilize INSERT OR IGNORE to automatically ignore if the record already exists
        val contentValues = ContentValues().apply {
            put(COL_REJECTED_MESSAGES_DATE, date)
            put(COL_REJECTED_MESSAGES_SMS_BODY, smsBody)
        }

        db.insertWithOnConflict(
            TABLE_REJECTED_SMS,
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        db.close()
    }

    fun isSmsExists(db: SQLiteDatabase, smsBody: String, smsDate: Long): Boolean {
        val selection = "$COL_ALL_SMS_BODY = ? AND $COL_TIMESTAMP_ALL_SMS = ?"
        val selectionArgs = arrayOf(smsBody, smsDate.toString())

        var cursor: Cursor? = null

        try {
            // Use the provided database if open; otherwise, create a new one
            val database = if (db.isOpen) db else writableDatabase

            cursor = database.query(
                TABLE_ALL_SMS,
                arrayOf(COL_ALL_SMS_ID),
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            return cursor.count > 0
        } finally {
            // Close only the cursor; the database will be closed outside the loop
            cursor?.close()
        }
    }




    fun createInitialTables(db: SQLiteDatabase){
        val SQL_CREATE_ENTRIES = "CREATE TABLE IF NOT EXISTS $TABLE_TRANSACTIONS" + " (" +
                COL_TRANSACTIONS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TRANSACTIONS_MPESA_CODE + " TEXT," +
                COL_TRANSACTIONS_MESSAGE_DATE + " TEXT," +
                COL_TRANSACTIONS_TRANSACTION_DATE + " TEXT," +
                COL_TRANSACTIONS_RECEPIENT_NAME + " TEXT," +
                COL_TRANSACTIONS_RECEPIENT_NO + " TEXT," +
                COL_TRANSACTIONS_AMOUNT + " REAL," +
                COL_TRANSACTIONS_ACCOUNT_NO + " INTEGER," +
                COL_TRANSACTIONS_COMPANY_OWNER + " TEXT," +
                COL_TRANSACTIONS_TRANSACTION_TYPE + " TEXT," +
                COL_TRANSACTIONS_USER + " TEXT," +
                COL_TRANSACTIONS_PAYMENT_MODE + " INTEGER," +
                COL_TRANSACTIONS_MPESA_BALANCE + " REAL," +
                COL_TRANSACTIONS_TRANSACTION_COST + " REAL," +
                COL_TRANSACTIONS_MPESA_DEPOSITOR + " TEXT," +
                COL_TRANSACTIONS_SMS_TEXT + " TEXT," +
                COL_TRANSACTIONS_PAYBILL_ACCOUNT + " TEXT," +
                COL_TRANSACTIONS_SENDER_NAME + " TEXT," +
                COL_TRANSACTIONS_SENDER_PHONE_NO + " TEXT," +
                COL_TRANSACTIONS_DESCRIPTION + " TEXT," +
                COL_TRANSACTIONS_IS_DELETED + " INTEGER" + // Removed the trailing comma
// Removed the trailing comma
                ")"

        val SQL_CREATE_ENTRIES_REJECTED_SMS = "CREATE TABLE IF NOT EXISTS $TABLE_REJECTED_SMS" + "(" +
                "$COL_REJECTED_MESSAGES_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_REJECTED_MESSAGES_DATE TEXT," +
                "$COL_REJECTED_MESSAGES_SMS_BODY TEXT," +
                // Other columns...
                "UNIQUE($COL_REJECTED_MESSAGES_DATE, $COL_REJECTED_MESSAGES_SMS_BODY))".trimIndent()

        val SQL_CREATE_ENTRIES_SEARCH_HISTORY = "CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY" + "(" +
                "$COL_SEARCH_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SEARCH_HISTORY_QUERY TEXT," +
                "$COL_TIMESTAMP INTEGER" +
            ")"
        .trimIndent()

        val SQL_CREATE_ENTRIES_ALL_SMS =
            "CREATE TABLE IF NOT EXISTS $TABLE_ALL_SMS" + "(" +
                    "$COL_ALL_SMS_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COL_ALL_SMS_UNIQUE_ID INTEGER," +  // New column for storing SMS ID
                    "$COL_ALL_SMS_BODY TEXT," +
                    "$COL_TIMESTAMP_ALL_SMS INTEGER" +
                    ")".trimIndent()

        val SQL_CREATE_TABLE_TRUCKS = """
            CREATE TABLE IF NOT EXISTS $TABLE_TRUCKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRUCK_NO TEXT, 
                $COL_TRUCK_MAKE TETX,
                $COL_TRUCK_OWNER TEXT,
                $COL_TRUCK_ACTIVE_STATUS INTEGER
                
            )
        """

        val SQL_CREATE_TABLE_OWNERS = """
            CREATE TABLE IF NOT EXISTS $TABLE_OWNERS (
                $COL_OWNER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OWNER_NAME TEXT, 
                $COL_OWNER_CODE TEXT
            )
        """




        db.execSQL(SQL_CREATE_ENTRIES_ALL_SMS)
        db.execSQL(SQL_CREATE_ENTRIES)
        db.execSQL(SQL_CREATE_ENTRIES_REJECTED_SMS)
        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY)
        db.execSQL(SQL_CREATE_TABLE_OWNERS)
        db.execSQL(SQL_CREATE_TABLE_TRUCKS)



    }

    fun getAllMpesaTransactions(): MutableList<MpesaTransaction> {

        val query = """
        SELECT *
        FROM $TABLE_TRANSACTIONS
        ORDER BY
        substr($COL_TRANSACTIONS_TRANSACTION_DATE, 7, 4) || '-' || substr($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 2) || '-' || substr($COL_TRANSACTIONS_TRANSACTION_DATE, 1, 2)
        || ' ' ||
        substr($COL_TRANSACTIONS_TRANSACTION_DATE, 12, 8) DESC
        """
        return getTransactionsFromQuery(query)

    }

    fun getThisMonthMpesaTransactions(): MutableList<MpesaTransaction> {

        val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        val query = "SELECT * FROM $TABLE_TRANSACTIONS WHERE substr($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 7) = '$currentMonthYear'"
        return getTransactionsFromQuery(query)
    }
    fun getThisMonthMpesaNonDeletedTransactions(): MutableList<MpesaTransaction> {

        val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        val query = "SELECT * FROM $TABLE_TRANSACTIONS WHERE substr($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 7) = '$currentMonthYear' AND is_deleted = 0"
        return getTransactionsFromQuery(query)
    }

    fun getLatestTransactionLastMonth(): MutableList<MpesaTransaction> {

        val lastMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault())
            .format(Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time)

        val query = """
            SELECT *
            FROM $TABLE_TRANSACTIONS
            WHERE SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 7) = '$lastMonthYear'
            ORDER BY $COL_TRANSACTIONS_TRANSACTION_DATE DESC
            LIMIT 1;
        """

        return getTransactionsFromQuery(query)
    }

    fun getMostRecentTransaction(): MutableList<MpesaTransaction> {

        val query = "SELECT * FROM $TABLE_TRANSACTIONS ORDER BY datetime(SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 7, 4) || '-' || SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 2) || '-' || SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 1, 2) || ' ' || SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 12, 8)) DESC LIMIT 1"

        return getTransactionsFromQuery(query)
    }

    fun addToSearchHistory(query: String) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_SEARCH_HISTORY_QUERY, query)
            put(COL_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_SEARCH_HISTORY, null, contentValues)
        db.close()
    }

    fun insertSms(smsId: Long, smsBody: String, timestamp: Long): Long {
        val db = writableDatabase

        println("wazimu")

        return try {
            // Utilize INSERT OR IGNORE to automatically ignore if the record already exists
            val contentValues = ContentValues().apply {
                put(COL_ALL_SMS_UNIQUE_ID, smsId)
                put(COL_ALL_SMS_BODY, smsBody)
                put(COL_TIMESTAMP_ALL_SMS, timestamp)
            }

            println("Mambo vp")


            db.beginTransaction()
            try {
                // Insert the values and ignore if the combination already exists
                val rowId = db.insertWithOnConflict(
                    TABLE_ALL_SMS,
                    null,
                    contentValues,
                    SQLiteDatabase.CONFLICT_IGNORE
                )

                println("Mambo")

                db.setTransactionSuccessful()
                rowId
            } finally {
                db.endTransaction()
            }
        } finally {
            //db.close()
        }
    }

    fun getSearchHistory(): MutableList<String> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SEARCH_HISTORY,
            arrayOf(COL_SEARCH_HISTORY_QUERY),
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )

        val searchHistory = mutableListOf<String>()
        with(cursor) {
            while (moveToNext()) {
                val query = getString(getColumnIndexOrThrow(COL_SEARCH_HISTORY_QUERY))
                searchHistory.add(query)
            }
        }
        cursor.close()
        db.close()

        return searchHistory
    }

    @SuppressLint("Range")
    fun getTransactionsFromQuery(query: String): MutableList<MpesaTransaction>{
        val transactions = mutableListOf<MpesaTransaction>()
        val db = readableDatabase

        val cursor = db.rawQuery(query, null)


        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COL_TRANSACTIONS_ID))
                val mpesa_code = cursor.getString(cursor.getColumnIndex(COL_TRANSACTIONS_MPESA_CODE))
                val recepient_name = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_RECEPIENT_NAME))
                val msg_date = cursor.getString(cursor.getColumnIndex(COL_TRANSACTIONS_MESSAGE_DATE))
                val transaction_date = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_TRANSACTION_DATE))
                val recepient_phone_no = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_RECEPIENT_NO))
                val amount = cursor.getDouble(cursor.getColumnIndex(COL_TRANSACTIONS_AMOUNT))
                val account_id = cursor.getInt(cursor.getColumnIndex(COL_TRANSACTIONS_ACCOUNT_NO))
                val company_owner_name = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_COMPANY_OWNER))
                val transaction_type = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_TRANSACTION_TYPE))
                val user_name = cursor.getString(cursor.getColumnIndex(COL_TRANSACTIONS_USER))
                val payment_mode_id = cursor.getInt(cursor.getColumnIndex(
                    COL_TRANSACTIONS_PAYMENT_MODE))
                val mpesa_balance = cursor.getDouble(cursor.getColumnIndex(
                    COL_TRANSACTIONS_MPESA_BALANCE))
                val transaction_cost = cursor.getDouble(cursor.getColumnIndex(
                    COL_TRANSACTIONS_TRANSACTION_COST))
                val mpesa_depositor = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_MPESA_DEPOSITOR))
                val sms_text = cursor.getString(cursor.getColumnIndex(COL_TRANSACTIONS_SMS_TEXT))
                val paybill_account = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_PAYBILL_ACCOUNT))
                val description = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_DESCRIPTION))
                val sender_name = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_SENDER_NAME))
                val sender_phone_no = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_SENDER_PHONE_NO))
                val is_deleted = cursor.getString(cursor.getColumnIndex(
                    COL_TRANSACTIONS_IS_DELETED))

                val transaction = MpesaTransaction(id = id, mpesaCode = mpesa_code, msgDate = msg_date, transactionDate = transaction_date,
                    account = Account(
                        id = 1,
                        name = "General Expenses",
                        description = "General Expenses",
                        type = "Expense"
                    ), amount = amount,
                    recipient = Recepient(name = recepient_name, phone_no = recepient_phone_no),
                    transactionType = transaction_type,
                    mpesaBalance = mpesa_balance,
                    transactionCost = transaction_cost,
                    mpesaDepositor = mpesa_depositor,
                    smsText = sms_text,
                    paybillAcount = paybill_account,
                    description = description,
                    sender = Sender(sender_name,sender_phone_no),
                    isDeleted = if (is_deleted.toInt() == 1) true else false
                )
                transactions.add(transaction)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return transactions
    }

    fun getCountAllTransactions(): Int {
        val countQuery = "SELECT COUNT(*) FROM $TABLE_TRANSACTIONS"

        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        var transactionCount = 0

        if (cursor.moveToFirst()) {
            transactionCount = cursor.getInt(0)
        }

        cursor.close()
        db.close()

        return transactionCount

    }

    fun getCountAllRejectedSms(): Int {
        val countQuery = "SELECT COUNT(*) FROM $TABLE_REJECTED_SMS"

        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        var rejectedSmsCount = 0

        if (cursor.moveToFirst()) {
            rejectedSmsCount = cursor.getInt(0)
        }

        cursor.close()
        db.close()

        return rejectedSmsCount

    }
    fun clearSearchHistory() {
        val db = writableDatabase

        try {
            // Clear the search history table
            db.execSQL("DELETE FROM $TABLE_SEARCH_HISTORY")
        } catch (e: SQLException) {
            // Handle exceptions, if any
            e.printStackTrace()
        } finally {
            db.close()
        }
    }

    fun hasTables(db: SQLiteDatabase): Boolean {
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        val hasTables = cursor.count > 0
        cursor.close()
        return hasTables
    }

    fun deleteAllTransactions() {
        val db = writableDatabase

        val contentValues = ContentValues().apply {
            put(COL_TRANSACTIONS_IS_DELETED, 1)
        }

        db.update(TABLE_TRANSACTIONS, contentValues, null, null)
    }

    fun updateTransactionDescription(transactionId: Int, newDescription: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_TRANSACTIONS_DESCRIPTION, newDescription)
        }

        val updateResult = db.update(TABLE_TRANSACTIONS, contentValues, "$COL_TRANSACTIONS_ID=?", arrayOf(transactionId.toString()))
        db.close()
        return updateResult != -1
    }

    //NEW PETTY CASH METHODS

    private fun insertTruck(db: SQLiteDatabase, truck: Truck) {
        val values = ContentValues().apply {
            put(COL_TRUCK_NO, truck.truckNo)
            put(COL_TRUCK_MAKE, truck.make)
            put(COL_TRUCK_OWNER, truck.owner?.ownerCode)
            put(COL_TRUCK_ACTIVE_STATUS, if (truck.activeStatus == true) 1 else 0)

            // Add other truck details...
        }
        db.insertWithOnConflict(TABLE_TRUCKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertTruckIfNotAvailable(db: SQLiteDatabase, truck: Truck) {
        val truckNo = truck.truckNo

        // Check if the truck with the same truck_no already exists
        val cursor = db.query(
            TABLE_TRUCKS,
            arrayOf(COL_TRUCK_NO),
            "$COL_TRUCK_NO = ?",
            arrayOf(truckNo),
            null,
            null,
            null
        )

        if (cursor != null && cursor.count == 0) {
            // Insert the truck if it does not exist
            val values = ContentValues().apply {
                put(COL_TRUCK_NO, truck.truckNo)
                put(COL_TRUCK_MAKE, truck.make)
                put(COL_TRUCK_OWNER, truck.owner?.ownerCode)
                put(COL_TRUCK_ACTIVE_STATUS, if (truck.activeStatus == true) 1 else 0)
            }
            db.insert(TABLE_TRUCKS, null, values)
        }
    }

    fun insertTrucks(trucks: List<Truck>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            for (truck in trucks) {
                insertTruckIfNotAvailable(db, truck) // Pass the SQLiteDatabase instance to the insertTruck function
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }


    fun getLocalTrucks(): List<Truck> {
        val trucks = mutableListOf<Truck>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TRUCKS, arrayOf(
                COL_ID,
                COL_TRUCK_NO,
                COL_TRUCK_MAKE,
                COL_TRUCK_OWNER,
                COL_TRUCK_ACTIVE_STATUS
            ), null, null, null, null, null)
        while (cursor.moveToNext()) {
            val truckNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_NO))
            val truckId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID))
            val truckMake = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_MAKE))
            val truckOwnerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_OWNER))
            val truckActiveStatusInteger = cursor.getInt(cursor.getColumnIndexOrThrow(
                COL_TRUCK_ACTIVE_STATUS
            ))
            var truckActiveStatus = true
            if(truckActiveStatusInteger == 0){
                truckActiveStatus = false
            }

            val owner = getOwnerByCode(truckOwnerCode) ?: Owner(1, "Abdulcon Enterprises Limited", "abdulcon")

            trucks.add(Truck(truckId, truckNo, truckMake, owner, truckActiveStatus))
        }

        return trucks
    }

    fun getTruckUniqueModelStrings(): List<String> {
        val modelStrings = mutableListOf<String>()
        val db = this.readableDatabase
        val query = "SELECT DISTINCT $COL_TRUCK_MAKE FROM $TABLE_TRUCKS"
        val cursor: Cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val modelString = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_MAKE))
                modelStrings.add(modelString)
            }
        }
        return modelStrings
    }

    fun getOwnerByCode(ownerCode: String): Owner? {
        val db = this.readableDatabase
        val selection = "$COL_OWNER_CODE = ?"
        val selectionArgs = arrayOf(ownerCode)
        val cursor = db.query(TABLE_OWNERS, null, selection, selectionArgs, null, null, null)
        var owner: Owner? = null
        try {
            if (cursor.moveToFirst()) {
                val ownerId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_NAME))
                owner = Owner(ownerId, ownerName, ownerCode)
            }
        } finally {
            cursor.close()
            // Do not close the database connection here
        }
        // Close the database connection outside of the try-finally block
        db.close()
        return owner
    }



    // Modify insertOwner to accept a SQLiteDatabase instance
    private fun insertOwner(db: SQLiteDatabase, owner: Owner) {
        // Perform insertion of owner into the database using the provided SQLiteDatabase instance
        val values = ContentValues()
        values.put(COL_OWNER_ID, owner.id)
        values.put(COL_OWNER_NAME, owner.name)
        values.put(COL_OWNER_CODE, owner.ownerCode)
        db.insert(TABLE_OWNERS, null, values)
    }

    fun getAllOwners(): List<Owner> {
        val owners = mutableListOf<Owner>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_OWNERS", null)
        cursor.use {
            while (it.moveToNext()) {
                val ownerId = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = it.getString(it.getColumnIndexOrThrow(COL_OWNER_NAME))
                val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE))
                owners.add(Owner(ownerId, ownerName, ownerCode))
            }
        }
        // Close the database connection after the cursor is closed
        db.close()
        return owners
    }

    // Function to insert a list of owners into the local database
    fun insertOwners(owners: List<Owner>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            // Insert each owner into the owners table using insertOwner function
            for (owner in owners) {
                insertOwner(db, owner) // Pass the database instance to the insertOwner function
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (db.isOpen) { // Check if the database connection is open
                db.endTransaction()
                db.close()
            }
        }
    }


    companion object {
        // If you change the database schema, you must increment the database version.
            const val DATABASE_VERSION = 2
            const val DATABASE_NAME = "PettySms.db"
            const val TABLE_TRANSACTIONS = "transactions"
            const val COL_TRANSACTIONS_ID = "id"
            const val COL_TRANSACTIONS_MPESA_CODE = "mpesa_code"
            const val COL_TRANSACTIONS_MESSAGE_DATE = "message_date"
            const val COL_TRANSACTIONS_TRANSACTION_DATE = "transaction_date"
            const val COL_TRANSACTIONS_RECEPIENT_NAME = "recepient_name"
            const val COL_TRANSACTIONS_RECEPIENT_NO = "recepient_no"
            const val COL_TRANSACTIONS_AMOUNT = "amount"
            const val COL_TRANSACTIONS_ACCOUNT_NO = "account_no"
            const val COL_TRANSACTIONS_COMPANY_OWNER = "company_owner"
            const val COL_TRANSACTIONS_TRANSACTION_TYPE = "transaction_type"
            const val COL_TRANSACTIONS_USER = "user"
            const val COL_TRANSACTIONS_PAYMENT_MODE = "payment_mode"
            const val COL_TRANSACTIONS_DESCRIPTION = "description"
            const val COL_TRANSACTIONS_MPESA_BALANCE = "mpesa_balance"
            const val COL_TRANSACTIONS_TRANSACTION_COST = "transaction_cost"
            const val COL_TRANSACTIONS_MPESA_DEPOSITOR = "mpesa_depositor"
            const val COL_TRANSACTIONS_SMS_TEXT = "sms_text"
            const val COL_TRANSACTIONS_PAYBILL_ACCOUNT = "paybill_account"
            const val COL_TRANSACTIONS_SENDER_NAME = "sender_name"
            const val COL_TRANSACTIONS_SENDER_PHONE_NO = "sender_phone_no"
            const val COL_TRANSACTIONS_IS_DELETED = "is_deleted"

            //REJECTED SMS TABLE VARIABLES

            const val TABLE_REJECTED_SMS = "rejected_sms"
            const val COL_REJECTED_MESSAGES_ID = "id"
            const val COL_REJECTED_MESSAGES_DATE = "date"
            const val COL_REJECTED_MESSAGES_SMS_BODY = "sms_body"

            //SEARCH HISTORY TABLE VARIABLES

            const val COL_SEARCH_HISTORY_ID = "id"
            const val COL_SEARCH_HISTORY_QUERY = "query"
            const val TABLE_SEARCH_HISTORY = "search_history"
            const val COL_TIMESTAMP = "timestamp" // Add this line

            //ALL SMS TABLE VARIABLES

            const val COL_ALL_SMS_ID = "id"
            const val COL_ALL_SMS_BODY = "sms_body"
            const val TABLE_ALL_SMS = "all_sms"
            const val COL_ALL_SMS_UNIQUE_ID = "sms_unique_id"
            const val COL_TIMESTAMP_ALL_SMS = "timestamp" // Add this line

            //TRUCKS TABLE VARIABLES

            const val TABLE_TRUCKS = "trucks"
            const val COL_ID = "id"
            const val COL_TRUCK_NO = "truck_no"
            const val COL_TRUCK_MAKE = "make"
            const val COL_TRUCK_OWNER = "owner"
            const val COL_TRUCK_ACTIVE_STATUS = "active_status"

            //OWNERS TABLE VARIABLES

            const val TABLE_OWNERS = "owners"
            const val COL_OWNER_ID = "id"
            const val COL_OWNER_NAME = "name"
            const val COL_OWNER_CODE = "owner_code"



            fun dropAllTables(db: SQLiteDatabase) {

            // List all the table names you want to drop
            val tableNames = arrayOf(TABLE_TRANSACTIONS, TABLE_REJECTED_SMS, TABLE_ALL_SMS, TABLE_SEARCH_HISTORY, TABLE_TRUCKS, TABLE_OWNERS)

            for (tableName in tableNames) {
                db.execSQL("DROP TABLE IF EXISTS $tableName;")
            }

            db.close()
        }






    }
}