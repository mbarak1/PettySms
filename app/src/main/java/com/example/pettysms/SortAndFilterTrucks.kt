package com.example.pettysms

import android.app.Dialog
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.example.pettysms.databinding.FragmentSortAndFilterTrucksBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SortAndFilterTrucks : DialogFragment(){
    private lateinit var ownerList: List<Owner>
    private lateinit var modelList: List<String>
    private lateinit var chipGroupOwners: ChipGroup
    private lateinit var chipGroupModel: ChipGroup
    private lateinit var chipGroupSort: ChipGroup
    private lateinit var resetButton: Button
    private lateinit var chipTruckNo: Chip

    private var _binding: FragmentSortAndFilterTrucksBinding? = null
    private val binding get() = _binding!!
    private var onApplyClickListener: SortAndFilterTrucks.OnApplyClickListener? = null
    private var dbHelper1: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var keyValueMap = mutableMapOf<String, MutableList<String>>()
    private var sortArrayList = mutableListOf<String>()
    private var makeArrayList = mutableListOf<String>()
    private var ownerArrayList = mutableListOf<String>()





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.PrefsTheme)
        arguments?.let {
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        dialog.window?.attributes?.windowAnimations = R.style.FullscreenDialogAnimation
        return dialog
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
        // Inflate the layout for this fragment
        _binding = FragmentSortAndFilterTrucksBinding.inflate(inflater, container, false)
        dbHelper1 = DbHelper(requireContext())
        val mapFromActivity = arguments?.getSerializable("yourMapKey") as? MutableMap<String, MutableList<String>>
        db = dbHelper1?.writableDatabase
        chipGroupOwners = binding.chipGroupOwner
        chipGroupModel = binding.chipGroupModel
        chipGroupSort = binding.chipGroupSort
        resetButton = binding.btnResetAll
        chipTruckNo = binding.chipTruckNo

        ownerList = getOwnerList(dbHelper1!!)
        modelList = getModelList(dbHelper1!!)

        fillOwnerChips(ownerList, inflater)
        fillModelChips(modelList, inflater)

        if (!mapFromActivity.isNullOrEmpty()){
            println("Imetoka kwa activity: " + mapFromActivity)
            fillSortAndFilterForm(mapFromActivity)
        }

        return _binding?.root
    }

    private fun fillSortAndFilterForm(mapFromActivity: MutableMap<String, MutableList<String>>) {
        mapFromActivity.forEach { (key, value) ->
            if (key == "sort") {

                // Iterate through each sort criteria in the list
                value.forEach { criteria ->
                    // Find the Chip with text equal to the sort criteria
                    val chipToCheck = chipGroupSort.children
                        .filterIsInstance<Chip>()
                        .firstOrNull { it.text.toString() == criteria }

                    // Check the found chip
                    chipToCheck?.isChecked = true

                }

            }else if(key == "make") {

                // Iterate through each sort criteria in the list
                value.forEach { criteria ->
                    // Find the Chip with text equal to the sort criteria
                    val chipToCheck = chipGroupModel.children
                        .filterIsInstance<Chip>()
                        .firstOrNull { it.text.toString() == criteria }

                    // Check the found chip
                    chipToCheck?.isChecked = true

                }
            }else if(key == "owner") {

                // Iterate through each sort criteria in the list
                value.forEach { criteria ->
                    // Find the Chip with text equal to the sort criteria
                    val chipToCheck = chipGroupOwners.children
                        .filterIsInstance<Chip>()
                        .firstOrNull { it.text.toString() == criteria }

                    // Check the found chip
                    chipToCheck?.isChecked = true

                }
            }

        }

    }

    private fun fillModelChips(modelList: List<String>, inflater: LayoutInflater) {
        val models = this.modelList.map { it.replaceFirstChar(Char::titlecase) }

        for (text in models) {
            val chip = inflater.inflate(R.layout.chip_style, chipGroupModel, false) as Chip
            chip.text = text
            chip.isClickable = true
            chip.isCheckable = true
            chipGroupModel.addView(chip)
        }
    }

    private fun getModelList(dbHelper: DbHelper): List<String> {
        return dbHelper1!!.getTruckUniqueModelStrings()
    }

    private fun fillOwnerChips(ownerList: List<Owner>, inflater: LayoutInflater) {
        val ownerNames = this.ownerList.map {
            it.name?.split(" ")?.let { words ->
                val firstWord = words.getOrNull(0)?.replaceFirstChar(Char::titlecase) ?: ""
                val secondWord = words.getOrNull(1)?.take(3)?.replaceFirstChar(Char::titlecase) ?: ""
                "$firstWord $secondWord."
            }
        }
        for (text in ownerNames) {
            val chip = inflater.inflate(R.layout.chip_style, chipGroupOwners, false) as Chip
            chip.text = text
            chip.isClickable = true
            chip.isCheckable = true
            chipGroupOwners.addView(chip)
        }

    }

    private fun getOwnerList(dbHelper: DbHelper): List<Owner> {
        return dbHelper.getAllOwners()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbarSortAndFilter
        toolbar.title = "Sort and Filter Trucks"
        toolbar.setNavigationOnClickListener { v: View? -> dismiss() }

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

        resetButton.setOnClickListener {
            showResetDialog()
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Warning")
            .setMessage("Are you sure you want to reset your sort and filter criteria?")
            .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
            .setNegativeButton("Dismiss") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Confirm") { dialog, which ->
                // Respond to positive button press
                for (i in 0 until chipGroupModel.childCount) {
                    val chip = chipGroupModel.getChildAt(i) as Chip
                    chip.isChecked = false
                }

                for (i in 0 until chipGroupOwners.childCount) {
                    val chip = chipGroupOwners.getChildAt(i) as Chip
                    chip.isChecked = false
                }

                // Iterate through each chip in the ChipGroup
                for (i in 0 until chipGroupSort.childCount) {
                    val chip = chipGroupSort.getChildAt(i) as Chip
                    // Check if the current chip is the one to keep selected
                    chip.isChecked = chip == chipTruckNo
                }

            }
            .show()
    }

    private fun collectSortAndFilterData() {

        for (i in 0 until chipGroupSort.childCount) {
            val chip = chipGroupSort.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked) {
                sortArrayList.add(chip.text.toString())
            }
        }

        keyValueMap["sort"] = sortArrayList

        for (i in 0 until chipGroupModel.childCount) {
            val chip = chipGroupModel.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked) {
                makeArrayList.add(chip.text.toString())
            }
        }

        keyValueMap["make"] = makeArrayList

        for (i in 0 until chipGroupOwners.childCount) {
            val chip = chipGroupOwners.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked) {
                ownerArrayList.add(chip.text.toString())
            }
        }

        keyValueMap["owner"] = ownerArrayList

        println("Sort Array: " + sortArrayList.toString() + "Make Array: " + makeArrayList.toString() + "Owner List: " + ownerArrayList.toString())


        sendKeyValueMapToActivity()


    }

    private fun closeDialog() {
        dismiss()
    }

    companion object {
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