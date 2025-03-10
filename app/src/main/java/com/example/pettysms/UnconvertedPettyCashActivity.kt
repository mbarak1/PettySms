package com.example.pettysms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityUnconvertedPettyCashBinding
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchView
import com.google.gson.Gson
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UnconvertedPettyCashActivity : AppCompatActivity(),
    PettyCashSortFilterDialogFragment.OnApplyClickListener,
    AddPettyCashFragment.OnAddPettyCashListener, RefreshRecyclerViewCallback {

    private var _binding: ActivityUnconvertedPettyCashBinding? = null
    private val binding get() = _binding!!
    private var fab: ExtendedFloatingActionButton? = null

    private lateinit var dbHelper: DbHelper
    private var pettyCashAdapter: ViewAllPettyCashAdapter? = null

    private val pageSize = 20
    private var currentPage = 1
    private var isLoading = false
    private var hasMoreData = true
    private var allPettyCash = mutableListOf<PettyCash>()
    private var currentSortOption: String = "Date"
    private var currentDateFilter: String = "This Month"
    private var currentPaymentModes: List<String> = emptyList()
    private var customStartDate: String? = null
    private var customEndDate: String? = null

    private var searchJob: Job? = null

    companion object {
        var isActivityVisible = false
    }

    object CallbackSingleton {
        var refreshCallback: RefreshRecyclerViewCallback? = null
    }

    // Add broadcast receiver for petty cash updates
    private val pettyCashUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "pettycash_deleted_action") {
                Log.d("UnconvertedPettyCash", "Received petty cash delete broadcast")

                // Handle both main petty cash and transaction cost deletions
                val idsToDelete = listOfNotNull(
                    intent.getIntExtra("deleted_petty_cash_id", -1).takeIf { it != -1 },
                    intent.getIntExtra("deleted_transaction_cost_id", -1).takeIf { it != -1 }
                )

                if (idsToDelete.isNotEmpty()) {
                    try {
                        // Remove all deleted items from lists and adapters
                        idsToDelete.forEach { deletedId ->
                            Log.d("UnconvertedPettyCash", "Processing deletion for ID: $deletedId")

                            // Find and remove from allPettyCash list
                            val position = allPettyCash.indexOfFirst { it.id == deletedId }
                            Log.d("UnconvertedPettyCash", "Position of deleted item: $position")

                            if (position != -1) {
                                allPettyCash.removeAt(position)
                                // Use the correct ID for each removal
                                pettyCashAdapter?.removeItem(deletedId)
                                (binding.suggestionRecycler.adapter as? ViewAllPettyCashAdapter)?.removeItem(deletedId)
                                Log.d("UnconvertedPettyCash", "Removed item $deletedId at position $position")
                            } else {
                                Log.d("UnconvertedPettyCash", "Item $deletedId not found in current page")
                            }
                        }

                        // Reload once after all deletions
                        loadPettyCash()

                    } catch (e: Exception) {
                        Log.e("UnconvertedPettyCash", "Error handling deletion: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityUnconvertedPettyCashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the activity title programmatically
        title = "Unconverted Petty Cash"

        // Add toolbar setup - SearchBar doesn't support being used as ActionBar,
        // so we'll just use it as a regular view
        binding.searchBar.setHint("Search unconverted petty cash")
        
        // Setup back navigation
        binding.searchBar.setNavigationIcon(R.drawable.arrow_back_24px)
        binding.searchBar.setNavigationOnClickListener {
            // Use the more modern approach to handle back press
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                onBackPressed()
            } else {
                finish()
            }
        }

        // Initialize database helper
        dbHelper = DbHelper(this)

        setupViews()
        loadPettyCash()

        CallbackSingleton.refreshCallback = this

        // Register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            pettyCashUpdateReceiver,
            IntentFilter("pettycash_deleted_action")
        )

        setupFab()
    }

    private fun setupViews() {
        // SearchBar already has hint set in XML and now in onCreate too
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
            layoutManager = LinearLayoutManager(this@UnconvertedPettyCashActivity)
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
                    // Hide FAB when search is showing
                    fab?.hide()
                }
                SearchView.TransitionState.HIDDEN -> {
                    binding.suggestionRecycler.visibility = View.GONE
                    binding.noResultsTextView.visibility = View.GONE
                    binding.progressBarSuggestions.visibility = View.GONE
                    binding.pettyCashRecyclerView.visibility = View.VISIBLE
                    // Show and extend FAB when search is hidden
                    fab?.show()
                    fab?.extend()
                }
                SearchView.TransitionState.HIDING -> {
                    binding.searchView.editText.text = null
                    binding.pettyCashRecyclerView.visibility = View.VISIBLE
                    pettyCashAdapter?.setSearchQuery("")
                    Log.d("UnconvertedPettyCash", "Size of allPettyCash: ${allPettyCash.size}")
                    pettyCashAdapter?.updateData(allPettyCash)
                    // Show and extend FAB when search is hiding
                    fab?.show()
                    fab?.extend()
                }
                else -> {}
            }
        }
    }

    private fun searchPettyCash(query: String) {
        Log.d("UnconvertedPettyCash", "Search started: query='$query'")

        if (query.isEmpty()) {
            Log.d("UnconvertedPettyCash", "Empty query, showing all unconverted petty cash")
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
                Log.d("UnconvertedPettyCash", "Executing search query: $query")
                // Search only unconverted petty cash entries
                val searchResults = dbHelper.searchUnconvertedPettyCash(query)

                Log.d("UnconvertedPettyCash", "Search results for '$query': ${searchResults?.size ?: 0} items")

                withContext(Dispatchers.Main) {
                    binding.progressBarSuggestions.visibility = View.GONE
                    if (searchResults.isNullOrEmpty()) {
                        Log.d("UnconvertedPettyCash", "No results found for: $query")
                        binding.noResultsTextView.visibility = View.VISIBLE
                        binding.suggestionRecycler.visibility = View.GONE
                        binding.pettyCashRecyclerView.visibility = View.GONE
                    } else {
                        Log.d("UnconvertedPettyCash", "Showing ${searchResults.size} results for: $query")
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
                Log.e("UnconvertedPettyCash", "Error during search: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressBarSuggestions.visibility = View.GONE
                    Toast.makeText(
                        this@UnconvertedPettyCashActivity,
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
            layoutManager = LinearLayoutManager(this@UnconvertedPettyCashActivity)
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

    private fun showSortFilterDialog() {
        val dialogFragment = PettyCashSortFilterDialogFragment().apply {
            setOnApplyClickListener(this@UnconvertedPettyCashActivity)
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

        Log.d("UnconvertedPettyCash", """
            Applying Filters:
            Sort: $currentSortOption
            Date Filter: $currentDateFilter
            Payment Modes: $currentPaymentModes
            Custom Start Date: $customStartDate
            Custom End Date: $customEndDate
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
                // Load only unconverted petty cash entries
                val pettyCashList = dbHelper.getUnconvertedPettyCash(
                    page = currentPage,
                    pageSize = pageSize,
                    sortBy = currentSortOption,
                    dateFilter = dateFilter ?: "Any Time",
                    paymentModes = currentPaymentModes ?: emptyList(),
                    customStartDate = customStartDate,
                    customEndDate = customEndDate
                )
                
                // Ensure account and owner information is loaded for each petty cash entry
                pettyCashList.forEach { pettyCash ->
                    // If account is null, try to reload it from the database using the petty cash ID
                    if (pettyCash.account == null) {
                        val fullPettyCash = dbHelper.getPettyCashById(pettyCash.id ?: 0)
                        pettyCash.account = fullPettyCash?.account
                        pettyCash.owner = fullPettyCash?.owner
                    }
                }

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
                            pettyCashAdapter?.updateData(allPettyCash, true)
                            hasMoreData = pettyCashList.size >= pageSize
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UnconvertedPettyCash", "Error loading data", e)
                withContext(Dispatchers.Main) {
                    binding.progressBarSuggestions.visibility = View.GONE
                    Toast.makeText(
                        this@UnconvertedPettyCashActivity,
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
                    // Load only unconverted petty cash entries
                    val pageItems = dbHelper.getUnconvertedPettyCash(
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
                    
                    // Ensure account and owner information is loaded for each petty cash entry
                    pageItems.forEach { pettyCash ->
                        // If account is null, try to reload it from the database using the petty cash ID
                        if (pettyCash.account == null) {
                            val fullPettyCash = dbHelper.getPettyCashById(pettyCash.id ?: 0)
                            pettyCash.account = fullPettyCash?.account
                            pettyCash.owner = fullPettyCash?.owner
                        }
                    }

                    itemsToLoad.addAll(pageItems)
                }

                withContext(Dispatchers.Main) {
                    allPettyCash.clear()
                    allPettyCash.addAll(itemsToLoad)
                    pettyCashAdapter?.updateList(allPettyCash)
                    isLoading = false

                    // Show message if no unconverted petty cash
                    if (allPettyCash.isEmpty()) {
                        binding.noPettyCashMessage.visibility = View.VISIBLE
                        binding.pettyCashRecyclerView.visibility = View.GONE
                    } else {
                        binding.noPettyCashMessage.visibility = View.GONE
                        binding.pettyCashRecyclerView.visibility = View.VISIBLE
                    }

                    Log.d("UnconvertedPettyCash", """
                        Loaded unconverted petty cash:
                        Current page: $currentPage
                        Total items: ${allPettyCash.size}
                        Has more: $hasMoreData
                    """.trimIndent())
                }
            } catch (e: Exception) {
                Log.e("UnconvertedPettyCash", "Error loading petty cash: ${e.message}")
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(this@UnconvertedPettyCashActivity,
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
                // Load only unconverted petty cash entries
                val morePettyCash = dbHelper.getUnconvertedPettyCash(
                    page = currentPage,
                    pageSize = pageSize,
                    sortBy = currentSortOption ?: "Date",
                    dateFilter = currentDateFilter,
                    paymentModes = currentPaymentModes ?: emptyList(),
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
                        this@UnconvertedPettyCashActivity,
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
            Log.e("UnconvertedPettyCash", "Error parsing date: $dateStr", e)
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
            Log.e("UnconvertedPettyCash", "Error parsing date: $dateStr", e)
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
            Log.e("UnconvertedPettyCash", "Error parsing date: $dateStr", e)
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
            Log.e("UnconvertedPettyCash", "Error parsing date: $dateStr", e)
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
            Log.e("UnconvertedPettyCash", "Error parsing date: $dateStr", e)
            return false
        }
    }

    // Add new helper function for custom date range
    private fun isInCustomDateRange(dateStr: String?): Boolean {
        if (dateStr == null || customStartDate == null || customEndDate == null) {
            Log.d("UnconvertedPettyCash", "Null date values: dateStr=$dateStr, start=$customStartDate, end=$customEndDate")
            return false
        }

        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val date = LocalDate.parse(dateStr.substring(0, 10), formatter)
            val startDate = LocalDate.parse(customStartDate, formatter)
            val endDate = LocalDate.parse(customEndDate, formatter)

            return !date.isBefore(startDate) && !date.isAfter(endDate)
        } catch (e: Exception) {
            Log.e("UnconvertedPettyCash", "Error parsing date for custom range: $dateStr", e)
            return false
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
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
            .unregisterReceiver(pettyCashUpdateReceiver)

        fab = null
    }

    private fun setupFab() {
        fab = binding.addPettyCashFab

        // Handle scroll behavior
        binding.pettyCashRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && fab?.isExtended == true) {
                    fab?.shrink()
                } else if (dy < 0 && fab?.isExtended == false) {
                    fab?.extend()
                }
            }
        })

        fab?.setOnClickListener {
            // Disable the button to prevent multiple clicks
            fab?.isClickable = false

            // Show AddPettyCashFragment
            val addPettyCashFragment = AddPettyCashFragment.newInstance(action = "Add", pettyCash = null)

            // Explicitly set the listener
            addPettyCashFragment.setOnAddPettyCashListener(this)

            addPettyCashFragment.show(supportFragmentManager, "AddPettyCashFragment")

            // Re-enable the button after a delay
            lifecycleScope.launch {
                delay(1500)
                fab?.isClickable = true
            }
        }
    }

    override fun onAddPettyCash(newPettyCash: PettyCash, transactionCost: PettyCash?) {
        Log.d("UnconvertedPettyCash", "onAddPettyCash called with ID: ${newPettyCash.id}")

        // Only add the petty cash to the list if it's unconverted (petty cash number is null or empty)
        if (newPettyCash.pettyCashNumber.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // For new petty cash, add it to the top
                    withContext(Dispatchers.Main) {
                        allPettyCash.add(0, newPettyCash)
                        pettyCashAdapter?.addItemToTop(newPettyCash)

                        // Add transaction cost if exists and is unconverted
                        transactionCost?.let { tc ->
                            if (tc.pettyCashNumber.isNullOrEmpty()) {
                                allPettyCash.add(1, tc)
                                pettyCashAdapter?.addItemToTop(tc)
                            }
                        }

                        // Scroll to top to show new items
                        binding.pettyCashRecyclerView.scrollToPosition(0)
                    }
                } catch (e: Exception) {
                    Log.e("UnconvertedPettyCash", "Error in onAddPettyCash: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@UnconvertedPettyCashActivity,
                            "Error updating petty cash: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            // If petty cash has been converted, it should be removed from this activity's list
            val existingIndex = allPettyCash.indexOfFirst { it.id == newPettyCash.id }
            if (existingIndex != -1) {
                allPettyCash.removeAt(existingIndex)
                pettyCashAdapter?.removeItem(newPettyCash.id!!)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            if (requestCode == ViewAllPettyCashAdapter.PETTY_CASH_VIEWER_REQUEST && resultCode == Activity.RESULT_OK) {
                data?.let { intent ->
                    val pettyCashId = intent.getIntExtra("updated_petty_cash_id", -1)

                    if (pettyCashId != -1) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Fetch updated petty cash from database
                                val updatedPettyCash = dbHelper.getPettyCashById(pettyCashId)

                                withContext(Dispatchers.Main) {
                                    // If petty cash exists and is still unconverted, update it in the list
                                    if (updatedPettyCash != null && updatedPettyCash.pettyCashNumber.isNullOrEmpty()) {
                                        val index = allPettyCash.indexOfFirst { it.id == updatedPettyCash.id }
                                        if (index != -1) {
                                            allPettyCash[index] = updatedPettyCash
                                            pettyCashAdapter?.updateItem(updatedPettyCash)
                                        }
                                    } else if (updatedPettyCash != null) {
                                        // If petty cash was converted, remove it from the list
                                        val index = allPettyCash.indexOfFirst { it.id == updatedPettyCash.id }
                                        if (index != -1) {
                                            allPettyCash.removeAt(index)
                                            pettyCashAdapter?.removeItem(updatedPettyCash.id!!)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("UnconvertedPettyCash", "Error updating: ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UnconvertedPettyCash", "Error in onActivityResult: ${e.message}")
        }
    }

    override fun onRefresh() {
        Log.d("UnconvertedPettyCash", "onRefresh called")
        // Refresh the data
        loadPettyCash()
    }
}