package com.example.pettysms

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.pettysms.databinding.FragmentSortFilterDialogBinding
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Locale

class SortFilterDialogFragment : DialogFragment() {
    private var _binding: FragmentSortFilterDialogBinding? = null
    private val binding get() = _binding!!
    private var radioGroup: RadioGroup? = null

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

        // Access UI elements using binding
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbarSortAndFilter
        toolbar.title = "Sort and Filter"
        toolbar.setNavigationOnClickListener { v: View? -> dismiss() }
        var dateRangeTextView = binding.customRangeLink
        radioGroup = binding.radioGroup

        var chipGroupSort = binding.chipGroupSort
        val chipDate = binding.chipDateAll

        chipGroupSort.check(chipDate.id)

        chipGroupSort.setSelectionRequired(true)



        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuApply -> {
                    // Handle apply button click
                    true
                }
                else -> false
            }
        }

        // Add logic for UI elements and apply button click
        val resetButton: Button = binding.btnResetAll
        resetButton.setOnClickListener {
            // Handle apply button click
        }

        dateRangeTextView.setOnClickListener {
            showDateRangePicker(dateRangeTextView)
        }

        // Set the default selection by checking a specific radio button
        val defaultRadioButtonId = R.id.radio_any_time
        radioGroup?.check(defaultRadioButtonId)

        // Set an OnCheckedChangeListener for the RadioGroup
        radioGroup?.setOnCheckedChangeListener { _, checkedId ->

            println("Hala Hala: " + checkedId)

            // Handle the checkedId as needed
            // You can add additional logic based on the selected radio button if needed
            when (checkedId) {
                binding.radioAnyTime.id -> {
                    // Handle option 1
                }
                binding.radioToday.id -> {
                    // Handle option 2
                }
                binding.radioThisWeek.id -> {
                    // Handle option 2
                }
                binding.radioThisMonth.id -> {
                    // Handle option 2
                }
                binding.radioLastMonth.id -> {
                    // Handle option 2
                }
                binding.radioLastSixMonths.id -> {
                    // Handle option 2
                }
                // Add more cases for each radio button
            }

            if (checkedId != View.NO_ID){
                dateRangeTextView.text = "Select Custom Range"
            }




        }

        setHasOptionsMenu(true)

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
        return dialog
    }

    private fun showDateRangePicker(dateRangeTextView: TextView) {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            // Extract the selected date range
            val startDate = selection.first
            val endDate = selection.second

            // Format the dates as needed
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val formattedStartDate = dateFormat.format(startDate)
            val formattedEndDate = dateFormat.format(endDate)



            radioGroup?.clearCheck()
            // Display the selected date range in the TextView
            dateRangeTextView.text = "$formattedStartDate - $formattedEndDate"

        }

        picker.show(parentFragmentManager, picker.toString())
    }
}