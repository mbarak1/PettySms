package com.example.pettysms

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray


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
    private var accountsButton: Button? = null
    private var pettyCashRecyclerView: RecyclerView? = null
    private var noPettyCashMessageTextView: TextView? = null




    var dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
    var db = dbHelper?.writableDatabase



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
            updateMpesaCardDetails()
        }


        checkDbStatus()
        checkQbStatus()

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


        val task = lifecycleScope.launch(Dispatchers.IO) {
            mainTask()

        } // Execute the main task in the background

        task.invokeOnCompletion {
            requireActivity().runOnUiThread {
                syncMenuItem?.isEnabled = true
            }
        }


        // Inflate the layout for this fragment
        return binding.root
    }


    private fun mainTask() {
        lifecycleScope.launch(Dispatchers.IO) {
            initializeButtons() // Can stay in IO thread
            loadingDataTask(currentPage) // Move loading entirely to IO thread
            requireActivity().runOnUiThread{
                loadingDialog?.dismiss()
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

    override fun onDestroyView() {
        super.onDestroyView()

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
        qbStatus = true
        qbStatusTextLabel?.let { updateStatus(qbStatus, qbImageView!!, it) }

        // Use lifecycleScope to handle the delay
        lifecycleScope.launch {
            delay(10000) // Wait for 10 seconds
            qbStatus = false
            qbImageView?.let { qbStatusTextLabel?.let { it1 -> updateStatus(qbStatus, it, it1) } }
        }
    }


    private fun checkDbStatus() {
        dbStatus = true
        dbDotImageView?.let { dbStatusTextLabel?.let { it1 -> updateStatus(dbStatus, it, it1) } }

        lifecycleScope.launch {
            delay(5000)
            dbStatus = false
            dbDotImageView?.let { dbStatusTextLabel?.let { it1 -> updateStatus(dbStatus, it, it1) } }
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
        cashImage?.visibility = View.GONE
        cashLabel?.visibility = View.GONE
        mpesaImage?.visibility = View.VISIBLE

    }

    private fun updateCashCardDetails() {
        cashImage?.visibility = View.VISIBLE
        cashLabel?.visibility = View.VISIBLE
        mpesaImage?.visibility = View.GONE
    }

    override fun onAddPettyCash(pettyCash: PettyCash, transactionCostPettyCash: PettyCash?) {
        Log.d("PettyCashFragment", "onAddPettyCash called")
        println("Petty Cash Saved: ${pettyCash.id}")
        loadingDialog?.show()

        lifecycleScope.launch(Dispatchers.IO) {
            // Retrieve the specific item from the database by its ID (adjust the method if needed)
            val newOrUpdatedItem = pettyCash
            val newOrUpdatetransactionCost = transactionCostPettyCash



            withContext(Dispatchers.Main) {
                newOrUpdatedItem.let {
                    // If the adapter already has this item, update it; otherwise, add it to the beginning
                    val existingIndex = it.id?.let { it1 -> pettyCashAdapter?.findItemIndexById(it1) }
                    if (existingIndex != null && existingIndex >= 0) {
                        pettyCashAdapter?.updateItem(existingIndex, it)
                    } else {
                        pettyCashAdapter?.addItemToTop(it)
                        Log.d("PettyCashFragment", "New Petty Cash Added Trucks: " + it.trucks?.size)
                        pettyCashRecyclerView?.scrollToPosition(0) // Scroll to show the new item
                    }
                }

                if (newOrUpdatetransactionCost !=  null){
                    newOrUpdatetransactionCost.let {
                        val existingIndex = it.id?.let { it1 -> pettyCashAdapter?.findItemIndexById(it1) }
                        if (existingIndex != null && existingIndex >= 0) {
                            pettyCashAdapter?.updateItem(existingIndex, it)
                        } else {
                            pettyCashAdapter?.addItemToTop(it)
                            //pettyCashRecyclerView?.scrollToPosition(0) // Scroll to show the new item
                        }
                    }
                }

                loadingDialog?.dismiss()
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
        val task = lifecycleScope.launch(Dispatchers.IO) {
            try {
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error refreshing data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog?.dismiss() // Hide the loading dialog
                    syncMenuItem?.isEnabled = true // Re-enable the sync menu item
                }
            }
        }
    }



}