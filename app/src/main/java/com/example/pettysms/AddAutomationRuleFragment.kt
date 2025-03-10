package com.example.pettysms

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.pettysms.databinding.FragmentAddAutomationRuleBinding
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat

class AddAutomationRuleFragment : DialogFragment() {
    private var _binding: FragmentAddAutomationRuleBinding? = null
    private val binding get() = _binding!!
    
    private var dbHelper: DbHelper? = null
    private var action: String = "Add"
    private var automationRule: AutomationRule? = null
    private var onAddAutomationRuleListener: OnAddAutomationRuleListener? = null
    
    // Lists for dropdowns
    private var transactors: List<Transactor> = emptyList()
    private var accounts: List<Account> = emptyList()
    private var owners: List<Owner> = emptyList()
    private var trucks: List<Truck> = emptyList()
    
    // Selected IDs
    private var selectedTransactorId: Int? = null
    private var selectedAccountId: Int? = null
    private var selectedOwnerId: Int? = null
    private var selectedTruckId: Int? = null
    
    // Selected names
    private var selectedAccountName: String? = null
    private var selectedTruckName: String? = null
    
    // Add this data class at the top of the file
    private data class TruckItem(val id: Int, val name: String)
    // Add these data classes at the top of the file
    private data class AccountItem(val id: Int, val name: String)

    interface OnAddAutomationRuleListener {
        fun onAddAutomationRule(rule: AutomationRule)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.PrefsTheme)
        
        arguments?.let {
            action = it.getString(ARG_ACTION, "Add")
            automationRule = it.getSerializable(ARG_AUTOMATION_RULE) as? AutomationRule
        }
        
        dbHelper = DbHelper(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        dialog.window?.attributes?.windowAnimations = R.style.FullscreenDialogAnimationAddOrEdit
        return dialog
    }
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onAddAutomationRuleListener = context as OnAddAutomationRuleListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnAddAutomationRuleListener")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAutomationRuleBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up toolbar
        binding.toolbar.title = if (action == "Add") "Add Automation Rule" else "Edit Automation Rule"
        binding.toolbar.setNavigationIcon(R.drawable.baseline_close_24)
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        
        // Make sure the AppBarLayout doesn't change appearance on scroll
        binding.appBarLayout2.setExpanded(true, false)
        binding.appBarLayout2.setLiftOnScroll(false)
        
        // Load data for dropdowns
        loadDropdownData()
        
        // Set up form validation
        setupFormValidation()
        
        // Set up save button
        binding.saveButton.setOnClickListener {
            if (validateForm()) {
                saveAutomationRule()
            }
        }
        
        // Populate form if editing
        if (action == "Edit" && automationRule != null) {
            populateForm()
        }
    }
    
    private fun loadDropdownData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Load all data
                transactors = dbHelper?.getAllTransactors() ?: emptyList()
                accounts = dbHelper?.getAllAccounts() ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    // Set up transactor dropdown
                    val transactorAutoComplete = binding.transactorLayout.editText as? AutoCompleteTextView
                    transactorAutoComplete?.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    
                    // Format transactor names: capitalize and remove double spaces
                    val transactorNames = transactors.map { transactor -> 
                        transactor.name?.let { name ->
                            // Remove double spaces
                            val singleSpaceName = name.replace("\\s+".toRegex(), " ").trim()
                            
                            // Capitalize each word
                            singleSpaceName.split(" ").joinToString(" ") { word ->
                                word.lowercase().replaceFirstChar { 
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                                }
                            }
                        } ?: ""
                    }
                    
                    // Create a map of formatted names to original positions to handle selection correctly
                    val formattedNameToPositionMap = transactorNames.mapIndexed { index, name -> name to index }.toMap()
                    
                    val transactorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, transactorNames)
                    transactorAutoComplete?.setAdapter(transactorAdapter)
                    
                    // Add item click listener for transactor
                    transactorAutoComplete?.setOnItemClickListener { _, _, position, _ ->
                        // Get the selected formatted name
                        val selectedFormattedName = transactorAdapter.getItem(position) as String
                        
                        // Find the original position in the transactors list
                        val originalPosition = formattedNameToPositionMap[selectedFormattedName] ?: position
                        
                        // Set the selected transactor ID
                        selectedTransactorId = transactors[originalPosition].id
                        binding.transactorLayout.error = null
                        
                        Log.d("AddAutomationRule", "Selected transactor: ${transactors[originalPosition].name} (ID: $selectedTransactorId)")
                    }
                    
                    // Set up owner dropdown with custom adapter
                    setupOwnerDropdown()
                    
                    // Initially disable account and truck fields
                    binding.accountLayout.isEnabled = false
                    binding.truckLayout.isEnabled = false
                    
                    // If editing, populate the form after adapters are set
                    if (action == "Edit" && automationRule != null) {
                        populateForm()
                    }
                }
            } catch (e: Exception) {
                Log.e("AddAutomationRule", "Error loading dropdown data: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupOwnerDropdown() {
        val ownerAutoComplete = binding.ownerLayout.editText as? AutoCompleteTextView ?: return
        
        // Load owners from database
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                owners = dbHelper?.getAllOwners() ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    // Set input type for search
                    ownerAutoComplete.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    
                    // Create custom adapter for owners with logos
                    val ownerAdapter = object : ArrayAdapter<Owner>(
                        requireContext(),
                        R.layout.item_owner_dropdown,
                        R.id.ownerName,
                        owners
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            return createOwnerView(position, convertView, parent)
                        }
                        
                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            return createOwnerView(position, convertView, parent)
                        }
                        
                        private fun createOwnerView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = convertView ?: LayoutInflater.from(context)
                                .inflate(R.layout.item_owner_dropdown, parent, false)
                            
                            val owner = getItem(position) ?: return view
                            
                            val ownerNameTextView = view.findViewById<TextView>(R.id.ownerName)
                            val ownerLogoImageView = view.findViewById<ImageView>(R.id.ownerLogo)
                            
                            ownerNameTextView.text = owner.name
                            
                            // Set owner logo if available
                            if (!owner.logoPath.isNullOrEmpty()) {
                                try {
                                    val imageBytes = android.util.Base64.decode(owner.logoPath, android.util.Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    ownerLogoImageView.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    Log.e("OwnerAdapter", "Error decoding image: ${e.message}")
                                    ownerLogoImageView.setImageResource(R.mipmap.ic_p_logo_foreground)
                                }
                            } else {
                                ownerLogoImageView.setImageResource(R.mipmap.ic_p_logo_foreground)
                            }
                            
                            return view
                        }
                    }
                    
                    ownerAutoComplete.setAdapter(ownerAdapter)
                    
                    // Set item click listener for owner selection
                    ownerAutoComplete.setOnItemClickListener { _, _, position, _ ->
                        val selectedOwner = owners[position]
                        selectedOwnerId = selectedOwner.id
                        binding.ownerLayout.error = null
                        ownerAutoComplete.setText(selectedOwner.name, false)
                        
                        // Enable account and truck fields
                        binding.accountLayout.isEnabled = true
                        binding.truckLayout.isEnabled = true
                        
                        // Clear previous selections
                        selectedAccountId = null
                        selectedTruckId = null
                        (binding.accountLayout.editText as? AutoCompleteTextView)?.setText("", false)
                        (binding.truckLayout.editText as? AutoCompleteTextView)?.setText("", false)
                        
                        // Update account and truck dropdowns for this owner
                        updateAccountsForOwner(selectedOwner)
                        updateTrucksForOwner(selectedOwner)
                        
                        Log.d("AddAutomationRule", "Selected owner: ${selectedOwner.name} (ID: $selectedOwnerId)")
                    }
                    
                    // If editing, populate the form after adapters are set
                    if (action == "Edit" && automationRule != null) {
                        populateForm()
                    }
                }
            } catch (e: Exception) {
                Log.e("AddAutomationRule", "Error loading owners: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error loading owners: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateAccountsForOwner(owner: Owner) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                accounts = dbHelper?.getAccountsByOwner(owner) ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    val accountAutoComplete = binding.accountLayout.editText as? AutoCompleteTextView
                    accountAutoComplete?.let { autoComplete ->
                        // Enable type-to-search
                        autoComplete.inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                            android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

                        // Create map of account names to accounts for accurate selection
                        val accountMap = accounts.associateBy { it.name?.trim() ?: "" }
                        
                        val accountAdapter = object : ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            accounts.map { it.name?.trim() ?: "" }
                        ) {
                            override fun getFilter(): android.widget.Filter {
                                return object : android.widget.Filter() {
                                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                                        val filterResults = FilterResults()
                                        if (constraint == null || constraint.isEmpty()) {
                                            filterResults.values = accounts.map { it.name?.trim() ?: "" }
                                            filterResults.count = accounts.size
                                        } else {
                                            val filterPattern = constraint.toString().lowercase().trim()
                                            val filteredList = accounts.filter {
                                                it.name?.lowercase()?.contains(filterPattern) == true
                                            }.map { it.name?.trim() ?: "" }
                                            filterResults.values = filteredList
                                            filterResults.count = filteredList.size
                                        }
                                        return filterResults
                                    }

                                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                                        @Suppress("UNCHECKED_CAST")
                                        val filteredList = results?.values as? List<String> ?: emptyList()
                                        clear()
                                        addAll(filteredList)
                                        notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                        
                        autoComplete.setAdapter(accountAdapter)
                        
                        // Add item click listener
                        autoComplete.setOnItemClickListener { _, view, _, _ ->
                            val selectedText = (view as TextView).text.toString().trim()
                            accountMap[selectedText]?.let { selectedAccount ->
                                selectedAccountId = selectedAccount.id
                                selectedAccountName = selectedAccount.name
                                binding.accountLayout.error = null
                                Log.d("AddAutomationRule", "Selected account: ${selectedAccount.name} (ID: ${selectedAccount.id})")
                            }
                        }
                    }
                    binding.accountLayout.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("AddAutomationRule", "Error loading accounts: ${e.message}")
            }
        }
    }

    private fun updateTrucksForOwner(owner: Owner) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                trucks = dbHelper?.getLocalTrucksByOwner(owner) ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    val truckAutoComplete = binding.truckLayout.editText as? AutoCompleteTextView
                    truckAutoComplete?.let { autoComplete ->
                        // Enable type-to-search
                        autoComplete.inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                            android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

                        // Create list with "All Trucks" option and actual trucks
                        val truckItems = mutableListOf<TruckItem>()
                        if (trucks.size > 1) {
                            truckItems.add(TruckItem(-1, "All Trucks"))
                        }
                        truckItems.addAll(trucks.map { TruckItem(it.id!!, it.truckNo?.trim() ?: "") })
                        
                        // Create map of truck names to TruckItems for accurate selection
                        val truckMap = truckItems.associateBy { it.name.trim() }
                        
                        val truckAdapter = object : ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            truckItems.map { it.name }
                        ) {
                            override fun getFilter(): android.widget.Filter {
                                return object : android.widget.Filter() {
                                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                                        val filterResults = FilterResults()
                                        if (constraint == null || constraint.isEmpty()) {
                                            filterResults.values = truckItems.map { it.name }
                                            filterResults.count = truckItems.size
                                        } else {
                                            val filterPattern = constraint.toString().lowercase().trim()
                                            val filteredList = truckItems.filter {
                                                it.name.lowercase().contains(filterPattern)
                                            }.map { it.name }
                                            filterResults.values = filteredList
                                            filterResults.count = filteredList.size
                                        }
                                        return filterResults
                                    }

                                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                                        @Suppress("UNCHECKED_CAST")
                                        val filteredList = results?.values as? List<String> ?: emptyList()
                                        clear()
                                        addAll(filteredList)
                                        notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                        
                        autoComplete.setAdapter(truckAdapter)
                        
                        // Add item click listener
                        autoComplete.setOnItemClickListener { _, view, _, _ ->
                            val selectedText = (view as TextView).text.toString().trim()
                            truckMap[selectedText]?.let { selectedTruck ->
                                selectedTruckId = selectedTruck.id
                                selectedTruckName = selectedTruck.name
                                
                                if (selectedTruck.id == -1) {
                                    binding.truckLayout.helperText = "${trucks.size} trucks selected"
                                } else {
                                    binding.truckLayout.helperText = null
                                }
                                binding.truckLayout.error = null
                                Log.d("AddAutomationRule", "Selected truck: ${selectedTruck.name} (ID: ${selectedTruck.id})")
                            }
                        }
                    }
                    binding.truckLayout.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("AddAutomationRule", "Error loading trucks: ${e.message}")
            }
        }
    }

    private fun setupFormValidation() {
        // Rule name validation
        binding.nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    binding.nameLayout.error = "Rule name is required"
                } else {
                    binding.nameLayout.error = null
                }
            }
        })
        
        // Amount range validation
        binding.minAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateAmountRange()
            }
        })
        
        binding.maxAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateAmountRange()
            }
        })
    }
    
    private fun validateAmountRange() {
        val minAmountStr = binding.minAmountEditText.text.toString()
        val maxAmountStr = binding.maxAmountEditText.text.toString()
        
        if (minAmountStr.isNotBlank() && maxAmountStr.isNotBlank()) {
            try {
                val minAmount = minAmountStr.toDouble()
                val maxAmount = maxAmountStr.toDouble()
                
                if (minAmount > maxAmount) {
                    binding.minAmountLayout.error = "Min amount cannot be greater than max amount"
                    binding.maxAmountLayout.error = "Max amount cannot be less than min amount"
                } else {
                    binding.minAmountLayout.error = null
                    binding.maxAmountLayout.error = null
                }
            } catch (e: NumberFormatException) {
                binding.minAmountLayout.error = "Invalid number format"
                binding.maxAmountLayout.error = "Invalid number format"
            }
        } else {
            binding.minAmountLayout.error = null
            binding.maxAmountLayout.error = null
        }
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        // Validate rule name
        if (binding.nameEditText.text.isNullOrBlank()) {
            binding.nameLayout.error = "Rule name is required"
            isValid = false
        } else {
            binding.nameLayout.error = null
        }
        
        // Validate transactor
        if (selectedTransactorId == null) {
            binding.transactorLayout.error = "Transactor is required"
            isValid = false
        } else {
            binding.transactorLayout.error = null
        }
        
        // Validate owner
        if (selectedOwnerId == null) {
            binding.ownerLayout.error = "Owner is required"
            isValid = false
        } else {
            binding.ownerLayout.error = null
        }
        
        // Validate account
        if (selectedAccountId == null) {
            binding.accountLayout.error = "Account is required"
            isValid = false
        } else {
            binding.accountLayout.error = null
        }
        
        // Validate truck (special case for "Select All")
        if (selectedTruckId == null && selectedTruckId != -1) {
            binding.truckLayout.error = "Truck is required"
            isValid = false
        } else {
            binding.truckLayout.error = null
        }
        
        // Validate amount range
        val minAmountStr = binding.minAmountEditText.text.toString()
        val maxAmountStr = binding.maxAmountEditText.text.toString()
        
        if (minAmountStr.isNotBlank() && maxAmountStr.isNotBlank()) {
            try {
                val minAmount = minAmountStr.toDouble()
                val maxAmount = maxAmountStr.toDouble()
                
                if (minAmount > maxAmount) {
                    binding.minAmountLayout.error = "Min amount cannot be greater than max amount"
                    binding.maxAmountLayout.error = "Max amount cannot be less than min amount"
                    isValid = false
                } else {
                    binding.minAmountLayout.error = null
                    binding.maxAmountLayout.error = null
                }
            } catch (e: NumberFormatException) {
                binding.minAmountLayout.error = "Invalid number format"
                binding.maxAmountLayout.error = "Invalid number format"
                isValid = false
            }
        }
        
        return isValid
    }
    
    private fun populateForm() {
        automationRule?.let { rule ->
            binding.nameEditText.setText(rule.name)
            binding.descriptionPatternEditText.setText(rule.descriptionPattern)
            binding.minAmountEditText.setText(rule.minAmount?.toString() ?: "")
            binding.maxAmountEditText.setText(rule.maxAmount?.toString() ?: "")
            
            // Set selected IDs
            selectedTransactorId = rule.transactorId
            selectedAccountId = rule.accountId
            selectedOwnerId = rule.ownerId
            selectedTruckId = rule.truckId
            
            // Set transactor dropdown with formatted name
            rule.transactorName?.let { name ->
                // Remove double spaces
                val singleSpaceName = name.replace("\\s+".toRegex(), " ").trim()
                
                // Capitalize each word
                val formattedName = singleSpaceName.split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                    }
                }
                
                (binding.transactorLayout.editText as? AutoCompleteTextView)?.setText(formattedName, false)
            }
            
            // Set owner dropdown and enable account/truck fields
            rule.ownerName?.let { name ->
                (binding.ownerLayout.editText as? AutoCompleteTextView)?.setText(name, false)
                
                // Update account and truck dropdowns for this owner
                rule.ownerId?.let { ownerId ->
                    val owner = owners.find { it.id == ownerId }
                    if (owner != null) {
                        updateAccountsForOwner(owner)
                        updateTrucksForOwner(owner)
                        
                        // Set selections after a delay to ensure adapters are loaded
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Set account
                            if (rule.accountId != null && rule.accountName != null) {
                                selectedAccountId = rule.accountId
                                selectedAccountName = rule.accountName
                                (binding.accountLayout.editText as? AutoCompleteTextView)?.setText(rule.accountName, false)
                            }
                            
                            // Set truck - handle "All Trucks" special case
                            if (rule.truckId != null) {
                                selectedTruckId = rule.truckId
                                
                                if (rule.truckId == -1) {
                                    // For "All Trucks" selection
                                    selectedTruckName = "All Trucks"
                                    (binding.truckLayout.editText as? AutoCompleteTextView)?.setText("All Trucks", false)
                                    binding.truckLayout.helperText = "${trucks.size} trucks selected"
                                } else if (rule.truckName != null) {
                                    // For specific truck selection
                                    selectedTruckName = rule.truckName
                                    (binding.truckLayout.editText as? AutoCompleteTextView)?.setText(rule.truckName, false)
                                    binding.truckLayout.helperText = null
                                }
                            }
                        }, 500) // Increased delay to ensure adapters are fully loaded
                    }
                }
            }
        }
    }
    
    private fun saveAutomationRule() {
        val name = binding.nameEditText.text.toString()
        val descriptionPattern = binding.descriptionPatternEditText.text.toString().takeIf { it.isNotBlank() }
        
        val minAmount = binding.minAmountEditText.text.toString().takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val maxAmount = binding.maxAmountEditText.text.toString().takeIf { it.isNotBlank() }?.toDoubleOrNull()
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        // Get the transactor name from the selected transactor
        val transactorName = transactors.find { it.id == selectedTransactorId }?.name
        
        // Get the owner name from the selected owner
        val ownerName = owners.find { it.id == selectedOwnerId }?.name
        
        // Create a temporary rule for conflict checking
        val tempRule = AutomationRule(
            id = automationRule?.id,
            name = name,
            transactorId = selectedTransactorId,
            transactorName = transactorName,
            accountId = selectedAccountId,
            accountName = selectedAccountName,
            ownerId = selectedOwnerId,
            ownerName = ownerName,
            truckId = selectedTruckId, // Will be -1 for "All Trucks"
            truckName = if (selectedTruckId == -1) "All Trucks" else selectedTruckName,
            descriptionPattern = descriptionPattern,
            minAmount = minAmount,
            maxAmount = maxAmount,
            createdAt = automationRule?.createdAt ?: currentDate,
            updatedAt = currentDate
        )
        
        // Check for conflicting rules before saving
        lifecycleScope.launch(Dispatchers.IO) {
            val conflictingRule = checkForConflictingRule(tempRule)
            
            withContext(Dispatchers.Main) {
                if (conflictingRule != null) {
                    // Show conflict alert
                    showConflictAlert(conflictingRule)
                } else {
                    // No conflict, proceed with saving
                    Log.d("AddAutomationRule", "Saving rule: ID=${tempRule.id}, Name=${tempRule.name}, TruckID=${tempRule.truckId}, TruckName=${tempRule.truckName}")
                    onAddAutomationRuleListener?.onAddAutomationRule(tempRule)
                    dismiss()
                }
            }
        }
    }
    
    /**
     * Checks if a rule with similar criteria already exists
     * @param rule The rule to check for conflicts
     * @return The existing conflicting rule, or null if no conflict exists
     */
    private fun checkForConflictingRule(rule: AutomationRule): AutomationRule? {
        // Get all existing rules from the database
        val allRules = dbHelper?.getAllAutomationRules() ?: emptyList()
        
        // Check for conflicts with existing rules
        return allRules.find { existingRule ->
            // Skip comparing with itself when updating
            if (existingRule.id == rule.id) {
                return@find false
            }
            
            // Check if key fields match
            val transactorMatch = existingRule.transactorId == rule.transactorId
            val accountMatch = existingRule.accountId == rule.accountId
            val ownerMatch = existingRule.ownerId == rule.ownerId
            
            // Special handling for truck - consider "All Trucks" (-1) as a potential conflict
            val truckMatch = when {
                // If either rule has "All Trucks", consider it a match if owners match
                existingRule.truckId == -1 && ownerMatch -> true
                rule.truckId == -1 && ownerMatch -> true
                // Otherwise, check for exact truck match
                else -> existingRule.truckId == rule.truckId
            }
            
            // Check description pattern match - null patterns are considered wildcards
            val descriptionMatch = when {
                existingRule.descriptionPattern.isNullOrBlank() && rule.descriptionPattern.isNullOrBlank() -> true
                existingRule.descriptionPattern.isNullOrBlank() || rule.descriptionPattern.isNullOrBlank() -> false
                else -> existingRule.descriptionPattern == rule.descriptionPattern
            }
            
            // Check amount range overlap
            val minAmountOverlap = when {
                existingRule.minAmount == null && rule.minAmount == null -> true
                existingRule.minAmount == null -> true
                rule.minAmount == null -> true
                else -> existingRule.minAmount!! <= (rule.maxAmount ?: Double.MAX_VALUE)
            }
            
            val maxAmountOverlap = when {
                existingRule.maxAmount == null && rule.maxAmount == null -> true
                existingRule.maxAmount == null -> true
                rule.maxAmount == null -> true
                else -> existingRule.maxAmount!! >= (rule.minAmount ?: 0.0)
            }
            
            // Rule is considered a conflict if all criteria match or overlap
            transactorMatch && accountMatch && ownerMatch && truckMatch && minAmountOverlap && maxAmountOverlap
        }
    }
    
    /**
     * Shows a Material Alert Dialog informing the user about a conflicting rule
     */
    private fun showConflictAlert(conflictingRule: AutomationRule) {
        val context = requireContext()
        
        // Format the conflict details
        val conflictDetails = StringBuilder()
        conflictDetails.append("• Rule name: ${conflictingRule.name}\n")
        conflictingRule.transactorName?.let { conflictDetails.append("• Transactor: $it\n") }
        conflictingRule.accountName?.let { conflictDetails.append("• Account: $it\n") }
        conflictingRule.ownerName?.let { conflictDetails.append("• Owner: $it\n") }
        
        if (conflictingRule.truckId == -1) {
            conflictDetails.append("• Truck: All Trucks\n")
        } else {
            conflictingRule.truckName?.let { conflictDetails.append("• Truck: $it\n") }
        }
        
        conflictingRule.descriptionPattern?.let { conflictDetails.append("• Description: $it\n") }
        
        // Format amount range
        val amountRange = when {
            conflictingRule.minAmount != null && conflictingRule.maxAmount != null -> 
                "Between ${formatCurrency(conflictingRule.minAmount!!)} and ${formatCurrency(conflictingRule.maxAmount!!)}"
            conflictingRule.minAmount != null -> 
                "Min: ${formatCurrency(conflictingRule.minAmount!!)}"
            conflictingRule.maxAmount != null -> 
                "Max: ${formatCurrency(conflictingRule.maxAmount!!)}"
            else -> "Any amount"
        }
        conflictDetails.append("• Amount: $amountRange")
        
        // Show the alert dialog
        MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Conflicting Rule Detected")
            .setMessage("This rule conflicts with an existing rule:\n\n${conflictDetails}")
            .setPositiveButton("Edit Rule") { dialog, _ ->
                // Keep the dialog open so user can edit the form
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Dismiss both the alert and the form
                dialog.dismiss()
                this@AddAutomationRuleFragment.dismiss()
            }
            .setIcon(R.drawable.baseline_warning_amber_white_24dp)
            .show()
    }
    
    /**
     * Helper method to format currency values
     */
    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        return formatter.format(amount)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_ACTION = "action"
        private const val ARG_AUTOMATION_RULE = "automation_rule"
        
        fun newInstance(action: String, automationRule: AutomationRule?): AddAutomationRuleFragment {
            return AddAutomationRuleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACTION, action)
                    putSerializable(ARG_AUTOMATION_RULE, automationRule)
                }
            }
        }
    }
} 