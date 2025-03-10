package com.example.pettysms.reports

import android.content.Intent
import android.graphics.Bitmap
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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.BuildConfig
import com.example.pettysms.DbHelper
import com.example.pettysms.Owner
import com.example.pettysms.PettyCash
import com.example.pettysms.PettyCashViewerActivity
import com.example.pettysms.R
import com.example.pettysms.databinding.FragmentPettyCashCopiesBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
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

class PettyCashCopiesFragment : Fragment() {
    
    private var _binding: FragmentPettyCashCopiesBinding? = null
    private val binding get() = _binding!!
    
    private val TAG = "PettyCashCopiesFragment"
    private val savedCopies = mutableListOf<Report>()
    private lateinit var copiesAdapter: PettyCashCopiesAdapter
    
    private val calendar = Calendar.getInstance()
    private var startDate: Date = calendar.time
    private var endDate: Date = calendar.time
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    private lateinit var dbHelper: DbHelper
    private var foundPettyCashList = mutableListOf<PettyCash>()
    
    private var selectedOwnerId: String? = null
    private var selectedOwnerObject: Owner? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPettyCashCopiesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        dbHelper = DbHelper(requireContext())
        
        setupTabLayout()
        setupDatePickers()
        setupSearchButtons()
        setupRecyclerView()
        setupAutocomplete()
        setupOwnerDropdown()
        
        // For placeholder purposes, we'll show a message
        updateEmptyState()
        
        // Load existing reports from database
        loadSavedReports()
    }
    
    private fun setupRecyclerView() {
        copiesAdapter = PettyCashCopiesAdapter(savedCopies)
        binding.recyclerViewCopies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = copiesAdapter
        }
    }
    
    private fun setupAutocomplete() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pettyCashNumbers = dbHelper.getAllPettyCashNumbers()
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    pettyCashNumbers
                )
                
                val autoCompleteTextView = binding.pettyCashNumberEditText
                autoCompleteTextView.setAdapter(adapter)
                
                // Set item click listener
                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val pettyCashNumber = parent.getItemAtPosition(position) as String
                    autoCompleteTextView.setText(pettyCashNumber)
                    searchByPettyCashNumber(pettyCashNumber)
                }
            }
        }
    }
    
    private fun setupTabLayout() {
        binding.searchTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Search by Number
                        binding.searchByNumberContainer.visibility = View.VISIBLE
                        binding.searchByDateContainer.visibility = View.GONE
                    }
                    1 -> { // Search by Date
                        binding.searchByNumberContainer.visibility = View.GONE
                        binding.searchByDateContainer.visibility = View.VISIBLE
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
    
    private fun setupSearchButtons() {
        binding.btnSearchByNumber.setOnClickListener {
            val pettyCashNumber = binding.pettyCashNumberEditText.text.toString()
            if (pettyCashNumber.isNotEmpty()) {
                searchByPettyCashNumber(pettyCashNumber)
            } else {
                Snackbar.make(binding.root, "Please enter a Petty Cash number", Snackbar.LENGTH_SHORT).show()
            }
        }
        
        binding.btnSearchByDate.setOnClickListener {
            searchByDateRange()
        }
    }
    
    private fun searchByPettyCashNumber(pettyCashNumber: String) {
        Log.d(TAG, "Searching for Petty Cash #$pettyCashNumber")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pettyCash = dbHelper.getPettyCashByPettyCashNumber(pettyCashNumber)
                
                withContext(Dispatchers.Main) {
                    foundPettyCashList.clear()
                    if (pettyCash != null) {
                        foundPettyCashList.add(pettyCash)
                        showPettyCashFoundDialog(pettyCash)
                    } else {
                        Snackbar.make(binding.root, "No Petty Cash found with number: $pettyCashNumber", Snackbar.LENGTH_SHORT).show()
                        updateEmptyState()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching by petty cash number: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error searching for Petty Cash: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    updateEmptyState()
                }
            }
        }
    }
    
    private fun searchByDateRange() {
        val startDateStr = dateFormat.format(startDate)
        val endDateStr = dateFormat.format(endDate)
        
        Log.d(TAG, "Searching for Petty Cash copies between $startDateStr and $endDateStr" + 
              (selectedOwnerId?.let { " for owner ID: $it" } ?: " for all owners"))
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val startDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(startDate)
                val endDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(endDate)
                
                val pettyCashList = if (selectedOwnerId != null) {
                    dbHelper.getPettyCashByDateRangeAndOwner(startDateFormatted, endDateFormatted, selectedOwnerId!!)
                } else {
                    dbHelper.getPettyCashByDateRange(startDateFormatted, endDateFormatted)
                }
                
                withContext(Dispatchers.Main) {
                    foundPettyCashList.clear()
                    if (pettyCashList.isNotEmpty()) {
                        foundPettyCashList.addAll(pettyCashList)
                        Snackbar.make(binding.root, "Found ${pettyCashList.size} Petty Cash entries", Snackbar.LENGTH_SHORT).show()
                        showDateRangeResultsDialog(pettyCashList)
                    } else {
                        Snackbar.make(binding.root, "No Petty Cash found in the selected date range", Snackbar.LENGTH_SHORT).show()
                        updateEmptyState()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching by date range: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error searching by date range: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    updateEmptyState()
                }
            }
        }
    }
    
    private fun showPettyCashFoundDialog(pettyCash: PettyCash) {
        val options = arrayOf("View", "Generate Simplified PDF", "Generate Detailed PDF", "Cancel")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Petty Cash ${pettyCash.pettyCashNumber}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> viewPettyCash(pettyCash)
                    1 -> generatePettyCashPdf(pettyCash, isSimplified = true)
                    2 -> generatePettyCashPdf(pettyCash, isSimplified = false)
                    3 -> dialog.dismiss()
                }
            }
            .show()
    }
    
    private fun showDateRangeResultsDialog(pettyCashList: List<PettyCash>) {
        val options = arrayOf("Generate Simplified PDF", "Generate Detailed PDF", "Cancel")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Found ${pettyCashList.size} Petty Cash Entries")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> generateMultiplePettyCashPdf(pettyCashList, isSimplified = true)
                    1 -> generateMultiplePettyCashPdf(pettyCashList, isSimplified = false)
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }
    
    private fun viewPettyCash(pettyCash: PettyCash) {
        val intent = Intent(requireContext(), PettyCashViewerActivity::class.java).apply {
            putExtra("petty_cash_number", pettyCash.pettyCashNumber)
        }
        startActivity(intent)
    }
    
    private fun generatePettyCashPdf(pettyCash: PettyCash, isSimplified: Boolean) {
        val includeSupportingDocs = when (binding.searchTabLayout.selectedTabPosition) {
            0 -> binding.supportingDocumentSwitchNumber.isChecked
            1 -> binding.supportingDocumentSwitchDate.isChecked
            else -> true
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val reportFile = if (isSimplified) {
                    generateSimplifiedPdf(listOf(pettyCash), includeSupportingDocs)
                } else {
                    generateDetailedPdf(listOf(pettyCash), includeSupportingDocs)
                }
                
                if (reportFile != null) {
                    // Save the report to database
                    val reportId = UUID.randomUUID().toString()
                    val reportName = "${pettyCash.owner?.name} ${pettyCash.pettyCashNumber} ${if (isSimplified) "Simplified" else "Detailed"}"
                    val report = Report(
                        id = reportId,
                        name = reportName,
                        type = ReportType.PETTY_CASH_COPY,
                        generatedDate = Date(),
                        filePath = reportFile.absolutePath,
                        excelFilePath = null,
                        filters = mapOf(
                            "pettyCashNumber" to (pettyCash.pettyCashNumber ?: ""),
                            "reportType" to (if (isSimplified) "Simplified" else "Detailed")
                        )
                    )
                    
                    // Add to local list
                    savedCopies.add(0, report)
                    
                    // Save the report to database
                    dbHelper.saveReport(report)
                    
                    withContext(Dispatchers.Main) {
                        updateReportsList()
                        showPdfOptionsDialog(reportFile, report)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "Failed to generate PDF", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error generating PDF: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun generateMultiplePettyCashPdf(pettyCashList: List<PettyCash>, isSimplified: Boolean) {
        val includeSupportingDocs = when (binding.searchTabLayout.selectedTabPosition) {
            0 -> binding.supportingDocumentSwitchNumber.isChecked
            1 -> binding.supportingDocumentSwitchDate.isChecked
            else -> true
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val reportFile = if (isSimplified) {
                    generateSimplifiedPdf(pettyCashList, includeSupportingDocs)
                } else {
                    generateDetailedPdf(pettyCashList, includeSupportingDocs)
                }
                
                if (reportFile != null) {
                    // Save the report to database
                    val reportId = UUID.randomUUID().toString()
                    val startDateStr = dateFormat.format(startDate)
                    val endDateStr = dateFormat.format(endDate)
                    val reportName = "${selectedOwnerObject?.name} ${startDateStr} - ${endDateStr} ${if (isSimplified) "Simplified" else "Detailed"}"
                    
                    val report = Report(
                        id = reportId,
                        name = reportName,
                        type = ReportType.PETTY_CASH_COPY,
                        generatedDate = Date(),
                        filePath = reportFile.absolutePath,
                        excelFilePath = null,
                        filters = mapOf(
                            "startDate" to startDateStr,
                            "endDate" to endDateStr,
                            "reportType" to (if (isSimplified) "Simplified" else "Detailed"),
                            "count" to pettyCashList.size.toString()
                        )
                    )
                    
                    // Add to local list
                    savedCopies.add(0, report)
                    
                    // Save the report to database
                    dbHelper.saveReport(report)
                    
                    withContext(Dispatchers.Main) {
                        updateReportsList()
                        showPdfOptionsDialog(reportFile, report)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "Failed to generate PDF", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error generating PDF: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showPdfOptionsDialog(file: File, report: Report) {
        val options = arrayOf("View", "Share", "Download")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("PDF Generated Successfully")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> viewPdf(file)
                    1 -> sharePdf(file)
                    2 -> downloadPdf(file, report.name)
                }
            }
            .show()
    }
    
    private fun viewPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error viewing PDF: ${e.message}", e)
            Snackbar.make(binding.root, "Error viewing PDF: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun sharePdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing PDF: ${e.message}", e)
            Snackbar.make(binding.root, "Error sharing PDF: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun downloadPdf(file: File, fileName: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDir, "${fileName}.pdf")
            
            file.copyTo(destinationFile, overwrite = true)
            
            Snackbar.make(binding.root, "PDF saved to Downloads folder", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading PDF: ${e.message}", e)
            Snackbar.make(binding.root, "Error downloading PDF: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun generateSimplifiedPdf(pettyCashList: List<PettyCash>, includeSupportingDocs: Boolean): File? {
        if (pettyCashList.isEmpty()) return null
        
        try {
            // Initialize the PdfDocument
            val pdf = android.graphics.pdf.PdfDocument()
            val pageWidth = 600
            val pageHeight = 800
            
            // Process each petty cash in the list
            pettyCashList.forEachIndexed { index, pettyCash ->
                // Create a page for each petty cash
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas
                val paint = android.graphics.Paint()
                
                // Draw company logo and details
                var logo: Bitmap? = null
                try {
                    logo = if (pettyCash.owner?.logoPath != null) {
                        val decodedString = android.util.Base64.decode(pettyCash.owner?.logoPath, android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    } else {
                        android.graphics.BitmapFactory.decodeResource(resources, R.drawable.p_logo_cropped)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading logo: ${e.message}", e)
                    // Try to load default logo as fallback
                    try {
                        logo = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.p_logo_cropped)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Error loading default logo: ${e2.message}", e2)
                    }
                }
                
                // If we have a valid logo, draw it
                if (logo != null) {
                    // Get the original width and height of the logo
                    val originalWidth = logo.width
                    val originalHeight = logo.height
                    
                    // Define the maximum width and height for the scaled image
                    val maxWidth = 90
                    val maxHeight = 100
                    
                    // Calculate the aspect ratio of the original image
                    val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
                    
                    // Calculate the new dimensions maintaining the aspect ratio
                    var scaledWidth = maxWidth
                    var scaledHeight = (scaledWidth / aspectRatio).toInt()
                    
                    // If the scaled height exceeds the maximum height, scale it by height instead
                    if (scaledHeight > maxHeight) {
                        scaledHeight = maxHeight
                        scaledWidth = (scaledHeight * aspectRatio).toInt()
                    }
                    
                    // Scale the image to the new dimensions while maintaining the aspect ratio
                    val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logo, scaledWidth, scaledHeight, true)
                    
                    canvas.drawBitmap(scaledLogo, 25f, 25f, paint)
                }
                
                // Set paint attributes for text
                paint.textSize = 18f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText(pettyCash.owner?.name?.uppercase(java.util.Locale.getDefault()).orEmpty(), 160f, 75f, paint)
                
                // Draw voucher title
                paint.textSize = 16f
                // Calculate position to center the text
                val text = "PETTY CASH VOUCHER"
                val textWidth = paint.measureText(text)
                val xPos = (canvas.width - textWidth) / 2
                val yPos = 130f
                
                // Draw the text centered
                canvas.drawText(text, xPos, yPos, paint)
                
                // Draw petty cash fields
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT
                canvas.drawText("No.: ", 20f, 160f, paint)
                canvas.drawText("Date: ", 420f, 160f, paint)
                canvas.drawText("Name: ", 20f, 190f, paint)
                
                // Draw petty cash fields with values
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                canvas.drawText(pettyCash.pettyCashNumber.orEmpty(), 50f, 160f, paint)
                canvas.drawText(pettyCash.date.orEmpty(), 455f, 160f, paint)
                canvas.drawText(capitalizeEachWord(pettyCash.transactor?.name.toString()).orEmpty(), 60f, 190f, paint)
                
                // Draw petty cash fields
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT
                
                // Draw table headers
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1f
                val cornerRadius = 5f
                canvas.drawRoundRect(20f, 220f, pageWidth - 20f, 320f, cornerRadius, cornerRadius, paint)
                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawText("Details of payments (For What's Required)", 30f, 240f, paint)
                
                // Draw content inside the table
                paint.textSize = 12f
                canvas.drawText(pettyCash.description.orEmpty(), 30f, 270f, paint)
                
                // Draw amount section
                canvas.drawText("Amount in words: ${convertAmountToWords(pettyCash.amount ?: 0.0)}", 20f, 350f, paint)
                canvas.drawText("Total: ${"%.2f".format(pettyCash.amount ?: 0.0)}", 450f, 350f, paint)
                
                // Draw signature section
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT
                
                // Checked by
                canvas.drawText("Checked by: ______________________", 20f, 400f, paint)
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                canvas.drawText(pettyCash.user?.name.orEmpty(), 130f, 395f, paint)
                
                // Draw signature section
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT
                
                // Authorized by
                canvas.drawText("Authorized by: ___________________", 20f, 430f, paint)
                canvas.drawText("A/C Code: _________________________", 390f, 430f, paint)
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                canvas.drawText(pettyCash.user?.name.orEmpty(), 130f, 425f, paint)
                canvas.drawText(pettyCash.account?.name.orEmpty(), 460f, 425f, paint)
                
                // Draw signature section
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT
                
                // Signature of Recipient
                canvas.drawText("Signature of Recipient: ___________________________________________", 20f, 550f, paint)
                
                // Add signature or Mpesa code
                if (pettyCash.paymentMode == "Cash") {
                    pettyCash.signature?.let {
                        try {
                            val decodedString = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                            val signatureBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            
                            signatureBitmap?.let {
                                val upscaledSignature = android.graphics.Bitmap.createScaledBitmap(it, it.width * 5, it.height * 5, true)
                                val scaledSignature = android.graphics.Bitmap.createScaledBitmap(upscaledSignature, 200, 100, true)
                                canvas.drawBitmap(scaledSignature, 180f, 465f, paint)
                            } ?: run {
                                paint.textSize = 12f
                                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                                canvas.drawText("No signature available", 200f, 545f, paint)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error drawing signature: ${e.message}", e)
                            paint.textSize = 12f
                            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            canvas.drawText("No signature available", 200f, 545f, paint)
                        }
                    } ?: run {
                        paint.textSize = 12f
                        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                        canvas.drawText("No signature available", 200f, 545f, paint)
                    }
                } else {
                    paint.textSize = 12f
                    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                    canvas.drawText("Mpesa Code: ${pettyCash.mpesaTransaction?.mpesa_code.orEmpty()}", 180f, 545f, paint)
                }
                
                // Finish the page
                pdf.finishPage(page)
                
                // Add supporting document images if requested
                if (includeSupportingDocs && pettyCash.supportingDocument != null) {
                    val imagePath1 = pettyCash.supportingDocument?.imagePath1
                    val imagePath2 = pettyCash.supportingDocument?.imagePath2
                    val imagePath3 = pettyCash.supportingDocument?.imagePath3
                    
                    // Add each image to a new page if it exists
                    imagePath1?.let {
                        val imageFile1 = File(it)
                        if (imageFile1.exists()) {
                            try {
                                val image1Bitmap = android.graphics.BitmapFactory.decodeFile(imageFile1.absolutePath)
                                if (image1Bitmap != null) {
                                    // Create a new page for the first image
                                    val pageInfo1 = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.pages.size + 1).create()
                                    val page1 = pdf.startPage(pageInfo1)
                                    val canvas1 = page1.canvas
                                    val paint1 = android.graphics.Paint()
                                    
                                    // Draw the title at the top of the page
                                    val titlePaint = android.graphics.Paint()
                                    titlePaint.textSize = 22f
                                    titlePaint.color = android.graphics.Color.BLACK
                                    canvas1.drawText("${pettyCash.pettyCashNumber} Supporting Document Image 1", 20f, 40f, titlePaint)
                                    
                                    // Resizing the image to fit the page while keeping aspect ratio
                                    val margin = 40f  // Equal margin from all edges
                                    val imageWidth = pageWidth - 2 * margin
                                    val imageHeight = (image1Bitmap.height.toFloat() / image1Bitmap.width.toFloat()) * imageWidth
                                    
                                    // Positioning the image below the title with a small margin
                                    val imageTopPosition = 60f
                                    
                                    // Define the rectangle for the image with rounded corners
                                    val rectF = android.graphics.RectF(
                                        margin,
                                        imageTopPosition,
                                        margin + imageWidth,
                                        imageTopPosition + imageHeight
                                    )
                                    
                                    // Create a rounded bitmap with the radius for all corners
                                    val roundedBitmap = getRoundedBitmap(image1Bitmap, 20f)
                                    
                                    // Draw the rounded image with the defined position
                                    canvas1.drawBitmap(roundedBitmap, null, rectF, paint)
                                    
                                    // Finish the page
                                    pdf.finishPage(page1)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error adding supporting document image 1: ${e.message}", e)
                            }
                        }
                    }
                    
                    imagePath2?.let {
                        val imageFile2 = File(it)
                        if (imageFile2.exists()) {
                            try {
                                val image2Bitmap = android.graphics.BitmapFactory.decodeFile(imageFile2.absolutePath)
                                if (image2Bitmap != null) {
                                    // Create a new page for the second image
                                    val pageInfo2 = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.pages.size + 1).create()
                                    val page2 = pdf.startPage(pageInfo2)
                                    val canvas2 = page2.canvas
                                    val paint2 = android.graphics.Paint()
                                    
                                    // Draw the title at the top of the page
                                    val titlePaint = android.graphics.Paint()
                                    titlePaint.textSize = 22f
                                    titlePaint.color = android.graphics.Color.BLACK
                                    canvas2.drawText("${pettyCash.pettyCashNumber} Supporting Document Image 2", 20f, 40f, titlePaint)
                                    
                                    // Resizing the image to fit the page while keeping aspect ratio
                                    val margin = 40f  // Equal margin from all edges
                                    val imageWidth = pageWidth - 2 * margin
                                    val imageHeight = (image2Bitmap.height.toFloat() / image2Bitmap.width.toFloat()) * imageWidth
                                    
                                    // Positioning the image below the title with a small margin
                                    val imageTopPosition = 60f
                                    
                                    // Define the rectangle for the image with rounded corners
                                    val rectF = android.graphics.RectF(
                                        margin,
                                        imageTopPosition,
                                        margin + imageWidth,
                                        imageTopPosition + imageHeight
                                    )
                                    
                                    // Create a rounded bitmap with the radius for all corners
                                    val roundedBitmap = getRoundedBitmap(image2Bitmap, 20f)
                                    
                                    // Draw the rounded image with the defined position
                                    canvas2.drawBitmap(roundedBitmap, null, rectF, paint)
                                    
                                    // Finish the page
                                    pdf.finishPage(page2)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error adding supporting document image 2: ${e.message}", e)
                            }
                        }
                    }
                    
                    imagePath3?.let {
                        val imageFile3 = File(it)
                        if (imageFile3.exists()) {
                            try {
                                val image3Bitmap = android.graphics.BitmapFactory.decodeFile(imageFile3.absolutePath)
                                if (image3Bitmap != null) {
                                    // Create a new page for the third image
                                    val pageInfo3 = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.pages.size + 1).create()
                                    val page3 = pdf.startPage(pageInfo3)
                                    val canvas3 = page3.canvas
                                    val paint3 = android.graphics.Paint()
                                    
                                    // Draw the title at the top of the page
                                    val titlePaint = android.graphics.Paint()
                                    titlePaint.textSize = 22f
                                    titlePaint.color = android.graphics.Color.BLACK
                                    canvas3.drawText("${pettyCash.pettyCashNumber} Supporting Document Image 3", 20f, 40f, titlePaint)
                                    
                                    // Resizing the image to fit the page while keeping aspect ratio
                                    val margin = 40f  // Equal margin from all edges
                                    val imageWidth = pageWidth - 2 * margin
                                    val imageHeight = (image3Bitmap.height.toFloat() / image3Bitmap.width.toFloat()) * imageWidth
                                    
                                    // Positioning the image below the title with a small margin
                                    val imageTopPosition = 60f
                                    
                                    // Define the rectangle for the image with rounded corners
                                    val rectF = android.graphics.RectF(
                                        margin,
                                        imageTopPosition,
                                        margin + imageWidth,
                                        imageTopPosition + imageHeight
                                    )
                                    
                                    // Create a rounded bitmap with the radius for all corners
                                    val roundedBitmap = getRoundedBitmap(image3Bitmap, 20f)
                                    
                                    // Draw the rounded image with the defined position
                                    canvas3.drawBitmap(roundedBitmap, null, rectF, paint)
                                    
                                    // Finish the page
                                    pdf.finishPage(page3)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error adding supporting document image 3: ${e.message}", e)
                            }
                        }
                    }
                }
            }
            
            // Generate a file name based on the content
            val fileName = if (pettyCashList.size == 1) {
                "PettyCash_${pettyCashList[0].pettyCashNumber?.replace("/", "_").orEmpty()}_Simplified_${System.currentTimeMillis()}.pdf"
            } else {
                "PettyCash_Multiple_Simplified_${System.currentTimeMillis()}.pdf"
            }
            
            // Save PDF to file
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            pdf.writeTo(FileOutputStream(file))
            pdf.close()
            
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating simplified PDF: ${e.message}", e)
            return null
        }
    }
    
    private fun generateDetailedPdf(pettyCashList: List<PettyCash>, includeSupportingDocs: Boolean): File? {
        if (pettyCashList.isEmpty()) return null
        
        try {
            // Initialize PDF document
            val pdf = android.graphics.pdf.PdfDocument()
            val pageWidth = 400
            val pageHeight = 500
            var pageIndex = 1
            
            // Process each petty cash in the list
            pettyCashList.forEach { pettyCash ->
                // Create the first page for this petty cash
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas
                val paint = android.graphics.Paint()
                
                paint.color = android.graphics.Color.BLACK
                
                // Add gradient background
                val colors = intArrayOf(
                    android.graphics.Color.parseColor(pettyCash.transactor?.avatarColor ?: "#000000"),
                    android.graphics.Color.WHITE
                )
                val gradient = android.graphics.LinearGradient(
                    0f, -200f, 0f, page.info.pageHeight.toFloat(),
                    colors, null, android.graphics.Shader.TileMode.CLAMP
                )
                
                paint.shader = gradient
                canvas.drawRect(0f, 0f, page.info.pageWidth.toFloat(), page.info.pageHeight.toFloat(), paint)
                paint.shader = null // Reset shader
                
                // Add company logo and title
                val logoBitmap = android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_p_logo_foreground)
                val logoWidth = 100f
                val logoHeight = logoWidth * logoBitmap.height / logoBitmap.width
                canvas.drawBitmap(logoBitmap, null, android.graphics.RectF(10f, 10f, 20f + logoWidth, 20f + logoHeight), paint)
                
                paint.textSize = 11.5f
                paint.isFakeBoldText = true
                pettyCash.owner?.name?.uppercase(java.util.Locale.ROOT)
                    ?.let { canvas.drawText(it, 115f, 50f, paint) }
                paint.textSize = 11.5f
                canvas.drawText("PETTY CASH VOUCHER", 135f, 90f, paint)
                paint.isFakeBoldText = true
                
                var yPosition = 125f
                paint.textSize = 11f
                
                // Add Basic Information Section
                canvas.drawText("Basic Information", 20f, yPosition, paint)
                yPosition += 30f
                paint.isFakeBoldText = false
                paint.textSize = 10f
                
                val mpesaTransaction = pettyCash.mpesaTransaction?.mpesa_code ?: "N/A"
                
                val basicInfo = mapOf(
                    "Petty Cash Number" to pettyCash.pettyCashNumber?.toString(),
                    "Date" to pettyCash.date?.toString(),
                    "Amount" to pettyCash.amount?.toString(),
                    "User" to pettyCash.user?.name?.toString(),
                    "Account" to pettyCash.account?.name?.toString(),
                    "Payment Mode" to pettyCash.paymentMode?.toString(),
                    "Description" to pettyCash.description?.toString(),
                    "Mpesa Transaction" to mpesaTransaction,
                )
                drawTableWithBorders(basicInfo, canvas, paint, 20f, yPosition - 10f, pageWidth - 40f)
                yPosition += basicInfo.size * 30f
                
                paint.isFakeBoldText = true
                paint.textSize = 11f
                
                // Signature Section
                pettyCash.signature?.let { signatureBase64 ->
                    canvas.drawText("Signature", 20f, yPosition - 20f, paint)
                    yPosition += 20f
                    
                    // Draw rounded rectangle for signature
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    val rectHeight = 40f * 2 // Double the height
                    canvas.drawRoundRect(20f, yPosition - 30f, pageWidth - 20f, yPosition + rectHeight, 10f, 10f, paint)
                    paint.style = android.graphics.Paint.Style.FILL
                    
                    // Decode and draw the signature image
                    val decodedBytes = android.util.Base64.decode(signatureBase64, android.util.Base64.DEFAULT)
                    val signatureBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    signatureBitmap?.let {
                        val reducedHeight = 50f // Adjust the height
                        val reducedWidthPadding = 80f // Increase padding to reduce width
                        canvas.drawBitmap(
                            it,
                            null,
                            android.graphics.RectF(
                                reducedWidthPadding, // Left
                                yPosition - 10f, // Top
                                pageWidth - reducedWidthPadding, // Right
                                yPosition + reducedHeight + 30f // Bottom
                            ),
                            paint
                        )
                    }
                    
                    yPosition += 90f // Adjust yPosition to account for the doubled height and some spacing
                }
                
                paint.isFakeBoldText = false
                paint.textSize = 10f
                
                // Add owner's logo from Base64 to top right corner
                pettyCash.owner?.logoPath?.let { logoBase64 ->
                    val decodedLogoBytes = android.util.Base64.decode(logoBase64, android.util.Base64.DEFAULT)
                    val logoBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedLogoBytes, 0, decodedLogoBytes.size)
                    logoBitmap?.let {
                        val logoWidth = 50f
                        val logoHeight = logoWidth * it.height / it.width
                        val logoX = pageWidth - logoWidth - 30f // 10f margin from the right edge
                        val logoY = 35f // 10f margin from the top edge
                        canvas.drawBitmap(it, null, android.graphics.RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight), paint)
                    }
                }
                
                pdf.finishPage(page)
                
                // Create a new page for the transactor info
                val pageInfo2 = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
                val page2 = pdf.startPage(pageInfo2)
                val canvas2 = page2.canvas
                
                paint.shader = gradient
                canvas2.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)
                paint.shader = null // Reset shader
                
                val transactor = pettyCash.transactor
                transactor?.let {
                    // Add Transactor Title
                    val title: String = "Transactor"
                    paint.isFakeBoldText = true
                    paint.textSize = 11f
                    var yPosition2 = 40f
                    
                    canvas2.drawText(title, 20f, yPosition2, paint)
                    yPosition2 += 20f
                    
                    // Transactor Details Table
                    paint.isFakeBoldText = false
                    paint.textSize = 10f
                    
                    // Define the transactor info table
                    val info = mutableMapOf(
                        "Name" to pettyCash.transactor?.name,
                        "Phone Number" to pettyCash.transactor?.phoneNumber,
                        "ID Card" to pettyCash.transactor?.idCard?.toString(),
                        "Address" to pettyCash.transactor?.address,
                        "KRA PIN" to pettyCash.transactor?.kraPin
                    )
                    
                    // Draw the table with transactor details
                    drawTableWithBorders(info, canvas2, paint, 20f, yPosition2, pageWidth - 40f)
                }
                
                pdf.finishPage(page2)
                
                // Add supporting document if it exists and is requested
                if (includeSupportingDocs && pettyCash.supportingDocument != null) {
                    // Create a page for the supporting document info
                    val pageInfo3 = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
                    val page3 = pdf.startPage(pageInfo3)
                    val canvas3 = page3.canvas
                    
                    paint.color = android.graphics.Color.BLACK
                    
                    // Add title with Petty Cash Number and Supporting Document
                    paint.isFakeBoldText = true
                    paint.textSize = 11f
                    val title = "Supporting Document"
                    canvas3.drawText(title, 20f, 40f, paint)
                    
                    // Supporting Document Details Table
                    var yPosition3 = 60f
                    paint.isFakeBoldText = false
                    paint.textSize = 10f
                    
                    val document = pettyCash.supportingDocument
                    val docInfo = mapOf(
                        "Document ID" to document?.id.toString(),
                        "Document Type" to document?.type,
                        "Document No" to document?.documentNo,
                        "Supplier Name" to document?.supplierName,
                        "CU Number" to document?.cuNumber,
                        "Taxable Total Amount" to document?.taxableTotalAmount?.toString(),
                        "Tax Amount" to document?.taxAmount?.toString(),
                        "Total Amount" to document?.totalAmount?.toString(),
                        "Document Date" to document?.documentDate
                    )
                    drawTableWithBorders(docInfo, canvas3, paint, 20f, yPosition3, pageWidth - 40f)
                    
                    pdf.finishPage(page3)
                    
                    // Add supporting document images if they exist
                    val imagePaths = listOf(
                        pettyCash.supportingDocument?.imagePath1,
                        pettyCash.supportingDocument?.imagePath2,
                        pettyCash.supportingDocument?.imagePath3
                    )
                    
                    imagePaths.forEachIndexed { index, imagePath ->
                        imagePath?.let {
                            val imageFile = File(it)
                            if (imageFile.exists()) {
                                val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                                bitmap?.let { img ->
                                    val imagePageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
                                    val imagePage = pdf.startPage(imagePageInfo)
                                    val imageCanvas = imagePage.canvas
                                    
                                    // Resizing the image to fit the page while keeping aspect ratio
                                    val margin = 40f  // Equal margin from all edges
                                    val imageWidth = pageWidth - 2 * margin
                                    val imageHeight = (img.height.toFloat() / img.width.toFloat()) * imageWidth
                                    
                                    // Positioning the image below the title with a small margin
                                    val imageTopPosition = 60f
                                    
                                    // Define the rectangle for the image with rounded corners
                                    val rectF = android.graphics.RectF(
                                        margin,
                                        imageTopPosition,
                                        margin + imageWidth,
                                        imageTopPosition + imageHeight
                                    )
                                    
                                    // Create a rounded bitmap with the radius for all corners
                                    val roundedBitmap = getRoundedBitmap(img, 20f)
                                    
                                    // Draw the rounded image with the defined position
                                    imageCanvas.drawBitmap(roundedBitmap, null, rectF, paint)
                                    
                                    // Draw title above the image
                                    paint.isFakeBoldText = true
                                    paint.textSize = 14f
                                    val imageTitle = "Supporting Document ${index + 1} - ${pettyCash.pettyCashNumber}"
                                    imageCanvas.drawText(imageTitle, margin, imageTopPosition - 10f, paint)
                                    
                                    pdf.finishPage(imagePage)
                                }
                            }
                        }
                    }
                }
            }
            
            // Generate a file name based on the content
            val fileName = if (pettyCashList.size == 1) {
                "PettyCash_${pettyCashList[0].pettyCashNumber?.replace("/", "_").orEmpty()}_Detailed_${System.currentTimeMillis()}.pdf"
            } else {
                "PettyCash_Multiple_Detailed_${System.currentTimeMillis()}.pdf"
            }
            
            // Save PDF to file
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            pdf.writeTo(FileOutputStream(file))
            pdf.close()
            
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating detailed PDF: ${e.message}", e)
            return null
        }
    }
    
    private fun getRoundedBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint()
        val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = android.graphics.RectF(rect)
        paint.isAntiAlias = true
        canvas.drawRoundRect(rectF, radius, radius, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
    
    private fun drawTableWithBorders(
        data: Map<String, String?>,
        canvas: android.graphics.Canvas,
        paint: android.graphics.Paint,
        x: Float,
        startY: Float,
        tableWidth: Float
    ) {
        val defaultCellHeight = 23f
        val columnSpacing = tableWidth / 2f
        val cornerRadius = 10f
        val padding = 10f
        val bottomPadding = 10f // Added padding for the bottom of the table
        
        var currentY = startY
        val textPaint = android.graphics.Paint(paint).apply {
            style = android.graphics.Paint.Style.FILL
            textAlign = android.graphics.Paint.Align.LEFT
        }
        
        // Loop through the data entries to determine cell heights dynamically
        data.entries.forEach { (key, value) ->
            val keyLines = calculateTextLines(key, paint, columnSpacing - padding * 2)
            val valueLines = calculateTextLines(value.orEmpty(), paint, columnSpacing - padding * 2)
            
            val rowHeight = maxOf(
                defaultCellHeight,
                (paint.textSize + 6f) * maxOf(keyLines, valueLines)
            )
            
            // Draw the key text in the first column
            drawMultilineText(
                canvas,
                key,
                x + padding,
                currentY + padding + paint.textSize,
                textPaint,
                columnSpacing - padding * 2
            )
            
            // Draw the value text in the second column
            drawMultilineText(
                canvas,
                value.orEmpty(),
                x + columnSpacing + padding,
                currentY + padding + paint.textSize,
                textPaint,
                columnSpacing - padding * 2
            )
            
            currentY += rowHeight
        }
        
        // Add bottom padding to the table
        currentY += bottomPadding
        
        // Draw the outer border of the table (final enclosing rectangle)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 1f // Increased thickness of the border
        canvas.drawRoundRect(x, startY, x + tableWidth, currentY, cornerRadius, cornerRadius, paint)
        
        paint.color = android.graphics.Color.BLACK
        paint.style = android.graphics.Paint.Style.FILL
    }
    
    private fun calculateTextLines(text: String, paint: android.graphics.Paint, maxWidth: Float): Int {
        val words = text.split(" ")
        var currentLineWidth = 0f
        var lineCount = 1
        
        for (word in words) {
            val wordWidth = paint.measureText("$word ")
            if (currentLineWidth + wordWidth > maxWidth) {
                lineCount++
                currentLineWidth = wordWidth
            } else {
                currentLineWidth += wordWidth
            }
        }
        
        return lineCount
    }
    
    private fun drawMultilineText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: android.graphics.Paint,
        maxWidth: Float
    ) {
        val words = text.split(" ")
        var currentLineWidth = 0f
        var lineY = y
        
        for (word in words) {
            val wordWidth = paint.measureText("$word ")
            if (currentLineWidth + wordWidth > maxWidth) {
                lineY += paint.textSize + 6f // Move to the next line
                currentLineWidth = 0f
            }
            canvas.drawText(word, x + currentLineWidth, lineY, paint)
            currentLineWidth += wordWidth
        }
    }
    
    private fun capitalizeEachWord(input: String): String? {
        val words = input.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result = StringBuilder()
        
        for (word in words) {
            if (word.isNotEmpty()) {
                // Replace special characters with space
                val cleanedWord = word.replace(Regex("[^A-Za-z0-9]"), " ")
                
                val capitalizedWord = cleanedWord.split(" ").joinToString(" ") {
                    if (it.isNotEmpty()) {
                        val firstLetter = it.substring(0, 1).uppercase(Locale.getDefault())
                        val rest = it.substring(1).lowercase(Locale.getDefault())
                        "$firstLetter$rest"
                    } else {
                        ""
                    }
                }
                
                result.append(capitalizedWord).append(" ")
            }
        }
        
        return result.toString().trim()
    }
    
    private fun convertAmountToWords(amount: Double): String {
        if (amount == 0.0) return "Zero Shillings"
        
        val units = arrayOf("", "Thousand", "Million", "Billion")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        )
        
        var integerPart = amount.toInt()
        val fractionalPart = ((amount - integerPart) * 100).toInt() // Get cents if any
        var result = ""
        
        var unitIndex = 0
        while (integerPart > 0) {
            val chunk = integerPart % 1000
            if (chunk > 0) {
                result = "${chunkToWords(chunk, ones, tens)} ${units[unitIndex]} $result"
            }
            integerPart /= 1000
            unitIndex++
        }
        
        result = result.trim() + " Shillings"
        
        if (fractionalPart > 0) {
            result += " and ${chunkToWords(fractionalPart, ones, tens)} Cents"
        }
        
        return result.trim()
    }
    
    private fun chunkToWords(number: Int, ones: Array<String>, tens: Array<String>): String {
        val hundreds = number / 100
        val remainder = number % 100
        val tensPart = remainder / 10
        val onesPart = remainder % 10
        
        val result = StringBuilder()
        
        if (hundreds > 0) {
            result.append("${ones[hundreds]} Hundred ")
        }
        
        if (remainder in 1..19) {
            result.append("${ones[remainder]} ")
        } else if (tensPart > 0) {
            result.append("${tens[tensPart]} ")
            if (onesPart > 0) {
                result.append("${ones[onesPart]} ")
            }
        }
        
        return result.toString().trim()
    }
    
    private fun loadSavedReports() {
        lifecycleScope.launch(Dispatchers.IO) {
            val reports = dbHelper.getReportsByType(ReportType.PETTY_CASH_COPY)
            withContext(Dispatchers.Main) {
                savedCopies.clear()
                savedCopies.addAll(reports)
                updateReportsList()
            }
        }
    }
    
    private fun updateReportsList() {
        copiesAdapter.notifyDataSetChanged()
        
        if (savedCopies.isEmpty()) {
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.recyclerViewCopies.visibility = View.GONE
        } else {
            binding.emptyStateContainer.visibility = View.GONE
            binding.recyclerViewCopies.visibility = View.VISIBLE
        }
    }
    
    fun generateReport() {
        // This is called from the ReportsActivity when the FAB is clicked
        // It would trigger the search action based on the active tab
        Log.d(TAG, "Generate Report called from ReportsActivity")
        
        when (binding.searchTabLayout.selectedTabPosition) {
            0 -> { // By Number
                val pettyCashNumber = binding.pettyCashNumberEditText.text.toString()
                if (pettyCashNumber.isNotEmpty()) {
                    searchByPettyCashNumber(pettyCashNumber)
                } else {
                    Snackbar.make(binding.root, "Please enter a Petty Cash number", Snackbar.LENGTH_SHORT).show()
                }
            }
            1 -> { // By Date
                searchByDateRange()
            }
        }
    }
    
    private fun updateEmptyState() {
        if (savedCopies.isEmpty()) {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.recyclerViewCopies.visibility = View.GONE
        } else {
            binding.emptyStateContainer.visibility = View.GONE
            binding.recyclerViewCopies.visibility = View.VISIBLE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Adapter for petty cash copies
    inner class PettyCashCopiesAdapter(private val reports: List<Report>) : 
        RecyclerView.Adapter<PettyCashCopiesAdapter.ReportViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_petty_cash_copy, parent, false)
            return ReportViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
            holder.bind(reports[position])
        }
        
        override fun getItemCount() = reports.size
        
        inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvPettyCashNumber: TextView = itemView.findViewById(R.id.tvPettyCashNumber)
            private val tvPettyCashDate: TextView = itemView.findViewById(R.id.tvPettyCashDate)
            private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            private val tvTransactor: TextView = itemView.findViewById(R.id.tvTransactor)
            private val tvPaymentMode: TextView = itemView.findViewById(R.id.tvPaymentMode)
            private val tvAccount: TextView = itemView.findViewById(R.id.tvAccount)
            private val btnView: Button = itemView.findViewById(R.id.btnView)
            private val btnPrint: Button = itemView.findViewById(R.id.btnPrint)
            private val btnShare: Button = itemView.findViewById(R.id.btnShare)
            private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
            
            fun bind(report: Report) {
                // Set report details
                tvPettyCashNumber.text = report.name
                tvPettyCashDate.text = "Generated: ${dateFormat.format(report.generatedDate)}"
                
                // Set placeholder values for other fields
                tvAmount.text = ""
                tvTransactor.text = ""
                tvPaymentMode.text = "Type: ${report.filters["reportType"] ?: ""}"
                tvAccount.text = ""
                
                // Set button click listeners
                btnView.setOnClickListener {
                    viewPdf(File(report.filePath))
                }
                
                btnPrint.setOnClickListener {
                    showReportTypeDialog(report)
                }
                
                btnShare.setOnClickListener {
                    sharePdf(File(report.filePath))
                }
                
                btnDelete.setOnClickListener {
                    showDeleteConfirmationDialog(report)
                }
            }
            
            private fun showReportTypeDialog(report: Report) {
                val options = arrayOf("Simplified", "Detailed")
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Report Type")
                    .setItems(options) { dialog, which ->
                        when (which) {
                            0 -> downloadPdf(File(report.filePath), "${report.name} Simplified")
                            1 -> {
                                // Check if we have the detailed version
                                if (report.excelFilePath != null) {
                                    downloadPdf(File(report.excelFilePath), "${report.name} Detailed")
                                } else {
                                    // Generate detailed version
                                    Snackbar.make(binding.root, "Detailed version not available", Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .show()
            }
            
            private fun showDeleteConfirmationDialog(report: Report) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to delete this report?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteReport(report)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun deleteReport(report: Report) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delete the file
                val file = File(report.filePath)
                if (file.exists()) {
                    file.delete()
                }
                
                // Delete the detailed file if it exists
                report.excelFilePath?.let { detailedPath ->
                    val detailedFile = File(detailedPath)
                    if (detailedFile.exists()) {
                        detailedFile.delete()
                    }
                }
                
                // Delete from database
                dbHelper.deleteReport(report.id)
                
                // Remove from the list and update UI
                withContext(Dispatchers.Main) {
                    val position = savedCopies.indexOf(report)
                    if (position != -1) {
                        savedCopies.removeAt(position)
                        copiesAdapter.notifyItemRemoved(position)
                        updateEmptyState()
                        Snackbar.make(binding.root, "Report deleted", Snackbar.LENGTH_SHORT).show()
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
                            selectedOwnerId = if (selectedOwner.ownerCode == "") null else selectedOwner.ownerCode
                            selectedOwnerObject = selectedOwner

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
} 