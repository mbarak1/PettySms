package com.example.pettysms.reports

import java.util.Date

/**
 * Represents a generated report in the application.
 */
data class Report(
    val id: String,
    val name: String,
    val type: ReportType,
    val generatedDate: Date,
    val filePath: String,
    val excelFilePath: String? = null,
    val filters: Map<String, String> = emptyMap()
)

/**
 * Enum representing different types of reports available in the application.
 */
enum class ReportType {
    MPESA_STATEMENT,
    PETTY_CASH_STATEMENT,
    PETTY_CASH_COPY,
    PETTY_CASH_SCHEDULE,
    VAT_REPORT
} 