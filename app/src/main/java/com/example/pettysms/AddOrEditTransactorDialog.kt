package com.example.pettysms

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import cdflynn.android.library.checkview.CheckView
import com.example.pettysms.databinding.FragmentAddOrEditTransactorDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import xyz.schwaab.avvylib.AvatarView
import java.io.ByteArrayOutputStream
import kotlin.random.Random


class AddOrEditTransactorDialog : DialogFragment() {

    private var transactorNameTextView: TextInputEditText? = null
    private var transactorTypeTextView: AutoCompleteTextView? = null
    private var transactorIdTextView: TextInputEditText? = null
    private var transactorPhoneNoTextView: TextInputEditText? = null
    private var transactorAddressTextView: TextInputEditText? = null
    private var transactorKraPinTextView: TextInputEditText? = null
    private var idImageView: ImageView? = null
    private var phoneImageView: ImageView? = null
    private var nameImageView: ImageView? = null
    private var transactorTypeImageView: ImageView? = null
    private var profileImageAvatar: AvatarView? = null
    private var profileImageImageButton: ImageButton? = null
    private var successfulDialog: AlertDialog? = null
    private var deleteSuccessfulDialog: AlertDialog? = null
    private var saveButton: Button? = null


    private var functionality: String? = null
    private var onAddTransactorListener: AddOrEditTransactorDialog.OnAddTransactorListener? = null
    private var _binding: FragmentAddOrEditTransactorDialogBinding? = null
    private var transactor: Transactor? = null
    private var justifications: MutableList<String> = mutableListOf()
    private var nameValidation: Boolean = false
    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var avatarColor: String? = getRandomAvatarColor()
    private var phoneNumberValidation: Boolean = true
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            profileImageAvatar?.setImageBitmap(imageBitmap)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            profileImageAvatar?.setImageBitmap(bitmap)
        }
    }


    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Enable options menu in fragment
        setStyle(STYLE_NORMAL, R.style.PrefsTheme)
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
        println("Functionality: $functionality")
        if (functionality == "Edit") {
            inflater.inflate(R.menu.menu_transactor, menu)
            val deleteItem = menu.findItem(R.id.deleteTransactor)

            println("Transactor imported Flagged: ${transactor?.isImported}")

            if (transactor?.isImported == true) {
                deleteItem.isEnabled = false
                deleteItem.icon?.mutate()?.let {
                    it.alpha = 130 // Adjust alpha to grey out the icon
                    deleteItem.icon = it
                }

                transactorNameTextView?.isEnabled = false
                nameImageView?.drawable?.mutate()?.let {
                    it.alpha = 130 // Adjust alpha to grey out the icon
                }

                transactorTypeTextView?.let { disableAutoCompleteTextView(it) }
                transactorTypeImageView?.drawable?.mutate()?.let {
                    it.alpha = 130 // Adjust alpha to grey out the icon
                }

                transactorPhoneNoTextView?.isEnabled = false
                phoneImageView?.drawable?.mutate()?.let {
                    it.alpha = 130 // Adjust alpha to grey out the icon
                }

            } else {
                deleteItem.isEnabled = true
            }
        }
    }

    private fun createDeleteSuccessfulDialog() {
        // Inflate custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.save_successful_dialog, null)

        // Set up loading text or any other views if needed
        val saveSuccessfullyText = customView.findViewById<TextView>(R.id.success_message_text)
        val checkView = customView.findViewById<CheckView>(R.id.check)
        saveSuccessfullyText.text = "Transactor Successfully Deleted!"
        checkView.check()

        // Create and customize the dialog
        deleteSuccessfulDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(true)
            .create()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.deleteTransactor -> {
                if (item.isEnabled) {
                    deleteTransactor(transactor)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteTransactor(transactor: Transactor?) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Warning")
            .setMessage("Are you sure you want to delete this transactor?")
            .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
            .setNegativeButton("Dismiss") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Confirm") { dialog, which ->
                db = dbHelper?.writableDatabase
                transactor?.let { dbHelper?.deleteTransactor(it) }
                createDeleteSuccessfulDialog()
                deleteSuccessfulDialog?.show()
                transactor?.let { onAddTransactorListener?.onAddTransactor(it) }
                closeDialog()

            }
            .show()

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddOrEditTransactorDialogBinding.inflate(inflater, container, false)
        functionality =  arguments?.getString("Action")
        transactorNameTextView = binding.transactorNameTextField
        transactorTypeTextView = binding.autoCompleteTextView
        transactorIdTextView = binding.transactorIdTextField
        transactorPhoneNoTextView = binding.transactorPhoneNumberTextField
        transactorAddressTextView = binding.transactorAddressTextField
        transactorKraPinTextView = binding.transactorKraPinTextField
        idImageView = binding.idImageView
        phoneImageView = binding.phoneImageView
        nameImageView = binding.nameImageView
        transactorTypeImageView = binding.transactorTypeImageView
        profileImageAvatar = binding.avatarView
        profileImageImageButton = binding.logoImageButton
        dbHelper = DbHelper(requireContext())
        db = dbHelper?.writableDatabase
        saveButton = binding.saveButton

        if (functionality == "Add") {
            profileImageAvatar?.highlightBorderColorEnd = avatarColor?.let { getColorInt(it) }!!
        }

        val toolbar = binding.addOrEditTransactorToolbar
        val transactorJson = arguments?.getString("TransactorJson")

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar.inflateMenu(R.menu.menu_transactor)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "$functionality Transactor"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            closeDialog()
        }



        if (transactorJson != null || transactorJson != "") {
            transactor = Gson().fromJson(transactorJson, Transactor::class.java)
        }


        formControl()

        if(functionality == "Edit"){
            fillForm(transactor)
        }

        return binding.root
    }

    private fun fillForm(transactor: Transactor?) {

        transactorNameTextView?.setText(Transactor.formatName(transactor?.name))
        transactorTypeTextView?.setText(transactor?.transactorType)
        transactorKraPinTextView?.setText(transactor?.kraPin)
        if (transactor?.idCard.toString() == "null" || transactor?.idCard.toString() == "0"){
            transactorIdTextView?.setText("")
        }
        else{
            transactorIdTextView?.setText(transactor?.idCard.toString())
        }
        transactorPhoneNoTextView?.setText(transactor?.phoneNumber)
        transactorAddressTextView?.setText(transactor?.address)
        profileImageAvatar?.highlightBorderColorEnd = transactor?.avatarColor?.let { getColorInt(it) }!!
        if (!transactor.transactorProfilePicturePath.isNullOrEmpty()){
            profileImageAvatar?.let { setImageViewFromBase64(it, transactor.transactorProfilePicturePath!!) }
        }

        val transactorType = transactor.transactorType ?: "Individual"
        transactorTypeTextView?.setText(transactorType, false)

        // Call the same method to handle transactor type selection logic
        handleTransactorTypeSelection(transactorType)
    }

    fun setImageViewFromBase64(imageView: ImageView, base64String: String) {
        // Decode the Base64 string into a byte array
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)

        // Convert the byte array into a Bitmap
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        // Set the Bitmap to the ImageView
        imageView.setImageBitmap(decodedByte)
    }

    private fun formControl() {
        initializeTransactorNameTextInput()
        initializeTransactorTypeAutoCompleteTextInput()
        initializeProfileImage()
        saveButton?.setOnClickListener {
            checkValidation()
        }
    }

    private fun checkValidation() {
        nameValidation = nameValidation()
        if (transactorTypeTextView?.text.toString() == "Individual"){
            phoneNumberValidation = phoneNumberValidation()
            if(nameValidation && phoneNumberValidation){
                if (functionality != "Edit"){
                    saveTransactor()
                }else{
                    updateTransactor()
                }
            }
        }
        else if (transactorTypeTextView?.text.toString() == "Corporate"){
            if(nameValidation){
                if (functionality != "Edit"){
                    saveTransactor()
                }else{
                    updateTransactor()
                }
            }

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        transactorNameTextView = null
        transactorTypeTextView = null
        transactorIdTextView = null
        transactorPhoneNoTextView = null
        transactorAddressTextView = null
        transactorKraPinTextView = null
        idImageView = null
        phoneImageView = null
        nameImageView = null
        transactorTypeImageView = null
        profileImageAvatar = null
        profileImageImageButton = null
        saveButton = null

        // Nullify dialogs if needed
        successfulDialog = null
        deleteSuccessfulDialog = null

        // Nullify views and related properties
        _binding = null
        functionality = null
        transactor = null
        onAddTransactorListener = null

        // Clear mutable list
        justifications.clear()

        // Reset validation flags
        nameValidation = false
        phoneNumberValidation = true

        // Close and nullify database resources
        db?.close()
        db = null
        dbHelper = null

        // Reset color and clean up resources if needed
        avatarColor = null
    }

    private fun updateTransactor() {
        if (transactor?.name != transactorNameTextView?.text.toString()){
            transactor?.name = transactorNameTextView?.text.toString()
        }
        if (transactor?.transactorType != transactorTypeTextView?.text.toString()){
            transactor?.transactorType = transactorTypeTextView?.text.toString()
        }
        if (transactorIdTextView?.text.toString() != "" && transactor?.idCard != transactorIdTextView?.text.toString().toInt()){
            transactor?.idCard = transactorIdTextView?.text.toString().toInt()
        }
        if (transactor?.phoneNumber != transactorPhoneNoTextView?.text.toString()){
            transactor?.phoneNumber = transactorPhoneNoTextView?.text.toString()
        }
        if (transactor?.address != transactorAddressTextView?.text.toString()){
            transactor?.address = transactorAddressTextView?.text.toString()
        }
        if (transactor?.kraPin != transactorKraPinTextView?.text.toString()){
            transactor?.kraPin = transactorKraPinTextView?.text.toString()
        }
        val hasProfilePic = profileImageAvatar?.let { avatarViewHasBitmap(it) }
        if (hasProfilePic == true){
            if (transactor?.transactorProfilePicturePath != bitmapToBase64(profileImageAvatar?.let {
                    getBitmapFromAvatarView(
                        it
                    )
                }!!)){
                transactor?.transactorProfilePicturePath = bitmapToBase64(getBitmapFromAvatarView(
                    profileImageAvatar!!
                )!!)
            }
        }

        createSaveSuccessfulDialog()
        successfulDialog?.show()

        updateTransactorInDb(transactor)
        transactor?.let { onAddTransactorListener?.onAddTransactor(it) }
        closeDialog()

    }

    private fun updateTransactorInDb(transactor: Transactor?) {
        db = dbHelper?.writableDatabase
        db?.let {
            if (transactor != null) {
                dbHelper?.updateTransactor(it, transactor)
            }
        }
    }


    private fun saveTransactor() {
        val hasProfilePic = profileImageAvatar?.let { avatarViewHasBitmap(it) }
        var profilePicString: String = "N/A"

        var transactor = Transactor(id = null, name = transactorNameTextView?.text.toString(), transactorType = transactorTypeTextView?.text.toString(), idCard = null, phoneNumber = null, avatarColor = avatarColor )
        println("Has profile pic: " + hasProfilePic)
        if (hasProfilePic == true){
            transactor.transactorProfilePicturePath = bitmapToBase64(profileImageAvatar?.let {
                getBitmapFromAvatarView(
                    it
                )
            }!!)
        }

        if(transactorTypeTextView?.text.toString() == "Individual"){
            transactor.idCard = transactorIdTextView?.text.toString().toInt()
            transactor.phoneNumber = transactorPhoneNoTextView?.text.toString()
        }

        if (!transactorTypeTextView?.text.isNullOrEmpty()){
            transactor.address = transactorAddressTextView?.text.toString()
        }

        createSaveSuccessfulDialog()
        successfulDialog?.show()

        saveTransactorToDb(transactor)
        onAddTransactorListener?.onAddTransactor(transactor)
        closeDialog()



    }

    interface OnAddTransactorListener {
        fun onAddTransactor(transactor: Transactor)
    }

    fun setOnAddTransactorListener(listener: OnAddTransactorListener) {
        this.onAddTransactorListener = listener

    }

    private fun createSaveSuccessfulDialog() {
        // Inflate custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.save_successful_dialog, null)

        // Set up loading text or any other views if needed
        val saveSuccessfullyText = customView.findViewById<TextView>(R.id.success_message_text)
        val checkView = customView.findViewById<CheckView>(R.id.check)
        saveSuccessfullyText.text = "Transactor Successfully Saved!"
        checkView.check()

        // Create and customize the dialog
        successfulDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(true)
            .create()
    }

    private fun saveTransactorToDb(transactor: Transactor){
        db = dbHelper?.writableDatabase
        db?.let { dbHelper?.insertTransactor(it, transactor) }
    }

    fun getBitmapFromAvatarView(avatarView: AvatarView): Bitmap? {
        val drawable = avatarView.drawable
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun phoneNumberValidation(): Boolean {
        if (transactorPhoneNoTextView?.text.toString() == "" ) {
            transactorPhoneNoTextView?.error = "Phone number is required"
            justifications.add("Phone number is required")
            return false
        }
        else if (transactorPhoneNoTextView?.text.toString().length != 10 || !(transactorPhoneNoTextView?.text.toString().startsWith("07") || transactorPhoneNoTextView?.text.toString().startsWith("01"))) {
            transactorPhoneNoTextView?.error = "Phone number must start with 07 or 01 and be 10 digits long"
            justifications.add("Phone number must start with 07 or 01 and be 10 digits long")
            return false
        }
        else if(transactorPhoneNoTextView?.text.toString().length > 0 ){
            if(checkPhoneNumberInDb(transactorPhoneNoTextView?.text.toString())){
                if (functionality != "Edit"){
                    transactorPhoneNoTextView?.error = "Phone number already exists"
                    justifications.add("Phone number already exists")
                    return false
                }else{
                    return true
                }

            }
                else{
                    return true
            }

        }
        else{
            return true
        }

    }

    private fun checkPhoneNumberInDb(phoneNumber: String): Boolean {
        db = dbHelper?.writableDatabase
        return dbHelper!!.checkIfTransactorExistsByPhoneNumber(db, phoneNumber)
    }

    private fun nameValidation(): Boolean {
        val words = transactorNameTextView?.text.toString().split("\\s+".toRegex())
        if (transactorNameTextView?.text.toString() == "") {
            transactorNameTextView?.error = "Name is required"
            justifications.add("Name is required")
            return false
        }
        else if(words.size < 2){
            transactorNameTextView?.error = "Transactor must have two or more names"
            justifications.add("Transactor must have two or more names")
            return false
        }
        else{
            return true
        }
    }

    private fun initializeProfileImage() {
        profileImageImageButton?.setOnClickListener {
            showImagePickerDialog()
        }

    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select an option")
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    "Take Photo" -> {
                        if (ContextCompat.checkSelfPermission(
                                requireContext(),
                                android.Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
                        } else {
                            openGallery()
                        }
                    }
                    "Choose from Gallery" -> {
                        openCamera()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openGallery() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun openCamera() {
        pickImageLauncher.launch("image/*")
    }

    private fun initializeTransactorTypeAutoCompleteTextInput() {
        val transactorTypes = listOf("Individual", "Corporate")
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, transactorTypes)

        transactorTypeTextView?.setAdapter(adapter)

        transactorTypeTextView?.setText("Individual", false)

        transactorTypeTextView?.setOnItemClickListener { _, _, position, _ ->
            handleTransactorTypeSelection(transactorTypes[position])
        }
    }

    private fun handleTransactorTypeSelection(selectedItem: String) {
        if (selectedItem == "Corporate") {
            transactorIdTextView?.isEnabled = false
            transactorPhoneNoTextView?.isEnabled = false


            idImageView?.drawable?.mutate()?.let { drawable ->
                drawable.alpha = 130
                idImageView?.setImageDrawable(drawable)
            }


            phoneImageView?.drawable?.mutate()?.let { drawable ->
                drawable.alpha = 130
                phoneImageView?.setImageDrawable(drawable)
            }
        } else if (selectedItem == "Individual") {
            transactorIdTextView?.isEnabled = true
            transactorPhoneNoTextView?.isEnabled = true


            idImageView?.drawable?.mutate()?.let { drawable ->
                drawable.alpha = 255
                idImageView?.setImageDrawable(drawable)
            }


            phoneImageView?.drawable?.mutate()?.let { drawable ->
                drawable.alpha = 255
                phoneImageView?.setImageDrawable(drawable)
            }
        }
    }

    private fun initializeTransactorNameTextInput() {
        // Add a TextWatcher to automatically capitalize each word
        transactorNameTextView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                // Get the current text
                val input = editable.toString()

                // Capitalize the first letter of each word
                val capitalized = input.split(" ").joinToString(" ") { it.capitalize() }

                // Remove the text watcher temporarily to prevent infinite loop
                transactorNameTextView?.removeTextChangedListener(this)

                // Update the text in the input field
                transactorNameTextView?.setText(capitalized)
                transactorNameTextView?.setSelection(capitalized.length) // Move the cursor to the end

                if (input == "") {
                    profileImageAvatar?.text = null
                }else{
                    profileImageAvatar?.text = capitalized

                }


                // Re-add the text watcher
                transactorNameTextView?.addTextChangedListener(this)
            }
        })
    }

    fun getRandomAvatarColor(): String {
        val random = Random.Default
        val r = random.nextInt(256)
        val g = random.nextInt(256)
        val b = random.nextInt(256)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun getColorInt(colorString: String): Int {
        return Color.parseColor(colorString)
    }


    private fun closeDialog() {
        dismiss()
    }

    fun avatarViewHasBitmap(avatarView: AvatarView): Boolean {
        val drawable = avatarView.drawable
        return drawable is BitmapDrawable && drawable.bitmap != null
    }

    private fun disableAutoCompleteTextView(autoCompleteTextView: AutoCompleteTextView) {
        // Disable user interaction
        autoCompleteTextView.isEnabled = false // Prevent interaction
        autoCompleteTextView.isClickable = false // Prevent clicking
        autoCompleteTextView.isFocusable = false // Prevent focus
        autoCompleteTextView.isFocusableInTouchMode = false // Prevent touch focus

        // Optionally hide the dropdown (if necessary)
        autoCompleteTextView.setAdapter(null) // Clear the adapter to ensure dropdown does not show
    }





    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }
}