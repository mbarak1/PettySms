package com.example.pettysms

import android.app.Dialog
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.pettysms.databinding.FragmentSortAndFilterTrucksBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SortAndFilterTrucks : DialogFragment(){
    private lateinit var ownerList: List<Owner>
    private lateinit var modelList: List<String>
    private lateinit var chipGroupOwners: ChipGroup
    private lateinit var chipGroupModel: ChipGroup

    private var _binding: FragmentSortAndFilterTrucksBinding? = null
    private val binding get() = _binding!!
    private var onApplyClickListener: SortAndFilterTrucks.OnApplyClickListener? = null
    private var db_helper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var keyValueMap = mutableMapOf<String, MutableList<String>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PettySMS)

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
        db_helper = DbHelper(requireContext())
        db = db_helper?.writableDatabase
        chipGroupOwners = binding.chipGroupOwner
        chipGroupModel = binding.chipGroupModel

        ownerList = getOwnerList(db_helper!!)
        modelList = getModelList(db_helper!!)

        fillOwnerChips(ownerList, inflater)
        fillModelChips(modelList, inflater)

        return _binding?.root
    }

    private fun fillModelChips(modelList: List<String>, inflater: LayoutInflater) {
        val models = this.modelList.map { it.capitalize() }

        for (text in models) {
            val chip = inflater.inflate(R.layout.chip_style, chipGroupModel, false) as Chip
            chip.text = text
            chip.isClickable = true
            chip.isCheckable = true
            chipGroupModel.addView(chip)
        }
    }

    private fun getModelList(dbHelper: DbHelper): List<String> {
        return db_helper!!.getTruckUniqueModelStrings()
    }

    private fun fillOwnerChips(ownerList: List<Owner>, inflater: LayoutInflater) {
        val ownerCodes = this.ownerList.map { it.ownerCode?.capitalize() }

        for (text in ownerCodes) {
            val chip = inflater.inflate(R.layout.chip_style, chipGroupOwners, false) as Chip
            chip.text = text
            chip.isClickable = true
            chip.isCheckable = true
            chipGroupOwners.addView(chip)
        }

    }

    private fun getOwnerList(db_helper: DbHelper): List<Owner> {
        return db_helper.getAllOwners()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbarSortAndFilter
        toolbar.title = "Sort and Filter Trucks"
        toolbar.setNavigationOnClickListener { v: View? -> dismiss() }



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