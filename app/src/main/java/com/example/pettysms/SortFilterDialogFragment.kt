package com.example.pettysms

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.example.pettysms.databinding.FragmentSortFilterDialogBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

class SortFilterDialogFragment : DialogFragment() {
    private var _binding: FragmentSortFilterDialogBinding? = null
    private val binding get() = _binding!!
    private var radioGroup: RadioGroup? = null
    private var sortArrayList = mutableListOf<String>()
    private var transactionTypeArrayList = mutableListOf<String>()
    private var dateRangeArrayList = mutableListOf<String>()
    private var keyValueMap = mutableMapOf<String, MutableList<String>>()
    private var onApplyClickListener: OnApplyClickListener? = null

    private var dateArrayHasDate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PettySMS)

    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSortFilterDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFromActivity = arguments?.getSerializable("yourMapKey") as? MutableMap<String, MutableList<String>>

        // Access UI elements using binding
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbarSortAndFilter
        toolbar.title = "Sort and Filter"
        toolbar.setNavigationOnClickListener { v: View? -> dismiss() }
        var dateRangeTextView = binding.customRangeLink
        radioGroup = binding.radioGroup

        var chipGroupSort = binding.chipGroupSort
        var chipGroupTransactionType = binding.chipGroupTransactionType
        val chipDate = binding.chipDate

        val defaultRadioButtonId = R.id.radio_any_time

        chipGroupSort.setSelectionRequired(true)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuApply -> {
                    // Handle apply button click
                    collectSortAndFilterData()
                    closeDialog()
                    true
                }
                else -> false
            }
        }

        // Add logic for UI elements and apply button click
        val resetButton: Button = binding.btnResetAll


        dateRangeTextView.setOnClickListener {
            showDateRangePicker(dateRangeTextView)
        }


        // Set an OnCheckedChangeListener for the RadioGroup
        radioGroup?.setOnCheckedChangeListener { _, checkedId ->

            var choice = "None"

            println("CheckedID: " + checkedId)

            println("Date Array in the beginning: " + dateRangeArrayList)

            println("has date bool" + dateArrayHasDate)

            if (checkedId != -1 && !dateArrayHasDate){
                choice = binding.root.findViewById<RadioButton>(checkedId).text.toString()
            }

            println("Choice in the beginning" + choice)

            // Handle the checkedId as needed
            // You can add additional logic based on the selected radio button if needed
            if(!dateArrayHasDate){
                println("Hello")
                when (checkedId) {
                    binding.radioAnyTime.id -> {
                        choice = binding.radioAnyTime.text.toString()
                    }
                    binding.radioToday.id -> {
                        choice = binding.radioToday.text.toString()
                    }
                    binding.radioThisWeek.id -> {
                        choice = binding.radioThisWeek.text.toString()
                    }
                    binding.radioThisMonth.id -> {
                        choice = binding.radioThisMonth.text.toString()
                    }
                    binding.radioLastMonth.id -> {
                        choice = binding.radioLastMonth.text.toString()
                    }
                    binding.radioLastSixMonths.id -> {
                        choice = binding.radioLastSixMonths.text.toString()
                    }
                    // Add more cases for each radio button
                }
            }else{
                dateArrayHasDate = false
            }

            if (checkedId != View.NO_ID){
                dateRangeTextView.text = "Select Custom Range"
            }

            if(choice != "None"){
                dateRangeArrayList.clear()
                dateRangeArrayList.add(choice)
            }

            println(choice)
            println("Date Array list: " + dateRangeArrayList.toString())




        }


        resetButton.setOnClickListener {
            // Handle reset all button click
            // Iterate through each chip in the ChipGroup and set checked state to false

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Warning")
                .setMessage("Are you sure you want to reset your sort and filter criteria?")
                .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
                .setNegativeButton("Dismiss") { dialog, which ->
                    // Respond to negative button press
                }
                .setPositiveButton("Confirm") { dialog, which ->
                    // Respond to positive button press
                    for (i in 0 until chipGroupTransactionType.childCount) {
                        val chip = chipGroupTransactionType.getChildAt(i) as Chip
                        chip.isChecked = false
                    }

                    // Iterate through each chip in the ChipGroup
                    for (i in 0 until chipGroupSort.childCount) {
                        val chip = chipGroupSort.getChildAt(i) as Chip

                        // Check if the current chip is the one to keep selected
                        chip.isChecked = chip == chipDate
                    }

                    radioGroup?.check(defaultRadioButtonId)
                }
                .show()

        }

        setHasOptionsMenu(true)
        if (!mapFromActivity.isNullOrEmpty()){
            println("Imetoka kwa activity: " + mapFromActivity)
            fillSortAndFilterForm(mapFromActivity)
        }else{
            chipGroupSort.check(chipDate.id)
            // Set the default selection by checking a specific radio button
            radioGroup?.check(defaultRadioButtonId)
            dateRangeArrayList.add(binding.root.findViewById<RadioButton>(defaultRadioButtonId).text.toString())
        }

    }

    private fun fillSortAndFilterForm(mapFromActivity: MutableMap<String, MutableList<String>>) {
        mapFromActivity.forEach { (key, value) ->
            if (key == "sort"){
                val chipGroup: ChipGroup = binding.chipGroupSort // Replace with your actual ChipGroup ID

                // Iterate through each sort criteria in the list
                value.forEach { criteria ->
                    // Find the Chip with text equal to the sort criteria
                    val chipToCheck = chipGroup.children
                        .filterIsInstance<Chip>()
                        .firstOrNull { it.text.toString() == criteria }

                    // Check the found chip
                    chipToCheck?.isChecked = true

                }

            }
            else if(key == "transaction_type") {
                val chipGroup: ChipGroup = binding.chipGroupTransactionType // Replace with your actual ChipGroup ID

                // Iterate through each sort criteria in the list
                value.forEach { criteria ->
                    // Find the Chip with text equal to the sort criteria
                    val chipToCheck = chipGroup.children
                        .filterIsInstance<Chip>()
                        .firstOrNull { it.text.toString() == criteria }

                    // Check the found chip
                    chipToCheck?.isChecked = true

                }
            }
            else if(key == "date") {
                val radioGroup: RadioGroup = binding.radioGroup
                val radioButtonId = when (value.first().toString()) {
                    "Any Time" -> R.id.radio_any_time
                    "Today" -> R.id.radio_today
                    "This Week" -> R.id.radio_this_week
                    "This Month" -> R.id.radio_this_month
                    "Last Month" -> R.id.radio_last_month
                    "Last Six Months" -> R.id.radio_last_six_months
                    else -> -1 // If the value doesn't match any RadioButton, set to -1
                }

                if (radioButtonId != -1) {
                    val radioButton: RadioButton = binding.root.findViewById(radioButtonId)
                    radioButton.isChecked = true
                }else{
                    val dateRangeTextView = binding.customRangeLink
                    dateArrayHasDate = true
                    dateRangeArrayList.clear()
                    dateRangeArrayList.add(value.first().toString())
                    dateRangeTextView.text = value.first().toString()

                }
            }
            println("Key: $key, Value: $value")
        }



    }

    private fun collectSortAndFilterData() {

        keyValueMap["date"] = dateRangeArrayList

        val sortChipGroup = binding.chipGroupSort

        for (i in 0 until sortChipGroup.childCount) {
            val chip = sortChipGroup.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked) {
                sortArrayList.add(chip.text.toString())
            }
        }

        keyValueMap["sort"] = sortArrayList

        val transactionTypeChipGroup = binding.chipGroupTransactionType

        for (i in 0 until transactionTypeChipGroup.childCount) {
            val chip = transactionTypeChipGroup.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked) {
                transactionTypeArrayList.add(chip.text.toString())
            }
        }

        keyValueMap["transaction_type"] = transactionTypeArrayList

        println("Sort Array: " + sortArrayList.toString() + "Transaction Type Array: " + transactionTypeArrayList.toString() + "Date Range List: " + dateRangeArrayList.toString())

        sendKeyValueMapToActivity()

    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_sort_filter, menu)

        // Find the menu item you want to customize
        val menuItemApply = menu.findItem(R.id.menuApply)

        // Set a custom action view for the menu item
        menuItemApply?.actionView = layoutInflater.inflate(R.layout.custom_layout_menu_sort_and_filter, null)

        // Customize the TextView within the custom layout
        val menuItemText = menuItemApply?.actionView?.findViewById<TextView>(R.id.menuItemText)
        menuItemText?.text = menuItemApply?.title
        // menuItemText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.your_custom_color))

        super.onCreateOptionsMenu(menu, inflater)
    }



    // The system calls this only when creating the layout in a dialog.
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using
        // onCreateView() is to modify the dialog characteristics. For example,
        // the dialog includes a title by default, but your custom layout might
        // not need it. Here, you can remove the dialog title, but you must
        // call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        dialog.window?.attributes?.windowAnimations = R.style.FullscreenDialogAnimation
        return dialog
    }

    // Function to close the DialogFragment
    private fun closeDialog() {
        dismiss()
    }

    private fun showDateRangePicker(dateRangeTextView: TextView) {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            // Extract the selected date range
            val startDate = selection.first
            val endDate = selection.second

            // Format the dates as needed
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedStartDate = dateFormat.format(startDate)
            val formattedEndDate = dateFormat.format(endDate)
            val dateRangeString = "$formattedStartDate - $formattedEndDate"

            dateArrayHasDate = true

            dateRangeArrayList.clear()
            dateRangeArrayList.add(dateRangeString)

            radioGroup?.clearCheck()
            // Display the selected date range in the TextView
            dateRangeTextView.text = dateRangeString

            println("Date List in Function: " + dateRangeArrayList.toString())


        }

        picker.show(parentFragmentManager, picker.toString())
    }

    interface OnApplyClickListener {
        fun onApplyClick(keyValueMap: Map<String, List<String>>)
    }

    // Your fragment code

    private fun sendKeyValueMapToActivity() {
        onApplyClickListener?.onApplyClick(keyValueMap)
    }

    fun setOnApplyClickListener(listener: OnApplyClickListener) {
        this.onApplyClickListener = listener
    }



}