package com.example.pettysms

import android.content.Context
import android.content.SharedPreferences

class PettyCashPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSortAndFilterValues(
        sortOption: String,
        dateFilter: String,
        paymentModes: List<String>,
        customStartDate: String?,
        customEndDate: String?
    ) {
        prefs.edit().apply {
            putString(KEY_SORT_OPTION, sortOption)
            putString(KEY_DATE_FILTER, dateFilter)
            putStringSet(KEY_PAYMENT_MODES, paymentModes.toSet())
            putString(KEY_CUSTOM_START_DATE, customStartDate)
            putString(KEY_CUSTOM_END_DATE, customEndDate)
        }.apply()
    }

    fun clearSortAndFilterValues() {
        prefs.edit().apply {
            remove(KEY_SORT_OPTION)
            remove(KEY_DATE_FILTER)
            remove(KEY_PAYMENT_MODES)
            remove(KEY_CUSTOM_START_DATE)
            remove(KEY_CUSTOM_END_DATE)
        }.apply()
    }

    fun getSortOption(): String = prefs.getString(KEY_SORT_OPTION, "Date") ?: "Date"
    fun getDateFilter(): String = prefs.getString(KEY_DATE_FILTER, "Any Time") ?: "Any Time"
    fun getPaymentModes(): List<String> = prefs.getStringSet(KEY_PAYMENT_MODES, emptySet())?.toList() ?: emptyList()
    fun getCustomStartDate(): String? = prefs.getString(KEY_CUSTOM_START_DATE, null)
    fun getCustomEndDate(): String? = prefs.getString(KEY_CUSTOM_END_DATE, null)

    companion object {
        private const val PREFS_NAME = "PettyCashPrefs"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_DATE_FILTER = "date_filter"
        private const val KEY_PAYMENT_MODES = "payment_modes"
        private const val KEY_CUSTOM_START_DATE = "custom_start_date"
        private const val KEY_CUSTOM_END_DATE = "custom_end_date"
    }
}