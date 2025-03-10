package com.example.pettysms.queue

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a queue item for sending petty cash data to QuickBooks
 */
data class QueueItem(
    val id: Int? = null,
    val pettyCashId: Int,
    val pettyCashNumber: String,
    val amount: Double,
    val description: String,
    val date: String,
    val accountId: Int?,
    val accountName: String?,
    val ownerId: Int?,
    val ownerName: String?,
    val truckNumbers: String?, // Comma-separated truck numbers if multiple
    val status: String, // PENDING, SENT, FAILED, SYNCED
    val errorMessage: String? = null,
    val createdAt: String = getCurrentDateTime(),
    val updatedAt: String = getCurrentDateTime(),
    val isTransactionCost: Boolean = false, // Indicates if this is a transaction cost entry
    val relatedQueueItemId: Int? = null // For linking transaction cost entries to main entries
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SYNCED = "SYNCED"
        
        fun getCurrentDateTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
    }
} 