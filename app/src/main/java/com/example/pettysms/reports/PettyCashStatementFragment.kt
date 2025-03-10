package com.example.pettysms.reports

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pettysms.R
import com.example.pettysms.databinding.FragmentPettyCashStatementBinding
import com.example.pettysms.DbHelper
import com.example.pettysms.Owner
import com.example.pettysms.Truck
import com.example.pettysms.Transactor
import com.example.pettysms.models.PettyCashTransaction
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import androidx.core.content.ContextCompat
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PettyCashStatementFragment : Fragment() {
    
    private var _binding: FragmentPettyCashStatementBinding? = null
    private val binding get() = _binding!!
    
    private val TAG = "PettyCashStatementFragment"
    private lateinit var dbHelper: DbHelper
    private lateinit var reportsAdapter: SavedReportAdapter
    private val savedReports = mutableListOf<Report>()
    
    // Add loading dialog
    private var loadingDialog: Dialog? = null
    
    private val calendar = Calendar.getInstance()
    private var startDate: Date = calendar.time
    private var endDate: Date = calendar.time
    
    private var selectedTransactor: Transactor? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPettyCashStatementBinding.inflate(inflater, container, false)
        dbHelper = DbHelper(requireContext())
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize loading dialog
        initLoadingDialog()
        
        setupRecyclerView()
        setupDatePickers()
        setupTransactorDropdown()
        setupPaymentModeDropdown()
        setupTruckDropdown()
        setupAccountDropdown()
        setupApplyFiltersButton()
        setupOwnerDropdown()
    }
    
    override fun onResume() {
        super.onResume()
        // Load any saved reports from storage when the fragment becomes visible
        loadSavedReports()
    }
    
    private fun setupRecyclerView() {
        reportsAdapter = SavedReportAdapter(
            savedReports,
            onReportClick = { report -> openReportPreview(report) },
            onDownloadPdf = { report -> downloadReportAsPdf(report) },
            onDownloadExcel = { report -> downloadReportAsExcel(report) },
            onShareReport = { report -> shareReport(report) },
            onDeleteReport = { report -> deleteReport(report) }
        )
        
        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportsAdapter
        }
    }
    
    private fun setupDatePickers() {
        // Set default dates (current month)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = calendar.time
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = calendar.time
        
        // Format and display the default dates
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.startDateEditText.setText(dateFormat.format(startDate))
        binding.endDateEditText.setText(dateFormat.format(endDate))
        
        // Set up click listeners for date inputs
        binding.startDateEditText.setOnClickListener {
            showMaterialDatePicker(binding.startDateEditText, true)
        }
        
        binding.endDateEditText.setOnClickListener {
            showMaterialDatePicker(binding.endDateEditText, false)
        }
    }
    
    private fun showMaterialDatePicker(dateInput: TextInputEditText, isStartDate: Boolean) {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText(if (isStartDate) "Select Start Date" else "Select End Date")
        
        // Try to set initial date from input
        try {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = dateInput.text.toString()
            if (date.isNotEmpty()) {
                val calendar = Calendar.getInstance()
                calendar.time = dateFormat.parse(date)!!
                builder.setSelection(calendar.timeInMillis)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date", e)
        }
        
        val picker = builder.build()
        
        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(calendar.time)
            
            if (isStartDate) {
                startDate = calendar.time
                binding.startDateEditText.setText(formattedDate)
            } else {
                endDate = calendar.time
                binding.endDateEditText.setText(formattedDate)
            }
        }
        
        picker.show(parentFragmentManager, picker.toString())
    }
    
    private fun setupTransactorDropdown() {
        val transactorAutoCompleteTextView = binding.transactorLayout.editText as? AutoCompleteTextView ?: return
        transactorAutoCompleteTextView.threshold = 1 // Start searching after 1 character

        // Don't set an adapter initially - only when user starts typing
        transactorAutoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Only search if user is actively typing (not during initial setup)
                if (!s.isNullOrEmpty() && before != 0) {
                    // Show loading dialog while searching
                    showLoading("Searching transactors...")
                    searchTransactors(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Initialize with "All" as default selection but don't show dropdown
        transactorAutoCompleteTextView.setText("All", false)
    }

    private fun searchTransactors(query: String) {
        // Perform search in the background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ensure database is open
                ensureDatabaseIsOpen()
                
                val results = dbHelper.getTransactorByName(query)

                withContext(Dispatchers.Main) {
                    hideLoading() // Hide loading dialog when search is done

                    // Prepare the list of results and always add "All" option at the beginning
                    val transactorNames = mutableListOf("All")
                    
                    // Add transactor names if they're not already in the list
                    results.forEach { transactor ->
                        transactor.name?.let { name ->
                            val capitalizedName = capitalizeEachWord(name)
                            if (!transactorNames.contains(capitalizedName)) {
                                transactorNames.add(capitalizedName)
                            }
                        }
                    }

                    // Create a map for easy access to transactor objects
                    val transactorMap = results.associateBy { 
                        it.name?.let { capitalizeEachWord(it) } 
                    }

                    // Update the AutoCompleteTextView suggestions
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        transactorNames
                    )
                    (binding.transactorLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
                    
                    // Only show dropdown if user is actively typing
                    // Don't automatically show dropdown

                    // Handle item selection
                    (binding.transactorLayout.editText as? AutoCompleteTextView)?.setOnItemClickListener { parent, _, position, _ ->
                        val selectedItem = parent.getItemAtPosition(position).toString()
                        (binding.transactorLayout.editText as? AutoCompleteTextView)?.setText(selectedItem, false)
                        
                        // Update selectedTransactor if not "All"
                        selectedTransactor = if (selectedItem == "All") {
                            null
                        } else {
                            transactorMap[selectedItem]
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching transactors: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Snackbar.make(binding.root, "Failed to search transactors", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Helper function to capitalize each word in a string
    private fun capitalizeEachWord(input: String): String {
        val words = input.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result = StringBuilder()
        for (word in words) {
            if (word.isNotEmpty()) {
                result.append(word.substring(0, 1).uppercase(Locale.getDefault()))
                    .append(word.substring(1).lowercase(Locale.getDefault()))
                    .append(" ")
            }
        }
        return result.toString().trim()
    }
    
    private fun setupPaymentModeDropdown() {
        val paymentModes = arrayOf("All", "M-Pesa", "Cash")
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_item, paymentModes)
        (binding.paymentModeLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        (binding.paymentModeLayout.editText as? AutoCompleteTextView)?.setText(paymentModes[0], false)
    }
    
    private fun setupTruckDropdown(owner: Owner? = null) {
        val truckAutoComplete = binding.truckLayout.editText as? AutoCompleteTextView ?: return
        truckAutoComplete.threshold = 1 // Start searching after 1 character

        truckAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Only search if user is actively typing (not during initial setup)
                if (!s.isNullOrEmpty() && before != 0) {
                    // Show loading dialog while searching
                    showLoading("Searching trucks...")
                    searchTrucks(s.toString(), owner)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Initialize with "All" as default selection
        truckAutoComplete.setText("All", false)
    }

    private fun searchTrucks(query: String, owner: Owner? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ensure database is open
                ensureDatabaseIsOpen()
                
                // Get truck numbers based on owner
                val truckNumbers = if (owner != null) {
                    // Get Truck objects and extract their numbers
                    dbHelper.getLocalTrucksByOwner(owner).mapNotNull { it.truckNo }
                } else {
                    // getAllTrucks already returns a list of truck numbers as strings
                    dbHelper.getAllTrucks()
                }
                
                withContext(Dispatchers.Main) {
                    hideLoading() // Hide loading dialog when search is done
                    
                    // Filter truck numbers based on query
                    val filteredTruckNumbers = truckNumbers.filter { truckNo ->
                        truckNo.contains(query, ignoreCase = true)
                    }
                    
                    // Prepare the list of results and always add "All" option at the beginning
                    val truckList = mutableListOf("All")
                    
                    // Add truck numbers if they're not already in the list
                    filteredTruckNumbers.forEach { truckNo ->
                        if (!truckList.contains(truckNo)) {
                            truckList.add(truckNo)
                        }
                    }
                    
                    // Update the AutoCompleteTextView suggestions
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        truckList
                    )
                    (binding.truckLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
                    
                    // Don't automatically show dropdown
                    // Only show dropdown if user is actively typing
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching trucks: ${e.message}")
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Snackbar.make(binding.root, "Failed to search trucks", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupAccountDropdown() {
        val accountAutoComplete = binding.accountLayout.editText as? AutoCompleteTextView ?: return
        accountAutoComplete.threshold = 1 // Start searching after 1 character

        // Don't set an adapter initially - only when user starts typing
        accountAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Only search if user is actively typing (not during initial setup)
                if (!s.isNullOrEmpty() && before != 0) {
                    // Show loading dialog while searching
                    showLoading("Searching accounts...")
                    searchAccounts(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Initialize with "All" as default selection but don't show dropdown
        accountAutoComplete.setText("All", false)
    }

    private fun searchAccounts(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ensure database is open
                ensureDatabaseIsOpen()
                
                val accounts = dbHelper.getAllAccounts()
                
                withContext(Dispatchers.Main) {
                    hideLoading() // Hide loading dialog when search is done
                    
                    // Filter accounts based on query
                    val filteredAccounts = accounts.filter { account ->
                        account.name?.contains(query, ignoreCase = true) == true
                    }
                    
                    // Prepare the list of results and always add "All" option at the beginning
                    val accountNames = mutableListOf("All")
                    
                    // Add account names if they're not already in the list
                    filteredAccounts.forEach { account ->
                        account.name?.let { name ->
                            if (!accountNames.contains(name)) {
                                accountNames.add(name)
                            }
                        }
                    }
                    
                    // Update the AutoCompleteTextView suggestions
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        accountNames
                    )
                    (binding.accountLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
                    
                    // Don't automatically show dropdown
                    // Only show dropdown if user is actively typing
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching accounts: ${e.message}")
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Snackbar.make(binding.root, "Failed to search accounts", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupOwnerDropdown() {
        val ownerAutoComplete = binding.ownerLayout.editText as? AutoCompleteTextView ?: return
        
        // Set threshold to 1 to start searching after 1 character
        ownerAutoComplete.threshold = 1
        
        // Set "All" as default selection but don't show dropdown
        ownerAutoComplete.setText("All", false)
        
        // Load owners in the background but don't show dropdown
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ensure database is open
                ensureDatabaseIsOpen()
                
                val owners = dbHelper.getAllOwners()
                
                withContext(Dispatchers.Main) {
                    // Add "All" option at the beginning
                    val ownerList = mutableListOf(Owner(-1, "All", null))
                    ownerList.addAll(owners)
                    
                    // Create custom adapter for owners with logos
                    val ownerAdapter = object : ArrayAdapter<Owner>(
                        requireContext(),
                        R.layout.item_owner_dropdown,
                        R.id.ownerName,
                        ownerList
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
                            
                            // Set owner logo if available and not "All"
                            if (owner.id != -1 && !owner.logoPath.isNullOrEmpty()) {
                                try {
                                    val imageBytes = android.util.Base64.decode(owner.logoPath, android.util.Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    ownerLogoImageView.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error decoding image: ${e.message}")
                                    ownerLogoImageView.setImageResource(R.mipmap.ic_p_logo_foreground)
                                }
                            } else {
                                ownerLogoImageView.setImageResource(R.mipmap.ic_p_logo_foreground)
                            }
                            
                            return view
                        }
                    }
                    
                    // Set the adapter but don't show dropdown
                    ownerAutoComplete.setAdapter(ownerAdapter)
                    
                    // Handle owner selection
                    ownerAutoComplete.setOnItemClickListener { _, _, position, _ ->
                        val selectedOwner = ownerAdapter.getItem(position)
                        if (selectedOwner != null) {
                            // Set the owner name in the dropdown
                            ownerAutoComplete.setText(selectedOwner.name, false)
                            
                            // Enable/populate account and truck dropdowns based on owner
                            if (selectedOwner.id == -1) {
                                // "All" selected - show all accounts and trucks
                                setupAccountDropdown()
                                setupTruckDropdown()
                            } else {
                                // Specific owner selected - filter accounts and trucks
                                setupAccountDropdown(selectedOwner)
                                setupTruckDropdown(selectedOwner)
                            }
                        }
                    }
                    
                    // Add text change listener to show dropdown when typing
                    ownerAutoComplete.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            // Only show dropdown if user is actively typing (not during initial setup)
                            if (!s.isNullOrEmpty() && before != 0 && ownerAutoComplete.adapter != null) {
                                ownerAutoComplete.showDropDown()
                            }
                        }
                        
                        override fun afterTextChanged(s: Editable?) {}
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading owners: ${e.message}")
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Failed to load owners", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupAccountDropdown(owner: Owner? = null) {
        val accountAutoComplete = binding.accountLayout.editText as? AutoCompleteTextView ?: return
        accountAutoComplete.threshold = 1 // Start searching after 1 character

        accountAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Only search if user is actively typing (not during initial setup)
                if (!s.isNullOrEmpty() && before != 0) {
                    // Show loading dialog while searching
                    showLoading("Searching accounts...")
                    searchAccounts(s.toString(), owner)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Initialize with "All" as default selection
        accountAutoComplete.setText("All", false)
    }

    private fun searchAccounts(query: String, owner: Owner? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ensure database is open
                ensureDatabaseIsOpen()
                
                val accounts = if (owner != null) {
                    dbHelper.getAccountsByOwner(owner)
                } else {
                    dbHelper.getAllAccounts()
                }
                
                withContext(Dispatchers.Main) {
                    hideLoading() // Hide loading dialog when search is done
                    
                    // Filter accounts based on query
                    val filteredAccounts = accounts.filter { account ->
                        account.name?.contains(query, ignoreCase = true) == true
                    }
                    
                    // Prepare the list of results and always add "All" option at the beginning
                    val accountNames = mutableListOf("All")
                    
                    // Add account names if they're not already in the list
                    filteredAccounts.forEach { account ->
                        account.name?.let { name ->
                            if (!accountNames.contains(name)) {
                                accountNames.add(name)
                            }
                        }
                    }
                    
                    // Update the AutoCompleteTextView suggestions
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        accountNames
                    )
                    (binding.accountLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
                    
                    // Show dropdown if there are results
                    if (accountNames.size > 1) {
                        (binding.accountLayout.editText as? AutoCompleteTextView)?.showDropDown()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching accounts: ${e.message}")
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Snackbar.make(binding.root, "Failed to search accounts", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupApplyFiltersButton() {
        binding.btnApplyFilters.setOnClickListener {
            if (validateFilters()) {
                // Generate a report with the current filters
                generateReport()
            }
        }
    }
    
    private fun validateFilters(): Boolean {
        if (startDate.after(endDate)) {
            Snackbar.make(binding.root, "Start date must be before end date", Snackbar.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun initLoadingDialog() {
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.loading_dialog, null)

        // Show loading dialog
        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(false)
            .create()
    }
    
    private fun showLoading(message: String = "Loading...") {
        loadingDialog?.findViewById<TextView>(R.id.loading_text)?.text = message
        loadingDialog?.show()
    }
    
    private fun hideLoading() {
        loadingDialog?.dismiss()
    }
    
    // Called from the ReportsActivity when the FAB is clicked
    fun generateReport() {
        if (!validateFilters()) return
        
        val transactor = (binding.transactorLayout.editText as? AutoCompleteTextView)?.text.toString()
        val paymentMode = (binding.paymentModeLayout.editText as? AutoCompleteTextView)?.text.toString()
        val truck = (binding.truckLayout.editText as? AutoCompleteTextView)?.text.toString()
        val account = (binding.accountLayout.editText as? AutoCompleteTextView)?.text.toString()
        val owner = (binding.ownerLayout.editText as? AutoCompleteTextView)?.text.toString()
        
        Log.d(TAG, "Generating PettyCash statement with filters: Transactor=$transactor, PaymentMode=$paymentMode, Owner=$owner, Truck=$truck, Account=$account, Start=${startDate}, End=${endDate}")
        
        // Show loading dialog
        showLoading("Generating Report...")
        
        // Disable apply filters button
        binding.btnApplyFilters.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Generate the report on a background thread
                val report = withContext(Dispatchers.IO) {
                    // Ensure database is open
                    ensureDatabaseIsOpen()
                    
                    generatePettyCashStatementReport()
                }
                
                // Save the report to the database using DbHelper
                withContext(Dispatchers.IO) {
                    // Ensure database is open
                    ensureDatabaseIsOpen()
                    
                    Log.d(TAG, "Saving report to database: ${report.name}, type: ${report.type}")
                    val result = dbHelper.saveReport(report)
                    Log.d(TAG, "Report saved to database, result: $result")
                }
                
                // Refresh the list of saved reports
                loadSavedReports()
                
                // Show success message
                Snackbar.make(binding.root, "Report generated successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error generating report: ${e.message}", e)
                Snackbar.make(binding.root, "Failed to generate report: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                // Hide loading dialog and re-enable button
                hideLoading()
                binding.btnApplyFilters.isEnabled = true
            }
        }
    }
    
    private fun generatePettyCashStatementReport(): Report {
        try {
            // Ensure database is open
            if (!dbHelper.isOpen()) {
                dbHelper = DbHelper(requireContext())
                Log.d(TAG, "Reopened database connection")
            }
            
            // Generate a unique report name with timestamp
            val timestamp = System.currentTimeMillis()
            val reportName = "PettyCash_Statement_$timestamp"
            
            // Create reports directory if it doesn't exist
            val reportsDir = File(requireContext().getExternalFilesDir(null), "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            
            // Format dates for database query
            val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDateStr = dbDateFormat.format(startDate)
            val endDateStr = dbDateFormat.format(endDate)
            
            // Get selected filters
            val transactor = selectedTransactor?.name ?: (binding.transactorLayout.editText as? AutoCompleteTextView)?.text.toString()
            val paymentMode = (binding.paymentModeLayout.editText as? AutoCompleteTextView)?.text.toString()
            val owner = (binding.ownerLayout.editText as? AutoCompleteTextView)?.text.toString()
            val truck = (binding.truckLayout.editText as? AutoCompleteTextView)?.text.toString()
            val account = (binding.accountLayout.editText as? AutoCompleteTextView)?.text.toString()
            
            // Log the filters being used
            Log.d(TAG, "Generating report with filters: startDate=$startDateStr, endDate=$endDateStr, " +
                    "transactor=$transactor, paymentMode=$paymentMode, owner=$owner, truck=$truck, account=$account")
            
            // Get transactions based on filters
            val transactions = try {
                dbHelper.getFilteredPettyCashTransactions(
                    startDate = startDateStr,
                    endDate = endDateStr,
                    transactor = transactor,
                    paymentMode = paymentMode,
                    owner = owner,
                    truck = truck,
                    account = account
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error retrieving transactions: ${e.message}", e)
                emptyList() // Return empty list in case of error
            }
            
            Log.d(TAG, "Retrieved ${transactions.size} transactions for report")
            
            // Create PDF file
            val pdfFile = File(reportsDir, "$reportName.pdf")
            
            // Create CSV file
            val csvFile = File(reportsDir, "$reportName.csv")
            
            // Generate PDF content
            generatePdf(pdfFile, transactions, startDate, endDate)
            
            // Generate CSV content
            generateCSV(csvFile.absolutePath, transactions)
            
            // Create report object
            val report = Report(
                id = UUID.randomUUID().toString(),
                name = reportName,
                type = ReportType.PETTY_CASH_STATEMENT,
                generatedDate = Date(),
                filePath = pdfFile.absolutePath,
                excelFilePath = csvFile.absolutePath,
                filters = mapOf(
                    "startDate" to startDateStr,
                    "endDate" to endDateStr,
                    "transactor" to transactor,
                    "paymentMode" to paymentMode,
                    "owner" to owner,
                    "truck" to truck,
                    "account" to account
                )
            )
            
            // Save report to database
            val success = dbHelper.saveReport(report)
            if (success) {
                Log.d(TAG, "Report saved successfully: $reportName")
            } else {
                Log.e(TAG, "Failed to save report to database")
            }
            
            return report
        } catch (e: Exception) {
            Log.e(TAG, "Error generating report: ${e.message}", e)
            throw e
        }
    }
    
    private fun loadSavedReports() {
        lifecycleScope.launch {
            try {
                // Show loading state
                withContext(Dispatchers.Main) {
                    showLoading("Loading Reports...")
                }
                
                // Load reports from database
                val reports = withContext(Dispatchers.IO) {
                    try {
                        Log.d(TAG, "Loading reports of type: ${ReportType.PETTY_CASH_STATEMENT}")
                        
                        // Ensure database is open
                        ensureDatabaseIsOpen()
                        
                        val result = dbHelper.getReportsByType(ReportType.PETTY_CASH_STATEMENT)
                        Log.d(TAG, "Loaded ${result.size} reports")
                        result
                    } catch (e: Exception) {
                        Log.e(TAG, "Database error while loading reports: ${e.message}", e)
                        null
                    }
                }
                
                // Update UI
                withContext(Dispatchers.Main) {
                    if (reports != null) {
                        savedReports.clear()
                        savedReports.addAll(reports)
                        reportsAdapter.notifyDataSetChanged()
                        updateEmptyState()
                    } else {
                        // Handle error
                        updateEmptyState()
                    }
                    hideLoading()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved reports: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    updateEmptyState()
                    hideLoading()
                    Snackbar.make(binding.root, "Failed to load saved reports: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateEmptyState() {
        binding.apply {
            if (savedReports.isEmpty()) {
                emptyStateContainer.visibility = View.VISIBLE
                recyclerViewReports.visibility = View.GONE
            } else {
                emptyStateContainer.visibility = View.GONE
                recyclerViewReports.visibility = View.VISIBLE
            }
        }
    }
    
    private fun openReportPreview(report: Report) {
        try {
            val pdfFile = File(report.filePath)
            val csvFile = report.excelFilePath?.let { File(it) }
            
            // Check if files exist
            val pdfExists = pdfFile.exists()
            val csvExists = csvFile?.exists() == true
            
            // If neither file exists, show error
            if (!pdfExists && !csvExists) {
                Snackbar.make(binding.root, "Report files not found", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            // Always show dialog to choose format
            val materialAlertDialogBuilder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("View Report")
                .setMessage("Choose the format you want to view")
            
            if (pdfExists) {
                materialAlertDialogBuilder.setPositiveButton("PDF") { _, _ ->
                    openPDFReport(report)
                }
            }
            
            if (csvExists) {
                materialAlertDialogBuilder.setNegativeButton("CSV") { _, _ ->
                    openCSVReport(report)
                }
            }
            
            materialAlertDialogBuilder.setNeutralButton("Cancel", null)
            materialAlertDialogBuilder.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing to open report: ${e.message}")
            Snackbar.make(binding.root, "Failed to open report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun openPDFReport(report: Report) {
        try {
            val file = File(report.filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(intent)
            } else {
                Snackbar.make(binding.root, "PDF file not found", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF report: ${e.message}")
            Snackbar.make(binding.root, "Failed to open PDF report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun openCSVReport(report: Report) {
        try {
            val excelFilePath = report.excelFilePath
            if (excelFilePath.isNullOrEmpty()) {
                Snackbar.make(binding.root, "CSV file not available for this report", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            val file = File(excelFilePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/csv")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(intent)
            } else {
                Snackbar.make(binding.root, "CSV file not found", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening CSV report: ${e.message}")
            Snackbar.make(binding.root, "Failed to open CSV report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun downloadReportAsPdf(report: Report) {
        try {
            // Check if PDF file exists
            val pdfFilePath = report.filePath
            if (pdfFilePath.isNullOrEmpty()) {
                Snackbar.make(binding.root, "PDF file not available for this report", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            val sourceFile = File(pdfFilePath)
            if (sourceFile.exists()) {
                // Create the Downloads directory if it doesn't exist
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                // Create a destination file in the Downloads directory
                val destFile = File(downloadsDir, "PettySMS_${report.name.replace(" ", "_")}.pdf")
                
                // Copy the file
                sourceFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Notify the user
                Snackbar.make(binding.root, "PDF saved to Downloads folder", Snackbar.LENGTH_SHORT).show()
                
                // Notify the system about the new file so it appears in Downloads app
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10 and above, use MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, destFile.name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    
                    val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    requireContext().contentResolver.insert(contentUri, contentValues)?.let { uri ->
                        requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                            val fileInputStream = destFile.inputStream()
                            fileInputStream.copyTo(os)
                            fileInputStream.close()
                        }
                    }
                }
            } else {
                Snackbar.make(binding.root, "Report file not found", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading PDF: ${e.message}")
            Snackbar.make(binding.root, "Failed to download PDF: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun downloadReportAsExcel(report: Report) {
        try {
            // Check if Excel file exists
            val excelFilePath = report.excelFilePath
            if (excelFilePath.isNullOrEmpty()) {
                Snackbar.make(binding.root, "CSV file not available for this report", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            val sourceFile = File(excelFilePath)
            if (!sourceFile.exists()) {
                Snackbar.make(binding.root, "CSV file not found", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            // Create the Downloads directory if it doesn't exist
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            // Create a destination file in the Downloads directory
            val destFile = File(downloadsDir, "PettySMS_${report.name.replace(" ", "_")}.csv")
            
            // Copy the file
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Notify the user
            Snackbar.make(binding.root, "CSV saved to Downloads folder", Snackbar.LENGTH_SHORT).show()
            
            // Notify the system about the new file so it appears in Downloads app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, destFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                requireContext().contentResolver.insert(contentUri, contentValues)?.let { uri ->
                    requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                        val fileInputStream = destFile.inputStream()
                        fileInputStream.copyTo(os)
                        fileInputStream.close()
                    }
                }
            } else {
                // For older Android versions, use the deprecated method
                @Suppress("DEPRECATION")
                val uri = Uri.fromFile(destFile)
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = uri
                requireContext().sendBroadcast(mediaScanIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading CSV: ${e.message}")
            Snackbar.make(binding.root, "Failed to download CSV: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun shareReport(report: Report) {
        try {
            val pdfFile = File(report.filePath)
            val csvFileExists = report.excelFilePath != null && File(report.excelFilePath).exists()
            
            // If PDF doesn't exist and CSV doesn't exist, show error
            if (!pdfFile.exists() && !csvFileExists) {
                Snackbar.make(binding.root, "Report files not found", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            // If only one format exists, share that directly
            if (!pdfFile.exists()) {
                shareCSVReport(report)
                return
            } else if (!csvFileExists) {
                sharePDFReport(report)
                return
            }
            
            // Both formats exist, show dialog to choose
            val materialAlertDialogBuilder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Share Report")
                .setMessage("Choose the format you want to share")
                .setPositiveButton("PDF") { _, _ ->
                    sharePDFReport(report)
                }
                .setNegativeButton("CSV") { _, _ ->
                    shareCSVReport(report)
                }
                .setNeutralButton("Cancel", null)
            
            materialAlertDialogBuilder.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing to share report: ${e.message}")
            Snackbar.make(binding.root, "Failed to share report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun sharePDFReport(report: Report) {
        try {
            val file = File(report.filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )
                
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "application/pdf"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "Share Petty Cash Statement (PDF)"))
            } else {
                Snackbar.make(binding.root, "PDF file not found", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing PDF report: ${e.message}")
            Snackbar.make(binding.root, "Failed to share PDF report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun shareCSVReport(report: Report) {
        try {
            val excelFilePath = report.excelFilePath
            if (excelFilePath.isNullOrEmpty()) {
                Snackbar.make(binding.root, "CSV file not available for this report", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            val file = File(excelFilePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )
                
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "text/csv"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "Share Petty Cash Statement (CSV)"))
            } else {
                Snackbar.make(binding.root, "CSV file not found", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing CSV report: ${e.message}")
            Snackbar.make(binding.root, "Failed to share CSV report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteReport(report: Report) {
        // Use Material 3 AlertDialog
        val materialAlertDialogBuilder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete this report? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Delete the report from the database
                        withContext(Dispatchers.IO) {
                            // Ensure database is open
                            ensureDatabaseIsOpen()
                            
                            dbHelper.deleteReport(report.id)
                        }
                        
                        // Delete the PDF file
                        val pdfFile = File(report.filePath)
                        if (pdfFile.exists()) {
                            pdfFile.delete()
                        }
                        
                        // Delete the CSV file if it exists
                        report.excelFilePath?.let { csvPath ->
                            val csvFile = File(csvPath)
                            if (csvFile.exists()) {
                                csvFile.delete()
                            }
                        }
                        
                        // Remove the report from the list and update the adapter
                        val position = savedReports.indexOf(report)
                        if (position != -1) {
                            savedReports.removeAt(position)
                            reportsAdapter.notifyItemRemoved(position)
                            
                            // Update empty state
                            updateEmptyState()
                        }
                        
                        // Show success message
                        Snackbar.make(binding.root, "Report deleted successfully", Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting report: ${e.message}")
                        Snackbar.make(binding.root, "Failed to delete report: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
        
        materialAlertDialogBuilder.show()
    }
    
    private fun generatePdf(
        file: File,
        transactions: List<PettyCashTransaction>,
        startDate: Date,
        endDate: Date
    ) {
        // Create a new PDF document
        val document = PdfDocument()
        
        // Page configuration - A4 size in landscape orientation
        val pageWidth = 842
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        var canvas = page.canvas
        
        // Paints
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        // Add logo
        val logoBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_p_logo_foreground)
        val logoWidth = 150f
        val logoHeight = logoWidth * logoBitmap.height / logoBitmap.width
        val logoLeft = 30f
        val logoTop = 10f
        canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), null)
        
        // Draw title
        canvas.drawText("PETTY CASH STATEMENT", 190f, 50f, titlePaint)
        
        // Draw filters
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        canvas.drawText("Period: ${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}", 190f, 80f, headerPaint)
        
        // Get filter values
        val transactor = (binding.transactorLayout.editText as? AutoCompleteTextView)?.text.toString()
        val paymentMode = (binding.paymentModeLayout.editText as? AutoCompleteTextView)?.text.toString()
        val owner = (binding.ownerLayout.editText as? AutoCompleteTextView)?.text.toString()
        val truck = (binding.truckLayout.editText as? AutoCompleteTextView)?.text.toString()
        val account = (binding.accountLayout.editText as? AutoCompleteTextView)?.text.toString()
        
        // Draw filter values
        canvas.drawText("Payment Mode: $paymentMode", 190f, 100f, headerPaint)
        if (transactor != "All") canvas.drawText("Transactor: $transactor", 190f, 120f, headerPaint)
        if (owner != "All") canvas.drawText("Owner: $owner", 190f, 140f, headerPaint)
        if (truck != "All") canvas.drawText("Truck: $truck", 190f, 160f, headerPaint)
        if (account != "All") canvas.drawText("Account: $account", 190f, 180f, headerPaint)
        
        // Margins and column widths - Adjusted to give more space to PC Number and push description to the right
        val leftMargin = 30f
        val dateWidth = 80f
        val pcNumberWidth = 130f
        val descriptionWidth = 180f
        val transactorWidth = 110f
        val paymentModeWidth = 70f
        val truckWidth = 90f
        val docWidth = 30f  // Width for the supporting document column
        val amountWidth = 100f
        
        // Add a small padding between columns
        val columnPadding = 5f
        
        // Initial y position for table
        var y = 200f
        
        // Draw table headers
        drawTableHeaders(canvas, leftMargin, y, headerPaint, dateWidth, pcNumberWidth, descriptionWidth, transactorWidth, paymentModeWidth, truckWidth, columnPadding, docWidth)
        
        // Add space after headers
        y += 25
        var currentPage = page
        var currentCanvas = canvas
        var totalAmount = 0.0
        
        // Get all active trucks for each owner to check for "All Trucks" display
        val ownerTrucksMap = mutableMapOf<String, List<String>>()
        
        // Track the current transaction's y position to ensure proper spacing
        var lastTransactionEndY = y
        
        // Draw transactions
        for (transaction in transactions) {
            // Reset y position to ensure consistent spacing between transactions
            y = lastTransactionEndY
            
            // Check if we need a new page
            if (y > pageHeight - 50) {
                document.finishPage(currentPage)
                val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
                currentPage = document.startPage(newPageInfo)
                currentCanvas = currentPage.canvas
                y = 50f
                
                // Draw headers on new page
                drawTableHeaders(currentCanvas, leftMargin, y, headerPaint, dateWidth, pcNumberWidth, descriptionWidth, transactorWidth, paymentModeWidth, truckWidth, columnPadding, docWidth)
                y += 25
                lastTransactionEndY = y
            }
            
            // Format date to dd/mm/yyyy only (without time)
            val dateStr = transaction.date ?: ""
            val formattedDate = if (dateStr.length > 10) {
                dateStr.substring(0, 10)
            } else {
                dateStr
            }
            
            // Track the maximum y position used by any column in this row
            var maxYOffset = 0f
            
            // Draw transaction details
            currentCanvas.drawText(formattedDate, leftMargin, y, textPaint)
            currentCanvas.drawText(transaction.pettyCashNumber ?: "", leftMargin + dateWidth + columnPadding, y, textPaint)
            
            // Handle long description - allow up to 3 lines if needed
            val description = transaction.description ?: ""
            val descriptionX = leftMargin + dateWidth + pcNumberWidth + (columnPadding * 2)
            
            if (textPaint.measureText(description) > descriptionWidth - 10) {
                // Split description into multiple lines
                val words = description.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = ""
                
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (textPaint.measureText(testLine) <= descriptionWidth - 10) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) {
                            lines.add(currentLine)
                            currentLine = word
                        } else {
                            // Word is too long for a single line, add it anyway
                            lines.add(word)
                        }
                    }
                }
                
                // Add the last line if not empty
                if (currentLine.isNotEmpty() && !lines.contains(currentLine)) {
                    lines.add(currentLine)
                }
                
                // Limit to 3 lines
                val displayLines = lines.take(3)
                
                // Draw each line
                for (i in displayLines.indices) {
                    currentCanvas.drawText(
                        displayLines[i], 
                        descriptionX, 
                        y + (i * 15), 
                        textPaint
                    )
                }
                
                // Update max Y offset if description takes multiple lines
                if (displayLines.size > 1) {
                    maxYOffset = maxOf(maxYOffset, (displayLines.size - 1) * 15f)
                }
            } else {
                currentCanvas.drawText(description, descriptionX, y, textPaint)
            }
            
            // Format transactor name - capitalize each word and fix spacing
            val transactorName = transaction.transactorName ?: ""
            val formattedTransactorName = formatTransactorName(transactorName)
            val transactorX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + (columnPadding * 3)
            
            // Handle long transactor names
            if (textPaint.measureText(formattedTransactorName) > transactorWidth - 5) {
                // Split into multiple lines if needed
                val words = formattedTransactorName.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = ""
                
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (textPaint.measureText(testLine) <= transactorWidth - 5) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) {
                            lines.add(currentLine)
                            currentLine = word
                        } else {
                            // Word is too long for a single line, add it anyway
                            lines.add(word)
                        }
                    }
                }
                
                // Add the last line if not empty
                if (currentLine.isNotEmpty() && !lines.contains(currentLine)) {
                    lines.add(currentLine)
                }
                
                // Limit to 2 lines
                val displayLines = lines.take(2)
                
                // Draw each line
                for (i in displayLines.indices) {
                    currentCanvas.drawText(
                        displayLines[i],
                        transactorX,
                        y + (i * 15),
                        textPaint
                    )
                }
                
                // Update max Y offset if transactor name takes multiple lines
                if (displayLines.size > 1) {
                    maxYOffset = maxOf(maxYOffset, (displayLines.size - 1) * 15f)
                }
            } else {
                currentCanvas.drawText(formattedTransactorName, transactorX, y, textPaint)
            }
            
            val paymentModeX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + (columnPadding * 4)
            currentCanvas.drawText(transaction.paymentMode ?: "", paymentModeX, y, textPaint)
            
            // Handle truck display - check if "All Trucks" should be shown
            val owner = transaction.ownerName?.let { dbHelper.getOwnerByName(it) }
            val ownerCode = owner?.ownerCode ?: ""
            val truckNumbers = transaction.truckNumber?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            var activeTruckList = emptyList<String>()

            if (owner != null) {
                // Get only active and not deleted trucks
                activeTruckList = dbHelper.getLocalTrucksByOwner(owner)
                    .filter { !it.isDeleted && it.activeStatus == true }
                    .mapNotNull { it.truckNo }
                    .filter { it.isNotEmpty() }
            }

            
            // Check if all active trucks are included
            // Log for debugging
            Log.d(TAG, "Transaction trucks: ${truckNumbers.joinToString()}")
            Log.d(TAG, "Active trucks for owner $ownerCode: ${activeTruckList.joinToString()}")
            
            // Determine if this is an "All Trucks" scenario - improved logic
            val isAllTrucks = if (activeTruckList.isNotEmpty() && truckNumbers.isNotEmpty()) {
                // Check if the transaction trucks contain all active trucks
                // This is a more lenient check that allows for "All Trucks" to be displayed
                // even if there are some extra trucks in the transaction
                val allActiveIncluded = activeTruckList.all { activeTruck ->
                    truckNumbers.any { it.trim() == activeTruck.trim() }
                }
                
                // Only consider it "All Trucks" if all active trucks are included
                // and the number of trucks is significant (more than 1)
                val result = allActiveIncluded && activeTruckList.size > 1 &&
                             truckNumbers.size >= activeTruckList.size
                
                Log.d(TAG, "Is All Trucks? $result (Active: ${activeTruckList.size}, Transaction: ${truckNumbers.size})")
                result
            } else {
                false
            }
            
            val truckX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + paymentModeWidth + (columnPadding * 5)
            val truckDisplay = if (isAllTrucks) {
                "All Trucks"
            } else if (truckNumbers.size > 1) {
                // If multiple trucks but not all, show on multiple lines
                val truckLines = mutableListOf<String>()
                var currentLine = ""
                
                for (truck in truckNumbers) {
                    val testLine = if (currentLine.isEmpty()) truck else "$currentLine, $truck"
                    if (textPaint.measureText(testLine) <= truckWidth - 5) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) {
                            truckLines.add(currentLine)
                            currentLine = truck
                        } else {
                            // Truck number is too long for a single line
                            truckLines.add(truck)
                        }
                    }
                }
                
                // Add the last line if not empty
                if (currentLine.isNotEmpty() && !truckLines.contains(currentLine)) {
                    truckLines.add(currentLine)
                }
                
                // Draw each line of trucks
                for (i in truckLines.indices) {
                    if (i > 0) {
                        // Only draw additional lines after the first one
                        currentCanvas.drawText(
                            truckLines[i],
                            truckX,
                            y + (i * 15),
                            textPaint
                        )
                    }
                }
                
                // Update max Y offset if truck display takes multiple lines
                if (truckLines.size > 1) {
                    maxYOffset = maxOf(maxYOffset, (truckLines.size - 1) * 15f)
                }
                
                // Return first line for initial drawing
                truckLines.firstOrNull() ?: ""
            } else {
                // Single truck or empty
                truckNumbers.firstOrNull() ?: ""
            }
            
            // Draw truck display
            currentCanvas.drawText(
                truckDisplay,
                truckX,
                y,
                textPaint
            )
            
            // Draw supporting document indicator
            val docX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + paymentModeWidth + truckWidth + (columnPadding * 5.5f)
            
            // Check if this transaction has a supporting document by querying the database
            val hasSupportingDoc = transaction.id?.let { transactionId ->
                // Query the database to check if this transaction has a supporting document
                val pettyCash = dbHelper.getPettyCashById(transactionId)
                pettyCash?.supportingDocument != null
            } ?: false
            
            if (hasSupportingDoc) {
                // Draw a checkmark for transactions with supporting documents
                currentCanvas.drawText("✓", docX + 10, y, textPaint)
            }
            
            // Format amount with commas
            val amount = transaction.amount ?: 0.0
            val formattedAmount = String.format("%,.2f", amount)
            val amountX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + paymentModeWidth + truckWidth + docWidth + (columnPadding * 6.5f)
            
            currentCanvas.drawText(
                formattedAmount,
                amountX,
                y,
                textPaint
            )
            
            totalAmount += amount
            
            // Update y position for next row based on the maximum offset used in this row
            // Add a fixed spacing between transactions
            lastTransactionEndY = y + maxYOffset + 20 // 20 is the fixed spacing between transactions
        }
        
        // Update y to the last transaction's end position
        y = lastTransactionEndY
        
        // Check if there's enough space for total and footer
        if (y + 80 > pageHeight - 50f) {
            document.finishPage(currentPage)
            val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
            currentPage = document.startPage(newPageInfo)
            currentCanvas = currentPage.canvas
            y = 50f
        }
        
        // Draw total
        y += 10
        currentCanvas.drawLine(leftMargin, y, pageWidth - leftMargin, y, Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })
        y += 20
        
        // Calculate the exact positions for total label and amount to ensure proper alignment
        // The total label should be right-aligned to the left of the amount column
        val totalLabelText = "Total Amount"
        val totalLabelWidth = headerPaint.measureText(totalLabelText)
        
        // Format total with commas
        val formattedTotal = String.format("%,.2f", totalAmount)
        
        // Calculate the exact position for the amount column (same as used for transaction amounts)
        val amountColumnX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + paymentModeWidth + truckWidth + docWidth + (columnPadding * 6.5f)
        
        // Position the total label to end just before the amount column
        val totalLabelX = amountColumnX - totalLabelWidth - 10 // 10 is extra padding
        
        // Draw the total label and amount
        currentCanvas.drawText(totalLabelText, totalLabelX, y, headerPaint)
        currentCanvas.drawText(formattedTotal, amountColumnX, y, headerPaint)
        
        // Draw footer
        y += 30
        currentCanvas.drawText("This is a computer-generated statement and does not require a signature.", leftMargin, y, textPaint)
        
        // Finish page and document
        document.finishPage(currentPage)
        
        // Write to file
        try {
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            
            // Make the file visible to the system's media scanner using modern approach
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                }
                
                val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                requireContext().contentResolver.insert(contentUri, contentValues)?.let { uri ->
                    requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                        val fileInputStream = file.inputStream()
                        fileInputStream.copyTo(os)
                        fileInputStream.close()
                    }
                }
            } else {
                // For older Android versions, use the deprecated method
                @Suppress("DEPRECATION")
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(file)
                mediaScanIntent.data = contentUri
                requireContext().sendBroadcast(mediaScanIntent)
            }
            
            // Also generate CSV file
            generateCSV(file.absolutePath.replace(".pdf", ".csv"), transactions)
            
            Log.d(TAG, "PDF generated successfully: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing PDF: ${e.message}", e)
        }
    }
    
    private fun drawTableHeaders(
        canvas: Canvas,
        leftMargin: Float,
        y: Float,
        headerPaint: Paint,
        dateWidth: Float,
        pcNumberWidth: Float,
        descriptionWidth: Float,
        transactorWidth: Float,
        paymentModeWidth: Float,
        truckWidth: Float,
        columnPadding: Float,
        docWidth: Float = 30f
    ) {
        canvas.drawText("Date", leftMargin, y, headerPaint)
        canvas.drawText("PC Number", leftMargin + dateWidth + columnPadding, y, headerPaint)
        canvas.drawText("Description", leftMargin + dateWidth + pcNumberWidth + (columnPadding * 2), y, headerPaint)
        canvas.drawText("Transactor", leftMargin + dateWidth + pcNumberWidth + descriptionWidth + (columnPadding * 3), y, headerPaint)
        canvas.drawText("Mode", leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + (columnPadding * 4), y, headerPaint)
        canvas.drawText("Truck", leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + paymentModeWidth + (columnPadding * 5), y, headerPaint)
        
        // Push the Doc column a bit to the left (reduce spacing)
        val docX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + paymentModeWidth + truckWidth + (columnPadding * 5.5f)
        canvas.drawText("Doc", docX, y, headerPaint)
        
        // Calculate the amount column position consistently
        val amountX = leftMargin + dateWidth + pcNumberWidth + descriptionWidth + transactorWidth + paymentModeWidth + truckWidth + docWidth + (columnPadding * 6.5f)
        canvas.drawText("Amount", amountX, y, headerPaint)
        
        // Draw line under headers
        val lineY = y + 5
        canvas.drawLine(leftMargin, lineY, 842 - leftMargin, lineY, Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })
    }
    
    // Helper function to format transactor names
    private fun formatTransactorName(name: String): String {
        if (name.isEmpty()) return ""
        
        // Replace multiple spaces with a single space
        val cleanedName = name.replace("\\s+".toRegex(), " ").trim()
        
        // Capitalize each word
        return capitalizeEachWord(cleanedName)
    }
    
    // Generate CSV file from transactions
    private fun generateCSV(filePath: String, transactions: List<PettyCashTransaction>) {
        try {
            val file = File(filePath)
            val writer = FileOutputStream(file).bufferedWriter()
            
            // Write CSV header
            writer.write("Date,PC Number,Description,Transactor,Payment Mode,Truck,Supporting Document,Amount")
            writer.newLine()
            
            // Write transaction data
            for (transaction in transactions) {
                val dateStr = transaction.date ?: ""
                val formattedDate = if (dateStr.length > 10) dateStr.substring(0, 10) else dateStr
                
                val pcNumber = transaction.pettyCashNumber ?: ""
                val description = transaction.description?.replace(",", ";") ?: ""
                val transactorName = formatTransactorName(transaction.transactorName ?: "")
                val paymentMode = transaction.paymentMode ?: ""
                val truckNumber = transaction.truckNumber ?: ""
                
                // Check if this transaction has a supporting document
                val hasSupportingDoc = transaction.id?.let { transactionId ->
                    // Query the database to check if this transaction has a supporting document
                    val pettyCash = dbHelper.getPettyCashById(transactionId)
                    pettyCash?.supportingDocument != null
                } ?: false
                
                val supportingDocValue = if (hasSupportingDoc) "1" else "0"
                val amount = transaction.amount ?: 0.0
                
                // Format CSV line
                val line = "$formattedDate,$pcNumber,\"$description\",\"$transactorName\",$paymentMode,\"$truckNumber\",$supportingDocValue,${String.format("%.2f", amount)}"
                writer.write(line)
                writer.newLine()
            }
            
            // Write total
            val totalAmount = transactions.sumOf { it.amount ?: 0.0 }
            writer.write(",,,,,,,${String.format("%.2f", totalAmount)}")
            
            writer.close()
            
            Log.d(TAG, "CSV generated successfully: $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating CSV: ${e.message}", e)
        }
    }
    
    /**
     * Ensures the database connection is open and reconnects if needed
     * This should be called before any database operation in background threads
     */
    private fun ensureDatabaseIsOpen() {
        try {
            if (!dbHelper.isOpen()) {
                Log.d(TAG, "Database was closed, reopening")
                dbHelper = DbHelper(requireContext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking database connection: ${e.message}", e)
            // Create a new connection anyway
            dbHelper = DbHelper(requireContext())
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Dismiss loading dialog to prevent leaks
        loadingDialog?.dismiss()
        loadingDialog = null
        
        try {
            if (::dbHelper.isInitialized) {
                // Don't close the database here, as it might be used by other fragments
                // dbHelper.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with database: ${e.message}")
        }
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the database when the fragment is completely destroyed
        try {
            if (::dbHelper.isInitialized) {
                dbHelper.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database: ${e.message}")
        }
    }
} 