package com.example.pettysms.reports

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pettysms.DbHelper
import com.example.pettysms.MpesaTransaction
import com.example.pettysms.R
import com.example.pettysms.Transactor
import com.example.pettysms.databinding.FragmentMpesaStatementBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
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
import android.app.Dialog
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MpesaStatementFragment : Fragment() {
    
    private var _binding: FragmentMpesaStatementBinding? = null
    private val binding get() = _binding!!
    
    private val TAG = "MpesaStatementFragment"
    private lateinit var dbHelper: DbHelper
    private lateinit var reportsAdapter: SavedReportAdapter
    private val savedReports = mutableListOf<Report>()
    private var selectedTransactor: Transactor? = null
    
    // Add loading dialog
    private var loadingDialog: Dialog? = null
    
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val transactionDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val transactionDateFormatShort = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMpesaStatementBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated started")
        
        // Initialize database helper
        dbHelper = DbHelper(requireContext())
        
        // Initialize views with default visibility
        binding.apply {
            recyclerViewReports.visibility = View.VISIBLE  // Changed to VISIBLE by default
            progressBar.visibility = View.GONE
            emptyStateContainer.visibility = View.GONE
        }
        
        // Initialize loading dialog
        initLoadingDialog()
        
        setupRecyclerView()
        setupDatePickers()
        setupTransactionTypeDropdown()
        setupTransactorDropdown()
        setupApplyFiltersButton()
        
        // Load reports immediately
        loadSavedReports()
        
        // Add lifecycle observer to handle visibility changes
        viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                super.onResume(owner)
                // Reinitialize database helper if needed
                if (!::dbHelper.isInitialized || dbHelper.readableDatabase?.isOpen == false) {
                    dbHelper = DbHelper(requireContext())
                }
                // Always reload reports on resume to ensure latest data
                loadSavedReports()
            }
            
            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
                super.onPause(owner)
                try {
                    if (::dbHelper.isInitialized) {
                        dbHelper.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing database: ${e.message}")
                }
            }
        })
    }
    
    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        reportsAdapter = SavedReportAdapter(
            savedReports,
            onReportClick = { report -> openReport(report) },
            onDownloadPdf = { report -> downloadReportAsPdf(report) },
            onDownloadExcel = { report -> downloadReportAsExcel(report) },
            onShareReport = { report -> shareReport(report) },
            onDeleteReport = { report -> deleteReport(report) }
        )
        
        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportsAdapter
            setHasFixedSize(true)  // Added for better performance
        }
        Log.d(TAG, "RecyclerView setup complete. Adapter item count: ${reportsAdapter.itemCount}")
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
            showMaterialDatePicker(binding.startDateEditText)
        }
        
        binding.endDateEditText.setOnClickListener {
            showMaterialDatePicker(binding.endDateEditText)
        }
    }
    
    private fun showMaterialDatePicker(dateInput: TextInputEditText) {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Select Date")
        
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
            
            // Update the corresponding date variable
            if (dateInput == binding.startDateEditText) {
                startDate = calendar.time
            } else {
                endDate = calendar.time
            }
            
            dateInput.setText(dateFormat.format(calendar.time))
        }
        
        picker.show(parentFragmentManager, picker.toString())
    }
    
    private fun setupTransactionTypeDropdown() {
        val transactionTypes = arrayOf("All", "Incoming", "Outgoing")
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_item, transactionTypes)
        (binding.transactionTypeLayout.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        (binding.transactionTypeLayout.editText as? AutoCompleteTextView)?.setText(transactionTypes[0], false)
    }
    
    private fun setupTransactorDropdown() {
        val transactorAutoCompleteTextView = binding.transactorDropdown
        transactorAutoCompleteTextView.threshold = 1 // Start searching after 1 character

        transactorAutoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    // No input, hide spinner
                    // No need to show loading for empty input
                } else {
                    // Show loading dialog while searching
                    showLoading("Searching transactors...")
                    searchTransactors(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Initialize with "All" as default selection
        transactorAutoCompleteTextView.setText("All", false)
    }

    private fun searchTransactors(query: String) {
        // Perform search in the background
        lifecycleScope.launch(Dispatchers.IO) {
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
                binding.transactorDropdown.setAdapter(adapter)

                // Handle item selection
                binding.transactorDropdown.setOnItemClickListener { parent, _, position, _ ->
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    binding.transactorDropdown.setText(selectedItem, false)
                    
                    // Update selectedTransactor if not "All"
                    selectedTransactor = if (selectedItem == "All") {
                        null
                    } else {
                        transactorMap[selectedItem]
                    }
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
    
    private fun setupApplyFiltersButton() {
        binding.btnApplyFilters.setOnClickListener {
            if (validateFilters()) {
                // Generate a report with the current filters
                generateReport()
            }
        }
    }
    
    private fun validateFilters(): Boolean {
        if (startDate == null) {
            Snackbar.make(binding.root, "Please select a start date", Snackbar.LENGTH_SHORT).show()
            return false
        }
        
        if (endDate == null) {
            Snackbar.make(binding.root, "Please select an end date", Snackbar.LENGTH_SHORT).show()
            return false
        }
        
        if (startDate!!.after(endDate)) {
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
    
    private fun showLoading(message: String = "Loading Transactions...") {
        loadingDialog?.findViewById<TextView>(R.id.loading_text)?.text = message
        loadingDialog?.show()
    }
    
    private fun hideLoading() {
        loadingDialog?.dismiss()
    }
    
    // Called from the ReportsActivity when the FAB is clicked
    fun generateReport() {
        if (!validateFilters()) return
        
        val transactionType = (binding.transactionTypeLayout.editText as? AutoCompleteTextView)?.text.toString()
        val transactor = binding.transactorDropdown.text.toString()
        val transactorFilter = if (transactor == "All") "" else transactor
        
        Log.d(TAG, "Generating M-Pesa statement with filters: Type=$transactionType, Transactor=$transactorFilter, Start=${startDate}, End=${endDate}")
        
        // Show a loading state
        binding.apply {
            btnApplyFilters.isEnabled = false
        }
        
        showLoading("Generating Report...")
        
        lifecycleScope.launch {
            try {
                // Generate the report on a background thread
                val report = withContext(Dispatchers.IO) {
                    generateMpesaStatementReport(
                        transactionType,
                        transactorFilter, 
                        dateFormat.format(startDate!!), 
                        dateFormat.format(endDate!!)
                    )
                }
                
                // Save the report to the database
                withContext(Dispatchers.IO) {
                    dbHelper.saveReport(report)
                }
                
                // Refresh the list of saved reports
                loadSavedReports()
                
                // Show success message
                Snackbar.make(binding.root, "Report generated successfully", Snackbar.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating report: ${e.message}", e)
                Snackbar.make(binding.root, "Failed to generate report: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                withContext(Dispatchers.Main) {
                    binding.btnApplyFilters.isEnabled = true
                    hideLoading()
                }
            }
        }
    }
    
    private fun updateEmptyState() {
        Log.d(TAG, "Updating empty state. Reports count: ${savedReports.size}")
        binding.apply {
            if (savedReports.isEmpty()) {
                Log.d(TAG, "No reports - showing empty state")
                emptyStateContainer.visibility = View.VISIBLE
                recyclerViewReports.visibility = View.GONE
            } else {
                Log.d(TAG, "Has reports - showing RecyclerView")
                emptyStateContainer.visibility = View.GONE
                recyclerViewReports.visibility = View.VISIBLE
            }
        }
    }
    
    fun loadSavedReports() {
        Log.d(TAG, "Starting to load saved reports")
        lifecycleScope.launch {
            try {
                // Show loading state
                withContext(Dispatchers.Main) {
                    showLoading("Loading Reports...")
                    binding.apply {
                        recyclerViewReports.visibility = View.VISIBLE  // Keep RecyclerView visible
                        emptyStateContainer.visibility = View.GONE
                    }
                }
                
                // Ensure database helper is initialized and open
                if (!::dbHelper.isInitialized || dbHelper.readableDatabase?.isOpen == false) {
                    dbHelper = DbHelper(requireContext())
                }
                
                // Load reports from database
                val reports = withContext(Dispatchers.IO) {
                    try {
                        val result = dbHelper.getReportsByType(ReportType.MPESA_STATEMENT)
                        Log.d(TAG, "Loaded ${result?.size ?: 0} reports from database")
                        result
                    } catch (e: Exception) {
                        Log.e(TAG, "Database error: ${e.message}")
                        null
                    }
                }
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    if (reports != null) {
                        savedReports.clear()
                        savedReports.addAll(reports)
                        Log.d(TAG, "Updated savedReports list. New size: ${savedReports.size}")
                        reportsAdapter.notifyDataSetChanged()
                        updateEmptyState()
                    } else {
                        Log.e(TAG, "Reports is null - showing empty state")
                        binding.emptyStateContainer.visibility = View.VISIBLE
                        binding.recyclerViewReports.visibility = View.GONE
                        Snackbar.make(binding.root, "Failed to load saved reports", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved reports: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    binding.recyclerViewReports.visibility = View.GONE
                    Snackbar.make(binding.root, "Failed to load saved reports: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    hideLoading()
                }
            }
        }
    }
    
    private suspend fun generateMpesaStatementReport(
        transactionType: String,
        transactor: String,
        startDate: String,
        endDate: String
    ): Report {
        // Generate a unique report name
        val timestamp = System.currentTimeMillis()
        val reportName = "MpesaStatement_${timestamp}"
        
        // Create directory for reports if it doesn't exist
        val reportsDir = File(requireContext().filesDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        
        // Create PDF file
        val pdfFile = File(reportsDir, "$reportName.pdf")
        
        // Create CSV file instead of Excel
        val csvFile = File(reportsDir, "$reportName.csv")
        
        // Get M-Pesa transactions from database with filters applied at the database level
        Log.d(TAG, "Fetching filtered transactions from database with parameters: Type=$transactionType, Transactor=$transactor, Start=$startDate, End=$endDate")
        val transactions = dbHelper.getFilteredMpesaTransactions(startDate, endDate, transactionType, transactor)
        
        Log.d(TAG, "Found ${transactions.size} transactions matching the filters")
        
        // Generate PDF report
        generatePdfReport(pdfFile, transactions, transactionType, transactor, startDate, endDate)
        
        // Generate CSV report
        generateCsvReport(csvFile, transactions, transactionType, transactor, startDate, endDate)
        
        // Create and return Report object
        return Report(
            id = UUID.randomUUID().toString(),
            name = reportName,
            type = ReportType.MPESA_STATEMENT,
            generatedDate = Date(),
            filePath = pdfFile.absolutePath,
            excelFilePath = csvFile.absolutePath,
            filters = mapOf(
                "transactionType" to transactionType,
                "transactor" to transactor,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
    }
    
    private fun generatePdfReport(
        file: File,
        transactions: List<MpesaTransaction>,
        transactionType: String,
        transactor: String,
        startDate: String,
        endDate: String
    ) {
        // Create a new PDF document
        val document = PdfDocument()
        
        // Page configuration - A4 size in portrait orientation
        // A4 is 210 x 297 mm, at 72 dpi that's approximately 595 x 842 points
        val pageWidth = 842  // Rotate to landscape for more width
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
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
        val logoTop = 0f
        canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), null)
        
        // Draw title
        canvas.drawText("M-PESA STATEMENT", 190f, 50f, titlePaint)
        
        // Draw date range
        canvas.drawText("Period: $startDate to $endDate", 190f, 80f, headerPaint)
        
        // Draw filters
        canvas.drawText("Transaction Type: $transactionType", 190f, 100f, headerPaint)
        canvas.drawText("Transactor: ${if (transactor.isEmpty()) "All" else transactor}", 190f, 120f, headerPaint)
        
        // Margins and column widths
        val leftMargin = 30f
        val dateWidth = 100f
        val codeWidth = 120f
        val typeWidth = 120f
        val transactorWidth = 180f
        val amountWidth = 100f
        val balanceWidth = 110f
        // Add column padding for better spacing
        val columnPadding = 5f
        
        // Draw table headers
        val headerY = 150f
        canvas.drawText("Date", leftMargin, headerY, headerPaint)
        canvas.drawText("Transaction Code", leftMargin + dateWidth + columnPadding, headerY, headerPaint)
        canvas.drawText("Transaction Type", leftMargin + dateWidth + codeWidth + (columnPadding * 2), headerY, headerPaint)
        canvas.drawText("Transactor", leftMargin + dateWidth + codeWidth + typeWidth + (columnPadding * 3), headerY, headerPaint)
        canvas.drawText("Amount (KES)", leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + (columnPadding * 4), headerY, headerPaint)
        canvas.drawText("Balance (KES)", leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + amountWidth + (columnPadding * 5), headerY, headerPaint)
        
        // Draw line under headers
        val linePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawLine(leftMargin, headerY + 5, pageWidth - leftMargin, headerY + 5, linePaint)
        
        // Draw transactions
        var y = headerY + 25
        var totalIncoming = 0.0
        var totalOutgoing = 0.0
        var currentCanvas = canvas
        var currentPage = page
        
        // Maximum width for transactor name before wrapping
        val maxTransactorTextWidth = transactorWidth - 10f
        
        Log.d(TAG, "Starting to draw ${transactions.size} transactions")
        
        for (transaction in transactions) {
            // Check if we need a new page
            if (y > pageHeight - 50) {
                // Finish current page
                document.finishPage(currentPage)
                
                // Create new page
                val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
                currentPage = document.startPage(newPageInfo)
                currentCanvas = currentPage.canvas
                
                // Reset y position
                y = 50f
                
                // Draw headers on new page
                currentCanvas.drawText("Date", leftMargin, y, headerPaint)
                currentCanvas.drawText("Transaction Code", leftMargin + dateWidth + columnPadding, y, headerPaint)
                currentCanvas.drawText("Transaction Type", leftMargin + dateWidth + codeWidth + (columnPadding * 2), y, headerPaint)
                currentCanvas.drawText("Transactor", leftMargin + dateWidth + codeWidth + typeWidth + (columnPadding * 3), y, headerPaint)
                currentCanvas.drawText("Amount (KES)", leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + (columnPadding * 4), y, headerPaint)
                currentCanvas.drawText("Balance (KES)", leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + amountWidth + (columnPadding * 5), y, headerPaint)
                
                // Draw line under headers
                currentCanvas.drawLine(leftMargin, y + 5, pageWidth - leftMargin, y + 5, linePaint)
                
                y += 25
            }
            
            // Get transaction date in a readable format
            val date = try {
                if (transaction.transaction_date?.contains(" ") == true) {
                    val parsed = transactionDateFormat.parse(transaction.transaction_date)
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsed)
                } else {
                    transaction.transaction_date?.split(" ")?.get(0) ?: ""
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting date for PDF: ${transaction.transaction_date}", e)
                transaction.transaction_date?.split(" ")?.get(0) ?: ""
            }
            
            // Get transactor name from transaction
            val transactorName = MpesaTransaction.getTitleTextByTransactionType(transaction)
            
            // Determine if transaction is incoming or outgoing
            val isIncoming = transaction.transaction_type == "Buy Goods" || 
                             transaction.transaction_type == "deposit" ||
                             transaction.transaction_type == "receival"
            
            if (isIncoming) {
                totalIncoming += transaction.amount ?: 0.0
            } else {
                totalOutgoing += transaction.amount ?: 0.0
            }
            
            // Format amount with negative sign for outgoing transactions
            val displayAmount = if (isIncoming) {
                String.format("%.2f", transaction.amount)
            } else {
                String.format("-%.2f", transaction.amount)
            }
            
            // Track the maximum Y position for this row
            var maxYPosition = y
            
            // Draw date
            currentCanvas.drawText(date, leftMargin, y, textPaint)
            
            // Draw transaction code
            currentCanvas.drawText(transaction.mpesa_code ?: "", leftMargin + dateWidth + columnPadding, y, textPaint)
            
            // Draw transaction type
            currentCanvas.drawText(formatTransactionType(transaction.transaction_type ?: ""), 
                leftMargin + dateWidth + codeWidth + (columnPadding * 2), y, textPaint)
            
            // Handle long transactor names by splitting into multiple lines if needed
            val transactorTextWidth = textPaint.measureText(transactorName)
            val transactorX = leftMargin + dateWidth + codeWidth + typeWidth + (columnPadding * 3)
            
            if (transactorTextWidth > maxTransactorTextWidth) {
                // Split the transactor name into words
                val words = transactorName.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = StringBuilder()
                
                // Group words into lines that fit within maxTransactorTextWidth
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                    if (textPaint.measureText(testLine) <= maxTransactorTextWidth) {
                        currentLine = StringBuilder(testLine)
                    } else {
                        if (currentLine.isNotEmpty()) {
                            lines.add(currentLine.toString())
                            currentLine = StringBuilder(word)
                        } else {
                            // If a single word is too long, force it on its own line
                            lines.add(word)
                        }
                    }
                }
                
                // Add the last line if not empty
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                
                // Draw each line
                var lineY = y
                for (line in lines) {
                    currentCanvas.drawText(line, transactorX, lineY, textPaint)
                    lineY += 15f // Line spacing
                }
                
                // Update maxYPosition to the position after the last line
                maxYPosition = lineY - 15f // Subtract one line spacing to get to the last line's position
            } else {
                // Draw transactor name on a single line
                currentCanvas.drawText(transactorName, transactorX, y, textPaint)
            }
            
            // Draw amount and balance using the initial y position (first line) instead of maxYPosition
            currentCanvas.drawText(displayAmount, 
                leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + (columnPadding * 4), 
                y, textPaint)
                
            currentCanvas.drawText(String.format("%.2f", transaction.mpesaBalance), 
                leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + amountWidth + (columnPadding * 5), 
                y, textPaint)
            
            // Move to the next row, adding some spacing
            y = maxYPosition + 20
        }
        
        // Check if there's enough space for totals and footer
        if (y + 120 > pageHeight - 50f) {  // 120 = space needed for totals and footer
            // Not enough space, create new page
            document.finishPage(currentPage)
            val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
            currentPage = document.startPage(newPageInfo)
            currentCanvas = currentPage.canvas
            y = 50f  // Reset y to top of new page with some margin
        }
        
        // Draw total
        y += 10
        currentCanvas.drawLine(leftMargin, y, pageWidth - leftMargin, y, linePaint)
        y += 20
        currentCanvas.drawText("Total Incoming", leftMargin + dateWidth + codeWidth + (columnPadding * 2), y, headerPaint)
        currentCanvas.drawText(String.format("%.2f", totalIncoming), 
            leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + (columnPadding * 4), y, headerPaint)
        y += 20
        currentCanvas.drawText("Total Outgoing", leftMargin + dateWidth + codeWidth + (columnPadding * 2), y, headerPaint)
        currentCanvas.drawText(String.format("-%.2f", totalOutgoing), 
            leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + (columnPadding * 4), y, headerPaint)
        y += 20
        currentCanvas.drawText("Net", leftMargin + dateWidth + codeWidth + (columnPadding * 2), y, headerPaint)
        currentCanvas.drawText(String.format("%.2f", totalIncoming - totalOutgoing), 
            leftMargin + dateWidth + codeWidth + typeWidth + transactorWidth + (columnPadding * 4), y, headerPaint)
        
        // Draw footer right below totals
        y += 30  // Add some space between totals and footer
        currentCanvas.drawText("This is a computer-generated statement and does not require a signature.", leftMargin, y, textPaint)
        
        // Finish page and document
        document.finishPage(currentPage)
        
        // Write to file
        try {
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            Log.d(TAG, "PDF generated successfully at ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing PDF", e)
            throw e
        }
    }
    
    /**
     * Formats transaction type strings for better readability
     * Converts "send_money" to "Send Money", "till" to "Till", etc.
     */
    private fun formatTransactionType(transactionType: String): String {
        return when (transactionType.lowercase()) {
            "send_money" -> "Send Money"
            "buy goods" -> "Buy Goods"
            "paybill" -> "Paybill"
            "till" -> "Till"
            "deposit" -> "Deposit"
            "withdraw" -> "Withdraw"
            "receival" -> "Receival"
            "topup" -> "Top Up"
            else -> transactionType.split("_").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
    }
    
    private fun getBitmapFromDrawable(drawableId: Int): Bitmap? {
        try {
            val drawable = resources.getDrawable(drawableId, null)
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error creating bitmap from drawable: ${e.message}")
            return null
        }
    }
    
    private fun openReport(report: Report) {
        try {
            val pdfFile = File(report.filePath)
            val csvFileExists = report.excelFilePath != null && File(report.excelFilePath).exists()
            
            // If PDF doesn't exist and CSV doesn't exist, show error
            if (!pdfFile.exists() && !csvFileExists) {
                Snackbar.make(binding.root, "Report files not found", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            // If only one format exists, open that directly
            if (!pdfFile.exists()) {
                openCSVReport(report)
                return
            } else if (!csvFileExists) {
                openPDFReport(report)
                return
            }
            
            // Both formats exist, show dialog to choose
            val materialAlertDialogBuilder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("View Report")
                .setMessage("Choose the format you want to view")
                .setPositiveButton("PDF") { _, _ ->
                    openPDFReport(report)
                }
                .setNegativeButton("CSV") { _, _ ->
                    openCSVReport(report)
                }
                .setNeutralButton("Cancel", null)
            
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
            val sourceFile = File(report.filePath)
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
                val uri = Uri.fromFile(destFile)
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = uri
                requireContext().sendBroadcast(mediaScanIntent)
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
            val uri = Uri.fromFile(destFile)
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = uri
            requireContext().sendBroadcast(mediaScanIntent)
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
                
                startActivity(Intent.createChooser(shareIntent, "Share M-Pesa Statement (PDF)"))
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
                
                startActivity(Intent.createChooser(shareIntent, "Share M-Pesa Statement (CSV)"))
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
                        
                        // Remove the report from the list and update the adapter ONLY after successful deletion
                        withContext(Dispatchers.Main) {
                            val position = savedReports.indexOf(report)
                            if (position != -1) {
                                savedReports.removeAt(position)
                                reportsAdapter.notifyItemRemoved(position)
                                
                                // Update empty state
                                updateEmptyState()
                            }
                            
                            // Show success message
                            Snackbar.make(binding.root, "Report deleted successfully", Snackbar.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting report: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Snackbar.make(binding.root, "Failed to delete report: ${e.message}", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
        
        // Show the Material 3 dialog
        materialAlertDialogBuilder.show()
    }
    
    /**
     * Generates a CSV report for M-Pesa transactions
     */
    private fun generateCsvReport(
        file: File,
        transactions: List<MpesaTransaction>,
        transactionType: String,
        transactor: String,
        startDate: String,
        endDate: String
    ) {
        try {
            val writer = file.bufferedWriter()
            
            // Write header information
            writer.write("M-PESA STATEMENT")
            writer.newLine()
            writer.write("Period: $startDate to $endDate")
            writer.newLine()
            writer.write("Transaction Type: $transactionType")
            writer.newLine()
            writer.write("Transactor: ${if (transactor.isEmpty()) "All" else transactor}")
            writer.newLine()
            writer.newLine()
            
            // Write column headers
            writer.write("Date,Transaction Code,Transaction Type,Transactor,Amount (KES),Balance (KES)")
            writer.newLine()
            
            // Write transaction data
            var totalIncoming = 0.0
            var totalOutgoing = 0.0
            
            for (transaction in transactions) {
                // Get transaction date in a readable format
                val date = try {
                    if (transaction.transaction_date?.contains(" ") == true) {
                        val parsed = transactionDateFormat.parse(transaction.transaction_date)
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsed)
                    } else {
                        transaction.transaction_date?.split(" ")?.get(0) ?: ""
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error formatting date for CSV: ${transaction.transaction_date}", e)
                    transaction.transaction_date?.split(" ")?.get(0) ?: ""
                }
                
                // Get transactor name from transaction
                val transactorName = MpesaTransaction.getTitleTextByTransactionType(transaction)
                
                // Determine if transaction is incoming or outgoing
                val isIncoming = transaction.transaction_type == "Buy Goods" || 
                                 transaction.transaction_type == "deposit" ||
                                 transaction.transaction_type == "receival"
                
                if (isIncoming) {
                    totalIncoming += transaction.amount ?: 0.0
                } else {
                    totalOutgoing += transaction.amount ?: 0.0
                }
                
                // Escape any commas in text fields
                val escapedTransactorName = transactorName.replace(",", " ")
                val escapedTransactionType = formatTransactionType(transaction.transaction_type ?: "").replace(",", " ")
                
                // Write transaction row
                writer.write("$date,${transaction.mpesa_code ?: ""},$escapedTransactionType,$escapedTransactorName,${String.format("%.2f", transaction.amount)},${String.format("%.2f", transaction.mpesaBalance)}")
                writer.newLine()
            }
            
            // Write totals
            writer.newLine()
            writer.write(",,Total Incoming,,$totalIncoming,")
            writer.newLine()
            writer.write(",,Total Outgoing,,$totalOutgoing,")
            writer.newLine()
            writer.write(",,Net,,${ totalIncoming - totalOutgoing },")
            writer.newLine()
            writer.newLine()
            writer.write("This is a computer-generated statement and does not require a signature.")
            
            writer.close()
            
            Log.d(TAG, "CSV generated successfully at ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating CSV", e)
            throw e
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.dismiss()
        loadingDialog = null
        try {
            if (::dbHelper.isInitialized) {
                dbHelper.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database: ${e.message}")
        }
        _binding = null
    }
}