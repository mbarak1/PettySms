package com.example.pettysms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

class AutomationAdapter(
    private val context: Context,
    automationRules: List<AutomationRule>,
    private val listener: OnAutomationRuleClickListener
) : RecyclerView.Adapter<AutomationAdapter.ViewHolder>() {

    interface OnAutomationRuleClickListener {
        fun onEditClick(rule: AutomationRule)
        fun onDeleteConfirmation(rule: AutomationRule)
    }

    // Create a private mutable list to store the rules
    private val automationRules = mutableListOf<AutomationRule>().apply {
        addAll(automationRules)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.automationCardView)
        val ownerImage: ImageView = itemView.findViewById(R.id.ownerImage)
        val ruleName: TextView = itemView.findViewById(R.id.ruleName)
        val amountRange: TextView = itemView.findViewById(R.id.amountRange)
        val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
        val transactorChip: Chip = itemView.findViewById(R.id.transactorChip)
        val accountChip: Chip = itemView.findViewById(R.id.accountChip)
        val truckChip: Chip = itemView.findViewById(R.id.truckChip)
        val descriptionPattern: TextView = itemView.findViewById(R.id.descriptionPattern)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_automation_rule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rule = automationRules[position]
        
        // Set rule name
        holder.ruleName.text = rule.name ?: "Unnamed Rule"
        
        // Set chips visibility and text with formatted names
        holder.transactorChip.apply {
            visibility = if (rule.transactorName != null) View.VISIBLE else View.GONE
            text = rule.transactorName?.let { formatName(it) }
        }
        
        holder.accountChip.apply {
            visibility = if (rule.accountName != null) View.VISIBLE else View.GONE
            text = rule.accountName
        }
        
        holder.truckChip.apply {
            visibility = if (rule.truckId != null) View.VISIBLE else View.GONE
            text = when (rule.truckId) {
                -1 -> "All Trucks (${rule.ownerName})"  // Show owner name with All Trucks
                null -> null
                else -> rule.truckName
            }
        }
        
        // Set description pattern
        holder.descriptionPattern.text = rule.descriptionPattern ?: "Any description"
        
        // Set amount range with currency formatting
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val minAmount = rule.minAmount?.let { formatter.format(it) } ?: "Any"
        val maxAmount = rule.maxAmount?.let { formatter.format(it) } ?: "Any"
        
        // Format the amount range text
        holder.amountRange.text = when {
            rule.minAmount != null && rule.maxAmount != null -> "Amount: $minAmount - $maxAmount"
            rule.minAmount != null -> "Min Amount: $minAmount"
            rule.maxAmount != null -> "Max Amount: $maxAmount"
            else -> "Any Amount"
        }
        
        // Handle owner image
        setOwnerImage(holder, rule)
        
        // Set click listeners
        holder.editButton.setOnClickListener { listener.onEditClick(rule) }
        holder.deleteButton.setOnClickListener { listener.onDeleteConfirmation(rule) }
    }

    private fun setOwnerImage(holder: ViewHolder, rule: AutomationRule) {
        rule.ownerId?.let { ownerId ->
            val dbHelper = DbHelper(context)
            val owner = rule.ownerName?.let { dbHelper.getOwnerByName(ownerName = it) }
            val ownerImage = owner?.logoPath
            if (ownerImage != null) {
                try {
                    // Convert Base64 string to byte array
                    val imageBytes = android.util.Base64.decode(ownerImage, android.util.Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.ownerImage.setImageBitmap(bitmap)
                    // Remove tint when using actual image
                    holder.ownerImage.setColorFilter(null)
                } catch (e: Exception) {
                    Log.e("AutomationAdapter", "Error decoding image: ${e.message}")
                    setDefaultOwnerImage(holder)
                }
            } else {
                setDefaultOwnerImage(holder)
            }
        } ?: run {
            // Use automation icon with tint for rules without owner
            holder.ownerImage.setImageResource(R.drawable.baseline_auto_awesome_24)
        }
    }

    private fun setDefaultOwnerImage(holder: ViewHolder) {
        holder.ownerImage.setImageResource(R.drawable.baseline_auto_awesome_24)
        holder.ownerImage.setColorFilter(MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnPrimary))
    }

    private fun formatName(name: String): String {
        return name.split(" ")
            .filter { it.isNotEmpty() } // Remove empty strings between multiple spaces
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
            }
    }

    override fun getItemCount(): Int {
        val count = automationRules.size
        Log.d("AutomationAdapter", "getItemCount() called, returning $count")
        return count
    }
    
    fun updateList(newList: List<AutomationRule>) {
        Log.d("AutomationAdapter", "updateList() called with ${newList.size} items")
        
        // Create a new list to avoid reference issues
        val newListCopy = ArrayList<AutomationRule>(newList)
        
        // Clear and update the list
        automationRules.clear()
        automationRules.addAll(newListCopy)
        
        Log.d("AutomationAdapter", "After update, automationRules.size = ${automationRules.size}")
        
        // Notify the adapter that the data has changed
        notifyDataSetChanged()
        
        // Debug the current state
        debugItems()
    }
    
    fun addItemToTop(rule: AutomationRule) {
        automationRules.add(0, rule)
        notifyItemInserted(0)
        notifyItemRangeChanged(0, minOf(5, automationRules.size))
    }
    
    fun updateItem(updatedRule: AutomationRule) {
        val position = automationRules.indexOfFirst { it.id == updatedRule.id }
        if (position != -1) {
            Log.d("AutomationAdapter", "Updating item at position $position: ${updatedRule.name} (ID: ${updatedRule.id})")
            
            // Create a copy of the current list to avoid concurrent modification issues
            val rulesCopy = ArrayList(automationRules)
            
            // Update the item in the copy
            rulesCopy[position] = updatedRule
            
            // Replace the original list with the updated copy
            automationRules.clear()
            automationRules.addAll(rulesCopy)
            
            // Only notify about the specific item that changed
            notifyItemChanged(position)
            
            // Log the current state for debugging
            Log.d("AutomationAdapter", "After updateItem, automationRules.size = ${automationRules.size}")
            debugItems()
        } else {
            Log.e("AutomationAdapter", "Failed to find item with ID ${updatedRule.id} to update")
        }
    }
    
    fun removeItem(ruleId: Int) {
        val position = automationRules.indexOfFirst { it.id == ruleId }
        if (position != -1) {
            automationRules.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, automationRules.size)
        }
    }

    // Add this method to debug the adapter
    fun debugItems() {
        Log.d("AutomationAdapter", "Current items in adapter: ${automationRules.size}")
        automationRules.forEachIndexed { index, rule ->
            Log.d("AutomationAdapter", "Item $index: ${rule.name} (ID: ${rule.id})")
        }
    }
} 