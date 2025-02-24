package com.example.pettysms

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityViewAllPettyCashBinding
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import android.util.Log
import com.google.android.material.color.MaterialColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.gson.Gson
import android.content.Intent
import android.content.BroadcastReceiver
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.os.bundleOf
import android.content.Context
import android.content.IntentFilter

class ViewAllPettyCashActivity : AppCompatActivity(), 
    PettyCashSortFilterDialogFragment.OnApplyClickListener,
    AddPettyCashFragment.OnAddPettyCashListener {
    private var _binding: ActivityViewAllPettyCashBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DbHelper
    private var pettyCashAdapter: ViewAllPettyCashAdapter? = null
    
    private val pageSize = 20
    private var currentPage = 1
    private var isLoading = false
    private var hasMoreData = true
    private var allPettyCash = mutableListOf<PettyCash>()
    private var currentSortOption: String = "Date"
    private var currentDateFilter: String = "Any Time"
    private var currentPaymentModes: List<String> = emptyList()
    private var customStartDate: String? = null
    private var customEndDate: String? = null

    private var searchJob: Job? = null

    // Add broadcast receiver
    private val pettyCashDeleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "pettycash_deleted_action") {
                Log.d("ViewAllPettyCash", "Received petty cash delete broadcast")
                
                // Handle both main petty cash and transaction cost deletions
                val idsToDelete = listOfNotNull(
                    intent.getIntExtra("deleted_petty_cash_id", -1).takeIf { it != -1 },
                    intent.getIntExtra("deleted_transaction_cost_id", -1).takeIf { it != -1 }
                )
                
                if (idsToDelete.isNotEmpty()) {
                    try {
                        // Remove all deleted items from lists and adapters
                        idsToDelete.forEach { deletedId ->
                            Log.d("ViewAllPettyCash", "Processing deletion for ID: $deletedId")
                            
                            // Find and remove from allPettyCash list
                            val position = allPettyCash.indexOfFirst { it.id == deletedId }
                            Log.d("ViewAllPettyCash", "Position of deleted item: $position")

                            if (position != -1) {
                                allPettyCash.removeAt(position)
                                // Use the correct ID for each removal
                                pettyCashAdapter?.removeItem(deletedId)
                                (binding.suggestionRecycler.adapter as? ViewAllPettyCashAdapter)?.removeItem(deletedId)
                                Log.d("ViewAllPettyCash", "Removed item $deletedId at position $position")
                            } else {
                                Log.d("ViewAllPettyCash", "Item $deletedId not found in current page")
                            }
                        }
                        
                        // Reload once after all deletions
                        loadPettyCash()
                        
                    } catch (e: Exception) {
                        Log.e("ViewAllPettyCash", "Error handling deletion: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityViewAllPettyCashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add toolbar setup
        setSupportActionBar(binding.searchBar)

        dbHelper = DbHelper(this)
        setupViews()
        loadPettyCash()

        // Register for petty cash updates
        registerForPettyCashUpdates()

        // Register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            pettyCashDeleteReceiver,
            IntentFilter("pettycash_deleted_action")
        )
    }

    private fun setupViews() {
        // Setup SearchBar and SearchView
        binding.searchBar.setHint("Search petty cash")
        binding.searchView.setupWithSearchBar(binding.searchBar)
        setupSearchView()

        // Setup RecyclerView
        setupRecyclerView()
    }

    private fun setupSearchView() {
        // Create a separate adapter for suggestions
        val suggestionAdapter = ViewAllPettyCashAdapter(
            context = this,
            pettyCashList = mutableListOf(),
            fragmentManager = supportFragmentManager
        )

        // Setup suggestion recycler view
        binding.suggestionRecycler.apply {
            layoutManager = LinearLayoutManager(this@ViewAllPettyCashActivity)
            adapter = suggestionAdapter
            setHasFixedSize(true)
            visibility = View.GONE
        }

        binding.searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Cancel any existing search job
                searchJob?.cancel()
                
                val query = s?.toString() ?: ""
                // Start a new search job with debouncing
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce for 300ms
                    searchPettyCash(query)
                }
            }
        })

        // Add SearchView transition listener
        binding.searchView.addTransitionListener { searchView, previousState, newState ->
            when (newState) {
                SearchView.TransitionState.SHOWING -> {
                    binding.suggestionRecycler.visibility = View.VISIBLE
                    binding.pettyCashRecyclerView.visibility = View.GONE
                }
                SearchView.TransitionState.HIDDEN -> {
                    binding.suggestionRecycler.visibility = View.GONE
                    binding.noResultsTextView.visibility = View.GONE
                    binding.progressBarSuggestions.visibility = View.GONE
                    binding.pettyCashRecyclerView.visibility = View.VISIBLE
                }
                SearchView.TransitionState.HIDING -> {
                    binding.searchView.editText.text = null
                    binding.pettyCashRecyclerView.visibility = View.VISIBLE
                    pettyCashAdapter?.setSearchQuery("")
                    Log.d("ViewAllPettyCash", "Size of allPettyCash: ${allPettyCash.size}")
                    pettyCashAdapter?.updateData(allPettyCash)
                }
                else -> {}
            }
        }

        val searchFocusedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer,"")
        val defaultStatusBarColor = this.window.statusBarColor

        binding.searchView.addTransitionListener { searchView: com.google.android.material.search.SearchView?, previousState: SearchView.TransitionState?, newState: SearchView.TransitionState ->
            if (newState == SearchView.TransitionState.SHOWING) {
                // Change the status bar color to match the SearchView's surface color
                this.window.statusBarColor = searchFocusedColor
                println("is focused")
            }
            else if (newState == SearchView.TransitionState.HIDDEN){
                // Revert the status bar color to the default color
                this.window.statusBarColor = defaultStatusBarColor
                println("is not focused")
            }
        }
        
    }

    private fun searchPettyCash(query: String) {
        Log.d("ViewAllPettyCash", """
            Search started:
            Device: ${android.os.Build.MODEL}
            Android Version: ${android.os.Build.VERSION.SDK_INT}
            Query: '$query'
            Memory: ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB
        """.trimIndent())
        
        if (query.isEmpty()) {
            Log.d("ViewAllPettyCash", "Empty query, showing all petty cash")
            binding.progressBarSuggestions.visibility = View.GONE
            binding.noResultsTextView.visibility = View.GONE
            binding.suggestionRecycler.visibility = View.GONE
            binding.pettyCashRecyclerView.visibility = View.VISIBLE
            return
        }

        binding.progressBarSuggestions.visibility = View.VISIBLE
        binding.noResultsTextView.visibility = View.GONE
        binding.suggestionRecycler.visibility = View.VISIBLE
        binding.pettyCashRecyclerView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("ViewAllPettyCash", "Executing search query: $query")
                val searchResults = dbHelper.searchPettyCash(query)
                
                Log.d("ViewAllPettyCash", "Search results for '$query': ${searchResults?.size ?: 0} items")

                withContext(Dispatchers.Main) {
                    binding.progressBarSuggestions.visibility = View.GONE
                    if (searchResults.isNullOrEmpty()) {
                        Log.d("ViewAllPettyCash", "No results found for: $query")
                        binding.noResultsTextView.visibility = View.VISIBLE
                        binding.suggestionRecycler.visibility = View.GONE
                        binding.pettyCashRecyclerView.visibility = View.GONE
                    } else {
                        Log.d("ViewAllPettyCash", "Showing ${searchResults.size} results for: $query")
                        binding.noResultsTextView.visibility = View.GONE
                        binding.suggestionRecycler.visibility = View.VISIBLE
                        binding.pettyCashRecyclerView.visibility = View.GONE
                        
                        // Update suggestion adapter
                        (binding.suggestionRecycler.adapter as? ViewAllPettyCashAdapter)?.apply {
                            setSearchQuery(query)
                            updateData(searchResults)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewAllPettyCash", "Error during search for '$query': ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressBarSuggestions.visibility = View.GONE
                    Toast.makeText(
                        this@ViewAllPettyCashActivity,
                        "Error searching: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        pettyCashAdapter = ViewAllPettyCashAdapter(
            context = this,
            pettyCashList = mutableListOf(),
            fragmentManager = supportFragmentManager
        )

        // Set the activity as the listener for the adapter
        pettyCashAdapter?.setOnAddPettyCashListener(this)

        (binding.pettyCashRecyclerView as FastScrollRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@ViewAllPettyCashActivity)
            adapter = pettyCashAdapter
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && hasMoreData) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadMorePettyCash()
                        }
                    }
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.search_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_bar_options -> {
                showSortFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortFilterDialog() {
        val dialogFragment = PettyCashSortFilterDialogFragment().apply {
            setOnApplyClickListener(this@ViewAllPettyCashActivity)
        }
        dialogFragment.show(supportFragmentManager, "SortFilterDialog")
    }

    override fun onApplyClick(keyValueMap: Map<String, List<String>>) {
        // Handle the sort and filter options
        currentSortOption = keyValueMap["sort"]?.firstOrNull() ?: "Date"
        currentDateFilter = keyValueMap["date"]?.firstOrNull() ?: "Any Time"
        currentPaymentModes = keyValueMap["payment_mode"] ?: emptyList()
        
        // Get custom date range from the filter string
        if (currentDateFilter.contains(" - ")) {
            val dates = currentDateFilter.split(" - ")
            if (dates.size == 2) {
                customStartDate = dates[0].trim()
                customEndDate = dates[1].trim()
                currentDateFilter = "Custom Range"  // Set to Custom Range to trigger proper filtering
            }
        } else {
            customStartDate = null
            customEndDate = null
        }

        Log.d("ViewAllPettyCash", """
            Applying Filters:
            Sort: $currentSortOption
            Date Filter: $currentDateFilter
            Payment Modes: $currentPaymentModes
            Custom Start Date: $customStartDate
            Custom End Date: $customEndDate
            Raw Filter String: ${keyValueMap["date"]?.firstOrNull()}
        """.trimIndent())

        // Reset to first page when applying new filters
        currentPage = 1
        allPettyCash.clear()

        loadPettyCashWithFilters(currentSortOption, currentDateFilter, currentPaymentModes)
    }

    private fun loadPettyCashWithFilters(
        sortOption: String?,
        dateFilter: String?,
        paymentModes: List<String>
    ) {
        isLoading = true
        binding.progressBarSuggestions.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pettyCashList = dbHelper.getAllPettyCash(
                    page = currentPage,
                    pageSize = pageSize,
                    sortBy = currentSortOption,
                    dateFilter = dateFilter ?: "Any Time",
                    paymentModes = currentPaymentModes ?: emptyList(), // Get all payment modes, we'll filter in memory
                    customStartDate = customStartDate,
                    customEndDate = customEndDate
                )

                withContext(Dispatchers.Main) {
                    binding.progressBarSuggestions.visibility = View.GONE
                    if (pettyCashList.isNullOrEmpty()) {
                        if (currentPage == 1) {
                            binding.noPettyCashMessage.visibility = View.VISIBLE
                            binding.pettyCashRecyclerView.visibility = View.GONE
                        }
                    } else {
                        // First filter by date
                        val dateFiltered = pettyCashList.filter { pettyCash ->
                            when (dateFilter) {
                                "Today" -> isToday(pettyCash.date)
                                "This Week" -> isThisWeek(pettyCash.date)
                                "This Month" -> isThisMonth(pettyCash.date)
                                "Last Month" -> isLastMonth(pettyCash.date)
                                "Last Six Months" -> isLastSixMonths(pettyCash.date)
                                "Custom Range" -> isInCustomDateRange(pettyCash.date)
                                else -> true
                            }
                        }

                        // Then filter by payment mode
                        val modeFiltered = if (paymentModes.isNotEmpty()) {
                            dateFiltered.filter { pettyCash ->
                                paymentModes.contains(pettyCash.paymentMode)
                            }
                        } else dateFiltered

                        // Finally sort the filtered results
                        val sortedList = when (currentSortOption) {
                            "Transactor" -> modeFiltered.sortedBy { it.transactor?.name }
                            else -> modeFiltered
                        }

                        if (sortedList.isEmpty()) {
                            binding.noPettyCashMessage.visibility = View.VISIBLE
                            binding.pettyCashRecyclerView.visibility = View.GONE
                        } else {
                            binding.noPettyCashMessage.visibility = View.GONE
                            binding.pettyCashRecyclerView.visibility = View.VISIBLE
                            allPettyCash = sortedList.toMutableList()
                            pettyCashAdapter?.updateData(allPettyCash)
                            hasMoreData = pettyCashList.size >= pageSize
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewAllPettyCash", "Error loading data", e)
                withContext(Dispatchers.Main) {
                    binding.progressBarSuggestions.visibility = View.GONE
                    Toast.makeText(
                        this@ViewAllPettyCashActivity,
                        "Error loading data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadPettyCash() {
        if (isLoading) return
        isLoading = true
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // If we're reloading due to deletion, load all pages up to current page
                val itemsToLoad = mutableListOf<PettyCash>()
                
                for (page in 1..currentPage) {
                    val pageItems = dbHelper.getAllPettyCash(
                        page,
                        pageSize,
                        currentSortOption,
                        currentDateFilter,
                        currentPaymentModes,
                        customStartDate,
                        customEndDate
                    ) ?: emptyList()
                    
                    if (pageItems.isEmpty() && page == 1) {
                        // No data at all
                        hasMoreData = false
                        break
                    } else if (pageItems.isEmpty()) {
                        // Reached end of data
                        hasMoreData = false
                        currentPage = page - 1
                        break
                    }
                    
                    itemsToLoad.addAll(pageItems)
                }

                withContext(Dispatchers.Main) {
                    allPettyCash.clear()
                    allPettyCash.addAll(itemsToLoad)
                    pettyCashAdapter?.updateList(allPettyCash)
                    isLoading = false
                    
                    Log.d("ViewAllPettyCash", """
                        Loaded petty cash:
                        Current page: $currentPage
                        Total items: ${allPettyCash.size}
                        Has more: $hasMoreData
                    """.trimIndent())
                }
            } catch (e: Exception) {
                Log.e("ViewAllPettyCash", "Error loading petty cash: ${e.message}")
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(this@ViewAllPettyCashActivity, 
                        "Error loading data: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadMorePettyCash() {
        if (isLoading || !hasMoreData) return
        
        isLoading = true
        currentPage++
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val morePettyCash = dbHelper.getAllPettyCash(
                    page = currentPage,
                    pageSize = pageSize,
                    sortBy = currentSortOption ?: "Date",
                    dateFilter = currentDateFilter,
                    paymentModes = currentPaymentModes ?: emptyList(), // Get all, filter in memory
                    customStartDate = customStartDate,
                    customEndDate = customEndDate
                )
                
                withContext(Dispatchers.Main) {
                    if (!morePettyCash.isNullOrEmpty()) {
                        // First filter by date
                        val dateFiltered = morePettyCash.filter { pettyCash ->
                            when (currentDateFilter) {
                                "Today" -> isToday(pettyCash.date)
                                "This Week" -> isThisWeek(pettyCash.date)
                                "This Month" -> isThisMonth(pettyCash.date)
                                "Last Month" -> isLastMonth(pettyCash.date)
                                "Last Six Months" -> isLastSixMonths(pettyCash.date)
                                "Custom Range" -> isInCustomDateRange(pettyCash.date)
                                else -> true
                            }
                        }

                        // Then filter by payment mode
                        val modeFiltered = if (currentPaymentModes.isNotEmpty()) {
                            dateFiltered.filter { pettyCash ->
                                currentPaymentModes.contains(pettyCash.paymentMode)
                            }
                        } else dateFiltered

                        if (modeFiltered.isNotEmpty()) {
                            // Sort the filtered results
                            val sortedMorePettyCash = when (currentSortOption) {
                                "Transactor" -> modeFiltered.sortedBy { it.transactor?.name }
                                else -> modeFiltered
                            }
                            
                            pettyCashAdapter?.addMoreItems(sortedMorePettyCash)
                            hasMoreData = morePettyCash.size >= pageSize
                        } else {
                            hasMoreData = false
                        }
                    } else {
                        hasMoreData = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewAllPettyCashActivity, 
                        "Error loading more data: ${e.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Helper functions to check dates
    private fun isThisWeek(dateStr: String?): Boolean {
        if (dateStr == null) return false
        try {
            val date = LocalDate.parse(
                dateStr.substring(0, 10),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
            val today = LocalDate.now()
            val weekStart = today.with(DayOfWeek.MONDAY)
            val weekEnd = today.with(DayOfWeek.SUNDAY)
            return !date.isBefore(weekStart) && !date.isAfter(weekEnd)
        } catch (e: Exception) {
            Log.e("ViewAllPettyCash", "Error parsing date: $dateStr", e)
            return false
        }
    }

    private fun isToday(dateStr: String?): Boolean {
        if (dateStr == null) return false
        try {
            val date = LocalDate.parse(
                dateStr.substring(0, 10),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
            return date == LocalDate.now()
        } catch (e: Exception) {
            Log.e("ViewAllPettyCash", "Error parsing date: $dateStr", e)
            return false
        }
    }

    // Helper functions for date filtering
    private fun isThisMonth(dateStr: String?): Boolean {
        if (dateStr == null) return false
        try {
            val date = LocalDate.parse(
                dateStr.substring(0, 10),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
            val today = LocalDate.now()
            return date.year == today.year && date.month == today.month
        } catch (e: Exception) {
            Log.e("ViewAllPettyCash", "Error parsing date: $dateStr", e)
            return false
        }
    }

    private fun isLastMonth(dateStr: String?): Boolean {
        if (dateStr == null) return false
        try {
            val date = LocalDate.parse(
                dateStr.substring(0, 10),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
            val lastMonth = LocalDate.now().minusMonths(1)
            return date.year == lastMonth.year && date.month == lastMonth.month
        } catch (e: Exception) {
            Log.e("ViewAllPettyCash", "Error parsing date: $dateStr", e)
            return false
        }
    }

    private fun isLastSixMonths(dateStr: String?): Boolean {
        if (dateStr == null) return false
        try {
            val date = LocalDate.parse(
                dateStr.substring(0, 10),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
            val sixMonthsAgo = LocalDate.now().minusMonths(6)
            return !date.isBefore(sixMonthsAgo) && !date.isAfter(LocalDate.now())
        } catch (e: Exception) {
            Log.e("ViewAllPettyCash", "Error parsing date: $dateStr", e)
            return false
        }
    }

    // Add new helper function for custom date range
    private fun isInCustomDateRange(dateStr: String?): Boolean {
        if (dateStr == null || customStartDate == null || customEndDate == null) {
            Log.d("ViewAllPettyCash", "Null date values: dateStr=$dateStr, start=$customStartDate, end=$customEndDate")
            return false
        }
        
        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val date = LocalDate.parse(dateStr.substring(0, 10), formatter)
            val startDate = LocalDate.parse(customStartDate, formatter)
            val endDate = LocalDate.parse(customEndDate, formatter)
            
            val isInRange = !date.isBefore(startDate) && !date.isAfter(endDate)
            
            Log.d("ViewAllPettyCash", """
                Date Check:
                Checking Date: $date
                Start Date: $startDate
                End Date: $endDate
                Is In Range: $isInRange
            """.trimIndent())
            
            return isInRange
        } catch (e: Exception) {
            Log.e("ViewAllPettyCash", "Error parsing date for custom range: $dateStr", e)
            Log.e("ViewAllPettyCash", "Start date: $customStartDate, End date: $customEndDate")
            return false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Safely cleanup binding and adapter
        if (_binding != null) {
            _binding?.pettyCashRecyclerView?.adapter = null
            pettyCashAdapter = null
            _binding = null
        }

        // Unregister receiver
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(pettyCashDeleteReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save any necessary state
        outState.putInt("currentPage", currentPage)
        outState.putBoolean("hasMoreData", hasMoreData)
        outState.putBoolean("isLoading", isLoading)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore saved state
        currentPage = savedInstanceState.getInt("currentPage", 1)
        hasMoreData = savedInstanceState.getBoolean("hasMoreData", true)
        isLoading = savedInstanceState.getBoolean("isLoading", false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes if needed
    }

    private fun registerForPettyCashUpdates() {
        supportFragmentManager.setFragmentResultListener(
            "pettyCash_updated",
            this
        ) { _, bundle ->
            val updatedPettyCashJson = bundle.getString("updated_petty_cash")
            updatedPettyCashJson?.let { json ->
                val updatedPettyCash = Gson().fromJson(json, PettyCash::class.java)
                updatePettyCashInAdapter(updatedPettyCash)
            }
        }
    }

    private fun updatePettyCashInAdapter(updatedPettyCash: PettyCash) {
        // Update in suggestions adapter
        (binding.suggestionRecycler.adapter as? ViewAllPettyCashAdapter)?.let { adapter ->
            adapter.updateItem(updatedPettyCash)
        }

        // Update in main adapter
        pettyCashAdapter?.updateItem(updatedPettyCash)

        // Update in allPettyCash list
        val index = allPettyCash.indexOfFirst { it.id == updatedPettyCash.id }
        if (index != -1) {
            allPettyCash[index] = updatedPettyCash
        }
    }

    // Implement all required interface methods
    override fun onAddPettyCash(newOrUpdatedItem: PettyCash, newOrUpdatetransactionCost: PettyCash?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    // Update main petty cash
                    pettyCashAdapter?.updateItem(newOrUpdatedItem)
                    
                    // Update transaction cost if exists
                    newOrUpdatetransactionCost?.let { tc ->
                        pettyCashAdapter?.updateItem(tc)
                    }

                    // Update suggestion adapter if visible
                    (binding.suggestionRecycler.adapter as? ViewAllPettyCashAdapter)?.let { adapter ->
                        adapter.updateItem(newOrUpdatedItem)
                        newOrUpdatetransactionCost?.let { adapter.updateItem(it) }
                    }

                    Log.d("ViewAllPettyCash", "Updated items in adapter")
                }
            } catch (e: Exception) {
                Log.e("ViewAllPettyCash", "Error in onAddPettyCash: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewAllPettyCashActivity,
                        "Error updating petty cash: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun refreshData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val layoutManager = binding.pettyCashRecyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

                // Get only the current page data
                val updatedList = dbHelper.getAllPettyCash(
                    currentPage,
                    pageSize,
                    currentSortOption,
                    currentDateFilter,
                    currentPaymentModes,
                    customStartDate,
                    customEndDate
                )

                withContext(Dispatchers.Main) {
                    updatedList?.let { newItems ->
                        // Update only the items in the current page
                        pettyCashAdapter?.updateData(newItems)
                        layoutManager.scrollToPosition(firstVisiblePosition)
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewAllPettyCash", "Error refreshing data: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewAllPettyCashActivity,
                        "Error refreshing data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("ViewAllPettyCash", "onActivityResult called")

        try {
            if (requestCode == ViewAllPettyCashAdapter.PETTY_CASH_VIEWER_REQUEST && resultCode == Activity.RESULT_OK) {
                data?.let { intent ->
                    val pettyCashId = intent.getIntExtra("updated_petty_cash_id", -1)
                    val transactionCostId = intent.getIntExtra("transaction_cost_id", -1)

                    if (pettyCashId != -1) {
                        // Record updated IDs in PettyCashFragment
                        PettyCashFragment.updatedPettyCashIds.add(pettyCashId)
                        if (transactionCostId != -1) {
                            PettyCashFragment.updatedPettyCashIds.add(transactionCostId)
                        }

                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Fetch updated petty cash from database
                                val updatedPettyCash = dbHelper.getPettyCashById(pettyCashId)
                                
                                // Fetch transaction cost if present
                                val transactionCost = if (transactionCostId != -1) {
                                    dbHelper.getPettyCashById(transactionCostId)
                                } else null

                                withContext(Dispatchers.Main) {
                                    // Update UI with fetched data
                                    updatedPettyCash?.let { pettyCash ->
                                        pettyCashAdapter?.updateItem(pettyCash)
                                        
                                        val index = allPettyCash.indexOfFirst { it.id == pettyCash.id }
                                        if (index != -1) {
                                            allPettyCash[index] = pettyCash
                                        }
                                    }

                                    transactionCost?.let { tc ->
                                        pettyCashAdapter?.updateItem(tc)
                                        
                                        val index = allPettyCash.indexOfFirst { it.id == tc.id }
                                        if (index != -1) {
                                            allPettyCash[index] = tc
                                        }
                                    }

                                    // Update suggestion adapter if visible
                                    (binding.suggestionRecycler.adapter as? ViewAllPettyCashAdapter)?.let { adapter ->
                                        updatedPettyCash?.let { adapter.updateItem(it) }
                                        transactionCost?.let { adapter.updateItem(it) }
                                    }

                                    // Refresh the data to ensure everything is in sync
                                    refreshData()
                                }
                            } catch (e: Exception) {
                                Log.e("ViewAllPettyCash", "Error updating database: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@ViewAllPettyCashActivity,
                                        "Error updating petty cash: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ViewAllPettyCash", "Error in onActivityResult: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun openAddPettyCashFragment(pettyCash: PettyCash, actionType: String) {
        val fragment = AddPettyCashFragment.newInstance(pettyCash, actionType)
        fragment.show(supportFragmentManager, "fragment_add_petty_cash")
    }
} 