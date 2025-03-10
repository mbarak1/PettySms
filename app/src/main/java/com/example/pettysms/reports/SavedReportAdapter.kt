package com.example.pettysms.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying saved reports in a RecyclerView.
 */
class SavedReportAdapter(
    private val reports: MutableList<Report>,
    private val onReportClick: (Report) -> Unit,
    private val onDownloadPdf: ((Report) -> Unit)?,
    private val onDownloadExcel: ((Report) -> Unit)?,
    private val onShareReport: (Report) -> Unit,
    private val onDeleteReport: (Report) -> Unit
) : RecyclerView.Adapter<SavedReportAdapter.ReportViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reportTitle: TextView = itemView.findViewById(R.id.textReportTitle)
        val reportDate: TextView = itemView.findViewById(R.id.textReportDate)
        val reportDetails: TextView = itemView.findViewById(R.id.textReportDetails)
        val btnViewReport: Chip = itemView.findViewById(R.id.btnViewReport)
        val btnDownloadPdf: Chip = itemView.findViewById(R.id.btnDownloadPdf)
        val btnDownloadExcel: Chip = itemView.findViewById(R.id.btnDownloadExcel)
        val btnShare: MaterialButton = itemView.findViewById(R.id.btnShare)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun getItemCount() = reports.size

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        
        // Set report title
        holder.reportTitle.text = formatReportName(report.name)
        
        // Set generation date
        holder.reportDate.text = dateFormat.format(report.generatedDate)
        
        // Build details string from filters
        val detailsBuilder = StringBuilder()
        report.filters.forEach { (key, value) ->
            if (value.isNotEmpty()) {
                val formattedKey = key.replaceFirstChar { it.uppercase() }
                    .replace(Regex("([a-z])([A-Z])"), "$1 $2") // Convert camelCase to Title Case
                
                if (detailsBuilder.isNotEmpty()) detailsBuilder.append(" • ")
                detailsBuilder.append("$formattedKey: $value")
            }
        }
        holder.reportDetails.text = detailsBuilder.toString()
        
        // Set click listeners
        holder.btnViewReport.setOnClickListener { onReportClick(report) }
        
        // Handle PDF button
        if (onDownloadPdf != null) {
            holder.btnDownloadPdf.visibility = View.VISIBLE
            holder.btnDownloadPdf.setOnClickListener { onDownloadPdf.invoke(report) }
        } else {
            holder.btnDownloadPdf.visibility = View.GONE
        }
        
        // Handle Excel button
        if (onDownloadExcel != null) {
            holder.btnDownloadExcel.visibility = View.VISIBLE
            holder.btnDownloadExcel.setOnClickListener { onDownloadExcel.invoke(report) }
        } else {
            holder.btnDownloadExcel.visibility = View.GONE
        }
        
        holder.btnShare.setOnClickListener { onShareReport(report) }
        holder.btnDelete.setOnClickListener { 
            onDeleteReport(report)
            //removeReport(position)
        }
    }
    
    /**
     * Format a report name by removing underscores and any timestamp.
     */
    private fun formatReportName(name: String): String {
        // Remove timestamp suffix (e.g., MpesaStatement_1622547689)
        val baseName = name.split("_").firstOrNull() ?: name
        
        // Insert spaces before capital letters
        return baseName.replace(Regex("([a-z])([A-Z])"), "$1 $2")
    }
    
    /**
     * Remove a report from the list and notify the adapter
     */
    fun removeReport(position: Int) {
        if (position >= 0 && position < reports.size) {
            reports.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, reports.size)
        }
    }
} 