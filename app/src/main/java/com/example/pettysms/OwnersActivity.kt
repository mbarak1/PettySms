package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityOwnersBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView

class OwnersActivity : AppCompatActivity(), AddOrEditOwnerDialog.OnAddOwnerListener {

    private lateinit var binding: ActivityOwnersBinding
    private lateinit var ownersRecyclerView: RecyclerView
    private lateinit var nestedScrollingView: NestedScrollView
    private lateinit var floatingActionButton: ExtendedFloatingActionButton
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: com.google.android.material.search.SearchView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var noOwnersMessageText: TextView
    private lateinit var clearAllTextView: TextView
    private lateinit var ownersAdapter: OwnersAdapter



    private val activityName = "Owners Activity"
    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var owners = mutableListOf<Owner>()
    private var searchHistory = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnersBinding.inflate(layoutInflater)
        ownersRecyclerView = binding.ownersRecyclerView
        nestedScrollingView = binding.nestedScrollViewOwners
        floatingActionButton = binding.floatingActionButton
        searchBar = binding.searchBarOwners
        searchView = binding.ownersSearchView
        appBarLayout = binding.appbar
        clearAllTextView = binding.clearAllHistoryLink
        noOwnersMessageText = binding.noOwnersMessage
        dbHelper = DbHelper(this)
        db = dbHelper?.writableDatabase
        owners = getOwners()

        clearAllTextView.setOnClickListener {
            clearAllHistory()
        }

        ownersRecyclerView.layoutManager = GridLayoutManager(this, 2)


        if (owners.isEmpty()) {
            noOwnersMessageText.visibility = View.VISIBLE
        } else {
            noOwnersMessageText.visibility = View.GONE
        }

        ownersAdapter = OwnersAdapter(this, owners)
        ownersRecyclerView.adapter = ownersAdapter

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
            showAddOrEditOwnerDialog("Add")

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
                floatingActionButton.hide()
            }
            else if (newState == SearchView.TransitionState.HIDDEN){
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

    fun showAddOrEditOwnerDialog(action: String, ownerJson: String = "") {
        val dialog = AddOrEditOwnerDialog()

        val args = Bundle()
        args.putString("Action", action)
        if (ownerJson != "") {
            args.putString("OwnerJson", ownerJson)
        }
        dialog.arguments = args

        dialog.setOnAddOwnerListener(this)
        dialog.show(supportFragmentManager, "AddOrEditOwnerDialog")

    }

    private fun loadSearchHistoryDb() {
        if (dbHelper == null) {
            dbHelper = DbHelper(this)
        }

        val allSearchHistory = dbHelper?.getOwnerSearchHistory() ?: emptyList()

        // Take the first 5 items from the set and convert it to a mutable list
        searchHistory = allSearchHistory.toMutableList()
    }

    private fun getOwners(): MutableList<Owner> {
        if (dbHelper == null) {
            dbHelper = DbHelper(this)
        }
        dbHelper?.openDatabase()
        val owners = dbHelper?.getAllOwners() ?: emptyList()
        Toast.makeText(this, "owners: " + owners.size, Toast.LENGTH_SHORT).show()
        dbHelper?.closeDatabase()
        return owners.toMutableList()

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
        dbHelper?.addOwnerQueryToSearchHistory(query)
    }

    private fun searchImplementation(query: String) {
        println("query: " + query)
        addToSearchHistory(query)
        val filteredList = owners.filter { owner ->
            owner.name!!.contains(query.replace(" ", "").trim(), ignoreCase = true)
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

    fun updateRecyclerView(owners: List<Owner>) {
        if (owners.isEmpty()){
            noOwnersMessageText.visibility = View.VISIBLE
        }else{
            noOwnersMessageText.visibility = View.GONE
        }
        ownersAdapter.updateOwners(owners)
        ownersRecyclerView.adapter = ownersAdapter
    }

    private fun clearAllHistory() {
        // Clear all items from the search history
        searchHistory.clear()

        // Update the UI to reflect the changes
        updateSearchHistoryUI()

        // Also, clear the history from the database
        dbHelper?.clearOwnerSearchHistory()
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

    override fun onAddOwner() {

        owners = getOwners()
        updateRecyclerView(owners)

    }


}