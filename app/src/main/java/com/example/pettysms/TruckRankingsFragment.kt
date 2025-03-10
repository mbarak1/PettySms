package com.example.pettysms

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import android.view.WindowManager
import com.example.pettysms.databinding.FragmentTruckRankingsBinding
import com.example.pettysms.databinding.FragmentTruckExpensesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragment for displaying truck rankings based on expenses
 */
class TruckRankingsFragment : Fragment() {

    private var _binding: FragmentTruckRankingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DbHelper
    private lateinit var truckRankingsAdapter: TruckRankingsAdapter
    private var owners = mutableListOf<Owner>()
    private var selectedOwner: Owner? = null
    private var isDataLoaded = false
    private val PAGE_SIZE = 50
    private var currentPage = 0
    

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTruckRankingsBinding.inflate(inflater, container, false)
        
        // Setup toolbar
        val actionbar = binding.toolbar
        actionbar.title = "Truck Rankings"
        (activity as AppCompatActivity).setSupportActionBar(actionbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize database helper
        dbHelper = DbHelper(requireContext())
        
        // Setup RecyclerView with optimized settings
        setupRecyclerView()
        
        // Setup owner spinner
        setupOwnerSpinner()
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set Fragment fade transitions
        val fadeIn = android.transition.Fade(android.transition.Fade.IN).apply {
            duration = 300L
            addTarget(view)
            excludeTarget(android.R.id.statusBarBackground, true)
            excludeTarget(android.R.id.navigationBarBackground, true)
        }
        
        val fadeOut = android.transition.Fade(android.transition.Fade.OUT).apply {
            duration = 300L
            addTarget(view)
            excludeTarget(android.R.id.statusBarBackground, true)
            excludeTarget(android.R.id.navigationBarBackground, true)
        }
        
        enterTransition = fadeIn
        exitTransition = fadeOut
        
        // Disable transition overlap for smoother animations
        allowEnterTransitionOverlap = false
        allowReturnTransitionOverlap = false
        
        // Initialize loading state tag
        binding.recyclerViewTrucks.setTag(R.id.tag_is_loading, false)
        
        // Setup pagination with optimized scroll listener
        binding.recyclerViewTrucks.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // Only process if scrolling down
                if (dy > 0) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Check if we're already loading
                    val isLoading = binding.recyclerViewTrucks.getTag(R.id.tag_is_loading) as? Boolean ?: false
                    
                    // Check if we need to load more data
                    if (!isLoading && 
                        binding.progressBar.visibility != View.VISIBLE && 
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && // Pre-load before reaching the end
                        firstVisibleItemPosition >= 0) {
                        
                        // Set loading flag
                        binding.recyclerViewTrucks.setTag(R.id.tag_is_loading, true)
                        currentPage++
                        loadMoreTruckRankings()
                    }
                }
            }
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                // When scrolling stops, check if we're at the bottom and need to load more
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = layoutManager.itemCount
                    
                    // If the last visible item is the last item in the list, we're at the bottom
                    if (lastVisibleItemPosition == totalItemCount - 1) {
                        Log.d("TruckRankingsFragment", "Reached the bottom of the list")
                    }
                }
            }
        })
        
        // Load initial truck rankings
        if (!isDataLoaded) {
            currentPage = 0
            loadTruckRankings()
        }
    }
    
    private fun setupRecyclerView() {
        truckRankingsAdapter = TruckRankingsAdapter(
            requireContext(), 
            mutableListOf(),
            object : TruckRankingsAdapter.OnTruckClickListener {
                override fun onTruckClick(truckNo: String, amount: Double) {
                    // Show truck expenses fragment when a truck is clicked
                    showTruckExpensesFragment(truckNo)
                }
            }
        )
        
        binding.recyclerViewTrucks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = truckRankingsAdapter
            // Set these flags to true to improve performance
            setHasFixedSize(true)
            itemAnimator = null // Disable animations for better performance
            // Reduce overdraw by setting a smaller recycled view pool
            setItemViewCacheSize(20)
            // Optimize for large datasets
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_LOW
            
            // Add item decoration for spacing
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    // Add bottom margin to each item except the last one
                    val position = parent.getChildAdapterPosition(view)
                    if (position < parent.adapter?.itemCount?.minus(1) ?: 0) {
                        outRect.bottom = resources.getDimensionPixelSize(R.dimen.item_margin)
                    } else {
                        // Add extra space to the last item to ensure it can be scrolled fully into view
                        outRect.bottom = resources.getDimensionPixelSize(R.dimen.last_item_margin)
                    }
                }
            })
        }
    }
    
    private fun showTruckExpensesFragment(truckNo: String) {
        // Navigate using the Navigation Component with a Bundle
        val bundle = Bundle().apply {
            putString("truck_no", truckNo)
        }
        findNavController().navigate(R.id.action_truckRankingsFragment_to_truckExpensesFragment, bundle)
    }
    
    private fun setupOwnerSpinner() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get all owners
                val allOwners = dbHelper.getAllOwners()
                
                // Add "All Owners" option
                val allOwnersOption = Owner(0, "All Owners", "ALL")
                owners = mutableListOf(allOwnersOption)
                owners.addAll(allOwners)
                
                withContext(Dispatchers.Main) {
                    // Create adapter for spinner
                    val ownerAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        owners.map { it.name }
                    )
                    ownerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    
                    // Set adapter to spinner
                    binding.spinnerOwner.adapter = ownerAdapter
                    
                    // Set listener for spinner
                    binding.spinnerOwner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedOwner = owners[position]
                            loadTruckRankings()
                        }
                        
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Do nothing
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TruckRankingsFragment", "Error loading owners", e)
                withContext(Dispatchers.Main) {
                    binding.textErrorMessage.visibility = View.VISIBLE
                    binding.textErrorMessage.text = "Error loading owners: ${e.message}"
                }
            }
        }
    }
    
    private fun getFirstAndLastDayOfCurrentMonth(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        
        // Set to first day of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDay = dateFormat.format(calendar.time)
        
        // Set to last day of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastDay = dateFormat.format(calendar.time)
        
        return Pair(firstDay, lastDay)
    }
    
    private fun loadMoreTruckRankings() {
        binding.progressBarBottom.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get date range for current month
                val (startDate, endDate) = getFirstAndLastDayOfCurrentMonth()
                
                // Get active truck expenses with pagination for current month
                val currentOwner = selectedOwner
                val rawData = if (currentOwner != null && currentOwner.ownerCode != "ALL") {
                    // Filter by owner with pagination for current month
                    currentOwner.ownerCode?.let { ownerCode ->
                        getActiveTruckExpensesForCurrentMonth(ownerCode, startDate, endDate, PAGE_SIZE, currentPage * PAGE_SIZE)
                    } ?: emptyList()
                } else {
                    // Get all active truck expenses with pagination for current month
                    getActiveTruckExpensesForCurrentMonth(null, startDate, endDate, PAGE_SIZE, currentPage * PAGE_SIZE)
                }
                
                withContext(Dispatchers.Main) {
                    binding.progressBarBottom.visibility = View.GONE
                    
                    if (rawData.isNotEmpty()) {
                        // Store position before adding data
                        val oldItemCount = truckRankingsAdapter.itemCount
                        
                        // Add more data to adapter
                        truckRankingsAdapter.addData(rawData)
                        
                        // Scroll to show the newly added items
                        if (oldItemCount > 0) {
                            // Scroll to the first new item to ensure it's visible
                            scrollToPosition(oldItemCount)
                        }
                    }
                    
                    // Reset loading flag in scroll listener
                    binding.recyclerViewTrucks.getTag(R.id.tag_is_loading)?.let {
                        binding.recyclerViewTrucks.setTag(R.id.tag_is_loading, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("TruckRankingsFragment", "Error loading more truck rankings", e)
                withContext(Dispatchers.Main) {
                    binding.progressBarBottom.visibility = View.GONE
                    Toast.makeText(context, "Error loading more data: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    // Reset loading flag in scroll listener
                    binding.recyclerViewTrucks.getTag(R.id.tag_is_loading)?.let {
                        binding.recyclerViewTrucks.setTag(R.id.tag_is_loading, false)
                    }
                }
            }
        }
    }
    
    private fun getActiveTruckExpensesForCurrentMonth(
        ownerCode: String?,
        startDate: String,
        endDate: String,
        limit: Int, 
        offset: Int
    ): List<Pair<String, Double>> {
        val db = dbHelper.readableDatabase
        val truckExpensesMap = mutableMapOf<String, Double>()
        
        try {
            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val currentYear = calendar.get(Calendar.YEAR).toString()
            
            Log.d("TruckRankingsFragment", "Filtering for month: $currentMonth and year: $currentYear")
            
            // Query to get active petty cash transactions for current month
            val queryBuilder = StringBuilder()
            queryBuilder.append("SELECT * FROM ${DbHelper.TABLE_PETTY_CASH} WHERE ")
            queryBuilder.append("${DbHelper.COL_PETTY_CASH_TRUCKS} IS NOT NULL ")
            queryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_TRUCKS} != '' ")
            queryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_IS_DELETED} = 0 ")
            
            // Date filter using substr - dates are in format dd-MM-yyyy
            queryBuilder.append("AND (SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 7, 4) = ? AND SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 4, 2) = ?) ")
            
            val args = mutableListOf<String>(currentYear, currentMonth)
            
            if (ownerCode != null) {
                queryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_OWNER} = ? ")
                args.add(ownerCode)
            }
            
            val query = queryBuilder.toString()
            Log.d("TruckRankingsFragment", "SQL Query: $query with args: ${args.joinToString()}")
            
            val cursor = db.rawQuery(query, args.toTypedArray())
            
            // Log how many records were found
            val recordCount = cursor.count
            Log.d("TruckRankingsFragment", "Found $recordCount petty cash records with trucks for the current month")
            
            // Process each petty cash entry
            while (cursor.moveToNext()) {
                val truckString = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_TRUCKS))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_AMOUNT))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_DATE))
                
                // Log truck data for debugging
                Log.d("TruckRankingsFragment", "Processing petty cash entry from date: $date, trucks: $truckString, amount: $amount")
                
                // Split truck string and distribute amount equally among trucks
                val trucks = truckString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                if (trucks.isNotEmpty()) {
                    val amountPerTruck = amount / trucks.size
                    
                    // Add amount to each truck
                    trucks.forEach { truck ->
                        val currentAmount = truckExpensesMap.getOrDefault(truck, 0.0)
                        truckExpensesMap[truck] = currentAmount + amountPerTruck
                        Log.d("TruckRankingsFragment", "Added $amountPerTruck to truck $truck (now has total: ${truckExpensesMap[truck]})")
                    }
                }
            }
            cursor.close()
            
            // Log final truck expense map
            Log.d("TruckRankingsFragment", "Final truck expenses map: ${truckExpensesMap.entries.joinToString { "${it.key}:${it.value}" }}")
            
            // Convert map to sorted list for pagination
            val allTruckExpenses = truckExpensesMap.entries
                .map { Pair(it.key, it.value) }
                .sortedBy { it.second }
            
            // Apply pagination manually
            return if (offset < allTruckExpenses.size) {
                val end = minOf(offset + limit, allTruckExpenses.size)
                val paginatedResults = allTruckExpenses.subList(offset, end)
                Log.d("TruckRankingsFragment", "Returning ${paginatedResults.size} truck results (from $offset to $end)")
                paginatedResults
            } else {
                Log.d("TruckRankingsFragment", "Offset $offset exceeds result size ${allTruckExpenses.size}, returning empty list")
                emptyList()
            }
            
        } catch (e: Exception) {
            Log.e("TruckRankingsFragment", "Error getting active truck expenses for current month", e)
            Log.e("TruckRankingsFragment", "Exception details: ${e.message}")
        }
        
        return emptyList()
    }
    
    private fun loadTruckRankings() {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewTrucks.visibility = View.GONE
        binding.textNoData.visibility = View.GONE
        binding.textErrorMessage.visibility = View.GONE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Reset pagination
                currentPage = 0
                
                // Get date range for current month
                val (startDate, endDate) = getFirstAndLastDayOfCurrentMonth()
                Log.d("TruckRankingsFragment", "Loading initial truck rankings for date range: $startDate to $endDate")
                
                // Get active truck expenses with pagination for current month
                val currentOwner = selectedOwner
                val ownerString = if (currentOwner != null && currentOwner.ownerCode != "ALL") {
                    currentOwner.ownerCode ?: "null"
                } else {
                    "ALL"
                }
                Log.d("TruckRankingsFragment", "Selected owner: $ownerString")
                
                val truckExpenses = if (currentOwner != null && currentOwner.ownerCode != "ALL") {
                    // Filter by owner with pagination for current month
                    currentOwner.ownerCode?.let { ownerCode ->
                        getActiveTruckExpensesForCurrentMonth(ownerCode, startDate, endDate, PAGE_SIZE, 0)
                    } ?: emptyList()
                } else {
                    // Get all active truck expenses with pagination for current month
                    getActiveTruckExpensesForCurrentMonth(null, startDate, endDate, PAGE_SIZE, 0)
                }
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    
                    if (truckExpenses.isEmpty()) {
                        Log.d("TruckRankingsFragment", "No truck expenses found to display")
                        binding.textNoData.visibility = View.VISIBLE
                        binding.textNoData.text = "No truck data available for the current month"
                        binding.recyclerViewTrucks.visibility = View.GONE
                    } else {
                        Log.d("TruckRankingsFragment", "Found ${truckExpenses.size} truck expenses to display")
                        binding.textNoData.visibility = View.GONE
                        binding.recyclerViewTrucks.visibility = View.VISIBLE
                        
                        // Update adapter with truck expenses
                        truckRankingsAdapter.updateData(truckExpenses)
                        isDataLoaded = true
                        
                        // Scroll to show the first few items
                        binding.recyclerViewTrucks.post {
                            binding.recyclerViewTrucks.scrollToPosition(0)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TruckRankingsFragment", "Error loading truck rankings", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.textErrorMessage.visibility = View.VISIBLE
                    binding.textErrorMessage.text = "Error loading truck rankings: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Scrolls to the bottom of the RecyclerView to show the last item.
     * Call this method when you need to ensure the last card is visible.
     */
    private fun scrollToBottom() {
        val itemCount = truckRankingsAdapter.itemCount
        if (itemCount > 0) {
            binding.recyclerViewTrucks.post {
                binding.recyclerViewTrucks.smoothScrollToPosition(itemCount - 1)
            }
        }
    }
    
    /**
     * Helper method to scroll to a specific position in the RecyclerView
     */
    private fun scrollToPosition(position: Int) {
        if (position >= 0 && position < truckRankingsAdapter.itemCount) {
            binding.recyclerViewTrucks.post {
                binding.recyclerViewTrucks.smoothScrollToPosition(position)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure we're not doing unnecessary work when returning to the fragment
        if (isDataLoaded && truckRankingsAdapter.itemCount == 0) {
            loadTruckRankings()
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel any ongoing operations
        lifecycleScope.coroutineContext.cancelChildren()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear references to prevent memory leaks
        binding.recyclerViewTrucks.adapter = null
        binding.recyclerViewTrucks.clearOnScrollListeners()
        truckRankingsAdapter.clearData()
        _binding = null
    }
} 