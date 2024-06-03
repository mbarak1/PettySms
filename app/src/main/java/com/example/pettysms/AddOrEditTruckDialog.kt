package com.example.pettysms

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import cdflynn.android.library.checkview.CheckView
import com.example.pettysms.databinding.FragmentAddOrEditTruckDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

interface ViewPagerChangeListener {
    fun onPageChanged(position: Int)
}
class AddOrEditTruckDialog : DialogFragment(), ViewPagerChangeListener {
    private lateinit var toolbar: Toolbar
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: TruckModelImageAdapter
    private lateinit var dotIndicator: WormDotsIndicator
    private lateinit var ownerList: List<Owner>
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var truckNumberEditText: TextInputEditText
    private lateinit var truckNumberInputLayout: TextInputLayout
    private lateinit var saveButton: Button
    private lateinit var successfulDialog: AlertDialog
    private lateinit var saveErrorDialog: AlertDialog
    private lateinit var activeStatusLinearLayout: LinearLayout
    private lateinit var statusSwitch: MaterialSwitch

    private var onAddTruckListener: OnAddTruckListener? = null
    private var _binding: FragmentAddOrEditTruckDialogBinding? = null
    private var dbHelper1: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var truck: Truck? = null
    private var truckNoValidity = false
    private var truckExists = false
    private var selectedPosition: Int = 0
    private var functionality: String? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Enable options menu in fragment
        setStyle(STYLE_NORMAL, R.style.PrefsTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        dialog.window?.attributes?.windowAnimations = R.style.FullscreenDialogAnimationAddOrEdit
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (functionality == "Edit") {
            inflater.inflate(R.menu.menu_truck, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.deleteTruck -> {
                deleteTruck(truck?.id)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteTruck(id: Int?) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Warning")
            .setMessage("Are you sure you want to permanently delete this truck?")
            .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
            .setNegativeButton("Dismiss") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Confirm") { dialog, which ->
                deleteTruckFromDb(id)

            }
            .show()

    }

    private fun deleteTruckFromDb(id: Int?) {
        db = dbHelper1?.writableDatabase
        db?.let { dbHelper1?.deleteTruck(it, id) }
        onAddTruckListener?.onAddTruck()
        closeDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddOrEditTruckDialogBinding.inflate(inflater, container, false)
        toolbar = binding.addTruckToolbar
        viewPager = binding.viewPagerTrucks
        dotIndicator = binding.wormDotsIndicatorTrucks
        autoCompleteTextView = binding.autoCompleteTextView
        truckNumberEditText = binding.truckNoTextField
        truckNumberInputLayout = binding.truckNoTextFieldLayout
        saveButton = binding.saveButton
        activeStatusLinearLayout = binding.activeStatusLayout
        statusSwitch = binding.statusSwitch

        dbHelper1 = DbHelper(requireContext())
        db = dbHelper1?.writableDatabase
        ownerList = getOwnerList(dbHelper1!!)

        val ownerNames = ownerList.map { it.name }


        functionality =  arguments?.getString("Action")
        val truckJson = arguments?.getString("TruckJson")
        if (truckJson != null || truckJson != "") {
            truck = Gson().fromJson(truckJson, Truck::class.java)
        }

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar.inflateMenu(R.menu.menu_truck)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "$functionality Truck"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            closeDialog()
        }

        val truckModels = listOf("Mercedes-Benz Axor", "Mercedes-Benz Actros")
        val truckImages = listOf(R.drawable.axor_flipped, R.drawable.actros_flipped)
        adapter = TruckModelImageAdapter(truckModels, truckImages, this)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                showCircularViewWithCheck()
                selectedPosition = position
                onPageChanged(position)

            }
        })

        prepareTruckNoEditText()
        prepareFormOnEdit(functionality, adapter, truckModels)


        dotIndicator.attachTo(viewPager)

        populateOwnerDropdown(ownerNames)

        saveButton.setOnClickListener {
            saveButtonAction()
        }


        // Inflate the layout for this fragment
        return binding.root
    }

    private fun prepareFormOnEdit(
        functionality: String?,
        adapter: TruckModelImageAdapter,
        truckModels: List<String>
    ) {
        if (functionality == "Edit") {
            truckNumberEditText.setText(truck?.truckNo?.let { addSpaceAfterThirdCharacter(it) })
            truckNumberEditText.isEnabled = false
            truckNoValidity = true
            autoCompleteTextView.setText(truck?.owner?.name)
            var position = findAllSubstringsIgnoreCase(truckModels.toTypedArray(), truck!!.make.toString())
            println("Edit Position: $position")
            adapter.setSelectedItemPosition(position[0])

            activeStatusLinearLayout.visibility = View.VISIBLE
            statusSwitch.isChecked = truck!!.activeStatus!!

            println("Truck Id: ${truck?.id}")


        }
    }

    fun findAllSubstringsIgnoreCase(array: Array<String>, searchString: String): List<Int> {
        return array.withIndex().filter { it.value.contains(searchString, ignoreCase = true) }.map { it.index }
    }

    private fun saveButtonAction() {
        var model = ""

        try {
            println("Selected truck Model: " + getSelectedTruckModel())
            model = getSelectedTruckModel()
        }catch (e:Exception){
            println(e.toString())
        }

        println("Selected Owner: " + getSelectedOwner().isEmpty())

        println("Model: ${model.isEmpty()}")

        println("Truck No Validity: " + truckNoValidity)

        println("is owner in list: " + isOwnerNameInList(getSelectedOwner(), ownerList))

        try {
            println("Selected Owner: " + getOwnerByName(getSelectedOwner(), ownerList)?.name)

        }catch (_: Exception){}

        var ownerInListBoolean = isOwnerNameInList(getSelectedOwner(), ownerList)

        if (truckNoValidity){
            truckExists = checkIfTruckExists(truckNumberEditText.text.toString())!!
        }


        if (model.isNotEmpty() && truckNoValidity && getSelectedOwner().isNotEmpty() && ownerInListBoolean && !truckExists){
            createSaveSuccessfulDialog()
            successfulDialog.show()
            var truckSelectedModel = "Axor"
            if(getSelectedTruckModel() == "Mercedes-Benz Actros"){
                truckSelectedModel = "Actros"
            }
            saveTruckToDb(truckNumberEditText.text.toString(), truckSelectedModel, getOwnerByName(getSelectedOwner(), ownerList))
            onAddTruckListener?.onAddTruck()
            closeDialog()
        }
        else{
            val justification = mutableListOf<String>()
            if(model.isEmpty()){
                justification.add("Truck model not selected!")
            }
            if(truckNoValidity == false){
                justification.add("Truck number not valid!")
            }
            if(getSelectedOwner().isEmpty()){
                justification.add("Truck owner not selected!")
            }else{
                if (ownerInListBoolean == false){
                    justification.add("Owner does not exist!")
                }
            }
            if (truckExists == true){
                justification.add("Truck already exists!")
            }

            if(justification.size == 1 && justification[0] == "Truck already exists!" && functionality == "Edit"){
                createSaveSuccessfulDialog()
                successfulDialog.show()
                var truckSelectedModel = "Axor"
                if(getSelectedTruckModel() == "Mercedes-Benz Actros"){
                    truckSelectedModel = "Actros"
                }

                editTruckDetails(truck, truckNumberEditText.text.toString(), truckSelectedModel, getOwnerByName(getSelectedOwner(), ownerList), statusSwitch.isChecked)
                onAddTruckListener?.onAddTruck()
                closeDialog()
                return
            }else if (functionality == "Edit"){
                justification.remove("Truck already exists!")
            }

            createSaveErrorDialog(R.drawable.baseline_warning_amber_white_24dp, "Error", justification)
            saveErrorDialog.show()
        }
        

    }

    private fun editTruckDetails(
        truck: Truck?,
        truckNo: String,
        truckSelectedModel: String,
        owner: Owner?,
        activeStatus: Boolean
    ) {
        var truckId = truck?.id
        db = dbHelper1?.writableDatabase
        db?.let { dbHelper1?.updateTruck(it, truckId, truckNo.replace(" ", ""), truckSelectedModel, owner, activeStatus) }
    }

    private fun saveTruckToDb(truckNo: String, model: String, owner: Owner?) {
        db = dbHelper1?.writableDatabase

        db?.let { dbHelper1?.insertTruck(it, Truck(id = null, truckNo = truckNo.replace(" ", ""), make = model, owner = owner, activeStatus = true)) }
    }

    private fun checkIfTruckExists(truckNo: String): Boolean? {
       return dbHelper1?.isTruckExists(truckNo.replace(" ", ""))
    }

    private fun createSaveErrorDialog(iconResId: Int, title: String, justification: List<String>) {
        // Inflate custom view for the save error dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.truck_dialog_save_error, null)

        // Find and set the icon, title, and message
        val iconImageView = customView.findViewById<ImageView>(R.id.error_icon)
        val titleTextView = customView.findViewById<TextView>(R.id.error_title)
        val detailsLinearLayout = customView.findViewById<LinearLayout>(R.id.error_details)

        iconImageView.setImageResource(iconResId)
        titleTextView.text = title

        // Populate the justification message with bullet points
        justification.forEach { justificationItem ->
            val bulletPointTextView = TextView(requireContext()).apply {
                text = "• $justificationItem"
                val textAppearance = androidx.constraintlayout.widget.R.style.TextAppearance_AppCompat_Body1
                gravity = Gravity.CENTER_HORIZONTAL
            }
            detailsLinearLayout.addView(bulletPointTextView)
        }

        // Create and customize the dialog
        saveErrorDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(true)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private fun createSaveSuccessfulDialog() {
        // Inflate custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.save_successful_dialog, null)

        // Set up loading text or any other views if needed
        val saveSuccessfullyText = customView.findViewById<TextView>(R.id.success_message_text)
        val checkView = customView.findViewById<CheckView>(R.id.check)
        saveSuccessfullyText.text = "Truck Successfully Saved!"
        checkView.check()

        // Create and customize the dialog
        successfulDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(true)
            .create()
    }

    private fun prepareTruckNoEditText() {
        // Set the input type to avoid multi-line input but allow normal text input
        truckNumberEditText.inputType = InputType.TYPE_CLASS_TEXT

        truckNumberEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val input = editable.toString()
                val isValid = isValidTruckNumber(input)
                truckNoValidity = isValid
                if (isValid) {
                    truckNumberInputLayout.error = null
                    truckNumberInputLayout.isErrorEnabled = false
                    // Show green check icon
                    truckNumberInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    truckNumberInputLayout.setEndIconDrawable(R.drawable.baseline_check_circle_black_24dp)
                    truckNumberInputLayout.boxStrokeColor = resources.getColor(R.color.new_green_color)
                } else {
                    truckNumberInputLayout.error = "Truck number must be in the format KXX 000X e.g (KCZ 811H)"
                    // Show error message
                    truckNumberInputLayout.isErrorEnabled = true
                    // Hide end icon
                    truckNumberInputLayout.setEndIconDrawable(0)
                }
            }

            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed
            }
        })

        // Set up the TextInputEditText to disable the "Enter" key
        truckNumberEditText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                // Intercept the Enter key press and do nothing
                true
            } else {
                false
            }
        }

        // Alternatively, override the onEditorAction to handle the "Enter" key
        truckNumberEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_NEXT ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                // Intercept the Enter key press and do nothing
                true
            } else {
                false
            }
        }
    }


    private fun showCircularViewWithCheck() {
    }

    private fun populateOwnerDropdown(ownerNames: List<String?>) {
        // Create an ArrayAdapter with owner names
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ownerNames)

        // Set the adapter to the AutoCompleteTextView
        autoCompleteTextView.setAdapter(adapter)

        // Set the input type to restrict multi-line input
        autoCompleteTextView.inputType = InputType.TYPE_CLASS_TEXT

        // Override the key listener to prevent line breaks
        autoCompleteTextView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                // Intercept the Enter key press and do nothing
                true
            } else {
                false
            }
        }
    }

    private fun closeDialog() {
        dismiss()
    }

    companion object

    interface OnAddTruckListener {
        fun onAddTruck()
    }

    fun setOnAddTruckListener(listener: OnAddTruckListener) {
        this.onAddTruckListener = listener
    }

    private fun getOwnerList(dbHelper: DbHelper): List<Owner> {
        return dbHelper.getAllOwners()
    }

    override fun onPageChanged(position: Int) {
        //adapter.setSelectedItemPosition(position)
    }

    private fun isValidTruckNumber(input: String): Boolean {
        val pattern = Regex("K[A-Z]{2} [0-9]{3}[A-Z]{1}")
        return pattern.matches(input)
    }

    private fun getSelectedTruckModel(): String {
        return adapter.getSelectedTruckModel()
    }
    private fun getSelectedOwner(): String {
        return autoCompleteTextView.text.toString()
    }
    private fun getOwnerByName(name: String, ownerList: List<Owner>): Owner? {
        for (owner in ownerList) {
            if (owner.name.equals(name, ignoreCase = true)) {
                return owner
            }
        }
        return null
    }

    private fun isOwnerNameInList(name: String, ownerList: List<Owner>): Boolean {
        for (owner in ownerList) {
            if (owner.name.equals(name, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun addSpaceAfterThirdCharacter(input: String): String {
        return if (input.length > 3) {
            input.substring(0, 3) + " " + input.substring(3)
        } else {
            input
        }
    }
}