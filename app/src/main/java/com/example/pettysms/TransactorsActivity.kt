package com.example.pettysms

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityTransactorsBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.l4digital.fastscroll.FastScrollRecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class TransactorsActivity : AppCompatActivity(), AddOrEditTransactorDialog.OnAddTransactorListener {
    private lateinit var binding: ActivityTransactorsBinding
    private lateinit var adapter: TransactorsAdapter
    private lateinit var recyclerView: FastScrollRecyclerView
    private lateinit var searchRecyclerview: RecyclerView
    private lateinit var fab: ExtendedFloatingActionButton
    private lateinit var noTransactorsMessageText: TextView
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var loadingText: TextView
    private lateinit var loadingDialog: AlertDialog
    private lateinit var mpesaTransactions: MutableList<MpesaTransaction>

    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var transactors: MutableList<Transactor>? = null
    private var notCheckedTransactions:  MutableList<MpesaTransaction>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactorsBinding.inflate(layoutInflater)
        recyclerView = binding.fastScroller
        fab = binding.floatingActionButton
        noTransactorsMessageText = binding.noTransactorsMessage
        searchBar = binding.searchBarTransactors
        searchView = binding.transactorsSearchView
        searchRecyclerview = binding.searchRecyclerView

        dbHelper = DbHelper(this)
        db = dbHelper?.writableDatabase

        notCheckedTransactions = dbHelper?.getTransactorNotCheckedTransactions()
        var notCheckedTransactors = Transactor.getTransactorsFromTransactions(notCheckedTransactions!!)

        syncNewTransactors(notCheckedTransactors, notCheckedTransactions!!)



        searchView.setupWithSearchBar(searchBar)

        searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed before text changes
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = searchView.text.toString()
                println("query: $query")
                searchImplementation(query)
            }

            override fun afterTextChanged(s: Editable?) {
                // No action needed after text changes
            }
        })


        //var transactors = generateRandomTransactors()




        val searchFocusedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer,"")
        // Define the default color for the status bar
        val defaultStatusBarColor = this.window.statusBarColor

        searchView.addTransitionListener { searchView: com.google.android.material.search.SearchView?, previousState: SearchView.TransitionState?, newState: SearchView.TransitionState ->
            if (newState == SearchView.TransitionState.SHOWING) {
                // Change the status bar color to match the SearchView's surface color
                this.window.statusBarColor = searchFocusedColor
                println("is focused")
                fab.hide()
            }
            else if (newState == SearchView.TransitionState.HIDDEN){
                // Revert the status bar color to the default color
                this.window.statusBarColor = defaultStatusBarColor
                fab.show()
                println("is not focused")
            }
        }

        fab.setOnClickListener {
            showAddOrEditTransactorDialog("Add")
        }




        /*searchView.editText?.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                val query = searchView.text.toString()
                println("query: " + query)
                searchImplementation(query)
                searchBar.setText(query)
                searchView.hide()

            return@setOnEditorActionListener false
        }*/



        setContentView(binding.root)
    }

    private fun searchImplementation(query: String) {
        println("query: $query")

        // Filter the transactors that match the search query and are not deleted (isDeleted == 0)
        var filteredAndSortedList = transactors?.filter { transactor ->
            transactor.name!!.contains(query.replace(" ", "").trim(), ignoreCase = true) &&
                    transactor.isDeleted == false // Ensure only non-deleted transactors are included
        }?.sortedByDescending { transactor ->
            transactor.interactions
        }

        // Limit results to top 15 if the query is empty
        if (query.isEmpty()) {
            filteredAndSortedList = filteredAndSortedList?.take(15)
        }

        println("filteredList: ${filteredAndSortedList?.size}")

        // Update the search RecyclerView with filtered results
        updateSearchRecyclerView(filteredAndSortedList)
    }

    private fun updateSearchRecyclerView(filteredList: List<Transactor>?) {
        searchRecyclerview.layoutManager = LinearLayoutManager(this)
        searchRecyclerview.adapter = filteredList?.let { dbHelper?.let { it1 ->
            SuggestedTransactorsAdapter(it, it1, supportFragmentManager, listener = this)
        } }
    }

    private fun syncNewTransactors(
        notCheckedTransactors: List<Transactor>,
        notCheckedTransactions: MutableList<MpesaTransaction>
    ) {
        val loadingDialog = createLoadingDialog()
        loadingDialog.show()
        var task = GlobalScope.launch(Dispatchers.Main) {
            addTransactorsToDb(notCheckedTransactors)
            for (transaction in notCheckedTransactions) {
                updateTransactionCheck(transaction)
            }
            loadingDialog.dismiss()
        }
        task.invokeOnCompletion {

            setUpRecyclerView()

        }
    }

    private fun setUpRecyclerView() {
        // Fetch all transactors
        transactors = dbHelper?.getAllTransactors()?.toMutableList()

        // Filter out deleted transactors (where isDeleted == 1)
        val nonDeletedTransactors = transactors?.filter { it.isDeleted == false }?.toMutableList()

        mpesaTransactions = dbHelper?.getAllMpesaTransactions()!!

        if (nonDeletedTransactors!!.isEmpty()) {
            noTransactorsMessageText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noTransactorsMessageText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        // Group transactors by their first letter and prepare for the adapter
        val groupedTransactors = groupTransactors(nonDeletedTransactors)
        val sortedTransactors = nonDeletedTransactors.sortedBy { it.name }

        // Set the adapter with the non-deleted transactors
        adapter = TransactorsAdapter(groupedTransactors, dbHelper!!, supportFragmentManager, listener = this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Add sticky headers
        recyclerView.addItemDecoration(StickyHeaderItemDecoration(adapter))

        // Handle FAB visibility based on scroll
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 12 && fab.isExtended) {
                    fab.shrink()
                } else if (dy < -12 && !fab.isExtended) {
                    fab.extend()
                }
                if (!recyclerView.canScrollVertically(-1)) {
                    fab.extend()
                }
            }
        })
    }

    private fun getIndexList(list: List<Any>): CharArray {
        return list.filterIsInstance<Transactor>().map { it.name?.firstOrNull()?.toUpperCase() ?: '#' }.distinct().toCharArray()
    }

    fun groupTransactors(transactors: List<Transactor>): List<Any> {
        val groupedTransactors = mutableListOf<Any>()
        val sortedTransactors = transactors.sortedBy { it.name }
        var currentHeader: Char? = null

        for (transactor in sortedTransactors) {
            val firstChar = transactor.name?.firstOrNull()?.toUpperCase() ?: '#'
            if (currentHeader != firstChar) {
                currentHeader = firstChar
                groupedTransactors.add(currentHeader)
            }
            groupedTransactors.add(transactor)
        }

        return groupedTransactors
    }

    fun generateRandomTransactors(): List<Transactor> {
        val transactors = mutableListOf<Transactor>()

        val names = listOf(
            "Alice", "Bob", "Charlie", "David", "Emma", "Frank", "Grace", "Henry", "Ivy", "Jack",
            "Katherine", "Liam", "Mia", "Noah", "Olivia", "Peter", "Quinn", "Rachel", "Samuel", "Taylor",
            "Ursula", "Victor", "Wendy", "Xavier", "Yvonne", "Zane", "Amelia", "Benjamin", "Catherine", "Daniel"
        )

        val phoneNumbers = listOf(
            "1234567890", "2345678901", "3456789012", "4567890123", "5678901234",
            "6789012345", "7890123456", "8901234567", "9012345678", "0123456789"
        )

        val transactorTypes = listOf("Type1", "Type2", "Type3", "Type4", "Type5")

        repeat(30) {
            val name = names.random()
            val phoneNumber = phoneNumbers.random()
            val idCard = (1000..9999).random()
            val transactorType = transactorTypes.random()
            transactors.add(Transactor(null, name, phoneNumber, idCard, transactorType = transactorType))
        }

        return transactors
    }

    private fun createLoadingDialog(): AlertDialog {
        // Create a custom view for the loading dialog
        val customView = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)
        loadingText.text = "Syncing... Please Wait"

        // Show loading dialog
        loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(customView)
            .setCancelable(false)
            .create()

        return loadingDialog
    }

    private fun addTransactorsToDb(transactor: List<Transactor>) {
        if (db?.isOpen == true) {
            dbHelper?.insertTransactors(transactor)
        }
        else{
            dbHelper = this.applicationContext?.let { DbHelper(it) }
            db = dbHelper?.writableDatabase
            dbHelper?.insertTransactors(transactor)
        }

    }

    private fun updateTransactionCheck(mpesaTransaction: MpesaTransaction){
        val dbHelper = DbHelper(this)
        mpesaTransaction.id?.let { dbHelper.transactorCheckUpdateTransaction(it) }
    }

    fun showAddOrEditTransactorDialog(action: String, ownerJson: String = "") {
        val dialog = AddOrEditTransactorDialog()

        val args = Bundle()
        args.putString("Action", action)
        if (ownerJson != "") {
            args.putString("TransactorJson", ownerJson)
        }
        dialog.arguments = args

        dialog.setOnAddTransactorListener(this)
        dialog.show(supportFragmentManager, "AddOrEditTransactorDialog")

    }

    override fun onAddTransactor() {
        println("Back to Activity")
        setUpRecyclerView()
        searchView.hide()
    }
}