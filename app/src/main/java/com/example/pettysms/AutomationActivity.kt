package com.example.pettysms

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityAutomationBinding
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors
import android.graphics.Color
import android.app.AlertDialog
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat

class AutomationActivity : AppCompatActivity(), AddAutomationRuleFragment.OnAddAutomationRuleListener {
    private lateinit var binding: ActivityAutomationBinding
    private var automationAdapter: AutomationAdapter? = null
    private var dbHelper: DbHelper? = null
    private var allAutomationRules: MutableList<AutomationRule> = mutableListOf()
    private var filteredAutomationRules: MutableList<AutomationRule> = mutableListOf()
    private var fab: ExtendedFloatingActionButton? = null
    private var searchBar: SearchBar? = null
    private var searchView: com.google.android.material.search.SearchView? = null
    private var currentQuery: String = ""
    private var searchJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutomationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Automation Rules"
        
        // Initialize database helper
        dbHelper = DbHelper(this)

        // Make sure the AppBarLayout doesn't change appearance on scroll
        binding.appBarLayout.setExpanded(true, false)
        binding.appBarLayout.setLiftOnScroll(false)
        
        // Set up search bar and search view
        setupSearch()
        
        // Set up RecyclerView
        setupRecyclerView()
        
        // Set up FAB
        setupFab()
        
        // Load automation rules
        loadAutomationRules()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We're using Material SearchBar/SearchView instead
        // No need to inflate menu or set up SearchView here
        return true
    }
    
    private fun setupSearch() {
        searchBar = binding.searchBar
        searchView = binding.searchView

        searchView?.editText?.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView?.text.toString()
                filterAutomationRules(query)
                searchBar?.setText(query)
                searchView?.hide()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        
        // Set up search view text listener
        /*searchView?.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce to avoid too many searches while typing
                    s?.toString()?.let {
                        currentQuery = it
                        filterAutomationRules(it)
                    }
                }
            }
        })*/

        binding.searchView.addTransitionListener { searchView, previousState, newState ->
            when (newState) {
                com.google.android.material.search.SearchView.TransitionState.SHOWING -> {
                    // Hide FAB when search is showing
                    fab?.hide()
                }
                com.google.android.material.search.SearchView.TransitionState.HIDDEN -> {
                    // Show and extend FAB when search is hidden
                    fab?.show()
                    fab?.extend()
                }
                com.google.android.material.search.SearchView.TransitionState.HIDING -> {
                    binding.searchView.editText.text = null
                    fab?.show()
                    fab?.extend()
                }
                else -> {}
            }
        }

        val searchFocusedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer,"")
        val defaultStatusBarColor = this.window.statusBarColor

        binding.searchView.addTransitionListener { searchView: com.google.android.material.search.SearchView?, previousState: com.google.android.material.search.SearchView.TransitionState?, newState: com.google.android.material.search.SearchView.TransitionState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.SHOWING) {
                // Change the status bar color to match the SearchView's surface color
                this.window.statusBarColor = searchFocusedColor
                println("is focused")
            }
            else if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN){
                // Revert the status bar color to the default color
                this.window.statusBarColor = defaultStatusBarColor
                println("is not focused")
            }
        }
        
        // Set up search bar menu click listener
        searchBar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_bar_options -> {
                    // Show filter dialog or handle other options
                    // You can implement a filter dialog similar to PettyCashSortFilterDialogFragment
                    true
                }
                else -> false
            }
        }
    }
    
    private fun filterAutomationRules(query: String) {
        Log.d("AutomationActivity", "Filtering rules with query: '$query'")
        Log.d("AutomationActivity", "All rules count before filtering: ${allAutomationRules.size}")
        
        // Clear the filtered list
        filteredAutomationRules.clear()
        
        // If query is empty, add all rules without filtering
        if (query.isEmpty()) {
            Log.d("AutomationActivity", "Empty query - adding all rules without filtering")
            filteredAutomationRules.addAll(allAutomationRules)
        } else {
            // Filter rules based on query
            for (rule in allAutomationRules) {
                if (matchesQuery(rule, query)) {
                    filteredAutomationRules.add(rule)
                    Log.d("AutomationActivity", "Added rule '${rule.name}' to filtered list (matched query)")
                }
            }
        }
        
        Log.d("AutomationActivity", "Filtered rules count: ${filteredAutomationRules.size}")
        
        // Update adapter with filtered rules
        automationAdapter?.updateList(filteredAutomationRules)
        
        // Update empty state
        updateEmptyState(filteredAutomationRules.isEmpty())
    }
    
    private fun setupRecyclerView() {
        binding.automationRecyclerView.layoutManager = LinearLayoutManager(this)
        automationAdapter = AutomationAdapter(this, filteredAutomationRules, object : AutomationAdapter.OnAutomationRuleClickListener {
            override fun onEditClick(rule: AutomationRule) {
                showEditAutomationRuleDialog(rule)
            }

            override fun onDeleteConfirmation(rule: AutomationRule) {
                showDeleteConfirmationDialog(rule)
            }
        })
        binding.automationRecyclerView.adapter = automationAdapter
        
        // Add scroll listener for pagination and FAB behavior
        binding.automationRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Handle FAB visibility
                if (dy > 0 && fab?.isExtended == true) {
                    fab?.shrink()
                } else if (dy < 0 && fab?.isExtended == false) {
                    fab?.extend()
                }
            }
        })
    }
    
    private fun setupFab() {
        fab = binding.addRuleFab
        
        fab?.setOnClickListener {
            // Disable the button to prevent multiple clicks
            fab?.isClickable = false
            
            // Show AddAutomationRuleFragment
            val addAutomationRuleFragment = AddAutomationRuleFragment.newInstance("Add", null)
            addAutomationRuleFragment.show(supportFragmentManager, "AddAutomationRuleFragment")
            
            // Re-enable the button after a delay
            lifecycleScope.launch {
                delay(1000)
                fab?.isClickable = true
            }
        }
    }
    
    private fun loadAutomationRules() {
        Log.d("AutomationActivity", "Starting to load automation rules")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ensure we have a valid dbHelper
                if (dbHelper == null) {
                    dbHelper = DbHelper(this@AutomationActivity)
                    Log.d("AutomationActivity", "Created new DbHelper instance")
                }
                
                val rules = dbHelper?.getAllAutomationRules() ?: emptyList()
                Log.d("AutomationActivity", "Fetched ${rules.size} rules from database")
                
                // Log each rule for debugging
                rules.forEachIndexed { index, rule ->
                    Log.d("AutomationActivity", "Rule $index: ${rule.name} (ID: ${rule.id})")
                }
                
                withContext(Dispatchers.Main) {
                    // Create new lists to avoid reference issues
                    allAutomationRules = ArrayList(rules)
                    filteredAutomationRules = ArrayList(rules)
                    
                    // If no rules were found, show empty state
                    if (rules.isEmpty()) {
                        Log.d("AutomationActivity", "No rules found, showing empty state")
                        updateEmptyState(true)
                        return@withContext
                    }
                    
                    // Update adapter with the new list
                    Log.d("AutomationActivity", "Updating adapter with ${rules.size} rules")
                    automationAdapter = AutomationAdapter(this@AutomationActivity, filteredAutomationRules, object : AutomationAdapter.OnAutomationRuleClickListener {
                        override fun onEditClick(rule: AutomationRule) {
                            showEditAutomationRuleDialog(rule)
                        }

                        override fun onDeleteConfirmation(rule: AutomationRule) {
                            showDeleteConfirmationDialog(rule)
                        }
                    })
                    binding.automationRecyclerView.adapter = automationAdapter
                    
                    // Update empty state - explicitly pass false since we know we have rules
                    updateEmptyState(false)
                    
                    Log.d("AutomationActivity", "Loaded ${rules.size} automation rules")
                    
                    // Check RecyclerView state
                    val recyclerView = binding.automationRecyclerView
                    Log.d("AutomationActivity", "RecyclerView visibility: ${if (recyclerView.visibility == View.VISIBLE) "VISIBLE" else "GONE"}")
                    Log.d("AutomationActivity", "Empty state visibility: ${if (binding.emptyStateLayout.visibility == View.VISIBLE) "VISIBLE" else "GONE"}")
                    Log.d("AutomationActivity", "Adapter item count: ${automationAdapter?.itemCount}")
                }
            } catch (e: Exception) {
                Log.e("AutomationActivity", "Error loading automation rules: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AutomationActivity,
                        "Error loading automation rules: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        Log.d("AutomationActivity", "Updating empty state: isEmpty=$isEmpty")
        Log.d("AutomationActivity", "Current counts - allRules: ${allAutomationRules.size}, filteredRules: ${filteredAutomationRules.size}")
        
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.automationRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.automationRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun showEditAutomationRuleDialog(rule: AutomationRule) {
        val fragment = AddAutomationRuleFragment.newInstance("Edit", rule)
        fragment.show(supportFragmentManager, "EditAutomationRuleFragment")
    }
    
    private fun deleteAutomationRule(rule: AutomationRule) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ruleId = rule.id
                if (ruleId != null) {
                    val success = dbHelper?.deleteAutomationRule(ruleId) ?: false
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            // Remove from both lists
                            allAutomationRules.remove(rule)
                            filteredAutomationRules.remove(rule)
                            
                            // Update adapter
                            automationAdapter?.removeItem(ruleId)
                            
                            // Log the current state
                            Log.d("AutomationActivity", "After deletion - allRules: ${allAutomationRules.size}, filteredRules: ${filteredAutomationRules.size}")
                            
                            // Only update empty state if both lists are actually empty
                            updateEmptyState(allAutomationRules.isEmpty())
                            
                            // If we have rules but filtered list is empty, reset the filter
                            if (allAutomationRules.isNotEmpty() && filteredAutomationRules.isEmpty()) {
                                currentQuery = ""
                                filterAutomationRules(currentQuery)
                            }
                            
                            Toast.makeText(
                                this@AutomationActivity,
                                "Automation rule deleted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@AutomationActivity,
                                "Failed to delete automation rule",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AutomationActivity", "Error deleting automation rule: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AutomationActivity,
                        "Error deleting automation rule: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun showDeleteConfirmationDialog(rule: AutomationRule) {
        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Delete Automation Rule")
            .setMessage("Are you sure you want to delete \"${rule.name}\"? This action cannot be undone.")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { dialog, _ ->
                deleteAutomationRule(rule)
                dialog.dismiss()
            }
            .setIcon(R.drawable.baseline_warning_amber_white_24dp)
            .show()
            .apply {
                // Style the buttons
                getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.red_color))
                }
                getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(MaterialColors.getColor(this@AutomationActivity, com.google.android.material.R.attr.colorPrimary, Color.GRAY))
                }
            }
    }
    
    override fun onAddAutomationRule(rule: AutomationRule) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (rule.id == null) {
                    // Add new rule
                    val newId = dbHelper?.addAutomationRule(rule)
                    if (newId != null && newId > 0) {
                        rule.id = newId.toInt()
                        
                        withContext(Dispatchers.Main) {
                            allAutomationRules.add(0, rule)
                            
                            if (currentQuery.isEmpty() || matchesQuery(rule, currentQuery)) {
                                filteredAutomationRules.add(0, rule)
                                automationAdapter?.addItemToTop(rule)
                            }
                            
                            updateEmptyState(allAutomationRules.isEmpty())
                            Toast.makeText(this@AutomationActivity, "Rule added successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Update existing rule
                    val success = dbHelper?.updateAutomationRule(rule) ?: false
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Log.d("AutomationActivity", "Rule updated successfully in DB: ${rule.id} - ${rule.name}")
                            
                            // Update in allAutomationRules list
                            val allPosition = allAutomationRules.indexOfFirst { it.id == rule.id }
                            if (allPosition != -1) {
                                allAutomationRules[allPosition] = rule
                            }
                            
                            // Update in filteredAutomationRules list
                            val filteredPosition = filteredAutomationRules.indexOfFirst { it.id == rule.id }
                            if (filteredPosition != -1) {
                                filteredAutomationRules[filteredPosition] = rule
                                
                                // Reload the entire adapter to ensure consistency
                                automationAdapter?.updateList(filteredAutomationRules)
                            } else if (currentQuery.isEmpty() || matchesQuery(rule, currentQuery)) {
                                // If the rule wasn't in the filtered list but now matches the query, add it
                                filteredAutomationRules.add(rule)
                                automationAdapter?.updateList(filteredAutomationRules)
                            }
                            
                            Toast.makeText(this@AutomationActivity, "Rule updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AutomationActivity, "Failed to update rule", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AutomationActivity", "Error saving rule: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AutomationActivity, "Error saving rule: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun matchesQuery(rule: AutomationRule, query: String): Boolean {
        // If query is empty, always match
        if (query.isEmpty()) {
            return true
        }
        
        val lowerQuery = query.lowercase()
        
        // Check if any field matches the query
        return (rule.name?.lowercase()?.contains(lowerQuery) == true) ||
               (rule.transactorName?.lowercase()?.contains(lowerQuery) == true) ||
               (rule.accountName?.lowercase()?.contains(lowerQuery) == true) ||
               (rule.ownerName?.lowercase()?.contains(lowerQuery) == true) ||
               (rule.truckName?.lowercase()?.contains(lowerQuery) == true) ||
               (rule.descriptionPattern?.lowercase()?.contains(lowerQuery) == true)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}