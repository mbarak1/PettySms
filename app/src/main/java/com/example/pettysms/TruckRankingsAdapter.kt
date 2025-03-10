package com.example.pettysms

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter for displaying truck rankings in a RecyclerView
 */
class TruckRankingsAdapter(
    private val context: Context,
    private var truckExpenses: MutableList<Pair<String, Double>> = mutableListOf(),
    private val clickListener: OnTruckClickListener? = null
) : RecyclerView.Adapter<TruckRankingsAdapter.ViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
    
    init {
        numberFormat.currency = java.util.Currency.getInstance("KES")
    }

    interface OnTruckClickListener {
        fun onTruckClick(truckNo: String, amount: Double)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_truck_expense_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (truckNo, amount) = truckExpenses[position]
        holder.bind(truckNo, amount, position + 1)
    }

    override fun getItemCount(): Int = truckExpenses.size

    /**
     * Update the adapter data using DiffUtil for efficient updates
     */
    fun updateData(newTruckExpenses: List<Pair<String, Double>>) {
        val diffCallback = TruckExpensesDiffCallback(truckExpenses, newTruckExpenses)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        truckExpenses.clear()
        truckExpenses.addAll(newTruckExpenses)
        
        diffResult.dispatchUpdatesTo(this)
    }
    
    /**
     * Add more data for pagination
     */
    fun addData(moreExpenses: List<Pair<String, Double>>) {
        val startPosition = truckExpenses.size
        truckExpenses.addAll(moreExpenses)
        notifyItemRangeInserted(startPosition, moreExpenses.size)
    }
    
    /**
     * Clear all data to free up memory
     */
    fun clearData() {
        truckExpenses.clear()
        notifyDataSetChanged()
    }

    /**
     * ViewHolder for truck ranking items
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val rankTextView: TextView = view.findViewById(R.id.textRank)
        private val truckNoTextView: TextView = view.findViewById(R.id.textTruckNo)
        private val amountTextView: TextView = view.findViewById(R.id.textAmount)
        private val rootView: View = view
        
        fun bind(truckNo: String, amount: Double, rank: Int) {
            rankTextView.text = "#$rank"
            truckNoTextView.text = truckNo
            amountTextView.text = String.format("KES %.2f", amount)
            
            // Set up click listener
            rootView.setOnClickListener {
                clickListener?.onTruckClick(truckNo, amount)
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient updates
     */
    private class TruckExpensesDiffCallback(
        private val oldList: List<Pair<String, Double>>,
        private val newList: List<Pair<String, Double>>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].first == newList[newItemPosition].first
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
} 