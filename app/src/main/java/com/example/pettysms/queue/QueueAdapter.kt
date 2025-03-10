package com.example.pettysms.queue

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.DbHelper
import com.example.pettysms.R
import com.example.pettysms.databinding.ItemQueueBinding
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class QueueAdapter(
    private val queueItems: List<QueueItem>,
    private val onItemClick: (QueueItem) -> Unit,
    private val onStatusChanged: (QueueItem) -> Unit
) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {

    private lateinit var context: Context
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("KES")
    }
    
    companion object {
        private const val TAG = "QueueAdapter"
    }

    init {
        Log.d(TAG, "QueueAdapter initialized with ${queueItems.size} items")
        queueItems.forEachIndexed { index, item ->
            Log.d(TAG, "Item $index: ${item.pettyCashNumber}, status: ${item.status}")
        }
    }

    inner class QueueViewHolder(val binding: ItemQueueBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            Log.d(TAG, "ViewHolder initialized with layout height: ${binding.root.layoutParams?.height}, width: ${binding.root.layoutParams?.width}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        context = parent.context
        Log.d(TAG, "onCreateViewHolder called with parent width: ${parent.width}, height: ${parent.height}")
        
        val binding = ItemQueueBinding.inflate(LayoutInflater.from(context), parent, false)
        Log.d(TAG, "Created ViewHolder with root view: ${binding.root.javaClass.simpleName}")
        
        // Ensure the item has proper layout params
        val layoutParams = binding.root.layoutParams
        Log.d(TAG, "ViewHolder layout params - width: ${layoutParams.width}, height: ${layoutParams.height}")
        
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val queueItem = queueItems[position]
        val binding = holder.binding
        Log.d(TAG, "Binding item at position $position: ${queueItem.pettyCashNumber}")

        try {
            // Set primary information
            binding.tvPettyCashNumber.text = queueItem.pettyCashNumber ?: "No Number"
            binding.tvAmount.text = currencyFormatter.format(queueItem.amount ?: 0.0)
            binding.tvDescription.text = queueItem.description ?: "No description"
            
            // Format and set date
            binding.tvDate.text = queueItem.date ?: "Unknown date"
    
            // Set account and owner chips
            binding.chipAccount.text = queueItem.accountName ?: "Unknown"
            binding.chipOwner.text = queueItem.ownerName ?: "Unknown"
    
            // Set status chip with appropriate styling
            binding.chipStatus.text = queueItem.status ?: "UNKNOWN"
            setupStatusChip(binding.chipStatus, queueItem.status)
    
            // Handle error message for failed items
            if (queueItem.status == "FAILED" || queueItem.status == QueueItem.STATUS_FAILED) {
                binding.tvErrorMessage.visibility = View.VISIBLE
                binding.tvErrorMessage.text = queueItem.errorMessage ?: "Unknown error"
                binding.btnRetry.visibility = View.VISIBLE
                Log.d(TAG, "Item ${queueItem.pettyCashNumber} has FAILED status, showing error message")
            } else {
                binding.tvErrorMessage.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
            }
    
            // Set up click listeners
            binding.btnDetails.setOnClickListener {
                Log.d(TAG, "Details button clicked for item ${queueItem.pettyCashNumber}")
                onItemClick(queueItem)
            }
    
            binding.btnRetry.setOnClickListener {
                Log.d(TAG, "Retry button clicked for item ${queueItem.pettyCashNumber}")
                // Create a mutable copy of the queue item
                val mutableQueueItem = queueItem.copy(
                    status = QueueItem.STATUS_PENDING,
                    errorMessage = null
                )
                
                // Update database
                val dbHelper = DbHelper(context)
                mutableQueueItem.id?.let { id ->
                    if (dbHelper.updateQueueItemStatus(id, QueueItem.STATUS_PENDING)) {
                        // Immediately update UI elements
                        binding.chipStatus.text = QueueItem.STATUS_PENDING
                        binding.tvErrorMessage.visibility = View.GONE
                        binding.btnRetry.visibility = View.GONE
                        setupStatusChip(binding.chipStatus, QueueItem.STATUS_PENDING)
                        
                        // Notify the activity about the status change
                        onStatusChanged(mutableQueueItem)
                        
                        Log.d(TAG, "Successfully updated item ${queueItem.pettyCashNumber} to PENDING status")
                    } else {
                        Log.e(TAG, "Failed to update status for item ${queueItem.pettyCashNumber}")
                    }
                }
            }
    
            // Make the entire card clickable
            binding.root.setOnClickListener {
                Log.d(TAG, "Card clicked for item ${queueItem.pettyCashNumber}")
                onItemClick(queueItem)
            }
            
            // Check if the item view has proper dimensions after binding
            binding.root.post {
                Log.d(TAG, "Item at position $position post-layout - width: ${binding.root.width}, height: ${binding.root.height}")
            }
            
            Log.d(TAG, "Successfully bound item at position $position: ${queueItem.pettyCashNumber}")
        } catch (e: Exception) {
            // Log any errors during binding
            Log.e(TAG, "Error binding view holder at position $position: ${e.message}", e)
        }
    }

    private fun setupStatusChip(chip: View, status: String?) {
        try {
            if (chip !is Chip) {
                Log.d(TAG, "View is not a Chip, it's a ${chip.javaClass.simpleName}")
                return
            }
            
            // Set background color based on status
            val backgroundColor = when (status) {
                QueueItem.STATUS_PENDING, "PENDING" -> ContextCompat.getColor(context,
                    R.color.pendingColor
                )
                QueueItem.STATUS_SENT, "SENT" -> ContextCompat.getColor(context, R.color.sentColor)
                QueueItem.STATUS_SYNCED, "SYNCED" -> ContextCompat.getColor(context,
                    R.color.syncedColor
                )
                QueueItem.STATUS_FAILED, "FAILED" -> ContextCompat.getColor(context,
                    R.color.failedColor
                )
                else -> ContextCompat.getColor(context, R.color.defaultColor)
            }
            
            // Set text color based on status - white for SENT and SYNCED, black for others
            val textColor = when (status) {
                QueueItem.STATUS_SENT, "SENT", QueueItem.STATUS_SYNCED, "SYNCED" ->
                    ContextCompat.getColor(context, android.R.color.white)
                else -> 
                    ContextCompat.getColor(context, android.R.color.black)
            }
            
            // Apply the colors
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(backgroundColor)
            chip.setTextColor(textColor)
            
            Log.d(TAG, "Applied simple chip styling for status: $status with color: $backgroundColor and text color: $textColor")
        } catch (e: Exception) {
            Log.e(TAG, "Error styling chip for status $status: ${e.message}", e)
        }
    }

    override fun getItemCount(): Int {
        val count = queueItems.size
        Log.d(TAG, "getItemCount() returning $count items")
        return count
    }
} 