package com.example.pettysms

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import com.example.pettysms.databinding.FragmentMpesaBinding
import com.google.android.material.snackbar.Snackbar
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
interface RefreshRecyclerViewCallback {
    fun onRefresh()
}
class MpesaFragment : Fragment(), RefreshRecyclerViewCallback  {

    private var _binding: FragmentMpesaBinding? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var currency_label: TextView
    private lateinit var balance_text: TextView
    private lateinit var mpesa_balance_label: TextView
    private lateinit var mpesa_sync_label: TextView
    private lateinit var handler: Handler
    private lateinit var verticalShrinkAnimation: Animation
    private lateinit var fadeInAnimation: Animation
    private lateinit var updateTextBox : TextView
    private lateinit var netSpendTextBox: TextView
    private lateinit var viewAlLink: TextView
    private lateinit var circularProgressDrawable: CircularProgressIndicator
    private lateinit var bigResult: MpesaTransaction.Companion.MpesaTransactionResult
    private var rejectedSmsList = mutableListOf<MutableList<String>>()
    private val dataViewModel: DataViewModel by activityViewModels()


    private var all_mpesa_transactions= mutableListOf<MpesaTransaction>()


    var db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
    var db = db_helper?.writableDatabase






    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)

        _binding = FragmentMpesaBinding.inflate(inflater, container, false)
        progressBar = binding.progressBar2

        mpesa_balance_label = binding.root.findViewById(R.id.mpesa_balance_label)
        mpesa_balance_label.text = "Balance: "
        currency_label = binding.root.findViewById(R.id.currency_label)
        currency_label.text = "Ksh."
        balance_text = binding.root.findViewById(R.id.balance_text)
        balance_text.text = "0.00"
        mpesa_sync_label = binding.root.findViewById(R.id.sync_progress_label)
        mpesa_sync_label.text = "Sync Progress:"
        updateTextBox = binding.transactionsThisMonth
        netSpendTextBox= binding.netSpendThisMonth
        circularProgressDrawable = binding.circularProgress
        viewAlLink = binding.viewAllLink
        //constraintLayout.loadSkeleton { balance_text }
        //mpesa_balance_label.loadSkeleton()
        /*balance_text.loadSkeleton()
        currency_label.loadSkeleton()
        mpesa_sync_label.loadSkeleton()
        mpesa_balance_label.loadSkeleton()
        mpesa_sync_label.loadSkeleton()
        updateTextBox.loadSkeleton()
        netSpendTextBox.loadSkeleton()*/

        CallbackSingleton.refreshCallback = this






        val actionbar = binding.root.findViewById<Toolbar>(R.id.toolbar)
        actionbar.setTitle(R.string.mpesa_tool)
        (activity as AppCompatActivity).setSupportActionBar(actionbar)

        verticalShrinkAnimation = AnimationUtils.loadAnimation(activity, R.anim.vertical_shrink)
        fadeInAnimation = AnimationUtils.loadAnimation(activity, R.anim.fade_in)

        return binding.root

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        //(activity as AppCompatActivity).invalidateOptionsMenu()
        (activity as AppCompatActivity).menuInflater.inflate(R.menu.menu_mpesa, menu)
        var toolbar = binding.toolbar
        var menu = toolbar.menu
        //var filter = menu.findItem(R.id.filter)
        //val month = SimpleDateFormat("MMMM").format(Calendar.getInstance().time)
        //filter.setTitle(month)
        //super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
       /*if (item.itemId == R.id.filter){
            Toast.makeText(activity?.baseContext, "Clicked", Toast.LENGTH_SHORT).show()
        }
        return when (item.itemId) {
            R.id.filter -> true
            else -> super.onOptionsItemSelected(item)
        }*/
        return true
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var msg_str: ArrayList<MutableList<String>>? = null
        var tableExists = false

        // Initialize the handler
        handler = Handler(Looper.getMainLooper())

        db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
        db = db_helper?.writableDatabase


        /*if (db != null) {
            DbHelper.dropAllTables(db!!)
            activity?.finishAffinity()
        }*/

        if (db != null) {
            if(db!!.isOpen){
                tableExists = db?.let { doesTableExist("transactions", it) }!!

            }
        }


        if (tableExists == true) {
            if(db_helper?.let { isTableEmpty("transactions", it) } == true){
                msg_str = getAllSmsFromProvider()

                all_mpesa_transactions = retrieveArrayFromViewModel() ?: mutableListOf()

                if(all_mpesa_transactions.isNullOrEmpty()){
                    println("Start conversion")
                    all_mpesa_transactions = startConversionTask(msg_str)
                    saveArrayToViewModel(all_mpesa_transactions)
                }

                Toast.makeText(activity?.baseContext, "Table Exists but Empty", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(activity?.baseContext, "Table Exists", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.INVISIBLE

                all_mpesa_transactions = retrieveArrayFromViewModel() ?: mutableListOf()

                if(all_mpesa_transactions.isNullOrEmpty() && all_mpesa_transactions.isNullOrEmpty()) {
                    println("check DB")
                    all_mpesa_transactions = db_helper?.getThisMonthMpesaTransactions()!!
                    saveArrayToViewModel(all_mpesa_transactions)
                }



                updateTransactionThisMonth(all_mpesa_transactions)



            }

        } else {
            Toast.makeText(activity?.baseContext, "Table Does not Exist", Toast.LENGTH_SHORT).show()
            msg_str = getAllSmsFromProvider()
            all_mpesa_transactions=startConversionTask(msg_str)
            //balance_text.text = all_mpesa_transactions.first().mpesa_balance.toString()
            if (db?.isOpen == true) {
                db_helper?.createInitialTables(db!!)
            }
            else{
                db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
                db = db_helper?.writableDatabase
                db_helper?.createInitialTables(db!!)
            }
        }

    }

    private fun saveArrayToViewModel(array: MutableList<MpesaTransaction>) {
        dataViewModel?.dataArray = array
    }
    fun retrieveArrayFromViewModel() : MutableList<MpesaTransaction>? {
        // Accessing the member property to retrieve the data
        return dataViewModel?.dataArray
    }

    private fun updateTransactionThisMonth(all_mpesa_transactions: MutableList<MpesaTransaction>) {
        updateTextBox = binding.transactionsThisMonth
        netSpendTextBox= binding.netSpendThisMonth
        val no_transactions_textbox: TextView = binding.noTransactionsMessage

        if (all_mpesa_transactions.isEmpty()){
            no_transactions_textbox.visibility = View.VISIBLE
        }else{
            no_transactions_textbox.visibility = View.INVISIBLE
        }

        viewAlLink.setOnClickListener{
            Toast.makeText(
                activity?.baseContext,
                "Just need to setup an intent to the new activity",
                Toast.LENGTH_SHORT
            ).show()
            // Create an Intent to start SecondActivity
            val intent = Intent(activity, ViewAllTransactionsActivity::class.java)

            // Optionally, you can pass data to the second activity using extras
            intent.putExtra("key", "value")

            // Start the second activity
            startActivity(intent)

        }

        // Get the current month and year programmatically
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val currentDate = Calendar.getInstance().time


        // Define the date format
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

        // Filter transactions for the current month and year
        val transactions_today = all_mpesa_transactions.filter { transaction ->
            // Assuming transactionDate is a String property in the format "dd/MM/yyyy HH:mm:ss"
            isSameDay(transaction.transaction_date.toString(), currentDate)
        }.toMutableList()

        if(!transactions_today.isNullOrEmpty()){
        }

        val totalExpensesThisMonth = all_mpesa_transactions
            .filter { !it.transaction_date?.isEmpty()!! && isValidDate(it.transaction_date!!, dateFormat, currentMonth, currentYear) && (it.transaction_type != "deposit" && it.transaction_type != "receival" && it.transaction_type != "reverse") }
            .sumOf { it.amount!! }

        val totalIncomeThisMonth = all_mpesa_transactions
            .filter { !it.transaction_date?.isEmpty()!! && isValidDate(it.transaction_date!!, dateFormat, currentMonth, currentYear) && (it.transaction_type == "deposit" || it.transaction_type == "receival" || it.transaction_type == "reverse") }
            .sumOf { it.amount!! }

        val netSpendThisMonth = totalIncomeThisMonth - totalExpensesThisMonth


        updateTextBox.text = totalExpensesThisMonth.toString()
        netSpendTextBox.text = netSpendThisMonth.toString()

        val recyclerView: RecyclerView = binding.transactionsRecycler
        val adapter = MpesaTransactionAdapter(all_mpesa_transactions)

        if(all_mpesa_transactions.isNullOrEmpty()){
            var latest_mpesa_transaction = mutableListOf<MpesaTransaction>()
            latest_mpesa_transaction = db_helper?.getLatestTransaction()!!
            //println("size ya hii mpya ni:" + latest_mpesa_transaction_last_month.size + " " + latest_mpesa_transaction_last_month.first().transaction_date)

            balance_text.text = String.format("%,.2f", latest_mpesa_transaction.first().mpesa_balance)

        }else{
            balance_text.text = String.format("%,.2f", all_mpesa_transactions.first().mpesa_balance)
        }

        /*balance_text.hideSkeleton()
        currency_label.hideSkeleton()
        mpesa_sync_label.hideSkeleton()
        mpesa_balance_label.hideSkeleton()
        mpesa_sync_label.hideSkeleton()
        updateTextBox.hideSkeleton()
        netSpendTextBox.hideSkeleton()*/

        updatesyncCircularProgress()

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter


    }

    private fun updatesyncCircularProgress() {
        var totalSMS:Int = 0
        val cr: ContentResolver = requireActivity().contentResolver
        if(ContextCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            val c = cr.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS,Telephony.Sms.Inbox.DATE),  // Select body text
                Telephony.Sms.ADDRESS + " = ?",
                arrayOf("MPESA"),
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
            ) // Default sort order
            totalSMS = c!!.count
            c.close()
        }


        var transactionCount = db_helper?.getCountAllTransactions()!! ?: 0

        var rejectedSmsCount = db_helper?.getCountAllRejectedSms()!! ?: 0

        var progressPercentage = (transactionCount.toDouble() + rejectedSmsCount.toDouble()) / totalSMS.toDouble() * 100

        println("Values Zake:" + totalSMS.toInt() )
        println("Values Zake:" + transactionCount.toInt() )
        println("Values Zake:" + rejectedSmsCount.toInt() )




        circularProgressDrawable.setProgress(progressPercentage.toDouble(), 100.00)




    }

    private fun isValidDate(date: String, dateFormat: SimpleDateFormat, currentMonth: Int, currentYear: Int): Boolean {
        try {
            val transactionDate = dateFormat.parse(date)
            val calendar = Calendar.getInstance()
            calendar.time = transactionDate
            val transactionYear = calendar.get(Calendar.YEAR)
            val transactionMonth = calendar.get(Calendar.MONTH) + 1
            return transactionYear == currentYear && transactionMonth == currentMonth
        } catch (e: Exception) {
            return false
        }
    }

    private fun isSameDay(dateString: String, currentDate: Date): Boolean {
        if (dateString != "") {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val transactionDate = dateFormat.parse(dateString)

            val calendar1 = Calendar.getInstance()
            val calendar2 = Calendar.getInstance()
            calendar1.time = transactionDate
            calendar2.time = currentDate

            return (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                    && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
                    && calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH))
        }
        return false
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getAllSmsFromProvider(): ArrayList<MutableList<String>> {
        val sms = ArrayList<MutableList<String>>()
        val lstSms: MutableList<String> = ArrayList()
        val lstRcvr: MutableList<String> = ArrayList()
        var lstDate: MutableList<String> = ArrayList()
        var lstId: MutableList<String> = ArrayList()


        val cr: ContentResolver = requireActivity().contentResolver
        if(ContextCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            val c = cr.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS,Telephony.Sms.Inbox.DATE,Telephony.Sms.Inbox._ID),  // Select body text
                Telephony.Sms.ADDRESS + " = ?",
                arrayOf("MPESA"),
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
            ) // Default sort order
            val totalSMS = c!!.count
            if (c.moveToFirst()) {
                for (i in 0 until totalSMS) {
                    lstSms.add(c.getString(0))
                    lstRcvr.add(c.getString(1))
                    lstDate.add(c.getString(2))
                    lstId.add(c.getString(3))
                    c.moveToNext()
                }
            } else {
                throw RuntimeException("You have no SMS in Inbox")
            }
            c.close()
        }
        sms.add(lstSms)
        sms.add(lstRcvr)
        sms.add(lstDate)
        sms.add(lstId)
        println("Hodi: " + sms[0].first() + " " + sms[1].first() +" " + sms[2].first() + " " + sms[3].first())
        addAllSmsToDb(sms)
        println("size ya maana: " + sms.size)
        return sms
    }

    private fun addAllSmsToDb(sms: ArrayList<MutableList<String>>) {
        // Check if the database is open
        if (db?.isOpen == true) {
            val tableExists = doesTableExist("all_sms", db!!)

            if (!tableExists) {
                // If the table does not exist, create it
                db_helper?.createInitialTables(db!!)
            }

            // Begin the transaction
            db?.beginTransaction()

            try {
                // Insert SMS into the database
                doInsert(sms)
                // Mark the transaction as successful if it reaches this point
                db?.setTransactionSuccessful()
            } finally {
                // End the transaction (either commit or rollback)
                db?.endTransaction()
            }
        }
    }

    private fun doInsert(sms: ArrayList<MutableList<String>>) {
        println("mambo ni: " + sms[0].indices)
        for (i in sms[0].indices) {
            val smsBody = sms[0][i]
            val timestamp = sms[2][i].toLong()
            val smsUniqueId = sms[3][i].toLong()

            // Insert the SMS into the database
            insertSmsIntoDb(smsUniqueId, smsBody, timestamp)
        }
    }


    private fun insertSmsIntoDb(uniqueId: Long, smsBody: String, timestamp: Long) {
        // Check if the database is open before inserting
        if (db?.isOpen == true) {
            // Assuming you have a method insertSms in your DbHelper class
            db_helper?.insertSms(uniqueId, smsBody, timestamp)
        }
    }

    fun doesTableExist(tableName: String, db: SQLiteDatabase): Boolean {
        val query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
        val cursor: Cursor? = db.rawQuery(query, arrayOf(tableName))
        var tableExists = false

        cursor?.use {
            tableExists = it.count > 0
        }

        return tableExists
    }

    fun isTableEmpty(tableName: String, dbHelper: DbHelper): Boolean {
        val db = dbHelper.readableDatabase
        val query = "SELECT COUNT(*) FROM $tableName"
        val cursor: Cursor = db.rawQuery(query, null)

        cursor.use {
            if (it.moveToFirst()) {
                val count = it.getInt(0)
                return count == 0
            }
        }

        return true // Default to true if there was an error or the table doesn't exist
    }

    fun findDatesInText(text: String): List<Date> {
        val dateFound = mutableListOf<Date>()

        val datePattern = "\\d{1,2}/\\d{1,2}/\\d{2,4}"
        val timePattern = "\\d{1,2}:\\d{2}\\s(?:AM|PM)"

        val pattern = Pattern.compile("$datePattern\\s(?:at)\\s$timePattern")
        val matcher = pattern.matcher(text)

        val sdf = SimpleDateFormat("d/M/yy 'at' h:mm a", Locale.ENGLISH) // Adjust date format as needed

        while (matcher.find()) {
            val dateTimeString = matcher.group()
            try {
                val date = sdf.parse(dateTimeString)
                if (date != null) {
                    dateFound.add(date)
                }
            } catch (e: Exception) {
                // Handle parsing errors if needed
            }
        }

        return dateFound
    }

    fun extractSubstringBetweenWords(text: String, startWord: String, endWord: String): String? {
        val patternString = "\\b$startWord\\b(.*?)\\b$endWord\\b"
        val pattern = Pattern.compile(patternString)
        val matcher = pattern.matcher(text)

        if (matcher.find()) {
            return matcher.group(1)
        }

        return null
    }

    fun findWordAfterPhrase(input: String, phrase: String): String? {
        val regex = Regex("$phrase\\s+([\\w,\\.]+)")
        val match = regex.find(input)
        return match?.groups?.get(1)?.value?.replace(",", "") // Remove commas
    }

    fun findFirstKshPhrase(input: String): String? {
        val regex = Regex("\\bKsh[\\d,.]*")
        val match = regex.find(input)
        return match?.value?.replace(",", "")
    }

    fun removeNonNumericText(input: String): String {
        val regex = Regex("\\d+\\.?\\d*")
        val matches = regex.findAll(input)
        val numericStrings = matches.map { it.value }
        return numericStrings.joinToString("")
    }

    fun itHasMpesaCode(input: String): Boolean {
        val regex = Regex("^\\b[A-Z0-9]{10}\\b")
        val matchResult = regex.find(input)
        return matchResult != null
    }

    fun extractDoubleFromInput(input: String): Double? {
        // Use regular expression to extract the first decimal point and up to two decimal numbers
        val regex = Regex("""(\d+\.\d{1,2})""")
        val match = regex.find(input)

        return match?.value?.toDouble()
    }

    fun convertSmstoMpesaTransactions(msg_str: ArrayList<MutableList<String>>): MutableList<MpesaTransaction> {
        var transactions_list = mutableListOf<MpesaTransaction>()
        if (msg_str != null) {

            val dateFrmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            var progress = 0

            var totalWork = msg_str[0].size

            println("lines zake ni: " + msg_str[2][0])

            requireActivity().runOnUiThread {


                Toast.makeText(
                    activity?.baseContext,
                    "Please Wait While SMS(s) are Converted to Mpesa Transactions",
                    Toast.LENGTH_SHORT
                ).show()

            }

            for (i in msg_str[0].indices) {

                progress = i

                var recipient: Recepient? = null
                var transaction_type = "none"
                var paybill_account = "none"
                var mpesa_depositor = "none"
                var msg_date = ""
                var mpesa_balance = 0.00
                var transaction_cost = 0.00
                var amount = 0.00
                var transaction_date = ""
                var message_is_not_balance = true
                var sender: Sender? = null


                var msg_arr = msg_str[0][i].split(" ").toTypedArray()
                var msg_txt = msg_arr.joinToString(separator = " ")


                if (msg_txt.contains("Your account balance was:")) {
                    message_is_not_balance = false
                    var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                    rejectedSmsList.add(rejectedList)
                }

                if (itHasMpesaCode(msg_txt) && message_is_not_balance) {

                    val dateObjct = Date(msg_str[2][i].toLong())
                    msg_date = dateFrmt.format(dateObjct)


                    val foundDateTimes = findDatesInText(msg_txt)

                    for (dateTime in foundDateTimes) {
                        transaction_date = dateFrmt.format(dateTime)
                    }

                    var mpesa_code = msg_arr[0]

                    if (msg_arr[3] == "sent") {
                        transaction_type = "send_money"
                        if (msg_txt.contains("for account")) {
                            transaction_type = "paybill"
                        }
                    } else if (msg_txt.contains("You bought")) {
                        transaction_type = "topup"
                    } else if (msg_arr[7] == "Give") {
                        transaction_type = "deposit"
                    } else if (msg_arr[3] == "paid") {
                        transaction_type = "till"
                    }
                    else if(msg_txt.contains("reversed") || msg_txt.contains("Reversal")){
                        transaction_type = "reverse"
                    }
                    else if (msg_txt.contains("received") && msg_txt.contains("from")){
                        transaction_type = "receival"
                    }
                    else if (msg_arr[5].contains("Withdraw")){
                        transaction_type = "withdraw"
                    }

                    if (transaction_type == "send_money") {

                        var recepient_substr = extractSubstringBetweenWords(msg_txt, "to", "on")
                        var recepient_arr = recepient_substr?.split(" ")?.toTypedArray()
                        var name = ""
                        name = recepient_arr?.joinToString(
                            separator = " ",
                            limit = recepient_arr?.size - 2,
                            truncated = ""
                        ).toString()
                        var phone_no = recepient_arr?.get(recepient_arr.size - 2)
                        recipient = Recepient(name, phone_no)

                    } else if (transaction_type == "till") {
                        var recepient_substr = extractSubstringBetweenWords(msg_txt, "to", "on")
                        var name = recepient_substr?.trim()?.dropLast(1)

                        recipient = Recepient(name, "none")


                    } else if (transaction_type == "topup") {
                        recipient = Recepient("Mbarak Ahmed", "0700234463")
                    } else if (transaction_type == "paybill") {
                        var recepient_substr =
                            extractSubstringBetweenWords(msg_txt, "to", "for")
                        var name = recepient_substr?.trim()
                        recipient = Recepient(name, "none")
                        var paybill_account_string =
                            extractSubstringBetweenWords(msg_txt, "for account", "on")
                        if (paybill_account_string?.trim() != "") {
                            paybill_account = paybill_account_string?.trim().toString()
                        }
                    } else if (transaction_type == "deposit") {
                        var depositor_string =
                            extractSubstringBetweenWords(msg_txt, "to", "New")
                        mpesa_depositor = depositor_string?.trim().toString()
                    }
                    else if (transaction_type == "receival"){
                        var sender_substr = extractSubstringBetweenWords(msg_txt, "from", "on")
                        var sender_arr = sender_substr?.split(" ")?.toTypedArray()
                        var name = ""
                        name = sender_arr?.joinToString(
                            separator = " ",
                            limit = sender_arr?.size - 2,
                            truncated = ""
                        ).toString()
                        var phone_no = sender_arr?.get(sender_arr.size - 2)
                        sender = Sender(name, phone_no)
                    }
                    else if (transaction_type == "withdraw"){
                        var depositor_string =
                            extractSubstringBetweenWords(msg_txt, "from", "New")
                        mpesa_depositor = depositor_string?.trim().toString()
                    }
                    else if (transaction_type == "reverse"){
                        recipient = Recepient("Mbarak Ahmed", "0700234463")

                    }


                    var balance_string = findWordAfterPhrase(msg_txt, "balance is")
                    if (transaction_type == "reverse"){
                        balance_string = findWordAfterPhrase(msg_txt, "balance is now") ?: findWordAfterPhrase(msg_txt, "account balance is")
                    }
                    var transaction_cost_string =
                        findWordAfterPhrase(msg_txt, "Transaction cost,") ?: "none"
                    var amount_string = findFirstKshPhrase(msg_txt)

                    if (transaction_type == "reverse") {
                        println(amount_string)
                    }




                        if (balance_string != null) {
                        var provisional_mpesa_balance =
                            balance_string?.replace("Ksh", "")


                        if (provisional_mpesa_balance.isNullOrEmpty() == false){
                            println("Hebu" + provisional_mpesa_balance.toString())
                            mpesa_balance = extractDoubleFromInput(provisional_mpesa_balance) ?: 0.00

                        }
                        else{

                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "Bad Message Balance @ index: " + i.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                            rejectedSmsList.add(rejectedList)

                            continue

                        }

                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                activity,
                                "Bad Message Balance @ index: " + i.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                            var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                            rejectedSmsList.add(rejectedList)
                        continue

                    }

                    if (transaction_cost_string != "none") {
                        transaction_cost =
                            removeNonNumericText(transaction_cost_string)?.toDouble()
                        //transaction_cost = transaction_cost_string?.replace("Ksh", "")?.dropLast(1)?.toDouble()!!
                    }

                    if (amount_string != null) {
                        if (amount_string.isNullOrBlank()) {

                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "Bad Message Amount @ index Empty String: " + i.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                            rejectedSmsList.add(rejectedList)

                            continue

                        } else {
                                amount =
                                    amount_string?.replace("Ksh", "")?.dropLast(1)?.toDouble()!!

                        }

                    } else {

                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                activity,
                                "Bad Message Amount @ index Empty String: " + i.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                        rejectedSmsList.add(rejectedList)
                        continue

                    }


                    var mpesa_transaction = MpesaTransaction(
                        id = null,
                        msg_date = msg_date,
                        transaction_date = transaction_date,
                        mpesa_code = mpesa_code,
                        recipient = recipient,
                        account = Account(
                            id = 1,
                            name = "General Expenses",
                            description = "General Expenses",
                            type = "Expense"
                        ),
                        amount = amount,
                        transaction_type = transaction_type,
                        mpesa_balance = mpesa_balance,
                        transaction_cost = transaction_cost,
                        mpesa_depositor = mpesa_depositor,
                        sms_text = msg_txt,
                        paybill_acount = paybill_account,
                        sender = sender
                    )

                    transactions_list.add(mpesa_transaction)

                    val currentProgress = (progress * 100 / totalWork)

                    // Update the ProgressBar on the main thread
                    handler.post {
                        progressBar.progress = currentProgress
                    }


                    //println(msg_txt)


//                    binding.smsSize.text =
  //                      msg_arr[0] + " - " + msg_date + " - " + transaction_date + " - " + mpesa_transaction.recipient?.name + " - cool - " + recipient?.name + " - " + recipient?.phone_no + " - " + transaction_type + " - " + msg_txt + " - " + msg_arr[7] + " - " + mpesa_balance + " - " + balance_string + " - " + transaction_cost + " - " + transaction_cost_string + " - " + amount_string + " - " + amount + " - " + paybill_account + " - " + mpesa_depositor + " - " + mpesa_transaction.mpesa_code + " - " + msg_str.size + " - " + msg_str[0].size + " - " + msg_str[2].size + " - " + msg_str[1].size
                }
                else{
                    var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                    rejectedSmsList.add(rejectedList)
                }
            }
        }
        return transactions_list
    }

    private fun startConversionTask(msg_str: ArrayList<MutableList<String>>): MutableList<MpesaTransaction> {
        // Show the ProgressBar
        var result = mutableListOf<MpesaTransaction>()
        var progress = 0
        val rootLayout: CoordinatorLayout? = _binding?.root?.findViewById(R.id.coordinator_layout_appbar)

        // Apply the fade-in transition to show the ProgressBar
        progressBar.startAnimation(fadeInAnimation)
        progressBar.visibility = View.VISIBLE

        // Start the task in a Coroutine
        var task = GlobalScope.launch(Dispatchers.IO) {
            bigResult = MpesaTransaction.convertSmstoMpesaTransactions(msg_str = msg_str, progressBar)
            result = bigResult.transactionsList

            withContext(Dispatchers.Main) {
                // Hide the ProgressBar with vertical shrink transition
                progressBar.startAnimation(verticalShrinkAnimation)
                progressBar.visibility = View.INVISIBLE


                // Process the result or update UI as needed
                // For example, display the result in a TextView
                // textView.text = result
            }
        }

        task.invokeOnCompletion {

            onConversionTaskCompletion(result)
        }

        return result
    }

    private fun onConversionTaskCompletion(convertedTransactions: MutableList<MpesaTransaction> ) {
        // Show the ProgressBar
        progressBar.visibility = View.VISIBLE
        progressBar.startAnimation(fadeInAnimation)
        val dateFrmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

        requireActivity().runOnUiThread {
            Toast.makeText(
                activity?.baseContext,
                "Adding Transactions to DB ...",
                Toast.LENGTH_SHORT
            ).show()
        }


        println("progress bar resetting")
        var i = 0

        progressBar.progress = i




        db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
        db = db_helper?.writableDatabase

        if (db != null) {
            for (transaction in convertedTransactions) {
                i++
                db_helper?.insertMpesaTransaction(transaction)

                val currentProgress = (i * 100 / convertedTransactions.size)


                progressBar.progress = currentProgress
            }
        }


        println("End: " + progressBar.progress)

        progressBar.startAnimation(verticalShrinkAnimation)
        progressBar.visibility = View.INVISIBLE

        progressBar.visibility = View.VISIBLE
        progressBar.startAnimation(fadeInAnimation)

        i = 0

        progressBar.progress = i

        if (db != null) {
            for (rejectedMessage in bigResult.rejectedSmsList) {
                i++
                val dateObjct = Date(rejectedMessage[0].toLong())
                var msg_date = dateFrmt.format(dateObjct)
                db_helper?.insertRejectedSMS(msg_date, rejectedMessage[1].toString())

                val currentProgress = (i * 100 / convertedTransactions.size)


                progressBar.progress = currentProgress
            }
        }

        progressBar.startAnimation(verticalShrinkAnimation)
        progressBar.visibility = View.INVISIBLE





        all_mpesa_transactions = db_helper?.getThisMonthMpesaTransactions()!!




        print(all_mpesa_transactions.toString())

        requireActivity().runOnUiThread {
            updateTransactionThisMonth(all_mpesa_transactions)

        }

    }
    override fun onRefresh() {
        // This method will be called when you need to refresh the RecyclerViews
        all_mpesa_transactions = db_helper?.getThisMonthMpesaTransactions()!!
        requireActivity().runOnUiThread {
            updateTransactionThisMonth(all_mpesa_transactions)

        }


        // If you have an instance of ViewAllTransactionsActivity, call its refresh method
        // Example:
        // val viewAllTransactionsActivity = supportFragmentManager.findFragmentByTag(ViewAllTransactionsActivity::class.java.simpleName) as? ViewAllTransactionsActivity
        // viewAllTransactionsActivity?.refreshRecyclerView()
    }

    object CallbackSingleton {
        var refreshCallback: RefreshRecyclerViewCallback? = null
    }








}