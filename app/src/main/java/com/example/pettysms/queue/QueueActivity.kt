package com.example.pettysms.queue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pettysms.DbHelper
import com.example.pettysms.R
import com.example.pettysms.databinding.ActivityQueueBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import java.text.NumberFormat
import java.util.*

class QueueActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQueueBinding
    private lateinit var adapter: QueueAdapter
    private var dbHelper: DbHelper? = null
    private var allQueueItems = mutableListOf<QueueItem>()
    private var filteredQueueItems = mutableListOf<QueueItem>()
    private var currentStatusFilter: String? = null
    private var searchBar: SearchBar? = null
    private var searchView: SearchView? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager
    
    // Currency formatter for displaying amounts
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("KES")
    }
    
    // Broadcast receiver for queue data changes
    private val queueDataChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "🔔 Received broadcast: ${intent?.action}")
            if (intent?.action == ACTION_QUEUE_DATA_CHANGED) {
                Log.d(TAG, "🔄 Processing queue data changed broadcast")
                runOnUiThread {
                    Log.d(TAG, "🔄 Reloading queue items on UI thread")
                    loadQueueItems()
                }
            }
        }
    }

    companion object {
        private const val TAG = "QueueActivity"
        
        // Broadcast action for queue data changes
        const val ACTION_QUEUE_DATA_CHANGED = "com.example.pettysms.ACTION_QUEUE_DATA_CHANGED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize LocalBroadcastManager
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        
        // Initialize database helper
        dbHelper = DbHelper(this)
        
        // Initialize lists
        allQueueItems = mutableListOf()
        filteredQueueItems = mutableListOf()
        
        // Add a test item directly to the filtered list
        val testItem = QueueItem(
            id = 999,
            pettyCashId = 1,
            pettyCashNumber = "TEST-123",
            amount = 1000.0,
            description = "Test Item",
            date = "2023-05-01",
            accountId = 1,
            accountName = "Test Account",
            ownerId = 1,
            ownerName = "Test Owner",
            truckNumbers = "T-001",
            status = "PENDING"
        )
        filteredQueueItems.add(testItem)
        Log.d(TAG, "Added test item directly to filteredQueueItems in onCreate")
        
        // Set up Toolbar
        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.setNavigationOnClickListener {
            onNavigateUp()
        }
        
        // Disable elevation on AppBarLayout to prevent shading on scroll
        binding.appBarLayout.stateListAnimator = null
        binding.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        
        // Ensure SearchBar doesn't show shadow
        binding.searchBar.elevation = 0f

        // Setup RecyclerView
        binding.queueRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Add a dummy item directly to the RecyclerView for testing
        val dummyItem = QueueItem(
            id = 1000,
            pettyCashId = 2,
            pettyCashNumber = "DUMMY-456",
            amount = 2000.0,
            description = "Dummy Item for Testing",
            date = "2023-06-01",
            accountId = 2,
            accountName = "Dummy Account",
            ownerId = 2,
            ownerName = "Dummy Owner",
            truckNumbers = "D-002",
            status = "SENT"
        )
        
        // Make sure we have at least one item in the list
        if (filteredQueueItems.isEmpty()) {
            filteredQueueItems.add(dummyItem)
            Log.d(TAG, "Added dummy item to empty filteredQueueItems list")
        }
        
        Log.d(TAG, "RecyclerView setup with fixed height")
        
        // Initialize adapter with filtered list that already contains the test item
        Log.d(TAG, "Initializing adapter with filtered list containing ${filteredQueueItems.size} items")
        adapter = QueueAdapter(filteredQueueItems,
            onItemClick = { queueItem ->
                showQueueItemDetailsDialog(queueItem)
            },
            onStatusChanged = { queueItem ->
                // Handle status change
                handleQueueItemStatusChange(queueItem)
            }
        )
        binding.queueRecyclerView.adapter = adapter
        
        // Setup Search Bar and Search View
        setupSearch()

        // Setup Status Filter Cards
        setupStatusFilterCards()

        // Initial load of queue items
        loadQueueItems()
        
        // Force visibility
        binding.queueRecyclerView.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        Log.d(TAG, "Forced RecyclerView visibility at end of onCreate")
        
        // Force adapter update
        adapter.notifyDataSetChanged()
        Log.d(TAG, "Forced adapter update at end of onCreate")
        
        // Register both local and global broadcast receivers for maximum compatibility
        // 1. Register with LocalBroadcastManager (preferred for in-app communication)
        val intentFilter = IntentFilter(ACTION_QUEUE_DATA_CHANGED)
        localBroadcastManager.registerReceiver(queueDataChangedReceiver, intentFilter)
        Log.d(TAG, "📡 Registered local broadcast receiver with action: $ACTION_QUEUE_DATA_CHANGED")
        
        // 2. Also register with global context for backward compatibility
        ContextCompat.registerReceiver(
            this,
            queueDataChangedReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "📡 Registered global broadcast receiver with action: $ACTION_QUEUE_DATA_CHANGED and RECEIVER_NOT_EXPORTED flag")
        
        // 3. Test the broadcast receiver immediately
        sendTestBroadcast()
    }
    
    private fun sendTestBroadcast() {
        // Send a test broadcast to verify the receiver is working
        val testIntent = Intent(ACTION_QUEUE_DATA_CHANGED)
        // Make the intent explicit by setting the package name
        testIntent.setPackage(packageName)
        testIntent.putExtra("test", "true")
        localBroadcastManager.sendBroadcast(testIntent)
        Log.d(TAG, "📤 Sent test broadcast with action: $ACTION_QUEUE_DATA_CHANGED and package: $packageName")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 onResume called, reloading queue items")
        loadQueueItems()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister both broadcast receivers
        try {
            // Unregister from LocalBroadcastManager
            localBroadcastManager.unregisterReceiver(queueDataChangedReceiver)
            Log.d(TAG, "Unregistered local broadcast receiver")
            
            // Unregister from global context
            unregisterReceiver(queueDataChangedReceiver)
            Log.d(TAG, "Unregistered global broadcast receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.queue_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadQueueItems()
                true
            }
            R.id.action_sync_all -> {
                syncAllPendingItems()
                true
            }
            R.id.action_sync_to_quickbooks -> {
                syncToQuickBooks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupSearch() {
        searchBar = binding.searchBar
        searchView = binding.searchView

        // Set up search view text listener
        searchView?.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView?.text.toString()
                filterQueueItems(query)
                searchBar?.setText(query)
                searchView?.hide()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        // Handle search view transitions
        searchView?.addTransitionListener { _, previousState, newState ->
            when (newState) {
                SearchView.TransitionState.SHOWING -> {
                    // When search is showing
                }
                SearchView.TransitionState.HIDDEN -> {
                    // When search is hidden
                }
                SearchView.TransitionState.HIDING -> {
                    searchView?.editText?.text = null
                }
                else -> {}
            }
        }

        // Change status bar color when search is focused
        val searchFocusedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer, 0)
        val defaultStatusBarColor = this.window.statusBarColor

        searchView?.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWING) {
                // Change the status bar color to match the SearchView's surface color
                this.window.statusBarColor = searchFocusedColor
            } else if (newState == SearchView.TransitionState.HIDDEN) {
                // Revert the status bar color to the default color
                this.window.statusBarColor = defaultStatusBarColor
            }
        }
    }

    private fun setupStatusFilterCards() {
        binding.pendingCard.setOnClickListener {
            filterByStatus("PENDING")
        }
        binding.sentCard.setOnClickListener {
            filterByStatus("SENT")
        }
        binding.syncedCard.setOnClickListener {
            filterByStatus("SYNCED")
        }
        binding.failedCard.setOnClickListener {
            filterByStatus("FAILED")
        }
    }

    private fun filterByStatus(status: String) {
        currentStatusFilter = if (currentStatusFilter == status) {
            // Clicking the same filter again clears it
            null
        } else {
            status
        }
        
        applyFilters()
        updateStatusCardSelection()
    }

    private fun updateStatusCardSelection() {
        // Reset all cards to default appearance
        binding.pendingCard.strokeWidth = 0
        binding.sentCard.strokeWidth = 0
        binding.syncedCard.strokeWidth = 0
        binding.failedCard.strokeWidth = 0
        
        // Set stroke color to primary color for better visibility
        binding.pendingCard.strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0)
        binding.sentCard.strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0)
        binding.syncedCard.strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0)
        binding.failedCard.strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0)

        // Highlight selected card with a thicker stroke
        when (currentStatusFilter) {
            "PENDING", QueueItem.STATUS_PENDING -> binding.pendingCard.strokeWidth = 4
            "SENT", QueueItem.STATUS_SENT -> binding.sentCard.strokeWidth = 4
            "SYNCED", QueueItem.STATUS_SYNCED -> binding.syncedCard.strokeWidth = 4
            "FAILED", QueueItem.STATUS_FAILED -> binding.failedCard.strokeWidth = 4
        }
    }

    private fun filterQueueItems(query: String) {
        try {
            applyFilters(query)
        } catch (e: Exception) {
            Log.e(TAG, "Error filtering queue items: ${e.message}", e)
            Toast.makeText(this, "Error filtering items", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFilters(searchQuery: String? = null) {
        // Get the search query from the parameter or try to get it from the SearchView
        val query = searchQuery ?: searchBar?.text?.toString() ?: ""
        val lowerCaseQuery = query.toLowerCase(Locale.ROOT)
        
        filteredQueueItems.clear()
        filteredQueueItems.addAll(allQueueItems.filter { queueItem ->
            val matchesQuery = query.isEmpty() ||
                    queueItem.pettyCashNumber.toLowerCase(Locale.ROOT).contains(lowerCaseQuery) ||
                    queueItem.description?.toLowerCase(Locale.ROOT)?.contains(lowerCaseQuery) == true ||
                    queueItem.accountName?.toLowerCase(Locale.ROOT)?.contains(lowerCaseQuery) == true ||
                    queueItem.ownerName?.toLowerCase(Locale.ROOT)?.contains(lowerCaseQuery) == true
            
            val matchesStatus = currentStatusFilter == null || queueItem.status == currentStatusFilter
            
            matchesQuery && matchesStatus
        })
        
        Log.d(TAG, "After filtering: ${filteredQueueItems.size} items remain (from ${allQueueItems.size} total)")
        Log.d(TAG, "Current status filter: $currentStatusFilter, Search query: '$query'")
        
        // Force a complete refresh of the adapter
        adapter.notifyDataSetChanged()
        
        // Update empty state visibility
        updateEmptyState()
    }

    private fun loadQueueItems() {
        try {
            // Get all queue items
            allQueueItems.clear()
            dbHelper?.getAllQueueItems()?.let { items ->
                allQueueItems.addAll(items)
                Log.d(TAG, "Loaded ${items.size} queue items from database")
            }

            // Get queue summary for dashboard
            updateQueueSummary()
            
            // Apply filters to update the displayed list
            applyFilters()
            
            // Force adapter to update
            Log.d(TAG, "Notifying adapter of data change with ${filteredQueueItems.size} items")
            adapter.notifyDataSetChanged()
            
            // Ensure RecyclerView is visible
            binding.queueRecyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = if (filteredQueueItems.isEmpty()) View.VISIBLE else View.GONE
            
            // Log the visibility state
            Log.d(TAG, "RecyclerView visibility: ${if (binding.queueRecyclerView.visibility == View.VISIBLE) "VISIBLE" else "GONE"}")
            Log.d(TAG, "Empty state visibility: ${if (binding.emptyStateLayout.visibility == View.VISIBLE) "VISIBLE" else "GONE"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading queue items: ${e.message}", e)
            Toast.makeText(this, "Error loading queue items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateQueueSummary() {
        try {
            val summary = dbHelper?.getQueueSummary() ?: mapOf()
            
            // Update total count
            val totalCount = summary.values.sum()
            binding.totalCount.text = totalCount.toString()
            
            // Update individual status counts
            binding.pendingCount.text = (summary["PENDING"] ?: 0).toString()
            binding.sentCount.text = (summary["SENT"] ?: 0).toString()
            binding.syncedCount.text = (summary["SYNCED"] ?: 0).toString()
            binding.failedCount.text = (summary["FAILED"] ?: 0).toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating queue summary: ${e.message}", e)
        }
    }

    private fun updateEmptyState() {
        val isEmpty = filteredQueueItems.isEmpty()
        Log.d(TAG, "updateEmptyState: isEmpty=$isEmpty, filteredQueueItems.size=${filteredQueueItems.size}")
        
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.queueRecyclerView.visibility = View.GONE
            Log.d(TAG, "Empty state shown, RecyclerView hidden")
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.queueRecyclerView.visibility = View.VISIBLE
            Log.d(TAG, "Empty state hidden, RecyclerView shown with ${filteredQueueItems.size} items")
            
            // Force layout refresh
            binding.queueRecyclerView.post {
                adapter.notifyDataSetChanged()
                Log.d(TAG, "Forced adapter refresh in post() callback")
            }
        }
    }

    private fun showQueueItemDetailsDialog(queueItem: QueueItem) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_queue_item_details, null)
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Queue Item Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create()
    
            // Populate dialog fields with queue item details - using safe access
            dialogView?.let { view ->
                // Safely set text for each field
                safeSetText(view, R.id.tv_petty_cash_number_value, queueItem.pettyCashNumber)
                
                // Format amount with currency
                if (queueItem.amount != null) {
                    safeSetText(view, R.id.tv_amount_value, currencyFormatter.format(queueItem.amount))
                } else {
                    safeSetText(view, R.id.tv_amount_value, "N/A")
                }
                
                // Set date directly (it's already a string)
                safeSetText(view, R.id.tv_date_value, queueItem.date)
                
                safeSetText(view, R.id.tv_description_value, queueItem.description)
                safeSetText(view, R.id.tv_account_value, queueItem.accountName)
                safeSetText(view, R.id.tv_owner_value, queueItem.ownerName)
                safeSetText(view, R.id.tv_trucks_value, queueItem.truckNumbers)
                safeSetText(view, R.id.tv_status_value, queueItem.status)
                safeSetText(view, R.id.tv_error_message_value, queueItem.errorMessage)
                
                // Set timestamps directly (they're already formatted strings)
                safeSetText(view, R.id.tv_created_at_value, queueItem.createdAt)
                safeSetText(view, R.id.tv_updated_at_value, queueItem.updatedAt)
    
                // Set status color based on status - safely get the TextView
                view.findViewById<TextView>(R.id.tv_status_value)?.let { statusTextView ->
                    val color = when (queueItem.status) {
                        "PENDING", QueueItem.STATUS_PENDING -> ContextCompat.getColor(this@QueueActivity,
                            R.color.pendingColor
                        )
                        "SENT", QueueItem.STATUS_SENT -> ContextCompat.getColor(this@QueueActivity,
                            R.color.sentColor
                        )
                        "SYNCED", QueueItem.STATUS_SYNCED -> ContextCompat.getColor(this@QueueActivity,
                            R.color.syncedColor
                        )
                        "FAILED", QueueItem.STATUS_FAILED -> ContextCompat.getColor(this@QueueActivity,
                            R.color.failedColor
                        )
                        else -> ContextCompat.getColor(this@QueueActivity, R.color.defaultColor)
                    }
                    statusTextView.setTextColor(color)
                }
    
                // Only show error message row if there is an error - safely get the view
                view.findViewById<View>(R.id.error_message_row)?.visibility =
                    if ((queueItem.status == "FAILED" || queueItem.status == QueueItem.STATUS_FAILED)
                        && !queueItem.errorMessage.isNullOrEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
            
            dialog.show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing queue item details dialog: ${e.message}", e)
            Toast.makeText(this, "Error displaying details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Helper function to safely set text on a TextView
    private fun safeSetText(parentView: View, textViewId: Int, text: String?) {
        try {
            parentView.findViewById<TextView>(textViewId)?.text = text ?: "N/A"
        } catch (e: Exception) {
            Log.e(TAG, "Error setting text for view $textViewId: ${e.message}")
        }
    }

    private fun syncAllPendingItems() {
        try {
            // Get all pending items
            val pendingItems = allQueueItems.filter { it.status == "PENDING" }
            
            if (pendingItems.isEmpty()) {
                Toast.makeText(this, "No pending items to sync", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Update UI to show we're starting sync
            Toast.makeText(this, "Starting sync for ${pendingItems.size} items...", Toast.LENGTH_SHORT).show()
            
            // Schedule immediate sync using the QuickBooksWorker
            QuickBooksWorker.scheduleImmediateSync(this)
            
            Toast.makeText(this, "Sync job scheduled for ${pendingItems.size} items", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending items: ${e.message}", e)
            Toast.makeText(this, "Error syncing items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Sync to QuickBooks using EasyQuickImport
     */
    private fun syncToQuickBooks() {
        try {
            // Show confirmation dialog
            MaterialAlertDialogBuilder(this)
                .setTitle("Sync to QuickBooks")
                .setMessage("This will send all pending items to EasyQuickImport for QuickBooks integration. Continue?")
                .setPositiveButton("Sync") { _, _ ->
                    // Schedule immediate sync
                    QuickBooksWorker.scheduleImmediateSync(this)
                    Toast.makeText(this, "Sync to QuickBooks scheduled", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncToQuickBooks: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleQueueItemStatusChange(queueItem: QueueItem) {
        try {
            Log.d(TAG, "Handling status change for item ${queueItem.pettyCashNumber}: ${queueItem.status}")
            
            // Remove item from current filtered list if it doesn't match current filter
            if (currentStatusFilter != null && queueItem.status != currentStatusFilter) {
                val position = filteredQueueItems.indexOfFirst { it.id == queueItem.id }
                if (position != -1) {
                    Log.d(TAG, "Removing item from filtered list at position $position because it no longer matches filter '$currentStatusFilter'")
                    filteredQueueItems.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    
                    // If we're specifically changing from FAILED to PENDING and filtering by FAILED
                    if (currentStatusFilter == QueueItem.STATUS_FAILED && queueItem.status == QueueItem.STATUS_PENDING) {
                        Log.d(TAG, "Item was changed from FAILED to PENDING while filtering by FAILED")
                    }
                } else {
                    Log.d(TAG, "Item not found in filtered list")
                }
            } else if (currentStatusFilter == queueItem.status) {
                // Item still matches the filter, just update it
                val position = filteredQueueItems.indexOfFirst { it.id == queueItem.id }
                if (position != -1) {
                    Log.d(TAG, "Updating item in filtered list at position $position")
                    filteredQueueItems[position] = queueItem
                    adapter.notifyItemChanged(position)
                }
            }

            // Update the item in the main list
            val mainPosition = allQueueItems.indexOfFirst { it.id == queueItem.id }
            if (mainPosition != -1) {
                Log.d(TAG, "Updating item in main list at position $mainPosition")
                allQueueItems[mainPosition] = queueItem
            } else {
                Log.d(TAG, "Item not found in main list")
            }

            // Update queue summary in dashboard
            updateQueueSummary()

            // Update empty state if needed
            updateEmptyState()

            Log.d(TAG, "Queue item status change handled: ID=${queueItem.id}, New Status=${queueItem.status}")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling queue item status change: ${e.message}", e)
        }
    }
} 