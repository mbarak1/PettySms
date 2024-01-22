package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityViewAllTransactionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ViewAllTransactionsActivity : AppCompatActivity(), SortFilterDialogFragment.OnApplyClickListener {
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var binding: ActivityViewAllTransactionsBinding
    private lateinit var loadingDialog: AlertDialog
    private lateinit var loadingText: TextView
    private lateinit var clearAllTextView: TextView
    private lateinit var progressSuggestions: ProgressBar
    private lateinit var noResultsTextView: TextView


    private var allMpesaTransactions = mutableListOf<MpesaTransaction>()
    private var db_helper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var searchHistory = mutableListOf<String>()
    private var keyValueMapToFilterAndSortFragment = mutableMapOf<String, MutableList<String>>()





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAllTransactionsBinding.inflate(layoutInflater)
        searchBar = binding.root.findViewById(R.id.search_bar)
        searchView = binding.searchView
        clearAllTextView = binding.clearAllHistoryLink
        progressSuggestions = binding.progressBarSuggestions
        noResultsTextView = binding.noResultsTextView

        clearAllTextView.setOnClickListener {
            clearAllHistory()
        }

        searchBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_bar_options -> {
                    // Handle the "Settings" menu item click
                    showDialog(keyValueMapToFilterAndSortFragment)
                    //showSecondaryMenu(menuItem)
                    true
                }
                // Add more cases for other menu items if needed
                else -> false
            }
        }

        loadTransactions()
        loadSearchHistoryDb()



        /*swipeRefreshLayout = binding.swipeRefreshViewAllTransactions

        // Set a listener to handle the refresh action
        swipeRefreshLayout.setOnRefreshListener {

            swipeRefreshFunction()
        }

        // Customize the refresh indicator colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.aqua_color,
            android.R.color.holo_green_light,
            R.color.orange_color,
            R.color.red_color,
            R.color.pink_color,
            R.color.purple_color,
            R.color.yellow_color
        )*/

        searchView
            .editText
            .setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                searchBar.text = searchView.text
                searchView.hide()
                false
            }

        setupSearchView()

        // Add a listener to the search view to detect visibility changes
        /*if (!searchView.isShowing){
            swipeRefreshLayout.isEnabled
        }*/

        println(searchView.isShowing)


        setContentView(binding.root)

    }

    private fun setupSearchView() {
        if(!searchHistory.isNullOrEmpty()){
            showHistoryitems("")
        }
        searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == KeyEvent.KEYCODE_ENTER) {
                val query = searchView.text.toString()
                if (!query.isNullOrBlank()) {
                    addToSearchHistory(query)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }

        searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                progressSuggestions.visibility = View.VISIBLE
                val searchText = editable?.toString() ?: ""
                performSearch(searchText)
            }
        })
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
    }

    private fun performSearch(query: String) {

        // Use Coroutine to avoid blocking the UI thread during the search
        // Show progress bar while loading suggestions
        if (!query.isNullOrEmpty()){
            progressSuggestions.visibility = View.VISIBLE
            binding.suggestionRecycler.visibility = View.GONE

        }else{
            progressSuggestions.visibility = View.GONE

        }
        hideNoResultsMessage()
        CoroutineScope(Dispatchers.Main).launch {

            // Simulate a delay for demonstration purposes
            delay(1000)

            var searchResults = emptyList<MpesaTransaction>()


            // Check if the query is empty
            if (!query.isNullOrEmpty()) {
                //addToSearchHistory(query)
                // Show progress bar while loading suggestions
                // Filter transactions based on the search query
                searchResults = allMpesaTransactions.filter { mpesaTransaction ->
                    val transactionDate = mpesaTransaction.transaction_date
                    val msgDate = mpesaTransaction.msg_date

                    // Use transaction_date if not empty, otherwise use msg_date
                    val dateToSearch = if (transactionDate.isNullOrEmpty()) msgDate else transactionDate

                    mpesaTransaction.transaction_type?.contains(
                        query,
                        ignoreCase = true
                    ) ?: false ||
                            mpesaTransaction.mpesa_depositor.toString()
                                .contains(query, ignoreCase = true) ||
                            mpesaTransaction.amount.toString().contains(query, ignoreCase = true) ||
                            /*mpesaTransaction.mpesa_code.toString()
                                .contains(query, ignoreCase = true) ?: false ||*/
                            mpesaTransaction.recipient?.name.toString()
                                .contains(query, ignoreCase = true) ||
                            mpesaTransaction.transaction_type.toString()
                                .contains(query, ignoreCase = true) ||
                            formatDate(dateToSearch.toString())
                                .contains(query, ignoreCase = true) ||
                            mpesaTransaction.sender?.name?.contains(
                                query,
                                ignoreCase = true
                            ) ?: false
                }

                // Hide progress bar when the task is complete
                binding.progressBarSuggestions.visibility = View.GONE

                // Show no results message if there are no search results
                if (searchResults.isEmpty()) {
                    showNoResultsMessage()
                }
                else{
                    hideNoResultsMessage()
                }

            }
            // Display search results in the RecyclerView within the SearchView
            val suggestionsAdapter = SuggestionsAdapter(searchResults, query) { suggestion ->
                // Handle the suggestion click (e.g., perform a specific action)
                // You can update the SearchView text or do something else
                //searchBar.text = query
                // You may also want to close the SearchView here
                //searchBar.hide()
            }

            // Set the adapter to the SearchView's suggestion recycler view
            binding.suggestionRecycler.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = suggestionsAdapter
                visibility = View.VISIBLE
            }

            progressSuggestions.visibility = View.GONE


            // Update the RecyclerView with search results
           // updateRecyclerView(searchResults)
        }

    }

    private fun addToSearchHistory(query: String) {
        // Remove the query if it already exists in the history
        searchHistory.remove(query)

        // Add the query to the beginning of the search history
        searchHistory.add(0, query)

        // Limit the search history to the latest 5 results
        if (searchHistory.size > 5) {
            searchHistory.removeAt(searchHistory.size - 1)
        }

        // Save the updated search history to the database
        addToSearchHistoryDb(query)


        showHistoryitems(query)


    }

    private fun showHistoryitems(query: String){

        // Create an instance of SearchHistoryAdapter with the click listener
        val historyAdapter = SearchHistoryAdapter(searchHistory) { clickedQuery ->
            // Handle the click event, for example, update the search view text
            searchView.setText(clickedQuery)
            hideKeyboard()
        }

        // Assuming you have a reference to the view
        val myView = binding.suggestionsLayout

        // Get the current layout parameters
        val layoutParams = myView.layoutParams as ViewGroup.MarginLayoutParams

        // Update the top margin (change 0 to the desired value)
        layoutParams.topMargin = 0

        // Apply the updated layout parameters
        myView.layoutParams = layoutParams
        binding.searchHistoryLabel.visibility = View.VISIBLE
        binding.historyLayout.visibility = View.VISIBLE

        // Clear the existing adapter data
        binding.searchHistoryRecycler.adapter = null

        // Update the RecyclerView with search history
        binding.searchHistoryRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
            visibility = View.VISIBLE
        }
    }

    private fun createLoadingDialog(): AlertDialog {
        // Create a custom view for the loading dialog
        val customView = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)

        // Show loading dialog
        loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(customView)
            .setCancelable(false)
            .create()

        return loadingDialog
    }


    private fun loadTransactions() {

        db_helper = DbHelper(this)
        db = db_helper?.writableDatabase

        // Create a custom view for the loading dialog
        val loadingDialog = createLoadingDialog()

        loadingDialog.show()

        // Start a coroutine for background tasks
        lifecycleScope.launch {
            // Simulate a delay for demonstration purposes
            delay(150)


            // Perform background tasks, such as loading transactions from the database
            allMpesaTransactions = db_helper?.getAllMpesaTransactions() ?: mutableListOf()

            updateMainRecyclerView(allMpesaTransactions)



            // Perform background tasks, such as loading transactions from the database
            // For example, you can call your existing function getAllMpesaTransactions()

            // Dismiss the loading dialog after loading transactions
            loadingDialog.dismiss()
        }
    }

    private fun updateMainRecyclerView(transactions: MutableList<MpesaTransaction>){
        val recyclerView: RecyclerView = binding.recyclerView
        val adapter = MpesaTransactionAdapter(transactions)

        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.adapter = adapter

        // Get reference to your empty message TextView
        val emptyMessage: TextView = binding.emptyMessage

        // Check the dataset size and toggle visibility accordingly
        if (adapter.itemCount == 0) {
            recyclerView.visibility = View.GONE
            emptyMessage.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyMessage.visibility = View.GONE
        }
    }



    private fun showSecondaryMenu() {
        val anchorView: View = searchBar.findViewById(R.id.search_bar_options)
        val popupMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            PopupMenu(this, anchorView, Gravity.END, 0, R.style.PopupMenuStyle)
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP_MR1")
            PopupMenu(this, anchorView)

        }
        popupMenu.inflate(R.menu.search_bar_options_menu)

        // Set a listener to the secondary menu items
        popupMenu.setOnMenuItemClickListener { secondaryMenuItem ->
            when (secondaryMenuItem.itemId) {
                R.id.menu_sort -> {
                    // Handle the first option in the secondary menu
                    true
                }
                // Add more cases for other secondary menu items if needed
                else -> false
            }
        }

        // Show the secondary menu
        popupMenu.show()
    }

    private fun addToSearchHistoryDb(query: String) {
        db_helper?.addToSearchHistory(query)
    }

    private fun loadSearchHistoryDb() {
        val allSearchHistory = db_helper?.getSearchHistory() ?: emptyList()

        // Create a set to store unique search history items
        val uniqueSearchHistorySet = allSearchHistory.toMutableSet()

        // Take the first 5 items from the set and convert it to a mutable list
        searchHistory = uniqueSearchHistorySet.take(5).toMutableList()
    }

    /*private fun swipeRefreshFunction() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Update your data here

            // Stop the refresh animation
            swipeRefreshLayout.isRefreshing = false
        }, 2000) // Simulate a delay
    }*/

    fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US)

        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    fun clearAllHistory() {
        // Clear all items from the search history
        searchHistory.clear()

        // Update the UI to reflect the changes
        updateSearchHistoryUI()

        // Also, clear the history from the database
        db_helper?.clearSearchHistory()
    }

    private fun updateSearchHistoryUI() {

        binding.searchHistoryRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = SearchHistoryAdapter(searchHistory) { query ->
                // Handle the click event, for example, update the search view text
                searchView.setText(query)
            }
            visibility = View.VISIBLE
        }

        // Update visibility based on whether there are items in the search history
        binding.searchHistoryLabel.visibility = if (searchHistory.isNotEmpty()) View.VISIBLE else View.GONE
        binding.historyLayout.visibility = if (searchHistory.isNotEmpty()) View.VISIBLE else View.GONE
        binding.quickResultsLabel.visibility = View.VISIBLE

        // Assuming you have a reference to the view
        val myView = binding.suggestionsLayout

        // Get the current layout parameters
        val layoutParams = myView.layoutParams as ViewGroup.MarginLayoutParams

        // Update the top margin (change 0 to the desired value)
        layoutParams.topMargin = 30

        // Apply the updated layout parameters
        myView.layoutParams = layoutParams
        // Update the RecyclerView with search history
    }

    private fun showNoResultsMessage() {
        // Display a TextView in the middle of the screen indicating no results
        noResultsTextView.text = "No results found"
        noResultsTextView.visibility = View.VISIBLE // Show the view
    }

    private fun hideNoResultsMessage() {
        // Remove the TextView from the root layout
        noResultsTextView.visibility = View.GONE // Hide the view
    }

    fun showDialog(mapToSend: MutableMap<String, MutableList<String>>) {
        val dialog = SortFilterDialogFragment()

        // Create a Bundle and add the MutableMap as an argument
        if(!keyValueMapToFilterAndSortFragment.isNullOrEmpty()){
            val args = Bundle()
            args.putSerializable("yourMapKey", mapToSend as Serializable)
            dialog.arguments = args
        }

        dialog.setOnApplyClickListener(this)
        dialog.show(supportFragmentManager, "SortFilterDialogFragment")

    }

    override fun onApplyClick(keyValueMap: Map<String, List<String>>) {
        // Apply custom sorting based on the selected criteria
        var allMpesaTransactionsToSortAndFilter = allMpesaTransactions
        println("In the beginning: " + allMpesaTransactions.first().sms_text)
        val sortCriteria = keyValueMap["sort"] ?: mutableListOf()
        var filterTransactionTypes = keyValueMap["transaction_type"] ?: mutableListOf()
        var dateRange = keyValueMap["date"] ?: mutableListOf()

        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Send Money", "send_money")
        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Deposit", "deposit")
        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Paybill", "paybill")
        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Till No.", "till")
        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Reverse", "reverse")
        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Receival", "receival")
        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Withdrawal", "withdraw")
        filterTransactionTypes = replaceStringElement(filterTransactionTypes, "Topup", "topup")


        println("mafilter: " + dateRange)

        //filterTransactionsByTypes(allMpesaTransactionsToSortAndFilter, filterTransactionTypes)
        val loadingDialog = createLoadingDialog()

        loadingDialog.show()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    // Simulate a delay for demonstration purposes
                    delay(10)

                    if (!filterTransactionTypes.isNullOrEmpty()) {
                        filterTransactionTypes?.let { types ->
                            allMpesaTransactionsToSortAndFilter =
                                allMpesaTransactionsToSortAndFilter.filter { it.transaction_type in types }
                                    .toMutableList()
                        }
                    }

                    allMpesaTransactionsToSortAndFilter =
                        filterByDate(allMpesaTransactionsToSortAndFilter, dateRange)

                    allMpesaTransactionsToSortAndFilter = sortTransactionsByPermutations(allMpesaTransactionsToSortAndFilter, sortCriteria).toMutableList()
                }

                withContext(Dispatchers.Main) {
                    updateMainRecyclerView(allMpesaTransactionsToSortAndFilter)
                    loadingDialog.dismiss()
                    keyValueMapToFilterAndSortFragment = keyValueMap as MutableMap<String, MutableList<String>>
                }
            } catch (e: Exception) {
                // Handle exceptions if necessary
            }
        }


    }

    private fun filterByDate(
        allMpesaTransactionsToSortAndFilter: MutableList<MpesaTransaction>,
        dateRange: List<String>
    ): MutableList<MpesaTransaction> {
        var allTransactions = allMpesaTransactionsToSortAndFilter
        var startDate = ""
        var endDate = ""
        if (dateRange.first().toString() == "Any Time"){
            return allTransactions
        }
        else if (dateRange.first().toString() == "Today"){
            startDate = getCurrentDateInString()
            endDate = getCurrentDateInString()
        }
        else if (dateRange.first().toString() == "This Week"){
            startDate = getStartOfWeek()
            endDate = getCurrentDateInString()
        }
        else if (dateRange.first().toString() == "This Month"){
            startDate = getStartOfMonth()
            endDate = getCurrentDateInString()
        }
        else if (dateRange.first().toString() == "Last Month"){
            startDate = getLastMonthDates().first
            endDate = getLastMonthDates().second
        }
        else if (dateRange.first().toString() == "Last Six Months") {
            startDate = getStartOfLastSixMonths()
            endDate = getCurrentDateInString()
                //println("StartDate six m: " + startDate + "EndDateDate six m: " + endDate )
        }
        else{
            startDate = extractCustomDatesFromString(dateRange.first().toString())?.first.toString()
            endDate = extractCustomDatesFromString(dateRange.first().toString())?.second.toString()
            println("StartDate CUSTOM: " + startDate + "EndDateDate CUSTOM: " + endDate )

        }

        val filteredTransactions = allTransactions.filter { transaction ->
            var transactionDate = transaction.transaction_date
            if (transactionDate.isNullOrEmpty() || transactionDate.toString() == ""){
                transactionDate = transaction.msg_date
            }
            // Check if the transaction date is within the specified range
            isDateInRange(transactionDate.toString(), startDate, endDate)
        }

        /*startDate?.let { start ->
            endDate?.let { end ->
                allTransactions =
                    allTransactions.filter { ((it.transaction_date ?: it.msg_date)!!) in start..end }.toMutableList()
            }
        }*/

        return filteredTransactions.toMutableList()

    }

    // Function to check if a date is within a given date range
    fun isDateInRange(date: String, startDate: String, endDate: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val transactionDate = dateFormat.parse(date)
        val startDateParsed = dateFormat.parse(startDate)
        val endDateParsed = dateFormat.parse(endDate)

        return transactionDate in startDateParsed..endDateParsed
    }

    fun getCurrentDateInString(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = Date(System.currentTimeMillis())
        return dateFormat.format(currentDate)
    }

    fun getStartOfWeek(): String {
        val calendar = Calendar.getInstance()

        // Set the calendar to the current date
        calendar.time = Date()

        // Set the calendar to the beginning of the week
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        // Format the date as "dd/MM/yyyy"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
    fun getStartOfMonth(): String {
        val calendar = Calendar.getInstance()

        // Set the calendar to the current date
        calendar.time = Date()

        // Set the calendar to the beginning of the month
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        // Format the date as "dd/MM/yyyy"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun getLastMonthDates(): Pair<String, String> {
        val calendar = Calendar.getInstance()

        // Set the calendar to the current date
        calendar.time = Date()

        // Move to the first day of the current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        // Move to the previous month
        calendar.add(Calendar.MONTH, -1)

        // Get the first day of the last month
        val startOfMonth = calendar.time

        // Move to the last day of the last month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endOfMonth = calendar.time

        // Format the dates as "dd/MM/yyyy"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDate = dateFormat.format(startOfMonth)
        val endDate = dateFormat.format(endOfMonth)

        return Pair(startDate, endDate)
    }

    fun getStartOfLastSixMonths(): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date()

        // Subtract six months from the current date
        calendar.add(Calendar.MONTH, -6)

        // Set the day of the month to the first day (1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        // Format the date as "dd/MM/yyyy"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
    fun extractCustomDatesFromString(dateRange: String): Pair<String, String>? {
        // Split the date range string using the hyphen separator
        val dateParts = dateRange.split(" - ")

        // Ensure that the string has two parts
        if (dateParts.size == 2) {
            return Pair(dateParts.first(), dateParts.last())
        }

        // Return null if parsing is unsuccessful or the input is invalid
        return null
    }

    fun replaceStringElement(list: List<String>, stringToReplace: String, replacement: String): List<String> {
        return list.map { if (it == stringToReplace) replacement else it }
    }

    fun sortTransactionsByPermutations(transactions: MutableList<MpesaTransaction>, sortCriteria: List<String>): List<MpesaTransaction> {
        return transactions.sortedWith(Comparator { obj1, obj2 ->
            for (criteria in sortCriteria) {
                val result = when (criteria) {
                    "Date" -> {
                        //println("sort by date")
                        val date1 = parseDate(obj1.transaction_date) ?: parseDate(obj1.msg_date)
                        val date2 = parseDate(obj2.transaction_date) ?: parseDate(obj2.msg_date)
                        compareValues(date2, date1) // Compare dates in descending order
                    }
                    "Amount" -> {
                        //println("sort by amount")
                        compareValues(obj2.amount ?: 0.0, obj1.amount ?: 0.0)
                    } // Compare amounts
                    "Transactor" -> {
                        //println("sort by transactor")
                        compareValues("${obj1.recipient?.name}${obj1.sender?.name}${obj1.mpesa_depositor}",
                            "${obj2.recipient?.name}${obj2.sender?.name}${obj2.mpesa_depositor}")
                    }
                    // add other criteria as needed
                    else -> 0 // default to 0 if criteria is not recognized
                }

                if (result != 0) {
                    return@Comparator result
                }
            }

            return@Comparator 0 // default to 0 if all criteria are equal
        })

        /*
        val comparators = generateAllPermutations(sortCriteria)
            .map { permutation ->
                compareBy<MpesaTransaction> { transaction ->
                    for (criteria in permutation) {
                        when (criteria) {
                            "Date" -> {
                                println("sort by date")
                                val date1 = parseDate(transaction.transaction_date) ?: parseDate(transaction.msg_date)
                                val date2 = parseDate(transaction.transaction_date) ?: parseDate(transaction.msg_date)
                                if (date1 != null && date2 != null) {
                                    compareValues(date1, date2) // Compare dates in descending order
                                } else {
                                    0 // Default comparison if any date is null
                                }
                            }
                            "Amount" -> -transaction.amount!! // Reverse the order for "Amount"
                            "Transactor" -> "${transaction.recipient?.name}${transaction.sender?.name}${transaction.mpesa_depositor}"
                            // Add more criteria as needed
                            else -> null // Handle other cases
                        }?.let { return@compareBy it }
                    }
                    0 // Default comparison if all criteria are null
                }
            }

        transactions.sortWith(comparators.reduce { acc, comparator -> acc.then(comparator) })

         */
    }

// Rest of the code remains the same...

    // Add this function to handle date parsing with null check
    fun parseDate(dateString: String?): Date? {
        if (!dateString.isNullOrBlank()) {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                return dateFormat.parse(dateString)
            } catch (e: ParseException) {
                // Handle parse exception
            }
        }
        return null
    }

    fun generateAllPermutations(criteria: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()

        fun generate(current: List<String>, remaining: List<String>) {
            if (remaining.isEmpty()) {
                result.add(current.toList())
            } else {
                for (i in remaining.indices) {
                    generate(current + remaining[i], remaining.subList(0, i) + remaining.subList(i + 1, remaining.size))
                }
            }
        }

        generate(emptyList(), criteria)
        return result
    }

}