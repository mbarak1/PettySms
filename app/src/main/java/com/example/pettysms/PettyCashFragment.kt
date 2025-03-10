package com.example.pettysms

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.pettysms.databinding.FragmentPettyCashBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.android.material.button.MaterialButton
import android.database.sqlite.SQLiteDatabase
import com.example.pettysms.queue.QueueActivity
import kotlinx.coroutines.Job
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.isActive


/**
 * A simple [Fragment] subclass.
 * Use the [PettyCashFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PettyCashFragment : Fragment(), AddPettyCashFragment.OnAddPettyCashListener,
    RefreshRecyclerViewCallback {
    private var _binding: FragmentPettyCashBinding? = null



    private val binding get() = _binding!!
    private var pettyCashAdapter: PettyCashAdapter? = null
    private var dbStatus = false
    private var qbStatus = false
    private var transactors: MutableList<Transactor>? = null
    private var notCheckedTransactions:  MutableList<MpesaTransaction>? = null
    private var pettyCashList: MutableList<PettyCash>? = null
    private val pageSize = 20 // Number of items per page
    private var currentPage = 1 // Start from the first page
    private var isLoading = false // Flag to prevent multiple loads
    private var syncMenuItem: MenuItem? = null
    private var constraintLayout: ConstraintLayout? = null
    private var floatingActionButton: ExtendedFloatingActionButton? = null
    private var nestedScrollView: NestedScrollView? = null
    private var toolbar: MaterialToolbar? = null
    private var appBarLayout: AppBarLayout? = null
    private var dotIndicator: WormDotsIndicator? = null
    private var viewPager: ViewPager2? = null
    private var cashImage: ImageView? = null
    private var cashLabel: TextView? = null
    private var mpesaImage: ImageView? = null
    private var dbDotImageView: ImageView? = null
    private var dbStatusTextLabel: TextView? = null
    private var qbImageView: ImageView? = null
    private var qbStatusTextLabel: TextView? = null
    private var loadingDialog: AlertDialog? = null
    private var loadingText: TextView? = null
    private var truckButton: Button? = null
    private var ownerButton: Button? = null
    private var transactorsButton: Button? = null
    private var reportsButton: Button? = null
    private var accountsButton: Button? = null
    private var pettyCashRecyclerView: RecyclerView? = null
    private var noPettyCashMessageTextView: TextView? = null
    private var viewAllLink: TextView? = null
    private var automationButton: MaterialButton? = null
    private var queueButton: MaterialButton? = null
    private var totalAmountCard: TextView? = null

    // Add class level variables to track current filters
    private var currentDateFilter = "This Month"
    private var currentPaymentModes = emptyList<String>()
    private var currentSortOption = "Date"
    private var currentCustomStartDate: String? = null
    private var currentCustomEndDate: String? = null

    var dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
    var db = dbHelper?.writableDatabase

    // Add broadcast receiver
    private val pettyCashDeleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "pettycash_deleted_action") {
                Log.d("PettyCashFragment", "Received petty cash delete broadcast")
                
                // Handle both main petty cash and transaction cost deletions
                val idsToDelete = listOfNotNull(
                    intent.getIntExtra("deleted_petty_cash_id", -1).takeIf { it != -1 },
                    intent.getIntExtra("deleted_transaction_cost_id", -1).takeIf { it != -1 }
                )
                
                if (idsToDelete.isNotEmpty()) {
                    try {
                        var anyItemRemoved = false
                        // Remove all deleted items from lists and adapters
                        idsToDelete.forEach { deletedId ->
                            Log.d("PettyCashFragment", "Processing deletion for ID: $deletedId")
                            
                            // Find position in adapter's list
                            pettyCashAdapter?.let { adapter ->
                                val position = adapter.findItemIndexById(deletedId)
                                Log.d("PettyCashFragment", "Position in adapter: $position")
                                
                                if (position != -1) {
                                    // Remove from adapter
                                    adapter.removeItem(deletedId)
                                    anyItemRemoved = true
                                    Log.d("PettyCashFragment", "Removed item $deletedId from adapter")
                                }
                            }
                            
                            // Also remove from fragment's list if it exists
                            val listPosition = pettyCashList?.indexOfFirst { it.id == deletedId }
                            if (listPosition != -1) {
                                listPosition?.let {
                                    pettyCashList?.removeAt(it)
                                    Log.d("PettyCashFragment", "Removed item $deletedId from fragment list")
                                }
                            }
                        }
                        
                        if (anyItemRemoved) {
                            // Update empty state and related UI only if items were actually removed
                            updateEmptyState()
                            // Update card totals when items are removed
                            updateCardTotals()
                            Log.d("PettyCashFragment", "Updated UI after deletions, remaining items: ${pettyCashList?.size}")
                        }
                        
                    } catch (e: Exception) {
                        Log.e("PettyCashFragment", "Error handling deletion: ${e.message}")
                    }
                }
            }
        }
    }

    private val pettyCashAddedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("PettyCashFragment", "Received broadcast with action: ${intent?.action}")
            
            // Dump all extras for debugging
            intent?.extras?.keySet()?.forEach { key ->
                Log.d("PettyCashFragment", "Intent extra: $key = ${intent.extras?.get(key)}")
            }
            
            if (intent?.action == "petty_cash_added_action") {
                Log.d("PettyCashFragment", "Processing petty cash added broadcast")
                
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Make sure database is open
                        dbHelper?.openDatabase()
                        
                        // Check if we have JSON data (for null IDs) or regular IDs
                        val newPettyCashJson = intent.getStringExtra("new_petty_cash_json")
                        val newTransactionCostJson = intent.getStringExtra("new_transaction_cost_json")
                        
                        // Variables to hold the petty cash objects
                        var newPettyCash: PettyCash? = null
                        var transactionCost: PettyCash? = null
                        
                        if (newPettyCashJson != null) {
                            // Parse from JSON
                            try {
                                newPettyCash = Gson().fromJson(newPettyCashJson, PettyCash::class.java)
                                Log.d("PettyCashFragment", "Parsed petty cash from JSON: ${newPettyCash?.id}")
                                
                                if (newTransactionCostJson != null) {
                                    transactionCost = Gson().fromJson(newTransactionCostJson, PettyCash::class.java)
                                    Log.d("PettyCashFragment", "Parsed transaction cost from JSON: ${transactionCost?.id}")
                                }
                            } catch (e: Exception) {
                                Log.e("PettyCashFragment", "Error parsing JSON: ${e.message}", e)
                            }
                        } else {
                            // Get by ID
                            val newPettyCashId = intent.getIntExtra("new_petty_cash_id", -1)
                            val newTransactionCostId = intent.getIntExtra("new_transaction_cost_id", -1)
                            
                            try {
                                if (newPettyCashId != -1) {
                                    newPettyCash = dbHelper?.getPettyCashById(newPettyCashId)
                                    Log.d("PettyCashFragment", "Retrieved petty cash by ID: $newPettyCashId, result: ${newPettyCash != null}")
                                }
                                
                                if (newTransactionCostId != -1) {
                                    transactionCost = dbHelper?.getPettyCashById(newTransactionCostId)
                                    Log.d("PettyCashFragment", "Retrieved transaction cost by ID: $newTransactionCostId, result: ${transactionCost != null}")
                                }
                            } catch (e: Exception) {
                                Log.e("PettyCashFragment", "Error retrieving from database: ${e.message}", e)
                            }
                        }
                        
                        // Check if we have valid objects before updating UI
                        if (newPettyCash == null) {
                            Log.e("PettyCashFragment", "Failed to get valid petty cash object")
                            return@launch
                        }
                        
                        // Instead of modifying the existing list, let's reload the data
                        // This avoids index out of bounds errors
                        val freshData = dbHelper?.getAllPettyCash(1, pageSize, currentSortOption, 
                            currentDateFilter, currentPaymentModes, 
                            currentCustomStartDate, currentCustomEndDate)
                        
                        withContext(Dispatchers.Main) {
                            try {
                                // Replace the entire list with fresh data
                                if (freshData != null && freshData.isNotEmpty()) {
                                    // Create a new list to avoid concurrent modification
                                    pettyCashList = freshData.toMutableList()
                                    
                                    // Update the adapter with the new list
                                    pettyCashAdapter?.updateList(pettyCashList ?: mutableListOf())
                                    
                                    Log.d("PettyCashFragment", "Refreshed entire list with ${pettyCashList?.size} items")
                                    
                                    // Scroll to top
                                    pettyCashRecyclerView?.scrollToPosition(0)
                                } else {
                                    Log.d("PettyCashFragment", "No data returned from database refresh")
                                }
                                
                                // Update UI
                                updateEmptyState()
                                updateViewPagerData()
                                updateMpesaCardDetails()
                            } catch (e: Exception) {
                                Log.e("PettyCashFragment", "Error updating UI: ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PettyCashFragment", "Error handling new petty cash: ${e.message}", e)
                    } finally {
                        // Make sure to close database
                        dbHelper?.closeDatabase()
                    }
                }
            }
        }
    }

    // Add this variable to store the periodic ping job
    private var dbStatusCheckJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true) // Enable options menu in fragment
        setRetainInstance(true) // Retain fragment across orientation changes
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPettyCashBinding.inflate(inflater, container, false)
        toolbar = binding.pettyCashToolbar
        viewPager = binding.viewPager
        dotIndicator = binding.wormDotsIndicator
        dbDotImageView = binding.dbDotImageView
        dbStatusTextLabel = binding.dbStatusLabel
        qbImageView = binding.qbDotImageView
        qbStatusTextLabel = binding.qbStatusLabel
        nestedScrollView = binding.nestedScrollView
        floatingActionButton = binding.floatingActionButton
        truckButton = binding.trucksButton
        ownerButton = binding.ownersButton
        transactorsButton = binding.transactorsButton
        accountsButton = binding.accountsButton
        pettyCashRecyclerView = binding.pettyCashRecycler
        constraintLayout = binding.frameLayout2
        appBarLayout = binding.appBarLayoutPettyCash
        noPettyCashMessageTextView = binding.noPettyCashMessage
        viewAllLink = binding.viewAllLink
        queueButton = binding.queueButton
        reportsButton = binding.reportsButton
        automationButton = binding.automationButton

        dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
        db = dbHelper?.writableDatabase

        CallbackSingleton.refreshCallback = this










        val cards = listOf(R.layout.card_mpesa_petty_cash)

        val adapter = ViewPagerAdapter(cards)
        viewPager?.adapter = adapter



        // Bind the ViewPager to the WormDotsIndicator
        dotIndicator?.attachTo(viewPager!!)


        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Petty Cash"
        }

        // Delay accessing ImageView until the layout is inflated
        viewPager?.post {
            val dotMenuButtonImage: ImageView = requireView().findViewById(R.id.menuIcon)
            // Set click listener on ImageView
            dotMenuButtonImage.setOnClickListener {
                // Show a toast message
                showPopupMenu(it)
            }
            cashImage = viewPager?.rootView?.findViewById<ImageView>(R.id.cashImage)
            cashLabel = viewPager?.rootView?.findViewById<TextView>(R.id.cashLabel)
            mpesaImage = viewPager?.rootView?.findViewById<ImageView>(R.id.mpesaImage)
            totalAmountCard = viewPager?.rootView?.findViewById<TextView>(R.id.total_spend_text)
            updateMpesaCardDetails()
        }


        nestedScrollView?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY -> // the delay of the extension of the FAB is set for 12 items
            if (scrollY > oldScrollY + 12 && floatingActionButton!!.isExtended) {
                floatingActionButton!!.shrink()
            }

            // the delay of the extension of the FAB is set for 12 items
            if (scrollY < oldScrollY - 12 && !floatingActionButton!!.isExtended) {
                floatingActionButton!!.extend()
            }

            // if the nestedScrollView is at the first item of the list then the
            // extended floating action should be in extended state
            if (scrollY == 0) {
                floatingActionButton!!.extend()
            }
        })

        floatingActionButton!!.setOnClickListener {
            // Disable the button to prevent multiple clicks
            floatingActionButton?.isClickable = false

            // Perform fragment transaction
            addNewPettyCash()

            // Re-enable the button once the transaction is complete
            // You can set this to be done after a short delay, or use a callback if you know when the transaction finishes
            lifecycleScope.launch {
                delay(1500)  // Wait for a brief period before re-enabling (you can adjust this)
                floatingActionButton?.isClickable = true
            }
        }

        createLoadingDialog()
        loadingDialog?.show()
        syncMenuItem?.isEnabled = false

        checkDbStatus()
        checkQbStatus()


        val task = lifecycleScope.launch(Dispatchers.IO) {
            mainTask()
            loadingDialog?.dismiss()
        } // Execute the main task in the background

        task.invokeOnCompletion {
            requireActivity().runOnUiThread {
                syncMenuItem?.isEnabled = true
            }
        }

        // Set up click listener for view all
        viewAllLink?.setOnClickListener {
            val intent = Intent(requireContext(), ViewAllPettyCashActivity::class.java)
            startActivity(intent)
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Register receiver
        LocalBroadcastManager.getInstance(requireContext()).apply {
            registerReceiver(pettyCashDeleteReceiver, IntentFilter("pettycash_deleted_action"))
            registerReceiver(pettyCashAddedReceiver, IntentFilter("petty_cash_added_action"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Cancel the periodic ping job
        dbStatusCheckJob?.cancel()
        dbStatusCheckJob = null

        // Nullify the view binding (if using ViewBinding)
        _binding = null

        // Nullify other references to prevent memory leaks
        pettyCashRecyclerView?.adapter = null // This is usually done when clearing references to adapters
        pettyCashAdapter = null // Nullify the adapter reference
        notCheckedTransactions = null // Nullify the list or object reference

        constraintLayout = null
        dbHelper = null
        db = null
        floatingActionButton = null
        nestedScrollView = null
        toolbar = null
        appBarLayout = null
        dotIndicator = null
        viewPager = null
        cashImage = null
        cashLabel= null
        mpesaImage= null
        dbDotImageView = null
        dbStatusTextLabel = null
        qbImageView = null
        qbStatusTextLabel= null
        loadingDialog= null
        loadingText = null
        truckButton= null
        ownerButton= null
        transactorsButton = null
        accountsButton= null
        pettyCashRecyclerView = null
        viewAllLink = null
        automationButton = null
        queueButton = null
        totalAmountCard = null

        // Unregister receiver
        LocalBroadcastManager.getInstance(requireContext()).apply {
            unregisterReceiver(pettyCashDeleteReceiver)
            unregisterReceiver(pettyCashAddedReceiver)
        }
    }

    private fun mainTask() {
        lifecycleScope.launch(Dispatchers.IO) {
            initializeButtons() // Can stay in IO thread
            loadingDataTask(currentPage) // Move loading entirely to IO thread
            requireActivity().runOnUiThread{
                syncMenuItem?.isEnabled = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // Clear the menu to prevent stacking of items
        inflater.inflate(R.menu.petty_cash_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        syncMenuItem = item
        return when (item.itemId) {
            R.id.syncPettyCash -> {
                lifecycleScope.launch(Dispatchers.IO){
                    requireActivity().runOnUiThread {
                        loadingDialog?.show()
                        syncMenuItem?.isEnabled = false
                    }
                    mainTask()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadingDataTask(page: Int) {
        // Prevent further loading if already loading
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch(Dispatchers.IO) {
            dbHelper?.openDatabase()
            try {
                val offset = (page - 1) * pageSize // Calculate the offset
                val pettyCashData = loadPettyCashData(offset, pageSize)

                withContext(Dispatchers.Main) {
                    // If no data is found, show the "No more records" message
                    if (pettyCashData.isNullOrEmpty()) {
                        if (page == 1) {
                            noPettyCashMessageTextView?.visibility = View.VISIBLE
                            Toast.makeText(requireContext(), "No petty cash records found.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "No more records to load.", Toast.LENGTH_SHORT).show()
                        }
                        isLoading = false
                        return@withContext
                    }
                    noPettyCashMessageTextView?.visibility = View.GONE



                    // If it's the first page, initialize the RecyclerView
                    if (page == 1) {
                        setupPettyCashRecyclerView(pettyCashData)
                    } else {
                        // Append data for subsequent pages
                        pettyCashAdapter?.addItems(pettyCashData)
                    }

                    // Increment the page number after successful loading
                    currentPage++
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            } finally {
                dbHelper?.closeDatabase()
            }
        }
    }




    override fun onStart() {
        super.onStart()
        FragmentVisibilityTracker.isPettyCashFragmentVisible = true
    }

    override fun onStop() {
        super.onStop()
        FragmentVisibilityTracker.isPettyCashFragmentVisible = false
    }

    private fun loadPettyCashData(limit: Int, offset: Int): MutableList<PettyCash> {
        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())
        }

        // Modify the query in DbHelper to support LIMIT and OFFSET
        return dbHelper?.getAllPettyCashForCurrentMonthWithPagination(offset, limit)?.toMutableList() ?: mutableListOf()
    }


    private fun setupPettyCashRecyclerView(pettyCashList: MutableList<PettyCash>?) {
        pettyCashAdapter = PettyCashAdapter(requireContext(), pettyCashList ?: mutableListOf(), requireActivity().supportFragmentManager, this)
        pettyCashRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        pettyCashRecyclerView?.adapter = pettyCashAdapter

        // Add scroll listener for pagination logic
        pettyCashRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                // Check if we have reached the end of the list and there is more data to load
                if (!isLoading && lastVisibleItemPosition == totalItemCount - 1 && hasMoreData()) {
                    loadMoreData()
                }
            }
        })

        // Detect when the NestedScrollView has been scrolled to the bottom
        nestedScrollView?.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // Calculate the max scrollable distance (bottom)
            val maxScroll = nestedScrollView!!.getChildAt(0).height - nestedScrollView!!.height

            // Check if we are at the bottom of the NestedScrollView
            if (scrollY == maxScroll) {
                println("At maxScroll, bottom reached")

                // Trigger pagination if not already loading and more data is available
                isLoading = false
                if (!isLoading && hasMoreData()) {
                    println("Triggering pagination")
                    loadingDialog?.show()
                    val task = lifecycleScope.launch(Dispatchers.IO) {
                        loadingDataTask(currentPage)  // Trigger loading more data
                        loadingDialog?.dismiss()

                    }
                }
            }
        }

    }


    private fun hasMoreData(): Boolean {
        // Check if there is more data based on the current page and page size
        val totalRecords = getTotalPettyCashRecords()  // Fetch the total count of records from the database
        val currentDataSize = pettyCashAdapter?.itemCount ?: 0  // Get the current data size from the adapter

        // Return true if there are more records to load
        return totalRecords > currentDataSize
    }

    private fun getTotalPettyCashRecords(): Int {
        var totalRecords = 0

        dbHelper?.openDatabase()
            try {
                // Modify your DbHelper to get the total count of records in the PettyCash table
                totalRecords = dbHelper?.getCountPettyCashForCurrentMonth() ?: 0
            } finally {
                dbHelper?.closeDatabase()
            }
        return totalRecords
    }

    private fun loadMoreData() {
        if (isLoading) return  // Prevent multiple calls when already loading
        isLoading = true
        loadingDataTask(currentPage)

    }





    private fun initializeButtons() {
        truckButton?.setOnClickListener {
            trucksButtonAction()
        }
        ownerButton?.setOnClickListener {
            ownersButtonAction()
        }
        transactorsButton?.setOnClickListener {
            transactorsButtonAction()
        }
        accountsButton?.setOnClickListener {
            accountsButtonAction()
        }
        queueButton?.setOnClickListener {
            queueButtonAction()
        }
        reportsButton?.setOnClickListener {
            reportsButtonAction()
        }
        automationButton?.setOnClickListener {
            automationButtonAction()
        }

    }

    private fun automationButtonAction() {
        val intent = Intent(requireContext(), AutomationActivity::class.java)
        startActivity(intent)
    }


    private fun reportsButtonAction() {
        val intent = Intent(requireContext(), com.example.pettysms.reports.ReportsActivity::class.java)
        startActivity(intent)
    }

    private fun queueButtonAction() {
        val intent = Intent(requireContext(), QueueActivity::class.java)
        startActivity(intent)
    }

    private fun accountsButtonAction() {
        val intent = Intent(activity, AccountsActivity::class.java)

        intent.putExtra("key", "value")


        startActivity(intent)
    }

    private fun transactorsButtonAction() {
        val intent = Intent(activity, TransactorsActivity::class.java)

        intent.putExtra("key", "value")

        startActivity(intent)
    }

    private fun ownersButtonAction() {
        val intent = Intent(activity, OwnersActivity::class.java)

        // Optionally, you can pass data to the second activity using extras
        intent.putExtra("key", "value")

        // Start the second activity
        startActivity(intent)
    }

    private fun trucksButtonAction() {
        val intent = Intent(activity, TrucksActivity::class.java)

        // Optionally, you can pass data to the second activity using extras
        intent.putExtra("key", "value")

        // Start the second activity
        startActivity(intent)

    }

    private fun addNewPettyCash() {
        val fragmentManager = requireActivity().supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag("fragment_add_petty_cash")

        if (existingFragment != null && existingFragment.isVisible) {
            // If the fragment is already visible, do nothing
            return
        }

        // If the fragment is not visible, create and show it
        val dialogFragment = AddPettyCashFragment()
        dialogFragment.setOnAddPettyCashListener(this)
        dialogFragment.show(fragmentManager, "fragment_add_petty_cash")
    }


    private fun checkQbStatus() {
        qbStatus = false
        qbStatusTextLabel?.let { updateStatus(qbStatus, qbImageView!!, it) }
    }


    private fun checkDbStatus() {
        // Initial status is offline until we confirm connectivity
        dbStatus = false
        dbDotImageView?.let { dbStatusTextLabel?.let { it1 -> updateStatus(dbStatus, it, it1) } }
        
        // Cancel any existing job
        dbStatusCheckJob?.cancel()
        
        // Start a new periodic job to check server status every 5 minutes
        dbStatusCheckJob = lifecycleScope.launch {
            while (isActive) {
                pingServer()
                // Wait for 5 minutes before pinging again
                delay(5 * 60 * 1000) // 5 minutes in milliseconds
            }
        }
    }
    
    private fun pingServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("PettyCashFragment", "Pinging server at api.abdulcon.com...")
                
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url("https://${SERVER_IP}")
                    .build()
                
                val response = client.newCall(request).execute()
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("PettyCashFragment", "Server ping successful: ${response.code}")
                        dbStatus = true
                    } else {
                        Log.d("PettyCashFragment", "Server ping failed with code: ${response.code}")
                        dbStatus = false
                    }
                    
                    // Update the UI
                    dbDotImageView?.let { dbStatusTextLabel?.let { it1 -> updateStatus(dbStatus, it, it1) } }
                }
            } catch (e: Exception) {
                Log.e("PettyCashFragment", "Error pinging server: ${e.message}", e)
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    dbStatus = false
                    dbDotImageView?.let { dbStatusTextLabel?.let { it1 -> updateStatus(dbStatus, it, it1) } }
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun updateStatus(status: Boolean, imageView: ImageView, statusLabel: TextView) {
        if (status) {
            imageView.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.new_green_color
                ), PorterDuff.Mode.SRC_IN
            )
            statusLabel.text = "Online"
            addFadeInAndOutAnimation(imageView)

        } else {
            imageView.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.grey_color
                ), PorterDuff.Mode.SRC_IN
            )
            imageView.clearAnimation()
            statusLabel.text = "Offline"
        }
    }

    private fun addFadeInAndOutAnimation(imageView: ImageView){

        val fadeInOut = AlphaAnimation(0f, 1f)
        fadeInOut.duration = 1000 // Duration for each fade (in milliseconds)
        fadeInOut.repeatMode = Animation.REVERSE // Reverse the animation
        fadeInOut.repeatCount = Animation.INFINITE // Repeat the animation indefinitely

        val handler = android.os.Handler()
        val delay = 2000 // Delay in milliseconds before starting the animation

        handler.postDelayed({
            imageView.startAnimation(fadeInOut)
        }, delay.toLong())

    }



    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, GravityCompat.END, 0, R.style.PopupMenuStyle)
        popupMenu.menuInflater.inflate(R.menu.petty_cash_card_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_cash -> {
                    updateCashCardDetails()
                    true
                }
                R.id.menu_mpesa -> {
                    updateMpesaCardDetails()
                    true
                }
                else -> false
            }
        }
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    private fun updateMpesaCardDetails() {
        try {
            // Safety check for null views
            if (mpesaImage == null || cashImage == null || cashLabel == null || totalAmountCard == null) {
                Log.d("PettyCashFragment", "Card views not initialized yet")
                return
            }
            
            // Set UI to M-Pesa mode
            mpesaImage?.visibility = View.VISIBLE
            cashImage?.visibility = View.GONE
            cashLabel?.visibility = View.GONE
            
            // Calculate M-Pesa total for current month using optimized database query
            lifecycleScope.launch(Dispatchers.IO) {
                var db: SQLiteDatabase? = null
                try {
                    // Ensure we have a valid dbHelper
                    if (dbHelper == null) {
                        dbHelper = DbHelper(requireContext())
                    }
                    
                    // Open the database explicitly
                    db = dbHelper?.writableDatabase
                    
                    // Get total directly from database without loading all records
                    val mpesaTotal = dbHelper?.getTotalPettyCashAmountForCurrentMonthByPaymentMode("M-Pesa") ?: 0.0
                    
                    withContext(Dispatchers.Main) {
                        // Format the amount as a simple number with commas
                        val formattedAmount = String.format("%,.0f", mpesaTotal)
                        
                        // Update the total amount text
                        totalAmountCard?.text = formattedAmount
                        
                        Log.d("PettyCashFragment", "Updated M-Pesa card with total: $mpesaTotal")
                    }
                } catch (e: Exception) {
                    Log.e("PettyCashFragment", "Error calculating M-Pesa total: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PettyCashFragment", "Error updating M-Pesa card details: ${e.message}", e)
        }
    }

    private fun updateCashCardDetails() {
        try {
            // Safety check for null views
            if (mpesaImage == null || cashImage == null || cashLabel == null || totalAmountCard == null) {
                Log.d("PettyCashFragment", "Card views not initialized yet")
                return
            }
            
            // Set UI to Cash mode
            cashImage?.visibility = View.VISIBLE
            cashLabel?.visibility = View.VISIBLE
            mpesaImage?.visibility = View.GONE
            
            // Calculate Cash total for current month using optimized database query
            lifecycleScope.launch(Dispatchers.IO) {
                var db: SQLiteDatabase? = null
                try {
                    // Ensure we have a valid dbHelper
                    if (dbHelper == null) {
                        dbHelper = DbHelper(requireContext())
                    }
                    
                    // Open the database explicitly
                    db = dbHelper?.writableDatabase
                    
                    // Get total directly from database without loading all records
                    val cashTotal = dbHelper?.getTotalPettyCashAmountForCurrentMonthByPaymentMode("Cash") ?: 0.0
                    
                    withContext(Dispatchers.Main) {
                        // Format the amount as a simple number with commas
                        val formattedAmount = String.format("%,.0f", cashTotal)
                        
                        // Update the total amount text
                        totalAmountCard?.text = formattedAmount
                        
                        Log.d("PettyCashFragment", "Updated Cash card with total: $cashTotal")
                    }
                } catch (e: Exception) {
                    Log.e("PettyCashFragment", "Error calculating Cash total: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PettyCashFragment", "Error updating Cash card details: ${e.message}", e)
        }
    }

    override fun onAddPettyCash(pettyCash: PettyCash, transactionCostPettyCash: PettyCash?) {
        Log.d("PettyCashFragment", "onAddPettyCash called for ID: ${pettyCash.id}")
        loadingDialog?.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Refresh the entire first page of data
                val freshData = dbHelper?.getAllPettyCash(
                    1, pageSize, currentSortOption,
                    currentDateFilter, currentPaymentModes,
                    currentCustomStartDate, currentCustomEndDate
                )

                withContext(Dispatchers.Main) {
                    try {
                        // Replace the entire list with fresh data
                        if (freshData != null) {
                            // Create a new list to avoid concurrent modification
                            pettyCashList = freshData.toMutableList()
                            
                            // Update the adapter with the new list
                            pettyCashAdapter?.updateList(pettyCashList ?: mutableListOf())
                            
                            // Reset current page to 1 since we're refreshing
                            currentPage = 1
                            
                            // Scroll to top
                            pettyCashRecyclerView?.scrollToPosition(0)
                            
                            Log.d("PettyCashFragment", "Refreshed list with ${pettyCashList?.size} items")
                    } else {
                            Log.d("PettyCashFragment", "No data returned from database refresh")
                        }
                        
                        // Update UI states
                        updateEmptyState()
                        updateViewPagerData()
                        // Update card totals based on current view
                        updateCardTotals()
                        
                        loadingDialog?.dismiss()
                        Log.d("PettyCashFragment", "Successfully refreshed data after update")
                    } catch (e: Exception) {
                        Log.e("PettyCashFragment", "Error updating UI: ${e.message}", e)
                        loadingDialog?.dismiss()
                        Toast.makeText(
                            requireContext(),
                            "Error updating display: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PettyCashFragment", "Error refreshing data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                loadingDialog?.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Error refreshing data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }



    @SuppressLint("SetTextI18n")
    private fun createLoadingDialog(): AlertDialog {
        // Create a custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)
        loadingText?.text = "Syncing... Please Wait"

        // Show loading dialog
            loadingDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(customView)
                .setCancelable(false)
                .create()

        return loadingDialog as AlertDialog
    }

    companion object {
        const val SERVER_IP = "api.abdulcon.com"
        const val PETTY_CASH_VIEWER_REQUEST = 1001
        var updatedPettyCashIds = mutableSetOf<Int>()  // Store updated petty cash IDs
        var isRefreshSingletonFromViewAllPettyCashActivity = false
    }

    object FragmentVisibilityTracker {
        var isPettyCashFragmentVisible: Boolean = false
    }

    object CallbackSingleton {
        var refreshCallback: RefreshRecyclerViewCallback? = null
    }

    override fun onRefresh() {
        // Show the loading dialog
        loadingDialog?.show()
        syncMenuItem?.isEnabled = false

        // Launch a coroutine in the lifecycleScope to reload data
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("PettyCashFragment", "Starting full data refresh...")
                
                // Get fresh data from database
                val freshData = dbHelper?.getAllPettyCash(
                    1, pageSize, currentSortOption,
                    currentDateFilter, currentPaymentModes,
                    currentCustomStartDate, currentCustomEndDate
                )

                withContext(Dispatchers.Main) {
                    try {
                        // Replace the entire list with fresh data
                        if (freshData != null) {
                            // Create a new list to avoid concurrent modification
                            pettyCashList = freshData.toMutableList()
                            
                            // Update the adapter with the new list
                            pettyCashAdapter?.updateList(pettyCashList ?: mutableListOf())
                            
                            // Reset current page to 1 since we're refreshing
                            currentPage = 1
                            
                            // Scroll to top
                            pettyCashRecyclerView?.scrollToPosition(0)
                            
                            Log.d("PettyCashFragment", "Refreshed list with ${pettyCashList?.size} items")
                        } else {
                            Log.d("PettyCashFragment", "No data returned from database refresh")
                        }
                        
                        // Update UI states
                        updateEmptyState()
                        updateViewPagerData()
                        // Update card totals based on current view
                        updateCardTotals()
                        
                        Log.d("PettyCashFragment", "Successfully completed full data refresh")
                    } catch (e: Exception) {
                        Log.e("PettyCashFragment", "Error updating UI during refresh: ${e.message}", e)
                        Toast.makeText(
                            requireContext(),
                            "Error updating display: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PettyCashFragment", "Error during full refresh: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error refreshing data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog?.dismiss() // Hide the loading dialog
                    syncMenuItem?.isEnabled = true // Re-enable the sync menu item
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("PettyCashFragment", "onActivityResult called")

        try {
            if (requestCode == PETTY_CASH_VIEWER_REQUEST && resultCode == Activity.RESULT_OK) {
                data?.let { intent ->
                    val pettyCashId = intent.getIntExtra("updated_petty_cash_id", -1)
                    val transactionCostId = intent.getIntExtra("transaction_cost_id", -1)

                    if (pettyCashId != -1) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Fetch updated petty cash from database
                                val updatedPettyCash = dbHelper?.getPettyCashById(pettyCashId)
                                
                                // Fetch transaction cost if present
                                val transactionCost = if (transactionCostId != -1) {
                                    dbHelper?.getPettyCashById(transactionCostId)
                                } else null

                                withContext(Dispatchers.Main) {
                                    // Update UI with fetched data
                                    updatedPettyCash?.let { pettyCash ->
                                        pettyCashAdapter?.findItemIndexById(pettyCash.id!!)?.let { index ->
                                            pettyCashAdapter?.updateItem(index, pettyCash)
                                            Log.d("PettyCashFragment", "Updated main petty cash at position: $index")
                                        }
                                    }

                                    // Update transaction cost if present
                                    transactionCost?.let { tc ->
                                        pettyCashAdapter?.findItemIndexById(tc.id!!)?.let { index ->
                                            pettyCashAdapter?.updateItem(index, tc)
                                            Log.d("PettyCashFragment", "Updated transaction cost at position: $index")
                                        }
                                    }

                                    // Scroll to show the updated item if needed
                                    updatedPettyCash?.let { pettyCash ->
                                        pettyCashAdapter?.findItemIndexById(pettyCash.id!!)?.let { index ->
                                            pettyCashRecyclerView?.scrollToPosition(index)
                                        }
                                    }
                                    
                                    // Update card totals after updating items
                                    updateCardTotals()
                                }
                            } catch (e: Exception) {
                                Log.e("PettyCashFragment", "Error updating database: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        requireContext(),
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
            Log.e("PettyCashFragment", "Error in onActivityResult: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("PettyCashFragment", "onResume called")

        
        // Refresh the RecyclerView data
        refreshPettyCashList()
        if (isRefreshSingletonFromViewAllPettyCashActivity){
            this.onRefresh()
            isRefreshSingletonFromViewAllPettyCashActivity = false
        }
    }

    private fun refreshPettyCashListOnRefresh(){
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get only the updated petty cash items


                val updatedItems = updatedPettyCashIds.mapNotNull { id ->
                    dbHelper?.getPettyCashById(id)
                }

                withContext(Dispatchers.Main) {
                    if (updatedItems.isNotEmpty()) {
                        // Update each modified item in the adapter
                        updatedItems.forEach { pettyCash ->
                            pettyCashAdapter?.findItemIndexById(pettyCash.id!!)?.let { index ->
                                pettyCashAdapter?.updateItem(index, pettyCash)
                                Log.d("PettyCashFragment", "Updated item at position: $index")

                                // Scroll to the first updated item
                                if (pettyCash.id == updatedItems.first().id) {
                                    pettyCashRecyclerView?.scrollToPosition(index)
                                }
                            }
                            // Remove from the update set after updating
                            updatedPettyCashIds.remove(pettyCash.id)
                        }

                        // Update view pager and Mpesa card
                        updateViewPagerData()
                        updateMpesaCardDetails()

                        Log.d("PettyCashFragment", """
                            Updated specific items:
                            Count: ${updatedItems.size}
                            IDs: ${updatedItems.map { it.id }}
                            Remaining Updates: ${updatedPettyCashIds.size}
                        """.trimIndent())
                    }
                }
            } catch (e: Exception) {
                Log.e("PettyCashFragment", "Error updating items: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error updating items: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun refreshPettyCashList() {
        if (updatedPettyCashIds.isEmpty()) {
            Log.d("PettyCashFragment", "No items to update")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get only the updated petty cash items


                val updatedItems = updatedPettyCashIds.mapNotNull { id ->
                    dbHelper?.getPettyCashById(id)
                }

                withContext(Dispatchers.Main) {
                    if (updatedItems.isNotEmpty()) {
                        // Update each modified item in the adapter
                        updatedItems.forEach { pettyCash ->
                            pettyCashAdapter?.findItemIndexById(pettyCash.id!!)?.let { index ->
                                pettyCashAdapter?.updateItem(index, pettyCash)
                                Log.d("PettyCashFragment", "Updated item at position: $index")
                                
                                // Scroll to the first updated item
                                if (pettyCash.id == updatedItems.first().id) {
                                    pettyCashRecyclerView?.scrollToPosition(index)
                                }
                            }
                            // Remove from the update set after updating
                            updatedPettyCashIds.remove(pettyCash.id)
                        }

                        // Update view pager and Mpesa card
                        updateViewPagerData()
                        updateMpesaCardDetails()

                        Log.d("PettyCashFragment", """
                            Updated specific items:
                            Count: ${updatedItems.size}
                            IDs: ${updatedItems.map { it.id }}
                            Remaining Updates: ${updatedPettyCashIds.size}
                        """.trimIndent())
                    }
                }
            } catch (e: Exception) {
                Log.e("PettyCashFragment", "Error updating items: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error updating items: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateViewPagerData() {
        // Update view pager data if it exists
        viewPager?.let { pager ->
            pager.adapter?.notifyDataSetChanged()
        }
    }

    // Update filter methods to save current state
    fun updateFilters(
        dateFilter: String,
        paymentModes: List<String>,
        sortOption: String,
        customStartDate: String? = null,
        customEndDate: String? = null
    ) {
        currentDateFilter = dateFilter
        currentPaymentModes = paymentModes
        currentSortOption = sortOption
        currentCustomStartDate = customStartDate
        currentCustomEndDate = customEndDate
        
        refreshPettyCashList()
    }

    private fun updateEmptyState() {
        try {
            // Get the actual size from adapter
            val currentListSize = pettyCashAdapter?.itemCount ?: 0
            Log.d("PettyCashFragment", "Checking empty state - Adapter list size: $currentListSize")

            if (currentListSize == 0) {
                // Show empty state
                noPettyCashMessageTextView?.visibility = View.VISIBLE
                pettyCashRecyclerView?.visibility = View.GONE
                viewAllLink?.visibility = View.GONE
                Log.d("PettyCashFragment", "Showing empty state - list is empty")
            } else {
                // Show list
                noPettyCashMessageTextView?.visibility = View.GONE
                pettyCashRecyclerView?.visibility = View.VISIBLE
                viewAllLink?.visibility = View.VISIBLE
                Log.d("PettyCashFragment", "Showing list - $currentListSize items")
            }

            // Update UI components that depend on list state
            try {
                updateViewPagerData()
            } catch (e: Exception) {
                Log.e("PettyCashFragment", "Error updating view pager: ${e.message}", e)
            }
            
            try {
                // Update card totals when empty state changes
                updateCardTotals()
            } catch (e: Exception) {
                Log.e("PettyCashFragment", "Error updating card totals: ${e.message}", e)
            }

            Log.d("PettyCashFragment", "Empty state updated, RecyclerView visibility: ${pettyCashRecyclerView?.visibility == View.VISIBLE}")
        } catch (e: Exception) {
            Log.e("PettyCashFragment", "Error in updateEmptyState: ${e.message}", e)
        }
    }

    private fun updateCardTotals() {
        // Check which card is currently visible and update accordingly
        if (mpesaImage?.visibility == View.VISIBLE) {
            updateMpesaCardDetails()
        } else {
            updateCashCardDetails()
        }
    }

}