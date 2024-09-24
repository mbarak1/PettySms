package com.example.pettysms

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import cdflynn.android.library.checkview.CheckView
import com.example.pettysms.databinding.FragmentAddOrEditOwnerDialogBinding
import com.example.pettysms.databinding.FragmentAddOrEditTruckDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import xyz.schwaab.avvylib.AvatarView
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AddOrEditOwnerDialog : DialogFragment() {
    private lateinit var toolbar: Toolbar
    private lateinit var avatarView: AvatarView
    private lateinit var companyNameTextView: TextInputEditText
    private lateinit var companyCodeTextView: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var logoImageBtn: ImageButton
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var successfulDialog: AlertDialog
    private lateinit var deleteSuccessfulDialog: AlertDialog
    private lateinit var saveErrorDialog: AlertDialog




    private var _binding: FragmentAddOrEditOwnerDialogBinding? = null
    private val binding get() = _binding!!
    private var onAddOwnerListener: AddOrEditOwnerDialog.OnAddOwnerListener? = null
    private var functionality: String? = null
    private var imageString: String? = null
    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var owner: Owner? = null
    private var isEditing = false
    private val PICK_IMAGE_REQUEST = 1


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
            inflater.inflate(R.menu.menu_owner, menu)
            val deleteItem = menu.findItem(R.id.deleteOwner)

            // Assuming you have the ownerId available
            val ownerCode = owner?.ownerCode
            val ownerHasTrucks = ownerCode?.let { dbHelper?.doesOwnerHaveTrucks(it) }

            if (ownerHasTrucks == true) {
                deleteItem.isEnabled = false
                deleteItem.icon?.mutate()?.let {
                    it.alpha = 130 // Adjust alpha to grey out the icon
                    deleteItem.icon = it
                }
            } else {
                deleteItem.isEnabled = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.deleteOwner -> {
                if (item.isEnabled) {
                    // Perform delete operation
                    deleteOwner(owner?.id)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteOwner(id: Int?) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Warning")
            .setMessage("Are you sure you want to delete this company?")
            .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
            .setNegativeButton("Dismiss") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Confirm") { dialog, which ->
                db = dbHelper?.writableDatabase
                id?.let { dbHelper?.deleteOwner(it) }
                createDeleteSuccessfulDialog()
                deleteSuccessfulDialog.show()
                onAddOwnerListener?.onAddOwner()
                closeDialog()

            }
            .show()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddOrEditOwnerDialogBinding.inflate(inflater, container, false)
        toolbar = binding.addOrEditOwnerToolbar
        functionality =  arguments?.getString("Action")

        avatarView = binding.avatarView
        companyNameTextView = binding.ownerNameTextField
        companyCodeTextView = binding.ownerCodeTextField
        saveButton = binding.saveButton
        logoImageBtn = binding.logoImageButton
        db = dbHelper?.writableDatabase

        val defaultLogoDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.p_logo_cropped)
        val ownerJson = arguments?.getString("OwnerJson")


        //avatarView.setImageDrawable(defaultLogoDrawable)
        companyCodeTextView.isEnabled = false

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar.inflateMenu(R.menu.menu_owner)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "$functionality Company"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        if (ownerJson != null || ownerJson != "") {
            owner = Gson().fromJson(ownerJson, Owner::class.java)
        }

        prepareFormOnEdit()




        toolbar.setNavigationOnClickListener {
            closeDialog()
        }

        selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val imageUri: Uri? = result.data?.data
                if (imageUri != null) {
                    val inputStream: InputStream? = requireContext().contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    avatarView.setImageBitmap(bitmap)
                    imageString = bitmapToBase64(bitmap)
                    //dbHelper.insertImage(base64String)
                }
            }
        }

        initializeEditTexts()

        initializeLogoImageButton()

        initializeSaveButton()


        // Inflate the layout for this fragment
        return binding.root
    }

    private fun prepareFormOnEdit() {
        if (functionality == "Edit") {
            owner?.let {
                companyNameTextView.setText(it.name)
                companyCodeTextView.setText(it.ownerCode)
                imageString = it.logoPath
            }

            if (!imageString.isNullOrEmpty()) {
                val bitmap = base64ToBitmap(imageString!!)
                avatarView.setImageBitmap(bitmap)
            }


        }
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    private fun initializeSaveButton() {
        saveButton.setOnClickListener {
            val companyName = companyNameTextView.text.toString()
            val companyCode = companyCodeTextView.text.toString()

            val isCompanyNameInDb = dbHelper?.isOwnerNameExists(companyName)
            val isCompanyCodeInDb = dbHelper?.isOwnerCodeExists(companyCode)

            if (companyName.isEmpty() || isCompanyCodeInDb == true || isCompanyNameInDb == true || functionality == "Edit") {
                val justification = mutableListOf<String>()

                if (companyName.isEmpty()){
                    justification.add("No Company Name")
                }
                if (isCompanyNameInDb == true){
                    justification.add("Company Name already exists")
                }
                if (isCompanyCodeInDb == true){
                    justification.add("Company Code already exists")
                }

                if((justification.contains("Company Name already exists") || justification.contains("Company Code already exists")) && functionality == "Edit"){
                    createSaveSuccessfulDialog()
                    successfulDialog.show()
                    println("edited")

                    updateCompanyToDb(companyName, companyCode)
                    onAddOwnerListener?.onAddOwner()
                    closeDialog()
                    return@setOnClickListener
                }else{
                    if(functionality == "Edit"){
                        justification.remove("Company Name already exists")
                        justification.remove("Company Code already exists")
                    }
                }


                createSaveErrorDialog(R.drawable.baseline_warning_amber_white_24dp, "Error", justification)
                saveErrorDialog.show()

            }else{
                createSaveSuccessfulDialog()
                successfulDialog.show()
                saveCompanyToDb(companyName, companyCode)
                onAddOwnerListener?.onAddOwner()

                closeDialog()
            }


        }
    }

    private fun updateCompanyToDb(companyName: String, companyCode: String) {
        db = dbHelper?.writableDatabase
        db?.let { dbHelper?.updateOwner(it, Owner(id = owner?.id, companyName, companyCode, imageString)) }
    }

    private fun saveCompanyToDb(companyName: String, companyCode: String) {
        db = dbHelper?.writableDatabase
        db?.let { dbHelper?.insertOwner(it, Owner(id = null, companyName, companyCode, imageString)) }
    }

    private fun initializeLogoImageButton() {
        logoImageBtn.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            selectImageLauncher.launch(intent)
        }
    }

    private fun initializeEditTexts() {
        companyNameTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isEditing) return

                isEditing = true
                val formattedText = capitalizeWords(s.toString())
                companyNameTextView.setText(formattedText)
                companyNameTextView.setSelection(formattedText.length)  // Set cursor to the end

                // Update the company code
                if (functionality != "Edit"){
                    val companyCode = generateCompanyCode(formattedText)
                    companyCodeTextView.setText(companyCode)
                }


                isEditing = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun generateCompanyCode(companyName: String): String {
        if (companyName.isBlank()) return ""

        val words = companyName.split(" ")
        val firstWord = words.first().lowercase()
        val initials = words.drop(1).joinToString("") { it.firstOrNull()?.lowercaseChar()?.toString() ?: "" }

        return firstWord + initials
    }

    interface OnAddOwnerListener {
        fun onAddOwner()
    }

    private fun closeDialog() {
        dismiss()
    }

    fun setOnAddOwnerListener(listener: OnAddOwnerListener) {
        this.onAddOwnerListener = listener

    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun createSaveSuccessfulDialog() {
        // Inflate custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.save_successful_dialog, null)

        // Set up loading text or any other views if needed
        val saveSuccessfullyText = customView.findViewById<TextView>(R.id.success_message_text)
        val checkView = customView.findViewById<CheckView>(R.id.check)
        saveSuccessfullyText.text = "Company Successfully Saved!"
        checkView.check()

        // Create and customize the dialog
        successfulDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(true)
            .create()
    }

    private fun createDeleteSuccessfulDialog() {
        // Inflate custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.save_successful_dialog, null)

        // Set up loading text or any other views if needed
        val saveSuccessfullyText = customView.findViewById<TextView>(R.id.success_message_text)
        val checkView = customView.findViewById<CheckView>(R.id.check)
        saveSuccessfullyText.text = "Company Successfully Deleted!"
        checkView.check()

        // Create and customize the dialog
        deleteSuccessfulDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(true)
            .create()
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

    companion object {

    }
}