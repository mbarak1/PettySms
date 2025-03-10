package com.example.pettysms

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.FragmentTruckExpensesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment for displaying expenses for a specific truck
 */
class TruckExpensesFragment : Fragment() {
    private var _binding: FragmentTruckExpensesBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DbHelper
    private lateinit var truckExpensesAdapter: TruckExpensesAdapter
    private var truckNo: String = ""
    
    companion object {
        private const val ARG_TRUCK_NO = "truck_no"
        
        fun newInstance(truckNo: String): TruckExpensesFragment {
            return TruckExpensesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRUCK_NO, truckNo)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            truckNo = it.getString(ARG_TRUCK_NO, "")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTruckExpensesBinding.inflate(inflater, container, false)
        
        // Setup toolbar
        val actionbar = binding.toolbar
        actionbar.title = "Expenses for $truckNo"
        (activity as AppCompatActivity).setSupportActionBar(actionbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize database helper
        dbHelper = DbHelper(requireContext())
        
        // Setup RecyclerView
        setupRecyclerView()
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set Fragment fade transitions
        val fadeIn = android.transition.Fade(android.transition.Fade.IN).apply {
            duration = 300L
            addTarget(view)
            excludeTarget(android.R.id.statusBarBackground, true)
            excludeTarget(android.R.id.navigationBarBackground, true)
        }
        
        val fadeOut = android.transition.Fade(android.transition.Fade.OUT).apply {
            duration = 300L
            addTarget(view)
            excludeTarget(android.R.id.statusBarBackground, true)
            excludeTarget(android.R.id.navigationBarBackground, true)
        }
        
        enterTransition = fadeIn
        exitTransition = fadeOut
        
        // Disable transition overlap for smoother animations
        allowEnterTransitionOverlap = false
        allowReturnTransitionOverlap = false
        
        // Load expenses for the truck
        loadTruckExpenses()
    }
    
    private fun setupRecyclerView() {
        truckExpensesAdapter = TruckExpensesAdapter(requireContext(), mutableListOf())
        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = truckExpensesAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun loadTruckExpenses() {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewExpenses.visibility = View.GONE
        binding.textNoData.visibility = View.GONE
        binding.textErrorMessage.visibility = View.GONE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get current month and year
                val calendar = java.util.Calendar.getInstance()
                val currentMonth = String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)
                val currentYear = calendar.get(java.util.Calendar.YEAR).toString()
                
                // Query to get petty cash entries for this truck in the current month
                val queryBuilder = StringBuilder()
                queryBuilder.append("SELECT * FROM ${DbHelper.TABLE_PETTY_CASH} WHERE ")
                queryBuilder.append("${DbHelper.COL_PETTY_CASH_TRUCKS} LIKE ? ")
                queryBuilder.append("AND ${DbHelper.COL_PETTY_CASH_IS_DELETED} = 0 ")
                // Date filter using substr - dates are in format dd-MM-yyyy
                queryBuilder.append("AND (SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 7, 4) = ? AND SUBSTR(${DbHelper.COL_PETTY_CASH_DATE}, 4, 2) = ?) ")
                queryBuilder.append("ORDER BY ${DbHelper.COL_PETTY_CASH_DATE} DESC")
                
                val args = arrayOf("%$truckNo%", currentYear, currentMonth)
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery(queryBuilder.toString(), args)
                
                val expenses = mutableListOf<TruckExpense>()
                var totalAmount = 0.0
                
                // Process each petty cash entry
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_ID))
                    val date = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_DATE))
                    val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_AMOUNT))
                    val description = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_DESCRIPTION))
                    val truckString = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_TRUCKS))
                    val ownerCode = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COL_PETTY_CASH_OWNER))
                    
                    // Get owner name
                    val ownerName = dbHelper.getOwnerNameByCode(ownerCode) ?: ownerCode
                    
                    // Calculate amount per truck
                    val trucks = truckString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val amountPerTruck = if (trucks.isNotEmpty()) amount / trucks.size else amount
                    
                    // Add to list
                    expenses.add(TruckExpense(id, date, amountPerTruck, description, ownerName, truckString))
                    totalAmount += amountPerTruck
                }
                cursor.close()
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    
                    if (expenses.isEmpty()) {
                        binding.textNoData.visibility = View.VISIBLE
                        binding.textNoData.text = "No expenses found for Truck $truckNo in the current month"
                        binding.recyclerViewExpenses.visibility = View.GONE
                    } else {
                        binding.textNoData.visibility = View.GONE
                        binding.recyclerViewExpenses.visibility = View.VISIBLE
                        
                        // Update total amount
                        binding.textTotalAmount.text = String.format("Total: KES %.2f", totalAmount)
                        binding.textTotalAmount.visibility = View.VISIBLE
                        
                        // Update adapter with expenses
                        truckExpensesAdapter.updateData(expenses)
                    }
                }
            } catch (e: Exception) {
                Log.e("TruckExpensesFragment", "Error loading truck expenses", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.textErrorMessage.visibility = View.VISIBLE
                    binding.textErrorMessage.text = "Error loading expenses: ${e.message}"
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Data class for truck expenses
 */
data class TruckExpense(
    val id: Int,
    val date: String,
    val amount: Double,
    val description: String,
    val owner: String,
    val trucks: String
)

/**
 * Adapter for displaying truck expenses
 */
class TruckExpensesAdapter(
    private val context: android.content.Context,
    private var expenses: MutableList<TruckExpense>
) : RecyclerView.Adapter<TruckExpensesAdapter.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_truck_expense, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]
        holder.bind(expense)
    }
    
    override fun getItemCount(): Int = expenses.size
    
    fun updateData(newExpenses: List<TruckExpense>) {
        expenses.clear()
        expenses.addAll(newExpenses)
        notifyDataSetChanged()
    }
    
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateTextView: TextView = view.findViewById(R.id.textDate)
        private val amountTextView: TextView = view.findViewById(R.id.textAmount)
        private val descriptionTextView: TextView = view.findViewById(R.id.textDescription)
        private val ownerTextView: TextView = view.findViewById(R.id.textOwner)
        private val trucksTextView: TextView = view.findViewById(R.id.textTrucks)
        
        fun bind(expense: TruckExpense) {
            dateTextView.text = expense.date
            amountTextView.text = String.format("KES %.2f", expense.amount)
            descriptionTextView.text = expense.description
            ownerTextView.text = expense.owner
            trucksTextView.text = "Shared with: ${expense.trucks}"
        }
    }
} 