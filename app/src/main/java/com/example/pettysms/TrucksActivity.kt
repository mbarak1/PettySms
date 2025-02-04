package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityTrucksBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView.TransitionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable


class TrucksActivity : AppCompatActivity(), SortAndFilterTrucks.OnApplyClickListener,
    AddOrEditTruckDialog.OnAddTruckListener {
    private lateinit var trucksRecyclerView: RecyclerView
    private lateinit var binding: ActivityTrucksBinding
    private lateinit var truckAdapter: TruckAdapter
    private lateinit var nestedScrollingView: NestedScrollView
    private lateinit var floatingActionButton: ExtendedFloatingActionButton
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: com.google.android.material.search.SearchView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var loadingText: TextView
    private lateinit var loadingDialog: AlertDialog
    private lateinit var noTrucksMessageText: TextView
    private lateinit var clearAllTextView: TextView

    private var keyValueMapToFilterAndSortFragment = mutableMapOf<String, MutableList<String>>()
    private var trucks = mutableListOf<Truck>()
    private var searchHistory = mutableListOf<String>()
    private val activityName = "Trucks Activity"
    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrucksBinding.inflate(layoutInflater)
        trucksRecyclerView = binding.trucksRecyclerView
        nestedScrollingView = binding.nestedScrollView3
        floatingActionButton = binding.floatingActionButton
        appBarLayout = binding.appbar
        searchBar = binding.searchBar
        searchView = binding.truckSearchView
        noTrucksMessageText = binding.noTrucksMessage
        dbHelper = DbHelper(this)
        db = dbHelper?.writableDatabase
        clearAllTextView = binding.clearAllHistoryLink

        clearAllTextView.setOnClickListener {
            clearAllHistory()
        }


        trucksRecyclerView.layoutManager = LinearLayoutManager(this)

        trucks = getTrucks() // Assuming getAllTrucks() retrieves all trucks from the database
        println("size of trucks in the beginning: " + trucks.size)

        if (trucks.isEmpty() || trucks == null){
            noTrucksMessageText.visibility = View.VISIBLE
        }else{
            noTrucksMessageText.visibility = View.GONE
        }

        truckAdapter = TruckAdapter(this,trucks)
        trucksRecyclerView.adapter = truckAdapter


        nestedScrollingView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY -> // the delay of the extension of the FAB is set for 12 items
            if (scrollY > oldScrollY + 12 && floatingActionButton.isExtended) {
                floatingActionButton.shrink()
            }

            // the delay of the extension of the FAB is set for 12 items
            if (scrollY < oldScrollY - 12 && !floatingActionButton.isExtended) {
                floatingActionButton.extend()
            }

            // if the nestedScrollView is at the first item of the list then the
            // extended floating action should be in extended state
            if (scrollY == 0) {
                floatingActionButton.extend()
            }
        })

        val searchBarHeight = searchBar.height
        val slideUpDistance = -searchBarHeight.toFloat()
        val slideDownDistance = 0f

        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val slideDistance = if (verticalOffset < 0) slideUpDistance else slideDownDistance
            searchBar.animate()
                .translationY(slideDistance)
                .setDuration(200)
                .start()
        })

        floatingActionButton.setOnClickListener {
            showAddOrEditTruckDialog("Add")

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

        // Define the color for the status bar when the search view is focused
        val searchFocusedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer,"")
        // Define the default color for the status bar
        val defaultStatusBarColor = this.window.statusBarColor

        searchView.addTransitionListener { searchView: com.google.android.material.search.SearchView?, previousState: TransitionState?, newState: TransitionState ->
            if (newState == TransitionState.SHOWING) {
                // Change the status bar color to match the SearchView's surface color
                this.window.statusBarColor = searchFocusedColor
                println("is focused")
                floatingActionButton.hide()
            }
            else if (newState == TransitionState.HIDDEN){
                // Revert the status bar color to the default color
                this.window.statusBarColor = defaultStatusBarColor
                floatingActionButton.show()
                println("is not focused")
            }
        }

        loadSearchHistoryDb()

        if(!searchHistory.isNullOrEmpty()){
            showHistoryitems("")
        }

        searchView.editText?.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView.text.toString()
                searchImplementation(query)
                searchBar.setText(query)
                searchView.hide()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }





        setContentView(binding.root)
    }

    private fun searchImplementation(query: String) {
        println("query: " + query)
        addToSearchHistory(query)
        val filteredList = trucks.filter { truck ->
            truck.truckNo!!.contains(query.replace(" ", "").trim(), ignoreCase = true)
        }
        updateRecyclerView(filteredList)

    }

    private fun addToSearchHistory(query: String) {
        // Remove the query if it already exists in the history
        searchHistory.remove(query)

        // Add the query to the beginning of the search history
        searchHistory.add(0, query)

        // Save the updated search history to the database
        addToSearchHistoryDb(query)


        showHistoryitems(query)


    }

    private fun showHistoryitems(query: String){

        // Create an instance of SearchHistoryAdapter with the click listener
        val historyAdapter = SearchHistoryAdapter(searchHistory) { clickedQuery ->
            // Handle the click event, for example, update the search view text
            searchImplementation(clickedQuery)
            searchView.hide()
            searchBar.setText(clickedQuery)
            searchView.setText(clickedQuery)
            hideKeyboard()
        }

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

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
    }

    private fun addToSearchHistoryDb(query: String) {
        dbHelper?.addTruckQueryToSearchHistory(query)
    }

    fun clearAllHistory() {
        // Clear all items from the search history
        searchHistory.clear()

        // Update the UI to reflect the changes
        updateSearchHistoryUI()

        // Also, clear the history from the database
        dbHelper?.clearTruckSearchHistory()
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

    }

    private fun loadSearchHistoryDb() {
        if (dbHelper == null) {
            dbHelper = DbHelper(this)
        }

        dbHelper?.openDatabase()
        val allSearchHistory = dbHelper?.getTruckSearchHistory() ?: emptyList()
        dbHelper?.closeDatabase()

        // Take the first 5 items from the set and convert it to a mutable list
        searchHistory = allSearchHistory.toMutableList()
    }

    fun showAddOrEditTruckDialog(action: String, truckJson: String = "") {
        val dialog = AddOrEditTruckDialog()

        val args = Bundle()
        args.putString("Action", action)
        if (truckJson != "") {
            args.putString("TruckJson", truckJson)
        }
        dialog.arguments = args

        dialog.setOnAddTruckListener(this)
        dialog.show(supportFragmentManager, "AddOrEditTruckDialog")
    }

    private fun getTrucks(): MutableList<Truck>{
        if (dbHelper == null) {
            dbHelper = DbHelper(this)
        }
        dbHelper?.openDatabase()
        val allTrucks = dbHelper?.getLocalTrucks() ?: emptyList()
        dbHelper?.closeDatabase()
        return allTrucks.toMutableList()
    }

    fun showDialog(mapToSend: MutableMap<String, MutableList<String>>) {
        val dialog = SortAndFilterTrucks()

        // Create a Bundle and add the MutableMap as an argument
        if(!keyValueMapToFilterAndSortFragment.isNullOrEmpty()){
            val args = Bundle()
            args.putSerializable("yourMapKey", mapToSend as Serializable)
            dialog.arguments = args
        }

        dialog.setOnApplyClickListener(this)
        dialog.show(supportFragmentManager, "SortAndFilterTrucks")

    }

    override fun onApplyClick(keyValueMap: Map<String, List<String>>) {
        var allTrucksToSortAndFilter = trucks
        val sortCriteria = keyValueMap["sort"] ?: mutableListOf()
        var filterMake = keyValueMap["make"] ?: mutableListOf()
        var filterOwner = keyValueMap["owner"] ?: mutableListOf()

        // Apply filtering criteria
        val filteredTrucks = allTrucksToSortAndFilter.filter { truck ->
            // Extract the first word of the truck's owner and make it lowercase
            val truckOwnerFirstWord = truck.owner?.name?.split(" ")?.firstOrNull()?.lowercase()
            // Check if the first word of the truck's owner matches any of the first words in the owner filter criteria (lowercased)
            val ownerMatch = filterOwner.isEmpty() || filterOwner.any { it.split(" ").firstOrNull()?.lowercase() == truckOwnerFirstWord }
            // Check if the truck's make matches any of the make filter criteria
            val makeMatch = filterMake.isEmpty() || filterMake.contains(truck.make)
            ownerMatch && makeMatch
        }

        // Apply sorting criteria if provided
        val sortedAndFilteredTrucks = when (sortCriteria.first()) {
            "Truck No." -> filteredTrucks.sortedBy { it.truckNo }
            "Make" -> filteredTrucks.sortedBy { it.make }
            "Owner" -> filteredTrucks.sortedBy { it.owner?.name }
            else -> filteredTrucks // No sorting criteria provided
        }

        if(sortedAndFilteredTrucks.isEmpty()){
            noTrucksMessageText.visibility = View.VISIBLE
        }else{
            noTrucksMessageText.visibility = View.GONE
        }

        val loadingDialog = createLoadingDialog()

        loadingDialog.show()

        lifecycleScope.launch {
            delay(50)
            updateRecyclerView(sortedAndFilteredTrucks)
            loadingDialog.dismiss()
            keyValueMapToFilterAndSortFragment = keyValueMap as MutableMap<String, MutableList<String>>
        }







        //Log.d(activityName, "Filtered Trucks: " + sortedAndFilteredTrucks.first().truckNo)




    }

    fun updateRecyclerView(trucks: List<Truck>) {
        if (trucks.isEmpty()){
            noTrucksMessageText.visibility = View.VISIBLE
        }else{
            noTrucksMessageText.visibility = View.GONE
        }
        truckAdapter.updateTrucks(trucks)
        trucksRecyclerView.adapter = truckAdapter
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

    override fun onAddTruck() {
        trucks = getTrucks()
        println("size of trucks after add truck: " + trucks.size)
        println("New truck Refresh")
        updateRecyclerView(trucks)

    }


}