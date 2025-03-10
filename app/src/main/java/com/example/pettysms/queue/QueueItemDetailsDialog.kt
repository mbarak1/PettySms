package com.example.pettysms.queue

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.example.pettysms.R
import java.text.SimpleDateFormat
import java.util.*

class QueueItemDetailsDialog(
    context: Context,
    private val queueItem: QueueItem
) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_queue_item_details)
        
        // Set dialog width to match parent
        window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        // Initialize views
        val pettyCashNumberTextView = findViewById<TextView>(R.id.tv_petty_cash_number_value)
        val amountTextView = findViewById<TextView>(R.id.tv_amount_value)
        val dateTextView = findViewById<TextView>(R.id.tv_date_value)
        val descriptionTextView = findViewById<TextView>(R.id.tv_description_value)
        val accountTextView = findViewById<TextView>(R.id.tv_account_value)
        val ownerTextView = findViewById<TextView>(R.id.tv_owner_value)
        val trucksTextView = findViewById<TextView>(R.id.tv_trucks_value)
        val statusTextView = findViewById<TextView>(R.id.tv_status_value)
        val errorMessageTextView = findViewById<TextView>(R.id.tv_error_message_value)
        val createdAtTextView = findViewById<TextView>(R.id.tv_created_at_value)
        val updatedAtTextView = findViewById<TextView>(R.id.tv_updated_at_value)
        val closeButton = findViewById<Button>(R.id.btn_close)
        
        // Set values
        pettyCashNumberTextView.text = queueItem.pettyCashNumber
        amountTextView.text = String.format("%.2f", queueItem.amount)
        
        // Format the date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(queueItem.date)
            date?.let {
                dateTextView.text = outputFormat.format(it)
            } ?: run {
                dateTextView.text = queueItem.date
            }
        } catch (e: Exception) {
            dateTextView.text = queueItem.date
        }
        
        // Set other fields
        descriptionTextView.text = queueItem.description ?: "No description"
        accountTextView.text = queueItem.accountName ?: "No account"
        ownerTextView.text = queueItem.ownerName ?: "No owner"
        trucksTextView.text = queueItem.truckNumbers ?: "No trucks"
        statusTextView.text = queueItem.status
        
        // Format timestamps
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
            
            val createdDate = inputFormat.parse(queueItem.createdAt)
            createdDate?.let {
                createdAtTextView.text = outputFormat.format(it)
            } ?: run {
                createdAtTextView.text = queueItem.createdAt
            }
            
            val updatedDate = inputFormat.parse(queueItem.updatedAt)
            updatedDate?.let {
                updatedAtTextView.text = outputFormat.format(it)
            } ?: run {
                updatedAtTextView.text = queueItem.updatedAt
            }
        } catch (e: Exception) {
            createdAtTextView.text = queueItem.createdAt
            updatedAtTextView.text = queueItem.updatedAt
        }
        
        // Show error message if available
        if (queueItem.errorMessage.isNullOrEmpty()) {
            errorMessageTextView.text = "No error"
        } else {
            errorMessageTextView.text = queueItem.errorMessage
        }
        
        // Set close button action
        closeButton.setOnClickListener {
            dismiss()
        }
    }
} 