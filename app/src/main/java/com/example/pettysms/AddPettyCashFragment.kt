package com.example.pettysms

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.Gravity
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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import cdflynn.android.library.checkview.CheckView
import com.example.pettysms.MpesaTransaction.Companion.getTitleTextByTransactionTypeWithoutFormatting
import com.example.pettysms.Transactor.Companion.toJson
import com.example.pettysms.databinding.FragmentAddPettyCashBinding
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.zxing.integration.android.IntentIntegrator
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class AddPettyCashFragment : DialogFragment(), AddOrEditTransactorDialog.OnAddTransactorListener {

    private var _binding: FragmentAddPettyCashBinding? = null
    // Nullable views
    private var toolbar: Toolbar? = null
    private var amountEditText: EditText? = null
    private var chipGroup: ChipGroup? = null
    private var defaultChip: Chip? = null
    private var transactorAutoCompleteTextView: AutoCompleteTextView? = null
    private var progressBar: ProgressBar? = null
    private var ownerCarouselRecyclerView: RecyclerView? = null
    private var ownerCarouselAdapter: OwnerPettyCashAdapter? = null
    private var trucksAutoCompleteTextView: MultiAutoCompleteTextView? = null
    private var accountsAutoCompleteTextView: AutoCompleteTextView? = null
    private var pettyCashDateTextInputLayout: TextInputLayout? = null
    private var pettyCashDateEditText: TextInputEditText? = null
    private var pettyCashNumberInputEditText: TextInputEditText? = null
    private var dottedBox: LinearLayout? = null
    private var addImageIcon: ImageView? = null
    private var supportingDocumentFormLinearLayout: LinearLayout? = null
    private var supportingDocumentSwitch: MaterialSwitch? = null
    private var supportingDocumentTypeAutoCompleteTextView: AutoCompleteTextView? = null
    private var supportingDocumentNumberEditText: TextInputEditText? = null
    private var supportingDocumentNumberTextInputLayout: TextInputLayout? = null
    private var supportingDocumentCuNumberEditText: TextInputEditText? = null
    private var supportingDocumentCuNumberTextInputLayout: TextInputLayout? = null
    private var supportingDocumentDateEditText: TextInputEditText? = null
    private var supportingDocumentDateTextInputLayout: TextInputLayout? = null
    private var supportingDocumentSupplierName: TextInputEditText? = null
    private var supportingDocumentSupplierNameTextInputLayout: TextInputLayout? = null
    private var supportingDocumentTotalTaxableAmountEditText: TextInputEditText? = null
    private var supportingDocumentTotalTaxableAmountTextInputLayout: TextInputLayout? = null
    private var supportingDocumentTotalTaxEditText: TextInputEditText? = null
    private var supportingDocumentTotalTaxTextInputLayout: TextInputLayout? = null
    private var supportingDocumentTotalAmountEditText: TextInputEditText? = null
    private var supportingDocumentTotalAmountTextInputLayout: TextInputLayout? = null
    private var saveButton: Button? = null
    private var descriptionTextInputEditText: TextInputEditText? = null
    private var signatureView: SignatureView? = null
    private var signatureClearButton: ImageButton? = null
    private var signatureLabelTextView: TextView? = null
    private var mpesaTransactionLabel: TextView? = null
    private var mpesaTransactionTextInputLayout: TextInputLayout? = null
    private var mpesaTransactionAutoCompleteTextView: AutoCompleteTextView? = null
    private var loadingDialog: AlertDialog? = null
    private var loadingText: TextView? = null
    private var saveErrorDialog: AlertDialog? = null
    private var successfulDialog: AlertDialog? = null
    private var signatureViewModel: SignatureViewModel? = null
    private var horizontalScrollView: HorizontalScrollView? = null
    private var linearLayoutImages: LinearLayout? = null






    private val binding get() = _binding!!
    private var functionality = "Add"
    private val initialUrl = "https://itax.kra.go.ke/KRA-Portal/main.htm?actionCode=showHomePageLnclick"
    private val postUrl = "https://itax.kra.go.ke/KRA-Portal/middlewareController.htm?actionCode=fetchInvoiceDtl"
    private lateinit var sessionCookie: String
    private val TAG = "AddPettyCashFragment"
    private var gloablResponse: String? = null
    private var current = ""
    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private var owners = mutableListOf<Owner>()
    private var trucks = mutableListOf<Truck>()
    private var selectedTrucks = mutableListOf<Truck>() // Holds the currently selected trucks
    private val accounts = mutableListOf<Account>()
    private var globalSelectedOwner = Owner(0,"", "")
    private var globalTransactor: Transactor? = null
    private var globalAccount: Account? = null
    private var globalMpesaTransaction: MpesaTransaction? = null
    private var onAddPettyCashListener: OnAddPettyCashListener? = null
    private var pettyCash: PettyCash? = null
    private var pettyCashString: String = ""
    private var updateSupportingDocumentFlag: Boolean = false





    private val selectedImagesPath = mutableListOf<String>() // List of selected images
    private val selectedImagesUri = mutableListOf<Uri>() // List of selected images
    private val IMAGE_REQUEST_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002
    private val QR_CODE_REQUEST_CODE = 1003


    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle scanning result
            val result = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (result != null) {
                result.getPages()?.let { pages ->
                    for (page in pages) {
                        val imageUri = page.getImageUri() // JPEG URI
                        // Save image as a file and store its path
                        if (selectedImagesPath.size < 3) {
                            val savedFilePath = saveImageAsFile(imageUri)
                            if (savedFilePath != null) {
                                selectedImagesUri.add(imageUri)
                                selectedImagesPath.add(savedFilePath)
                                addImageToLayout(Uri.fromFile(File(savedFilePath))) // Pass Uri for display if needed
                            }
                        }
                    }
                }
            }

            // Handle PDF result if available
            result?.getPdf()?.let { pdf ->
                val pdfUri = pdf.getUri() // PDF URI
                val pageCount = pdf.getPageCount() // PDF page count
                // Handle the PDF URI (e.g., display, save, etc.)
            }
        } else {
            Toast.makeText(requireContext(), "Document scanning failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Save the image as a file and return the file path
    private fun saveImageAsFile(imageUri: Uri): String? {
        return try {
            // Open the image and decode it into a Bitmap
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // Save the Bitmap to a file
            val savedFile = File(requireContext().cacheDir, "scanned_image_${System.currentTimeMillis()}.jpg")
            savedFile.outputStream().use { fos ->
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos) // Compress with 80% quality
            }

            savedFile.absolutePath // Return the file path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Enable options menu in fragment
        setStyle(STYLE_NORMAL, R.style.PrefsTheme)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        //menu.clear() // Clear the menu to prevent stacking of items
        println("Functionality: $functionality")
        inflater.inflate(R.menu.add_or_edit_petty_cash_menu, menu)
        val scanEtr = menu.findItem(R.id.scanEtr)
        val deleteItem = menu.findItem(R.id.deletePettyCash)
        val syncMenu = menu.findItem(R.id.syncPettyCash)

        syncMenu.isVisible = false


        if (functionality == "Edit") {
            scanEtr.isVisible = true
            deleteItem.isVisible = true
        }
        else{
            scanEtr.isVisible = true
            deleteItem.isVisible = false
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.scanEtr -> {
                if (item.isEnabled) {
                    scanEtr()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun scanEtr() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setCaptureActivity(MyCaptureActivity::class.java)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a QR code")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.setRequestCode(QR_CODE_REQUEST_CODE) // Use the same QR code request code
        integrator.initiateScan()
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
        _binding = FragmentAddPettyCashBinding.inflate(inflater, container, false)
        toolbar = binding.addPettyCashToolbar
        amountEditText = binding.editTextAmount
        chipGroup = binding.chipGroup
        defaultChip = binding.chipCash
        transactorAutoCompleteTextView = binding.autoCompleteTextView
        progressBar = binding.progressSpinner
        dbHelper = DbHelper(requireContext())
        db = dbHelper?.writableDatabase
        ownerCarouselRecyclerView = binding.companyCarouselRecyclerView
        trucksAutoCompleteTextView = binding.trucksAutoCompleteTextView
        accountsAutoCompleteTextView = binding.accountsAutoCompleteTextView
        pettyCashDateTextInputLayout = binding.pettyCashDateTextInputLayout
        pettyCashDateEditText = binding.pettyCashDateInputEditText
        dottedBox = binding.dottedBox
        addImageIcon = binding.uploadIcon
        supportingDocumentFormLinearLayout = binding.supportingDocumentFormLinearLayout
        supportingDocumentSwitch = binding.supportingDocumentSwitch
        signatureView = binding.signatureView
        signatureClearButton = binding.signatureClearButton
        signatureLabelTextView = binding.signatureLabelTextView
        mpesaTransactionLabel = binding.mpesaTransactionLabel
        mpesaTransactionTextInputLayout = binding.mpesaTransactionTextInputLayout
        mpesaTransactionAutoCompleteTextView = binding.mpesaTransactionAutoCompleteTextView
        supportingDocumentNumberEditText = binding.supportingDocumentNumberTextInputEditText
        supportingDocumentNumberTextInputLayout = binding.supportingDocumentNumberTextInputLayout
        supportingDocumentTypeAutoCompleteTextView = binding.supportingDocumentTypeAutoCompleteTextView
        supportingDocumentDateEditText = binding.supportingDocumentDateInputEditText
        supportingDocumentDateTextInputLayout = binding.supportingDocumentDateTextInputLayout
        supportingDocumentSupplierName = binding.supplierNameTextInputEditText
        supportingDocumentSupplierNameTextInputLayout = binding.supplierNameTextInputLayout
        supportingDocumentTotalTaxableAmountEditText = binding.totalTaxableAmountTextInputEditText
        supportingDocumentTotalTaxableAmountTextInputLayout = binding.totalTaxableAmountTextInputLayout
        supportingDocumentTotalTaxEditText = binding.taxAmountTextInputEditText
        supportingDocumentTotalTaxTextInputLayout = binding.taxAmountTextInputLayout
        supportingDocumentTotalAmountEditText = binding.totalAmountTextInputEditText
        supportingDocumentTotalAmountTextInputLayout = binding.totalAmountTextInputLayout
        supportingDocumentCuNumberEditText = binding.cuTaxNumberTextInputEditText
        supportingDocumentCuNumberTextInputLayout = binding.cuTaxNumberTextInputLayout
        saveButton = binding.saveButton
        pettyCashNumberInputEditText = binding.pettyCashNumberInputEditText
        descriptionTextInputEditText = binding.descriptionTextInputEditText
        horizontalScrollView = binding.horizontalScrollView
        linearLayoutImages = binding.linearLayoutImages
        functionality = arguments?.getString("action").toString()

        if(functionality != "Edit"){
            functionality = "Add"
        }
        pettyCashString = arguments?.getString("pettyCash").toString()

        if(pettyCashString != null || pettyCashString != ""){
            pettyCash = Gson().fromJson(pettyCashString, PettyCash::class.java)
        }

        pettyCashNumberInputEditText?.isEnabled = false



        owners = getOwners()

        amountEditText?.filters = arrayOf(InputFilter.LengthFilter(14))

        defaultChip?.isChecked = true

        var previouslySelectedChipId = R.id.chipCash // Set a default chip

        chipGroup?.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) {
                // Prevent deselecting the chip by rechecking the previously selected one
                chipGroup?.check(previouslySelectedChipId)
            } else {
                // Update the previously selected chip ID
                previouslySelectedChipId = checkedId
            }
            if(previouslySelectedChipId == R.id.chipMpesa){
                mpesaTransactionLabel?.visibility = View.VISIBLE
                mpesaTransactionTextInputLayout?.visibility = View.VISIBLE
                mpesaTransactionAutoCompleteTextView?.visibility = View.VISIBLE
                hideSignatureLayout()
            }else{
                mpesaTransactionLabel?.visibility = View.GONE
                mpesaTransactionTextInputLayout?.visibility = View.GONE
                mpesaTransactionAutoCompleteTextView?.visibility = View.GONE
                if(globalTransactor != null){
                    if(globalTransactor!!.transactorType == "Individual"){
                        showSignatureLayout()
                    }
                    else{
                        hideSignatureLayout()
                    }
                }
            }
        }


        signatureViewModel = ViewModelProvider(this).get(SignatureViewModel::class.java)

        // Restore the signature if it exists
        signatureViewModel?.signatureBitmap?.observe(viewLifecycleOwner, Observer { bitmap ->
            bitmap?.let {
                signatureView?.setSignatureBitmap(it)
            }
        })







        setupAmountEditText()

        // Set up the AutoCompleteTextView
        setupAutoCompleteTextView()

        setupCompanyCarousel()

        setupTrucksAutoCompleteTextView()

        setUpAccountsAutoCompleteTextView()

        setupMpesaTransactionAutoCompleteTextView()

        setUpSupportingDocumentSection()

        setupDatePickerForField(pettyCashDateTextInputLayout!!,
            pettyCashDateEditText!!, requireActivity().supportFragmentManager)

        addImageIcon?.setOnClickListener {
            if (selectedImagesPath.size < 3) {
                showImageSourceDialog()
            } else {
                Toast.makeText(requireContext(), "You can upload a maximum of 3 images.", Toast.LENGTH_SHORT).show()
            }
        }
        dottedBox?.setOnClickListener {
            if (selectedImagesPath.size < 3) {
                showImageSourceDialog()
            } else {
                Toast.makeText(requireContext(), "You can upload a maximum of 3 images.", Toast.LENGTH_SHORT).show()
            }
        }



        signatureClearButton?.setOnClickListener {
            signatureView?.clearSignature() // Clear the signature when button is clicked
        }


        saveButton?.setOnClickListener {
            if (functionality == "Add"){
                if(pettyCashValidation()){
                    val pettyCash = createPettyCashObject()
                    var transactionCostPettyCash: PettyCash? = null

                    savePettyCashToDb(pettyCash)
                    if (pettyCash.paymentMode == "M-Pesa") {
                        if(pettyCash.mpesaTransaction?.transactionCost!! > 0){
                            transactionCostPettyCash = createPettyCashTransactionCostObject(pettyCash)
                            savePettyCashToDb(transactionCostPettyCash)
                            println(transactionCostPettyCash.toJson())
                        }
                        markTransactionAsConverted(pettyCash.mpesaTransaction)
                    }

                    println("running onAdd Petty cash")
                    this.onAddPettyCashListener?.onAddPettyCash(pettyCash, transactionCostPettyCash)
                    closeDialog()
                }
            }else{
                if (pettyCashValidation()){
                    val pettyCash = createPettyCashObject()
                    var transactionCostPettyCash: PettyCash? = null
                    println(pettyCash.toJson())
                    updatePettyCashInDb(pettyCash)
                    if (pettyCash.paymentMode == "M-Pesa") {
                        transactionCostPettyCash = updatePettyCashTransactionCostObject(pettyCash)
                        if (transactionCostPettyCash != null){
                            updatePettyCashInDb(transactionCostPettyCash)
                            println(transactionCostPettyCash.toJson())
                        }
                    }
                    Log.d("AddPettyCashFragment" , "In Edit Petty Cash")
                    println("OnAddPettyCashListener" + onAddPettyCashListener)
                    if (onAddPettyCashListener == null) {
                        Log.e("AddPettyCashFragment", "onAddPettyCashListener is null in edit mode")
                    }

                    onAddPettyCashListener?.onAddPettyCash(pettyCash, transactionCostPettyCash)


                    Log.d("AddPettyCashFragment" , "After Calling listener")
                    closeDialog()
                }
            }

        }






        // Add trucks as chips to the ChipsInput
        // Add trucks as chips to the ChipsInput








        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "$functionality Petty Cash"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar?.setNavigationOnClickListener {
            closeDialog()
        }

        if (functionality == "Edit"){
            fillFields(pettyCash)
        }


        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Nullify binding and views
        _binding = null

        // Nullify and clear other resources
        dbHelper?.close()
        dbHelper = null
        db = null

        gloablResponse = null
        current = ""

        globalSelectedOwner = Owner(0, "", "") // Reset to default value
        globalTransactor = null
        globalAccount = null
        globalMpesaTransaction = null
        onAddPettyCashListener = null
        pettyCash = null
        pettyCashString = ""

        // Clear mutable lists
        owners.clear()
        trucks.clear()
        selectedTrucks.clear()
        accounts.clear()
        selectedImagesUri.clear()
        selectedImagesPath.clear()

        // Reset update flag
        updateSupportingDocumentFlag = false

        toolbar = null
        amountEditText = null
        chipGroup = null
        defaultChip = null
        transactorAutoCompleteTextView = null
        progressBar = null
        ownerCarouselRecyclerView = null
        ownerCarouselAdapter = null
        trucksAutoCompleteTextView = null
        accountsAutoCompleteTextView = null
        pettyCashDateTextInputLayout = null
        pettyCashDateEditText = null
        pettyCashNumberInputEditText = null
        dottedBox = null
        horizontalScrollView = null
        addImageIcon = null
        supportingDocumentFormLinearLayout = null
        supportingDocumentSwitch = null
        supportingDocumentTypeAutoCompleteTextView = null
        supportingDocumentNumberEditText = null
        supportingDocumentNumberTextInputLayout = null
        supportingDocumentCuNumberEditText = null
        supportingDocumentCuNumberTextInputLayout = null
        supportingDocumentDateEditText = null
        supportingDocumentDateTextInputLayout = null
        supportingDocumentSupplierName = null
        supportingDocumentSupplierNameTextInputLayout = null
        supportingDocumentTotalTaxableAmountEditText = null
        supportingDocumentTotalTaxableAmountTextInputLayout = null
        supportingDocumentTotalTaxEditText = null
        supportingDocumentTotalTaxTextInputLayout = null
        supportingDocumentTotalAmountEditText = null
        supportingDocumentTotalAmountTextInputLayout = null
        saveButton = null
        descriptionTextInputEditText = null
        signatureView = null
        signatureClearButton = null
        signatureLabelTextView = null
        mpesaTransactionLabel = null
        mpesaTransactionTextInputLayout = null
        mpesaTransactionAutoCompleteTextView = null
        loadingDialog = null
        loadingText = null
        saveErrorDialog = null
        successfulDialog = null
        signatureViewModel = null

        // Nullify other resources
        sessionCookie = ""
        gloablResponse = null
        dbHelper?.close()
        dbHelper = null
        db = null
        globalTransactor = null
        globalAccount = null
        globalMpesaTransaction = null
        onAddPettyCashListener = null
        pettyCash = null
        MpesaFragment.CallbackSingleton.refreshCallback = null
    }

    private fun updatePettyCashTransactionCostObject(pettyCash: PettyCash): PettyCash? {
        val latestPettyCashNo = pettyCash.pettyCashNumber
        val parts = latestPettyCashNo!!.split("/")
        val ownerCode = pettyCash.owner?.ownerCode
        var nextPettyCashNo: String = latestPettyCashNo
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Validate the format of the latest petty cash number
        if (parts.size == 3 && parts[0] == ownerCode && parts[2] == currentYear.toString()) {
            val currentNumber = parts[1].toIntOrNull() ?: 0
            val newNumber = currentNumber + 1 // Increment the petty cash number
            nextPettyCashNo = "$ownerCode/${String.format("%08d", newNumber)}/$currentYear"
            println("in here in change")
        }

        if(dbHelper == null){
            dbHelper = DbHelper(requireContext())
        }

        val transactionCostAccount: Account? = dbHelper!!.getTransactionCostAccountByOwner(ownerCode)

        val nonUpdatedTransactionCostPettyCashObject = dbHelper!!.getTransactionCostPettyCashByMpesaTransaction(pettyCash.mpesaTransaction?.mpesa_code)

        if (nonUpdatedTransactionCostPettyCashObject == null){
            return null
        }

        if (nonUpdatedTransactionCostPettyCashObject?.pettyCashNumber != null){
            if (nonUpdatedTransactionCostPettyCashObject.owner?.ownerCode == pettyCash.owner?.ownerCode){
                nextPettyCashNo = nonUpdatedTransactionCostPettyCashObject.pettyCashNumber!!
            }
        }




        return PettyCash(
            id = nonUpdatedTransactionCostPettyCashObject.id,
            pettyCashNumber = nextPettyCashNo,
            date = nonUpdatedTransactionCostPettyCashObject.date,
            amount = nonUpdatedTransactionCostPettyCashObject.amount,
            isDeleted = false,
            trucks = selectedTrucks,
            transactor = globalTransactor,
            account = transactionCostAccount,
            description = nonUpdatedTransactionCostPettyCashObject.description,
            mpesaTransaction = null,
            owner = globalSelectedOwner,
            paymentMode = chipGroup?.checkedChipId?.let { chipGroup?.findViewById<Chip>(it)?.text.toString() },
            signature = null,
            supportingDocument = null
        )

    }

    private fun fillFields(pettyCash: PettyCash?) {
        pettyCash?.let {
            it.amount?.let { it1 -> amountEditText?.let { it2 -> simulateTyping(it2, it1) } }
            transactorAutoCompleteTextView?.setText(capitalizeEachWord(it.transactor?.name.toString()))
            pettyCashNumberInputEditText?.setText(it.pettyCashNumber)
            globalTransactor = it.transactor
            globalMpesaTransaction = it.mpesaTransaction
            pettyCashDateEditText?.setText(it.date)
            descriptionTextInputEditText?.setText(it.description)
            mpesaTransactionAutoCompleteTextView?.setText(it.mpesaTransaction?.mpesa_code)
            globalAccount = it.account
            accountsAutoCompleteTextView?.setText(it.account?.name ?: "")
            trucksAutoCompleteTextView?.setText(it.trucks?.joinToString(", ") ?: "")
            if (pettyCash.paymentMode != "Cash") {
                amountEditText?.isEnabled = false
                pettyCashDateEditText?.isEnabled = false
                transactorAutoCompleteTextView?.isEnabled = false
                mpesaTransactionAutoCompleteTextView?.isEnabled = false
                chipGroup?.let { it1 -> selectMpesaAndDisableOthers(it1) }

            }else{
                chipGroup?.let { it1 -> selectCashAndDisableOthers(it1) }
            }

            if (it.owner != null){
                selectOwner(it.owner!!)
                globalSelectedOwner= it.owner!!
            }

            if (it.supportingDocument?.id != null){
                updateSupportingDocumentFlag = true
                supportingDocumentSwitch?.isChecked = true
                supportingDocumentTypeAutoCompleteTextView?.setText(it.supportingDocument!!.type)
                supportingDocumentNumberEditText?.setText(it.supportingDocument!!.documentNo)
                supportingDocumentDateEditText?.setText(it.supportingDocument!!.documentDate)
                supportingDocumentSupplierName?.setText(it.supportingDocument!!.supplierName)
                supportingDocumentTotalTaxableAmountEditText?.setText(it.supportingDocument!!.taxableTotalAmount.toString())
                supportingDocumentTotalTaxEditText?.setText(it.supportingDocument!!.taxAmount.toString())
                supportingDocumentTotalAmountEditText?.setText(it.supportingDocument!!.totalAmount.toString())
                supportingDocumentCuNumberEditText?.setText(it.supportingDocument!!.cuNumber)

                if (it.supportingDocument!!.imagePath1 != null){
                    val image1 = it.supportingDocument!!.imagePath1?.let { it1 -> base64ToUri(it1) }
                    if (image1 != null) {
                        addImageToLayout(image1)
                    }
                }
                if (it.supportingDocument!!.imagePath2 != null){
                    val image2 = it.supportingDocument!!.imagePath2?.let { it1 -> base64ToUri(it1) }
                    if (image2 != null) {
                        addImageToLayout(image2)
                    }
                }
                if (it.supportingDocument!!.imagePath3 != null){
                    val image3 = it.supportingDocument!!.imagePath3?.let { it1 -> base64ToUri(it1) }
                    if (image3 != null) {
                        addImageToLayout(image3)
                    }
                }




            }

            if (it.signature != null){
                base64ToBitmap(it.signature!!)?.let { it1 -> signatureView?.setSignatureBitmap(it1) }
            }





        }

    }

    // Function to convert Base64 string to Bitmap
    private fun base64ToBitmap(base64Image: String): Bitmap? {
        return try {
            // Decode the Base64 string into a byte array
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            // Convert byte array to Bitmap
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    private fun base64ToUri(base64Image: String): Uri? {
        return try {
            // Decode Base64 string into a Bitmap
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            // Save the bitmap to a temporary file and return its Uri
            val tempFile = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
            }
            Uri.fromFile(tempFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun selectOwner(owner: Owner) {
        val position = owners.indexOf(owner)
        if (position >= 0) {
            ownerCarouselRecyclerView?.scrollToPosition(position)
            ownerCarouselAdapter?.setSelectedPosition(position) // Update the selected position in the adapter

            // Trigger the click behavior to apply any other effects of selecting an owner
            ownerCarouselAdapter?.setSelectedPosition(position)
        } else {
            Log.e(TAG, "Owner not found in the list.")
        }
    }

    fun selectMpesaAndDisableOthers(chipGroup: ChipGroup) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let {
                if (it.text.toString() == "M-Pesa") {
                    // Select the "M-Pesa" chip
                    it.isChecked = true
                } else {
                    // Disable chips that are not "M-Pesa"
                    it.isEnabled = false
                }
            }
        }
    }

    fun selectCashAndDisableOthers(chipGroup: ChipGroup) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let {
                if (it.text.toString() == "Cash") {
                    // Select the "Cash" chip
                    it.isChecked = true
                } else {
                    // Disable chips that are not "Cash"
                    it.isEnabled = false
                }
            }
        }
    }


    private fun markTransactionAsConverted(mpesaTransaction: MpesaTransaction?) {
        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())

        }
        mpesaTransaction?.let { dbHelper?.updateMpesaTransactionAsConverted(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Restore the signature bitmap if it exists
        savedInstanceState?.getByteArray("signatureBitmap")?.let { byteArray ->
            val bitmap = byteArrayToBitmap(byteArray)
            if (bitmap != null) {
                signatureView?.setSignatureBitmap(bitmap)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the signature bitmap
        val bitmap = signatureView?.getSignatureBitmap()
        if (bitmap != null) {
            outState.putByteArray("signatureBitmap", bitmapToByteArray(bitmap))
            Log.d("AddPettyCashFragment", "Signature saved")
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    override fun onPause() {
        super.onPause()
        Log.d("AddPettyCashFragment", "onPause called")
    }

    private fun setUpPettyCashNo(ownerCode: String?) {
        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())
        }

        println("ownerCode: $ownerCode")

        val latestPettyCash = ownerCode?.let { dbHelper!!.getLatestPettyCashByOwnerAndPettyCashNumber(it) }
        println("Latest Petty Cash: ${latestPettyCash?.pettyCashNumber}")

        // Get the current year dynamically
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var nextPettyCashNo: String

        if (latestPettyCash != null) {
            // Extract the current petty cash number and increment it
            val latestPettyCashNo = latestPettyCash.pettyCashNumber
            val parts = latestPettyCashNo!!.split("/")

            // Validate the format of the latest petty cash number
            if (parts.size == 3 && parts[0] == ownerCode && parts[2] == currentYear.toString()) {
                val currentNumber = parts[1].toIntOrNull() ?: 0
                val newNumber = currentNumber + 1 // Increment the petty cash number
                nextPettyCashNo = "$ownerCode/${String.format("%08d", newNumber)}/$currentYear"
                println("in here in change")
            } else {
                // Fallback if the format is unexpected
                nextPettyCashNo = "$ownerCode/00000001/$currentYear"
                println("in here in esle change")
            }
        } else {
            // No petty cash found, revert to the initial petty cash number
            nextPettyCashNo = "$ownerCode/00000001/$currentYear"
            println("in here in esle  esle")

        }

        if (pettyCash?.pettyCashNumber != null){
            nextPettyCashNo = pettyCash?.pettyCashNumber!!
        }

        // Use nextPettyCashNo as needed, e.g., displaying it or saving it somewhere
        println("nextPettyCashNo: $nextPettyCashNo")
        pettyCashNumberInputEditText?.setText(nextPettyCashNo)
        println("Next Petty Cash No: $nextPettyCashNo")
    }

    private fun removeEditTextError(editText: EditText) {
        editText.error = null
    }

    private fun validateField(
        editText: EditText,
        validationCondition: Boolean,
        errorMessage: String,
        justification: MutableList<String>
    ): Boolean {
        return if (validationCondition) {
            removeEditTextError(editText)
            true
        } else {
            editText.error = errorMessage
            justification.add(errorMessage)
            false
        }
    }

    private fun pettyCashValidation(): Boolean {
        val justification = mutableListOf<String>()

        val amountValidation = amountEditText?.let {
            validateField(
                it,
                !amountEditText?.text.isNullOrEmpty() && amountEditText?.text.toString().substringBefore(" KES").replace(",", "").toDouble() > 0.00,
                "Amount is required",
                justification
            )
        }

        val dateValidation = pettyCashDateEditText?.let {
            validateField(
                it,
                !pettyCashDateEditText?.text.isNullOrEmpty(),
                "Date is required",
                justification
            )
        }

        val pettyCashNoValidation = pettyCashNumberInputEditText?.let {
            validateField(
                it,
                !pettyCashNumberInputEditText?.text.isNullOrEmpty(),
                "Petty Cash Number is required",
                justification
            )
        }

        val transactorValidation = globalTransactor?.let {
            transactorAutoCompleteTextView?.let { it1 -> removeEditTextError(it1) }
            true
        } ?: run {
            transactorAutoCompleteTextView?.error = "Transactor is required"
            justification.add("Transactor is required")
            false
        }

        val accountValidation = globalAccount?.let {
            accountsAutoCompleteTextView?.let { it1 -> removeEditTextError(it1) }
            true
        } ?: run {
            accountsAutoCompleteTextView?.error = "Account is required"
            justification.add("Account is required")
            false
        }

        val descriptionValidation = descriptionTextInputEditText?.let {
            validateField(
                it,
                !descriptionTextInputEditText?.text.isNullOrEmpty(),
                "Description is required",
                justification
            )
        }

        val mpesaTransactionValidation = if (chipGroup?.checkedChipId == R.id.chipMpesa) {
            globalMpesaTransaction?.let {
                mpesaTransactionAutoCompleteTextView?.let { it1 -> removeEditTextError(it1) }
                true
            } ?: run {
                mpesaTransactionAutoCompleteTextView?.error = "Mpesa Transaction selection is required"
                justification.add("Mpesa Transaction selection is required")
                false
            }
        } else true

        val trucksValidation = trucksAutoCompleteTextView?.let {
            validateField(
                it,
                !trucksAutoCompleteTextView?.text.isNullOrEmpty(),
                "Truck selection is required",
                justification
            )
        }

        val supportingDocumentValidations = if (supportingDocumentSwitch?.isChecked == true) {
            val supportingDocumentTypeValidation = supportingDocumentTypeAutoCompleteTextView?.let {
                validateField(
                    it,
                    !supportingDocumentTypeAutoCompleteTextView?.text.isNullOrEmpty(),
                    "Supporting Document type is required",
                    justification
                )
            }

            val supportingDocumentNumberValidation = supportingDocumentNumberEditText?.let {
                validateField(
                    it,
                    !supportingDocumentNumberEditText?.text.isNullOrEmpty(),
                    "Supporting Document Number is required",
                    justification
                )
            }

            val supportingDocumentDateValidation = supportingDocumentDateEditText?.let {
                validateField(
                    it,
                    !supportingDocumentDateEditText?.text.isNullOrEmpty(),
                    "Supporting Document Date is required",
                    justification
                )
            }

            val supportingDocumentSupplierNameValidation = supportingDocumentSupplierName?.let {
                validateField(
                    it,
                    !supportingDocumentSupplierName?.text.isNullOrEmpty(),
                    "Supplier Name is required",
                    justification
                )
            }

            val taxInvoiceValidations = if (supportingDocumentTypeAutoCompleteTextView?.text.toString() in listOf("Tax Invoice", "Tax Cash Receipt")) {
                val cuNumberValidation = supportingDocumentCuNumberEditText?.let {
                    validateField(
                        it,
                        !supportingDocumentCuNumberEditText?.text.isNullOrEmpty(),
                        "CU Number is required",
                        justification
                    )
                }

                val taxableAmountValidation = supportingDocumentTotalTaxableAmountEditText?.let {
                    validateField(
                        it,
                        supportingDocumentTotalTaxableAmountEditText?.text.toString().toDoubleOrNull()?.let { it > 0 } ?: false,
                        "Total Taxable Amount is required",
                        justification
                    )
                }

                val taxValidation = supportingDocumentTotalTaxEditText?.let {
                    validateField(
                        it,
                        supportingDocumentTotalTaxEditText?.text.toString().toDoubleOrNull()?.let { it > 0 } ?: false,
                        "Total Tax is required",
                        justification
                    )
                }

                cuNumberValidation == true && taxableAmountValidation == true && taxValidation == true
            } else true

            val totalAmountValidation = supportingDocumentTotalAmountEditText?.let {
                validateField(
                    it,
                    supportingDocumentTotalAmountEditText?.text.toString().toDoubleOrNull()?.let { it > 0 } ?: false,
                    "Total Amount is required",
                    justification
                )
            }


            val amountsEquality =
                amountValidation?.let { checkAmountsEquality(it, totalAmountValidation == true) }
            if(!amountsEquality!!){
                justification.add("Total Amount In Support Document and Amount fields do not match")
            }




            supportingDocumentTypeValidation == true &&
                    supportingDocumentNumberValidation == true &&
                    supportingDocumentDateValidation == true &&
                    supportingDocumentSupplierNameValidation == true &&
                    totalAmountValidation == true && taxInvoiceValidations && amountsEquality == true
        } else true

        val companyValidation = if (globalSelectedOwner.name!!.isNotEmpty()) true else {
            justification.add("Company selection is required")
            false
        }

        val overallValidation = amountValidation == true && dateValidation == true && companyValidation &&
                pettyCashNoValidation == true && transactorValidation && accountValidation && descriptionValidation == true &&
                supportingDocumentValidations && mpesaTransactionValidation && trucksValidation == true

        return if (overallValidation) {
            createSaveSuccessfulDialog("Petty Cash Saved Successfully")
            successfulDialog?.show()
            true
        } else {
            createSaveErrorDialog(R.drawable.baseline_warning_amber_white_24dp, "Error", justification)
            saveErrorDialog?.show()
            false
        }
    }

    private fun savePettyCashToDb(pettyCash: PettyCash){
        PettyCash.insertPettyCashToDb(pettyCash, requireContext())
    }

    private fun updatePettyCashInDb(pettyCash: PettyCash){
        PettyCash.updatePettyCashInDb(pettyCash, requireContext(), updateSupportingDocumentFlag)
    }



    private fun createPettyCashObject(): PettyCash {
        // Set signature to Base64-encoded string if available, otherwise null
        val signature = if (!signatureView?.isSignatureEmpty()!!) {
            encodeBitmapToBase64(signatureView?.getSignatureBitmap()!!)
        } else {
            null
        }

        selectedTrucks = getSelectedTrucks()

        // Retrieve supporting document if switch is enabled, else null
        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())
        }

        var supportingDocumentId = dbHelper!!.getLatestSupportingDocumentId() + 1

        if (pettyCash?.supportingDocument?.id != null){
            supportingDocumentId = pettyCash?.supportingDocument?.id!!
        }

        val supportingDocument = if (supportingDocumentSwitch?.isChecked == true) {
            SupportingDocument(
                id = supportingDocumentId,
                type = supportingDocumentTypeAutoCompleteTextView?.text.toString(),
                documentNo = supportingDocumentNumberEditText?.text.toString(),
                documentDate = supportingDocumentDateEditText?.text.toString(),
                supplierName = supportingDocumentSupplierName?.text.toString(),
                taxableTotalAmount = supportingDocumentTotalTaxableAmountEditText?.text.toString().toDoubleOrNull() ?: 0.0,
                taxAmount = supportingDocumentTotalTaxEditText?.text.toString().toDoubleOrNull() ?: 0.0,
                totalAmount = supportingDocumentTotalAmountEditText?.text.toString().toDoubleOrNull() ?: 0.0,
                cuNumber = supportingDocumentCuNumberEditText?.text.toString(),
                imagePath1 = selectedImagesPath.getOrNull(0),
                imagePath2 = selectedImagesPath.getOrNull(1),
                imagePath3 = selectedImagesPath.getOrNull(2)
            )
        } else {
            null
        }

        var amount = amountEditText?.text.toString().substringBefore(" KES").replace(",", "").toDouble()
        if (chipGroup?.checkedChipId?.let { chipGroup?.findViewById<Chip>(it)?.text.toString().trim() } == "M-Pesa") {
            //amount += globalMpesaTransaction?.transactionCost!!
        }

        var pettyCashId: Int? = null
        if(pettyCash?.id != null){
            pettyCashId = pettyCash!!.id
        }

        return PettyCash(
            id = pettyCashId,
            pettyCashNumber = pettyCashNumberInputEditText?.text.toString(),
            date = pettyCashDateEditText?.text.toString(),
            amount = amount,
            isDeleted = false,
            trucks = selectedTrucks,
            transactor = globalTransactor,
            account = globalAccount,
            description = descriptionTextInputEditText?.text.toString(),
            mpesaTransaction = globalMpesaTransaction,
            owner = globalSelectedOwner,
            paymentMode = chipGroup?.checkedChipId?.let { chipGroup?.findViewById<Chip>(it)?.text.toString() },
            signature = signature,
            supportingDocument = supportingDocument
        )
    }

    private fun createPettyCashTransactionCostObject(pettyCash: PettyCash): PettyCash {

        val latestPettyCashNo = pettyCash.pettyCashNumber
        val parts = latestPettyCashNo!!.split("/")
        val ownerCode = pettyCash.owner?.ownerCode
        var nextPettyCashNo: String = latestPettyCashNo
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Validate the format of the latest petty cash number
        if (parts.size == 3 && parts[0] == ownerCode && parts[2] == currentYear.toString()) {
            val currentNumber = parts[1].toIntOrNull() ?: 0
            val newNumber = currentNumber + 1 // Increment the petty cash number
            nextPettyCashNo = "$ownerCode/${String.format("%08d", newNumber)}/$currentYear"
            println("in here in change")
        }

        if(dbHelper == null){
            dbHelper = DbHelper(requireContext())
        }

        val transactionCostAccount: Account? = dbHelper!!.getTransactionCostAccountByOwner(ownerCode)




        return PettyCash(
            id = null,
            pettyCashNumber = nextPettyCashNo,
            date = pettyCashDateEditText?.text.toString(),
            amount = pettyCash.mpesaTransaction?.transactionCost,
            isDeleted = false,
            trucks = selectedTrucks,
            transactor = globalTransactor,
            account = transactionCostAccount,
            description = "Mpesa Transaction Cost on Petty Cash No: " + pettyCash.pettyCashNumber + " M-Pesa Transaction: " + pettyCash.mpesaTransaction?.mpesa_code,
            mpesaTransaction = null,
            owner = globalSelectedOwner,
            paymentMode = chipGroup?.checkedChipId?.let { chipGroup?.findViewById<Chip>(it)?.text.toString() },
            signature = null,
            supportingDocument = null
        )
    }

    private fun getSelectedTrucks(): MutableList<Truck> {
        val selectedTrucksString = trucksAutoCompleteTextView?.text.toString()
        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())
        }

        println("Selected Trucks String: $selectedTrucksString")

        if (selectedTrucksString.trim() == "Select All ,"){
            return dbHelper!!.getLocalTrucksByOwner(globalSelectedOwner).toMutableList()
        }

        return selectedTrucksString.split(" ,")
            .mapNotNull { it.trim().takeIf { it.isNotEmpty() }?.let { dbHelper?.getTruckByTruckNumber(it) } }
            .toMutableList()
    }

    interface OnAddPettyCashListener {
        fun onAddPettyCash(pettyCash: PettyCash, transactionCostPettyCash: PettyCash?)

    }

    fun setOnAddPettyCashListener(listener: OnAddPettyCashListener) {
        this.onAddPettyCashListener = listener

    }


    private fun encodeUriToBase64(context: Context, uri: Uri): String? {
        return try {
            // Get the bitmap from the Uri
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Compress bitmap to ByteArrayOutputStream
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()

            // Encode to Base64
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }


    private fun checkAmountsEquality(amountValidation: Boolean, totalAmountValidation: Boolean): Boolean {
        if(amountValidation && totalAmountValidation){
            println("here we go")
            return amountEditText?.text.toString().substringBefore(" KES").replace(",", "").toDouble() == supportingDocumentTotalAmountEditText?.text.toString().toDouble()
        }else{
            return false
        }

    }


    private fun setUpSupportingDocumentSection() {
        // Initialize the switch with its listener
        supportingDocumentSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                supportingDocumentFormLinearLayout?.let { expandLayout(it) }
            } else {
                supportingDocumentFormLinearLayout?.let { collapseLayout(it) }
            }
        }

        val documentTypes = listOf("Tax Invoice", "Non-Tax Invoice", "Tax Cash Receipt", "Non-Tax Cash Receipt")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, documentTypes)

        supportingDocumentTypeAutoCompleteTextView?.setAdapter(adapter)

        supportingDocumentDateTextInputLayout?.let { supportingDocumentDateEditText?.let { it1 ->
            setupDatePickerForField(it,
                it1, requireActivity().supportFragmentManager)
        } }

    }

    private fun setupMpesaTransactionAutoCompleteTextView() {
        val adapter = MpesaAutoCompleteTextInputAdapter(requireContext(), emptyList())
        mpesaTransactionAutoCompleteTextView?.setAdapter(adapter)

        mpesaTransactionAutoCompleteTextView?.addTextChangedListener(object : TextWatcher {
            private var previousText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s.toString() // Store the previous text
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s.toString()

                // Detect backspace when text length decreases
                if (before > 0 && currentText.length < previousText.length && previousText.length == 10) {
                    mpesaTransactionAutoCompleteTextView?.text?.clear() // Clear the entire transaction code
                    globalMpesaTransaction = null // Reset the selected transaction
                    amountEditText?.isEnabled = true // Re-enable amount input
                    return
                }

                searchMpesaTransactions(currentText)
                handleBackspace(mpesaTransactionAutoCompleteTextView!!)

                if (currentText.isEmpty()) {
                    amountEditText?.isEnabled = true
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun searchMpesaTransactions(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (dbHelper == null) {
                dbHelper = DbHelper(requireContext())
            }
            val results = dbHelper!!.getTransactionsFromQueryInTextInput(query)
            Log.d(TAG, "Results: $results") // Log results

            withContext(Dispatchers.Main) {
                // Check if results are not empty
                if (results.isNotEmpty()) {
                    val adapter = MpesaAutoCompleteTextInputAdapter(requireContext(), results)

                    // Update the AutoCompleteTextView suggestions
                    mpesaTransactionAutoCompleteTextView?.setAdapter(adapter)

                    // Optional: Notify the adapter that data has changed
                    adapter.notifyDataSetChanged()

                    // Handle item selection
                    mpesaTransactionAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                        Log.d(TAG, "Position: $position") // Log the position
                        val selectedTransaction = adapter.getItem(position)
                        globalMpesaTransaction = selectedTransaction

                        mpesaTransactionAutoCompleteTextView?.setText(selectedTransaction?.mpesa_code, false)
                        selectedTransaction?.amount?.let { amountEditText?.let { it1 ->
                            simulateTyping(
                                it1, it)
                        } }
                        amountEditText?.isEnabled = false
                        pettyCashDateEditText?.setText(selectedTransaction?.transaction_date)
                        pettyCashDateTextInputLayout?.let { pettyCashDateEditText?.let { it1 ->
                            showDatePickerCorrespondingToEditText(it,
                                it1, requireActivity().supportFragmentManager)
                        } }
                        val name = getTitleTextByTransactionTypeWithoutFormatting(selectedTransaction!!)

                        println(name)
                        if(dbHelper == null){
                            dbHelper = DbHelper(requireContext())
                        }
                        val selectedTransactor = dbHelper!!.getTransactorByName(name.trim())
                        println("Selected Transactor: $selectedTransactor")
                        transactorAutoCompleteTextView?.setText(selectedTransactor.first().name?.let {
                            capitalizeEachWord(
                                it
                            )
                        })
                        globalTransactor = selectedTransactor.first()

                        // Move the cursor to the end of the text
                        mpesaTransactionAutoCompleteTextView?.setSelection(selectedTransaction?.mpesa_code!!.length)

                        Log.d(TAG, "Selected Transaction:" + selectedTransaction.mpesa_code)


                    }
                } else {
                    Log.d(TAG, "No results found for query: $query")
                }
            }
        }
    }


    private fun expandLayout(layout: View) {
        layout.visibility = View.VISIBLE

        val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((view?.width ?: 0), View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        layout.measure(matchParentMeasureSpec, wrapContentMeasureSpec)

        val targetHeight = layout.measuredHeight

        // Set initial height to 0 and animate to target height
        layout.layoutParams.height = 0
        layout.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener { animation ->
            layout.layoutParams.height = animation.animatedValue as Int
            layout.requestLayout()
        }
        animator.duration = 300 // duration in ms
        animator.start()
    }

    private fun collapseLayout(layout: View) {
        val initialHeight = layout.measuredHeight

        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener { animation ->
            layout.layoutParams.height = animation.animatedValue as Int
            layout.requestLayout()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                layout.visibility = View.GONE
            }
        })
        animator.duration = 300 // duration in ms
        animator.start()
    }


    private fun showImageSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Image Source")
            .setItems(arrayOf("Gallery", "Camera")) { dialog, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .show()
    }

    // Open gallery to pick an image
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    // Open camera to take a photo
    private fun openCamera() {
        // Set up the scanner options for document scanning
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)  // Disable gallery import
            .setPageLimit(2)                 // Limit number of pages
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)  // Output formats
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)  // Full page scanning
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        // Start scanning using the scanner client
        scanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to start document scanner", Toast.LENGTH_SHORT).show()
            }
    }

    // Handle the result from the gallery or camera
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                QR_CODE_REQUEST_CODE -> {
                    val result = IntentIntegrator.parseActivityResult(Activity.RESULT_OK, data)
                    println("Result: $result")
                    result?.contents?.let { qrContent ->
                        val invoiceNoPattern = "https://itax.kra.go.ke/KRA-Portal/invoiceChk.htm\\?actionCode=loadPage&invoiceNo=([\\d]+)"
                        val regex = Regex(invoiceNoPattern)
                        val matchResult = regex.find(qrContent)

                        if (matchResult != null) {
                            val invoiceNo = matchResult.groupValues[1]
                            // Use the invoiceNo as needed
                            println("Invoice No: $invoiceNo")
                            getNewSessionCookie(invoiceNo)
                        } else {
                            Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show()
                    }
                }
                IMAGE_REQUEST_CODE -> {
                    val imageUri: Uri? = data.data
                    imageUri?.let {
                        handleAndSaveImage(it)
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val imageUri: Uri? = data.getParcelableExtra("imageUri")
                    imageUri?.let {
                        handleAndSaveImage(it)
                    }
                }
            }
        }
    }

    private fun handleAndSaveImage(imageUri: Uri) {
        val savedFilePath = saveImageAsFile(imageUri)
        if (savedFilePath != null) {
            selectedImagesPath.add(savedFilePath) // Add file path instead of URI
            addImageToLayout(Uri.fromFile(File(savedFilePath))) // Pass Uri for display if needed
        }
    }


    // This function will detect the document and return the modified Bitmap
    private fun addImageToLayout(imageUri: Uri) {
        // Create a new FrameLayout to hold the image and the close icon
        val frameLayout = FrameLayout(requireContext())

        // Set layout params for each image with margins for spacing
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 8, 8, 8)
        frameLayout.layoutParams = layoutParams

        // Use ShapeableImageView for rounded corners
        val imageView = ShapeableImageView(requireContext())
        imageView.layoutParams = FrameLayout.LayoutParams(110.dpToPx(), 110.dpToPx())
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageURI(imageUri)

        // Set rounded corners using ShapeAppearanceModel
        imageView.shapeAppearanceModel = imageView.shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(12.dpToPx().toFloat()) // Adjust corner size as needed
            .build()

        // "X" icon for removing the image, slightly outside the image
        val closeIcon = ImageView(requireContext())
        val closeIconParams = FrameLayout.LayoutParams(20.dpToPx(), 20.dpToPx())
        closeIconParams.gravity = Gravity.END or Gravity.TOP
        closeIconParams.setMargins(0, 3.dpToPx(), 3.dpToPx(), 0) // Positioning outside top-right
        closeIcon.layoutParams = closeIconParams
        closeIcon.setImageResource(R.drawable.circular_red_close)
        closeIcon.setOnClickListener {
            // Remove image from view and list
            linearLayoutImages?.removeView(frameLayout)
            selectedImagesUri.removeIf { it.toString() == imageUri.toString() }
            println("SelectedImagesUri Size: " + selectedImagesUri.size)
            selectedImagesPath.remove(imageUri.path)
            println("SelectedImagesPath Size: " + selectedImagesPath.size)
            Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show()
            if (selectedImagesUri.isEmpty()) {
                addImageIcon?.visibility = View.VISIBLE
                horizontalScrollView?.visibility = View.GONE
            }
        }

        // Add ImageView and close icon to the FrameLayout
        frameLayout.addView(imageView)
        frameLayout.addView(closeIcon)

        // Add FrameLayout (with image and close icon) to the dotted box (which is a horizontal LinearLayout)
        linearLayoutImages?.addView(frameLayout)

        if (selectedImagesUri.isNotEmpty()) {
            addImageIcon?.visibility = View.GONE
            horizontalScrollView?.visibility = View.VISIBLE
        }else{
            addImageIcon?.visibility = View.VISIBLE
            horizontalScrollView?.visibility = View.GONE
        }
    }


    // Utility to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }


    fun setupDatePickerForField(
        dateInputLayout: TextInputLayout,
        dateEditText: TextInputEditText,
        fragmentManager: FragmentManager // Required to show the DatePicker dialog
    ) {
        // Get the current date in milliseconds
        val today = System.currentTimeMillis()

        // Set up the Material Date Picker
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Select Date")

        // Set the default selection to the date in the EditText, if available
        val existingDateText = dateEditText.text.toString()
        val initialSelection = if (existingDateText.isNotBlank()) {
            // Parse the existing date string to milliseconds
            try {
                val existingDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(existingDateText)
                existingDate?.time ?: today // Use existing date or fallback to today
            } catch (e: ParseException) {
                today // If parsing fails, fallback to today's date
            }
        } else {
            today // Fallback to today's date if no date is present
        }

        builder.setSelection(initialSelection) // Set initial selection

        // Create the DatePicker instance
        val datePicker = builder.build()

        // Show DatePicker when clicking on the end icon
        dateInputLayout.setEndIconOnClickListener {
            datePicker.show(fragmentManager, datePicker.toString())
        }

        // Handle the selected date
        datePicker.addOnPositiveButtonClickListener { selection ->
            // Convert the selected date (in milliseconds) to a formatted string
            val selectedDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(selection))
            // Set the selected date to the EditText
            dateEditText.setText(selectedDate)
        }

        // Set today's date by default in the TextInputEditText if no other date is set
        if (existingDateText.isBlank()) {
            val defaultDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(today))
            dateEditText.setText(defaultDate)
        }
    }





    private fun setUpAccountsAutoCompleteTextView() {
        accountsAutoCompleteTextView?.threshold = 1 // Start searching after 1 character

        accountsAutoCompleteTextView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                searchAccounts(s.toString())

            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchAccounts(query: String) {

        CoroutineScope(Dispatchers.IO).launch {
            if (dbHelper == null) {
                dbHelper = DbHelper(requireContext())
            }
            val results = dbHelper!!.getAccountsByNameAndOwner(query, globalSelectedOwner)

            withContext(Dispatchers.Main) {

                // Prepare the list of results and always add "Add new transactor" option at the end
                val accountNames = results.map { it.name }.toMutableList()

                // Update the AutoCompleteTextView suggestions
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    accountNames
                )
                accountsAutoCompleteTextView?.setAdapter(adapter)

                // Handle item selection
                accountsAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    globalAccount = results.find { it.name == selectedItem }
                    accountsAutoCompleteTextView?.setText(selectedItem, false)
                    // Move the cursor to the end of the text
                    accountsAutoCompleteTextView?.setSelection(selectedItem.length)
                    Log.d(TAG, "Selected Account: $selectedItem")
                    Log.d(TAG, "Global Account: ${globalAccount?.name}")

                }
            }
        }

    }





    private fun setupTrucksAutoCompleteTextView() {
        var truckNoList = trucks.map { it.truckNo }.toMutableList()
        println("Trucks: $truckNoList")
        if (trucks.size > 1){
            truckNoList.add("All")
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, truckNoList)
        trucksAutoCompleteTextView?.setAdapter(adapter)

        // Use a custom tokenizer to simulate "chips"
        trucksAutoCompleteTextView?.threshold = 1 // Start searching after 1 character

        trucksAutoCompleteTextView?.setTokenizer(SpaceTokenizer()) // Custom Tokenizer
        trucksAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
            val selectedTruckNo = parent.getItemAtPosition(position) as String
            addChip(selectedTruckNo)
        }

        // Add a TextWatcher to monitor text changes
        trucksAutoCompleteTextView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (before > 0 && count == 0) {
                    // Backspace detected
                    handleBackspace(trucksAutoCompleteTextView!!)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun handleBackspace(autoCompleteTextView: AutoCompleteTextView) {
        val currentText = autoCompleteTextView.text.toString()

        // Trim any leading/trailing whitespace
        val trimmedText = currentText.trim()

        // Check if the text ends with a comma and space
        if (trimmedText.endsWith(",") || trimmedText.endsWith(", ") || trimmedText.endsWith(".")) {
            // Find the index of the last truck number before the comma
            val lastCommaIndex = trimmedText.lastIndexOf(", ", trimmedText.length - 3) // Second-last comma

            // If there is a second-last comma, remove the last truck number
            if (lastCommaIndex != -1) {
                val updatedText = trimmedText.substring(0, lastCommaIndex + 2) // Keep text up to the second-last comma and space
                autoCompleteTextView.setText(updatedText)
                autoCompleteTextView.setSelection(updatedText.length) // Move cursor to end
            } else {
                // If no second-last comma, clear the field entirely
                autoCompleteTextView.setText("")
            }
        }
    }

    // Custom Tokenizer to separate chips
    class SpaceTokenizer : MultiAutoCompleteTextView.Tokenizer {
        override fun findTokenStart(text: CharSequence, cursor: Int): Int {
            var i = cursor

            while (i > 0 && text[i - 1] != ' ') {
                i--
            }
            while (i < cursor && text[i] == ' ') {
                i++
            }

            return i
        }

        override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
            var i = cursor
            val len = text.length

            while (i < len) {
                if (text[i] == ' ') {
                    return i
                } else {
                    i++
                }
            }

            return len
        }

        override fun terminateToken(text: CharSequence): CharSequence {
            var i = text.length

            while (i > 0 && text[i - 1] == ' ') {
                i--
            }

            return if (i > 0 && text[i - 1] == ' ') {
                text
            } else {
                "$text "
            }
        }
    }

    private fun addChip(truckNo: String) {
        // Add the selected truck number to the AutoCompleteTextView
        val currentText = trucksAutoCompleteTextView?.text.toString()
        trucksAutoCompleteTextView?.setText("$currentText, ") // Append the new truck number
        trucksAutoCompleteTextView?.setSelection(trucksAutoCompleteTextView!!.text.length) // Move cursor to end
    }




    private fun getOwners(): MutableList<Owner> {
        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())
        }
        return dbHelper?.getAllOwners()?.toMutableList() ?: mutableListOf<Owner>()

    }

    private fun getTrucks(): MutableList<Truck>{

        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())
        }
        return dbHelper?.getLocalTrucks()!!.toMutableList()
    }

    private fun setupCompanyCarousel() { val carouselLayoutManager = CarouselLayoutManager()
        ownerCarouselRecyclerView?.layoutManager = carouselLayoutManager

        // Attach CarouselSnapHelper to RecyclerView
        val snapHelper = CarouselSnapHelper()
        snapHelper.attachToRecyclerView(ownerCarouselRecyclerView)

        ownerCarouselAdapter = OwnerPettyCashAdapter(owners) { position ->
            // Handle item click
            val selectedOwner = owners[position]

            if (globalSelectedOwner != selectedOwner) {
                trucksAutoCompleteTextView?.setText("")
            }

            globalSelectedOwner = selectedOwner
            Toast.makeText(requireContext(), "Selected: ${selectedOwner.name}", Toast.LENGTH_SHORT).show()
            trucks = getTrucksByOwner(selectedOwner)
            setUpPettyCashNo(selectedOwner.ownerCode)
            var truckNoList = trucks.map { it.truckNo }.toMutableList()
            println("Trucks: $truckNoList")
            if (trucks.size > 1){
                truckNoList.add("Select All")
            }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, truckNoList )
            trucksAutoCompleteTextView?.setAdapter(adapter)
            ownerCarouselAdapter?.setSelectedPosition(position) // Update selected position
        }

        ownerCarouselRecyclerView?.adapter = ownerCarouselAdapter
        Log.d(TAG, "Adapter set with ${owners.size} owners") // Log the number of owners set in the adapter
    }

    private fun getTrucksByOwner(owner: Owner): MutableList<Truck>{
        if (dbHelper == null) {
            dbHelper = DbHelper(requireContext())
        }
        return dbHelper?.getLocalTrucksByOwner(owner)?.toMutableList() ?: mutableListOf<Truck>()
    }


    private fun setupAutoCompleteTextView() {
        transactorAutoCompleteTextView?.threshold = 1 // Start searching after 1 character

        transactorAutoCompleteTextView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    // No input, hide spinner and icons
                    progressBar?.visibility = View.GONE
                } else {
                    // Show a progress spinner while searching
                    progressBar?.visibility = View.VISIBLE
                    searchTransactors(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupAmountEditText() {
        amountEditText?.addTextChangedListener(object : TextWatcher {
            private var current = ""
            private var isDecimalInput = false // Flag to check if we are in decimal input mode
            private var decimalPlacesCount = 0 // To count the decimal places
            private var isDeleting = false // Track if backspace is pressed

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Detect if the user is pressing backspace by checking if text is being removed
                isDeleting = count > after
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    amountEditText?.removeTextChangedListener(this)

                    // Clean input to allow only digits and a single decimal point
                    val cleanString = s.toString().replace("[^\\d.]".toRegex(), "")
                    Log.d(TAG, "Clean String: $cleanString")

                    if (cleanString.contains(".")) {
                        isDecimalInput = true
                        val parts = cleanString.split(".")

                        // Handle whole numbers and decimal part
                        val wholePart = parts[0]
                        val decimalPart = if (parts.size > 1) parts[1] else ""
                        decimalPlacesCount = decimalPart.length

                        // Handle backspace deletion in decimal mode
                        if (isDeleting && decimalPlacesCount > 0) {
                            // Remove the last digit from the decimal part
                            current = if (decimalPart.length == 1) {
                                // If the decimal part has only one digit, keep the decimal point
                                wholePart + "."
                            } else {
                                wholePart + "." + decimalPart.dropLast(1) // Drop the last decimal digit
                            }
                        } else if (isDeleting && decimalPlacesCount == 0) {
                            // If the decimal part is empty after deletion, remove the decimal point
                            current = wholePart
                        } else {
                            // Limit whole numbers to 9 digits and decimal to 2 digits
                            val formattedWholePart = if (wholePart.length > 9) {
                                wholePart.substring(0, 9)
                            } else {
                                wholePart
                            }

                            // Handle when decimal is pressed but no digit after it
                            current = if (decimalPlacesCount == 0 && cleanString.endsWith(".")) {
                                formattedWholePart + "." // Just add the decimal, no zero
                            } else if (decimalPlacesCount > 2) {
                                formattedWholePart + "." + decimalPart.substring(0, 2) // Limit decimal to 2 digits
                            } else {
                                formattedWholePart + (if (decimalPlacesCount > 0) "." + decimalPart else "")
                            }
                        }
                    } else {
                        // If no decimal point, just treat it as a whole number
                        isDecimalInput = false
                        val numberPart = cleanString.replace(",", "")
                        current = if (numberPart.isEmpty()) {
                            "0" // If everything is deleted, set current to "0"
                        } else if (numberPart.length > 9) {
                            numberPart.substring(0, 9)
                        } else {
                            numberPart
                        }
                    }

                    // Format the number with commas
                    val formatted = if (current.isNotEmpty()) {
                        try {
                            val numericValue = current.toDoubleOrNull() ?: 0.0
                            if (isDecimalInput) {
                                current // Don't format when inputting decimals directly
                            } else {
                                formatNumberWithoutDecimals(numericValue) // Custom formatting for whole numbers
                            }
                        } catch (e: NumberFormatException) {
                            current // Just use the current string if parsing fails
                        }
                    } else {
                        "0" // If empty, show "0"
                    }

                    // Update the EditText with the formatted string followed by " KES"
                    amountEditText?.setText("$formatted KES")

                    // Adjust cursor position
                    if (isDecimalInput && current.contains(".")) {
                        // Keep cursor after the last digit in the decimal part
                        amountEditText?.setSelection(current.length)
                    } else {
                        amountEditText?.setSelection(amountEditText?.text?.length!! - 4) // Place cursor before " KES"
                    }

                    amountEditText?.addTextChangedListener(this)
                }
            }
        })
    }

    // Custom function to format numbers with commas without decimals
    private fun formatNumberWithoutDecimals(value: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value.toLong()) // Format as whole number
    }




    private fun searchTransactors(query: String) {
        // Perform search in the background
        CoroutineScope(Dispatchers.IO).launch {
            if (dbHelper == null) {
                dbHelper = DbHelper(requireContext())
            }
            val results = dbHelper!!.getTransactorByName(query)

            withContext(Dispatchers.Main) {
                progressBar?.visibility = View.GONE // Hide spinner when search is done

                // Prepare the list of results and always add "Add new transactor" option at the end
                val transactorNames = results.map { it.name?.let { it1 -> capitalizeEachWord(it1) } }.toMutableList()
                val transactorMap = results.associateBy { it.name?.let { it1 ->
                    capitalizeEachWord(
                        it1
                    )
                } } // Create a map for easy access

                // Always add "Add new transactor" option at the end
                if (!transactorNames.contains("Add a new transactor")) {
                    transactorNames.add("Add a new transactor") // Add the special option
                }

                // Update the AutoCompleteTextView suggestions
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    transactorNames
                )
                transactorAutoCompleteTextView?.setAdapter(adapter)

                // Handle item selection
                transactorAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                    val selectedItem = parent.getItemAtPosition(position).toString()

                    if (selectedItem == "Add a new transactor") {
                        transactorAutoCompleteTextView?.setText("", false)
                        // Handle the case when the user selects "Add new transactor"
                        showAddOrEditTransactorDialog("Add")
                    } else {
                        // Handle the case when a valid transactor is selected
                        transactorAutoCompleteTextView?.setText(selectedItem, false)
                        Log.d(TAG, "Selected Transactor: $selectedItem")
                        transactorAutoCompleteTextView?.setSelection(selectedItem.length)

                        // Retrieve the transactor object using the map
                        val transactor = transactorMap[selectedItem]
                        globalTransactor = transactor
                        if (transactor != null && transactor.transactorType == "Individual") {
                            // Now you have the transactor object; you can use it as needed
                            showSignatureLayout()
                            Log.d(TAG, "Transactor Object: $transactor")
                        } else {
                            hideSignatureLayout()
                            Log.e(TAG, "Transactor not found!")
                        }
                    }
                }
            }
        }
    }

    fun capitalizeEachWord(input: String): String? {
        val words = input.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result = StringBuilder()

        for (word in words) {
            if (!word.isEmpty()) {
                // Replace special characters with space
                val cleanedWord = word.replace(Regex("[^A-Za-z0-9]"), " ")

                val capitalizedWord = cleanedWord.split(" ").joinToString(" ") {
                    if (it.isNotEmpty()) {
                        val firstLetter = it.substring(0, 1).uppercase(Locale.getDefault())
                        val rest = it.substring(1).lowercase(Locale.getDefault())
                        "$firstLetter$rest"
                    } else {
                        ""
                    }
                }

                result.append(capitalizedWord).append(" ")
            }
        }

        return result.toString().trim()
    }

    private fun hideSignatureLayout() {
        signatureLabelTextView?.visibility = View.GONE
        signatureView?.visibility = View.GONE
        signatureClearButton?.visibility = View.GONE
    }

    private fun showSignatureLayout() {
        signatureLabelTextView?.visibility = View.VISIBLE
        signatureView?.visibility = View.VISIBLE
        signatureClearButton?.visibility = View.VISIBLE
    }


    fun showAddOrEditTransactorDialog(action: String, transactorJson: String = "") {
        val dialog = AddOrEditTransactorDialog()

        val args = Bundle()
        args.putString("Action", action)
        if (transactorJson != "") {
            args.putString("TransactorJson", transactorJson)
        }
        dialog.arguments = args

        dialog.setOnAddTransactorListener(this)
        dialog.show(requireActivity().supportFragmentManager, "AddOrEditTransactorDialog")

    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }



    // Function to get a new session cookie by making a GET request asynchronously
    private fun getNewSessionCookie(invoiceNo: String) {

        val loadingDialog = createLoadingDialog()
        loadingDialog.show()

        if (!isNetworkAvailable()) {
            Log.e(TAG, "No internet connection")
            return
        }

        val client = OkHttpClient()
        println("Hmma ")

        // Create the GET request
        val request = Request.Builder()
            .url(initialUrl)
            .get()
            .build()

        // Make the network request asynchronously using enqueue
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get session cookie: ${e.message}")
                loadingDialog.dismiss()
                val justification = listOf("Failed to get session cookie: ${e.message}, Please try again.")
                requireActivity().runOnUiThread {
                    createSaveErrorDialog(
                        R.drawable.baseline_warning_amber_white_24dp,
                        "Error",
                        justification
                    )
                    saveErrorDialog?.show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Unexpected code $response")
                        loadingDialog.dismiss()
                        val justification = listOf("Unexpected code $response, Please try again.")
                        requireActivity().runOnUiThread {
                            createSaveErrorDialog(
                                R.drawable.baseline_warning_amber_white_24dp,
                                "Error",
                                justification
                            )
                            saveErrorDialog?.show()
                        }
                    } else {
                        val cookies = response.headers("Set-Cookie")
                        for (cookie in cookies) {
                            if (cookie.startsWith("JSESSIONID")) {
                                sessionCookie = cookie
                                Log.d(TAG, "Session cookie obtained: $sessionCookie")

                                // Now that we have the cookie, proceed with the POST request
                                sendPostRequest(invoiceNo)
                                break
                            }
                        }
                    }
                }
            }
        })
    }

    // Function to send the POST request asynchronously
    // Function to send the POST request using the newly obtained session cookie
    private fun sendPostRequest(invoiceNo: String) {
        if (!::sessionCookie.isInitialized) {
            Log.e(TAG, "Session cookie is not initialized")
            return
        }



        // Create OkHttp client
        val client = OkHttpClient()

        // Create POST request body
        val requestBody = FormBody.Builder()
            .add("invNo", invoiceNo)  // This replicates the --data-urlencode part of the cURL command
            .build()

        // Build the POST request with all the headers from the cURL command, except the Accept-Encoding (which OkHttp handles)
        val request = Request.Builder()
            .url(postUrl)
            .post(requestBody)
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
            .addHeader("Connection", "keep-alive")
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Cookie", sessionCookie)  // Use the session cookie obtained earlier
            .addHeader("Host", "itax.kra.go.ke")
            .addHeader("Origin", "https://itax.kra.go.ke")
            .addHeader("Referer", "https://itax.kra.go.ke/KRA-Portal/invoiceNumberChecker.htm?actionCode=loadPageInvoiceNumber")
            .addHeader("Sec-Fetch-Dest", "empty")
            .addHeader("Sec-Fetch-Mode", "cors")
            .addHeader("Sec-Fetch-Site", "same-origin")
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()

        // Make the network request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to make POST request: ${e.message}")
                loadingDialog?.dismiss()
                val justification = listOf("Failed to make POST request: ${e.message}, Please try again.")
                requireActivity().runOnUiThread {
                    createSaveErrorDialog(
                        R.drawable.baseline_warning_amber_white_24dp,
                        "Error",
                        justification
                    )
                    saveErrorDialog?.show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Unexpected code $response")
                        loadingDialog?.dismiss()
                        val justification = listOf("Unexpected code $response, Please try again.")
                        requireActivity().runOnUiThread {
                            createSaveErrorDialog(
                                R.drawable.baseline_warning_amber_white_24dp,
                                "Error",
                                justification

                            )

                            saveErrorDialog?.show()
                        }
                    } else {
                        // Decompress the response and convert to string
                        val responseBody = it.body?.string()

                        if (responseBody != null) {
                            // Check if the response is likely an HTML page
                            val contentType = response.header("Content-Type")
                            if (contentType?.contains("text/html", ignoreCase = true) == true) {
                                Log.d(TAG, "Response is an HTML page. Skipping UI update.")
                                loadingDialog?.dismiss()
                                val justification = listOf("KRA Server Error")
                                requireActivity().runOnUiThread {
                                    createSaveErrorDialog(
                                        R.drawable.baseline_warning_amber_white_24dp,
                                        "Error",
                                        justification
                                    )
                                    /*val resp = "{\"traderSystemInvNo\":\"9020230060924\",\"mwInvNo\":\"0090976350000328375\",\"totalInvAmt\":1245,\"invDate\":\"08/03/2024\",\"supplierName\":\"Chandarana Supermarket Limited\",\"taxableAmt\":1073.27,\"taxAmt\":171.73,\"buyerName\":\"\",\"buyerPIN\":\"\",\"invTransmissionDt\":\"08/03/2024 10:37:23\",\"invCategory\":\"Tax Invoice\",\"invType\":\"Original\",\"errorDTO\":{}}"
                                    fillSupportingDocumentSection(resp)*/
                                    saveErrorDialog?.show()
                                }



                                return
                            }

                            // Log the response or handle the JSON parsing
                            Log.d(TAG, "Response: $responseBody")
                            gloablResponse = responseBody

                            requireActivity().runOnUiThread {
                                // Update your UI elements using the parsed response
                                createSaveSuccessfulDialog("KRA invoice record retrieved successfully!")
                                successfulDialog?.show()
                                println(gloablResponse)
                                fillSupportingDocumentSection(gloablResponse!!)
                            }

                            loadingDialog?.dismiss()
                        } else {
                            Log.e(TAG, "Response body is null")
                            loadingDialog?.dismiss()
                            val justification = listOf("Response body is null")
                            requireActivity().runOnUiThread {
                                createSaveErrorDialog(
                                    R.drawable.baseline_warning_amber_white_24dp,
                                    "Error",
                                    justification
                                )
                                saveErrorDialog?.show()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun fillSupportingDocumentSection(gloablResponse: String) {
        val invoiceObject = JSONObject(gloablResponse)

        val traderSystemInvNo = invoiceObject.getString("traderSystemInvNo")
        val mwInvNo = invoiceObject.getString("mwInvNo")
        val totalInvAmt = invoiceObject.getDouble("totalInvAmt")
        val invDate = invoiceObject.getString("invDate")
        val supplierName = invoiceObject.getString("supplierName")
        val taxableAmt = invoiceObject.getDouble("taxableAmt")
        val taxAmt = invoiceObject.getDouble("taxAmt")
        val buyerName = invoiceObject.getString("buyerName")
        val buyerPIN = invoiceObject.getString("buyerPIN")
        val invTransmissionDt = invoiceObject.getString("invTransmissionDt")
        val invCategory = invoiceObject.getString("invCategory")
        val invType = invoiceObject.getString("invType")
        val errorDTO = invoiceObject.getJSONObject("errorDTO")

        supportingDocumentSwitch?.isChecked = true
        supportingDocumentTypeAutoCompleteTextView?.setText(invCategory)
        supportingDocumentNumberEditText?.setText(traderSystemInvNo)
        supportingDocumentDateEditText?.setText(invDate)
        supportingDocumentDateTextInputLayout?.let { supportingDocumentDateEditText?.let { it1 ->
            showDatePickerCorrespondingToEditText(it,
                it1, requireActivity().supportFragmentManager)
        } }
        supportingDocumentTotalAmountEditText?.setText(totalInvAmt.toString())
        supportingDocumentSupplierName?.setText(supplierName)
        supportingDocumentCuNumberEditText?.setText(mwInvNo)
        supportingDocumentTotalTaxableAmountEditText?.setText(taxableAmt.toString())
        supportingDocumentTotalTaxEditText?.setText(taxAmt.toString())
        //amountEditText.setText(totalInvAmt.toString())
        amountEditText?.let { simulateTyping(it, totalInvAmt) }








    }

    private fun showDatePickerCorrespondingToEditText(
        dateInputLayout: TextInputLayout,
        dateEditText: TextInputEditText,
        fragmentManager: FragmentManager // Required to show the DatePicker dialog
    ) {
        // Get today's date in milliseconds
        val today = MaterialDatePicker.todayInUtcMilliseconds()

        // Set up the Material Date Picker
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Select Date")

        // Attempt to parse the existing date from the EditText
        val existingDateText = dateEditText.text.toString()
        Log.d("DatePicker", "Existing date text: $existingDateText")
        var initialSelection: Long

        if (existingDateText.isNotBlank()) {
            try {
                // Parse the existing date string to a Date object
                val existingDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(existingDateText)
                Log.d("DatePicker", "Existing date: $existingDate")

                // Adjust for UTC +3 timezone
                // Create a calendar instance and set the time based on the parsed date
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
                existingDate?.let { calendar.time = it }

                // Convert to milliseconds
                initialSelection = calendar.timeInMillis
                Log.d("DatePicker", "Initial selection: $initialSelection")
            } catch (e: Exception) {
                Log.e("DatePicker", "Failed to parse date: ${e.message}")
                initialSelection = today // Fallback to today's date if parsing fails
            }
        } else {
            initialSelection = today // Fallback to today's date if no date is present
        }

        Log.d("DatePicker", "Initial selection: $initialSelection")

        // Set the initial selection for the date picker
        builder.setSelection(initialSelection.plus(24 * 60 * 60 * 1000))

        // Create the DatePicker instance
        val datePicker = builder.build()

        // Show DatePicker when clicking on the end icon
        dateInputLayout.setEndIconOnClickListener {
            datePicker.show(fragmentManager, datePicker.toString())
        }

        // Handle the selected date
        datePicker.addOnPositiveButtonClickListener { selection ->
            // Convert the selected date (in milliseconds) to a formatted string
            val selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selection))

            // Set the selected date to the EditText
            dateEditText.setText(selectedDate)


            // Log the selected date for debugging
            Log.d("DatePicker", "Selected date: $selectedDate")
        }
    }





    private fun simulateTyping(editText: EditText, amount: Double, delay: Long = 100) {
        // Check if there's existing text and remove it
        if (editText.text.isNotEmpty()) {
            editText.setText("") // Clear existing text
        }

        val handler = Handler(Looper.getMainLooper())
        val formattedText = formatAmount(amount) // Format the double value
        var index = 0

        val typingRunnable = object : Runnable {
            override fun run() {
                if (index < formattedText.length) {
                    editText.append(formattedText[index].toString())
                    index++
                    handler.postDelayed(this, delay) // Post next character with delay
                }
            }
        }

        handler.post(typingRunnable)
    }

    private fun formatAmount(amount: Double): String {
        return formatNumberWithoutDecimals(amount) + " KES" // Ensure to replace with your actual formatting logic
    }

    private fun createLoadingDialog(): AlertDialog {
        // Create a custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)
        loadingText?.text = "Loading... Please Wait"

        // Show loading dialog
        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(false)
            .create()

        return loadingDialog as AlertDialog
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

    private fun createSaveSuccessfulDialog(successMessage: String) {
        // Inflate custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.save_successful_dialog, null)

        // Set up loading text or any other views if needed
        val saveSuccessfullyText = customView.findViewById<TextView>(R.id.success_message_text)
        val checkView = customView.findViewById<CheckView>(R.id.check)
        saveSuccessfullyText.text = successMessage
        checkView.check()

        // Create and customize the dialog
        successfulDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(true)
            .create()
    }



    private fun closeDialog() {
        dismiss()
    }

    companion object {
    }

    override fun onAddTransactor(transactor: Transactor) {
        // Log the added transactor
        Log.d(TAG, "Transactor added: $transactor")

        // Update the AutoCompleteTextView by calling searchTransactors with an empty query
        searchTransactors("") // Refresh the list to include the newly added transactor

        // Optionally, set the AutoCompleteTextView to the newly added transactor
        transactorAutoCompleteTextView?.setText(transactor.name, false)
        transactor.name?.length?.let { transactorAutoCompleteTextView?.setSelection(it) }

        if (transactor.transactorType == "Individual") {
            // Now you have the transactor object; you can use it as needed
            showSignatureLayout()
            Log.d(TAG, "Transactor Object: $transactor")
        } else {
            hideSignatureLayout()
            Log.e(TAG, "Transactor not found!")
        }


    }

    class SignatureViewModel : ViewModel() {
        val signatureBitmap = MutableLiveData<Bitmap?>()
    }
}