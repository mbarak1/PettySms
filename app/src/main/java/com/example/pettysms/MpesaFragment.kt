package com.example.pettysms

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import com.example.pettysms.databinding.FragmentMpesaBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
interface RefreshRecyclerViewCallback {
    fun onRefresh()
}
interface OnActionModeInteraction {
    fun onDestroyActionMode()
}
class MpesaFragment : Fragment(), RefreshRecyclerViewCallback  {

    private var _binding: FragmentMpesaBinding? = null
    private var progressBar: ProgressBar? = null
    private var currency_label: TextView? = null
    private var balance_text: TextView? = null
    private var mpesa_balance_label: TextView? = null
    private var mpesa_sync_label: TextView? = null
    private var handler: Handler? = null
    private var verticalShrinkAnimation: Animation? = null
    private var fadeInAnimation: Animation? = null
    private var fadeOutAnimation: Animation? = null
    private var verticalShrinkFadeOut: Animation? = null
    private var fadeInselectLayoutAnimation: Animation? = null
    private var updateTextBox: TextView? = null
    private var netSpendTextBox: TextView? = null
    private var viewAlLink: TextView? = null
    private var appBar: AppBarLayout? = null
    private var circularProgressDrawable: CircularProgressIndicator? = null
    private var bigResult: MpesaTransaction.Companion.MpesaTransactionResult? = null
    private var layoutSelectAll: LinearLayout? = null
    private var lineChart: LineChart? = null
    private var selectAllCheckBox: CheckBox? = null
    private var loadingDialog: AlertDialog? = null
    private var loadingText: TextView? = null
    private var recyclerView: RecyclerView? = null






    private var actionMode: ActionMode? = null
    private var progressAnimator: ValueAnimator? = null // Single ValueAnimator instance
    private var adapter: MpesaTransactionAdapter? = null


    private var selectedTransactions = HashSet<Int>()
    private var removedTransactions = HashSet<Int>()
    private var rejectedSmsList = mutableListOf<MutableList<String>>()
    private val dataViewModel: DataViewModel by activityViewModels()


    private var all_mpesa_transactions= mutableListOf<MpesaTransaction>()
    private var activeTransactions = mutableListOf<MpesaTransaction>()


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
        mpesa_balance_label?.text = "Balance: "
        currency_label = binding.root.findViewById(R.id.currency_label)
        currency_label?.text = "Ksh."
        balance_text = binding.root.findViewById(R.id.balance_text)
        balance_text?.text = "0.00"
        mpesa_sync_label = binding.root.findViewById(R.id.sync_progress_label)
        mpesa_sync_label?.text = "Sync Progress:"
        updateTextBox = binding.transactionsThisMonth
        netSpendTextBox= binding.netSpendThisMonth
        circularProgressDrawable = binding.circularProgress
        viewAlLink = binding.viewAllLink
        appBar = binding.appbar
        lineChart = binding.lineChart
        layoutSelectAll = binding.selectAllLayout
        selectAllCheckBox = binding.checkboxSelectAllItems
        verticalShrinkFadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.vertical_shrink_fade_out)
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

        TaskPreferences.setTaskRunning(requireContext(), true)  // Set to true at start






        val actionbar = binding.root.findViewById<Toolbar>(R.id.toolbar)
        actionbar.setTitle(R.string.mpesa_tool)
        (activity as AppCompatActivity).setSupportActionBar(actionbar)

        verticalShrinkAnimation = AnimationUtils.loadAnimation(activity, R.anim.vertical_shrink)
        fadeInAnimation = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
        fadeInselectLayoutAnimation = AnimationUtils.loadAnimation(activity, R.anim.fade_in_select_all_layout)
        fadeOutAnimation = AnimationUtils.loadAnimation(activity, R.anim.fade_out)

        viewAlLink?.setOnClickListener{
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
        createLoadingDialog()
        loadingDialog?.show()


        val task = lifecycleScope.launch(Dispatchers.IO) {
            beginOperations()
        }

        task.invokeOnCompletion {
            loadingDialog?.dismiss()
        }



        //getSmsFrom2023(requireContext())

        return binding.root

    }

    @SuppressLint("SetTextI18n")
    private fun createLoadingDialog(): AlertDialog {
        // Create a custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)
        loadingText?.text = "Loading... Please Wait"

        // Show loading dialog
        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(false)
            .create()


        return loadingDialog as AlertDialog
    }

    private fun beginOperations() {
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
                    Log.d("MpesaFragment","Start conversion")
                    all_mpesa_transactions = startConversionTask(msg_str)
                    saveArrayToViewModel(all_mpesa_transactions)
                }

                    Log.d("MpesaFragment", "Table Exists but Empty")

            }
            else{

                Log.d("MpesaFragment", "Table Exists")
                progressBar?.visibility = View.INVISIBLE

                val all_mpesa_transactions_from_db = db_helper?.getThisMonthMpesaNonDeletedTransactions()!!

                val all_mpesa_transactions_from_view_model= retrieveArrayFromViewModel() ?: mutableListOf()

                if(all_mpesa_transactions_from_view_model.isNullOrEmpty() || all_mpesa_transactions_from_db.size > all_mpesa_transactions_from_view_model.size) {
                    println("check DB")
                    all_mpesa_transactions = all_mpesa_transactions_from_db
                    saveArrayToViewModel(all_mpesa_transactions)
                }else{
                    all_mpesa_transactions = all_mpesa_transactions_from_view_model
                }



                updateTransactionThisMonth(all_mpesa_transactions)



            }

        } else {

            Log.d("MpesaFragment" , "Table Does not Exist")
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

    override fun onStart() {
        super.onStart()
        FragmentVisibilityTracker.isMpesaFragmentVisible = true
    }

    override fun onStop() {
        super.onStop()
        FragmentVisibilityTracker.isMpesaFragmentVisible = false
    }

    private fun getSmsFrom2023(context: Context) {
        val smsList = mutableListOf<SmsMessage>()
        val contentResolver = context.contentResolver

        // Start and end timestamps for the year 2023
        val startOf2023 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2023-01-01")!!.time
        val endOf2023 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2023-12-31")!!.time

        if(ContextCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox._ID),
                Telephony.Sms.Inbox.DATE + " BETWEEN ? AND ?",
                arrayOf(startOf2023.toString(), endOf2023.toString()),
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY))
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS))
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE))
                    val id = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox._ID))

                    smsList.add(SmsMessage(id, address, body, date))
                }
            }
        } else {
            Log.e("SMS Retrieval", "Permission to read SMS not granted")
        }

        println("Retrieved SMS from 2023: ${smsList.size} messages")
        smsList.forEach { sms ->
            println("${sms.date}: ${sms.address} - ${sms.body}")
        }
    }


    data class SmsMessage(val id: String, val address: String, val body: String, val date: Long)


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

    @Deprecated("Deprecated in Java")
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


    private fun saveArrayToViewModel(array: MutableList<MpesaTransaction>) {
        dataViewModel?.dataArray = array
    }
    fun retrieveArrayFromViewModel() : MutableList<MpesaTransaction>? {
        // Accessing the member property to retrieve the data
        return dataViewModel?.dataArray
    }

    private fun updateTransactionThisMonth(mpesa_transactions: MutableList<MpesaTransaction>) {
        updateTextBox = binding.transactionsThisMonth
        netSpendTextBox = binding.netSpendThisMonth
        val no_transactions_textbox: TextView = binding.noTransactionsMessage
        val currentDate = Calendar.getInstance().time

        // Check if there are any transactions to display
        no_transactions_textbox.visibility = if (mpesa_transactions.isEmpty()) View.VISIBLE else View.INVISIBLE

        val sortedTransactions = sortTransactions(mpesa_transactions)
        activeTransactions = sortedTransactions

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

        // Filter transactions for today and this month
        val todayTransactions = sortedTransactions.filter {
            it.transaction_date?.let { date -> isSameDay(date, currentDate) } ?: false
        }

        val (expensesThisMonth, incomeThisMonth) = sortedTransactions.fold(0.0 to 0.0) { acc, transaction ->
            val amount = transaction.amount ?: 0.0
            if (transaction.transaction_date?.let {
                    isValidDate(it, dateFormat, currentMonth, currentYear)
                } == true) {
                when (transaction.transaction_type) {
                    "deposit", "receival", "reverse" -> acc.first to acc.second + amount
                    else -> acc.first + amount to acc.second
                }
            } else acc
        }

        val netSpendThisMonth = incomeThisMonth - expensesThisMonth

        // Update Text Views
        val decimalFormat = DecimalFormat("#.0")
        updateTextBox?.text = decimalFormat.format(expensesThisMonth)
        netSpendTextBox?.text = decimalFormat.format(netSpendThisMonth)



        // Initialize RecyclerView with adapter and listeners
        recyclerView = binding.transactionsRecycler
        requireActivity().runOnUiThread {

            adapter = MpesaTransactionAdapter(
                requireContext(),
                sortedTransactions,
                object : MpesaTransactionAdapter.OnItemClickListener {
                    override fun onItemClick(transactionId: Int?) {
                        if (actionMode != null) {
                            toggleSelection(transactionId)
                        } else {
                            sortedTransactions.find { it.id == transactionId }
                                ?.let { selectedTransaction ->
                                    val intent = Intent(
                                        requireContext(),
                                        TransactionViewerActivity::class.java
                                    ).apply {
                                        putExtra(
                                            "mpesaTransactionJson",
                                            Gson().toJson(selectedTransaction)
                                        )
                                    }
                                    transactionViewerActivityLauncher.launch(intent)
                                }
                        }
                    }

                    override fun onItemLongClick(transactionId: Int?) {
                        (view?.context as? AppCompatActivity)?.let { context ->
                            if (actionMode == null) {
                                actionMode = context.startSupportActionMode(actionModeCallback)!!
                                toggleSelection(transactionId)
                            } else {
                                actionMode?.finish()
                            }
                        }
                    }
                })

            // Configure RecyclerView
            recyclerView?.layoutManager = LinearLayoutManager(activity)
            recyclerView?.adapter = adapter



            updatesyncCircularProgress()

        }

        // Set balance text based on available transactions
        val balanceText = if (sortedTransactions.isNotEmpty()) {
            sortedTransactions.first().mpesaBalance
        } else {
            db_helper?.getMostRecentTransaction()?.firstOrNull()?.mpesaBalance
        }
        balance_text?.text = String.format("%,.2f", balanceText ?: 0.0)

        // Update chart if supported
        lifecycleScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                chartUpdate(sortedTransactions)
            }
        }

        TaskPreferences.setTaskRunning(requireContext(), false)  // Set to true at start




    }

    fun restartApp() {
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish() // Optional: Close the current fragment/activity
    }


    private val transactionViewerActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // TransactionViewerActivity activity is finished
            // Execute your code here
            println("habari yako")
            all_mpesa_transactions = db_helper?.getThisMonthMpesaNonDeletedTransactions()!!
            saveArrayToViewModel(all_mpesa_transactions)
            updateTransactionThisMonth(all_mpesa_transactions)

        }else{
            println("hallo hallo")
            all_mpesa_transactions = db_helper?.getThisMonthMpesaNonDeletedTransactions()!!
            saveArrayToViewModel(all_mpesa_transactions)
            updateTransactionThisMonth(all_mpesa_transactions)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun chartUpdate(sorted_transactions: MutableList<MpesaTransaction>) {

        val count_map = countTransactionsByDate(sorted_transactions)
        val colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0)
        val colorSurfaceContainer = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainer, 0)
        val colorControlNormal = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorControlNormal, 0)
        val entries = ArrayList<Entry>()
        val xAxisLabels = mutableListOf<String>()

        // Convert LocalDate to formatted strings and add to entries and xAxisLabels
        val formatter = DateTimeFormatter.ofPattern("dd-MMM", Locale.ENGLISH)
        var index = 0f

        val minDate = count_map.keys.minOrNull()
        val maxDate = count_map.keys.maxOrNull()

        if (minDate != null && maxDate != null) {
            var currentDate = minDate
            while (currentDate?.isBefore(maxDate) == true || currentDate == maxDate) {
                val value = count_map[currentDate] ?: 0f
                entries.add(Entry(index++, value))
                xAxisLabels.add(currentDate.format(formatter))
                currentDate = currentDate.plusDays(1) // Move to the next day
            }
        }
        val dataSet = LineDataSet(entries, "Number of transactions this month")

        // Set gradient fill
        val startColor = colorPrimary // Red
        val endColor = colorSurfaceContainer  // Green
        val gradientDrawable = ChartUtils.getGradientDrawable(startColor, endColor)
        dataSet.fillDrawable = gradientDrawable
        dataSet.setDrawFilled(true)

        // Customize markers
        dataSet.setDrawCircles(true)
        dataSet.circleRadius = 4f // Increased size
        dataSet.setCircleColor(colorPrimary)

        // Customize line
        dataSet.color = colorPrimary
        dataSet.lineWidth = 2f // Increased thickness
        dataSet.mode = LineDataSet.Mode.LINEAR // Smooth line

        val lineData = LineData(dataSet)
        lineChart?.data = lineData

        // Set XAxis formatter to display dates
        val xAxis = lineChart?.xAxis
        println("malabel: " + xAxisLabels.toString())
        xAxis?.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.setGranularity(1f) // Ensure all x-axis values are shown


        // Hide Y axis grid lines
        val leftAxis: YAxis? = lineChart?.axisLeft
        leftAxis?.setDrawGridLines(false)

        // Hide right Y axis
        val rightAxis: YAxis? = lineChart?.axisRight
        rightAxis?.isEnabled = false

        //lineChart.setTouchEnabled(false) // Disable touch gestures (including zooming)
        lineChart?.isDragEnabled = false // Disable dragging (panning)
        lineChart?.setScaleEnabled(false) // Disable scaling (zooming)
        lineChart?.isDoubleTapToZoomEnabled = false // Disable double-tap zoom
        lineChart?.getDescription()?.setEnabled(false);

        // Set text color for x-axis and y-axis labels
        lineChart?.xAxis?.textColor = colorControlNormal
        lineChart?.axisLeft?.textColor = colorControlNormal
        lineChart?.axisRight?.textColor = colorControlNormal

// Set text color for legend
        val legend = lineChart?.legend
        legend?.textColor = colorControlNormal



        // Set animation
        // Set the initial alpha to 0
        lineChart?.alpha = 0f

        // Animate the alpha to 1 (fully visible) over a specified duration
        requireActivity().runOnUiThread {
            lineChart?.animate()
                ?.alpha(1f)
                ?.setDuration(1000) // Adjust the duration as needed
                ?.start()
        }
        //lineChart.animateX(1500, Easing.EaseInOutExpo)
        lineChart?.invalidate()

        // Display marker when clicked
        lineChart?.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)
                val date = e?.x?.toInt()?.let { index ->
                    count_map.keys.sorted().toList().getOrNull(index)?.format(formatter) ?: ""
                } ?: ""
                val value = e?.y ?: 0f // Get the value of the selected data point
                Toast.makeText(
                    requireContext(),
                    "Date: $date Value: $value",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected() {}
        })
    }

    object ChartUtils {

        // Function to create a gradient drawable
        fun getGradientDrawable(startColor: Int, endColor: Int): Drawable {
            return object : Drawable() {
                override fun draw(canvas: Canvas) {
                    val gradient = LinearGradient(
                        0f, bounds.top.toFloat(), 0f, bounds.bottom.toFloat(),
                        startColor, endColor, Shader.TileMode.CLAMP
                    )
                    val paint = Paint().apply { shader = gradient }
                    canvas.drawRect(bounds, paint)
                }

                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: ColorFilter?) {}
                override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            }
        }
    }

    fun sortTransactions(transactions: MutableList<MpesaTransaction>): MutableList<MpesaTransaction> {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val comparator = Comparator<MpesaTransaction> { t1, t2 ->
            val date1 = getDate(t1)
            val date2 = getDate(t2)
            date2.compareTo(date1)
        }
        transactions.sortWith(comparator)
        return transactions.toMutableList()
    }

    private fun getDate(transaction: MpesaTransaction): Date {
        val dateString = if (transaction.transaction_date?.isNotEmpty() == true) transaction.transaction_date else transaction.msg_date
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormatter.parse(dateString)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun countTransactionsByDate(transactions: MutableList<MpesaTransaction>): Map<LocalDate, Float> {
        val transactionCountMap = mutableMapOf<LocalDate, Float>()

        for (transaction in transactions) {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            val localDate = LocalDate.parse(transaction.transaction_date ?: transaction.msg_date, formatter)
            //val date = transaction.transaction_date?.split(" ")?.get(0) ?: transaction.msg_date?.split(" ")?.get(0) // Extracting only the date part
            val count = transactionCountMap[localDate] ?: 0
            transactionCountMap[localDate] = count.toFloat() + 1f
        }

        return transactionCountMap
    }

    private fun toggleSelection(transactionId: Int?) {
        if (selectedTransactions.contains(transactionId)) {
            selectedTransactions.remove(transactionId)
            removedTransactions.add(transactionId!!)
        } else {
            transactionId?.let {
                selectedTransactions.add(it)
                removedTransactions.remove(transactionId)
            }
        }

        updateActionModeTitle()

        // Notify the adapter about the change
        adapter?.setSelectedTransactions(selectedTransactions, false)
    }


    private fun showWarnigDialog(){

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Warning")
            .setMessage(getDeleteMessageString())
            .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
            .setNegativeButton("Dismiss") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Confirm") { dialog, which ->
                // Respond to positive button press
                adapter?.setRemovedTransactions(selectedTransactions,
                    selectAllCheckBox?.isChecked == true
                )
                Log.d(this.activity.toString(), "deleted utilised")

                if(selectAllCheckBox?.isChecked == true){
                    deleteAllTransactions()
                    selectAllCheckBox?.isChecked = false
                }
                else{
                    deleteSeletectedTransactionsFromDb(selectedTransactions)
                }

                clearSelectionWithoutNotifyingAdapter()

                updateActionModeTitle()
            }
            .show()
    }

    private fun getDeleteMessageString() : SpannableStringBuilder {
        val message = "Are you sure you want to delete ${selectedTransactions.size} transaction(s)?"
        val boldText = selectedTransactions.size.toString()

        val startIndex = message.indexOf(boldText)
        val endIndex = startIndex + boldText.length

        val spannable = SpannableStringBuilder(message)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun deleteSelectedTransactions() {
        // Handle the deletion of selected transactions
        // Update your data source or perform other actions
        showWarnigDialog()
    }

    private fun deleteSeletectedTransactionsFromDb(selectedTransactions: HashSet<Int>) {
        for (selectedTransaction in selectedTransactions){
            db_helper?.deleteTransaction(selectedTransaction)
            Log.d(this.activity.toString(), "Deleted Transaction: " + selectedTransaction)
        }

        updateDeletedTransactionsArray()
        actionMode?.finish()

    }

    fun updateDeletedTransactionsArray(){
        val iterator = all_mpesa_transactions.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.id in selectedTransactions) {
                iterator.remove()
            }
        }

        saveArrayToViewModel(all_mpesa_transactions)

        updateTransactionThisMonth(all_mpesa_transactions)
    }

    fun deleteAllTransactions(){
        db_helper?.deleteAllTransactions()
        updateDeletedTransactionsArray()
    }

    private fun clearSelection() {
        selectedTransactions.clear()
        adapter?.clearSelection()
    }

    private fun clearSelectionWithoutNotifyingAdapter(){
        selectedTransactions.clear()
    }

    private fun updateActionModeTitle() {
        actionMode?.title = "${selectedTransactions.size} selected"
    }

    @SuppressLint("RestrictedApi")
    private fun changeStatusBarColorWithAnimation(colorResId: Int) {
        val window = activity?.window ?: return
        val currentStatusBarColor = window.statusBarColor
        
        // Get the new color
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(colorResId, typedValue, true)
        val newStatusBarColor = typedValue.data
        
        // If colors are the same, don't animate
        if (currentStatusBarColor == newStatusBarColor) {
            return
        }
        
        // Create a ValueAnimator with shorter duration
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentStatusBarColor, newStatusBarColor)
        colorAnimator.addUpdateListener { animator ->
            val animatedColor = animator.animatedValue as Int
            window.statusBarColor = animatedColor
        }
        
        // Set up animation duration - shorter to reduce rendering load
        colorAnimator.duration = 300
        
        // Start the color animation
        colorAnimator.start()
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness =
            1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(
                color
            )) / 255
        return darkness < 0.5
    }



    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            //val popupMenuBackgroundColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.popupMenuBackground,"")
            //setSystemBarColor(requireActivity(), R.color.red_color);

            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_action_mode_layout, null)
            //mode?.customView = customView
            mode?.menuInflater?.inflate(R.menu.context_menu, menu)

            layoutSelectAll?.startAnimation(fadeInselectLayoutAnimation)
            layoutSelectAll?.visibility = View.VISIBLE
            changeStatusBarColorWithAnimation(com.google.android.material.R.attr.colorSurfaceContainer)
            selectAllCheckBox?.setOnCheckedChangeListener { buttonView, isChecked ->
                if(isChecked){
                    selectedTransactions.clear()
                    for (activetransaction in activeTransactions){
                        selectedTransactions.add(activetransaction.id!!)
                    }
                    updateActionModeTitle()
                    adapter?.setSelectedTransactions(selectedTransactions,true)

                }else{
                    selectedTransactions.clear()
                    updateActionModeTitle()
                    adapter?.setSelectedTransactions(selectedTransactions, true)

                }
            }


            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // You can perform any actions you want to update the menu here
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.delete_mpesa_transaction -> {
                    deleteSelectedTransactions()
                    // Handle delete action
                    // Implement this method to delete selected items
                    // For now, let's just finish the ActionMode
                    return true
                }
                // Add other actions as needed
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear the selection and finish ActionMode
            selectedTransactions.clear()
            adapter?.reinitializeAdapter()
            adapter?.setActionModeStatus(true)
            changeStatusBarColorWithAnimation(com.google.android.material.R.attr.colorSurface)
            layoutSelectAll?.startAnimation(verticalShrinkFadeOut)
            layoutSelectAll?.visibility = View.GONE
            actionMode = null
            selectAllCheckBox?.isChecked = false

        }
    }


    private fun updatesyncCircularProgress() {
        var totalSMS = 0
        val cr: ContentResolver = requireActivity().contentResolver
        if (ContextCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            val c = cr.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE),
                Telephony.Sms.ADDRESS + " = ?",
                arrayOf("MPESA"),
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
            )
            totalSMS = c?.count ?: 0
            c?.close()
        }

        val transactionCount = db_helper?.getCountAllTransactions() ?: 0
        val rejectedSmsCount = db_helper?.getCountAllRejectedSms() ?: 0
        val progressPercentage = ((transactionCount.toDouble() + rejectedSmsCount.toDouble()) / totalSMS.toDouble()) * 100

        // Check if there's an ongoing animation and cancel it
        progressAnimator?.cancel()

        // Initialize the animator with the new progress percentage
        progressAnimator = circularProgressDrawable?.progress?.toFloat()?.let {
            ValueAnimator.ofFloat(it, progressPercentage.toFloat()).apply {
                duration = 1000
                addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue as Float
                    circularProgressDrawable?.setProgress(animatedValue.toDouble(), 100.0)
                }
                start()
            }
        }
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
        adapter = null
        recyclerView?.adapter = null
        selectedTransactions = HashSet<Int>()
        removedTransactions = HashSet<Int>()
        rejectedSmsList = mutableListOf<MutableList<String>>()
        progressBar = null
        currency_label = null
        balance_text = null
        mpesa_balance_label = null
        mpesa_sync_label = null
        handler = null
        verticalShrinkAnimation = null
        fadeInAnimation = null
        fadeOutAnimation = null
        verticalShrinkFadeOut = null
        fadeInselectLayoutAnimation = null
        updateTextBox = null
        netSpendTextBox = null
        viewAlLink = null
        appBar = null
        circularProgressDrawable = null
        bigResult = null
        layoutSelectAll = null
        lineChart = null
        selectAllCheckBox = null
        loadingDialog = null
        loadingText = null
        recyclerView = null


    }

    private fun getAllSmsFromProvider(): ArrayList<MutableList<String>> {
        Log.d("MpesaFragment", "Getting all sms from provider")
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
                "${Telephony.Sms.Inbox.DATE} ASC"
            ) // Default sort order
            val totalSMS = c!!.count

            println("Total SMS: $totalSMS")


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
        addAllSmsToDb(sms)
        println("size ya maana: " + sms.size)
        return sms
    }

    private fun addAllSmsToDb(sms: ArrayList<MutableList<String>>) {
        Log.d("MpesaFragment", "Adding all SMS to db")

        if (db_helper == null) {
            db_helper = DbHelper(requireContext())
        }

        if (db == null || !db!!.isOpen) {
            db = db_helper?.writableDatabase
        }

        db?.let { database ->
            val tableExists = doesTableExist("all_sms", database)

            if (!tableExists) {
                Log.d("MpesaFragment", "Table does not exist, creating table")
                db_helper?.createInitialTables(database)
            } else {
                Log.d("MpesaFragment", "Table exists, proceeding to insert SMS")
            }

            database.beginTransaction()
            try {
                Log.d("MpesaFragment", "Inserting SMS into db")
                doInsert(sms)
                database.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("MpesaFragment", "Error during insertion: ${e.message}")
            } finally {
                database.endTransaction()
                Log.d("MpesaFragment", "Transaction ended")
            }
        } ?: Log.e("MpesaFragment", "Database is null or closed, cannot add SMS")
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

    private fun doesTableExist(tableName: String, db: SQLiteDatabase): Boolean {
        val query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
        db.rawQuery(query, arrayOf(tableName)).use { cursor ->
            return cursor.count > 0
        }
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
                            limit = recepient_arr?.size!! - 2,
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
                            limit = sender_arr?.size?.minus(2) ?: 0,
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


                    var mpesa_transaction = MpesaTransaction(
                        id = null,
                        msgDate = msg_date,
                        transactionDate = transaction_date,
                        mpesaCode = mpesa_code,
                        recipient = recipient,
                        account = Account(
                            id = 1,
                            name = "General Expenses",
                            type = "Expense",
                            owner = null,
                            accountNumber = null,
                            currency = "Kenyan Shilling"
                        ),
                        amount = amount,
                        transactionType = transaction_type,
                        mpesaBalance = mpesa_balance,
                        transactionCost = transaction_cost,
                        mpesaDepositor = mpesa_depositor,
                        smsText = msg_txt,
                        paybillAcount = paybill_account,
                        sender = sender
                    )

                    transactions_list.add(mpesa_transaction)

                    val currentProgress = (progress * 100 / totalWork)

                    // Update the ProgressBar on the main thread
                    handler?.post {
                        progressBar?.progress = currentProgress
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
        progressBar?.startAnimation(fadeInAnimation)
        progressBar?.visibility = View.VISIBLE

        // Start the task in a Coroutine
        var task = GlobalScope.launch(Dispatchers.IO) {
            bigResult = MpesaTransaction.convertSmstoMpesaTransactions(msg_str = msg_str, progressBar)
            result = bigResult?.transactionsList!!

            withContext(Dispatchers.Main) {
                // Hide the ProgressBar with vertical shrink transition
                progressBar?.startAnimation(verticalShrinkAnimation)
                progressBar?.visibility = View.INVISIBLE


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
        progressBar?.visibility = View.VISIBLE
        progressBar?.startAnimation(fadeInAnimation)
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

        progressBar?.progress = i




        db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
        db = db_helper?.writableDatabase

        if (db != null) {
            for (transaction in convertedTransactions) {
                i++
                db_helper?.insertMpesaTransaction(transaction)

                val currentProgress = (i * 100 / convertedTransactions.size)


                progressBar?.progress = currentProgress
            }

            val transactors = Transactor.getTransactorsFromTransactions(convertedTransactions)
            addTransactorsToDb(transactors)
            for (transaction in convertedTransactions){
                updateTransactionCheck(transaction)
            }

        }


        println("End: " + progressBar?.progress)

        progressBar?.startAnimation(verticalShrinkAnimation)
        progressBar?.visibility = View.INVISIBLE

        progressBar?.visibility = View.VISIBLE
        progressBar?.startAnimation(fadeInAnimation)

        i = 0

        progressBar?.progress = i

        if (db != null) {
            for (rejectedMessage in bigResult?.rejectedSmsList!!) {
                i++
                val dateObjct = Date(rejectedMessage[0].toLong())
                var msg_date = dateFrmt.format(dateObjct)
                db_helper?.insertRejectedSMS(msg_date, rejectedMessage[1].toString())

                val currentProgress = (i * 100 / convertedTransactions.size)


                progressBar?.progress = currentProgress
            }
        }

        progressBar?.startAnimation(verticalShrinkAnimation)
        progressBar?.visibility = View.INVISIBLE





        all_mpesa_transactions = db_helper?.getThisMonthMpesaNonDeletedTransactions()!!




        print(all_mpesa_transactions.toString())

        requireActivity().runOnUiThread {
            updateTransactionThisMonth(all_mpesa_transactions)
            val prefs = requireActivity().applicationContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("mpesa_first_launch", true).apply()

        }

    }

    private fun addTransactorsToDb(transactors: List<Transactor>) {
        db_helper = this.activity?.applicationContext?.let { DbHelper(it) }

        db_helper?.insertTransactors(transactors)


    }

    private fun updateTransactionCheck(mpesaTransaction: MpesaTransaction){

        db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
        mpesaTransaction.let { db_helper?.transactorCheckUpdateTransaction(it) }
    }

    override fun onRefresh() {
        // This method will be called when you need to refresh the RecyclerViews
        all_mpesa_transactions = db_helper?.getThisMonthMpesaNonDeletedTransactions()!!
        requireActivity().runOnUiThread {
            updateTransactionThisMonth(all_mpesa_transactions)

        }


        // If you have an instance of ViewAllTransactionsActivity, call its refresh method
        // Example:
        // val viewAllTransactionsActivity = supportFragmentManager.findFragmentByTag(ViewAllTransactionsActivity::class.java.simpleName) as? ViewAllTransactionsActivity
        // viewAllTransactionsActivity?.refreshRecyclerView()
    }

    override fun onResume() {
        if (db_helper == null) {
            db_helper = DbHelper(requireContext())
        }
        all_mpesa_transactions = db_helper?.getThisMonthMpesaNonDeletedTransactions()!!
        requireActivity().runOnUiThread {
            updateTransactionThisMonth(all_mpesa_transactions)

        }
        super.onResume()
    }

    fun destroyActionMode() {
        println("its been called dah")
        actionMode?.finish()
        // Add any additional cleanup code if needed
    }

    object CallbackSingleton {
        var refreshCallback: RefreshRecyclerViewCallback? = null
    }

    object FragmentVisibilityTracker {
        var isMpesaFragmentVisible: Boolean = false
    }

    object TaskPreferences {
        private const val PREFERENCE_NAME = "task_preferences"
        private const val CONVERSION_TASK_RUNNING = "conversion_task_running"

        fun setTaskRunning(context: Context, isRunning: Boolean) {
            val prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(CONVERSION_TASK_RUNNING, isRunning).apply()
        }

        fun isTaskRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(CONVERSION_TASK_RUNNING, false)
        }
    }

    override fun onPause() {
        super.onPause()
        
        // Cancel any running animations to prevent leaks and rendering issues
        progressAnimator?.cancel()
        progressAnimator = null
        
        // Ensure we're not leaving animations running when fragment is paused
        verticalShrinkAnimation?.cancel()
        fadeInAnimation?.cancel()
        fadeOutAnimation?.cancel()
        verticalShrinkFadeOut?.cancel()
        fadeInselectLayoutAnimation?.cancel()
    }

    private fun updateCircularProgress(progressPercentage: Int) {
        // Check if there's an ongoing animation and cancel it
        progressAnimator?.cancel()
        
        // Only animate if the view is visible
        if (circularProgressDrawable?.visibility != View.VISIBLE) {
            circularProgressDrawable?.setProgress(progressPercentage.toDouble(), 100.0)
            return
        }
        
        // Initialize the animator with the new progress percentage
        val currentProgress = circularProgressDrawable?.progress?.toFloat() ?: 0f
        progressAnimator = ValueAnimator.ofFloat(currentProgress, progressPercentage.toFloat()).apply {
            duration = 500 // Shorter duration to reduce rendering load
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                circularProgressDrawable?.setProgress(animatedValue.toDouble(), 100.0)
            }
            start()
        }
    }

}