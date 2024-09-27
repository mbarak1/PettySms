package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityAccountsBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountsBinding
    private lateinit var accountsRecyclerView: RecyclerView
    private lateinit var accountsSearchResultsRecyclerView: RecyclerView
    private lateinit var nestedScrollingView: NestedScrollView
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var noAccountsMessageText: TextView
    private lateinit var accountsAdapter: AccountsAdapter
    private lateinit var accountsSearchResultsAdapter: AccountsAdapter

    private lateinit var loadingDialog: AlertDialog
    private lateinit var loadingText: TextView
    private lateinit var clearAllTextView: TextView

    private val activityName = "Accounts Activity"
    private var dbHelper: DbHelper? = null
    private var accounts = mutableListOf<Account>()
    private var searchHistory = mutableListOf<String>()

    // Pagination variables
    private var currentOffset = 0
    private val pageSize = 30
    private var isLoading = false
    private var hasMoreAccounts = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountsBinding.inflate(layoutInflater)
        nestedScrollingView = binding.nestedScrollViewAccounts
        searchBar = binding.searchBarAccounts
        appBarLayout = binding.appbar
        noAccountsMessageText = binding.noAccountsMessage
        dbHelper = DbHelper(this)
        searchView = binding.accountsSearchView
        clearAllTextView = binding.clearAllHistoryLink
        accountsSearchResultsRecyclerView = binding.accountsSearchResultsRecyclerView

        // Initialize the RecyclerView
        accountsRecyclerView = binding.accountsRecyclerView
        accountsRecyclerView.setHasFixedSize(true)
        accountsRecyclerView.layoutManager = LinearLayoutManager(this)
        accountsSearchResultsRecyclerView.layoutManager = LinearLayoutManager(this)

        clearAllTextView.setOnClickListener {
            clearAllHistory()
        }

        loadSearchHistoryDb()

        // Load initial accounts
        loadAccounts()

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

        // Define the color for the status bar when the search view is focused
        val searchFocusedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer, "")
        // Define the default color for the status bar
        val defaultStatusBarColor = this.window.statusBarColor

        searchView.addTransitionListener { searchView: com.google.android.material.search.SearchView?, previousState: SearchView.TransitionState?, newState: SearchView.TransitionState ->
            if (newState == SearchView.TransitionState.SHOWING) {
                // Change the status bar color to match the SearchView's surface color
                this.window.statusBarColor = searchFocusedColor
            } else if (newState == SearchView.TransitionState.HIDDEN) {
                // Revert the status bar color to the default color
                this.window.statusBarColor = defaultStatusBarColor
            }
        }

        if (!searchHistory.isNullOrEmpty()) {
            showHistoryitems("")
        }

        searchView.editText?.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView.text.toString()
                searchImplementation(query)
                searchBar.setText(query)
                searchView.hide()
                hideKeyboard() // Hide keyboard after search
                deselectAllItems() // Deselect all items after search
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        setContentView(binding.root)
    }

    private fun loadSearchHistoryDb() {
        if (dbHelper == null) {
            dbHelper = DbHelper(this)
        }

        var allSearchHistory = emptyList<String>()

        try {
            allSearchHistory = dbHelper?.getAccountSearchHistory()!!
        } catch (Exception: Exception) {
            Log.d(activityName, "No search history found.")
        }

        searchHistory = allSearchHistory.toMutableList()
    }

    private fun showHistoryitems(query: String) {
        val historyAdapter = SearchHistoryAdapter(searchHistory) { clickedQuery ->
            searchImplementation(clickedQuery)
            searchView.hide()
            searchBar.setText(clickedQuery)
            searchView.setText(clickedQuery)
            hideKeyboard()
        }

        binding.searchHistoryLabel.visibility = View.VISIBLE
        binding.historyLayout.visibility = View.VISIBLE

        binding.searchHistoryRecycler.adapter = null

        binding.searchHistoryRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
            visibility = View.VISIBLE
        }
    }

    private fun searchImplementation(query: String) {
        addToSearchHistory(query)
        if (query.isEmpty()) {
            accountsSearchResultsRecyclerView.visibility = View.GONE
            accountsRecyclerView.visibility = View.VISIBLE
        } else {

            val filteredList = dbHelper?.getAccountsByName(query)
            if (filteredList != null) {
                updateRecyclerView(filteredList)
            }
        }
        hideKeyboard() // Hide keyboard after search
        searchView.clearFocus() // Clear focus from SearchView
        accountsRecyclerView.clearFocus() // Clear focus from RecyclerView
    }

    fun updateRecyclerView(filteredList: List<Account>) {
        if (filteredList.isEmpty()) {
            noAccountsMessageText.visibility = View.VISIBLE
        } else {
            noAccountsMessageText.visibility = View.GONE
        }

        accountsSearchResultsRecyclerView.visibility = View.VISIBLE
        accountsRecyclerView.visibility = View.GONE

        accountsSearchResultsAdapter = AccountsAdapter(filteredList.toMutableList())
        accountsSearchResultsRecyclerView.adapter = accountsSearchResultsAdapter
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
    }

    private fun addToSearchHistory(query: String) {
        searchHistory.remove(query)
        searchHistory.add(0, query)
        addToSearchHistoryDb(query)
        showHistoryitems(query)
    }

    private fun addToSearchHistoryDb(query: String) {
        dbHelper?.addAccountQueryToSearchHistory(query)
    }

    private fun loadAccounts() {
        accountsRecyclerView = binding.accountsRecyclerView

        accountsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (layoutManager.findLastCompletelyVisibleItemPosition() == accounts.size - 1) {
                    loadAccounts()
                }
            }
        })

        if (isLoading || !hasMoreAccounts) return

        isLoading = true
        loadingDialog = createLoadingDialog()
        loadingDialog.show()

        lifecycleScope.launch {
            val accountsList = withContext(Dispatchers.IO) {
                getAccounts(currentOffset, pageSize)
            }

            if (accountsList.isEmpty()) {
                hasMoreAccounts = false
            } else {
                currentOffset += accountsList.size
                accounts.addAll(accountsList)
                if (!::accountsAdapter.isInitialized) {
                    accountsAdapter = AccountsAdapter(accounts)
                    accountsRecyclerView.adapter = accountsAdapter
                } else {
                    accountsAdapter.notifyItemRangeInserted(accounts.size - accountsList.size, accountsList.size)
                }
            }

            noAccountsMessageText.visibility = if (accounts.isEmpty()) {
                Log.d(activityName, "No accounts found.")
                View.VISIBLE
            } else {
                Log.d(activityName, "${accounts.size} accounts loaded.")
                View.GONE
            }

            loadingDialog.dismiss()
            isLoading = false
        }
    }

    private fun getAccounts(offset: Int, limit: Int): MutableList<Account> {
        if (dbHelper == null) {
            dbHelper = DbHelper(this)
        }
        return dbHelper?.getAllAccountsWithPagination(offset, limit)?.toMutableList() ?: mutableListOf()
    }

    private fun createLoadingDialog(): AlertDialog {
        val customView = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)
        loadingText.text = "Syncing... Please Wait"

        return MaterialAlertDialogBuilder(this)
            .setView(customView)
            .setCancelable(false)
            .create()
    }

    private fun deselectAllItems() {
        accountsRecyclerView.clearFocus()
        accountsAdapter.notifyDataSetChanged() // Re-render the items without selection
    }

    private fun clearAllHistory() {
        searchHistory.clear()
        updateSearchHistoryUI()
        dbHelper?.clearAccountSearchHistory()
    }

    private fun updateSearchHistoryUI() {
        binding.searchHistoryRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = SearchHistoryAdapter(searchHistory) { query ->
                searchView.setText(query)
            }
            visibility = View.VISIBLE
        }

        binding.searchHistoryLabel.visibility = if (searchHistory.isNotEmpty()) View.VISIBLE else View.GONE
        binding.historyLayout.visibility = if (searchHistory.isNotEmpty()) View.VISIBLE else View.GONE
    }
}
