package com.example.pettysms

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.example.pettysms.databinding.ActivityViewAllTransactionsBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.gson.Gson
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
    private lateinit var searchView: SearchView
    private lateinit var binding: ActivityViewAllTransactionsBinding
    private lateinit var loadingDialog: AlertDialog
    private lateinit var loadingText: TextView
    private lateinit var clearAllTextView: TextView
    private lateinit var progressSuggestions: ProgressBar
    private lateinit var noResultsTextView: TextView
    private lateinit var appBar: AppBarLayout
    private lateinit var adapter: MpesaTransactionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fadeInAnimation: Animation
    private lateinit var fadeOutAnimation: Animation
    private lateinit var verticalShrinkFadeOut: Animation
    private lateinit var fadeInselectLayoutAnimation: Animation
    private lateinit var layoutSelectAll: LinearLayout
    private lateinit var selectAllCheckBox: CheckBox



    private var actionMode: ActionMode? = null
    private val selectedTransactions = HashSet<Int>()
    private val removedTransactions = HashSet<Int>()



    private var allMpesaTransactions = mutableListOf<MpesaTransaction>()
    private var activeTransactions = mutableListOf<MpesaTransaction>()

    private var searchBar: SearchBar? = null
    private var db_helper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private val activityName = this::class.simpleName
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
        appBar = binding.appBarLayout
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        fadeInselectLayoutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_select_all_layout)
        layoutSelectAll = binding.selectAllLayoutViewAllTransactions
        selectAllCheckBox = binding.checkboxSelectAllItems
        verticalShrinkFadeOut = AnimationUtils.loadAnimation(this, R.anim.vertical_shrink_fade_out)

        clearAllTextView.setOnClickListener {
            clearAllHistory()
        }

        searchBar?.setOnMenuItemClickListener { menuItem ->
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


        searchView.editText.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            updateSearch(searchView.text.toString())
            searchView.hide()
                false
            }

        setupSearchView()

        // Add a listener to the search view to detect visibility changes
        /*if (!searchView.isShowing){
            swipeRefreshLayout.isEnabled
        }*/

        Log.d(activityName,searchView.isShowing.toString())


        setContentView(binding.root)

    }

    fun updateSearch(text:String){
        //searchBar?.text = text.toString()
    }

    private fun setupSearchView() {
        if(!searchHistory.isNullOrEmpty()){
            showHistoryitems("")
        }
        // Define the color for the status bar when the search view is focused
        val searchFocusedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer,"")
        // Define the default color for the status bar
        val defaultStatusBarColor = this.window.statusBarColor
        searchView.addTransitionListener { searchView: com.google.android.material.search.SearchView?, previousState: SearchView.TransitionState?, newState: SearchView.TransitionState ->
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
                            mpesaTransaction.mpesaDepositor.toString()
                                .contains(query, ignoreCase = true) ||
                            mpesaTransaction.amount.toString().contains(query, ignoreCase = true) ||
                            /*mpesaTransaction.mpesa_code.toString()
                                .contains(query, ignoreCase = true) ?: false ||*/
                            removeExtraSpaces(mpesaTransaction.recipient?.name.toString())
                                .contains(query, ignoreCase = true) ||
                            mpesaTransaction.transaction_type.toString()
                                .contains(query, ignoreCase = true) ||
                            formatDate(dateToSearch.toString())
                                .contains(query, ignoreCase = true) ||
                            mpesaTransaction.sender?.name?.contains(query, ignoreCase = true) ?: false
                }

                if (!searchResults.isEmpty()){
                    Log.d(activityName, searchResults.first().recipient?.name.toString())
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
            val suggestionsAdapter = SuggestionsAdapter(searchResults, query, this@ViewAllTransactionsActivity) { suggestion ->
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
    fun removeExtraSpaces(input: String): String {
        // Replace multiple spaces with a single space using regex
        return input.replace(Regex("\\s+"), " ")
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

    private fun showHistoryitems(quƒery: String){

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
        recyclerView = binding.recyclerView
        activeTransactions = transactions
        adapter = MpesaTransactionAdapter(this,transactions, object : MpesaTransactionAdapter.OnItemClickListener{
            override fun onItemClick(transactionId: Int?) {
                if (actionMode != null) {
                    toggleSelection(transactionId)
                }else{
                    val selectedTransaction = activeTransactions.find { it.id == transactionId }
                    val gson = Gson()
                    val mpesaTransactionJson = gson.toJson(selectedTransaction)

                    val intent = Intent(this@ViewAllTransactionsActivity, TransactionViewer::class.java).apply {
                        putExtra("mpesaTransactionJson", mpesaTransactionJson)
                    }
                    transactionViewerLauncher.launch(intent)

                }
            }

            override fun onItemLongClick(transactionId: Int?) {

                    if (actionMode == null) {
                        adapter.setActionModeStatus(true)
                        actionMode = startSupportActionMode(actionModeCallback)!!
                        //println("hello")
                        toggleSelection(transactionId)
                    }
                    else{
                        actionMode!!.finish()
                    }


            }

        })

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

    private val transactionViewerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // TransactionViewer activity is finished
            // Execute your code here
            println("habari yako")
            allMpesaTransactions = db_helper?.getAllMpesaTransactions() ?: mutableListOf()
            updateMainRecyclerView(allMpesaTransactions)

        }else{
            println("hallo hallo")
            allMpesaTransactions = db_helper?.getAllMpesaTransactions() ?: mutableListOf()
            updateMainRecyclerView(allMpesaTransactions)
        }
    }



    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            //val popupMenuBackgroundColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.popupMenuBackground,"")
            //setSystemBarColor(requireActivity(), R.color.red_color);

            val customView = LayoutInflater.from(this@ViewAllTransactionsActivity).inflate(R.layout.custom_action_mode_layout, null)
            //mode?.customView = customView

            mode?.menuInflater?.inflate(R.menu.context_menu, menu)

            layoutSelectAll.startAnimation(fadeInselectLayoutAnimation)
            layoutSelectAll.visibility = View.VISIBLE
            changeStatusBarColorWithAnimation(com.google.android.material.R.attr.colorSurfaceContainer)
            selectAllCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if(isChecked){
                    selectedTransactions.clear()
                    for (activetransaction in activeTransactions){
                        selectedTransactions.add(activetransaction.id!!)
                    }
                    updateActionModeTitle()
                    adapter.setSelectedTransactions(selectedTransactions,true)

                }else{
                    selectedTransactions.clear()
                    updateActionModeTitle()
                    adapter.setSelectedTransactions(selectedTransactions, true)

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
                    //actionMode?.finish()
                    return true
                }
                // Add other actions as needed
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear the selection and finish ActionMode
            selectedTransactions.clear()
            adapter.reinitializeAdapter()
            adapter.setActionModeStatus(true)
            changeStatusBarColorWithAnimation(com.google.android.material.R.attr.colorSurface)
            layoutSelectAll.startAnimation(verticalShrinkFadeOut)
            layoutSelectAll.visibility = View.GONE
            actionMode = null
            selectAllCheckBox.isChecked = false
        }
    }

    @SuppressLint("RestrictedApi")
    private fun changeStatusBarColorWithAnimation(colorResId: Int) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Retrieve the color from the color resource ID
            val newStatusBarColor = MaterialColors.getColor(this, colorResId,"")

            // Get the current status bar color
            val currentStatusBarColor = this.window.statusBarColor

            // Create a ValueAnimator to animate the color change
            val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentStatusBarColor, newStatusBarColor)
            colorAnimator.addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                // Set the animated color to the status bar
                this.window.statusBarColor = animatedColor
            }

            // Set up animation duration
            colorAnimator.duration = 490 // Adjust the duration as needed

            // Start the color animation
            colorAnimator.start()

            // For a light status bar, you may need to adjust the system UI visibility
            if (isColorLight(newStatusBarColor)) {
                this.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                this.window.decorView.systemUiVisibility = 0
            }
        }
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness =
            1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(
                color
            )) / 255
        return darkness < 0.5
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
        adapter.setSelectedTransactions(selectedTransactions, false)
    }


    private fun deleteSelectedTransactions() {
        // Handle the deletion of selected transactions
        // Update your data source or perform other actions
        showWarnigDialog()
    }

    private fun showWarnigDialog(){

        MaterialAlertDialogBuilder(this)
            .setTitle("Warning")
            .setMessage(getDeleteMessageString())
            .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
            .setNegativeButton("Dismiss") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Confirm") { dialog, which ->
                // Respond to positive button press
                adapter.setRemovedTransactions(selectedTransactions, selectAllCheckBox.isChecked)
                Log.d(activityName, "deleted utilised")

                if(selectAllCheckBox.isChecked){
                    deleteAllTransactions()
                    selectAllCheckBox.isChecked = false
                }
                else{
                    deleteSeletectedTransactionsFromDb(selectedTransactions)
                }

                clearSelectionWithoutNotifyingAdapter()

                updateActionModeTitle()
            }
            .show()
    }

    private fun getDeleteMessageString() : SpannableStringBuilder{
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



    private fun deleteSeletectedTransactionsFromDb(selectedTransactions: HashSet<Int>) {
        for (selectedTransaction in selectedTransactions){
            db_helper?.deleteTransaction(selectedTransaction)
            Log.d(activityName, "Deleted Transaction: " + selectedTransaction)
        }
    }

    fun deleteAllTransactions(){
        db_helper?.deleteAllTransactions()
    }

    private fun clearSelection() {
        selectedTransactions.clear()
        adapter.clearSelection()
    }

    private fun clearSelectionWithoutNotifyingAdapter(){
        selectedTransactions.clear()
    }

    private fun updateActionModeTitle() {
        actionMode?.title = "${selectedTransactions.size} selected"
    }



    private fun showSecondaryMenu() {
        val anchorView: View? = searchBar?.findViewById(R.id.search_bar_options)
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
        Log.d(activityName, "In the beginning: " + allMpesaTransactions.first().smsText)
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


        Log.d(activityName, "mafilter: " + dateRange)

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
            Log.d(activityName, "StartDate CUSTOM: " + startDate + "EndDateDate CUSTOM: " + endDate )

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
                        compareValues("${obj1.recipient?.name}${obj1.sender?.name}${obj1.mpesaDepositor}",
                            "${obj2.recipient?.name}${obj2.sender?.name}${obj2.mpesaDepositor}")
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

}