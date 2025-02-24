package com.example.pettysms

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.pettysms.databinding.FragmentPettyCashSortFilterDialogBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Locale

class PettyCashSortFilterDialogFragment : DialogFragment() {
    private var _binding: FragmentPettyCashSortFilterDialogBinding? = null
    private val binding get() = _binding!!
    private var radioGroup: RadioGroup? = null
    private var sortArrayList = mutableListOf<String>()
    private var paymentModeArrayList = mutableListOf<String>()
    private var dateRangeArrayList = mutableListOf<String>()
    private var keyValueMap = mutableMapOf<String, MutableList<String>>()
    private var onApplyClickListener: OnApplyClickListener? = null
    private var dateArrayHasDate = false
    private lateinit var preferences: PettyCashPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.PrefsTheme)
        preferences = PettyCashPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPettyCashSortFilterDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views first
        radioGroup = binding.radioGroup
        val dateRangeTextView = binding.customRangeLink
        val chipGroupSort = binding.chipGroupSort
        val chipGroupPaymentMode = binding.chipGroupPaymentMode
        val resetButton = binding.btnResetAll

        // Set tags for radio buttons
        binding.radioAnyTime.tag = "Any Time"
        binding.radioToday.tag = "Today"
        binding.radioThisWeek.tag = "This Week"
        binding.radioThisMonth.tag = "This Month"
        binding.radioLastMonth.tag = "Last Month"
        binding.radioLastSixMonths.tag = "Last Six Months"

        // Set default values if no saved state
        if (arguments == null) {
            setDefaultFilters(chipGroupSort)
        }

        // Restore previous selections or use defaults
        arguments?.let { args ->
            // Restore date filter
            val dateFilter = args.getString("selected_date_filter")
            val dateRange = args.getString("selected_date_range")
            
            when {
                dateRange != null -> {
                    dateRangeTextView.text = dateRange
                    dateArrayHasDate = true
                    dateRangeArrayList.clear()
                    dateRangeArrayList.add(dateRange)
                    radioGroup?.clearCheck()
                }
                dateFilter != null -> {
                    radioGroup?.findViewWithTag<RadioButton>(dateFilter)?.isChecked = true
                    dateArrayHasDate = false
                    dateRangeArrayList.clear()
                }
                else -> {
                    // Set default date filter if none selected
                    setDefaultDateFilter()
                }
            }

            // Restore sort option or use default
            val sortOption = args.getString("selected_sort_option")
            if (sortOption != null) {
                val chipId = when (sortOption) {
                    "Date" -> R.id.chipDate
                    "Amount" -> R.id.chipAmount
                    else -> null
                }
                chipId?.let { 
                    binding.chipGroupSort.check(it)
                    sortArrayList.clear()
                    sortArrayList.add(sortOption)
                }
            } else {
                // Set default sort if none selected
                binding.chipGroupSort.check(R.id.chipDate)
                sortArrayList.clear()
                sortArrayList.add("Date")
            }

            // Restore payment modes
            args.getStringArray("selected_payment_modes")?.let { modes ->
                paymentModeArrayList.clear()
                paymentModeArrayList.addAll(modes)
                modes.forEach { paymentMode ->
                    val chipId = when (paymentMode) {
                        "Cash" -> R.id.chipCash
                        "M-Pesa" -> R.id.chipMpesa
                        else -> null
                    }
                    chipId?.let { binding.chipGroupPaymentMode.check(it) }
                }
            }
        }

        val toolbar = binding.toolbarSortAndFilter
        toolbar.title = "Sort and Filter Petty Cash"
        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.inflateMenu(R.menu.menu_sort_filter)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuApply -> {
                    collectSortAndFilterData()
                    dismiss()
                    true
                }
                else -> false
            }
        }

        setupDateRangePicker(dateRangeTextView)
        setupChipGroups(chipGroupSort, chipGroupPaymentMode)
        setupResetButton(resetButton, chipGroupSort, chipGroupPaymentMode)

        // Load saved values
        loadSavedValues()
    }

    private fun setupDateRangePicker(dateRangeTextView: TextView) {
        dateRangeTextView.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker().build()
            
            picker.addOnPositiveButtonClickListener { selection ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val startDate = dateFormat.format(selection.first)
                val endDate = dateFormat.format(selection.second)
                val dateRangeString = "$startDate - $endDate"

                dateArrayHasDate = true
                dateRangeArrayList.clear()
                dateRangeArrayList.add(dateRangeString)
                radioGroup?.clearCheck() // Clear radio selection when date range is selected
                dateRangeTextView.text = dateRangeString
            }

            picker.show(parentFragmentManager, picker.toString())
        }
    }

    private fun setupChipGroups(chipGroupSort: ChipGroup, chipGroupPaymentMode: ChipGroup) {
        // Sort options
        chipGroupSort.setOnCheckedStateChangeListener { group, _ ->
            sortArrayList.clear()
            group.checkedChipIds.forEach { chipId ->
                val chip = group.findViewById<Chip>(chipId)
                chip?.text?.toString()?.let { sortArrayList.add(it) }
            }
        }

        // Payment mode options
        chipGroupPaymentMode.setOnCheckedStateChangeListener { group, _ ->
            paymentModeArrayList.clear()
            group.checkedChipIds.forEach { chipId ->
                val chip = group.findViewById<Chip>(chipId)
                chip?.text?.toString()?.let { paymentModeArrayList.add(it) }
            }
        }
    }

    private fun setDefaultFilters(chipGroupSort: ChipGroup) {
        // Set default sort by Date
        binding.chipGroupSort.check(R.id.chipDate)
        sortArrayList.clear()
        sortArrayList.add("Date")

        // Set default date filter to This Month
        setDefaultDateFilter()
    }

    private fun setDefaultDateFilter() {
        binding.radioAnyTime.isChecked = true
        dateArrayHasDate = false
        dateRangeArrayList.clear()
    }

    private fun setupResetButton(resetButton: Button, vararg chipGroups: ChipGroup) {
        resetButton.setOnClickListener {
            // Clear all selections first
            chipGroups.forEach { it.clearCheck() }
            radioGroup?.clearCheck()
            dateArrayHasDate = false
            binding.customRangeLink.text = "Custom Range"
            sortArrayList.clear()
            paymentModeArrayList.clear()
            dateRangeArrayList.clear()

            // Set default values
            setDefaultFilters(binding.chipGroupSort)

            // Clear saved values
            preferences.clearSortAndFilterValues()
        }
    }

    private fun collectSortAndFilterData() {
        // Collect sort options
        keyValueMap["sort"] = sortArrayList

        // Collect payment mode filters
        keyValueMap["payment_mode"] = paymentModeArrayList

        // Collect date range
        if (dateArrayHasDate) {
            keyValueMap["date"] = dateRangeArrayList
        } else {
            radioGroup?.checkedRadioButtonId?.let { checkedId ->
                if (checkedId != -1) {
                    val radioButton = binding.root.findViewById<RadioButton>(checkedId)
                    keyValueMap["date"] = mutableListOf(radioButton.text.toString())
                }
            }
        }

        // Save the current values
        preferences.saveSortAndFilterValues(
            sortOption = getCurrentSortOption(),
            dateFilter = getCurrentDateFilter(),
            paymentModes = getCurrentPaymentModes(),
            customStartDate = getCustomStartDate(),
            customEndDate = getCustomEndDate()
        )

        onApplyClickListener?.onApplyClick(keyValueMap)
    }

    private fun loadSavedValues() {
        // Load and set the saved values
        val sortOption = preferences.getSortOption()
        val dateFilter = preferences.getDateFilter()
        val paymentModes = preferences.getPaymentModes()
        val customStartDate = preferences.getCustomStartDate()
        val customEndDate = preferences.getCustomEndDate()

        // Set the values to your views
        when (sortOption) {
            "Date" -> binding.chipGroupSort.check(R.id.chipDate)
            "Amount" -> binding.chipGroupSort.check(R.id.chipAmount)
            "Transactor" -> binding.chipGroupSort.check(R.id.chipTransactor)
        }

        // Set date filter
        when {
            dateFilter.contains(" - ") -> {
                binding.customRangeLink.text = dateFilter
                dateArrayHasDate = true
                dateRangeArrayList.clear()
                dateRangeArrayList.add(dateFilter)
                radioGroup?.clearCheck()
            }
            else -> when (dateFilter) {
                "Today" -> {
                    binding.radioToday.isChecked = true
                    dateArrayHasDate = false
                    dateRangeArrayList.clear()
                }
                "This Week" -> {
                    binding.radioThisWeek.isChecked = true
                    dateArrayHasDate = false
                    dateRangeArrayList.clear()
                }
                "This Month" -> {
                    binding.radioThisMonth.isChecked = true
                    dateArrayHasDate = false
                    dateRangeArrayList.clear()
                }
                "Last Month" -> {
                    binding.radioLastMonth.isChecked = true
                    dateArrayHasDate = false
                    dateRangeArrayList.clear()
                }
                "Last Six Months" -> {
                    binding.radioLastSixMonths.isChecked = true
                    dateArrayHasDate = false
                    dateRangeArrayList.clear()
                }
                else -> {
                    binding.radioAnyTime.isChecked = true
                    dateArrayHasDate = false
                    dateRangeArrayList.clear()
                }
            }
        }

        // Set payment modes
        binding.chipCash.isChecked = paymentModes.contains("Cash")
        binding.chipMpesa.isChecked = paymentModes.contains("M-Pesa")
    }

    private fun getCurrentSortOption(): String {
        val checkedChipIds = binding.chipGroupSort.checkedChipIds
        return if (checkedChipIds.isNotEmpty()) {
            val chip = binding.chipGroupSort.findViewById<Chip>(checkedChipIds.first())
            chip?.text?.toString() ?: "Date"
        } else {
            "Date"
        }
    }

    private fun getCurrentDateFilter(): String {
        return if (dateArrayHasDate) {
            dateRangeArrayList.first()
        } else {
            val checkedRadioId = radioGroup?.checkedRadioButtonId
            checkedRadioId?.let {
                val radioButton = binding.root.findViewById<RadioButton>(it)
                radioButton.text.toString()
            } ?: "Any Time"
        }
    }

    private fun getCurrentPaymentModes(): List<String> {
        val checkedChipIds = binding.chipGroupPaymentMode.checkedChipIds
        return checkedChipIds.map { chipId ->
            val chip = binding.chipGroupPaymentMode.findViewById<Chip>(chipId)
            chip?.text?.toString() ?: ""
        }
    }

    private fun getCustomStartDate(): String? {
        return if (dateArrayHasDate) {
            dateRangeArrayList.first()
        } else {
            null
        }
    }

    private fun getCustomEndDate(): String? {
        return if (dateArrayHasDate) {
            dateRangeArrayList.last()
        } else {
            null
        }
    }

    interface OnApplyClickListener {
        fun onApplyClick(keyValueMap: Map<String, List<String>>)
    }

    fun setOnApplyClickListener(listener: OnApplyClickListener) {
        this.onApplyClickListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            window.requestFeature(Window.FEATURE_NO_TITLE)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes?.windowAnimations = R.style.FullscreenDialogAnimation
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 