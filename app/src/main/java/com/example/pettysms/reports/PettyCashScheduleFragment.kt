package com.example.pettysms.reports

import android.content.Intent
import android.graphics.BitmapFactory
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
import com.example.pettysms.BuildConfig
import com.example.pettysms.R
import com.example.pettysms.databinding.FragmentPettyCashScheduleBinding
import com.example.pettysms.DbHelper
import com.example.pettysms.Owner
import com.example.pettysms.PettyCash
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

class PettyCashScheduleFragment : Fragment() {
    
    private var _binding: FragmentPettyCashScheduleBinding? = null
    private val binding get() = _binding!!
    
    private val TAG = "PettyCashScheduleFragment"
    private lateinit var dbHelper: DbHelper
    private val savedSchedules = mutableListOf<Report>()
    private lateinit var schedulesAdapter: SavedReportAdapter
    
    private val calendar = Calendar.getInstance()
    private var startDate: Date = calendar.time
    private var endDate: Date = calendar.time
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    private var selectedOwnerId: String? = null
    private var selectedOwnerObject: Owner? = null
    private var selectedTruckId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPettyCashScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        dbHelper = DbHelper(requireContext())
        
        setupDatePickers()
        setupOwnerDropdown()
        setupTruckDropdown()
        setupGenerateButton()
        setupRecyclerView()
        
        // Load existing reports from database
        loadSavedReports()
        
        // For placeholder purposes, we'll show a message
        updateEmptyState()
    }
    
    private fun setupRecyclerView() {
        schedulesAdapter = SavedReportAdapter(
            savedSchedules,
            onReportClick = { report -> viewReport(report) },
            onDownloadPdf = null,
            onDownloadExcel = { report -> downloadReport(File(report.filePath), report.name) },
            onShareReport = { report -> shareReport(report) },
            onDeleteReport = { report -> showDeleteConfirmationDialog(report) }
        )
        
        binding.recyclerViewSchedules.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = schedulesAdapter
            addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(), 
                androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
            ))
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
    
    private fun showMaterialDatePicker(dateInput: com.google.android.material.textfield.TextInputEditText, isStartDate: Boolean) {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText(if (isStartDate) "Select Start Date" else "Select End Date")
        
        // Try to set initial date from input
        try {
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
    
    private fun setupOwnerDropdown() {
        val ownerAutoComplete = binding.ownerLayout.editText as? AutoCompleteTextView ?: return
        
        // Set threshold to 1 to start searching after 1 character
        ownerAutoComplete.threshold = 1
        
        // Set "All" as default selection but don't show dropdown
        ownerAutoComplete.setText("All", false)
        
        // Load owners in the background but don't show dropdown
        lifecycleScope.launch(Dispatchers.IO) {
            try {
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
                            
                            // Store the selected owner ID (or null for "All")
                            selectedOwnerId = if (selectedOwner.id == -1) null else selectedOwner.ownerCode
                            selectedOwnerObject = selectedOwner
                            
                            // Update truck dropdown based on selected owner
                            updateTruckDropdown(selectedOwnerId)
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
    
    private fun setupTruckDropdown() {
        updateTruckDropdown(null) // Initially load all trucks
    }
    
    private fun updateTruckDropdown(ownerId: String?) {
        lifecycleScope.launch {
            try {
                val trucks = withContext(Dispatchers.IO) {
                    if (ownerId != null) {
                        getTrucksByOwner(ownerId)
                    } else {
                    dbHelper.getAllTrucks()
                    }
                }
                
                val truckList = mutableListOf("All")
                truckList.addAll(trucks)
                
                val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_item, truckList)
                (binding.truckLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
                (binding.truckLayout.editText as? AutoCompleteTextView)?.setText(truckList[0], false)
                
                // Set up item click listener
                (binding.truckLayout.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
                    selectedTruckId = if (position == 0) null else truckList[position]
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading trucks: ${e.message}")
                Snackbar.make(binding.root, "Failed to load trucks", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getTrucksByOwner(ownerId: String): List<String> {
        // Get all trucks
        val allTrucks = dbHelper.getAllTrucks()
        
        // Get all petty cash entries for this owner
        val pettyCashList = dbHelper.getPettyCashByDateRangeAndOwner(
            "01/01/2000", // Use a wide date range to get all entries
            "31/12/2099",
            ownerId
        )
        
        // Extract unique truck numbers from petty cash entries
        val ownerTrucks = pettyCashList
            .flatMap { it.trucks ?: emptyList() }
            .mapNotNull { it.truckNo }
            .distinct()
        
        // Return only trucks that are in both lists
        return allTrucks.filter { it in ownerTrucks }
    }
    
    private fun setupGenerateButton() {
        binding.btnGenerateSchedule.setOnClickListener {
            generateSchedule()
        }
    }
    
    private fun getSelectedScheduleType(): String {
        val checkedChipId = binding.scheduleTypeChipGroup.checkedChipId
        return when (checkedChipId) {
            R.id.chipDaily -> "Daily"
            R.id.chipWeekly -> "Weekly"
            R.id.chipMonthly -> "Monthly"
            else -> "Daily"  // Default to daily if nothing is selected
        }
    }
    
    fun generateReport() {
        // This is called from the ReportsActivity when the FAB is clicked
        generateSchedule()
    }
    
    private fun generateSchedule() {
        val truck = (binding.truckLayout.editText as? AutoCompleteTextView)?.text.toString()
        val owner = (binding.ownerLayout.editText as? AutoCompleteTextView)?.text.toString()
        val scheduleType = getSelectedScheduleType()
        
        val startDateStr = dateFormat.format(startDate)
        val endDateStr = dateFormat.format(endDate)
        
        Log.d(TAG, "Generating $scheduleType schedule for truck: $truck, owner: $owner from $startDateStr to $endDateStr")
        
        // Show loading indicator
        Snackbar.make(binding.root, "Generating schedule...", Snackbar.LENGTH_SHORT).show()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Format dates for database query
                val startDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate)
                val endDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(endDate)
                
                // Get petty cash transactions based on filters
                var pettyCashList = when {
                    selectedOwnerId != null && selectedTruckId != null -> {
                        // Filter by both owner and truck
                        val ownerPettyCash = dbHelper.getPettyCashByDateRangeAndOwner(
                            startDateFormatted, 
                            endDateFormatted, 
                            selectedOwnerId!!
                        )
                        // Further filter by truck
                        ownerPettyCash.filter { pettyCash ->
                            pettyCash.trucks?.any { it.truckNo == selectedTruckId } == true
                        }
                    }
                    selectedOwnerId != null -> {
                        dbHelper.getPettyCashByDateRangeAndOwner(
                            startDateFormatted, 
                            endDateFormatted, 
                            selectedOwnerId!!
                        )
                    }
                    selectedTruckId != null -> {
                        // Get all petty cash and filter by truck
                        val allPettyCash = dbHelper.getPettyCashByDateRange(startDateFormatted, endDateFormatted)
                        allPettyCash.filter { pettyCash ->
                            pettyCash.trucks?.any { it.truckNo == selectedTruckId } == true
                        }
                    }
                    else -> {
                        dbHelper.getPettyCashByDateRange(startDateFormatted, endDateFormatted)
                    }
                }
                
                // Filter out entries with null pettyCashNumber
                pettyCashList = pettyCashList.filter { !it.pettyCashNumber.isNullOrEmpty() }
                
                if (pettyCashList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "No petty cash transactions found for the selected criteria", Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // Generate CSV file with tabs based on schedule type
                val csvFile = generateCsvWithTabs(pettyCashList, scheduleType)
                
                if (csvFile != null) {
                    // Save the report to database
                    val reportId = UUID.randomUUID().toString()
                    val reportName = buildReportName(owner, truck, scheduleType, startDateStr, endDateStr)
                    
                    val report = Report(
                        id = reportId,
                        name = reportName,
                        type = ReportType.PETTY_CASH_SCHEDULE,
                        generatedDate = Date(),
                        filePath = csvFile.absolutePath,
                        excelFilePath = null,
                        filters = mapOf(
                            "owner" to (owner),
                            "truck" to (truck),
                            "scheduleType" to scheduleType,
                            "startDate" to startDateStr,
                            "endDate" to endDateStr,
                            "count" to pettyCashList.size.toString()
                        )
                    )
                    
                    // Save the report to database
                    val success = dbHelper.saveReport(report)
                    
                    if (!success) {
                        Log.e(TAG, "Failed to save report to database")
                    }
                    
                    // Add to local list and update UI
                    withContext(Dispatchers.Main) {
                        savedSchedules.add(0, report)
                        schedulesAdapter.notifyItemInserted(0)
                        updateEmptyState()
                        showReportOptionsDialog(csvFile, report)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "Failed to generate schedule", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating schedule: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error generating schedule: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun buildReportName(owner: String, truck: String, scheduleType: String, startDateStr: String, endDateStr: String): String {
        val ownerPart = if (owner == "All") "" else "$owner "
        val truckPart = if (truck == "All") "" else "$truck "
        return "${ownerPart}${truckPart}${scheduleType} Schedule ${startDateStr} - ${endDateStr}"
    }
    
    private fun generateCsvWithTabs(pettyCashList: List<PettyCash>, scheduleType: String): File? {
        if (pettyCashList.isEmpty()) return null
        
        try {
            // Create a file for the CSV
            val fileName = "PettyCash_Schedule_${scheduleType}_${System.currentTimeMillis()}.csv"
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val outputStream = FileOutputStream(file)
            
            // Group petty cash transactions based on schedule type
            val groupedTransactions = when (scheduleType) {
                "Daily" -> groupTransactionsByDay(pettyCashList)
                "Weekly" -> groupTransactionsByWeek(pettyCashList)
                "Monthly" -> groupTransactionsByMonth(pettyCashList)
                else -> groupTransactionsByDay(pettyCashList) // Default to daily
            }
            
            // Write CSV header
            val header = "Date,Petty Cash Number,Amount,Transactor,Payment Mode,Account,Description\n"
            outputStream.write(header.toByteArray())
            
            // Write each group as a separate section with a tab header
            groupedTransactions.forEach { (groupKey, transactions) ->
                // Add a tab header
                val tabHeader = "\n--- $groupKey ---\n"
                outputStream.write(tabHeader.toByteArray())
                
                // Write transactions for this tab
                transactions.forEach { pettyCash ->
                    // Safely handle null values
                    val date = pettyCash.date ?: ""
                    val pettyCashNumber = pettyCash.pettyCashNumber ?: ""
                    val amount = pettyCash.amount ?: 0.0
                    val transactor = pettyCash.transactor?.name ?: ""
                    val paymentMode = pettyCash.paymentMode ?: ""
                    val account = pettyCash.account?.name ?: ""
                    val description = pettyCash.description?.replace("\"", "\"\"") ?: ""
                    
                    val line = "$date," +
                            "$pettyCashNumber," +
                            "$amount," +
                            "$transactor," +
                            "$paymentMode," +
                            "$account," +
                            "\"$description\"\n"
                    
                    outputStream.write(line.toByteArray())
                }
                
                // Add a summary for this tab
                val totalAmount = transactions.sumOf { it.amount ?: 0.0 }
                val summaryLine = "Total for $groupKey,,,,,,$totalAmount\n"
                outputStream.write(summaryLine.toByteArray())
            }
            
            // Add a grand total at the end
            val grandTotal = pettyCashList.sumOf { it.amount ?: 0.0 }
            val grandTotalLine = "\nGRAND TOTAL,,,,,,$grandTotal\n"
            outputStream.write(grandTotalLine.toByteArray())
            
            outputStream.close()
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating CSV: ${e.message}", e)
            return null
        }
    }
    
    private fun groupTransactionsByDay(pettyCashList: List<PettyCash>): Map<String, List<PettyCash>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return pettyCashList.groupBy { pettyCash ->
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(pettyCash.date ?: "")
            date?.let { dateFormat.format(it) } ?: "Unknown"
        }
    }
    
    private fun groupTransactionsByWeek(pettyCashList: List<PettyCash>): Map<String, List<PettyCash>> {
        val result = mutableMapOf<String, MutableList<PettyCash>>()
        val calendar = Calendar.getInstance()
        
        pettyCashList.forEach { pettyCash ->
            try {
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(pettyCash.date ?: "")
                date?.let {
                    calendar.time = it
                    val year = calendar.get(Calendar.YEAR)
                    val week = calendar.get(Calendar.WEEK_OF_YEAR)
                    val weekKey = "Week $week, $year"
                    
                    if (!result.containsKey(weekKey)) {
                        result[weekKey] = mutableListOf()
                    }
                    result[weekKey]?.add(pettyCash)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date: ${e.message}")
            }
        }
        
        return result
    }
    
    private fun groupTransactionsByMonth(pettyCashList: List<PettyCash>): Map<String, List<PettyCash>> {
        val result = mutableMapOf<String, MutableList<PettyCash>>()
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        pettyCashList.forEach { pettyCash ->
            try {
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(pettyCash.date ?: "")
                date?.let {
                    calendar.time = it
                    val monthKey = monthFormat.format(calendar.time)
                    
                    if (!result.containsKey(monthKey)) {
                        result[monthKey] = mutableListOf()
                    }
                    result[monthKey]?.add(pettyCash)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date: ${e.message}")
            }
        }
        
        return result
    }
    
    private fun showReportOptionsDialog(file: File, report: Report) {
        val options = arrayOf("View", "Share", "Download CSV")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Schedule Generated Successfully")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewReport(report)
                    1 -> shareReport(report)
                    2 -> downloadReport(file, report.name)
                }
            }
            .show()
    }
    
    private fun viewReport(report: Report) {
        try {
            val file = File(report.filePath)
            if (!file.exists()) {
                Snackbar.make(binding.root, "File not found", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Log.e(TAG, "Error viewing report: ${e.message}", e)
            Snackbar.make(binding.root, "Error viewing report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun shareReport(report: Report) {
        try {
            val file = File(report.filePath)
            if (!file.exists()) {
                Snackbar.make(binding.root, "File not found", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share Schedule"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing report: ${e.message}", e)
            Snackbar.make(binding.root, "Error sharing report: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun downloadReport(file: File, fileName: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDir, "${fileName}.csv")
            
            file.copyTo(destinationFile, overwrite = true)
            
            Snackbar.make(binding.root, "CSV schedule saved to Downloads folder", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading CSV: ${e.message}", e)
            Snackbar.make(binding.root, "Error downloading CSV: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun loadSavedReports() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val reports = dbHelper.getReportsByType(ReportType.PETTY_CASH_SCHEDULE)
                Log.d(TAG, "Loaded ${reports.size} schedule reports from database")
                
                // Verify report files exist
                val validReports = reports.filter { report ->
                    val fileExists = File(report.filePath).exists()
                    if (!fileExists) {
                        Log.w(TAG, "Report file not found: ${report.filePath}")
                    }
                    fileExists
                }
                
                if (validReports.size < reports.size) {
                    Log.w(TAG, "Found ${reports.size - validReports.size} reports with missing files")
                }
                
                withContext(Dispatchers.Main) {
                    savedSchedules.clear()
                    savedSchedules.addAll(validReports)
                    schedulesAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved reports: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error loading saved reports", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showDeleteConfirmationDialog(report: Report) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this schedule? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteReport(report)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteReport(report: Report) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delete the file
                val file = File(report.filePath)
                if (file.exists()) {
                    file.delete()
                }
                
                // Delete from database
                dbHelper.deleteReport(report.id)
                
                // Remove from the list and update UI
                withContext(Dispatchers.Main) {
                    val position = savedSchedules.indexOf(report)
                    if (position != -1) {
                        savedSchedules.removeAt(position)
                        schedulesAdapter.notifyItemRemoved(position)
                        updateEmptyState()
                        Snackbar.make(binding.root, "Schedule deleted", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting report: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error deleting report: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateEmptyState() {
        if (savedSchedules.isEmpty()) {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.recyclerViewSchedules.visibility = View.GONE
        } else {
            binding.emptyStateContainer.visibility = View.GONE
            binding.recyclerViewSchedules.visibility = View.VISIBLE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 