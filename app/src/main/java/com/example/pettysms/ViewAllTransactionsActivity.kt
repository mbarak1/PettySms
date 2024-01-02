package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ViewUtils
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.pettysms.databinding.ActivityViewAllTransactionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ViewAllTransactionsActivity : AppCompatActivity() {
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
                    showSecondaryMenu()
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
                    performSearch(query)
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

    private fun loadTransactions() {

        db_helper = DbHelper(this)
        db = db_helper?.writableDatabase

        // Create a custom view for the loading dialog
        val customView = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)

        // Show loading dialog
        loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(customView)
            .setCancelable(false)
            .create()

        loadingDialog.show()

        // Start a coroutine for background tasks
        lifecycleScope.launch {
            // Simulate a delay for demonstration purposes
            delay(150)


            // Perform background tasks, such as loading transactions from the database
            allMpesaTransactions = db_helper?.getAllMpesaTransactions() ?: mutableListOf()

            val recyclerView: RecyclerView = binding.recyclerView
            val adapter = MpesaTransactionAdapter(allMpesaTransactions)

            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            recyclerView.adapter = adapter



            // Perform background tasks, such as loading transactions from the database
            // For example, you can call your existing function getAllMpesaTransactions()

            // Dismiss the loading dialog after loading transactions
            loadingDialog.dismiss()
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
        val uniqueSearchHistorySet = mutableSetOf<String>()

        // Add all items to the set to ensure uniqueness
        uniqueSearchHistorySet.addAll(allSearchHistory)

        // Convert the set to a list (preserving order of insertion)
        searchHistory = uniqueSearchHistorySet.toList() as MutableList<String>

        // Take the first 5 items from the list
        searchHistory = searchHistory.take(5).toMutableList()
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







}