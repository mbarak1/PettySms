package com.example.pettysms

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.pettysms.databinding.FragmentHomeBinding
import com.example.pettysms.reports.ReportsActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dashboard fragment showing analytics for petty cash and trucks.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DbHelper

    // Data for analytics
    private var totalPettyCash = 0
    private var convertedPettyCash = 0
    private var unconvertedPettyCash = 0
    private var bestPerformingTruck: Pair<String, Double>? = null
    private var ownerSpendingData = mutableMapOf<String, MutableMap<Int, Double>>()
    
    // Track loading state
    private var isDataLoaded = false
    private var isChartDataLoaded = false
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        // Setup toolbar
        val actionbar = binding.toolbar1
        actionbar.title = "Home"
        (activity as AppCompatActivity).setSupportActionBar(actionbar)
        
        // Disable toolbar elevation to remove shading on scroll
        actionbar.elevation = 0f
        
        // Initialize database helper
        dbHelper = DbHelper(requireContext())
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup welcome message and date
        setupWelcomeSection()
        
        // Setup click listeners
        setupClickListeners()
        
        // Load analytics data
        if (!isDataLoaded) {
            loadAnalyticsData()
        } else {
            // Data already loaded, just update UI
            binding.progressBarLoading.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
        }
    }
    
    private fun setupWelcomeSection() {
        // Set welcome message based on time of day
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        
        val welcomeMessage = when {
            hourOfDay < 12 -> "Good morning!"
            hourOfDay < 17 -> "Good afternoon!"
            else -> "Good evening!"
        }
        
        binding.textWelcome.text = welcomeMessage
        
        // Set today's date
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        binding.textDate.text = dateFormat.format(calendar.time)
    }
    
    private fun setupClickListeners() {
        // Conversion progress card click listener
        binding.cardConversionProgress.setOnClickListener {
            navigateToUnconvertedPettyCash()
        }
        
        // View unconverted button click listener
        binding.btnViewUnconverted.setOnClickListener {
            navigateToUnconvertedPettyCash()
        }
        
        // Best performing truck card click listener
        binding.cardBestPerformingTruck.setOnClickListener {
            navigateToTruckRankings()
        }
        
        // View all trucks button click listener
        binding.btnViewAllTrucks.setOnClickListener {
            navigateToTruckRankings()
        }
        
        // Add Petty Cash FAB click listener
        binding.fabAddPettyCash.setOnClickListener {
            navigateToAddPettyCash()
        }
        
        // Retry button click listener
        binding.btnRetry.setOnClickListener {
            binding.errorContainer.visibility = View.GONE
            binding.progressBarLoading.visibility = View.VISIBLE
            loadAnalyticsData()
        }
        
        // Setup Quick Action buttons
        setupQuickActionButtons()
    }
    
    private fun setupQuickActionButtons() {
        // Find the buttons in the Quick Actions card
        binding.btnQuickAddPettyCash.setOnClickListener {
            navigateToAddPettyCash()
        }
        
        binding.btnQuickViewReports.setOnClickListener {
            navigateToReports()
        }
    }
    
    private fun loadAnalyticsData() {
        // Prevent multiple simultaneous loading operations
        if (isLoading) return
        
        // Show loading indicators
        binding.progressBarLoading.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        isLoading = true
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Reset data
                totalPettyCash = 0
                convertedPettyCash = 0
                unconvertedPettyCash = 0
                bestPerformingTruck = null
                ownerSpendingData.clear()
                
                // Load petty cash conversion data
                loadPettyCashConversionData()
                
                // Load best performing truck
                loadBestPerformingTruck()
                
                // Update UI for the data loaded so far
                withContext(Dispatchers.Main) {
                    // Get current month name for UI updates
                    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                    
                    // Update chart title with current month
                    binding.textChartTitle.text = "Spending Per Company - $monthName (Up to Today)"
                    
                    updateSummaryCards()
                    updateConversionProgress()
                    updateBestPerformingTruck()
                    binding.progressBarLoading.visibility = View.GONE
                    binding.layoutContent.visibility = View.VISIBLE
                    isDataLoaded = true
                    isLoading = false
                }
                
                // Load spending per company data in the background
                loadSpendingPerCompanyData()
                withContext(Dispatchers.Main) {
                    updateSpendingChart()
                    isChartDataLoaded = true
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading analytics data", e)
                withContext(Dispatchers.Main) {
                    binding.progressBarLoading.visibility = View.GONE
                    binding.errorContainer.visibility = View.VISIBLE
                    binding.textErrorMessage.text = "Error loading analytics: ${e.message}"
                    isLoading = false
                }
            }
        }
    }
    
    private fun loadPettyCashConversionData() {
        try {
            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val currentYear = calendar.get(Calendar.YEAR).toString()
            
            Log.d("HomeFragment", "Getting petty cash conversion data for month: $currentMonth, year: $currentYear")
            
            // Use direct SQL query to get counts for current month only
            val db = dbHelper.readableDatabase
            
            // Query for total petty cash count for current month
            val totalQueryBuilder = StringBuilder()
            totalQueryBuilder.append("SELECT COUNT(*) FROM ${DbHelper.TABLE_PETTY_CASH} WHERE ")
            totalQueryBuilder.append("${DbHelper.COL_PETTY_CASH_IS_DELETED} = 0 ")
            // Date filter using substr - dates are in format dd-MM-yyyy
            totalQueryBuilder.append("AND (SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 7, 4) = ? AND SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 4, 2) = ?) ")
            
            val totalArgs = arrayOf(currentYear, currentMonth)
            val totalCursor = db.rawQuery(totalQueryBuilder.toString(), totalArgs)
            
            totalPettyCash = if (totalCursor.moveToFirst()) {
                totalCursor.getInt(0)
            } else {
                0
            }
            totalCursor.close()
            
            // Query for converted petty cash count for current month
            val convertedQueryBuilder = StringBuilder()
            convertedQueryBuilder.append("SELECT COUNT(*) FROM ${DbHelper.TABLE_PETTY_CASH} WHERE ")
            convertedQueryBuilder.append("${DbHelper.COL_PETTY_CASH_IS_DELETED} = 0 ")
            convertedQueryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_NUMBER} IS NOT NULL ")
            convertedQueryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_NUMBER} != '' ")
            // Date filter using substr - dates are in format dd-MM-yyyy
            convertedQueryBuilder.append("AND (SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 7, 4) = ? AND SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 4, 2) = ?) ")
            
            val convertedArgs = arrayOf(currentYear, currentMonth)
            val convertedCursor = db.rawQuery(convertedQueryBuilder.toString(), convertedArgs)
            
            convertedPettyCash = if (convertedCursor.moveToFirst()) {
                convertedCursor.getInt(0)
            } else {
                0
            }
            convertedCursor.close()
            
            // Calculate unconverted count for current month
            unconvertedPettyCash = totalPettyCash - convertedPettyCash
            
            Log.d("HomeFragment", "Current month statistics - Total: $totalPettyCash, Converted: $convertedPettyCash, Unconverted: $unconvertedPettyCash")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error loading petty cash conversion data", e)
            throw e
        }
    }
    
    private fun loadBestPerformingTruck() {
        try {
            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val currentYear = calendar.get(Calendar.YEAR).toString()
            
            Log.d("HomeFragment", "Getting best performing truck for month: $currentMonth, year: $currentYear")
            
            // Use the same approach as in TruckRankingsFragment to get truck data for current month
            val db = dbHelper.readableDatabase
            val truckExpensesMap = mutableMapOf<String, Double>()
            
            // Build query for current month data with trucks
            val queryBuilder = StringBuilder()
            queryBuilder.append("SELECT * FROM ${DbHelper.TABLE_PETTY_CASH} WHERE ")
            queryBuilder.append("${DbHelper.COL_PETTY_CASH_TRUCKS} IS NOT NULL ")
            queryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_TRUCKS} != '' ")
            queryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_IS_DELETED} = 0 ")
            // Date filter using substr - dates are in format dd-MM-yyyy
            queryBuilder.append("AND (SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 7, 4) = ? AND SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 4, 2) = ?) ")
            
            val args = arrayOf(currentYear, currentMonth)
            val query = queryBuilder.toString()
            
            val cursor = db.rawQuery(query, args)
            
            // Process each petty cash entry and divide expenses among trucks
            while (cursor.moveToNext()) {
                val truckString = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_TRUCKS))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_AMOUNT))
                
                // Split truck string and distribute amount equally among trucks
                val trucks = truckString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                if (trucks.isNotEmpty()) {
                    val amountPerTruck = amount / trucks.size
                    
                    // Add amount to each truck
                    trucks.forEach { truck ->
                        val currentAmount = truckExpensesMap.getOrDefault(truck, 0.0)
                        truckExpensesMap[truck] = currentAmount + amountPerTruck
                    }
                }
            }
            cursor.close()
            
            // Find the truck with the least expenses (best performing)
            bestPerformingTruck = truckExpensesMap.entries
                .map { Pair(it.key, it.value) }
                .minByOrNull { it.second }
            
            Log.d("HomeFragment", "Best performing truck: ${bestPerformingTruck?.first ?: "None"} with expenses: ${bestPerformingTruck?.second ?: 0.0}")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error loading best performing truck", e)
            throw e
        }
    }
    
    private fun loadSpendingPerCompanyData() {
        try {
            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Set calendar to first day of month
            calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
            
            // Format for database query (just the date part)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDate = dateFormat.format(calendar.time)
            
            // Set calendar to last day of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endDate = dateFormat.format(calendar.time)
            
            // Get spending data per company for current month with non-null petty cash numbers only
            val spendingData = dbHelper.getSpendingPerCompanyForDateRangeWithPettyCashNumber(startDate, endDate)
            
            // Organize data by owner and day of month
            ownerSpendingData.clear()
            
            // Create a date format that can handle both formats: dd/MM/yyyy and dd/MM/yyyy HH:mm:ss
            val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            
            spendingData.forEach { triple: Triple<String, String, Double> ->
                val (ownerCode, dateStr, amount) = triple
                try {
                    // Try to parse with the full format first
                    val parsedDate = try {
                        fullDateFormat.parse(dateStr)
                    } catch (e: Exception) {
                        // If that fails, try with just the date format
                        dateFormat.parse(dateStr)
                    }
                    
                    if (parsedDate != null) {
                        val dayOfMonth = Calendar.getInstance().apply {
                            time = parsedDate
                        }.get(Calendar.DAY_OF_MONTH)
                        
                        // Add to map
                        if (!ownerSpendingData.containsKey(ownerCode)) {
                            ownerSpendingData[ownerCode] = mutableMapOf()
                        }
                        
                        val ownerData = ownerSpendingData[ownerCode]!!
                        ownerData[dayOfMonth] = (ownerData[dayOfMonth] ?: 0.0) + amount
                    } else {
                        Log.e("HomeFragment", "Failed to parse date: $dateStr")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error parsing date: $dateStr", e)
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error loading spending per company data", e)
            throw e
        }
    }
    
    private fun updateSummaryCards() {
        // Update total petty cash count
        binding.textTotalPettyCash.text = totalPettyCash.toString()
        
        // Update converted petty cash count
        binding.textConvertedPettyCash.text = convertedPettyCash.toString()
    }
    
    private fun updateConversionProgress() {
        // Get current month name for display
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().time)
        
        // Calculate conversion percentage
        val conversionPercentage = if (totalPettyCash > 0) {
            (convertedPettyCash.toFloat() / totalPettyCash.toFloat() * 100).roundToInt()
        } else {
            0
        }
        
        // Update progress bar
        binding.progressConversion.progress = conversionPercentage
        
        // Update text views
        binding.textConversionPercentage.text = "$conversionPercentage%"
        binding.textConversionDetails.text = "$convertedPettyCash of $totalPettyCash petty cash entries converted in $monthName"
        
        // Update unconverted count chip
        binding.chipUnconvertedCount.text = "$unconvertedPettyCash unconverted in $monthName"
        
        // Set color based on percentage
        val progressColor = when {
            conversionPercentage < 50 -> ContextCompat.getColor(requireContext(), R.color.red_color)
            conversionPercentage < 80 -> ContextCompat.getColor(requireContext(), R.color.orange_color)
            else -> ContextCompat.getColor(requireContext(), R.color.green_color)
        }
        
        binding.progressConversion.setIndicatorColor(progressColor)
    }
    
    private fun updateBestPerformingTruck() {
        if (bestPerformingTruck != null) {
            val (truckNo, expenses) = bestPerformingTruck!!
            binding.textBestTruckNumber.text = truckNo
            binding.textBestTruckExpenses.text = String.format("KES %.2f", expenses)
            binding.cardBestPerformingTruck.visibility = View.VISIBLE
            
            // Set the truck icon tint to a dynamic color based on Material 3
            val colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE)
            binding.imageTruckIcon.setColorFilter(colorPrimary)
        } else {
            binding.cardBestPerformingTruck.visibility = View.GONE
        }
    }
    
    private fun updateSpendingChart() {
        val lineChart = binding.chartSpendingPerCompany
        
        // Clear any existing chart data
        lineChart.clear()
        lineChart.data = null
        
        // Get current month name and current day
        val calendar = Calendar.getInstance()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        // If no data, show message and hide chart
        if (ownerSpendingData.isEmpty()) {
            binding.textNoChartData.visibility = View.VISIBLE
            binding.textNoChartData.text = "No converted petty cash entries for $monthName"
            binding.textChartTitle.text = "Spending Per Company - $monthName"
            lineChart.visibility = View.GONE
            return
        }
        
        binding.textNoChartData.visibility = View.GONE
        lineChart.visibility = View.VISIBLE
        
        try {
            // Only show days up to the current day
            val daysToShow = currentDay
            
            // Get Material 3 colors for the chart
            val colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE)
            val colorSecondary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSecondary, Color.GREEN)
            val colorTertiary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiary, Color.YELLOW)
            val colorSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant, Color.GRAY)
            val colorSurfaceContainer = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainer, Color.LTGRAY)
            val colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            val colorControlNormal = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorControlNormal, Color.DKGRAY)
            
            // Create a color array for different owners using Material 3 colors
            val colors = listOf(
                colorPrimary,
                colorSecondary,
                colorTertiary,
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_light),
                ContextCompat.getColor(requireContext(), android.R.color.holo_purple),
                ContextCompat.getColor(requireContext(), R.color.purple_color),
                ContextCompat.getColor(requireContext(), R.color.aqua_color),
                ContextCompat.getColor(requireContext(), R.color.yellow_color),
                ContextCompat.getColor(requireContext(), R.color.green_color),
                ContextCompat.getColor(requireContext(), R.color.pink_color)
            )
            
            // Create a dataset for each owner
            val dataSets = mutableListOf<LineDataSet>()
            
            ownerSpendingData.entries.forEachIndexed { index, (ownerCode, dayData) ->
                val entries = mutableListOf<Entry>()
                
                // Get owner name from database
                val ownerName = dbHelper.getOwnerNameByCode(ownerCode) ?: ownerCode
                
                // Create entries for each day up to the current day
                for (day in 1..daysToShow) {
                    val amount = dayData[day] ?: 0.0
                    entries.add(Entry((day-1).toFloat(), amount.toFloat()))
                }
                
                // Skip empty datasets
                if (entries.all { it.y == 0f }) return@forEachIndexed
                
                // Create dataset
                val dataSet = LineDataSet(entries, ownerName)
                val color = colors[index % colors.size]
                
                // Set gradient fill
                val startColor = color
                val endColor = colorSurfaceContainer
                val gradientDrawable = getGradientDrawable(startColor, endColor)
                dataSet.fillDrawable = gradientDrawable
                dataSet.setDrawFilled(true)
                
                // Customize markers
                dataSet.setDrawCircles(true)
                dataSet.circleRadius = 4f
                dataSet.setCircleColor(color)
                
                // Customize line
                dataSet.color = color
                dataSet.lineWidth = 2f
                dataSet.mode = LineDataSet.Mode.LINEAR
                
                dataSet.valueTextColor = colorOnSurface
                dataSet.valueTextSize = 10f
                
                dataSets.add(dataSet)
            }
            
            // If all datasets were empty, show no data message
            if (dataSets.isEmpty()) {
                binding.textNoChartData.visibility = View.VISIBLE
                binding.textNoChartData.text = "No converted petty cash entries for $monthName"
                binding.textChartTitle.text = "Spending Per Company - $monthName"
                lineChart.visibility = View.GONE
                return
            }
            
            // Update chart title
            binding.textChartTitle.text = "Spending Per Company - $monthName (Up to Today)"
            
            // Create line data
            val lineData = LineData(dataSets as List<ILineDataSet>)
            
            // Configure chart appearance with Material 3 styling
            lineChart.description.isEnabled = false
            
            lineChart.legend.isEnabled = true
            lineChart.legend.textSize = 12f
            lineChart.legend.textColor = colorControlNormal
            lineChart.setDrawGridBackground(false)
            lineChart.setPinchZoom(false)
            lineChart.isDragEnabled = false
            lineChart.setScaleEnabled(false)
            lineChart.isDoubleTapToZoomEnabled = false
            lineChart.setExtraOffsets(10f, 10f, 10f, 10f)
            
            // Configure X axis with Material 3 styling
            val xAxis = lineChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            
            // Prepare x-axis labels
            val xAxisLabels = mutableListOf<String>()
            for (day in 1..daysToShow) {
                xAxisLabels.add(day.toString())
            }
            
            // Use a custom value formatter for x-axis labels
            xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
            xAxis.textSize = 12f
            xAxis.textColor = colorControlNormal
            
            // Configure Y axis with Material 3 styling
            val leftAxis = lineChart.axisLeft
            leftAxis.setDrawGridLines(false)
            leftAxis.textSize = 12f
            leftAxis.textColor = colorControlNormal
            
            val rightAxis = lineChart.axisRight
            rightAxis.isEnabled = false
            
            // Set data to chart
            lineChart.data = lineData
            
            // Display marker when clicked
            lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val day = (e?.x?.toInt() ?: 0) + 1
                    val value = e?.y ?: 0f
                    Toast.makeText(
                        requireContext(),
                        "Day: $day, Value: $value",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                override fun onNothingSelected() {}
            })
            
            // Set the initial alpha to 0
            lineChart.alpha = 0f
            
            // Animate the alpha to 1 (fully visible) over a specified duration
            lineChart.animate()
                .alpha(1f)
                .setDuration(1000)
                .start()
            
            // Refresh chart
            lineChart.invalidate()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating spending chart", e)
            binding.textNoChartData.visibility = View.VISIBLE
            binding.textNoChartData.text = "Error loading chart data"
            binding.textChartTitle.text = "Spending Per Company - $monthName"
            lineChart.visibility = View.GONE
        }
    }
    
    // Helper function to create gradient drawable for chart
    private fun getGradientDrawable(startColor: Int, endColor: Int): Drawable {
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                val gradient = LinearGradient(
                    0f, bounds.top.toFloat(), 0f, bounds.bottom.toFloat(),
                    startColor, endColor, Shader.TileMode.CLAMP
                )
                val paint = Paint().apply { shader = gradient }
                canvas.drawRect(bounds, paint)
            }
            
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
    }
    
    private fun navigateToUnconvertedPettyCash() {
        // Create intent for ViewAllPettyCashActivity with filter for unconverted petty cash
        val intent = Intent(requireContext(), UnconvertedPettyCashActivity::class.java).apply {
            putExtra("FILTER_UNCONVERTED", true)
        }
        startActivity(intent)
    }
    
    private fun navigateToTruckRankings() {
        // Navigate to TruckRankingsFragment
        findNavController().navigate(R.id.action_HomeFragment_to_TruckRankingsFragment)
    }
    
    private fun navigateToAddPettyCash() {
        // Show AddPettyCashFragment dialog
        val addPettyCashFragment = AddPettyCashFragment()
        addPettyCashFragment.show(parentFragmentManager, "AddPettyCashFragment")
    }
    
    private fun navigateToReports() {
        // Navigate to Reports screen
        val intent = Intent(requireContext(), ReportsActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        
        // Reset data loading flags to ensure fresh data is loaded
        isDataLoaded = false
        isChartDataLoaded = false
        
        // Reset chart title to default
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        binding.textChartTitle.text = "Spending Per Company - $monthYear"
        
        // Reload analytics data to ensure we have the latest information
        loadAnalyticsData()
        
        // Update welcome message and date
        setupWelcomeSection()
    }

    override fun onPause() {
        super.onPause()
        // Clear chart data to free up memory
        if (binding.chartSpendingPerCompany != null) {
            binding.chartSpendingPerCompany.clear()
            binding.chartSpendingPerCompany.data = null
            binding.chartSpendingPerCompany.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear references to prevent memory leaks
        if (binding.chartSpendingPerCompany != null) {
            binding.chartSpendingPerCompany.clear()
            binding.chartSpendingPerCompany.data = null
        }
        _binding = null
    }
}