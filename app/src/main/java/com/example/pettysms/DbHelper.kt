package com.example.pettysms

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
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
        contentValues.put(COL_TRANSACTIONS_TRANSACTOR_CHECK, if (mpesaTransaction.transactorCheck == true) 1 else 0)





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

    fun transactorCheckUpdateTransaction(transactionId: Int){
        val db = writableDatabase

        val contentValues = ContentValues().apply {
            put(COL_TRANSACTIONS_TRANSACTOR_CHECK, 1)
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
                COL_TRANSACTIONS_IS_DELETED + " INTEGER" +
                COL_TRANSACTIONS_TRANSACTOR_CHECK + " INTEGER" +
// Removed the trailing comma
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
                $COL_TRUCK_ACTIVE_STATUS INTEGER,
                $COL_IS_DELETED INTEGER DEFAULT 0

                
            )
        """

        val SQL_CREATE_TABLE_OWNERS = """
            CREATE TABLE IF NOT EXISTS $TABLE_OWNERS (
                $COL_OWNER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OWNER_NAME TEXT, 
                $COL_OWNER_CODE TEXT,
                $COL_OWNER_LOGO_PATH TEXT,
                $COL_IS_OWNER_DELETED DEFAULT 0

            )
        """

        val SQL_CREATE_ENTRIES_SEARCH_HISTORY_TRUCKS = "CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY_TRUCKS" + "(" +
                "$COL_SEARCH_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SEARCH_HISTORY_QUERY TEXT," +
                "$COL_TIMESTAMP INTEGER" +
                ")"
                    .trimIndent()

        val SQL_CREATE_ENTRIES_SEARCH_HISTORY_OWNERS = "CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY_OWNERS" + "(" +
                "$COL_SEARCH_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SEARCH_HISTORY_QUERY TEXT," +
                "$COL_TIMESTAMP INTEGER" +
                ")"
                    .trimIndent()

        val SQL_CREATE_TABLE_TRANSACTORS = """
            CREATE TABLE IF NOT EXISTS $TABLE_TRANSACTORS (
                $COL_TRANSACTOR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRANSACTOR_NAME TEXT,
                $COL_TRANSACTOR_PHONE_NO TEXT,
                $COL_TRANSACTOR_ADDRESS TEXT DEFAULT 'N/A',
                $COL_TRANSACTOR_TYPE TEXT,
                $COL_TRANSACTOR_ID_CARD_NO INTEGER,
                $COL_IS_TRANSACTOR_DELETED BOOLEAN DEFAULT 0,
                $COL_IS_IMPORTED BOOLEAN DEFAULT 0,
                $COL_TRANSACTOR_LOGO_PATH TEXT,
                $COL_TRANSACTOR_INTERACTIONS INTEGER DEFAULT 0,
                $COL_TRANSACTOR_AVATAR_COLOR TEXT
            )
        """

        val SQL_CREATE_TABLE_ACCOUNTS = """
            CREATE TABLE IF NOT EXISTS $TABLE_ACCOUNTS (
                $COL_ACCOUNT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ACCOUNT_NAME TEXT,
                $COL_ACCOUNT_NUMBER TEXT,
                $COL_ACCOUNT_TYPE TEXT,
                $COL_ACCOUNT_OWNER INTEGER,
                $COL_ACCOUNT_CURRENCY TEXT DEFAULT 'Kenyan Shilling',
                $COL_ACCOUNT_IS_DELETED BOOLEAN DEFAULT 0
            )
        """

        val SQL_CREATE_ENTRIES_SEARCH_HISTORY_ACCOUNTS = "CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY_ACCOUNTS" + "(" +
                "$COL_SEARCH_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SEARCH_HISTORY_QUERY TEXT," +
                "$COL_TIMESTAMP INTEGER" +
                ")"
                    .trimIndent()




        db.execSQL(SQL_CREATE_ENTRIES_ALL_SMS)
        db.execSQL(SQL_CREATE_ENTRIES)
        db.execSQL(SQL_CREATE_ENTRIES_REJECTED_SMS)
        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY)
        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY_TRUCKS)
        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY_OWNERS)
        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY_ACCOUNTS)
        db.execSQL(SQL_CREATE_TABLE_OWNERS)
        db.execSQL(SQL_CREATE_TABLE_TRUCKS)
        db.execSQL(SQL_CREATE_TABLE_TRANSACTORS)
        db.execSQL(SQL_CREATE_TABLE_ACCOUNTS)



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

    fun getTransactorNotCheckedTransactions(): MutableList<MpesaTransaction> {

        val query = "SELECT * FROM $TABLE_TRANSACTIONS WHERE $COL_TRANSACTIONS_TRANSACTOR_CHECK = 0"
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
                val is_deleted = cursor.getInt(cursor.getColumnIndex(
                    COL_TRANSACTIONS_IS_DELETED))
                val transactorCheck = cursor.getInt(cursor.getColumnIndex(
                    COL_TRANSACTIONS_TRANSACTOR_CHECK))

                val transaction = MpesaTransaction(id = id, mpesaCode = mpesa_code, msgDate = msg_date, transactionDate = transaction_date,
                    account = Account(
                        id = 1,
                        name = "General Expenses",
                        type = "Expense",
                        accountNumber = null,
                        currency = "Kenyan Shillings",
                        owner = null
                    )
                    , amount = amount,
                    recipient = Recepient(name = recepient_name, phone_no = recepient_phone_no),
                    transactionType = transaction_type,
                    mpesaBalance = mpesa_balance,
                    transactionCost = transaction_cost,
                    mpesaDepositor = mpesa_depositor,
                    smsText = sms_text,
                    paybillAcount = paybill_account,
                    description = description,
                    sender = Sender(sender_name,sender_phone_no),
                    isDeleted = if (is_deleted == 1) true else false,
                    transactorCheck = if (transactorCheck == 1) true else false

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

    fun insertTruck(db: SQLiteDatabase, truck: Truck) {
        val values = ContentValues().apply {
            put(COL_TRUCK_NO, truck.truckNo)
            put(COL_TRUCK_MAKE, truck.make)
            put(COL_TRUCK_OWNER, truck.owner?.ownerCode)
            put(COL_TRUCK_ACTIVE_STATUS, if (truck.activeStatus == true) 1 else 0)
            put(COL_IS_DELETED, if (truck.isDeleted == true) 1 else 0) // Add is_deleted column
            // Add other truck details...
        }
        db.insertWithOnConflict(TABLE_TRUCKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }


    fun isTruckExists(truckNo: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_TRUCKS WHERE $COL_TRUCK_NO = ?"
        val cursor: Cursor = db.rawQuery(query, arrayOf(truckNo))
        val exists = cursor.count > 0
        return exists
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
                put(COL_IS_DELETED, if (truck.isDeleted == true) 1 else 0) // Add is_deleted column
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
                COL_TRUCK_ACTIVE_STATUS,
                COL_IS_DELETED // Include the is_deleted column
            ), "$COL_IS_DELETED = ?", arrayOf("0"), // Filter out deleted trucks
            null, null, COL_TRUCK_NO
        )
        while (cursor.moveToNext()) {
            val truckId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID))
            val truckNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_NO))
            val truckMake = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_MAKE))
            val truckOwnerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_OWNER))
            val truckActiveStatusInteger = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRUCK_ACTIVE_STATUS))
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DELETED)) == 1

            val truckActiveStatus = truckActiveStatusInteger == 1

            val owner = getOwnerByCode(truckOwnerCode) ?: Owner(1, "Abdulcon Enterprises Limited", "abdulcon")

            if (!isDeleted) {
                trucks.add(Truck(truckId, truckNo, truckMake, owner, truckActiveStatus, isDeleted))
            }
        }

        cursor.close()
        db.close()

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
        val selection = "$COL_OWNER_CODE = ? AND $COL_IS_DELETED = 0" // Ensure to exclude deleted owners
        val selectionArgs = arrayOf(ownerCode)
        val cursor = db.query(TABLE_OWNERS, null, selection, selectionArgs, null, null, null)
        var owner: Owner? = null
        try {
            if (cursor.moveToFirst()) {
                val ownerId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_NAME))
                val logoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)) // Retrieve logo path
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DELETED)) == 1 // Retrieve is_deleted flag

                owner = Owner(ownerId, ownerName, ownerCode, logoPath, isDeleted)
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
    fun insertOwner(db: SQLiteDatabase, owner: Owner) {
        val values = ContentValues().apply {
            put(COL_OWNER_ID, owner.id)
            put(COL_OWNER_NAME, owner.name)
            put(COL_OWNER_CODE, owner.ownerCode)
            put(COL_IS_DELETED, if (owner.isDeleted) 1 else 0)
            put(COL_OWNER_LOGO_PATH, owner.logoPath)
        }
        db.insert(TABLE_OWNERS, null, values)
    }

    fun getAllOwners(): List<Owner> {
        val owners = mutableListOf<Owner>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_OWNERS,
            arrayOf(COL_OWNER_ID, COL_OWNER_NAME, COL_OWNER_CODE, COL_OWNER_LOGO_PATH),
            "$COL_IS_DELETED = ?",
            arrayOf("0"),
            null,
            null,
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val ownerId = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = it.getString(it.getColumnIndexOrThrow(COL_OWNER_NAME))
                val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE))
                val logoPath = it.getString(it.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH))
                owners.add(Owner(ownerId, ownerName, ownerCode, logoPath))
            }
        }

        println("Inside owners db")
        cursor.close()
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

    fun addTruckQueryToSearchHistory(query: String) {
        val db = writableDatabase

        // Check if the query already exists in the database
        val selection = "$COL_SEARCH_HISTORY_QUERY = ?"
        val selectionArgs = arrayOf(query)
        val cursor = db.query(
            TABLE_SEARCH_HISTORY_TRUCKS,
            arrayOf(COL_SEARCH_HISTORY_QUERY),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        // If the query does not exist, insert it
        if (cursor.count == 0) {
            val contentValues = ContentValues().apply {
                put(COL_SEARCH_HISTORY_QUERY, query)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_SEARCH_HISTORY_TRUCKS, null, contentValues)
        }

        cursor.close()
        db.close()
    }

    fun clearTruckSearchHistory() {
        val db = writableDatabase
        try {
            // Clear the search history table
            db.execSQL("DELETE FROM $TABLE_SEARCH_HISTORY_TRUCKS")
        } catch (e: SQLException) {
            // Handle exceptions, if any
            e.printStackTrace()
        } finally {
            db.close()
        }
    }

    fun getTruckSearchHistory(): MutableList<String>{
        val db = readableDatabase

        val SQL_CREATE_ENTRIES_SEARCH_HISTORY_TRUCKS = "CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY_TRUCKS" + "(" +
                "$COL_SEARCH_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SEARCH_HISTORY_QUERY TEXT," +
                "$COL_TIMESTAMP INTEGER" +
                ")"
                    .trimIndent()

        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY_TRUCKS)



        val cursor = db.query(
            TABLE_SEARCH_HISTORY_TRUCKS,
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

    fun updateTruck(
        db: SQLiteDatabase,
        truckId: Int?,
        truckNo: String,
        make: String,
        owner: Owner?,
        activeStatus: Boolean,
        isDeleted: Boolean = false // New parameter for is_deleted
    ) {
        val values = ContentValues().apply {
            put(COL_TRUCK_NO, truckNo)
            put(COL_TRUCK_MAKE, make)
            owner?.ownerCode?.let { put(COL_TRUCK_OWNER, it) }
            put(COL_TRUCK_ACTIVE_STATUS, if (activeStatus) 1 else 0)
            put(COL_IS_DELETED, if (isDeleted) 1 else 0) // Add is_deleted column
        }

        val whereClause = "$COL_ID = ?"
        val whereArgs = arrayOf(truckId.toString())

        db.update(TABLE_TRUCKS, values, whereClause, whereArgs)
    }


    fun deleteTruck(db: SQLiteDatabase, id: Int?) {
        val values = ContentValues().apply {
            put(COL_IS_DELETED, 1)
        }
        db.update(TABLE_TRUCKS, values, "$COL_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun getOwnerSearchHistory(): MutableList<String> {
        val db = readableDatabase

        val SQL_CREATE_ENTRIES_SEARCH_HISTORY_TRUCKS = "CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY_OWNERS" + "(" +
                "$COL_SEARCH_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SEARCH_HISTORY_QUERY TEXT," +
                "$COL_TIMESTAMP INTEGER" +
                ")"
                    .trimIndent()

        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY_TRUCKS)



        val cursor = db.query(
            TABLE_SEARCH_HISTORY_OWNERS,
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

    fun addOwnerQueryToSearchHistory(query: String) {
        val db = writableDatabase

        // Check if the query already exists in the database
        val selection = "$COL_SEARCH_HISTORY_QUERY = ?"
        val selectionArgs = arrayOf(query)
        val cursor = db.query(
            TABLE_SEARCH_HISTORY_OWNERS,
            arrayOf(COL_SEARCH_HISTORY_QUERY),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        // If the query does not exist, insert it
        if (cursor.count == 0) {
            val contentValues = ContentValues().apply {
                put(COL_SEARCH_HISTORY_QUERY, query)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_SEARCH_HISTORY_OWNERS, null, contentValues)
        }

        cursor.close()
        db.close()
    }

    fun clearOwnerSearchHistory() {
        val db = writableDatabase
        try {
            // Clear the search history table
            db.execSQL("DELETE FROM $TABLE_SEARCH_HISTORY_OWNERS")
        } catch (e: SQLException) {
            // Handle exceptions, if any
            e.printStackTrace()
        } finally {
            db.close()
        }
    }

    fun isOwnerNameExists(ownerName: String): Boolean {
        val db = this.readableDatabase
        val selection = "$COL_OWNER_NAME = ?"
        val selectionArgs = arrayOf(ownerName)
        val cursor = db.query(
            TABLE_OWNERS,
            arrayOf(COL_OWNER_ID), // You can query any column, here we just need a count
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun isOwnerCodeExists(ownerCode: String): Boolean {
        val db = this.readableDatabase
        val selection = "$COL_OWNER_CODE = ?"
        val selectionArgs = arrayOf(ownerCode)
        val cursor = db.query(
            TABLE_OWNERS,
            arrayOf(COL_OWNER_ID), // You can query any column, here we just need a count
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun updateOwner(db: SQLiteDatabase, owner: Owner) {
        // Prepare the ContentValues to update the owner record
        val values = ContentValues().apply {
            put(COL_OWNER_NAME, owner.name)
            put(COL_OWNER_CODE, owner.ownerCode)
            put(COL_OWNER_LOGO_PATH, owner.logoPath)
            put(COL_IS_OWNER_DELETED, if (owner.isDeleted) 1 else 0)
        }

        // Define the WHERE clause to update the specific owner by ID
        val whereClause = "$COL_OWNER_ID = ?"
        val whereArgs = arrayOf(owner.id.toString())

        // Execute the update operation
        db.update(TABLE_OWNERS, values, whereClause, whereArgs)
    }

    fun doesOwnerHaveTrucks(ownerCode: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_TRUCKS WHERE $COL_TRUCK_OWNER = ?",
            arrayOf(ownerCode)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count > 0
    }

    fun deleteOwner(ownerId: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_OWNER_DELETED, 1)  // Set the is_deleted flag to 1 (true)
        }
        val whereClause = "$COL_OWNER_ID = ?"
        val whereArgs = arrayOf(ownerId.toString())

        // Perform the soft delete operation
        val rowsUpdated = db.update(TABLE_OWNERS, values, whereClause, whereArgs)
        db.close()

        // Return true if one or more rows were updated, false otherwise
        return rowsUpdated > 0
    }


    fun getAllTransactors(): List<Transactor> {
        val transactors = mutableListOf<Transactor>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRANSACTORS", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_NAME))
                val phoneNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_PHONE_NO))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_ADDRESS))
                val transactorType = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_TYPE))
                val idCardNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_ID_CARD_NO))
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_TRANSACTOR_DELETED)) > 0
                val isImported = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_IMPORTED)) > 0
                val logoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_LOGO_PATH))
                val interactions = cursor.getInt(cursor.getColumnIndexOrThrow(
                    COL_TRANSACTOR_INTERACTIONS))
                val avatarColor = cursor.getString(cursor.getColumnIndexOrThrow(
                    COL_TRANSACTOR_AVATAR_COLOR))


                val transactor = Transactor(id, name, phoneNo, idCardNo, address, transactorType, transactorProfilePicturePath = logoPath, interactions, isDeleted, isImported, avatarColor )
                transactors.add(transactor)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return transactors
    }

    fun isTableExists(db: SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'", null)
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun insertTransactors(transactors: List<Transactor>) {
        val db = this.writableDatabase
        println("transactor insert")

        // Check if the transactors table exists, create if it doesn't
        if (!isTableExists(db, TABLE_TRANSACTORS)) {
            onCreate(db)
        }

        db.beginTransaction()
        try {
            // Insert each transactor into the transactors table
            for (transactor in transactors) {
                insertTransactor(db, transactor) // Pass the database instance to the insertTransactor function
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

    fun insertTransactor(db: SQLiteDatabase, transactor: Transactor): Long {
        if (transactorExists(db, transactor.name, transactor.phoneNumber)) {
            return -1 // Transactor already exists
        }else if(checkIfTransactorExistsByPhoneNumber(db, transactor?.phoneNumber.toString()) && transactor?.transactorType != "Corporate"){
            updateTransactorName(db, transactor)
        }

        if (!transactor.name.isNullOrEmpty()){
            val contentValues = ContentValues().apply {
                put(COL_TRANSACTOR_NAME, transactor.name)
                put(COL_TRANSACTOR_PHONE_NO, transactor.phoneNumber)
                put(COL_TRANSACTOR_ADDRESS, transactor.address)
                put(COL_TRANSACTOR_TYPE, transactor.transactorType)
                put(COL_TRANSACTOR_ID_CARD_NO, transactor.idCard)
                put(COL_IS_TRANSACTOR_DELETED, transactor.isDeleted)
                put(COL_IS_IMPORTED, transactor.isImported)
                put(COL_TRANSACTOR_LOGO_PATH, transactor.transactorProfilePicturePath)
                put(COL_TRANSACTOR_INTERACTIONS, transactor.interactions)
                put(COL_TRANSACTOR_AVATAR_COLOR, transactor.avatarColor)
            }

            println("transactor insert")

            return db.insert(TABLE_TRANSACTORS, null, contentValues)
        }else{
            return -1
        }


    }

    fun updateTransactor(db: SQLiteDatabase, transactor: Transactor): Int {
        // Prepare the ContentValues with the fields that need to be updated
        val contentValues = ContentValues().apply {
            put(COL_TRANSACTOR_NAME, transactor.name)
            put(COL_TRANSACTOR_PHONE_NO, transactor.phoneNumber)
            put(COL_TRANSACTOR_ADDRESS, transactor.address)
            put(COL_TRANSACTOR_TYPE, transactor.transactorType)
            put(COL_TRANSACTOR_ID_CARD_NO, transactor.idCard)
            put(COL_TRANSACTOR_LOGO_PATH, transactor.transactorProfilePicturePath)
        }

        // Define the WHERE clause to update based on the transactor ID
        val whereClause = "$COL_TRANSACTOR_ID = ?"
        val whereArgs = arrayOf(transactor.id.toString())

        // Update the transactor in the database and return the number of rows affected
        return db.update(TABLE_TRANSACTORS, contentValues, whereClause, whereArgs)
    }

    private fun updateTransactorName(db: SQLiteDatabase, transactor: Transactor): Int {

        val contentValues = ContentValues().apply {
            put(COL_TRANSACTOR_NAME, transactor.name)
        }

        // Update the transactor record in the database where the name and phone number match
        val whereClause = "$COL_TRANSACTOR_PHONE_NO = ?"
        val whereArgs = arrayOf(transactor.phoneNumber)

        return db.update(TABLE_TRANSACTORS, contentValues, whereClause, whereArgs)
    }

    private fun transactorExists(db: SQLiteDatabase, name: String?, phoneNo: String?): Boolean {
        val queryBuilder = StringBuilder("SELECT * FROM $TABLE_TRANSACTORS WHERE ")
        val queryParams = mutableListOf<String>()

        if (name != null) {
            queryBuilder.append("$COL_TRANSACTOR_NAME = ?")
            queryParams.add(name)
        } else {
            queryBuilder.append("$COL_TRANSACTOR_NAME IS NULL")
        }

        if (phoneNo != null) {
            queryBuilder.append(" AND $COL_TRANSACTOR_PHONE_NO = ?")
            queryParams.add(phoneNo)
        } else {
            queryBuilder.append(" AND $COL_TRANSACTOR_PHONE_NO IS NULL")
        }

        val cursor: Cursor = db.rawQuery(queryBuilder.toString(), queryParams.toTypedArray())
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun incrementTransactorInteractions(transactorId: Int) {
        val db = this.writableDatabase
        val incrementQuery = """
            UPDATE transactors
            SET interactions = interactions + 1
            WHERE id = ?
        """
        db.execSQL(incrementQuery, arrayOf(transactorId))
        db.close()
    }

    fun checkIfTransactorExistsByPhoneNumber(db: SQLiteDatabase?, phoneNumber: String): Boolean {
        val query = "SELECT COUNT(*) FROM $TABLE_TRANSACTORS WHERE $COL_TRANSACTOR_PHONE_NO = ?"
        val cursor = db?.rawQuery(query, arrayOf(phoneNumber))

        var exists = false
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0 // Check if count is greater than 0
            }
            cursor.close()
        }
        return exists
    }

    fun deleteTransactor(transactor: Transactor): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_IS_TRANSACTOR_DELETED, 1) // Set is_deleted to 1 (soft delete)
        }

        // Ensure the Transactor has a valid ID before proceeding
        val transactorId = transactor.id ?: return false

        // Update the transactor's record where the id matches
        val result = db.update(
            TABLE_TRANSACTORS,
            contentValues,
            "$COL_TRANSACTOR_ID = ?",
            arrayOf(transactorId.toString())
        )

        db.close()

        return result > 0 // Return true if at least one row was updated
    }


    fun insertAccount(db: SQLiteDatabase, account: Account) {
        val values = ContentValues().apply {
            put(COL_ACCOUNT_ID, account.id) // Assuming this is set to null for autoincrement
            put(COL_ACCOUNT_NAME, account.name)
            put(COL_ACCOUNT_NUMBER, account.accountNumber)
            put(COL_ACCOUNT_TYPE, account.type)
            put(COL_ACCOUNT_OWNER, account.owner?.ownerCode) // Assuming this is a foreign key
            put(COL_ACCOUNT_CURRENCY, account.currency ?: "Kenyan Shilling") // Default currency if null
            put(COL_ACCOUNT_IS_DELETED, if (account.isDeleted) 1 else 0) // Convert boolean to int
        }

        try {
            db.insertOrThrow(TABLE_ACCOUNTS, null, values)
            println("Account inserted successfully: ${account.name}")
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
    }

    fun insertAccounts(accounts: List<Account>) {
        val db = this.writableDatabase
        println("account insert")

        db.beginTransaction()
        try {
            // Insert each account into the accounts table
            for (account in accounts) {
                insertAccount(db, account) // Pass the database instance to the insertAccount function
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

    fun getAllAccountsWithPagination(offset: Int, limit: Int = 30): List<Account> {
        val db = this.readableDatabase
        val accounts = mutableListOf<Account>()

        // SQL query with LIMIT and OFFSET
        val sql = "SELECT * FROM $TABLE_ACCOUNTS LIMIT ? OFFSET ?"
        val cursor = db.rawQuery(sql, arrayOf(limit.toString(), offset.toString()))

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                    val accountNumber = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER))
                    val type = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_TYPE))
                    val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_OWNER))
                    val currency = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED)) == 1

                    // Fetch the owner
                    val owner = getOwnerByCode(ownerCode)

                    // Create the Account object
                    val account = Account(id, name, owner, type, currency, accountNumber, isDeleted)
                    accounts.add(account)
                } while (it.moveToNext())
            }
        }

        return accounts
    }


    fun getAllAccounts(): List<Account> {
        val db = this.readableDatabase
        val accounts = mutableListOf<Account>()

        // SQL query to select all records from the accounts table
        val sql = "SELECT * FROM $TABLE_ACCOUNTS"
        val cursor: Cursor? = db.rawQuery(sql, null)

        cursor?.use {
            // Check if the cursor has any records
            if (it.moveToFirst()) {
                do {
                    // Extracting data from the cursor to create Account objects
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                    val accountNumber = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER))
                    val type = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_TYPE))
                    val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_OWNER))
                    val currency = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED)) == 1

                    // Creating an Owner object based on owner ID (you'll need to adjust this if you have an Owner class)
                    val owner = getOwnerByCode(ownerCode) // Adjust this as necessary based on how the Owner class is defined

                    // Creating an Account object and adding it to the list
                    val account = Account(id, name, owner, type, currency, accountNumber, isDeleted)
                    accounts.add(account)
                } while (it.moveToNext())
            }
        }

        return accounts
    }

    fun getOwnerByName(ownerName: String): Owner? {
        val db = this.readableDatabase
        val selection = "$COL_OWNER_NAME = ? AND $COL_IS_DELETED = 0" // Ensure to exclude deleted owners
        val selectionArgs = arrayOf(ownerName)
        val cursor = db.query(TABLE_OWNERS, null, selection, selectionArgs, null, null, null)
        var owner: Owner? = null
        try {
            if (cursor.moveToFirst()) {
                val ownerId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_CODE))
                val logoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)) // Retrieve logo path
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DELETED)) == 1 // Retrieve is_deleted flag

                owner = Owner(ownerId, ownerName, ownerCode, logoPath, isDeleted)
            }
        } finally {
            cursor.close()
            // Do not close the database connection here
        }
        // Close the database connection outside of the try-finally block
        return owner
    }

    fun getAccountSearchHistory(): MutableList<String> {
        val db = readableDatabase

        // Create the table if it doesn't exist
        val SQL_CREATE_ENTRIES_SEARCH_HISTORY_ACCOUNTS = "CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY_ACCOUNTS" + "(" +
                "$COL_SEARCH_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SEARCH_HISTORY_QUERY TEXT," +
                "$COL_TIMESTAMP INTEGER" +
                ")"
                    .trimIndent()

        db.execSQL(SQL_CREATE_ENTRIES_SEARCH_HISTORY_ACCOUNTS)

        // Query to get the search history
        val cursor = db.query(
            TABLE_SEARCH_HISTORY_ACCOUNTS,
            arrayOf(COL_SEARCH_HISTORY_QUERY),
            null,    // no selection
            null,    // no selection args
            null,    // no group by
            null,    // no having
            "$COL_TIMESTAMP DESC"    // order by timestamp descending
        )

        // Initialize the search history list
        val searchHistory = mutableListOf<String>()

        cursor.use {
            // Check if the cursor has at least one row
            if (it.count > 0) {
                it.moveToFirst() // Move to the first row
                do {
                    val query = it.getString(it.getColumnIndexOrThrow(COL_SEARCH_HISTORY_QUERY))
                    searchHistory.add(query)
                } while (it.moveToNext()) // Iterate over all rows
            }
        }

        return searchHistory
    }



    fun addAccountQueryToSearchHistory(query: String) {
        val db = writableDatabase

        // Check if the query already exists in the database
        val selection = "$COL_SEARCH_HISTORY_QUERY = ?"
        val selectionArgs = arrayOf(query)
        val cursor = db.query(
            TABLE_SEARCH_HISTORY_ACCOUNTS,
            arrayOf(COL_SEARCH_HISTORY_QUERY),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        // If the query does not exist, insert it
        if (cursor.count == 0) {
            val contentValues = ContentValues().apply {
                put(COL_SEARCH_HISTORY_QUERY, query)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_SEARCH_HISTORY_ACCOUNTS, null, contentValues)
        }

        cursor.close()
    }

    fun clearAccountSearchHistory() {
        val db = writableDatabase
        try {
            // Clear the search history table
            db.execSQL("DELETE FROM $TABLE_SEARCH_HISTORY_ACCOUNTS")
        } catch (e: SQLException) {
            // Handle exceptions, if any
            e.printStackTrace()
        } finally {
            //db.close()
        }
    }

    fun getAccountsByName(query: String): MutableList<Account> {
        val accountsList = mutableListOf<Account>()
        val db = this.readableDatabase

        // Use a parameterized query to prevent SQL injection
        val selection = "$COL_ACCOUNT_NAME LIKE ? AND $COL_ACCOUNT_IS_DELETED = ?"
        val selectionArgs = arrayOf("%$query%", "0") // Assuming "0" means not deleted

        val cursor = db.query(
            TABLE_ACCOUNTS,
            null, // Select all columns
            selection,
            selectionArgs,
            null, // Group by
            null, // Having
            null // Order by
        )

        // Iterate through the results
        if (cursor.moveToFirst()) {
            do {
                val accountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACCOUNT_ID))
                val accountName = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                val accountNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER))
                val accountType = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_TYPE))
                val accountOwner = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_OWNER))
                val accountCurrency = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))
                val accountIsDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED))

                val owner = getOwnerByCode(accountOwner) // Adjust this as necessary based on how the Owner class is defined


                // Create an Account object and add it to the list
                val account = Account(
                    id = accountId,
                    name = accountName,
                    accountNumber = accountNumber,
                    type = accountType,
                    owner = owner,
                    currency = accountCurrency,
                    isDeleted = accountIsDeleted == 1 // Assuming 1 means deleted
                )
                accountsList.add(account)

            } while (cursor.moveToNext())
        }

        cursor.close() // Always close the cursor after use
        db.close() // Close the database connection

        return accountsList
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
            const val COL_TRANSACTIONS_TRANSACTOR_CHECK = "transactor_check"


        //REJECTED SMS TABLE VARIABLES

            const val TABLE_REJECTED_SMS = "rejected_sms"
            const val COL_REJECTED_MESSAGES_ID = "id"
            const val COL_REJECTED_MESSAGES_DATE = "date"
            const val COL_REJECTED_MESSAGES_SMS_BODY = "sms_body"

            //SEARCH HISTORY TABLE VARIABLES

            const val COL_SEARCH_HISTORY_ID = "id"
            const val COL_SEARCH_HISTORY_QUERY = "query"
            const val TABLE_SEARCH_HISTORY = "search_history"
            const val TABLE_SEARCH_HISTORY_TRUCKS = "search_history_trucks"
            const val TABLE_SEARCH_HISTORY_OWNERS = "search_history_owners"
            const val TABLE_SEARCH_HISTORY_ACCOUNTS = "search_history_accounts"
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
            const val COL_IS_DELETED = "is_deleted"


        //OWNERS TABLE VARIABLES

            const val TABLE_OWNERS = "owners"
            const val COL_OWNER_ID = "id"
            const val COL_OWNER_NAME = "name"
            const val COL_OWNER_CODE = "owner_code"
            const val COL_IS_OWNER_DELETED = "is_deleted"
            const val COL_OWNER_LOGO_PATH = "logo_path"

            //TRANSACTORS TABLE VARIABLES

            const val TABLE_TRANSACTORS = "transactors"
            const val COL_TRANSACTOR_ID = "id"
            const val COL_TRANSACTOR_NAME = "name"
            const val COL_TRANSACTOR_PHONE_NO = "phone_no"
            const val COL_TRANSACTOR_ADDRESS = "address"
            const val COL_TRANSACTOR_TYPE = "transactor_type"
            const val COL_TRANSACTOR_ID_CARD_NO = "id_card_no"
            const val COL_IS_TRANSACTOR_DELETED = "is_deleted"
            const val COL_IS_IMPORTED = "is_imported"
            const val COL_TRANSACTOR_LOGO_PATH = "logo_path"
            const val COL_TRANSACTOR_INTERACTIONS = "interactions"
            const val COL_TRANSACTOR_AVATAR_COLOR = "avatar_color"

            // ACCOUNTS TABLE VARIABLES

            const val TABLE_ACCOUNTS = "accounts"
            const val COL_ACCOUNT_ID = "id"
            const val COL_ACCOUNT_NAME = "name"
            const val COL_ACCOUNT_NUMBER = "account_number"
            const val COL_ACCOUNT_TYPE = "account_type"
            const val COL_ACCOUNT_OWNER = "owner"
            const val COL_ACCOUNT_CURRENCY = "currency"
            const val COL_ACCOUNT_IS_DELETED = "is_deleted"



            fun dropAllTables(db: SQLiteDatabase) {

            // List all the table names you want to drop
            val tableNames = arrayOf(TABLE_TRANSACTIONS, TABLE_REJECTED_SMS, TABLE_ALL_SMS, TABLE_SEARCH_HISTORY, TABLE_TRUCKS, TABLE_OWNERS, TABLE_SEARCH_HISTORY_TRUCKS, TABLE_ACCOUNTS, TABLE_SEARCH_HISTORY_ACCOUNTS)

            for (tableName in tableNames) {
                db.execSQL("DROP TABLE IF EXISTS $tableName;")
            }

            db.close()
        }






    }
}