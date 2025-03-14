package com.example.pettysms

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.example.pettysms.queue.QueueItem
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.Date
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pettysms.reports.Report
import com.example.pettysms.reports.ReportType
import com.example.pettysms.models.PettyCashTransaction

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

    // Open database connection
    fun openDatabase() {
        this.writableDatabase
    }

    // Close database connection
    fun closeDatabase() {
        this.close()
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
        contentValues.put(COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH, if (mpesaTransaction.isConvertedToPettyCash == true) 1 else 0)





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

    fun transactorCheckUpdateTransaction(transaction: MpesaTransaction) {
        val db = writableDatabase

        val transactionCode = transaction.mpesa_code

        val query = """
        UPDATE $TABLE_TRANSACTIONS 
        SET $COL_TRANSACTIONS_TRANSACTOR_CHECK = 1 
        WHERE $COL_TRANSACTIONS_MPESA_CODE = '$transactionCode'
    """

        db.execSQL(query)
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
                val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_TRANSACTIONS (
                $COL_TRANSACTIONS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRANSACTIONS_MPESA_CODE TEXT UNIQUE,
                $COL_TRANSACTIONS_MESSAGE_DATE TEXT,
                $COL_TRANSACTIONS_TRANSACTION_DATE TEXT,
                $COL_TRANSACTIONS_RECEPIENT_NAME TEXT,
                $COL_TRANSACTIONS_RECEPIENT_NO TEXT,
                $COL_TRANSACTIONS_AMOUNT REAL,
                $COL_TRANSACTIONS_ACCOUNT_NO INTEGER,
                $COL_TRANSACTIONS_COMPANY_OWNER TEXT,
                $COL_TRANSACTIONS_TRANSACTION_TYPE TEXT,
                $COL_TRANSACTIONS_USER TEXT,
                $COL_TRANSACTIONS_MPESA_BALANCE REAL,
                $COL_TRANSACTIONS_TRANSACTION_COST REAL,
                $COL_TRANSACTIONS_MPESA_DEPOSITOR TEXT,
                $COL_TRANSACTIONS_SMS_TEXT TEXT,
                $COL_TRANSACTIONS_PAYBILL_ACCOUNT TEXT,
                $COL_TRANSACTIONS_SENDER_NAME TEXT,
                $COL_TRANSACTIONS_SENDER_PHONE_NO TEXT,
                $COL_TRANSACTIONS_DESCRIPTION TEXT,
                $COL_TRANSACTIONS_IS_DELETED INTEGER,
                $COL_TRANSACTIONS_TRANSACTOR_CHECK INTEGER,
                $COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH INTEGER
            )
        """.trimIndent()

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
                    "$COL_ALL_SMS_UNIQUE_ID INTEGER," + // New column for storing SMS ID
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
                $COL_TRANSACTOR_KRA_PIN TEXT,
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

        val SQL_CREATE_ENTRIES_PETTY_CASH = "CREATE TABLE IF NOT EXISTS $TABLE_PETTY_CASH (" +
                "$COL_PETTY_CASH_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_PETTY_CASH_NUMBER TEXT," +
                "$COL_PETTY_CASH_AMOUNT REAL," +
                "$COL_PETTY_CASH_DATE TEXT," +
                "$COL_PETTY_CASH_DESCRIPTION TEXT," +
                "$COL_PETTY_CASH_TRANSACTOR INTEGER," +
                "$COL_PETTY_CASH_ACCOUNT INTEGER," +
                "$COL_PETTY_CASH_PAYMENT_MODE TEXT," +
                "$COL_PETTY_CASH_OWNER TEXT," +
                "$COL_PETTY_CASH_MPESA_TRANSACTION TEXT," +  // Not unique
                "$COL_PETTY_CASH_TRUCKS TEXT," +
                "$COL_PETTY_CASH_SIGNATURE TEXT," +  // Signature as TEXT
                "$COL_PETTY_CASH_SUPPORTING_DOCUMENT INTEGER," +
                "$COL_PETTY_CASH_USER TEXT," +
                "$COL_PETTY_CASH_IS_DELETED INTEGER DEFAULT 0" +  // 0 for not deleted, 1 for deleted
                ")".trimIndent()

        val SQL_CREATE_ENTRIES_SUPPORTING_DOCUMENT = "CREATE TABLE IF NOT EXISTS $TABLE_SUPPORTING_DOCUMENT (" +
                "$COL_SUPPORTING_DOCUMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_SUPPORTING_DOCUMENT_TYPE TEXT," +
                "$COL_SUPPORTING_DOCUMENT_DOCUMENT_NO TEXT," +
                "$COL_SUPPORTING_DOCUMENT_CU_NUMBER TEXT," +
                "$COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME TEXT," +
                "$COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT REAL," +
                "$COL_SUPPORTING_DOCUMENT_TAX_AMOUNT REAL," +
                "$COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT REAL," +
                "$COL_SUPPORTING_DOCUMENT_DATE TEXT," +
                "$COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1 TEXT," +
                "$COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2 TEXT," +
                "$COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3 TEXT" +
                ")".trimIndent()

        // Create automation_rules table
        val CREATE_AUTOMATION_RULES_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_AUTOMATION_RULES (
                $KEY_AUTOMATION_RULE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_AUTOMATION_RULE_NAME TEXT NOT NULL,
                $KEY_AUTOMATION_RULE_TRANSACTOR_ID INTEGER,
                $KEY_AUTOMATION_RULE_ACCOUNT_ID INTEGER,
                $KEY_AUTOMATION_RULE_OWNER_ID INTEGER,
                $KEY_AUTOMATION_RULE_TRUCK_ID INTEGER,
                $KEY_AUTOMATION_RULE_DESCRIPTION_PATTERN TEXT,
                $KEY_AUTOMATION_RULE_MIN_AMOUNT REAL,
                $KEY_AUTOMATION_RULE_MAX_AMOUNT REAL,
                $KEY_AUTOMATION_RULE_CREATED_AT TEXT,
                $KEY_AUTOMATION_RULE_UPDATED_AT TEXT,
                FOREIGN KEY ($KEY_AUTOMATION_RULE_TRANSACTOR_ID) REFERENCES $TABLE_TRANSACTORS($COL_TRANSACTOR_ID) ON DELETE SET NULL,
                FOREIGN KEY ($KEY_AUTOMATION_RULE_ACCOUNT_ID) REFERENCES $TABLE_ACCOUNTS($COL_ACCOUNT_ID) ON DELETE SET NULL,
                FOREIGN KEY ($KEY_AUTOMATION_RULE_OWNER_ID) REFERENCES $TABLE_OWNERS($COL_OWNER_ID) ON DELETE SET NULL,
                FOREIGN KEY ($KEY_AUTOMATION_RULE_TRUCK_ID) REFERENCES $TABLE_TRUCKS($COL_ID) ON DELETE SET NULL
            )
        """
        db.execSQL(CREATE_AUTOMATION_RULES_TABLE)









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
        db.execSQL(SQL_CREATE_ENTRIES_PETTY_CASH)
        db.execSQL(SQL_CREATE_ENTRIES_SUPPORTING_DOCUMENT)

        // Create queue table
        createQueueTable(db)



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
    
    /**
     * Gets filtered M-Pesa transactions based on date range, transaction type, and transactor
     * This performs filtering at the database level for better performance with large datasets
     *
     * @param startDate The start date in "dd MMM yyyy" format
     * @param endDate The end date in "dd MMM yyyy" format
     * @param transactionType The transaction type filter ("All", "Incoming", or "Outgoing")
     * @param transactor The transactor name filter (empty or "All" for all transactors)
     * @return A list of filtered M-Pesa transactions
     */
    fun getFilteredMpesaTransactions(
        startDate: String,
        endDate: String,
        transactionType: String,
        transactor: String
    ): MutableList<MpesaTransaction> {
        Log.d("DbHelper", "Getting filtered transactions with: startDate=$startDate, endDate=$endDate, type=$transactionType, transactor=$transactor")
        
        // Convert dates from "dd MMM yyyy" to "dd/MM/yyyy" format for SQL comparison
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val sqlDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        val startDateObj = try {
            dateFormat.parse(startDate)
        } catch (e: Exception) {
            Log.e("DbHelper", "Error parsing start date: $startDate", e)
            return mutableListOf()
        }
        
        val endDateObj = try {
            dateFormat.parse(endDate)
        } catch (e: Exception) {
            Log.e("DbHelper", "Error parsing end date: $endDate", e)
            return mutableListOf()
        }
        
        // Add one day to end date to include transactions on the end date
        val calendar = Calendar.getInstance()
        calendar.time = endDateObj
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endDatePlusOne = calendar.time
        
        val startDateFormatted = sqlDateFormat.format(startDateObj)
        val endDateFormatted = sqlDateFormat.format(endDatePlusOne)
        
        // Build the base query
        val queryBuilder = StringBuilder()
        queryBuilder.append("SELECT * FROM $TABLE_TRANSACTIONS WHERE ")
        
        // Add date range filter
        // Compare dates using SQLite's substr function to handle the date format
        queryBuilder.append("(")
        queryBuilder.append("substr($COL_TRANSACTIONS_TRANSACTION_DATE, 7, 4) || '-' || ")
        queryBuilder.append("substr($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 2) || '-' || ")
        queryBuilder.append("substr($COL_TRANSACTIONS_TRANSACTION_DATE, 1, 2)")
        queryBuilder.append(" BETWEEN ")
        queryBuilder.append("'${startDateFormatted.substring(6, 10)}-${startDateFormatted.substring(3, 5)}-${startDateFormatted.substring(0, 2)}'")
        queryBuilder.append(" AND ")
        queryBuilder.append("'${endDateFormatted.substring(6, 10)}-${endDateFormatted.substring(3, 5)}-${endDateFormatted.substring(0, 2)}'")
        queryBuilder.append(")")
        
        // Add transaction type filter if not "All"
        if (transactionType != "All") {
            queryBuilder.append(" AND (")
            if (transactionType == "Incoming") {
                queryBuilder.append("$COL_TRANSACTIONS_TRANSACTION_TYPE IN ('Buy Goods', 'deposit', 'receival')")
            } else if (transactionType == "Outgoing") {
                queryBuilder.append("$COL_TRANSACTIONS_TRANSACTION_TYPE IN ('paybill', 'send_money', 'withdraw', 'till', 'topup')")
            }
            queryBuilder.append(")")
        }
        
        // Add transactor filter if specified
        if (transactor.isNotEmpty() && transactor != "All") {
            // This is a simplified approach - for a complete solution, you would need to 
            // implement logic similar to MpesaTransaction.getTitleTextByTransactionType
            // to match transactor names based on transaction type
            queryBuilder.append(" AND (")
            queryBuilder.append("$COL_TRANSACTIONS_RECEPIENT_NAME LIKE '%$transactor%' OR ")
            queryBuilder.append("$COL_TRANSACTIONS_SENDER_NAME LIKE '%$transactor%' OR ")
            queryBuilder.append("$COL_TRANSACTIONS_MPESA_DEPOSITOR LIKE '%$transactor%'")
            queryBuilder.append(")")
        }
        
        // Add sorting by date (newest first)
        queryBuilder.append(" ORDER BY ")
        queryBuilder.append("substr($COL_TRANSACTIONS_TRANSACTION_DATE, 7, 4) || '-' || ")
        queryBuilder.append("substr($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 2) || '-' || ")
        queryBuilder.append("substr($COL_TRANSACTIONS_TRANSACTION_DATE, 1, 2) || ' ' || ")
        queryBuilder.append("substr($COL_TRANSACTIONS_TRANSACTION_DATE, 12, 8) ASC")
        
        val query = queryBuilder.toString()
        Log.d("DbHelper", "Executing SQL query: $query")
        
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
    fun getTransactionsFromQuery(query: String): MutableList<MpesaTransaction> {
        val transactions = mutableListOf<MpesaTransaction>()
        val db = readableDatabase

        // Use try-catch for error handling
        try {
            val cursor = db.rawQuery(query, null)

            cursor.use {
                if (it.moveToFirst()) {
                    do {
                        val id = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_ID))
                        val mpesa_code = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_CODE))
                        val recepient_name = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NAME))
                        val msg_date = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MESSAGE_DATE))
                        val transaction_date = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_DATE))
                        val recepient_phone_no = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NO))
                        val amount = it.getDouble(it.getColumnIndexOrThrow(COL_TRANSACTIONS_AMOUNT))
                        val account_id = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_ACCOUNT_NO))
                        val transaction_type = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_TYPE))
                        val mpesa_balance = it.getDouble(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_BALANCE))
                        val transaction_cost = it.getDouble(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_COST))
                        val mpesa_depositor = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_DEPOSITOR))
                        val sms_text = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_SMS_TEXT))
                        val paybill_account = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_PAYBILL_ACCOUNT))
                        val description = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_DESCRIPTION))
                        val sender_name = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_NAME))
                        val sender_phone_no = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_PHONE_NO))
                        val is_deleted = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_DELETED))
                        val transactorCheck = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTOR_CHECK))
                        val isConvertedToPettyCash = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH))

                        // Create the transaction object
                        val transaction = MpesaTransaction(
                            id = id,
                            mpesaCode = mpesa_code,
                            msgDate = msg_date,
                            transactionDate = transaction_date,
                            account = Account(
                                id = account_id,
                                name = "General Expenses", // Placeholder, replace as necessary
                                type = "Expense", // Placeholder, replace as necessary
                                accountNumber = null, // Placeholder, replace as necessary
                                currency = "Kenyan Shillings", // Placeholder, replace as necessary
                                owner = null // Placeholder, replace as necessary
                            ),
                            amount = amount,
                            recipient = Recepient(name = recepient_name, phone_no = recepient_phone_no),
                            transactionType = transaction_type,
                            mpesaBalance = mpesa_balance,
                            transactionCost = transaction_cost,
                            mpesaDepositor = mpesa_depositor,
                            smsText = sms_text,
                            paybillAcount = paybill_account,
                            description = description,
                            sender = Sender(sender_name, sender_phone_no),
                            isDeleted = is_deleted == 1,
                            transactorCheck = transactorCheck == 1,
                            isConvertedToPettyCash = isConvertedToPettyCash == 1
                        )

                        transactions.add(transaction)
                    } while (it.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching transactions from query: $query", e)
        } finally {
            db.close() // Ensure the database connection is closed
        }

        return transactions
    }


    @SuppressLint("Range")
    fun getTransactionsFromQueryInTextInput(query: String): MutableList<MpesaTransaction> {
        val transactions = mutableListOf<MpesaTransaction>()
        val db = readableDatabase

        // Use a parameterized query to prevent SQL injection
        val sqlQuery = """
            SELECT * FROM $TABLE_TRANSACTIONS 
            WHERE 
                (${COL_TRANSACTIONS_RECEPIENT_NAME} LIKE ? 
                OR ${COL_TRANSACTIONS_TRANSACTION_TYPE} LIKE ? 
                OR ${COL_TRANSACTIONS_AMOUNT} LIKE ? 
                OR ${COL_TRANSACTIONS_TRANSACTION_DATE} LIKE ? 
                OR ${COL_TRANSACTIONS_MPESA_CODE} LIKE ? 
                OR ${COL_TRANSACTIONS_MPESA_DEPOSITOR} LIKE ? 
                OR ${COL_TRANSACTIONS_SENDER_NAME} LIKE ? 
                OR ${COL_TRANSACTIONS_ID} LIKE ?)
                AND ${COL_TRANSACTIONS_TRANSACTION_TYPE} NOT IN ('deposit', 'reverse', 'receival')
            ORDER BY datetime(
                SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 7, 4) || '-' || 
                SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 4, 2) || '-' || 
                SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 1, 2) || ' ' || 
                SUBSTR($COL_TRANSACTIONS_TRANSACTION_DATE, 12, 8)
            ) DESC 
            LIMIT 5
        """.trimIndent()

        // Prepare arguments for the query
        val args = Array(8) { "%$query%" }

        // Use try-catch for error handling
        try {
            db.rawQuery(sqlQuery, args).use { cursor ->
                while (cursor.moveToNext()) {
                    val transaction = createTransactionFromCursor(cursor)
                    transactions.add(transaction)
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching transactions from query: $query", e)
        }

        return transactions
    }


    // Helper function to create an MpesaTransaction from a cursor
    private fun createTransactionFromCursor(cursor: Cursor): MpesaTransaction {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_ID))
        val mpesa_code = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_CODE))
        val recepient_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NAME))
        val msg_date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MESSAGE_DATE))
        val transaction_date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_DATE))
        val recepient_phone_no = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NO))
        val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_AMOUNT))
        val account_id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_ACCOUNT_NO))
        val transaction_type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_TYPE))
        val mpesa_balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_BALANCE))
        val transaction_cost = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_COST))
        val mpesa_depositor = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_DEPOSITOR))
        val sms_text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SMS_TEXT))
        val paybill_account = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_PAYBILL_ACCOUNT))
        val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_DESCRIPTION))
        val sender_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_NAME))
        val sender_phone_no = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_PHONE_NO))
        val is_deleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_DELETED)) == 1
        val transactorCheck = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTOR_CHECK)) == 1
        val isConvertedToPettyCash = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH)) == 1

        return MpesaTransaction(
            id = id,
            mpesaCode = mpesa_code,
            msgDate = msg_date,
            transactionDate = transaction_date,
            account = Account(
                id = account_id,
                name = "General Expenses", // Placeholder, replace as necessary
                type = "Expense", // Placeholder, replace as necessary
                accountNumber = null, // Placeholder, replace as necessary
                currency = "Kenyan Shillings", // Placeholder, replace as necessary
                owner = null // Placeholder, replace as necessary
            ),
            amount = amount,
            recipient = Recepient(name = recepient_name, phone_no = recepient_phone_no),
            transactionType = transaction_type,
            mpesaBalance = mpesa_balance,
            transactionCost = transaction_cost,
            mpesaDepositor = mpesa_depositor,
            smsText = sms_text,
            paybillAcount = paybill_account,
            description = description,
            sender = Sender(sender_name, sender_phone_no),
            isDeleted = is_deleted,
            transactorCheck = transactorCheck,
            isConvertedToPettyCash = isConvertedToPettyCash
        )
    }

    fun getNotConvertedMpesaTransactions(): MutableList<MpesaTransaction> {
        val db = readableDatabase
        val transactions = mutableListOf<MpesaTransaction>()
        var cursor: Cursor? = null

        // SQL query to fetch non-converted Mpesa transactions
        val query = """
            SELECT * FROM $TABLE_TRANSACTIONS 
            WHERE $COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH = 0 
            AND $COL_TRANSACTIONS_TRANSACTION_TYPE IN ('withdraw', 'till', 'paybill', 'send_money', 'topup')
        """
        try {
            cursor = db.rawQuery(query, null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_ID))
                    val mpesa_code = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_CODE))
                    val recepient_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NAME))
                    val msg_date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MESSAGE_DATE))
                    val transaction_date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_DATE))
                    val recepient_phone_no = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NO))
                    val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_AMOUNT))
                    val account_id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_ACCOUNT_NO))
                    val company_owner_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_COMPANY_OWNER))
                    val transaction_type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_TYPE))
                    val user_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_USER))
                    val mpesa_balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_BALANCE))
                    val transaction_cost = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_COST))
                    val mpesa_depositor = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_DEPOSITOR))
                    val sms_text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SMS_TEXT))
                    val paybill_account = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_PAYBILL_ACCOUNT))
                    val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_DESCRIPTION))
                    val sender_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_NAME))
                    val sender_phone_no = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_PHONE_NO))
                    val is_deleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_DELETED))
                    val transactorCheck = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTOR_CHECK))
                    val isConvertedToPettyCash = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH))

                    val transaction = MpesaTransaction(
                        id = id,
                        mpesaCode = mpesa_code,
                        msgDate = msg_date,
                        transactionDate = transaction_date,
                        account = Account(
                            id = account_id,
                            name = "General Expenses",  // Placeholder; fetch real account details if available
                            type = "Expense",  // Placeholder; modify as needed
                            accountNumber = null,
                            currency = "Kenyan Shillings",  // Placeholder
                            owner = null  // Placeholder
                        ),
                        amount = amount,
                        recipient = Recepient(name = recepient_name, phone_no = recepient_phone_no),
                        transactionType = transaction_type,
                        mpesaBalance = mpesa_balance,
                        transactionCost = transaction_cost,
                        mpesaDepositor = mpesa_depositor,
                        smsText = sms_text,
                        paybillAcount = paybill_account,
                        description = description,
                        sender = Sender(sender_name, sender_phone_no),
                        isDeleted = is_deleted == 1,
                        transactorCheck = transactorCheck == 1,
                        isConvertedToPettyCash = isConvertedToPettyCash == 1
                    )

                    transactions.add(transaction)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching non-converted Mpesa transactions", e)
        } finally {
            cursor?.close()  // Ensure cursor is closed to prevent memory leaks
        }

        return transactions
    }

    fun updateMpesaTransactionListAsConverted(transactions: MutableList<MpesaTransaction>) {
        val db = writableDatabase
        db.beginTransaction() // Begin transaction for batch update

        try {
            val query = "UPDATE $TABLE_TRANSACTIONS SET $COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH = 1 WHERE $COL_TRANSACTIONS_ID = ?"
            val statement = db.compileStatement(query)

            transactions.forEach { transaction ->
                statement.clearBindings() // Clear previous bindings to avoid conflicts
                transaction.id?.let { statement.bindLong(1, it.toLong()) } // Bind transaction ID
                statement.executeUpdateDelete() // Execute the update
            }

            db.setTransactionSuccessful() // Mark transaction as successful
        } catch (e: Exception) {
            Log.e("DbHelper", "Error updating transactions to converted", e)
        } finally {
            db.endTransaction() // End the transaction
        }
    }

    fun updateMpesaTransactionAsConverted(transaction: MpesaTransaction) {
        val db = writableDatabase
        db.beginTransaction() // Begin transaction for atomic update

        try {
            // Ensure transaction ID is not null
            val transactionCode = transaction.mpesa_code ?: throw IllegalArgumentException("Transaction ID cannot be null")

            val query = "UPDATE $TABLE_TRANSACTIONS " +
                    "SET $COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH = 1 " +
                    "WHERE $COL_TRANSACTIONS_MPESA_CODE = '$transactionCode'"

            db.execSQL(query) // Execute the update directly
            Log.d("DbHelper", "Converted transaction with ID: $transactionCode")
            db.setTransactionSuccessful() // Mark transaction as successful
        } catch (e: Exception) {
            Log.e("DbHelper", "Error updating transaction to converted", e)
        } finally {
            db.endTransaction() // End the transaction
        }
    }



    fun getMpesaTransactionByCode(mpesaCode: String): MpesaTransaction? {
        val db = readableDatabase
        var transaction: MpesaTransaction? = null
        var cursor: Cursor? = null

        // SQL query to fetch the Mpesa transaction by code
        val query = "SELECT * FROM $TABLE_TRANSACTIONS WHERE $COL_TRANSACTIONS_MPESA_CODE = ?"

        try {
            cursor = db.rawQuery(query, arrayOf(mpesaCode))

            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_ID))
                val mpesa_code = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_CODE))
                val recepient_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NAME))
                val msg_date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MESSAGE_DATE))
                val transaction_date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_DATE))
                val recepient_phone_no = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NO))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_AMOUNT))
                val account_id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_ACCOUNT_NO))
                val company_owner_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_COMPANY_OWNER))
                val transaction_type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_TYPE))
                val user_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_USER))
                val mpesa_balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_BALANCE))
                val transaction_cost = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_COST))
                val mpesa_depositor = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_DEPOSITOR))
                val sms_text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SMS_TEXT))
                val paybill_account = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_PAYBILL_ACCOUNT))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_DESCRIPTION))
                val sender_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_NAME))
                val sender_phone_no = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_PHONE_NO))
                val is_deleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_DELETED))
                val transactorCheck = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTOR_CHECK))
                val isConvertedToPettyCash = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH))

                transaction = MpesaTransaction(
                    id = id,
                    mpesaCode = mpesa_code,
                    msgDate = msg_date,
                    transactionDate = transaction_date,
                    account = Account(
                        id = account_id,
                        name = "General Expenses",  // Placeholder, you might want to fetch real account details
                        type = "Expense",  // Placeholder, modify as needed
                        accountNumber = null,
                        currency = "Kenyan Shillings",  // Placeholder
                        owner = null  // Placeholder, modify as needed
                    ),
                    amount = amount,
                    recipient = Recepient(name = recepient_name, phone_no = recepient_phone_no),
                    transactionType = transaction_type,
                    mpesaBalance = mpesa_balance,
                    transactionCost = transaction_cost,
                    mpesaDepositor = mpesa_depositor,
                    smsText = sms_text,
                    paybillAcount = paybill_account,
                    description = description,
                    sender = Sender(sender_name, sender_phone_no),
                    isDeleted = is_deleted == 1,
                    transactorCheck = transactorCheck == 1,
                    isConvertedToPettyCash = isConvertedToPettyCash == 1
                )
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching Mpesa transaction", e)
        } finally {
            cursor?.close()  // Ensure cursor is closed to prevent memory leaks
            //db.close()  // Optionally close the database if you don't need it open anymore
        }

        return transaction
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

        try {
            val cursor = db.query(
                TABLE_TRUCKS,
                arrayOf(
                    COL_ID,
                    COL_TRUCK_NO,
                    COL_TRUCK_MAKE,
                    COL_TRUCK_OWNER,
                    COL_TRUCK_ACTIVE_STATUS,
                    COL_IS_DELETED
                ),
                "$COL_IS_DELETED = ?" ,
                arrayOf("0"),
                null, null,
                COL_TRUCK_NO
            )

            cursor.use {
                while (it.moveToNext()) {
                    val truckId = it.getInt(it.getColumnIndexOrThrow(COL_ID))
                    val truckNo = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_NO))
                    val truckMake = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_MAKE))
                    val truckOwnerCode = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_OWNER))
                    val truckActiveStatus = it.getInt(it.getColumnIndexOrThrow(COL_TRUCK_ACTIVE_STATUS)) == 1

                    // Ensure getOwnerByCode is only called if truckOwnerCode is not null
                    val owner = if (truckOwnerCode != null) {
                        getOwnerByCode(truckOwnerCode) ?: Owner(1, "Abdulcon Enterprises Limited", "abdulcon")
                    } else {
                        Owner(1, "Abdulcon Enterprises Limited", "abdulcon")
                    }

                    trucks.add(Truck(truckId, truckNo, truckMake, owner, truckActiveStatus, false))
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching local trucks", e)
        } finally {
            db.close()
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

    fun getLocalTrucksByOwner(owner: Owner): List<Truck> {
        val trucks = mutableListOf<Truck>()
        val db = this.readableDatabase

        // Query trucks by owner code, ensure they are not deleted
        val cursor = db.query(
            TABLE_TRUCKS,
            arrayOf(
                COL_ID,
                COL_TRUCK_NO,
                COL_TRUCK_MAKE,
                COL_TRUCK_OWNER,
                COL_TRUCK_ACTIVE_STATUS,
                COL_IS_DELETED
            ),
            "$COL_TRUCK_OWNER = ? AND $COL_IS_DELETED = 0 AND $COL_TRUCK_ACTIVE_STATUS = 1" , // Filter by owner code and is_deleted = 0
            arrayOf(owner.ownerCode), // Get trucks matching ownerCode
            null,
            null,
            COL_TRUCK_NO // Order by truck number
        )

        cursor.use {
            while (it.moveToNext()) {
                val truckId = it.getInt(it.getColumnIndexOrThrow(COL_ID))
                val truckNo = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_NO))
                val truckMake = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_MAKE))
                val truckActiveStatusInteger = it.getInt(it.getColumnIndexOrThrow(COL_TRUCK_ACTIVE_STATUS))
                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_DELETED)) == 1

                val truckActiveStatus = truckActiveStatusInteger == 1

                // Create and add the truck to the list
                trucks.add(Truck(truckId, truckNo, truckMake, owner, truckActiveStatus, isDeleted))
            }
        }

        println("Trucks size: " + trucks.size)

        return trucks
    }


    fun getTruckByTruckNumber(truckNo: String): Truck? {
        val db = this.readableDatabase // Get a readable instance of the database
        var truck: Truck? = null

        try {
            // Query the truck by truck number and ensure it's not deleted
            val cursor = db.query(
                TABLE_TRUCKS,
                arrayOf(
                    COL_ID,
                    COL_TRUCK_NO,
                    COL_TRUCK_MAKE,
                    COL_TRUCK_OWNER,
                    COL_TRUCK_ACTIVE_STATUS,
                    COL_IS_DELETED
                ),
                "$COL_TRUCK_NO = ?", // Filter by truck number and ensure it's not deleted
                arrayOf(truckNo), // Pass the truck number as the query parameter
                null, null, null // No grouping, having, or ordering needed
            )

            // Use the cursor to retrieve the truck information
            cursor.use {
                if (it.moveToFirst()) {
                    val truckId = it.getInt(it.getColumnIndexOrThrow(COL_ID))
                    val truckNumber = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_NO))
                    val truckMake = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_MAKE))
                    val truckOwnerCode = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_OWNER))
                    val truckActiveStatus = it.getInt(it.getColumnIndexOrThrow(COL_TRUCK_ACTIVE_STATUS)) == 1
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_DELETED)) == 1

                    // Assuming you have a function to get the owner by their code
                    val owner = getOwnerByCode(truckOwnerCode)

                    // Create the Truck object
                    if (!isDeleted) {
                        truck = Truck(truckId, truckNumber, truckMake, owner, truckActiveStatus, isDeleted)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching Truck by Truck Number", e)
        }

        return truck // Return the truck or null if not found
    }

    fun getTruckCountByOwner(ownerCode: String): Int {
        val db = this.readableDatabase // Get a readable instance of the database
        var truckCount = 0

        try {
            // Define the query to count trucks by owner and ensure they are not deleted
            val query = """
            SELECT COUNT(*)
            FROM $TABLE_TRUCKS
            WHERE $COL_TRUCK_OWNER = ? AND $COL_IS_DELETED = 0
        """

            // Execute the query with the ownerCode as a parameter
            val cursor = db.rawQuery(query, arrayOf(ownerCode))

            // Use the cursor to retrieve the count
            cursor.use {
                if (it.moveToFirst()) {
                    truckCount = it.getInt(0) // The first column in the result contains the count
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error counting trucks by owner", e)
        }

        return truckCount // Return the count
    }







    fun getOwnerByCode(ownerCode: String): Owner? {
        val db = this.readableDatabase // Open the database connection
        val selection = "$COL_OWNER_CODE = ?" // Exclude deleted owners
        val trimmedOwnerCode = ownerCode.trim() // Trim whitespace from the ownerCode
        val selectionArgs = arrayOf(trimmedOwnerCode) // Ensure we only retrieve non-deleted owners
        var owner: Owner? = null

        // Log the query for debugging
        Log.d("getOwnerByCode", "Searching for owner with code: $trimmedOwnerCode")

        // Use try-with-resources to avoid resource leaks and auto-close the cursor
        db.query(TABLE_OWNERS, null, selection, selectionArgs, null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                // Retrieve values from cursor
                val ownerId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_NAME))
                val logoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)) // Retrieve logo path
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DELETED)) == 1 // Retrieve is_deleted flag

                // Create and assign the Owner object if we found a matching record
                owner = Owner(ownerId, ownerName, trimmedOwnerCode, logoPath, isDeleted)
                Log.d("getOwnerByCode", "Owner found: $owner")
            } else {
                Log.d("getOwnerByCode", "No owner found for code: $trimmedOwnerCode")
            }
        }
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
        val cursor = db.rawQuery("SELECT * FROM $TABLE_OWNERS WHERE $COL_IS_OWNER_DELETED = 0", null)

        cursor.use {
            while (it.moveToNext()) {
                val ownerId = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = it.getString(it.getColumnIndexOrThrow(COL_OWNER_NAME))
                val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE))
                val logoPath = it.getString(it.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH))
                val ownerIsDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_OWNER_DELETED)) == 1
                owners.add(Owner(ownerId, ownerName, ownerCode, logoPath))
            }
        }

        db.close()
        println("Owners retrieved: ${owners.size}")
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
        val db = this.readableDatabase
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
        val rowsAffected = db.update(TABLE_OWNERS, values, whereClause, whereArgs)

        println("rows affected $rowsAffected")

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
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRANSACTORS ORDER BY $COL_TRANSACTOR_NAME ASC", null)

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
                val kraPinString = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_KRA_PIN))
                val interactions = cursor.getInt(cursor.getColumnIndexOrThrow(
                    COL_TRANSACTOR_INTERACTIONS))
                val avatarColor = cursor.getString(cursor.getColumnIndexOrThrow(
                    COL_TRANSACTOR_AVATAR_COLOR))


                val transactor = Transactor(id, name.trim().replace("  ", " "), phoneNo, idCardNo, address, transactorType, transactorProfilePicturePath = logoPath, interactions, kraPinString, isDeleted, isImported, avatarColor )
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
                put(COL_TRANSACTOR_KRA_PIN, transactor.kraPin)
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
            put(COL_TRANSACTOR_KRA_PIN, transactor.kraPin)
            put(COL_IS_TRANSACTOR_DELETED, transactor.isDeleted)
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

    fun getTransactorByName(name: String): List<Transactor> {
        val transactors = mutableListOf<Transactor>()
        val db = this.readableDatabase

        // Use a query with a WHERE clause to filter by the transactor name
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_TRANSACTORS WHERE TRIM($COL_TRANSACTOR_NAME) LIKE ? COLLATE NOCASE",
            arrayOf("%$name%")
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID))
                    val transactorName = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_NAME))
                    val phoneNo = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_PHONE_NO))
                    val address = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_ADDRESS))
                    val transactorType = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_TYPE))
                    val idCardNo = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID_CARD_NO))
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_TRANSACTOR_DELETED)) > 0
                    val isImported = it.getInt(it.getColumnIndexOrThrow(COL_IS_IMPORTED)) > 0
                    val logoPath = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_LOGO_PATH))
                    val kraPinString = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_KRA_PIN))
                    val interactions = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_INTERACTIONS))
                    val avatarColor = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_AVATAR_COLOR))

                    // Create a Transactor object and add it to the list
                    val transactor = Transactor(
                        id, transactorName, phoneNo, idCardNo, address, transactorType, transactorProfilePicturePath = logoPath,
                        interactions, kraPinString, isDeleted, isImported, avatarColor
                    )
                    transactors.add(transactor)
                } while (it.moveToNext())
            }
        }

        return transactors
    }

    fun getSingleTransactorByName(name: String): Transactor? {
        val db = this.readableDatabase
        var transactor: Transactor? = null

        // Query with a WHERE clause to filter by the exact transactor name
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_TRANSACTORS WHERE TRIM($COL_TRANSACTOR_NAME) = ? COLLATE NOCASE",
            arrayOf(name.trim())
        )

        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID))
                val transactorName = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_NAME))
                val phoneNo = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_PHONE_NO))
                val address = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_ADDRESS))
                val transactorType = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_TYPE))
                val idCardNo = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID_CARD_NO))
                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_TRANSACTOR_DELETED)) > 0
                val isImported = it.getInt(it.getColumnIndexOrThrow(COL_IS_IMPORTED)) > 0
                val logoPath = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_LOGO_PATH))
                val kraPinString = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_KRA_PIN))
                val interactions = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_INTERACTIONS))
                val avatarColor = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_AVATAR_COLOR))

                // Create and return a Transactor object
                transactor = Transactor(
                    id, transactorName, phoneNo, idCardNo, address,
                    transactorType, transactorProfilePicturePath = logoPath,
                    interactions, kraPinString, isDeleted, isImported, avatarColor
                )
            }
        }

        return transactor
    }



    fun getTransactorById(transactorId: Int): Transactor? {
        // Ensure the database is open
        val db = this.readableDatabase
        var transactor: Transactor? = null
        var cursor: Cursor? = null

        try {
            // Query to select the transactor by ID
            val query = "SELECT * FROM $TABLE_TRANSACTORS WHERE $COL_TRANSACTOR_ID = ?"
            cursor = db.rawQuery(query, arrayOf(transactorId.toString()))

            // Check if cursor is valid and move to the first result
            if (cursor.moveToFirst()) {
                // Extract fields from the cursor
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_ID))
                val transactorName = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_NAME))
                val phoneNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_PHONE_NO))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_ADDRESS))
                val transactorType = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_TYPE))
                val idCardNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_ID_CARD_NO))
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_TRANSACTOR_DELETED)) > 0
                val isImported = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_IMPORTED)) > 0
                val logoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_LOGO_PATH))
                val kraPinString = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_KRA_PIN))
                val interactions = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_INTERACTIONS))
                val avatarColor = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTOR_AVATAR_COLOR))

                // Create a Transactor object
                transactor = Transactor(
                    id = id,
                    name = transactorName,
                    phoneNumber = phoneNo,
                    idCard = idCardNo,
                    address = address,
                    transactorType = transactorType,
                    transactorProfilePicturePath = logoPath,
                    interactions = interactions,
                    kraPin = kraPinString,
                    isDeleted = isDeleted,
                    isImported = isImported,
                    avatarColor = avatarColor
                )
            } else {
                // Optionally log or handle the case where the transactor was not found
                Log.e("getTransactorById", "Transactor with ID $transactorId not found.")
            }
        } catch (e: Exception) {
            Log.e("getTransactorById", "Error retrieving transactor: ${e.message}")
        } finally {
            cursor?.close() // Close the cursor in the finally block
        }

        return transactor
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

        // Close the database connectiondb.close()

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

    fun getTransactionCostAccountByOwner(ownerCode: String?): Account? {
        val db = this.readableDatabase
        val sql = "SELECT * FROM $TABLE_ACCOUNTS WHERE $COL_ACCOUNT_NAME = ? AND $COL_ACCOUNT_OWNER = ?"
        val cursor: Cursor?
        var account: Account? = null

        try {
            cursor = db.rawQuery(sql, arrayOf("M-Pesa Transaction Costs", ownerCode))

            cursor.use {
                if (it.moveToFirst()) {
                    // Extract data from the cursor
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                    val accountNumber = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER))
                    val type = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_TYPE))
                    val currency = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED)) == 1

                    // Create the owner from the ownerCode
                    val owner = getOwnerByCode(ownerCode.toString()) ?: Owner(1, "Default Owner", "default")

                    // Instantiate the Account object
                    account = Account(id, name, owner, type, currency, accountNumber, isDeleted)
                }
            }
        } catch (e: Exception) {
            Log.e("getTransactionCostAccountByOwner", "Error retrieving account", e)
        } finally {
            db.close()
        }

        return account
    }



    fun getAllAccounts(): List<Account> {
        val db = this.readableDatabase
        val accounts = mutableListOf<Account>()
        val sql = "SELECT * FROM $TABLE_ACCOUNTS"
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(sql, null)

            // Use the cursor in a use block to ensure it gets closed
            cursor.use {
                if (it.count > 0) { // Check if cursor has records
                    while (it.moveToNext()) {
                        // Extract data from each column
                        val id = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_ID))
                        val name = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                        val accountNumber = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER))
                        val type = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_TYPE))
                        val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_OWNER))
                        val currency = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))
                        val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED)) == 1

                        // Check if ownerCode is not null or empty
                        val owner = if (ownerCode.isNotBlank()) {
                            getOwnerByCode(ownerCode) ?: Owner(1, "Default Owner", "default")
                        } else {
                            Owner(1, "Default Owner", "default")
                        }

                        // Add new Account object to the list
                        accounts.add(Account(id, name, owner, type, currency, accountNumber, isDeleted))
                    }
                } else {
                    Log.d("getAllAccounts", "No accounts found in the table.")
                }
            }
        } catch (e: Exception) {
            Log.e("getAllAccounts", "Error retrieving accounts", e)
        } finally {
            cursor?.close() // Ensure the cursor is closed
            db.close() // Ensure the database is closed
        }

        return accounts
    }



    fun getOwnerByName(ownerName: String): Owner? {
        val db = this.readableDatabase
        val selection = "$COL_OWNER_NAME = ?" // Exclude deleted owners
        val selectionArgs = arrayOf(ownerName)
        var owner: Owner? = null

        val cursor = db.query(TABLE_OWNERS, null, selection, selectionArgs, null, null, null)

        cursor.use { // Use `use` to auto-close the cursor after the block
            if (it.moveToFirst()) {
                val ownerId = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE))
                val logoPath = it.getString(it.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)) // Retrieve logo path
                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_DELETED)) == 1 // Retrieve is_deleted flag

                owner = Owner(ownerId, ownerName, ownerCode, logoPath, isDeleted)
            }
        }

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


        return accountsList
    }

    // Add this method to get accounts by owner
    fun getAccountsByOwner(owner: Owner): List<Account> {
        val accounts = mutableListOf<Account>()
        val db = this.readableDatabase

        // Use a more efficient query with JOIN to avoid additional database calls
        val query = """
            SELECT a.* 
            FROM $TABLE_ACCOUNTS a
            WHERE a.$COL_ACCOUNT_OWNER = ? 
              AND a.$COL_ACCOUNT_IS_DELETED = 0
            ORDER BY a.$COL_ACCOUNT_NAME
        """

        try {
            val cursor = db.rawQuery(query, arrayOf(owner.ownerCode))

            if (cursor.moveToFirst()) {
                do {
                    val accountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACCOUNT_ID))
                    val accountName = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                    val accountNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER))
                    val accountType = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_TYPE))
                    val accountCurrency = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))

                    // Create an Account object and add it to the list
                    // We already have the owner object, so no need to fetch it again
                    val account = Account(
                        id = accountId,
                        name = accountName,
                        accountNumber = accountNumber,
                        type = accountType,
                        owner = owner,  // Use the owner that was passed in
                        currency = accountCurrency,
                        isDeleted = false  // We're filtering for non-deleted accounts in the query
                    )

                    accounts.add(account)
                } while (cursor.moveToNext())
            }

            cursor.close()
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting accounts by owner: ${e.message}")
        }

        return accounts
    }

    fun getAccountById(accountId: Int): Account? {
        val db = this.readableDatabase
        var account: Account? = null
        var cursor: Cursor? = null

        // Define a parameterized query to prevent SQL injection
        val selection = "$COL_ACCOUNT_ID = ?"
        val selectionArgs = arrayOf(accountId.toString())

        try {
            cursor = db.query(
                TABLE_ACCOUNTS,
                null, // Select all columns
                selection,
                selectionArgs,
                null, // Group by
                null, // Having
                null  // Order by
            )

            // Check if the cursor has any records
            if (cursor.moveToFirst()) {
                val accountName = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                val accountNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER))
                val accountType = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_TYPE))
                val accountOwnerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_OWNER))
                val accountCurrency = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))
                val accountIsDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED)) == 1

                // Retrieve the owner by their code if available
                val owner = accountOwnerCode?.let { getOwnerByCode(it) }

                // Create an Account object
                account = Account(
                    id = accountId,
                    name = accountName,
                    accountNumber = accountNumber,
                    type = accountType,
                    owner = owner,
                    currency = accountCurrency,
                    isDeleted = accountIsDeleted // Assuming 1 means deleted
                )
            }
        } finally {
            cursor?.close() // Close cursor after use to free resources
        }

        return account // Avoid closing db here to prevent connection pool issues
    }



    fun getAccountsByNameAndOwner(query: String, owner: Owner): MutableList<Account> {
        val accountsList = mutableListOf<Account>()
        val db = this.readableDatabase

        // Use a parameterized query to prevent SQL injection
        val selection = "$COL_ACCOUNT_NAME LIKE ? AND $COL_ACCOUNT_IS_DELETED = ? AND $COL_ACCOUNT_OWNER LIKE ?"
        val selectionArgs = arrayOf("%$query%", "0", owner.ownerCode) // Assuming "0" means not deleted

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
                val accountCurrency = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY))
                val accountIsDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED))


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

        return accountsList
    }

    fun searchPettyCash(query: String, limit: Int = 100): List<PettyCash>? {
        val db = this.readableDatabase
        val pettyCashList = mutableListOf<PettyCash>()

        try {
            val sql = """
                SELECT pc.*, t.$COL_TRANSACTOR_NAME as transactor_name
                FROM $TABLE_PETTY_CASH pc
                LEFT JOIN $TABLE_TRANSACTORS t ON t.$COL_TRANSACTOR_ID = pc.$COL_PETTY_CASH_TRANSACTOR
                WHERE pc.$COL_PETTY_CASH_IS_DELETED = 0
                AND (
                    LOWER(COALESCE(t.$COL_TRANSACTOR_NAME, '')) LIKE LOWER(?)
                    OR ? = ''
                )
                ORDER BY 
                    CASE 
                        WHEN t.$COL_TRANSACTOR_NAME LIKE ? THEN 0
                        ELSE 1 
                    END,
                    pc.$COL_PETTY_CASH_ID DESC
                LIMIT $limit
            """.trimIndent()

            val searchPattern = "%$query%"
            val selectionArgs = arrayOf(searchPattern, query, searchPattern)

            // Debug logging
            Log.d("DbHelper", """
                Search Debug Info:
                Query: '$query'
                Pattern: '$searchPattern'
                SQL: $sql
            """.trimIndent())

            val cursor = db.rawQuery(sql, selectionArgs)
            Log.d("DbHelper", "Cursor count: ${cursor.count}")

            cursor.use { c ->
                while (c.moveToNext()) {
                    getPettyCashFromCursor(c)?.let { pettyCash -> 
                        pettyCashList.add(pettyCash)
                        Log.d("DbHelper", """
                            Row found:
                            ID: ${pettyCash.id}
                            Number: ${pettyCash.pettyCashNumber ?: "null"}
                            Transactor: ${pettyCash.transactor?.name ?: "null"}
                            Description: ${pettyCash.description ?: "null"}
                        """.trimIndent())
                    }
                }
            }

            Log.d("DbHelper", "Final results count: ${pettyCashList.size}")
            return pettyCashList
        } catch (e: Exception) {
            Log.e("DbHelper", "Error in searchPettyCash: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    fun getAllPettyCash(
        page: Int,
        pageSize: Int,
        sortBy: String = "Date",
        dateFilter: String = "Any Time",
        paymentModes: List<String> = emptyList(),
        customStartDate: String? = null,
        customEndDate: String? = null
    ): List<PettyCash>? {
        val pettyCashList = mutableListOf<PettyCash>()
        val db = this.readableDatabase
        val offset = (page - 1) * pageSize

        try {
            // Get current date in dd/MM/yyyy format
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val currentDate = today.format(formatter)
            
            // Calculate week dates
            val weekStart = today.with(DayOfWeek.MONDAY).format(formatter)
            val weekEnd = today.with(DayOfWeek.SUNDAY).format(formatter)

            // Debug log the dates and sample data
            Log.d("DbHelper", """
                Date Filter: $dateFilter
                Current Date: $currentDate
                Week Start: $weekStart
                Week End: $weekEnd
            """.trimIndent())

            // Sample query to check data format
            val sampleQuery = "SELECT $COL_PETTY_CASH_DATE FROM $TABLE_PETTY_CASH LIMIT 1"
            val sampleCursor = db.rawQuery(sampleQuery, null)
            if (sampleCursor.moveToFirst()) {
                val sampleDate = sampleCursor.getString(0)
                Log.d("DbHelper", "Sample date from DB: $sampleDate")
            }
            sampleCursor.close()

            // Build the date filter clause
            val dateFilterClause = when (dateFilter) {
                "Today" -> """
                    AND (
                        substr($COL_PETTY_CASH_DATE, 1, 10) = '$currentDate'
                    )
                """
                "This Week" -> """
                    AND (
                        date(
                            substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 1, 2)
                        ) >= date('${weekStart.substring(6, 10)}-${weekStart.substring(3, 5)}-${weekStart.substring(0, 2)}')
                        AND 
                        date(
                            substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 1, 2)
                        ) <= date('${weekEnd.substring(6, 10)}-${weekEnd.substring(3, 5)}-${weekEnd.substring(0, 2)}')
                    )
                """
                "This Month" -> """
                    AND strftime('%Y-%m', 
                        SUBSTR($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 1, 2)
                    ) = strftime('%Y-%m', 'now', 'localtime')
                """
                "Last Month" -> """
                    AND strftime('%Y-%m', 
                        SUBSTR($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 1, 2)
                    ) = strftime('%Y-%m', 'now', '-1 month', 'localtime')
                """
                "Last Six Months" -> """
                    AND date(
                        SUBSTR($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 1, 2)
                    ) >= date('now', '-6 months', 'localtime')
                    AND date(
                        SUBSTR($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        SUBSTR($COL_PETTY_CASH_DATE, 1, 2)
                    ) <= date('now', 'localtime')
                """
                "Custom Range" -> if (customStartDate != null && customEndDate != null) """
                    AND (
                        date(
                            substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 1, 2)
                        ) >= date('${customStartDate.substring(6, 10)}-${customStartDate.substring(3, 5)}-${customStartDate.substring(0, 2)}')
                        AND 
                        date(
                            substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                            substr($COL_PETTY_CASH_DATE, 1, 2)
                        ) <= date('${customEndDate.substring(6, 10)}-${customEndDate.substring(3, 5)}-${customEndDate.substring(0, 2)}')
                    )
                """ else ""
                else -> "" // "Any Time" case
            }

            // Add debug logging
            Log.d("DbHelper", """
                Custom Date Range:
                Start Date: $customStartDate
                End Date: $customEndDate
                Filter Clause: $dateFilterClause
            """.trimIndent())

            // Add debug logging for week dates
            Log.d("DbHelper", """
                Week Filter Details:
                Week Start: $weekStart (${weekStart.substring(6, 10)}-${weekStart.substring(3, 5)}-${weekStart.substring(0, 2)})
                Week End: $weekEnd (${weekEnd.substring(6, 10)}-${weekEnd.substring(3, 5)}-${weekEnd.substring(0, 2)})
                Filter Clause: $dateFilterClause
            """.trimIndent())

            // Build payment mode filter
            val paymentModeClause = if (paymentModes.isNotEmpty()) {
                "AND $COL_PETTY_CASH_PAYMENT_MODE IN (${paymentModes.joinToString { "'$it'" }})"
            } else ""

            // Build the order by clause
            val orderBy = when (sortBy) {
                "Amount" -> """
                    ORDER BY CAST($COL_PETTY_CASH_AMOUNT AS DECIMAL(10,2)) DESC
                """
                "Transactor" -> """
                    ORDER BY (
                        SELECT t.$COL_TRANSACTOR_NAME 
                        FROM $TABLE_TRANSACTORS t
                        WHERE t.$COL_TRANSACTOR_ID = filtered_petty_cash.$COL_PETTY_CASH_TRANSACTOR
                    ) COLLATE NOCASE ASC,
                    datetime(
                        substr(filtered_petty_cash.$COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        substr(filtered_petty_cash.$COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        substr(filtered_petty_cash.$COL_PETTY_CASH_DATE, 1, 2) || ' ' ||
                        COALESCE(substr(filtered_petty_cash.$COL_PETTY_CASH_DATE, 12), '00:00:00')
                    ) DESC
                """
                else -> """
                    ORDER BY 
                    datetime(
                        substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 1, 2) || ' ' ||
                        COALESCE(substr($COL_PETTY_CASH_DATE, 12), '00:00:00')
                    ) DESC
                """
            }

            // Build the query
            val sql = """
                WITH filtered_petty_cash AS (
                    SELECT * FROM $TABLE_PETTY_CASH 
                    WHERE $COL_PETTY_CASH_IS_DELETED = 0 
                    $paymentModeClause
                    $dateFilterClause
                )
                SELECT * FROM filtered_petty_cash 
                $orderBy
                LIMIT $pageSize OFFSET $offset
            """.trimIndent()

            // Log the query for debugging
            Log.d("DbHelper", """
                Query Details:
                Sort By: $sortBy
                Date Filter: $dateFilter
                Payment Modes: $paymentModes
                Custom Start: $customStartDate
                Custom End: $customEndDate
                Full Query: $sql
            """.trimIndent())

            // Execute query and process results
            val cursor = db.rawQuery(sql, null)
            
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                    val pettyCashNo = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                    val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                    val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                    val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                        val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                        val accountId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                    val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                        val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                        val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                    val trucks = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                    val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                    val supportingDocumentId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                    val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                        var ownerObject: Owner? = null
                        var truckList: List<Truck>? = null

                        // Fetch related data
                        if (ownerCode != null) {
                            ownerObject = getOwnerByCode(ownerCode)
                        }
                        val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                        val transactorObject = getTransactorById(transactorId)
                        val accountObject = getAccountById(accountId)
                    val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                        // Split trucks and get their objects
                        if (trucks != null) {
                            truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                        }

                        // Create PettyCash object
                    val pettyCash = PettyCash(
                        id = id,
                        pettyCashNumber = pettyCashNo,
                        amount = amount,
                        date = date,
                        description = description,
                        transactor = transactorObject,
                        account = accountObject,
                        paymentMode = paymentMode,
                        owner = ownerObject,
                        mpesaTransaction = mpesaTransactionObject,
                            trucks = truckList?.toMutableList(),
                        signature = signature,
                        supportingDocument = supportingDocument,
                            user = User(1, user, UserTypes(1, "admin")),
                        isDeleted = isDeleted
                    )
                    pettyCashList.add(pettyCash)
                } while (it.moveToNext())
            }
        }

        return pettyCashList
        } catch (e: Exception) {
            Log.e("DbHelper", "Error in getAllPettyCash: ${e.message}")
            return null
        } finally {
            db.close()
        }
    }

    private fun getPettyCashFromCursor(cursor: Cursor): PettyCash? {
        return try {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
            
            // Handle null petty cash number properly
            val pettyCashNo = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))) {
                null
            } else {
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
            }
            
            val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
            val transactorId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
            val accountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
            val paymentMode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
            
            // Handle other potentially null fields
            val ownerCode = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))) {
                null
            } else {
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
            }
            
            val mpesaTransactionCode = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))) {
                null
            } else {
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
            }
            
            val trucks = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
            val signature = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
            val supportingDocumentId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
            val user = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

            // Get related objects
            val ownerObject = if (!ownerCode.isNullOrEmpty()) getOwnerByCode(ownerCode) else null
            val mpesaTransactionObject = if (!mpesaTransactionCode.isNullOrEmpty()) getMpesaTransactionByCode(mpesaTransactionCode) else null
            val transactorObject = getTransactorById(transactorId)
            val accountObject = getAccountById(accountId)
            val supportingDocument = getSupportingDocumentById(supportingDocumentId)

            // Split trucks and get their objects
            val truckList = if (!trucks.isNullOrEmpty()) {
                trucks.split(", ").mapNotNull { getTruckByTruckNumber(it.trim()) }
            } else emptyList()

            Log.d("DbHelper", """
                Creating PettyCash object:
                ID: $id
                Number: ${pettyCashNo ?: "null"}
                Amount: $amount
                Transactor: ${transactorObject?.name ?: "null"}
            """.trimIndent())

            PettyCash(
                id = id,
                pettyCashNumber = pettyCashNo,  // This can be null
                amount = amount,
                date = date,
                description = description,
                transactor = transactorObject,
                account = accountObject,
                paymentMode = paymentMode,
                owner = ownerObject,
                mpesaTransaction = mpesaTransactionObject,
                trucks = truckList.toMutableList(),
                signature = signature,
                supportingDocument = supportingDocument,
                user = User(1, user ?: "Mbarak", UserTypes(1, "admin")),
                isDeleted = isDeleted
            )
        } catch (e: Exception) {
            Log.e("DbHelper", "Error reading petty cash from cursor: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun getAllPettyCashWithPagination(offset: Int, limit: Int): List<PettyCash> {
        val pettyCashList = mutableListOf<PettyCash>()

        val query = """
            SELECT * FROM ${TABLE_PETTY_CASH} 
            WHERE ${COL_PETTY_CASH_IS_DELETED} = 0 
            ORDER BY ${COL_PETTY_CASH_DATE} DESC 
            LIMIT $limit OFFSET $offset
        """
        
        val cursor = readableDatabase.rawQuery(query, null)
        
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                val pettyCashNo = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                val amount = c.getDouble(c.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                val date = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                val description = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                val transactorId = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                val accountId = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                val paymentMode = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                val ownerCode = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                val mpesaTransactionCode = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                val trucks = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val signature = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                val supportingDocumentId = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                val user = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                val isDeleted = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                // Fetch related data
                    val ownerObject = getOwnerByCode(ownerCode)
                    val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                    val transactorObject = getTransactorById(transactorId)
                    val accountObject = getAccountById(accountId)
                    val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                    // Split trucks and get their objects
                    val truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }

                    val pettyCash = PettyCash(
                        id = id,
                        pettyCashNumber = pettyCashNo,
                        amount = amount,
                        date = date,
                        description = description,
                        transactor = transactorObject,
                        account = accountObject,
                        paymentMode = paymentMode,
                        owner = ownerObject,
                        mpesaTransaction = mpesaTransactionObject,
                        trucks = truckList.toMutableList(),
                        signature = signature,
                        supportingDocument = supportingDocument,
                        user = User(1, "Mbarak", UserTypes(1, "admin")),
                        isDeleted = isDeleted
                    )
                    pettyCashList.add(pettyCash)
            }
        }

        return pettyCashList
    }

    fun getAllPettyCashForCurrentMonthWithPagination(limit: Int, offset: Int): List<PettyCash> {
        val db = this.readableDatabase
        val pettyCashList = mutableListOf<PettyCash>()

        val calendar = Calendar.getInstance()
        val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1) // Months are 0-indexed
        val currentYear = calendar.get(Calendar.YEAR).toString()

        // Modify the query to match the working query
        val sql = """
        SELECT * 
        FROM $TABLE_PETTY_CASH 
        WHERE (
            (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth') 
            OR 
            (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND LENGTH($COL_PETTY_CASH_DATE) = 10 AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth')
        ) AND $COL_PETTY_CASH_IS_DELETED = 0
        ORDER BY 
            datetime(
                SUBSTR($COL_PETTY_CASH_DATE, 7, 4) || '-' || 
                SUBSTR($COL_PETTY_CASH_DATE, 4, 2) || '-' || 
                SUBSTR($COL_PETTY_CASH_DATE, 1, 2) || ' ' || 
                SUBSTR($COL_PETTY_CASH_DATE, 12, 8)
            ) DESC
        LIMIT $limit OFFSET $offset
    """.trimIndent()


        val cursor: Cursor? = db.rawQuery(sql, null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                    val pettyCashNo = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                    val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                    val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                    val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                    val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                    val accountId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                    val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                    val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                    val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                    val trucks = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                    val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                    val supportingDocumentId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                    val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                    var ownerObject: Owner? = null
                    var truckList: List<Truck>? = null

                    // Fetch related data
                    if (ownerCode != null) {
                        ownerObject = getOwnerByCode(ownerCode)
                    }
                    val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                    val transactorObject = getTransactorById(transactorId)
                    val accountObject = getAccountById(accountId)
                    val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                    // Split trucks and get their objects
                    if (trucks != null) {
                        truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                    }

                    // Add PettyCash object to the list
                    val pettyCash = PettyCash(
                        id = id,
                        pettyCashNumber = pettyCashNo,
                        amount = amount,
                        date = date,
                        description = description,
                        transactor = transactorObject,
                        account = accountObject,
                        paymentMode = paymentMode,
                        owner = ownerObject,
                        mpesaTransaction = mpesaTransactionObject,
                        trucks = truckList?.toMutableList(),
                        signature = signature,
                        supportingDocument = supportingDocument,
                        user = User(1, "Mbarak", UserTypes(1, "admin")),
                        isDeleted = isDeleted
                    )
                    pettyCashList.add(pettyCash)
                } while (it.moveToNext())
            }
        }

        db.close() // Close the database after fetching all data
        return pettyCashList
    }

    fun getPettyCashByPettyCashNumber(pettyCashNumber: String): PettyCash? {
        val db = this.readableDatabase
        var pettyCash: PettyCash? = null

        val sql = """
        SELECT * FROM $TABLE_PETTY_CASH 
        WHERE $COL_PETTY_CASH_NUMBER = ?
    """.trimIndent()

        val cursor: Cursor? = db.rawQuery(sql, arrayOf(pettyCashNumber))

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                val accountId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                val trucks = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                val supportingDocumentId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                var ownerObject: Owner? = null
                var truckList: List<Truck>? = null

                // Fetch related data
                if (ownerCode != null) {
                    ownerObject = getOwnerByCode(ownerCode)
                }
                val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                val transactorObject = getTransactorById(transactorId)
                val accountObject = getAccountById(accountId)
                val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                // Split trucks and get their objects
                if (trucks != null) {
                    truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                }

                pettyCash = PettyCash(
                    id = id,
                    pettyCashNumber = pettyCashNumber,
                    amount = amount,
                    date = date,
                    description = description,
                    transactor = transactorObject,
                    account = accountObject,
                    paymentMode = paymentMode,
                    owner = ownerObject,
                    mpesaTransaction = mpesaTransactionObject,
                    trucks = truckList?.toMutableList(),
                    signature = signature,
                    supportingDocument = supportingDocument,
                    user = User(1, "Mbarak", UserTypes(1, "admin")),
                    isDeleted = isDeleted
                )
            }
        }

        db.close()
        return pettyCash
    }


    fun deletePettyCash(pettyCashId: Int) {
        var db: SQLiteDatabase? = null
        try {
            db = writableDatabase

            // Update is_deleted to 1 instead of actually deleting the record
            val values = ContentValues().apply {
                put(COL_PETTY_CASH_IS_DELETED, 1)
            }

            // Define the update condition
            val selection = "$COL_PETTY_CASH_ID = ?"
            val selectionArgs = arrayOf(pettyCashId.toString())

            // Perform the update
            val updatedRows = db.update(
                TABLE_PETTY_CASH,
                values,
                selection,
                selectionArgs
            )

            Log.d("DbHelper", "Marked petty cash as deleted: $pettyCashId, Rows updated: $updatedRows")

            // Get the petty cash to check for M-Pesa transaction
            val pettyCash = getPettyCashById(pettyCashId, db)

            // Handle M-Pesa transaction if exists - delete transaction cost
            pettyCash?.mpesaTransaction?.mpesa_code?.let { mpesaCode ->
                // First get the transaction cost petty cash
                val transactionCostPettyCash = getTransactionCostPettyCashByMpesaTransaction(mpesaCode)

                transactionCostPettyCash?.let { tcPettyCash ->
                    // Delete the transaction cost using its ID
                    val tcSelection = "$COL_PETTY_CASH_ID = ?"
                    val tcSelectionArgs = arrayOf(tcPettyCash.id.toString())

                    val transactionCostUpdated = db.update(
                        TABLE_PETTY_CASH,
                        values,
                        tcSelection,
                        tcSelectionArgs
                    )

                    Log.d("DbHelper", """
                    Transaction cost deletion:
                    - Transaction Cost ID: ${tcPettyCash.id}
                    - M-Pesa Code: $mpesaCode
                    - Description: ${tcPettyCash.description}
                    - Rows updated: $transactionCostUpdated
                """.trimIndent())
                }
            }

        } catch (e: Exception) {
            Log.e("DbHelper", "Error deleting petty cash: ${e.message}")
            throw e
        } finally {
            db?.close()
        }
    }


    fun getPettyCashById(id: Int, db: SQLiteDatabase? = null): PettyCash? {
        val shouldCloseDb = db == null
        val database = db ?: this.readableDatabase
        var pettyCash: PettyCash? = null

        val sql = """
        SELECT * FROM $TABLE_PETTY_CASH 
        WHERE $COL_PETTY_CASH_ID = ?
    """.trimIndent()

        val cursor: Cursor? = database.rawQuery(sql, arrayOf(id.toString()))

        try {
            if (cursor?.moveToFirst() == true) {
                val pettyCashNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                val transactorId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                val accountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                val paymentMode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                val ownerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                val mpesaTransactionCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                val trucks = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val signature = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                val supportingDocumentId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                val user = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                var ownerObject: Owner? = null
                var truckList: List<Truck>? = null

                // Fetch related data
                if (ownerCode != null) {
                    ownerObject = getOwnerByCode(ownerCode)
                }
                val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                val transactorObject = getTransactorById(transactorId)
                val accountObject = getAccountById(accountId)
                val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                // Split trucks and get their objects
                if (trucks != null) {
                    truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                }

                pettyCash = PettyCash(
                    id = id,
                    pettyCashNumber = pettyCashNo,
                    amount = amount,
                    date = date,
                    description = description,
                    transactor = transactorObject,
                    account = accountObject,
                    paymentMode = paymentMode,
                    owner = ownerObject,
                    mpesaTransaction = mpesaTransactionObject,
                    trucks = truckList?.toMutableList(),
                    signature = signature,
                    supportingDocument = supportingDocument,
                    user = User(1, "Mbarak", UserTypes(1, "admin")),
                    isDeleted = isDeleted
                )
            }
        } finally {
            cursor?.close()
            if (shouldCloseDb) {
                database.close()
            }
        }

        return pettyCash
    }




    fun getAllPettyCashForCurrentMonth(): List<PettyCash> {
        val db = this.readableDatabase
        val pettyCashList = mutableListOf<PettyCash>()

        val calendar = Calendar.getInstance()
        val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1) // Months are 0-indexed
        val currentYear = calendar.get(Calendar.YEAR).toString()

        // Construct SQL query using string concatenation
        val sql = """
            SELECT * FROM $TABLE_PETTY_CASH 
            WHERE (
                (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth') 
                OR 
                (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND LENGTH($COL_PETTY_CASH_DATE) = 10 AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth')
            ) 
            ORDER BY datetime(
                SUBSTR($COL_PETTY_CASH_DATE, 7, 4) || '-' || 
                SUBSTR($COL_PETTY_CASH_DATE, 4, 2) || '-' || 
                SUBSTR($COL_PETTY_CASH_DATE, 1, 2)
            ) DESC
        """.trimIndent()

        val cursor: Cursor? = db.rawQuery(sql, null)

        //Log.d("getAllPettyCashForCurrentMonthWithPagination", "Query: $sql")

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                    val pettyCashNo = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                    val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                    val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                    val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                    val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                    val accountId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                    val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                    val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                    val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                    val trucks = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                    val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                    val supportingDocumentId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                    val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                    val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                    var ownerObject: Owner? = null
                    var truckList: List<Truck>? = null

                    // Fetch related data
                    if (ownerCode != null){
                        ownerObject = getOwnerByCode(ownerCode)

                    }
                    val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                    val transactorObject = getTransactorById(transactorId)
                    val accountObject = getAccountById(accountId)
                    val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                    // Split trucks and get their objects

                    if (trucks != null) {
                        truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                    }


                    val pettyCash = PettyCash(
                        id = id,
                        pettyCashNumber = pettyCashNo,
                        amount = amount,
                        date = date,
                        description = description,
                        transactor = transactorObject,
                        account = accountObject,
                        paymentMode = paymentMode,
                        owner = ownerObject,
                        mpesaTransaction = mpesaTransactionObject,
                        trucks = truckList?.toMutableList(),
                        signature = signature,
                        supportingDocument = supportingDocument,
                        user = User(1, "Mbarak", UserTypes(1, "admin")),
                        isDeleted = isDeleted
                    )
                    pettyCashList.add(pettyCash)
                } while (it.moveToNext())
            }
        }

        db.close() // Close the database after fetching all data
        return pettyCashList
    }

    fun getCountPettyCashForCurrentMonth(): Int {
        val db = this.readableDatabase
        var count = 0

        val calendar = Calendar.getInstance()
        val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1) // Months are 0-indexed
        val currentYear = calendar.get(Calendar.YEAR).toString()

        // Construct SQL query to count the number of petty cash records for the current month
        val sql = """
        SELECT COUNT(*) FROM $TABLE_PETTY_CASH 
        WHERE (
            (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth') 
            OR 
            (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND LENGTH($COL_PETTY_CASH_DATE) = 10 AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth')
        )
    """.trimIndent()

        val cursor: Cursor? = db.rawQuery(sql, null)

        cursor?.use {
            if (it.moveToFirst()) {
                count = it.getInt(0) // Get the count from the result set
            }
        }

        db.close() // Close the database after fetching the count
        return count
    }

    fun getPettyCashCount(): Int {
        val db = this.readableDatabase
        var count = 0
        
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_PETTY_CASH WHERE $COL_PETTY_CASH_IS_DELETED = 0", 
                null
            )
            
            cursor.use {
                if (it.moveToFirst()) {
                    count = it.getInt(0)
                }
            }
            Log.d("DbHelper", "Total petty cash count: $count")
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting petty cash count: ${e.message}")
            e.printStackTrace()
        }
        
        return count
    }




    /*fun getAllPettyCash(): List<PettyCash> {
        val pettyCashList = mutableListOf<PettyCash>()
        val db = this.readableDatabase

        val query =  """
    SELECT pc.*, 
           ac.$COL_ACCOUNT_NAME, ac.$COL_ACCOUNT_NUMBER, ac.$COL_ACCOUNT_TYPE, ac.$COL_ACCOUNT_OWNER, ac.$COL_ACCOUNT_CURRENCY, ac.$COL_ACCOUNT_IS_DELETED,
           ow.$COL_OWNER_ID, ow.$COL_OWNER_CODE, ow.$COL_OWNER_NAME, ow.$COL_OWNER_LOGO_PATH, ow.$COL_IS_DELETED AS owner_is_deleted,
           mt.$COL_TRANSACTIONS_MPESA_CODE, mt.$COL_TRANSACTIONS_AMOUNT, mt.$COL_TRANSACTIONS_MESSAGE_DATE, mt.$COL_TRANSACTIONS_TRANSACTION_DATE, 
           mt.$COL_TRANSACTIONS_RECEPIENT_NAME, mt.$COL_TRANSACTIONS_RECEPIENT_NO, mt.$COL_TRANSACTIONS_SENDER_NAME, mt.$COL_TRANSACTIONS_SENDER_PHONE_NO,
           mt.$COL_TRANSACTIONS_TRANSACTION_TYPE, mt.$COL_TRANSACTIONS_MPESA_BALANCE, mt.$COL_TRANSACTIONS_TRANSACTION_COST, mt.$COL_TRANSACTIONS_ACCOUNT_NO, 
           mt.$COL_TRANSACTIONS_IS_DELETED, mt.$COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH, mt.$COL_TRANSACTIONS_SMS_TEXT,
           mt.$COL_TRANSACTIONS_MPESA_DEPOSITOR, mt.$COL_TRANSACTIONS_TRANSACTOR_CHECK, mt.$COL_TRANSACTIONS_PAYBILL_ACCOUNT, mt.$COL_TRANSACTIONS_DESCRIPTION,
           sd.$COL_SUPPORTING_DOCUMENT_TYPE, sd.$COL_SUPPORTING_DOCUMENT_DOCUMENT_NO, sd.$COL_SUPPORTING_DOCUMENT_CU_NUMBER, 
           sd.$COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME, sd.$COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT, sd.$COL_SUPPORTING_DOCUMENT_TAX_AMOUNT,
           sd.$COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT, sd.$COL_SUPPORTING_DOCUMENT_DATE, sd.$COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1, 
           sd.$COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2, sd.$COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3,
           tr.$COL_TRUCK_NO, tr.$COL_TRUCK_MAKE, tr.$COL_TRUCK_OWNER, tr.$COL_TRUCK_ACTIVE_STATUS, tr.$COL_ID, tr.$COL_IS_DELETED AS truck_is_deleted,
           t.$COL_TRANSACTOR_ID, t.$COL_TRANSACTOR_NAME, t.$COL_TRANSACTOR_PHONE_NO, t.$COL_TRANSACTOR_ADDRESS, t.$COL_TRANSACTOR_TYPE, 
           t.$COL_TRANSACTOR_ID_CARD_NO, t.$COL_IS_TRANSACTOR_DELETED, t.$COL_IS_IMPORTED, t.$COL_TRANSACTOR_LOGO_PATH, t.$COL_TRANSACTOR_KRA_PIN,
           t.$COL_TRANSACTOR_INTERACTIONS, t.$COL_TRANSACTOR_AVATAR_COLOR
    FROM $TABLE_PETTY_CASH AS pc
    LEFT JOIN $TABLE_ACCOUNTS AS ac ON pc.$COL_PETTY_CASH_ACCOUNT = ac.$COL_ACCOUNT_ID
    LEFT JOIN $TABLE_OWNERS AS ow ON pc.$COL_PETTY_CASH_OWNER = ow.$COL_OWNER_CODE
    LEFT JOIN $TABLE_TRANSACTIONS AS mt ON pc.$COL_PETTY_CASH_MPESA_TRANSACTION = mt.$COL_TRANSACTIONS_MPESA_CODE
    LEFT JOIN $TABLE_SUPPORTING_DOCUMENT AS sd ON pc.$COL_PETTY_CASH_SUPPORTING_DOCUMENT = sd.$COL_SUPPORTING_DOCUMENT_ID
    LEFT JOIN $TABLE_TRUCKS AS tr ON ',' || pc.$COL_PETTY_CASH_TRUCKS || ',' LIKE '%,' || tr.$COL_TRUCK_NO || ',%'
    LEFT JOIN $TABLE_TRANSACTORS AS t ON mt.$COL_TRANSACTIONS_TRANSACTOR_CHECK = t.$COL_TRANSACTOR_ID
    WHERE pc.$COL_PETTY_CASH_IS_DELETED = 0
    GROUP BY pc.$COL_PETTY_CASH_ID
"""


        val cursor = db.rawQuery(query, null)
        cursor.use {
            val columnCount = it.columnCount
            if (it.moveToFirst()) {
                do {
                    // Map PettyCash fields
                    val pettyCash = PettyCash(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID)),
                        pettyCashNumber = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT)),
                        date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION)),
                        paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE)),
                        isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1,
                        user = User(1, "Mbarak", UserTypes(1, "admin")), // Adjust user as needed
                        signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE)),
                        owner = Owner(
                            id = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                            name = it.getString(it.getColumnIndexOrThrow(COL_OWNER_NAME)),
                            ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE)),
                            logoPath = it.getString(it.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)),
                            isDeleted = it.getInt(it.getColumnIndexOrThrow("owner_is_deleted")) == 1
                        ),

                        // Map Account fields
                        account = Account(
                            id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT)),
                            name = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NAME)),
                            accountNumber = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NUMBER)),
                            type = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_TYPE)),
                            currency = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_CURRENCY)),
                            isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_ACCOUNT_IS_DELETED)) == 1,
                            owner = Owner(
                                id = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                                name = it.getString(it.getColumnIndexOrThrow(COL_OWNER_NAME)),
                                ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE)),
                                logoPath = it.getString(it.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)),
                                isDeleted = it.getInt(it.getColumnIndexOrThrow("owner_is_deleted")) == 1
                            )
                        ),

                        // Map MpesaTransaction fields
                        mpesaTransaction = MpesaTransaction(
                            id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION)),
                            mpesaCode = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_CODE)),
                            amount = it.getDouble(it.getColumnIndexOrThrow(COL_TRANSACTIONS_AMOUNT)),
                            msgDate = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MESSAGE_DATE)),
                            transactionDate = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_DATE)),
                            transactionType = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_TYPE)),
                            mpesaBalance = it.getDouble(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_BALANCE)),
                            transactionCost = it.getDouble(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTION_COST)),
                            mpesaDepositor = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_MPESA_DEPOSITOR)),
                            transactorCheck = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_TRANSACTOR_CHECK)) == 1,
                            recipient = Recepient(
                                name = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NAME)),
                                phone_no = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_RECEPIENT_NO))
                            ),
                            sender = Sender(
                                name = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_NAME)),
                                phone_no = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_SENDER_PHONE_NO))
                            ),
                            isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_DELETED)) == 1,
                            account = Account(
                                id = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_ACCOUNT_NO)),
                                name = "General Expenses", // Placeholder, replace as necessary
                                type = "Expense", // Placeholder, replace as necessary
                                accountNumber = null, // Placeholder, replace as necessary
                                currency = "Kenyan Shillings", // Placeholder, replace as necessary
                                owner = null // Placeholder, replace as necessary
                            ),
                            smsText = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_SMS_TEXT)),
                            isConvertedToPettyCash = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH)) == 1,
                            paybillAcount = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_PAYBILL_ACCOUNT)),
                            description = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTIONS_DESCRIPTION)),
                        ),

                        // Map SupportingDocument fields
                        supportingDocument = SupportingDocument(
                            id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT)),
                            type = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TYPE)),
                            documentNo = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_DOCUMENT_NO)),
                            cuNumber = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_CU_NUMBER)),
                            supplierName = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME)),
                            taxableTotalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT)),
                            taxAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TAX_AMOUNT)),
                            totalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT)),
                            documentDate = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_DATE)),
                            imagePath1 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1)),
                            imagePath2 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2)),
                            imagePath3 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3)),
                        ),

                        // Map Truck fields
                        trucks = mutableListOf<Truck>().apply {
                            do {
                                // Check if this truck's info is not null
                                if (!it.isNull(it.getColumnIndexOrThrow(COL_TRUCK_NO))) {
                                    add(
                                        Truck(
                                            id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                                            truckNo = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_NO)),
                                            make = it.getString(it.getColumnIndexOrThrow(COL_TRUCK_MAKE)),
                                            owner = Owner(
                                                id = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                                                name = it.getString(it.getColumnIndexOrThrow(COL_OWNER_NAME)),
                                                ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE)),
                                                logoPath = it.getString(it.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)),
                                                isDeleted = it.getInt(it.getColumnIndexOrThrow("owner_is_deleted")) == 1
                                            ),
                                            activeStatus = it.getInt(it.getColumnIndexOrThrow(COL_TRUCK_ACTIVE_STATUS)) == 1,
                                            isDeleted = it.getInt(it.getColumnIndexOrThrow("truck_is_deleted")) == 1
                                        )
                                    )
                                }
                            } while (it.moveToNext()) // Continue moving to the next row for trucks
                        },

                        transactor = Transactor(
                            id = if (it.getColumnIndex(COL_TRANSACTOR_ID) < columnCount) {
                                it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID))
                            } else {
                                -1 // or handle accordingly
                            },
                            name = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_NAME)),
                            phoneNumber = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_PHONE_NO)),
                            idCard = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID_CARD_NO)),
                            address = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_ADDRESS)),
                            transactorType = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_TYPE)),
                            transactorProfilePicturePath = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_LOGO_PATH)),
                            interactions = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_INTERACTIONS)),
                            kraPin = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_KRA_PIN)),
                            isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_TRANSACTOR_DELETED)) > 0,
                            isImported = it.getInt(it.getColumnIndexOrThrow(COL_IS_IMPORTED)) > 0,
                            avatarColor = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_AVATAR_COLOR))
                        )
                    )

                    pettyCashList.add(pettyCash)
                } while (cursor.moveToNext())
            }
        }
        return pettyCashList
    }*/




    fun insertPettyCash(pettyCash: PettyCash): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        // Prepare the ContentValues object with PettyCash data
        contentValues.put(COL_PETTY_CASH_NUMBER, pettyCash.pettyCashNumber)
        contentValues.put(COL_PETTY_CASH_AMOUNT, pettyCash.amount)
        contentValues.put(COL_PETTY_CASH_DATE, pettyCash.date)
        contentValues.put(COL_PETTY_CASH_DESCRIPTION, pettyCash.description)
        contentValues.put(COL_PETTY_CASH_TRANSACTOR, pettyCash.transactor?.id) // Assuming transactor is not null
        contentValues.put(COL_PETTY_CASH_ACCOUNT, pettyCash.account?.id) // Assuming account is not null
        contentValues.put(COL_PETTY_CASH_PAYMENT_MODE, pettyCash.paymentMode)
        contentValues.put(COL_PETTY_CASH_OWNER, pettyCash.owner?.ownerCode)
        contentValues.put(COL_PETTY_CASH_MPESA_TRANSACTION, pettyCash.mpesaTransaction?.mpesa_code)

        // Concatenate the truck numbers into a single string
        val truckNos = pettyCash.trucks?.joinToString(", ") { it.truckNo.toString() }
        contentValues.put(COL_PETTY_CASH_TRUCKS, truckNos)

        contentValues.put(COL_PETTY_CASH_SIGNATURE, pettyCash.signature)
        println("Support Document ID in Db: ${pettyCash.supportingDocument?.id}")
        contentValues.put(COL_PETTY_CASH_SUPPORTING_DOCUMENT, pettyCash.supportingDocument?.id)
        contentValues.put(COL_PETTY_CASH_USER, pettyCash.user?.name ?: "Mbarak") // Assuming user is an object with a name property
        contentValues.put(COL_PETTY_CASH_IS_DELETED, if (pettyCash.isDeleted == true) 1 else 0)

        // Insert the row into the petty_cash table
        val rowId = db.insert(TABLE_PETTY_CASH, null, contentValues)

        return rowId // Return the row ID of the newly inserted row, or -1 if an error occurred
    }

    fun insertPettyCashList(pettyCashList: List<PettyCash>): List<Long> {
        val db = this.writableDatabase
        val rowIds = mutableListOf<Long>()

        db.beginTransaction() // Start a database transaction
        try {
            for (pettyCash in pettyCashList) {
                // Call the insertPettyCash function to insert each PettyCash object
                val rowId = insertPettyCash(pettyCash)
                rowIds.add(rowId) // Store the rowId in the list
            }
            db.setTransactionSuccessful() // Mark the transaction as successful
        } catch (e: Exception) {
            e.printStackTrace() // Handle any exceptions during the insert
        } finally {
            db.endTransaction() // End the transaction
        }

        return rowIds // Return the list of row IDs
    }

    fun updatePettyCash(pettyCash: PettyCash): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        // Prepare the ContentValues object with updated PettyCash data
        contentValues.put(COL_PETTY_CASH_NUMBER, pettyCash.pettyCashNumber)
        contentValues.put(COL_PETTY_CASH_AMOUNT, pettyCash.amount)
        contentValues.put(COL_PETTY_CASH_DATE, pettyCash.date)
        contentValues.put(COL_PETTY_CASH_DESCRIPTION, pettyCash.description)
        contentValues.put(COL_PETTY_CASH_TRANSACTOR, pettyCash.transactor?.id) // Assuming transactor is not null
        contentValues.put(COL_PETTY_CASH_ACCOUNT, pettyCash.account?.id) // Assuming account is not null
        contentValues.put(COL_PETTY_CASH_PAYMENT_MODE, pettyCash.paymentMode)
        contentValues.put(COL_PETTY_CASH_OWNER, pettyCash.owner?.ownerCode)
        contentValues.put(COL_PETTY_CASH_MPESA_TRANSACTION, pettyCash.mpesaTransaction?.mpesa_code)

        // Concatenate the truck numbers into a single string
        val truckNos = pettyCash.trucks?.joinToString(", ") { it.truckNo.toString() }
        contentValues.put(COL_PETTY_CASH_TRUCKS, truckNos)

        contentValues.put(COL_PETTY_CASH_SIGNATURE, pettyCash.signature)
        contentValues.put(COL_PETTY_CASH_SUPPORTING_DOCUMENT, pettyCash.supportingDocument?.id)
        contentValues.put(COL_PETTY_CASH_USER, pettyCash.user?.name ?: "Mbarak") // Assuming user is an object with a name property
        contentValues.put(COL_PETTY_CASH_IS_DELETED, if (pettyCash.isDeleted == true) 1 else 0)

        // Update the row in the petty_cash table where the ID matches
        val selection = "$COL_PETTY_CASH_ID = ?"
        val selectionArgs = arrayOf(pettyCash.id.toString()) // Convert ID to String for selection



        // Perform the update and return the number of rows affected
        val rowsAffected = db.update(TABLE_PETTY_CASH, contentValues, selection, selectionArgs)

        Log.d("updatePettyCash", "Rows affected: $rowsAffected")

        return rowsAffected // Return the number of rows affected by the update
    }

    fun getLatestPettyCash(): PettyCash? {
        val db = this.readableDatabase

        // SQL query to get the latest added petty cash record
        val sql = "SELECT * FROM $TABLE_PETTY_CASH ORDER BY $COL_PETTY_CASH_ID DESC LIMIT 1"
        val cursor: Cursor? = db.rawQuery(sql, null)

        var latestPettyCash: PettyCash? = null

        cursor?.use {
            if (it.moveToFirst()) {
                // Extracting data from the cursor to create a PettyCash object
                val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                val pettyCashNumber = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                val accountId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                val trucksString = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                val supportingDocumentId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                // Create a PettyCash object
                latestPettyCash = PettyCash(
                    id = id,
                    pettyCashNumber = pettyCashNumber,
                    amount = amount,
                    date = date,
                    description = description,
                    transactor = getTransactorById(transactorId), // Fetching the Transactor object
                    account = getAccountById(accountId), // Fetching the Account object
                    paymentMode = paymentMode,
                    owner = getOwnerByCode(ownerCode), // Fetching the Owner object
                    mpesaTransaction = getMpesaTransactionByCode(mpesaTransactionCode) ?: null, // Fetching the MpesaTransaction object
                    trucks = trucksString?.split(", ")?.map { getTruckByTruckNumber(it) }?.filterNotNull() as MutableList<Truck>?, // Fetching Truck objects
                    signature = signature,
                    supportingDocument = getSupportingDocumentById(supportingDocumentId), // Fetching the SupportingDocument object
                    user = User(1, user, UserTypes(1, "admin")), // Assuming a User object
                    isDeleted = isDeleted
                )
            }
        }

        return latestPettyCash // Return the latest PettyCash object or null if not found
    }

    fun getLatestPettyCashByOwner(ownerCode: String): PettyCash? {
        val db = this.readableDatabase
        var latestPettyCash: PettyCash? = null

        // Query to get the latest petty cash by owner, ordered by ID or date
        val cursor = db.query(
            TABLE_PETTY_CASH,
            null, // Select all columns
            "$COL_PETTY_CASH_OWNER = ?",
            arrayOf(ownerCode),
            null, // Group by
            null, // Having
            "$COL_PETTY_CASH_ID DESC", // Order by ID or date descending
            "1" // Limit to 1 result
        )

        // Use the cursor to get the data
        if (cursor.moveToFirst()) {
            // Extract data to create a PettyCash object
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
            val pettyCashNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
            val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
            val transactor = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
            val account = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
            val paymentMode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
            val owner = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
            val mpesaTransactionCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
            val trucks = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                ?.split(", ")
                ?.mapNotNull { getTruckByTruckNumber(it) } // Filter out nulls
                ?: mutableListOf()
            val signature = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
            val supportingDocument = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
            val user = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

            val mpesaTransaction = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }

            latestPettyCash = PettyCash(
                id = id,
                amount = amount,
                pettyCashNumber = pettyCashNumber,
                date = date,
                description = description,
                transactor = getTransactorById(transactor), // Assuming you have a method to get a transactor by ID
                account = getAccountById(account), // Assuming you have a method to get an account by ID
                paymentMode = paymentMode,
                owner = getOwnerByCode(owner), // Assuming you have an Owner class
                mpesaTransaction = mpesaTransaction, // Assuming you have a method to get MpesaTransaction by code
                trucks = trucks.toMutableList(),
                signature = signature,
                supportingDocument = getSupportingDocumentById(supportingDocument), // Assuming you have a method to get a supporting document by ID
                user = User(1, user, UserTypes(1, "admin")), // Adjust this based on your User model
                isDeleted = isDeleted
            )
        }

        return latestPettyCash
    }

    fun getLatestPettyCashByOwnerAndPettyCashNumber(ownerCode: String): PettyCash? {
        val db = this.readableDatabase
        var latestPettyCash: PettyCash? = null

        // Get the current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Log the ownerCode and currentYear being used
        Log.d("PettyCashQuery", "ownerCode: $ownerCode, currentYear: $currentYear")

        // Using a raw SQL query with placeholder for ownerCode and filtering by the current year in petty cash number
        val query = """
        SELECT * FROM $TABLE_PETTY_CASH
        WHERE $COL_PETTY_CASH_OWNER = "$ownerCode"
        AND CAST(SUBSTR($COL_PETTY_CASH_NUMBER, -4) AS INTEGER) = $currentYear
        ORDER BY CAST(SUBSTR(
            $COL_PETTY_CASH_NUMBER,
            INSTR($COL_PETTY_CASH_NUMBER, '/') + 1,
            INSTR(SUBSTR($COL_PETTY_CASH_NUMBER, INSTR($COL_PETTY_CASH_NUMBER, '/') + 1), '/') - 1
        ) AS INTEGER) DESC
        LIMIT 1
    """

        // Log the query to verify correctness
        Log.d("PettyCashQuery", "Executed Query: $query")

        // Execute the query
        val cursor = db.rawQuery(query, null)

        // Use cursor to read the data
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                val pettyCashNumber = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                val transactor = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                val account = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                val owner = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                val trucks = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                    ?.split(", ")
                    ?.mapNotNull { getTruckByTruckNumber(it) }
                    ?: mutableListOf()
                val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                val supportingDocument = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                val mpesaTransaction = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }

                latestPettyCash = PettyCash(
                    id = id,
                    amount = amount,
                    pettyCashNumber = pettyCashNumber,
                    date = date,
                    description = description,
                    transactor = getTransactorById(transactor),
                    account = getAccountById(account),
                    paymentMode = paymentMode,
                    owner = getOwnerByCode(owner),
                    mpesaTransaction = mpesaTransaction,
                    trucks = trucks.toMutableList(),
                    signature = signature,
                    supportingDocument = getSupportingDocumentById(supportingDocument),
                    user = User(1, user, UserTypes(1, "admin")),
                    isDeleted = isDeleted
                )

                // Log the result to verify the returned PettyCash
                Log.d("PettyCashQuery", "Latest PettyCash: $latestPettyCash")
            }
        }

        return latestPettyCash
    }





    fun getDescriptionSuggestions(account: Account, transactor: Transactor): List<String> {
        val db = this.readableDatabase
        val suggestions = mutableListOf<String>()

        // Prepare your SQL query to get previous descriptions based on account and transactor
        val cursor = db.query(
            TABLE_PETTY_CASH, // Table name
            arrayOf(COL_PETTY_CASH_DESCRIPTION), // Selecting only the description column
            "$COL_PETTY_CASH_ACCOUNT = ? AND $COL_PETTY_CASH_TRANSACTOR = ?", // Where clause
            arrayOf(account.id.toString(), transactor.id.toString()), // Arguments for the where clause
            null, // Group By
            null, // Having
            null // Order By (you can order by date if you want)
        )

        // Loop through the cursor to get the descriptions
        while (cursor.moveToNext()) {
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
            suggestions.add(description)
        }

        return suggestions // Return the list of suggestions
    }


    fun getAllSupportingDocuments(): List<SupportingDocument> {
        val supportingDocumentsList = mutableListOf<SupportingDocument>()
        val db = this.readableDatabase

        // SQL query to select all records from the supporting_document table
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SUPPORTING_DOCUMENT", null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    // Extracting data from the cursor to create SupportingDocument objects
                    val id = it.getInt(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_ID))
                    val type = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TYPE))
                    val documentNo = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_DOCUMENT_NO))
                    val cuNumber = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_CU_NUMBER))
                    val supplierName = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME))
                    val taxableTotalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT))
                    val taxAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TAX_AMOUNT))
                    val totalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT))
                    val documentDate = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_DATE))
                    val imagePath1 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1))
                    val imagePath2 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2))
                    val imagePath3 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3))

                    // Create a SupportingDocument object and add it to the list
                    val supportingDocument = SupportingDocument(
                        id = id,
                        type = type,
                        documentNo = documentNo,
                        cuNumber = cuNumber,
                        supplierName = supplierName,
                        taxableTotalAmount = taxableTotalAmount,
                        taxAmount = taxAmount,
                        totalAmount = totalAmount,
                        documentDate = documentDate,
                        imagePath1 = imagePath1,
                        imagePath2 = imagePath2,
                        imagePath3 = imagePath3
                    )
                    supportingDocumentsList.add(supportingDocument)
                } while (it.moveToNext())
            }
        }


        return supportingDocumentsList
    }

    fun insertSupportingDocument(supportingDocument: SupportingDocument): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_SUPPORTING_DOCUMENT_ID, supportingDocument.id)
            put(COL_SUPPORTING_DOCUMENT_TYPE, supportingDocument.type)
            put(COL_SUPPORTING_DOCUMENT_DOCUMENT_NO, supportingDocument.documentNo)
            put(COL_SUPPORTING_DOCUMENT_CU_NUMBER, supportingDocument.cuNumber)
            put(COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME, supportingDocument.supplierName)
            put(COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT, supportingDocument.taxableTotalAmount)
            put(COL_SUPPORTING_DOCUMENT_TAX_AMOUNT, supportingDocument.taxAmount)
            put(COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT, supportingDocument.totalAmount)
            put(COL_SUPPORTING_DOCUMENT_DATE, supportingDocument.documentDate)
            put(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1, supportingDocument.imagePath1)
            put(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2, supportingDocument.imagePath2)
            put(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3, supportingDocument.imagePath3)
        }

        // Insert the row, returning the primary key value of the new row
        val result = db.insert(TABLE_SUPPORTING_DOCUMENT, null, contentValues)

        return result // Return the result (row ID of newly inserted record or -1 for failure)
    }

    fun insertSupportingDocuments(documents: List<SupportingDocument>): List<Long> {
        val insertedIds = mutableListOf<Long>() // To store the IDs of inserted records
        for (document in documents) {
            val result = insertSupportingDocument(document) // Call the single insert method
            insertedIds.add(result) // Add the result to the list
        }
        return insertedIds // Return the list of inserted IDs
    }

    fun updateSupportingDocument(supportingDocument: SupportingDocument): Int {
        val db = this.writableDatabase // Get a writable instance of the database
        val contentValues = ContentValues().apply {
            put(COL_SUPPORTING_DOCUMENT_TYPE, supportingDocument.type)
            put(COL_SUPPORTING_DOCUMENT_DOCUMENT_NO, supportingDocument.documentNo)
            put(COL_SUPPORTING_DOCUMENT_CU_NUMBER, supportingDocument.cuNumber)
            put(COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME, supportingDocument.supplierName)
            put(COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT, supportingDocument.taxableTotalAmount)
            put(COL_SUPPORTING_DOCUMENT_TAX_AMOUNT, supportingDocument.taxAmount)
            put(COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT, supportingDocument.totalAmount)
            put(COL_SUPPORTING_DOCUMENT_DATE, supportingDocument.documentDate)
            put(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1, supportingDocument.imagePath1)
            put(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2, supportingDocument.imagePath2)
            put(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3, supportingDocument.imagePath3)
        }

        // Define the WHERE clause and arguments to identify the record to update
        val selection = "$COL_SUPPORTING_DOCUMENT_ID = ?"
        val selectionArgs = arrayOf(supportingDocument.id.toString())

        // Execute the update and return the number of rows affected
        return db.update(
            TABLE_SUPPORTING_DOCUMENT,
            contentValues,
            selection,
            selectionArgs
        )
    }



    fun getSupportingDocumentById(id: Int): SupportingDocument? {
        val db = this.readableDatabase // Get a readable instance of the database
        var supportingDocument: SupportingDocument? = null

        // Define the selection criteria
        val selection = "$COL_SUPPORTING_DOCUMENT_ID = ?"
        val selectionArgs = arrayOf(id.toString()) // Convert the integer ID to a string

        // Use try-catch for better exception management
        try {
            // Query the database for the supporting document
            val cursor = db.query(
                TABLE_SUPPORTING_DOCUMENT,
                null, // Select all columns
                selection,
                selectionArgs,
                null, // Group by
                null, // Having
                null  // Order by
            )

            // Use the cursor to get the document if it exists
            cursor.use { // Automatically closes the cursor
                if (it.moveToFirst()) {
                    supportingDocument = SupportingDocument(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_ID)),
                        type = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TYPE)),
                        documentNo = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_DOCUMENT_NO)),
                        cuNumber = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_CU_NUMBER)),
                        supplierName = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME)),
                        taxableTotalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT)),
                        taxAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TAX_AMOUNT)),
                        totalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT)),
                        documentDate = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_DATE)),
                        imagePath1 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1)),
                        imagePath2 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2)),
                        imagePath3 = it.getString(it.getColumnIndexOrThrow(COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching Supporting Document", e)
        }

        return supportingDocument // Return the document or null if not found
    }


    fun getLatestSupportingDocumentId(): Int {
        val db = this.readableDatabase
        var latestId = 0

        // SQL query to select the maximum ID from the supporting_document table
        val cursor = db.rawQuery("SELECT MAX($COL_SUPPORTING_DOCUMENT_ID) AS max_id FROM $TABLE_SUPPORTING_DOCUMENT", null)

        cursor?.use {
            if (it.moveToFirst()) {
                latestId = it.getInt(it.getColumnIndexOrThrow("max_id"))
            }
        }

        return latestId
    }

    fun getTransactionCostPettyCashByMpesaTransaction(mpesaCode: String?): PettyCash? {
        if (mpesaCode.isNullOrEmpty()) return null

        val db = this.readableDatabase
        var pettyCash: PettyCash? = null

        // Query to find PettyCash where description contains the mpesaCode
        val cursor = db.query(
            TABLE_PETTY_CASH,
            null, // Select all columns
            "$COL_PETTY_CASH_DESCRIPTION LIKE ?",
            arrayOf("%$mpesaCode%"),
            null, // Group by
            null, // Having
            "$COL_PETTY_CASH_ID DESC", // Order by ID descending (optional)
            "1" // Limit to 1 result
        )

        // Use the cursor to get the data
        if (cursor.moveToFirst()) {
            // Extract data to create a PettyCash object (similar to your previous code)
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
            val pettyCashNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
            val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
            val transactor = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
            val accountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
            val paymentMode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
            val ownerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
            val mpesaTransactionCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
            val trucks = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                ?.split(", ")
                ?.mapNotNull { getTruckByTruckNumber(it) } ?: mutableListOf()
            val signature = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
            val supportingDocument = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
            val user = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

            var mpesaTransaction: MpesaTransaction? = null

            if (!mpesaTransactionCode.isNullOrEmpty()){
                mpesaTransaction = getMpesaTransactionByCode(mpesaTransactionCode)
            }

            var owner: Owner? = null

            if (!ownerCode.isNullOrEmpty()){
                owner = getOwnerByCode(ownerCode)
            }

            var account: Account? = null

            if (accountId != null){
                account = getAccountById(accountId)
            }


            pettyCash = PettyCash(
                id = id,
                amount = amount,
                pettyCashNumber = pettyCashNumber,
                date = date,
                description = description,
                transactor = getTransactorById(transactor),
                account = account,
                paymentMode = paymentMode,
                owner = owner,
                mpesaTransaction = mpesaTransaction,
                trucks = trucks.toMutableList(),
                signature = null,
                supportingDocument = null,
                user = User(1, user, UserTypes(1, "admin")), // Adjust this based on your User model
                isDeleted = isDeleted
            )
        }
        cursor.close()
        return pettyCash
    }

    // Methods for automation rules
    fun getAllAutomationRules(): List<AutomationRule> {
        // Ensure the automation rules table exists
        ensureAutomationRulesTableExists()
        
        val rulesList = mutableListOf<AutomationRule>()
        val db = this.readableDatabase
        
        try {
            val query = """
                SELECT ar.*, 
                       t.${COL_TRANSACTOR_NAME} as transactor_name,
                       a.${COL_ACCOUNT_NAME} as account_name,
                       o.${COL_OWNER_NAME} as owner_name,
                       tr.${COL_TRUCK_NO} as truck_name
                FROM $TABLE_AUTOMATION_RULES ar
                LEFT JOIN $TABLE_TRANSACTORS t ON ar.$KEY_AUTOMATION_RULE_TRANSACTOR_ID = t.$COL_TRANSACTOR_ID
                LEFT JOIN $TABLE_ACCOUNTS a ON ar.$KEY_AUTOMATION_RULE_ACCOUNT_ID = a.$COL_ACCOUNT_ID
                LEFT JOIN $TABLE_OWNERS o ON ar.$KEY_AUTOMATION_RULE_OWNER_ID = o.$COL_OWNER_ID
                LEFT JOIN $TABLE_TRUCKS tr ON ar.$KEY_AUTOMATION_RULE_TRUCK_ID = tr.$COL_ID
                ORDER BY ar.$KEY_AUTOMATION_RULE_UPDATED_AT DESC
            """
            
            val cursor = db.rawQuery(query, null)
            
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_NAME))
                    val transactorId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_TRANSACTOR_ID))
                    val accountId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_ACCOUNT_ID))
                    val ownerId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_OWNER_ID))
                    val truckId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_TRUCK_ID))
                    val descriptionPattern = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_DESCRIPTION_PATTERN))
                    val minAmount = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_MIN_AMOUNT))
                    val maxAmount = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_MAX_AMOUNT))
                    val createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_CREATED_AT))
                    val updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_UPDATED_AT))
                    
                    // Get related entity names from the JOIN
                    val transactorName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("transactor_name"))
                    val accountName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("account_name"))
                    val ownerName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("owner_name"))
                    val truckName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("truck_name"))
                    
                    val rule = AutomationRule(
                        id = id,
                        name = name,
                        transactorId = transactorId,
                        transactorName = transactorName,
                        accountId = accountId,
                        accountName = accountName,
                        ownerId = ownerId,
                        ownerName = ownerName,
                        truckId = truckId,
                        truckName = truckName,
                        descriptionPattern = descriptionPattern,
                        minAmount = minAmount,
                        maxAmount = maxAmount,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    
                    rulesList.add(rule)
                    Log.d("DbHelper", "Loaded rule: ${rule.name} (ID: ${rule.id})")
                } while (cursor.moveToNext())
            }
            
            cursor.close()
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting automation rules: ${e.message}", e)
        }
        
        Log.d("DbHelper", "Returning ${rulesList.size} automation rules")
        return rulesList
    }

    fun getAutomationRuleById(id: Int): AutomationRule? {
        val db = this.readableDatabase

        val query = """
            SELECT ar.*, 
                   t.name as transactor_name, 
                   a.name as account_name, 
                   o.name as owner_name, 
                   tr.truck_no as truck_name
            FROM $TABLE_AUTOMATION_RULES ar
            LEFT JOIN $TABLE_TRANSACTORS t ON ar.$KEY_AUTOMATION_RULE_TRANSACTOR_ID = t.$COL_TRANSACTOR_ID
            LEFT JOIN $TABLE_ACCOUNTS a ON ar.$KEY_AUTOMATION_RULE_ACCOUNT_ID = a.$COL_ACCOUNT_ID
            LEFT JOIN $TABLE_OWNERS o ON ar.$KEY_AUTOMATION_RULE_OWNER_ID = o.$COL_OWNER_ID
            LEFT JOIN $TABLE_TRUCKS tr ON ar.$KEY_AUTOMATION_RULE_TRUCK_ID = tr.$COL_ID
            WHERE ar.$KEY_AUTOMATION_RULE_ID = ?
        """

        var rule: AutomationRule? = null

        try {
            val cursor = db.rawQuery(query, arrayOf(id.toString()))

            if (cursor.moveToFirst()) {
                rule = AutomationRule(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_NAME)),
                    transactorId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_TRANSACTOR_ID)),
                    transactorName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("transactor_name")),
                    accountId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_ACCOUNT_ID)),
                    accountName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("account_name")),
                    ownerId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_OWNER_ID)),
                    ownerName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("owner_name")),
                    truckId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_TRUCK_ID)),
                    truckName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("truck_name")),
                    descriptionPattern = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_DESCRIPTION_PATTERN)),
                    minAmount = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_MIN_AMOUNT)),
                    maxAmount = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_MAX_AMOUNT)),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_CREATED_AT)),
                    updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_UPDATED_AT))
                )
            }

            cursor.close()
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting automation rule by ID: ${e.message}")
        }

        return rule
    }

    // Add this method to check and create the automation rules table if it doesn't exist
    private fun ensureAutomationRulesTableExists() {
        val db = this.writableDatabase
        
        // Check if the table exists
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='$TABLE_AUTOMATION_RULES'",
            null
        )
        
        val tableExists = cursor.count > 0
        cursor.close()
        
        if (!tableExists) {
            // Create the automation rules table
            val createTableSQL = """
                CREATE TABLE $TABLE_AUTOMATION_RULES (
                    $KEY_AUTOMATION_RULE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $KEY_AUTOMATION_RULE_NAME TEXT,
                    $KEY_AUTOMATION_RULE_TRANSACTOR_ID INTEGER,
                    $KEY_AUTOMATION_RULE_ACCOUNT_ID INTEGER,
                    $KEY_AUTOMATION_RULE_OWNER_ID INTEGER,
                    $KEY_AUTOMATION_RULE_TRUCK_ID INTEGER,
                    $KEY_AUTOMATION_RULE_DESCRIPTION_PATTERN TEXT,
                    $KEY_AUTOMATION_RULE_MIN_AMOUNT REAL,
                    $KEY_AUTOMATION_RULE_MAX_AMOUNT REAL,
                    $KEY_AUTOMATION_RULE_CREATED_AT TEXT,
                    $KEY_AUTOMATION_RULE_UPDATED_AT TEXT,
                    FOREIGN KEY ($KEY_AUTOMATION_RULE_TRANSACTOR_ID) REFERENCES $TABLE_TRANSACTORS($COL_TRANSACTOR_ID) ON DELETE SET NULL,
                    FOREIGN KEY ($KEY_AUTOMATION_RULE_ACCOUNT_ID) REFERENCES $TABLE_ACCOUNTS($COL_ACCOUNT_ID) ON DELETE SET NULL,
                    FOREIGN KEY ($KEY_AUTOMATION_RULE_OWNER_ID) REFERENCES $TABLE_OWNERS($COL_OWNER_ID) ON DELETE SET NULL,
                    FOREIGN KEY ($KEY_AUTOMATION_RULE_TRUCK_ID) REFERENCES $TABLE_TRUCKS($COL_ID) ON DELETE SET NULL
                )
            """
            
            try {
                db.execSQL(createTableSQL)
                Log.d("DbHelper", "Created automation rules table")
            } catch (e: Exception) {
                Log.e("DbHelper", "Error creating automation rules table: ${e.message}")
            }
        }
    }

    // Update the addAutomationRule method to ensure the table exists
    fun addAutomationRule(rule: AutomationRule): Long {
        // Ensure the automation rules table exists
        ensureAutomationRulesTableExists()
        
        val db = this.writableDatabase
        val values = ContentValues()
        
        values.put(KEY_AUTOMATION_RULE_NAME, rule.name)
        values.put(KEY_AUTOMATION_RULE_TRANSACTOR_ID, rule.transactorId)
        values.put(KEY_AUTOMATION_RULE_ACCOUNT_ID, rule.accountId)
        values.put(KEY_AUTOMATION_RULE_OWNER_ID, rule.ownerId)
        values.put(KEY_AUTOMATION_RULE_TRUCK_ID, rule.truckId)
        values.put(KEY_AUTOMATION_RULE_DESCRIPTION_PATTERN, rule.descriptionPattern)
        values.put(KEY_AUTOMATION_RULE_MIN_AMOUNT, rule.minAmount)
        values.put(KEY_AUTOMATION_RULE_MAX_AMOUNT, rule.maxAmount)
        values.put(KEY_AUTOMATION_RULE_CREATED_AT, rule.createdAt)
        values.put(KEY_AUTOMATION_RULE_UPDATED_AT, rule.updatedAt)
        
        return db.insert(TABLE_AUTOMATION_RULES, null, values)
    }

    // Update the updateAutomationRule method to ensure the table exists
    fun updateAutomationRule(rule: AutomationRule): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("name", rule.name)
            put("transactor_id", rule.transactorId)
            put("account_id", rule.accountId)
            put("owner_id", rule.ownerId)
            put("truck_id", rule.truckId)  // This will be -1 for "All Trucks"
            put("description_pattern", rule.descriptionPattern)
            put("min_amount", rule.minAmount)
            put("max_amount", rule.maxAmount)
            put("updated_at", rule.updatedAt)
        }

        // Begin transaction
        db.beginTransaction()
        try {
            // Update the main rule
            val success = db.update(
                TABLE_AUTOMATION_RULES,
                values,
                "$KEY_AUTOMATION_RULE_ID = ?",
                arrayOf(rule.id.toString())
            ) > 0

            db.setTransactionSuccessful()
            return success
        } catch (e: Exception) {
            Log.e("DbHelper", "Error updating automation rule: ${e.message}", e)
            return false
        } finally {
            db.endTransaction()
        }
    }

    // Update the deleteAutomationRule method to ensure the table exists
    fun deleteAutomationRule(id: Int): Boolean {
        // Ensure the automation rules table exists
        ensureAutomationRulesTableExists()
        
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_AUTOMATION_RULES,
            "$KEY_AUTOMATION_RULE_ID = ?",
            arrayOf(id.toString())
        )
        
        return result > 0
    }

    fun checkIsAutomated(pettyCash: PettyCash): Boolean {
        Log.d("DbHelper", "Checking automation for petty cash: amount=${pettyCash.amount}, transactor=${pettyCash.transactor?.name}")

        // Ensure the automation rules table exists
        ensureAutomationRulesTableExists()

        val amount = pettyCash.amount ?: return false
        val transactorId = pettyCash.transactor?.id
        
        // Log detailed information about the petty cash and transactor
        Log.d("DbHelper", "Finding automation rule for: amount=$amount")
        Log.d("DbHelper", "Transactor details: name=${pettyCash.transactor?.name}, id=${pettyCash.transactor?.id}")
        
        // First check if there are any automation rules at all
        val countQuery = "SELECT COUNT(*) FROM $TABLE_AUTOMATION_RULES"
        val db = this.readableDatabase
        val cursor = db.rawQuery(countQuery, null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        
        if (count == 0) {
            Log.d("DbHelper", "No automation rules found in database")
            return false
        }
        
        Log.d("DbHelper", "Found $count automation rules in database")

        // Query to find matching rules - now checks for both amount range and matching transactor
        val query = """
            SELECT * FROM $TABLE_AUTOMATION_RULES 
            WHERE (
                (${KEY_AUTOMATION_RULE_MIN_AMOUNT} IS NULL OR ${KEY_AUTOMATION_RULE_MIN_AMOUNT} <= ?) 
                AND (${KEY_AUTOMATION_RULE_MAX_AMOUNT} IS NULL OR ${KEY_AUTOMATION_RULE_MAX_AMOUNT} >= ?)
                AND (
                    ${KEY_AUTOMATION_RULE_TRANSACTOR_ID} IS NULL 
                    OR ${KEY_AUTOMATION_RULE_TRANSACTOR_ID} = ?
                )
            )
        """.trimIndent()

        var matchingRuleFound = false

        try {
            val params = arrayOf(
                amount.toString(),
                amount.toString(),
                transactorId?.toString() ?: "-1"  // Use -1 if transactorId is null
            )
            
            Log.d("DbHelper", "Executing query with params: amount=$amount, transactorId=$transactorId")
            val cursor = db.rawQuery(query, params)
            
            Log.d("DbHelper", "Query returned ${cursor.count} results")

            matchingRuleFound = cursor.count > 0
            
            if (matchingRuleFound) {
                cursor.moveToFirst()
                val ruleName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_NAME))
                val ruleId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_ID))
                val ruleTransactorId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_TRANSACTOR_ID))
                Log.d("DbHelper", "Found matching automation rule: $ruleName (ID: $ruleId, TransactorID: $ruleTransactorId)")
            } else {
                Log.d("DbHelper", "No matching automation rules found")
            }
            
            cursor.close()
        } catch (e: Exception) {
            Log.e("DbHelper", "Error checking automation rules: ${e.message}", e)
            return false
        }

        Log.d("DbHelper", "Is automated: $matchingRuleFound")
        return matchingRuleFound
    }

    /**
     * Get owner by ID
     * @param ownerId The ID of the owner to retrieve
     * @return Owner object if found, null otherwise
     */
    fun getOwnerById(ownerId: Int): Owner? {
        if (ownerId <= 0) return null

        val db = this.readableDatabase
        var owner: Owner? = null

        try {
            val query = "SELECT * FROM $TABLE_OWNERS WHERE $COL_OWNER_ID = ?"
            val cursor = db.rawQuery(query, arrayOf(ownerId.toString()))

            cursor.use { c ->
                if (c.moveToFirst()) {
                    owner = Owner(
                        id = c.getInt(c.getColumnIndexOrThrow(COL_OWNER_ID)),
                        name = c.getString(c.getColumnIndexOrThrow(COL_OWNER_NAME)),
                        logoPath = c.getString(c.getColumnIndexOrThrow(COL_OWNER_LOGO_PATH)),
                        ownerCode = c.getString(c.getColumnIndexOrThrow(COL_OWNER_CODE)),
                        isDeleted = c.getInt(c.getColumnIndexOrThrow(COL_IS_OWNER_DELETED)) == 1
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting owner by ID: ${e.message}", e)
        }

        return owner
    }

    /**
     * Get truck by ID
     * @param truckId The ID of the truck to retrieve
     * @return Truck object if found, null otherwise
     */
    fun getTruckById(truckId: Int): Truck? {
        if (truckId <= 0) return null

        val db = this.readableDatabase
        var truck: Truck? = null

        try {
            val query = """
                    SELECT t.*, o.${COL_OWNER_NAME} as owner_name 
                    FROM $TABLE_TRUCKS t
                    LEFT JOIN $TABLE_OWNERS o ON t.${COL_TRUCK_OWNER} = o.${COL_OWNER_CODE}
                    WHERE t.${COL_ID} = ?
                """.trimIndent()

            val cursor = db.rawQuery(query, arrayOf(truckId.toString()))

            cursor.use { c ->
                if (c.moveToFirst()) {
                    truck = Truck(
                        id = c.getInt(c.getColumnIndexOrThrow(COL_ID)),
                        truckNo = c.getString(c.getColumnIndexOrThrow(COL_TRUCK_NO)),
                        make = c.getString(c.getColumnIndexOrThrow(COL_TRUCK_MAKE)),
                        owner = getOwnerByCode(c.getString(c.getColumnIndexOrThrow(COL_TRUCK_OWNER))),
                        activeStatus = c.getInt(c.getColumnIndexOrThrow(COL_TRUCK_ACTIVE_STATUS)) == 1,
                        isDeleted = c.getInt(c.getColumnIndexOrThrow(COL_IS_DELETED)) == 1
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting truck by ID: ${e.message}", e)
        }

        return truck
    }

    fun findMatchingAutomationRule(pettyCash: PettyCash): AutomationRule? {
        // Ensure the automation rules table exists
        ensureAutomationRulesTableExists()
        
        val amount = pettyCash.amount ?: return null
        val transactorId = pettyCash.transactor?.id
        
        // Log detailed information about the petty cash and transactor
        Log.d("DbHelper", "Finding automation rule for: amount=$amount, transactorId=$transactorId")
        Log.d("DbHelper", "Transactor details: name=${pettyCash.transactor?.name}, id=${pettyCash.transactor?.id}")
        
        // First check if there are any automation rules at all
        val countQuery = "SELECT COUNT(*) FROM $TABLE_AUTOMATION_RULES"
        val db = this.readableDatabase
        val countCursor = db.rawQuery(countQuery, null)
        countCursor.moveToFirst()
        val count = countCursor.getInt(0)
        countCursor.close()
        
        if (count == 0) {
            Log.d("DbHelper", "No automation rules found in database")
            return null
        }
        
        Log.d("DbHelper", "Found $count automation rules in database")

        // Query to find matching rules - now checks for both amount range and matching transactor
        val query = """
            SELECT ar.*, 
                   t.${COL_TRANSACTOR_NAME} as transactor_name,
                   a.${COL_ACCOUNT_NAME} as account_name,
                   o.${COL_OWNER_NAME} as owner_name,
                   tr.${COL_TRUCK_NO} as truck_name
            FROM $TABLE_AUTOMATION_RULES ar
            LEFT JOIN $TABLE_TRANSACTORS t ON ar.$KEY_AUTOMATION_RULE_TRANSACTOR_ID = t.$COL_TRANSACTOR_ID
            LEFT JOIN $TABLE_ACCOUNTS a ON ar.$KEY_AUTOMATION_RULE_ACCOUNT_ID = a.$COL_ACCOUNT_ID
            LEFT JOIN $TABLE_OWNERS o ON ar.$KEY_AUTOMATION_RULE_OWNER_ID = o.$COL_OWNER_ID
            LEFT JOIN $TABLE_TRUCKS tr ON ar.$KEY_AUTOMATION_RULE_TRUCK_ID = tr.$COL_ID
            WHERE (
                (ar.${KEY_AUTOMATION_RULE_MIN_AMOUNT} IS NULL OR ar.${KEY_AUTOMATION_RULE_MIN_AMOUNT} <= ?) 
                AND (ar.${KEY_AUTOMATION_RULE_MAX_AMOUNT} IS NULL OR ar.${KEY_AUTOMATION_RULE_MAX_AMOUNT} >= ?)
                AND (
                    ar.${KEY_AUTOMATION_RULE_TRANSACTOR_ID} IS NULL 
                    OR ar.${KEY_AUTOMATION_RULE_TRANSACTOR_ID} = ?
                )
            )
            ORDER BY ar.${KEY_AUTOMATION_RULE_UPDATED_AT} DESC
            LIMIT 1
        """.trimIndent()

        try {
            val db = this.readableDatabase
            val params = arrayOf(
                amount.toString(),
                amount.toString(),
                transactorId?.toString() ?: "-1"  // Use -1 if transactorId is null
            )
            
            Log.d("DbHelper", "Executing query with params: amount=$amount, transactorId=$transactorId")
            val cursor = db.rawQuery(query, params)
            
            Log.d("DbHelper", "Query returned ${cursor.count} results")

            var rule: AutomationRule? = null
            
            if (cursor.moveToFirst()) {
                // Get all column names for debugging
                val columnNames = cursor.columnNames.joinToString(", ")
                Log.d("DbHelper", "Cursor columns: $columnNames")
                
                try {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_NAME))
                    val ruleTransactorId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_TRANSACTOR_ID))
                    Log.d("DbHelper", "Found rule: id=$id, name=$name, transactorId=$ruleTransactorId")
                    
                    rule = AutomationRule(
                        id = id,
                        name = name,
                        transactorId = ruleTransactorId,
                        transactorName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("transactor_name")),
                        accountId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_ACCOUNT_ID)),
                        accountName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("account_name")),
                        ownerId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_OWNER_ID)),
                        ownerName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("owner_name")),
                        truckId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_TRUCK_ID)),
                        truckName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("truck_name")),
                        descriptionPattern = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_DESCRIPTION_PATTERN)),
                        minAmount = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_MIN_AMOUNT)),
                        maxAmount = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_MAX_AMOUNT)),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_CREATED_AT)),
                        updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOMATION_RULE_UPDATED_AT))
                    )
                    Log.d("DbHelper", "Created AutomationRule object: ${rule.name}, accountId=${rule.accountId}, ownerId=${rule.ownerId}, transactorId=${rule.transactorId}")
                } catch (e: Exception) {
                    Log.e("DbHelper", "Error creating AutomationRule from cursor: ${e.message}", e)
                }
            } else {
                Log.d("DbHelper", "No matching automation rule found")
            }

            cursor.close()
            return rule
        } catch (e: Exception) {
            Log.e("DbHelper", "Error finding matching automation rule: ${e.message}", e)
            return null
        }
    }

    /**
     * Adds a PettyCash to the queue for syncing with QuickBooks
     */
    fun addToQueue(pettyCash: PettyCash): Long {
        var db = this.writableDatabase
        var shouldCloseDb = false
        
        try {
            // Check if the database is open, if not, get a new instance
            if (!db.isOpen) {
                Log.d("DbHelper", "Database was closed, opening a new connection")
                db = this.writableDatabase
                shouldCloseDb = true
            }
            
            // Ensure queue table exists before proceeding
            createQueueTable(db)

            // If the PettyCash ID is null but we have a pettyCashNumber, try to retrieve it from the database
            val pettyCashWithId = if (pettyCash.id == null && !pettyCash.pettyCashNumber.isNullOrEmpty()) {
                getPettyCashByPettyCashNumber(pettyCash.pettyCashNumber!!) ?: pettyCash
            } else {
                pettyCash
            }
            
            // Log the ID for debugging
            Log.d("DbHelper", "PettyCash ID: ${pettyCashWithId.id}, Number: ${pettyCashWithId.pettyCashNumber}")
            
            // Check if we have a valid ID now
            if (pettyCashWithId.id == null) {
                Log.e("DbHelper", "PettyCash still has null ID after retrieval attempt")
                return -1
            }
            
            // Use a local variable for the transaction to avoid issues with the outer db variable
            var localDb = db
            if (!localDb.isOpen) {
                Log.e("DbHelper", "Error getting database instance")
                localDb = this.writableDatabase
            }
            
            // Begin transaction for better reliability
            localDb.beginTransaction()
            try {
                val values = ContentValues()
                
                // Extract truck numbers into a comma-separated string
                val truckNumbers = pettyCashWithId.trucks?.joinToString(", ") { it.truckNo ?: "" } ?: ""
                
                // Populate ContentValues with PettyCash properties
                values.put(COL_QUEUE_PETTY_CASH_ID, pettyCashWithId.id)
                values.put(COL_QUEUE_PETTY_CASH_NUMBER, pettyCashWithId.pettyCashNumber)
                values.put(COL_QUEUE_AMOUNT, pettyCashWithId.amount)
                values.put(COL_QUEUE_DESCRIPTION, pettyCashWithId.description)
                values.put(COL_QUEUE_DATE, pettyCashWithId.date)
                values.put(COL_QUEUE_ACCOUNT_ID, pettyCashWithId.account?.id)
                values.put(COL_QUEUE_ACCOUNT_NAME, pettyCashWithId.account?.name)
                values.put(COL_QUEUE_OWNER_ID, pettyCashWithId.owner?.id)
                values.put(COL_QUEUE_OWNER_NAME, pettyCashWithId.owner?.name)
                values.put(COL_QUEUE_TRUCK_NUMBERS, truckNumbers)
                values.put(COL_QUEUE_STATUS, QueueItem.STATUS_PENDING) // Default status
                values.put(COL_QUEUE_CREATED_AT, getCurrentDateTime())
                values.put(COL_QUEUE_UPDATED_AT, getCurrentDateTime())
                
                // Check if the entry is already in the queue
                val existingId = getQueueItemIdByPettyCashId(pettyCashWithId.id ?: -1)
                
                val queueId = if (existingId != null) {
                    // Update existing entry
                    localDb.update(TABLE_QUEUE, values, "$COL_QUEUE_ID = ?", arrayOf(existingId.toString())).toLong()
                    existingId.toLong()
                } else {
                    // Insert new entry
                    localDb.insertOrThrow(TABLE_QUEUE, null, values)
                }
                
                // Mark transaction as successful
                localDb.setTransactionSuccessful()
                return queueId
            } catch (e: Exception) {
                Log.e("DbHelper", "Error adding to queue: ${e.message}")
                return -1
            } finally {
                try {
                    // End transaction
                    localDb.endTransaction()
                } catch (e: Exception) {
                    Log.e("DbHelper", "Error ending transaction: ${e.message}")
                }
                
                // Close the database if we opened it in this method
                if (shouldCloseDb) {
                    try {
                        db.close()
                    } catch (e: Exception) {
                        Log.e("DbHelper", "Error closing database: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error in addToQueue: ${e.message}")
            return -1
        }
    }

    /**
     * Add related petty cash entries (main and transaction cost) to queue, linking them
     */
    fun addRelatedPettyCashToQueue(mainPettyCash: PettyCash, transactionCostPettyCash: PettyCash): Pair<Long, Long> {
        var db = this.writableDatabase
        var shouldCloseDb = false
        
        try {
            // Check if the database is open, if not, get a new instance
            if (!db.isOpen) {
                Log.d("DbHelper", "Database was closed, opening a new connection")
                db = this.writableDatabase
                shouldCloseDb = true
            }
            
            // Ensure queue table exists before proceeding
            createQueueTable(db)
            
            // First, check if the PettyCash objects have valid IDs
            // If not, try to retrieve them from the database by their pettyCashNumber
            val mainPettyCashWithId = if (mainPettyCash.id == null && !mainPettyCash.pettyCashNumber.isNullOrEmpty()) {
                getPettyCashByPettyCashNumber(mainPettyCash.pettyCashNumber!!) ?: mainPettyCash
            } else {
                mainPettyCash
            }
            
            val transactionCostPettyCashWithId = if (transactionCostPettyCash.id == null && !transactionCostPettyCash.pettyCashNumber.isNullOrEmpty()) {
                getPettyCashByPettyCashNumber(transactionCostPettyCash.pettyCashNumber!!) ?: transactionCostPettyCash
            } else {
                transactionCostPettyCash
            }
            
            // Log the IDs for debugging
            Log.d("DbHelper", "Main PettyCash ID: ${mainPettyCashWithId.id}, Number: ${mainPettyCashWithId.pettyCashNumber}")
            Log.d("DbHelper", "Transaction Cost PettyCash ID: ${transactionCostPettyCashWithId.id}, Number: ${transactionCostPettyCashWithId.pettyCashNumber}")
            
            // Check if we have valid IDs now
            if (mainPettyCashWithId.id == null) {
                Log.e("DbHelper", "Main PettyCash still has null ID after retrieval attempt")
                return Pair(-1, -1)
            }
            
            if (transactionCostPettyCashWithId.id == null) {
                Log.e("DbHelper", "Transaction Cost PettyCash still has null ID after retrieval attempt")
                return Pair(-1, -1)
            }
            
            // Use a local variable for the transaction to avoid issues with the outer db variable
            var localDb = db
            if (!localDb.isOpen) {
                Log.e("DbHelper", "Error getting database instance")
                localDb = this.writableDatabase
            }
            localDb.beginTransaction()
            try {
                // Add main petty cash to queue using a separate method call that doesn't start its own transaction
                val mainQueueId = addToQueueInternal(localDb, mainPettyCashWithId)
                
                // Prepare values for transaction cost petty cash
                val values = ContentValues()
                
                // Extract truck numbers into a comma-separated string
                val truckNumbers = transactionCostPettyCashWithId.trucks?.joinToString(", ") { it.truckNo ?: "" } ?: ""
                
                // Populate ContentValues with transaction cost PettyCash properties
                values.put(COL_QUEUE_PETTY_CASH_ID, transactionCostPettyCashWithId.id)
                values.put(COL_QUEUE_PETTY_CASH_NUMBER, transactionCostPettyCashWithId.pettyCashNumber)
                values.put(COL_QUEUE_AMOUNT, transactionCostPettyCashWithId.amount)
                values.put(COL_QUEUE_DESCRIPTION, transactionCostPettyCashWithId.description)
                values.put(COL_QUEUE_DATE, transactionCostPettyCashWithId.date)
                values.put(COL_QUEUE_ACCOUNT_ID, transactionCostPettyCashWithId.account?.id)
                values.put(COL_QUEUE_ACCOUNT_NAME, transactionCostPettyCashWithId.account?.name)
                values.put(COL_QUEUE_OWNER_ID, transactionCostPettyCashWithId.owner?.id)
                values.put(COL_QUEUE_OWNER_NAME, transactionCostPettyCashWithId.owner?.name)
                values.put(COL_QUEUE_TRUCK_NUMBERS, truckNumbers)
                values.put(COL_QUEUE_STATUS, QueueItem.STATUS_PENDING) // Default status
                values.put(COL_QUEUE_CREATED_AT, getCurrentDateTime())
                values.put(COL_QUEUE_UPDATED_AT, getCurrentDateTime())
                values.put(COL_QUEUE_IS_TRANSACTION_COST, 1) // Mark as transaction cost
                values.put(COL_QUEUE_RELATED_ITEM_ID, mainQueueId) // Link to main queue item
                
                // Check if the transaction cost entry is already in the queue
                val existingId = getQueueItemIdByPettyCashId(transactionCostPettyCashWithId.id ?: -1)
                
                val costQueueId = if (existingId != null) {
                    // Update existing entry
                    localDb.update(TABLE_QUEUE, values, "$COL_QUEUE_ID = ?", arrayOf(existingId.toString())).toLong()
                    existingId.toLong()
                } else {
                    // Insert new entry
                    localDb.insertOrThrow(TABLE_QUEUE, null, values)
                }
                
                localDb.setTransactionSuccessful()
                return Pair(mainQueueId, costQueueId)
            } catch (e: Exception) {
                Log.e("DbHelper", "Error adding related petty cash to queue: ${e.message}")
                return Pair(-1, -1)
            } finally {
                try {
                    localDb.endTransaction()
                } catch (e: Exception) {
                    Log.e("DbHelper", "Error ending transaction: ${e.message}")
                }
                
                // Close the database if we opened it in this method
                if (shouldCloseDb) {
                    try {
                        db.close()
                    } catch (e: Exception) {
                        Log.e("DbHelper", "Error closing database: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error in addRelatedPettyCashToQueue: ${e.message}")
            return Pair(-1, -1)
        }
    }

    // Internal helper method to add a petty cash to queue without starting a new transaction
    private fun addToQueueInternal(db: SQLiteDatabase, pettyCash: PettyCash): Long {
        // Ensure queue table exists before proceeding
        var localDb = db
        createQueueTable(db)

        if (!db.isOpen) {
            localDb = this.writableDatabase
        }

        val values = ContentValues()
        
        // Extract truck numbers into a comma-separated string
        val truckNumbers = pettyCash.trucks?.joinToString(", ") { it.truckNo ?: "" } ?: ""
        
        // Populate ContentValues with PettyCash properties
        values.put(COL_QUEUE_PETTY_CASH_ID, pettyCash.id)
        values.put(COL_QUEUE_PETTY_CASH_NUMBER, pettyCash.pettyCashNumber)
        values.put(COL_QUEUE_AMOUNT, pettyCash.amount)
        values.put(COL_QUEUE_DESCRIPTION, pettyCash.description)
        values.put(COL_QUEUE_DATE, pettyCash.date)
        values.put(COL_QUEUE_ACCOUNT_ID, pettyCash.account?.id)
        values.put(COL_QUEUE_ACCOUNT_NAME, pettyCash.account?.name)
        values.put(COL_QUEUE_OWNER_ID, pettyCash.owner?.id)
        values.put(COL_QUEUE_OWNER_NAME, pettyCash.owner?.name)
        values.put(COL_QUEUE_TRUCK_NUMBERS, truckNumbers)
        values.put(COL_QUEUE_STATUS, QueueItem.STATUS_PENDING) // Default status
        values.put(COL_QUEUE_CREATED_AT, getCurrentDateTime())
        values.put(COL_QUEUE_UPDATED_AT, getCurrentDateTime())
        
        // Check if the entry is already in the queue
        val existingId = getQueueItemIdByPettyCashId(pettyCash.id ?: -1)
        
        return try {
            if (existingId != null) {
                // Update existing entry
                localDb.update(TABLE_QUEUE, values, "$COL_QUEUE_ID = ?", arrayOf(existingId.toString())).toLong()
                existingId.toLong()
            } else {
                // Insert new entry
                localDb.insertOrThrow(TABLE_QUEUE, null, values)
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error adding to queue: ${e.message}")
            -1
        }
    }

    /**
     * Check if a petty cash is already in the queue
     */
    fun isPettyCashInQueue(pettyCashId: Int): Boolean {
        return getQueueItemIdByPettyCashId(pettyCashId) != null
    }

    /**
     * Get queue item ID by petty cash ID
     */
    fun getQueueItemIdByPettyCashId(pettyCashId: Int): Int? {
        if (pettyCashId <= 0) return null

        var db = this.readableDatabase
        var shouldCloseDb = false
        var id: Int? = null

        try {
            // Check if the database is open, if not, get a new instance
            if (!db.isOpen) {
                Log.d("DbHelper", "Database was closed, opening a new connection")
                db = this.readableDatabase
                shouldCloseDb = true
            }

            try {
                val cursor = db.query(
                    TABLE_QUEUE,
                    arrayOf(COL_QUEUE_ID),
                    "$COL_QUEUE_PETTY_CASH_ID = ?",
                    arrayOf(pettyCashId.toString()),
                    null, null, null
                )

                cursor.use {
                    if (it.moveToFirst()) {
                        id = it.getInt(it.getColumnIndexOrThrow(COL_QUEUE_ID))
                    }
                }
            } catch (e: Exception) {
                Log.e("DbHelper", "Error checking if petty cash is in queue: ${e.message}", e)
            } finally {
                // Close the database if we opened it in this method
                if (shouldCloseDb) {
                    try {
                        db.close()
                    } catch (e: Exception) {
                        Log.e("DbHelper", "Error closing database: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error in getQueueItemIdByPettyCashId: ${e.message}")
        }

        return id
    }

    /**
     * Update queue item status
     */
    fun updateQueueItemStatus(queueItemId: Int, status: String, errorMessage: String? = null): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        try {
            values.apply {
                put(COL_QUEUE_STATUS, status)
                put(COL_QUEUE_UPDATED_AT, QueueItem.getCurrentDateTime())
                if (errorMessage != null) {
                    put(COL_QUEUE_ERROR_MESSAGE, errorMessage)
                }
            }

            val rowsAffected = db.update(
                TABLE_QUEUE,
                values,
                "$COL_QUEUE_ID = ?",
                arrayOf(queueItemId.toString())
            )

            return rowsAffected > 0
        } catch (e: Exception) {
            Log.e("DbHelper", "Error updating queue item status: ${e.message}", e)
            return false
        }
    }

    /**
     * Get queue items by status
     */
    fun getQueueItemsByStatus(status: String): List<QueueItem> {
        val queueItems = mutableListOf<QueueItem>()
        val db = this.readableDatabase

        try {
            val cursor = db.query(
                TABLE_QUEUE,
                null,
                "$COL_QUEUE_STATUS = ?",
                arrayOf(status),
                null, null,
                "$COL_QUEUE_CREATED_AT ASC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    queueItems.add(cursorToQueueItem(it))
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting queue items by status: ${e.message}", e)
        }

        return queueItems
    }

    /**
     * Get all queue items
     */
    fun getAllQueueItems(): List<QueueItem> {
        val queueItems = mutableListOf<QueueItem>()
        val db = this.readableDatabase

        try {
            val cursor = db.query(
                TABLE_QUEUE,
                null,
                null,
                null,
                null,
                null,
                "$COL_QUEUE_CREATED_AT DESC" // Newest first
            )

            cursor.use {
                while (it.moveToNext()) {
                    queueItems.add(cursorToQueueItem(it))
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting all queue items: ${e.message}", e)
        }

        return queueItems
    }
    
    /**
     * Get summary of queue items by status
     */
    fun getQueueSummary(): Map<String, Int> {
        val summary = mutableMapOf(
            QueueItem.STATUS_PENDING to 0,
            QueueItem.STATUS_SENT to 0,
            QueueItem.STATUS_SYNCED to 0,
            QueueItem.STATUS_FAILED to 0
        )
        
        val db = this.readableDatabase
        
        try {
            val cursor = db.rawQuery(
                "SELECT $COL_QUEUE_STATUS, COUNT(*) as count FROM $TABLE_QUEUE GROUP BY $COL_QUEUE_STATUS",
                null
            )
            
            cursor.use {
                while (it.moveToNext()) {
                    val status = it.getString(it.getColumnIndexOrThrow(COL_QUEUE_STATUS))
                    val count = it.getInt(it.getColumnIndexOrThrow("count"))
                    summary[status] = count
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting queue summary: ${e.message}", e)
        }
        
        return summary
    }
    
    /**
     * Search queue items
     */
    fun searchQueueItems(query: String): List<QueueItem> {
        val queueItems = mutableListOf<QueueItem>()
        val db = this.readableDatabase
        
        try {
            // Search in petty cash number, description, account name, or owner name
            val searchQuery = "%$query%"
            val cursor = db.query(
                TABLE_QUEUE,
                null,
                "$COL_QUEUE_PETTY_CASH_NUMBER LIKE ? OR " +
                "$COL_QUEUE_DESCRIPTION LIKE ? OR " +
                "$COL_QUEUE_ACCOUNT_NAME LIKE ? OR " +
                "$COL_QUEUE_OWNER_NAME LIKE ? OR " +
                "$COL_QUEUE_TRUCK_NUMBERS LIKE ?",
                arrayOf(searchQuery, searchQuery, searchQuery, searchQuery, searchQuery),
                null,
                null,
                "$COL_QUEUE_CREATED_AT DESC"
            )
            
            cursor.use {
                while (it.moveToNext()) {
                    queueItems.add(cursorToQueueItem(it))
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error searching queue items: ${e.message}", e)
        }
        
        return queueItems
    }
    
    /**
     * Convert database cursor to QueueItem object
     */
    private fun cursorToQueueItem(cursor: Cursor): QueueItem {
        return QueueItem(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUEUE_ID)),
            pettyCashId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUEUE_PETTY_CASH_ID)),
            pettyCashNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUEUE_PETTY_CASH_NUMBER)),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_QUEUE_AMOUNT)),
            description = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_DESCRIPTION)) ?: "",
            date = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUEUE_DATE)),
            accountId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_ACCOUNT_ID)),
            accountName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_ACCOUNT_NAME)),
            ownerId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_OWNER_ID)),
            ownerName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_OWNER_NAME)),
            truckNumbers = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_TRUCK_NUMBERS)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUEUE_STATUS)),
            errorMessage = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_ERROR_MESSAGE)),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUEUE_CREATED_AT)),
            updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUEUE_UPDATED_AT)),
            isTransactionCost = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUEUE_IS_TRANSACTION_COST)) == 1,
            relatedQueueItemId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(COL_QUEUE_RELATED_ITEM_ID))
        )
    }

    companion object {
        private const val TAG = "DbHelper"

        fun dropAllTables(db: SQLiteDatabase) {

            // List all the table names you want to drop
            val tableNames = arrayOf(
                TABLE_TRANSACTIONS,
                TABLE_REJECTED_SMS,
                TABLE_ALL_SMS,
                TABLE_SEARCH_HISTORY,
                TABLE_TRUCKS,
                TABLE_OWNERS,
                TABLE_SEARCH_HISTORY_TRUCKS,
                TABLE_ACCOUNTS,
                TABLE_SEARCH_HISTORY_ACCOUNTS,
                TABLE_PETTY_CASH,
                TABLE_SEARCH_HISTORY_OWNERS,
                TABLE_TRANSACTORS,
                TABLE_SUPPORTING_DOCUMENT
            )

            for (tableName in tableNames) {
                db.execSQL("DROP TABLE IF EXISTS $tableName;")
            }

            db.close()
        }

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
            const val COL_TRANSACTIONS_IS_CONVERTED_TO_PETTY_CASH = "is_converted_to_petty_cash"


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
            const val COL_TRANSACTOR_KRA_PIN = "kra_pin"
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

            // PETTY CASH TABLE VARIABLES

            const val TABLE_PETTY_CASH = "petty_cash"
            const val COL_PETTY_CASH_ID = "id"
            const val COL_PETTY_CASH_NUMBER = "petty_cash_no"
            const val COL_PETTY_CASH_AMOUNT = "amount"
            const val COL_PETTY_CASH_DATE = "date"
            const val COL_PETTY_CASH_DESCRIPTION = "description"
            const val COL_PETTY_CASH_TRANSACTOR = "transactor"
            const val COL_PETTY_CASH_ACCOUNT = "account"
            const val COL_PETTY_CASH_PAYMENT_MODE = "payment_mode"
            const val COL_PETTY_CASH_OWNER = "owner"
            const val COL_PETTY_CASH_MPESA_TRANSACTION = "mpesa_transaction"
            const val COL_PETTY_CASH_TRUCKS = "trucks"
            const val COL_PETTY_CASH_SIGNATURE = "signature"
            const val COL_PETTY_CASH_SUPPORTING_DOCUMENT = "supporting_document"
            const val COL_PETTY_CASH_USER = "user"
            const val COL_PETTY_CASH_IS_DELETED = "is_deleted"

            // SUPPORTING DOCUMENT TABLE VARIABLES

            const val TABLE_SUPPORTING_DOCUMENT = "supporting_documents"
            const val COL_SUPPORTING_DOCUMENT_ID = "id"
            const val COL_SUPPORTING_DOCUMENT_TYPE = "type"
            const val COL_SUPPORTING_DOCUMENT_DOCUMENT_NO = "document_no"
            const val COL_SUPPORTING_DOCUMENT_CU_NUMBER = "cu_number"
            const val COL_SUPPORTING_DOCUMENT_SUPPLIER_NAME = "supplier_name"
            const val COL_SUPPORTING_DOCUMENT_TAXABLE_TOTAL_AMOUNT = "taxable_total_amount"
            const val COL_SUPPORTING_DOCUMENT_TAX_AMOUNT = "tax_amount"
            const val COL_SUPPORTING_DOCUMENT_TOTAL_AMOUNT = "total_amount"
            const val COL_SUPPORTING_DOCUMENT_DATE = "document_date"
            const val COL_SUPPORTING_DOCUMENT_IMAGE_PATH_1 = "image_path_1"
            const val COL_SUPPORTING_DOCUMENT_IMAGE_PATH_2 = "image_path_2"
            const val COL_SUPPORTING_DOCUMENT_IMAGE_PATH_3 = "image_path_3"

        // Queue table constants
        const val TABLE_QUEUE = "queue_items"
        const val COL_QUEUE_ID = "id"
        const val COL_QUEUE_PETTY_CASH_ID = "petty_cash_id"
        const val COL_QUEUE_PETTY_CASH_NUMBER = "petty_cash_number"
        const val COL_QUEUE_AMOUNT = "amount"
        const val COL_QUEUE_DESCRIPTION = "description"
        const val COL_QUEUE_DATE = "date"
        const val COL_QUEUE_ACCOUNT_ID = "account_id"
        const val COL_QUEUE_ACCOUNT_NAME = "account_name"
        const val COL_QUEUE_OWNER_ID = "owner_id"
        const val COL_QUEUE_OWNER_NAME = "owner_name"
        const val COL_QUEUE_TRUCK_NUMBERS = "truck_numbers"
        const val COL_QUEUE_STATUS = "status"
        const val COL_QUEUE_ERROR_MESSAGE = "error_message"
        const val COL_QUEUE_CREATED_AT = "created_at"
        const val COL_QUEUE_UPDATED_AT = "updated_at"
        const val COL_QUEUE_IS_TRANSACTION_COST = "is_transaction_cost"
        const val COL_QUEUE_RELATED_ITEM_ID = "related_queue_item_id"

        // Automation Rules Table
        private const val TABLE_AUTOMATION_RULES = "automation_rules"
        private const val KEY_AUTOMATION_RULE_ID = "id"
        private const val KEY_AUTOMATION_RULE_NAME = "name"
        private const val KEY_AUTOMATION_RULE_TRANSACTOR_ID = "transactor_id"
        private const val KEY_AUTOMATION_RULE_ACCOUNT_ID = "account_id"
        private const val KEY_AUTOMATION_RULE_OWNER_ID = "owner_id"
        private const val KEY_AUTOMATION_RULE_TRUCK_ID = "truck_id"
        private const val KEY_AUTOMATION_RULE_DESCRIPTION_PATTERN = "description_pattern"
        private const val KEY_AUTOMATION_RULE_MIN_AMOUNT = "min_amount"
        private const val KEY_AUTOMATION_RULE_MAX_AMOUNT = "max_amount"
        private const val KEY_AUTOMATION_RULE_CREATED_AT = "created_at"
        private const val KEY_AUTOMATION_RULE_UPDATED_AT = "updated_at"

        // Reports Table
        private const val REPORTS_TABLE = "reports"
        private const val REPORT_ID = "id"
        private const val REPORT_NAME = "name"
        private const val REPORT_TYPE = "type"
        private const val REPORT_DATE = "generated_date"
        private const val REPORT_FILE_PATH = "file_path"
        private const val REPORT_EXCEL_FILE_PATH = "excel_file_path"
        private const val REPORT_FILTERS = "filters"
    }

    fun createQueueTable(db: SQLiteDatabase) {
        try {
            // Check if the database is open before proceeding
            if (!db.isOpen) {
                Log.e("DbHelper", "Database is closed, cannot create queue table")
                return
            }
            
            val SQL_CREATE_TABLE_QUEUE = """
                CREATE TABLE IF NOT EXISTS $TABLE_QUEUE (
                    $COL_QUEUE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_QUEUE_PETTY_CASH_ID INTEGER NOT NULL,
                    $COL_QUEUE_PETTY_CASH_NUMBER TEXT NOT NULL,
                    $COL_QUEUE_AMOUNT REAL NOT NULL,
                    $COL_QUEUE_DESCRIPTION TEXT,
                    $COL_QUEUE_DATE TEXT NOT NULL,
                    $COL_QUEUE_ACCOUNT_ID INTEGER,
                    $COL_QUEUE_ACCOUNT_NAME TEXT,
                    $COL_QUEUE_OWNER_ID INTEGER,
                    $COL_QUEUE_OWNER_NAME TEXT,
                    $COL_QUEUE_TRUCK_NUMBERS TEXT,
                    $COL_QUEUE_STATUS TEXT NOT NULL,
                    $COL_QUEUE_ERROR_MESSAGE TEXT,
                    $COL_QUEUE_CREATED_AT TEXT NOT NULL,
                    $COL_QUEUE_UPDATED_AT TEXT NOT NULL,
                    $COL_QUEUE_IS_TRANSACTION_COST INTEGER DEFAULT 0,
                    $COL_QUEUE_RELATED_ITEM_ID INTEGER,
                    FOREIGN KEY ($COL_QUEUE_PETTY_CASH_ID) REFERENCES $TABLE_PETTY_CASH($COL_PETTY_CASH_ID) ON DELETE CASCADE
                )
            """.trimIndent()

            db.execSQL(SQL_CREATE_TABLE_QUEUE)
            
            // Create indexes for faster lookups
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_queue_petty_cash_id ON $TABLE_QUEUE($COL_QUEUE_PETTY_CASH_ID)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_queue_status ON $TABLE_QUEUE($COL_QUEUE_STATUS)")
            } catch (e: Exception) {
                Log.e("DbHelper", "Error creating queue table indexes: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error creating queue table: ${e.message}")
        }
    }

    // Helper method to get current date time in the required format
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Calculate the total amount of petty cash for the current month filtered by payment mode
     * @param paymentMode The payment mode to filter by (e.g., "M-Pesa", "Cash")
     * @return The total amount as a Double
     */
    fun getTotalPettyCashAmountForCurrentMonthByPaymentMode(paymentMode: String): Double {
        val db = this.readableDatabase
        var totalAmount = 0.0

        try {
            val calendar = Calendar.getInstance()
            val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1) // Months are 0-indexed
            val currentYear = calendar.get(Calendar.YEAR).toString()

            // Construct SQL query to sum the amount directly in the database
            val sql = """
                SELECT SUM($COL_PETTY_CASH_AMOUNT) FROM $TABLE_PETTY_CASH 
                WHERE (
                    (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth') 
                    OR 
                    (SUBSTR($COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND LENGTH($COL_PETTY_CASH_DATE) = 10 AND SUBSTR($COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth')
                )
                AND $COL_PETTY_CASH_PAYMENT_MODE = ?
                AND $COL_PETTY_CASH_IS_DELETED = 0
            """.trimIndent()

            val cursor = db.rawQuery(sql, arrayOf(paymentMode))
            
            cursor.use {
                if (it.moveToFirst()) {
                    // Check if the result is not null
                    if (!it.isNull(0)) {
                        totalAmount = it.getDouble(0)
                    }
                }
            }
            
            Log.d("DbHelper", "Total $paymentMode amount for current month: $totalAmount")
        } catch (e: Exception) {
            Log.e("DbHelper", "Error calculating total amount for $paymentMode: ${e.message}", e)
        }
        // Don't close the database here, let the calling method handle it
        
        return totalAmount
    }

    // Add method to get all trucks
    fun getAllTrucks(): List<String> {
        val truckNames = mutableListOf<String>()
        val trucks = getLocalTrucks()
        
        for (truck in trucks) {
            val truckNo = truck.truckNo
            if (truckNo != null && truckNo.isNotEmpty()) {
                truckNames.add(truckNo)
            }
        }
        
        return truckNames
    }
    
    // Add method to get all transactors for M-Pesa
    fun getAllTransactorsForMpesa(): List<String> {
        val transactors = mutableListOf<String>()
        val db = this.readableDatabase
        
        try {
            // Query for transactors that have been involved in M-Pesa transactions
            val query = """
                SELECT DISTINCT t.$COL_TRANSACTOR_NAME FROM $TABLE_TRANSACTORS t
                INNER JOIN $TABLE_TRANSACTIONS tr 
                ON (t.$COL_TRANSACTOR_NAME = tr.$COL_TRANSACTIONS_RECEPIENT_NAME 
                    OR t.$COL_TRANSACTOR_NAME = tr.$COL_TRANSACTIONS_SENDER_NAME)
                WHERE tr.$COL_TRANSACTIONS_TRANSACTION_TYPE LIKE '%M-PESA%'
                AND t.$COL_IS_TRANSACTOR_DELETED = 0
                ORDER BY t.$COL_TRANSACTOR_NAME ASC
            """
            
            val cursor = db.rawQuery(query, null)
            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_NAME))
                    if (name != null && name.isNotEmpty()) {
                        transactors.add(name.trim().replace("  ", " "))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching transactors for M-Pesa", e)
        } finally {
            db.close()
        }
        
        return transactors
    }
    
    // Add method to get all account names
    fun getAllAccountNames(): List<String> {
        val accounts = mutableListOf<String>()
        val db = this.readableDatabase
        
        try {
            val query = """
                SELECT $COL_ACCOUNT_NAME FROM $TABLE_ACCOUNTS
                WHERE $COL_ACCOUNT_IS_DELETED = 0
                ORDER BY $COL_ACCOUNT_NAME ASC
            """
            
            val cursor = db.rawQuery(query, null)
            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndexOrThrow(COL_ACCOUNT_NAME))
                    if (name != null && name.isNotEmpty()) {
                        accounts.add(name)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching account names", e)
        } finally {
            db.close()
        }
        
        return accounts
    }

    // Add these methods to handle reports
    fun saveReport(report: Report): Boolean {
        try {
            Log.d("DbHelper", "Saving report: ${report.id}, ${report.name}, ${report.type}")
            val db = this.writableDatabase
            
            // Check if reports table exists
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(REPORTS_TABLE)
            )
            val tableExists = cursor.use { it.count > 0 }
            Log.d("DbHelper", "Reports table exists: $tableExists")
            
            // Create table if it doesn't exist
            if (!tableExists) {
                Log.d("DbHelper", "Creating reports table")
                db.execSQL("""
                    CREATE TABLE $REPORTS_TABLE (
                        $REPORT_ID TEXT PRIMARY KEY,
                        $REPORT_NAME TEXT NOT NULL,
                        $REPORT_TYPE TEXT NOT NULL,
                        $REPORT_DATE INTEGER NOT NULL,
                        $REPORT_FILE_PATH TEXT NOT NULL,
                        $REPORT_EXCEL_FILE_PATH TEXT,
                        $REPORT_FILTERS TEXT
                    )
                """.trimIndent())
            } else {
                // Check if the excel_file_path column exists
                val columnCursor = db.rawQuery("PRAGMA table_info($REPORTS_TABLE)", null)
                var excelColumnExists = false
                columnCursor.use {
                    while (it.moveToNext()) {
                        val columnName = it.getString(it.getColumnIndexOrThrow("name"))
                        if (columnName == REPORT_EXCEL_FILE_PATH) {
                            excelColumnExists = true
                            break
                        }
                    }
                }
                
                // Add the column if it doesn't exist
                if (!excelColumnExists) {
                    try {
                        Log.d("DbHelper", "Adding excel_file_path column to reports table")
                        db.execSQL("ALTER TABLE $REPORTS_TABLE ADD COLUMN $REPORT_EXCEL_FILE_PATH TEXT")
                    } catch (e: Exception) {
                        Log.e("DbHelper", "Error adding excel_file_path column: ${e.message}")
                    }
                }
            }
            
            val values = ContentValues().apply {
                put(REPORT_ID, report.id)
                put(REPORT_NAME, report.name)
                put(REPORT_TYPE, report.type.name)
                put(REPORT_DATE, report.generatedDate.time)
                put(REPORT_FILE_PATH, report.filePath)
                put(REPORT_FILTERS, Gson().toJson(report.filters))
                // Add excel file path if available
                if (report.excelFilePath != null) {
                    put(REPORT_EXCEL_FILE_PATH, report.excelFilePath)
                }
            }
            
            val result = db.insertWithOnConflict(REPORTS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d("DbHelper", "Report saved with result: $result")
            return result != -1L
        } catch (e: Exception) {
            Log.e("DbHelper", "Error saving report: ${e.message}", e)
            return false
        }
    }

    fun getReportsByType(type: ReportType): List<Report> {
        try {
            Log.d("DbHelper", "Getting reports of type: ${type.name}")
            val reports = mutableListOf<Report>()
            val db = this.readableDatabase
            
            // Check if reports table exists
            val tableCheckCursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(REPORTS_TABLE)
            )
            val tableExists = tableCheckCursor.use { it.count > 0 }
            if (!tableExists) {
                Log.d("DbHelper", "Reports table does not exist yet")
                return emptyList()
            }
            
            val cursor = db.query(
                REPORTS_TABLE,
                null,
                "$REPORT_TYPE = ?",
                arrayOf(type.name),
                null,
                null,
                "$REPORT_DATE DESC"
            )
            
            Log.d("DbHelper", "Found ${cursor.count} reports of type ${type.name}")
            
            cursor.use {
                while (it.moveToNext()) {
                    reports.add(cursor.toReport())
                }
            }
            
            Log.d("DbHelper", "Returning ${reports.size} reports")
            return reports
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting reports by type: ${e.message}", e)
            return emptyList()
        }
    }

    private fun Cursor.toReport(): Report {
        val excelFilePathIndex = getColumnIndex(REPORT_EXCEL_FILE_PATH)
        val excelFilePath = if (excelFilePathIndex != -1 && !isNull(excelFilePathIndex)) {
            getString(excelFilePathIndex)
        } else {
            null
        }
        
        return Report(
            id = getString(getColumnIndexOrThrow(REPORT_ID)),
            name = getString(getColumnIndexOrThrow(REPORT_NAME)),
            type = ReportType.valueOf(getString(getColumnIndexOrThrow(REPORT_TYPE))),
            generatedDate = Date(getLong(getColumnIndexOrThrow(REPORT_DATE))),
            filePath = getString(getColumnIndexOrThrow(REPORT_FILE_PATH)),
            excelFilePath = excelFilePath,
            filters = Gson().fromJson(
                getString(getColumnIndexOrThrow(REPORT_FILTERS)),
                object : TypeToken<Map<String, String>>() {}.type
            )
        )
    }

    /**
     * Deletes a report from the database by its ID
     */
    fun deleteReport(reportId: String): Boolean {
        val db = this.writableDatabase
        return db.delete(REPORTS_TABLE, "$REPORT_ID = ?", arrayOf(reportId)) > 0
    }

    /**
     * Get filtered petty cash transactions based on various parameters
     */
    fun getFilteredPettyCashTransactions(
        startDate: String,
        endDate: String,
        transactor: String = "",
        paymentMode: String = "",
        owner: String = "",
        truck: String = "",
        account: String = ""
    ): List<PettyCashTransaction> {
        val transactions = mutableListOf<PettyCashTransaction>()
        
        try {
            // Format the dates for SQLite date comparison
            // Input dates are in yyyy-MM-dd format, but database dates are in dd/MM/yyyy HH:mm:ss format
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dbFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            // Parse the input dates
            val parsedStartDate = inputFormat.parse(startDate)
            val parsedEndDate = inputFormat.parse(endDate)
            
            if (parsedStartDate == null || parsedEndDate == null) {
                Log.e("DbHelper", "Error parsing dates: startDate=$startDate, endDate=$endDate")
                return emptyList()
            }
            
            // Format to match the database date format
            val formattedStartDate = dbFormat.format(parsedStartDate)
            // Add 23:59:59 to the end date to include the entire day
            val formattedEndDate = dbFormat.format(parsedEndDate) + " 23:59:59"
            
            Log.d("DbHelper", "Formatted dates for query: start=$formattedStartDate, end=$formattedEndDate")
            
            // Build the query with a simpler approach
            val query = StringBuilder("""
                SELECT pc.*, 
                       t.name as transactor_name, 
                       o.name as owner_name,
                       a.name as account_name
                FROM $TABLE_PETTY_CASH pc
                LEFT JOIN transactors t ON pc.$COL_PETTY_CASH_TRANSACTOR = t.id
                LEFT JOIN owners o ON pc.$COL_PETTY_CASH_OWNER = o.owner_code
                LEFT JOIN accounts a ON pc.$COL_PETTY_CASH_ACCOUNT = a.id
                WHERE pc.$COL_PETTY_CASH_IS_DELETED = 0
                AND pc.$COL_PETTY_CASH_NUMBER IS NOT NULL
                AND pc.$COL_PETTY_CASH_NUMBER != ''
            """)

            // Add date filtering - we'll handle this in the code instead of complex SQL
            val args = mutableListOf<String>()

            if (transactor.isNotEmpty() && transactor != "All") {
                query.append(" AND t.name = ?")
                args.add(transactor)
            }

            if (paymentMode.isNotEmpty() && paymentMode != "All") {
                query.append(" AND pc.$COL_PETTY_CASH_PAYMENT_MODE = ?")
                args.add(paymentMode)
            }

            if (owner.isNotEmpty() && owner != "All") {
                query.append(" AND o.name = ?")
                args.add(owner)
            }

            // Store the truck filter value but don't apply it in SQL
            val truckFilter = if (truck.isNotEmpty() && truck != "All") truck else ""
            
            // We don't add truck filter to SQL query - we'll filter in memory

            if (account.isNotEmpty() && account != "All") {
                query.append(" AND a.name = ?")
                args.add(account)
            }

            query.append(" ORDER BY pc.$COL_PETTY_CASH_DATE ASC")

            Log.d("DbHelper", "Executing query: $query")
            Log.d("DbHelper", "With args: $args")

            val cursor = readableDatabase.rawQuery(query.toString(), args.toTypedArray())
            cursor.use {
                if (cursor.count > 0) {
                    Log.d("DbHelper", "Found ${cursor.count} transactions before date filtering")
                    while (it.moveToNext()) {
                        try {
                            // Get the date from the database
                            val dateStr = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                            
                            // Check if the date is within the range
                            if (isDateInRange(dateStr, formattedStartDate, formattedEndDate)) {
                                val trucksString = it.getStringOrNull(it.getColumnIndex(COL_PETTY_CASH_TRUCKS))?.replace(" ", "") ?: ""
                                val trucksList = trucksString.split(",").filter { t -> t.isNotEmpty() }
                                val truckCount = trucksList.size.coerceAtLeast(1) // At least 1 to avoid division by zero

                                Log.d("DbHelper", "Trucks for transaction: $trucksString")
                                Log.d("DbHelper", "Truck list: $trucksList")
                                Log.d("DbHelper", "Truck filter: $truckFilter")
                                
                                // Check if we should include this transaction based on truck filter
                                val shouldInclude = trucksList.contains(truckFilter)

                                Log.d("DbHelper", "Should include: $shouldInclude")
                                
                                // Only include transactions that match the truck filter
                                if (truckFilter.isEmpty() || shouldInclude) {
                                    var amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                                    
                                    // If filtering by a specific truck and the transaction has multiple trucks,
                                    // divide the amount by the number of trucks
                                    if (truckFilter.isNotEmpty() && truckCount > 1) {
                                        amount /= truckCount
                                    }
                                    
                                    val transaction = PettyCashTransaction(
                                        id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID)),
                                        pettyCashNumber = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER)),
                                        date = dateStr,
                                        amount = amount,
                                        description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION)),
                                        paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE)),
                                        transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR)),
                                        transactorName = it.getStringOrNull(it.getColumnIndex("transactor_name")),
                                        ownerId = it.getIntOrNull(it.getColumnIndex(COL_PETTY_CASH_OWNER)),
                                        ownerName = it.getStringOrNull(it.getColumnIndex("owner_name")),
                                        truckNumber = trucksString,
                                        accountId = it.getIntOrNull(it.getColumnIndex(COL_PETTY_CASH_ACCOUNT)),
                                        accountName = it.getStringOrNull(it.getColumnIndex("account_name"))
                                    )
                                    transactions.add(transaction)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("DbHelper", "Error parsing transaction: ${e.message}", e)
                            // Continue to next transaction instead of failing the whole query
                        }
                    }
                    Log.d("DbHelper", "After date filtering: ${transactions.size} transactions")
                } else {
                    Log.d("DbHelper", "No transactions found for the given filters")
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting filtered petty cash transactions: ${e.message}", e)
            // Return empty list instead of throwing exception
        }

        Log.d("DbHelper", "Returning ${transactions.size} transactions")
        return transactions
    }
    
    /**
     * Helper method to check if a date string is within a given range
     */
    private fun isDateInRange(dateStr: String?, startDateStr: String, endDateStr: String): Boolean {
        if (dateStr.isNullOrEmpty()) {
            return false
        }
        
        try {
            // Parse the date formats
            val dbFormat = if (dateStr.length > 10) {
                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            }
            
            val startFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val endFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            
            // Parse the dates
            val date = dbFormat.parse(dateStr) ?: return false
            val start = startFormat.parse(startDateStr) ?: return false
            val end = endFormat.parse(endDateStr) ?: return false
            
            // Check if the date is within the range
            return !date.before(start) && !date.after(end)
        } catch (e: Exception) {
            Log.e("DbHelper", "Error parsing date for range check: $dateStr, $startDateStr, $endDateStr", e)
            return false
        }
    }

    // Add this method to check if the database is open
    fun isOpen(): Boolean {
        return try {
            val db = writableDatabase
            db.isOpen
        } catch (e: Exception) {
            Log.e("DbHelper", "Error checking if database is open: ${e.message}")
            false
        }
    }

    /**
     * Updates an existing report in the database
     * @param report The report to update
     * @return true if the update was successful, false otherwise
     */
    fun updateReport(report: Report): Boolean {
        try {
            Log.d("DbHelper", "Updating report: ${report.id}, ${report.name}, ${report.type}")
            val db = this.writableDatabase
            
            val values = ContentValues().apply {
                put(REPORT_NAME, report.name)
                put(REPORT_TYPE, report.type.name)
                put(REPORT_DATE, report.generatedDate.time)
                put(REPORT_FILE_PATH, report.filePath)
                put(REPORT_FILTERS, Gson().toJson(report.filters))
                // Add excel file path if available
                if (report.excelFilePath != null) {
                    put(REPORT_EXCEL_FILE_PATH, report.excelFilePath)
                }
            }
            
            val whereClause = "$REPORT_ID = ?"
            val whereArgs = arrayOf(report.id)
            
            val result = db.update(REPORTS_TABLE, values, whereClause, whereArgs)
            Log.d("DbHelper", "Report updated with result: $result")
            return result > 0
        } catch (e: Exception) {
            Log.e("DbHelper", "Error updating report: ${e.message}", e)
            return false
        }
    }

    fun getAllPettyCashNumbers(): List<String> {
        val pettyCashNumbers = mutableListOf<String>()
        val db = this.readableDatabase
        
        try {
            val sql = """
                SELECT $COL_PETTY_CASH_NUMBER FROM $TABLE_PETTY_CASH 
                WHERE $COL_PETTY_CASH_IS_DELETED = 0
                ORDER BY $COL_PETTY_CASH_ID DESC
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, null)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val pettyCashNumber = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                        pettyCashNumber?.let { number -> pettyCashNumbers.add(number) }
                    } while (it.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting petty cash numbers: ${e.message}")
        } finally {
            db.close()
        }
        
        return pettyCashNumbers
    }

    fun getPettyCashByDateRange(startDate: String, endDate: String): List<PettyCash> {
        val pettyCashList = mutableListOf<PettyCash>()
        val db = this.readableDatabase

        try {
            val sql = """
                SELECT * FROM $TABLE_PETTY_CASH 
                WHERE $COL_PETTY_CASH_IS_DELETED = 0
                AND (
                    date(
                        substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 1, 2)
                    ) >= date('${startDate.substring(6, 10)}-${startDate.substring(3, 5)}-${startDate.substring(0, 2)}')
                    AND 
                    date(
                        substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 1, 2)
                    ) <= date('${endDate.substring(6, 10)}-${endDate.substring(3, 5)}-${endDate.substring(0, 2)}')
                )
                ORDER BY 
                datetime(
                    substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                    substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                    substr($COL_PETTY_CASH_DATE, 1, 2) || ' ' ||
                    COALESCE(substr($COL_PETTY_CASH_DATE, 12), '00:00:00')
                ) DESC
            """.trimIndent()

            val cursor = db.rawQuery(sql, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                        val pettyCashNo = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                        val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                        val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                        val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                        val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                        val accountId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                        val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                        val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                        val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                        val trucks = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                        val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                        val supportingDocumentId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                        val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                        val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                        var ownerObject: Owner? = null
                        var truckList: List<Truck>? = null

                        // Fetch related data
                        if (ownerCode != null) {
                            ownerObject = getOwnerByCode(ownerCode)
                        }
                        val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                        val transactorObject = getTransactorById(transactorId)
                        val accountObject = getAccountById(accountId)
                        val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                        // Split trucks and get their objects
                        if (trucks != null) {
                            truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                        }

                        // Create PettyCash object
                        val pettyCash = PettyCash(
                            id = id,
                            pettyCashNumber = pettyCashNo,
                            amount = amount,
                            date = date,
                            description = description,
                            transactor = transactorObject,
                            account = accountObject,
                            paymentMode = paymentMode,
                            owner = ownerObject,
                            mpesaTransaction = mpesaTransactionObject,
                            trucks = truckList?.toMutableList(),
                            signature = signature,
                            supportingDocument = supportingDocument,
                            user = User(1, user, UserTypes(1, "admin")),
                            isDeleted = isDeleted
                        )
                        pettyCashList.add(pettyCash)
                    } while (it.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting petty cash by date range: ${e.message}")
        } finally {
            db.close()
        }

        return pettyCashList
    }

    fun getPettyCashByDateRangeAndOwner(startDate: String, endDate: String, ownerId: String): List<PettyCash> {
        val pettyCashList = mutableListOf<PettyCash>()
        val db = this.readableDatabase

        try {
            val sql = """
                SELECT * FROM $TABLE_PETTY_CASH 
                WHERE $COL_PETTY_CASH_IS_DELETED = 0
                AND $COL_PETTY_CASH_OWNER = ?
                AND (
                    date(
                        substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 1, 2)
                    ) >= date('${startDate.substring(6, 10)}-${startDate.substring(3, 5)}-${startDate.substring(0, 2)}')
                    AND 
                    date(
                        substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                        substr($COL_PETTY_CASH_DATE, 1, 2)
                    ) <= date('${endDate.substring(6, 10)}-${endDate.substring(3, 5)}-${endDate.substring(0, 2)}')
                )
                ORDER BY 
                datetime(
                    substr($COL_PETTY_CASH_DATE, 7, 4) || '-' ||
                    substr($COL_PETTY_CASH_DATE, 4, 2) || '-' ||
                    substr($COL_PETTY_CASH_DATE, 1, 2) || ' ' ||
                    COALESCE(substr($COL_PETTY_CASH_DATE, 12), '00:00:00')
                ) DESC
            """.trimIndent()

            val cursor = db.rawQuery(sql, arrayOf(ownerId))

            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val id = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                        val pettyCashNo = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                        val amount = it.getDouble(it.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                        val date = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                        val description = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                        val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                        val accountId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                        val paymentMode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                        val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                        val mpesaTransactionCode = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                        val trucks = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                        val signature = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                        val supportingDocumentId = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                        val user = it.getString(it.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                        val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1

                        var ownerObject: Owner? = null
                        var truckList: List<Truck>? = null

                        // Fetch related data
                        if (ownerCode != null) {
                            ownerObject = getOwnerByCode(ownerCode)
                        }
                        val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                        val transactorObject = getTransactorById(transactorId)
                        val accountObject = getAccountById(accountId)
                        val supportingDocument = getSupportingDocumentById(supportingDocumentId)

                        // Split trucks and get their objects
                        if (trucks != null) {
                            truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                        }

                        // Create PettyCash object
                        val pettyCash = PettyCash(
                            id = id,
                            pettyCashNumber = pettyCashNo,
                            amount = amount,
                            date = date,
                            description = description,
                            transactor = transactorObject,
                            account = accountObject,
                            paymentMode = paymentMode,
                            owner = ownerObject,
                            mpesaTransaction = mpesaTransactionObject,
                            trucks = truckList?.toMutableList(),
                            signature = signature,
                            supportingDocument = supportingDocument,
                            user = User(1, user, UserTypes(1, "admin")),
                            isDeleted = isDeleted
                        )
                        pettyCashList.add(pettyCash)
                    } while (it.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting petty cash by date range and owner: ${e.message}")
        } finally {
            db.close()
        }

        return pettyCashList
    }

    /**
     * Get all petty cash entries with pagination and filtering
     */
    fun getAllPettyCash(
        page: Int = 1,
        pageSize: Int = 20,
        sortBy: String? = "Date",
        dateFilter: String? = "Any Time",
        paymentModes: List<String> = emptyList(),
        customStartDate: String? = null,
        customEndDate: String? = null,
        filterUnconverted: Boolean = false
    ): List<PettyCash> {
        val pettyCashList = mutableListOf<PettyCash>()
        val db = this.readableDatabase
        
        // Calculate offset for pagination
        val offset = (page - 1) * pageSize
        
        try {
            // Build the base query
            var query = """
                SELECT * FROM $TABLE_PETTY_CASH 
                WHERE $COL_PETTY_CASH_IS_DELETED = 0
            """
            
            // Add filter for unconverted petty cash if needed
            if (filterUnconverted) {
                query += " AND ($COL_PETTY_CASH_NUMBER IS NULL OR $COL_PETTY_CASH_NUMBER = '')"
            }
            
            // Add date filter if applicable
            if (dateFilter != "Any Time" || (customStartDate != null && customEndDate != null)) {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                
                val (startDate, endDate) = when (dateFilter) {
                    "Today" -> {
                        val today = dateFormat.format(calendar.time)
                        Pair(today, today)
                    }
                    "This Week" -> {
                        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                        calendar.add(Calendar.DAY_OF_YEAR, -currentDay + 1) // Move to beginning of week (Sunday)
                        val startDate = dateFormat.format(calendar.time)
                        calendar.add(Calendar.DAY_OF_YEAR, 6) // Move to end of week (Saturday)
                        val endDate = dateFormat.format(calendar.time)
                        Pair(startDate, endDate)
                    }
                    "This Month" -> {
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        val startDate = dateFormat.format(calendar.time)
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                        val endDate = dateFormat.format(calendar.time)
                        Pair(startDate, endDate)
                    }
                    "Last Month" -> {
                        calendar.add(Calendar.MONTH, -1)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        val startDate = dateFormat.format(calendar.time)
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                        val endDate = dateFormat.format(calendar.time)
                        Pair(startDate, endDate)
                    }
                    "Last Six Months" -> {
                        calendar.add(Calendar.MONTH, -6)
                        val startDate = dateFormat.format(calendar.time)
                        calendar.add(Calendar.MONTH, 6)
                        val endDate = dateFormat.format(calendar.time)
                        Pair(startDate, endDate)
                    }
                    "Custom Range" -> {
                        Pair(customStartDate ?: "", customEndDate ?: "")
                    }
                    else -> Pair("", "")
                }
                
                if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                    query += " AND $COL_PETTY_CASH_DATE BETWEEN '$startDate' AND '$endDate'"
                }
            }
            
            // Add payment mode filter if applicable
            if (paymentModes.isNotEmpty()) {
                val paymentModesList = paymentModes.joinToString("','", "'", "'")
                query += " AND $COL_PETTY_CASH_PAYMENT_MODE IN ($paymentModesList)"
            }
            
            // Add sorting
            query += when (sortBy) {
                "Date" -> " ORDER BY $COL_PETTY_CASH_DATE DESC"
                "Amount" -> " ORDER BY $COL_PETTY_CASH_AMOUNT DESC"
                "Petty Cash Number" -> " ORDER BY $COL_PETTY_CASH_NUMBER ASC"
                else -> " ORDER BY $COL_PETTY_CASH_DATE DESC"
            }
            
            // Add pagination
            query += " LIMIT $pageSize OFFSET $offset"
            
            // Execute the query
            val cursor = db.rawQuery(query, null)
            
            cursor.use { c ->
                if (c.moveToFirst()) {
                    do {
                        val id = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_ID))
                        val pettyCashNo = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_NUMBER))
                        val amount = c.getDouble(c.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                        val date = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                        val description = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_DESCRIPTION))
                        val transactorId = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_TRANSACTOR))
                        val accountId = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_ACCOUNT))
                        val paymentMode = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_PAYMENT_MODE))
                        val ownerCode = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                        val mpesaTransactionCode = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_MPESA_TRANSACTION))
                        val trucks = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                        val signature = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_SIGNATURE))
                        val supportingDocumentId = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_SUPPORTING_DOCUMENT))
                        val user = c.getString(c.getColumnIndexOrThrow(COL_PETTY_CASH_USER))
                        val isDeleted = c.getInt(c.getColumnIndexOrThrow(COL_PETTY_CASH_IS_DELETED)) == 1
                        
                        // Fetch related data
                        val ownerObject = getOwnerByCode(ownerCode)
                        val mpesaTransactionObject = mpesaTransactionCode?.let { getMpesaTransactionByCode(it) }
                        val transactorObject = getTransactorById(transactorId)
                        val accountObject = getAccountById(accountId)
                        val supportingDocument = getSupportingDocumentById(supportingDocumentId)
                        
                        // Split trucks and get their objects
                        val truckList = trucks.split(", ").mapNotNull { getTruckByTruckNumber(it) }
                        
                        // Create PettyCash object
                        val pettyCash = PettyCash(
                            id = id,
                            pettyCashNumber = pettyCashNo,
                            amount = amount,
                            date = date,
                            description = description,
                            transactor = transactorObject,
                            account = accountObject,
                            paymentMode = paymentMode,
                            owner = ownerObject,
                            mpesaTransaction = mpesaTransactionObject,
                            trucks = truckList.toMutableList(),
                            signature = signature,
                            supportingDocument = supportingDocument,
                            user = User(1, user, UserTypes(1, "admin")),
                            isDeleted = isDeleted
                        )
                        
                        pettyCashList.add(pettyCash)
                    } while (c.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting petty cash: ${e.message}", e)
        }
        
        return pettyCashList
    }

    // Get count of petty cash entries that have been converted (have a petty cash number)
    fun getConvertedPettyCashCount(): Int {
        val db = this.readableDatabase
        var count = 0
        
        try {
            val query = "SELECT COUNT(*) FROM $TABLE_PETTY_CASH WHERE $COL_PETTY_CASH_NUMBER IS NOT NULL AND $COL_PETTY_CASH_NUMBER != '' AND $COL_PETTY_CASH_IS_DELETED = 0"
            val cursor = db.rawQuery(query, null)
            
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting converted petty cash count", e)
        }
        
        return count
    }
    
    // Get all trucks with their total expenses
    fun getTruckExpenses(): List<Pair<String, Double>> {
        val db = this.readableDatabase
        val truckExpenses = mutableListOf<Pair<String, Double>>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_TRUCKS, SUM($COL_PETTY_CASH_AMOUNT) as total_expense
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_TRUCKS IS NOT NULL AND $COL_PETTY_CASH_TRUCKS != '' AND $COL_PETTY_CASH_IS_DELETED = 0
                GROUP BY $COL_PETTY_CASH_TRUCKS
                ORDER BY total_expense ASC
            """.trimIndent()
            
            val cursor = db.rawQuery(query, null)
            
            while (cursor.moveToNext()) {
                val truckNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("total_expense"))
                truckExpenses.add(Pair(truckNo, totalExpense))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting truck expenses", e)
        }
        
        return truckExpenses
    }
    
    // Get spending per company for a date range
    fun getSpendingPerCompanyForDateRange(startDate: String, endDate: String): List<Triple<String, String, Double>> {
        val db = this.readableDatabase
        val spendingData = mutableListOf<Triple<String, String, Double>>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_OWNER, $COL_PETTY_CASH_DATE, SUM($COL_PETTY_CASH_AMOUNT) as daily_amount
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_DATE BETWEEN ? AND ? AND $COL_PETTY_CASH_IS_DELETED = 0
                GROUP BY $COL_PETTY_CASH_OWNER, $COL_PETTY_CASH_DATE
                ORDER BY $COL_PETTY_CASH_OWNER, $COL_PETTY_CASH_DATE
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(startDate, endDate))
            
            while (cursor.moveToNext()) {
                val ownerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow("daily_amount"))
                spendingData.add(Triple(ownerCode, date, amount))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting spending per company for date range", e)
        }
        
        return spendingData
    }

    // Get spending per company for a date range with non-null petty cash numbers only
    fun getSpendingPerCompanyForDateRangeWithPettyCashNumber(startDate: String, endDate: String): List<Triple<String, String, Double>> {
        val db = this.readableDatabase
        val spendingData = mutableListOf<Triple<String, String, Double>>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_OWNER, $COL_PETTY_CASH_DATE, SUM($COL_PETTY_CASH_AMOUNT) as daily_amount
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_DATE BETWEEN ? AND ? 
                AND $COL_PETTY_CASH_IS_DELETED = 0
                AND $COL_PETTY_CASH_NUMBER IS NOT NULL
                AND $COL_PETTY_CASH_NUMBER != ''
                GROUP BY $COL_PETTY_CASH_OWNER, $COL_PETTY_CASH_DATE
                ORDER BY $COL_PETTY_CASH_OWNER, $COL_PETTY_CASH_DATE
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(startDate, endDate))
            
            while (cursor.moveToNext()) {
                val ownerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_OWNER))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_DATE))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow("daily_amount"))
                spendingData.add(Triple(ownerCode, date, amount))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting spending per company for date range with petty cash number", e)
        }
        
        return spendingData
    }

    // Get owner name by owner code
    fun getOwnerNameByCode(ownerCode: String?): String {
        if (ownerCode == null) return "Unknown Owner"
        
        val db = this.readableDatabase
        var ownerName = ""
        
        try {
            val query = "SELECT $COL_OWNER_NAME FROM $TABLE_OWNERS WHERE $COL_OWNER_CODE = ? AND $COL_IS_OWNER_DELETED = 0"
            val cursor = db.rawQuery(query, arrayOf(ownerCode))
            
            if (cursor.moveToFirst()) {
                ownerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_NAME))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting owner name by code", e)
        }
        
        return ownerName
    }

    // Get truck expenses filtered by owner
    fun getTruckExpensesByOwner(ownerCode: String): List<Pair<String, Double>> {
        val db = this.readableDatabase
        val truckExpenses = mutableListOf<Pair<String, Double>>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_TRUCKS, SUM($COL_PETTY_CASH_AMOUNT) as total_expense
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_TRUCKS IS NOT NULL 
                AND $COL_PETTY_CASH_TRUCKS != '' 
                AND $COL_PETTY_CASH_OWNER = ?
                AND $COL_PETTY_CASH_IS_DELETED = 0
                GROUP BY $COL_PETTY_CASH_TRUCKS
                ORDER BY total_expense ASC
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(ownerCode))
            
            while (cursor.moveToNext()) {
                val truckNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("total_expense"))
                truckExpenses.add(Pair(truckNo, totalExpense))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting truck expenses by owner", e)
        }
        
        return truckExpenses
    }

    // Get truck expenses with support for multiple trucks in a single petty cash entry
    fun getTruckExpensesWithMultipleTrucks(): List<Pair<String, Double>> {
        val db = this.readableDatabase
        val truckExpensesMap = mutableMapOf<String, Double>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_TRUCKS, $COL_PETTY_CASH_AMOUNT
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_TRUCKS IS NOT NULL 
                AND $COL_PETTY_CASH_TRUCKS != '' 
                AND $COL_PETTY_CASH_IS_DELETED = 0
            """.trimIndent()
            
            val cursor = db.rawQuery(query, null)
            
            while (cursor.moveToNext()) {
                val trucksString = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                
                // Split the trucks string by comma and trim each truck number
                val trucks = trucksString.split(",").map { it.trim() }
                
                // Divide the amount equally among all trucks
                val amountPerTruck = amount / trucks.size
                
                // Add the divided amount to each truck's total
                for (truck in trucks) {
                    if (truck.isNotEmpty()) {
                        truckExpensesMap[truck] = truckExpensesMap.getOrDefault(truck, 0.0) + amountPerTruck
                    }
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting truck expenses with multiple trucks", e)
        }
        
        // Convert the map to a list of pairs and sort by expense (ascending)
        return truckExpensesMap.toList().sortedBy { it.second }
    }

    // Get truck expenses by owner with support for multiple trucks
    fun getTruckExpensesByOwnerWithMultipleTrucks(ownerCode: String): List<Pair<String, Double>> {
        val db = this.readableDatabase
        val truckExpensesMap = mutableMapOf<String, Double>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_TRUCKS, $COL_PETTY_CASH_AMOUNT
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_TRUCKS IS NOT NULL 
                AND $COL_PETTY_CASH_TRUCKS != '' 
                AND $COL_PETTY_CASH_OWNER = ?
                AND $COL_PETTY_CASH_IS_DELETED = 0
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(ownerCode))
            
            while (cursor.moveToNext()) {
                val trucksString = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_AMOUNT))
                
                // Split the trucks string by comma and trim each truck number
                val trucks = trucksString.split(",").map { it.trim() }
                
                // Divide the amount equally among all trucks
                val amountPerTruck = amount / trucks.size
                
                // Add the divided amount to each truck's total
                for (truck in trucks) {
                    if (truck.isNotEmpty()) {
                        truckExpensesMap[truck] = truckExpensesMap.getOrDefault(truck, 0.0) + amountPerTruck
                    }
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting truck expenses by owner with multiple trucks", e)
        }
        
        // Convert the map to a list of pairs and sort by expense (ascending)
        return truckExpensesMap.toList().sortedBy { it.second }
    }

    // Get truck expenses with pagination
    fun getTruckExpensesPaginated(limit: Int, offset: Int): List<Pair<String, Double>> {
        val db = this.readableDatabase
        val truckExpenses = mutableListOf<Pair<String, Double>>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_TRUCKS, SUM($COL_PETTY_CASH_AMOUNT) as total_expense
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_TRUCKS IS NOT NULL AND $COL_PETTY_CASH_TRUCKS != '' AND $COL_PETTY_CASH_IS_DELETED = 0
                GROUP BY $COL_PETTY_CASH_TRUCKS
                ORDER BY total_expense ASC
                LIMIT ? OFFSET ?
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(limit.toString(), offset.toString()))
            
            while (cursor.moveToNext()) {
                val truckNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("total_expense"))
                truckExpenses.add(Pair(truckNo, totalExpense))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated truck expenses", e)
        }
        
        return truckExpenses
    }
    
    // Get truck expenses filtered by owner with pagination
    fun getTruckExpensesByOwnerPaginated(ownerCode: String, limit: Int, offset: Int): List<Pair<String, Double>> {
        val db = this.readableDatabase
        val truckExpenses = mutableListOf<Pair<String, Double>>()
        
        try {
            val query = """
                SELECT $COL_PETTY_CASH_TRUCKS, SUM($COL_PETTY_CASH_AMOUNT) as total_expense
                FROM $TABLE_PETTY_CASH
                WHERE $COL_PETTY_CASH_TRUCKS IS NOT NULL 
                AND $COL_PETTY_CASH_TRUCKS != '' 
                AND $COL_PETTY_CASH_OWNER = ?
                AND $COL_PETTY_CASH_IS_DELETED = 0
                GROUP BY $COL_PETTY_CASH_TRUCKS
                ORDER BY total_expense ASC
                LIMIT ? OFFSET ?
            """.trimIndent()
            
            val cursor = db.rawQuery(query, arrayOf(ownerCode, limit.toString(), offset.toString()))
            
            while (cursor.moveToNext()) {
                val truckNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_PETTY_CASH_TRUCKS))
                val totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("total_expense"))
                truckExpenses.add(Pair(truckNo, totalExpense))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated truck expenses by owner", e)
        }
        
        return truckExpenses
    }

// ... existing code ...

    /**
     * Gets a page of unconverted petty cash entries (those with null or empty petty cash number)
     */
    // ... existing code ...
    /**
     * Gets a page of unconverted petty cash entries (those with null or empty petty cash number)
     * Using SQL JOINs to load related data in a single query
     */
    fun getUnconvertedPettyCash(
        page: Int,
        pageSize: Int,
        sortBy: String? = "Date",
        dateFilter: String? = "This Month",
        paymentModes: List<String>? = null,
        customStartDate: String? = null,
        customEndDate: String? = null
    ): List<PettyCash> {
        val offset = (page - 1) * pageSize
        val pettyCashList = mutableListOf<PettyCash>()
        val db = this.readableDatabase

        try {
            val queryBuilder = StringBuilder()
            // Use JOIN to get transactor data in a single query
            queryBuilder.append("SELECT p.*, t.* FROM $TABLE_PETTY_CASH p ")
            queryBuilder.append("LEFT JOIN $TABLE_TRANSACTORS t ON p.$COL_PETTY_CASH_TRANSACTOR = t.$COL_TRANSACTOR_ID ")
            queryBuilder.append("WHERE p.$COL_PETTY_CASH_IS_DELETED = 0 ")

            // Filter for unconverted petty cash (null or empty petty cash number)
            queryBuilder.append("AND (p.$COL_PETTY_CASH_NUMBER IS NULL OR p.$COL_PETTY_CASH_NUMBER = '') ")

            // Add date filtering if needed
            when (dateFilter) {
                "Today" -> {
                    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    queryBuilder.append("AND p.$COL_PETTY_CASH_DATE LIKE '$today%' ")
                }
                "This Week" -> {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    val startDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                    calendar.add(Calendar.DAY_OF_WEEK, 6)
                    val endDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                    queryBuilder.append("AND p.$COL_PETTY_CASH_DATE BETWEEN '$startDate' AND '$endDate' ")
                }
                "This Month" -> {
                    val calendar = Calendar.getInstance()
                    val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
                    val currentYear = calendar.get(Calendar.YEAR).toString()

                    queryBuilder.append("AND (SUBSTR(p.$COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND SUBSTR(p.$COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth') ")
                }
                "Last Month" -> {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MONTH, -1)
                    val lastMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
                    val year = calendar.get(Calendar.YEAR).toString()

                    queryBuilder.append("AND (SUBSTR(p.$COL_PETTY_CASH_DATE, 7, 4) = '$year' AND SUBSTR(p.$COL_PETTY_CASH_DATE, 4, 2) = '$lastMonth') ")
                }
                "Last Six Months" -> {
                    val calendar = Calendar.getInstance()
                    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                    calendar.add(Calendar.MONTH, -6)
                    val sixMonthsAgo = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                    queryBuilder.append("AND p.$COL_PETTY_CASH_DATE BETWEEN '$sixMonthsAgo' AND '$currentDate' ")
                }
                "Custom Range" -> {
                    if (customStartDate != null && customEndDate != null) {
                        queryBuilder.append("AND p.$COL_PETTY_CASH_DATE BETWEEN '$customStartDate' AND '$customEndDate' ")
                    }
                }
            }

            // Add payment mode filter if specified
            if (!paymentModes.isNullOrEmpty()) {
                queryBuilder.append("AND (")
                paymentModes.forEachIndexed { index, paymentMode ->
                    if (index > 0) queryBuilder.append(" OR ")
                    queryBuilder.append("p.$COL_PETTY_CASH_PAYMENT_MODE = '$paymentMode'")
                }
                queryBuilder.append(") ")
            }

            // Add sorting
            queryBuilder.append("ORDER BY ")
            when (sortBy) {
                "Amount" -> queryBuilder.append("p.$COL_PETTY_CASH_AMOUNT DESC ")
                "Transactor" -> queryBuilder.append("t.$COL_TRANSACTOR_NAME ") // Sort by transactor name directly
                else -> queryBuilder.append("p.$COL_PETTY_CASH_DATE DESC ")
            }

            // Add pagination
            queryBuilder.append("LIMIT $pageSize OFFSET $offset")

            val cursor = db.rawQuery(queryBuilder.toString(), null)

            cursor.use {
                while (it.moveToNext()) {
                    val pettyCash = getPettyCashFromCursor(it)

                    // Create transactor object directly from the joined data
                    if (pettyCash != null && it.getColumnIndex(COL_TRANSACTOR_ID) != -1) {
                        try {
                            val transactorId = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID))
                            if (transactorId > 0) {
                                val transactorName = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_NAME))
                                val phoneNo = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_PHONE_NO))
                                val address = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_ADDRESS))
                                val transactorType = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_TYPE))
                                val idCardNo = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_ID_CARD_NO))
                                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_TRANSACTOR_DELETED)) > 0
                                val isImported = it.getInt(it.getColumnIndexOrThrow(COL_IS_IMPORTED)) > 0
                                val logoPath = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_LOGO_PATH))
                                val kraPinString = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_KRA_PIN))
                                val interactions = it.getInt(it.getColumnIndexOrThrow(COL_TRANSACTOR_INTERACTIONS))
                                val avatarColor = it.getString(it.getColumnIndexOrThrow(COL_TRANSACTOR_AVATAR_COLOR))

                                // Create transactor directly from cursor data
                                pettyCash.transactor = Transactor(
                                    id = transactorId,
                                    name = transactorName,
                                    phoneNumber = phoneNo,
                                    idCard = idCardNo,
                                    address = address,
                                    transactorType = transactorType,
                                    transactorProfilePicturePath = logoPath,
                                    interactions = interactions,
                                    kraPin = kraPinString,
                                    isDeleted = isDeleted,
                                    isImported = isImported,
                                    avatarColor = avatarColor
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("DbHelper", "Error creating transactor from cursor: ${e.message}")
                        }
                    }

                    if (pettyCash != null) {
                        pettyCashList.add(pettyCash)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error getting unconverted petty cash: ${e.message}")
            e.printStackTrace()
        }

        return pettyCashList
    }
    /**
     * Searches for unconverted petty cash entries based on a query
     */
    fun searchUnconvertedPettyCash(query: String): List<PettyCash> {
        val pettyCashList = mutableListOf<PettyCash>()
        val db = this.readableDatabase

        val likeQuery = "%$query%"

        try {
            val queryBuilder = StringBuilder()
            // Get all fields from both petty cash and transactor tables
            queryBuilder.append("SELECT p.*, ")
            queryBuilder.append("t.$COL_TRANSACTOR_ID as t_id, ")
            queryBuilder.append("t.$COL_TRANSACTOR_NAME as t_name, ")
            queryBuilder.append("t.$COL_TRANSACTOR_PHONE_NO as t_phone, ")
            queryBuilder.append("t.$COL_TRANSACTOR_ADDRESS as t_address, ")
            queryBuilder.append("t.$COL_TRANSACTOR_TYPE as t_type, ")
            queryBuilder.append("t.$COL_TRANSACTOR_ID_CARD_NO as t_id_card, ")
            queryBuilder.append("t.$COL_IS_TRANSACTOR_DELETED as t_is_deleted, ")
            queryBuilder.append("t.$COL_IS_IMPORTED as t_is_imported, ")
            queryBuilder.append("t.$COL_TRANSACTOR_LOGO_PATH as t_logo_path, ")
            queryBuilder.append("t.$COL_TRANSACTOR_KRA_PIN as t_kra_pin, ")
            queryBuilder.append("t.$COL_TRANSACTOR_INTERACTIONS as t_interactions, ")
            queryBuilder.append("t.$COL_TRANSACTOR_AVATAR_COLOR as t_avatar_color ")
            queryBuilder.append("FROM $TABLE_PETTY_CASH p ")
            queryBuilder.append("LEFT JOIN $TABLE_TRANSACTORS t ON p.$COL_PETTY_CASH_TRANSACTOR = t.$COL_TRANSACTOR_ID ")
            queryBuilder.append("WHERE p.$COL_PETTY_CASH_IS_DELETED = 0 ")

            // Filter for unconverted petty cash (null or empty petty cash number)
            queryBuilder.append("AND (p.$COL_PETTY_CASH_NUMBER IS NULL OR p.$COL_PETTY_CASH_NUMBER = '') ")

            // Add current month filter
            val calendar = Calendar.getInstance()
            val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val currentYear = calendar.get(Calendar.YEAR).toString()
            queryBuilder.append("AND (SUBSTR(p.$COL_PETTY_CASH_DATE, 7, 4) = '$currentYear' AND SUBSTR(p.$COL_PETTY_CASH_DATE, 4, 2) = '$currentMonth') ")

            // Search criteria
            queryBuilder.append("AND (")
            queryBuilder.append("p.$COL_PETTY_CASH_DATE LIKE ? OR ")
            queryBuilder.append("p.$COL_PETTY_CASH_AMOUNT LIKE ? OR ")
            queryBuilder.append("p.$COL_PETTY_CASH_DESCRIPTION LIKE ? OR ")
            queryBuilder.append("p.$COL_PETTY_CASH_PAYMENT_MODE LIKE ? OR ")
            queryBuilder.append("t.$COL_TRANSACTOR_NAME LIKE ? OR ")
            queryBuilder.append("p.$COL_PETTY_CASH_TRUCKS LIKE ?")
            queryBuilder.append(") ")

            // Order by date descending
            queryBuilder.append("ORDER BY p.$COL_PETTY_CASH_DATE DESC ")

            // Limit results to 100
            queryBuilder.append("LIMIT 100")

            val args = arrayOf(likeQuery, likeQuery, likeQuery, likeQuery, likeQuery, likeQuery)
            val cursor = db.rawQuery(queryBuilder.toString(), args)

            cursor.use {
                while (it.moveToNext()) {
                    val pettyCash = getPettyCashFromCursor(it)

                    // Create transactor object directly from the joined data
                    if (pettyCash != null && it.getColumnIndex("t_id") != -1) {
                        try {
                            if (!it.isNull(it.getColumnIndex("t_id"))) {
                                val transactorId = it.getInt(it.getColumnIndex("t_id"))
                                val transactorName = it.getString(it.getColumnIndex("t_name"))
                                val phoneNo = it.getString(it.getColumnIndex("t_phone"))
                                val address = it.getString(it.getColumnIndex("t_address"))
                                val transactorType = it.getString(it.getColumnIndex("t_type"))
                                val idCardNo = it.getInt(it.getColumnIndex("t_id_card"))
                                val isDeleted = it.getInt(it.getColumnIndex("t_is_deleted")) > 0
                                val isImported = it.getInt(it.getColumnIndex("t_is_imported")) > 0
                                val logoPath = it.getString(it.getColumnIndex("t_logo_path"))
                                val kraPinString = it.getString(it.getColumnIndex("t_kra_pin"))
                                val interactions = it.getInt(it.getColumnIndex("t_interactions"))
                                val avatarColor = it.getString(it.getColumnIndex("t_avatar_color"))

                                // Create transactor directly from cursor data
                                pettyCash.transactor = Transactor(
                                    id = transactorId,
                                    name = transactorName,
                                    phoneNumber = phoneNo,
                                    idCard = idCardNo,
                                    address = address,
                                    transactorType = transactorType,
                                    transactorProfilePicturePath = logoPath,
                                    interactions = interactions,
                                    kraPin = kraPinString,
                                    isDeleted = isDeleted,
                                    isImported = isImported,
                                    avatarColor = avatarColor
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("DbHelper", "Error creating transactor from cursor: ${e.message}")
                        }
                    }

                    if (pettyCash != null) {
                        pettyCashList.add(pettyCash)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DbHelper", "Error searching unconverted petty cash: ${e.message}")
            e.printStackTrace()
        }

        return pettyCashList
    }



}

