package com.example.pettysms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.widget.NestedScrollView
import com.example.pettysms.AddOrEditTransactorDialog.Companion.REQUEST_IMAGE_CAPTURE
import com.example.pettysms.databinding.ActivityPettyCashViewerBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import xyz.schwaab.avvylib.AvatarView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.properties.Delegates
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher

// Add this interface at the top of the class
interface OnPettyCashDeletedListener {
    fun onPettyCashDeleted(pettyCashId: Int)
}

class PettyCashViewerActivity : AppCompatActivity(), AddPettyCashFragment.OnAddPettyCashListener {

    private var binding: ActivityPettyCashViewerBinding? = null
    private var amountCard: MaterialCardView? = null
    private var logoCard: MaterialCardView? = null
    private var scrollView: NestedScrollView? = null
    private var pettyCashIdChip: Chip? = null
    private var transactorCard: MaterialCardView? = null
    private var avatarView: AvatarView? = null
    private var basicInformationCard: MaterialCardView? = null
    private var supportingDocumentCard: MaterialCardView? = null
    private var signatureCard: MaterialCardView? = null
    private var pettyCashDateLayout: LinearLayout? = null
    private var pettyCashAccountLayout: LinearLayout? = null
    private var pettyCashPaymentModeLayout: LinearLayout? = null
    private var pettyCashDescriptionLayout: LinearLayout? = null
    private var pettyCashMpesaTransactionLayout: LinearLayout? = null
    private var pettyCashTrucksLayout: LinearLayout? = null
    private var pettyCashUserLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentIdLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentSupplierNameLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentTypeLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentCuNumberLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentNumberLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentTaxableAmountLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentTaxAmountLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentTotalAmountLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentDateLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentImagesLabelLayout: LinearLayout? = null
    private var pettyCashSupportingDocumentImagesLayout: LinearLayout? = null
    private var actionButtonsLayout: LinearLayout? = null
    private var transactionAmount: TextView? = null
    private var transactorDetailsLabel: TextView? = null
    private var basicInformationLabel: TextView? = null
    private var pettyCashDateLabel: TextView? = null
    private var pettyCashDateValue: TextView? = null
    private var pettyCashAccountLabel: TextView? = null
    private var pettyCashAccountValue: TextView? = null
    private var pettyCashPaymentModeLabel: TextView? = null
    private var pettyCashPaymentModeValue: TextView? = null
    private var pettyCashDescriptionLabel: TextView? = null
    private var pettyCashDescriptionValue: TextView? = null
    private var pettyCashMpesaTransactionLabel: TextView? = null
    private var pettyCashMpesaTransactionValue: TextView? = null
    private var pettyCashTrucksLabel: TextView? = null
    private var pettyCashTrucksValue: TextView? = null
    private var pettyCashUserLabel: TextView? = null
    private var pettyCashUserValue: TextView? = null
    private var pettyCashSupportingDocumentIdLabel: TextView? = null
    private var pettyCashSupportingDocumentIdValue: TextView? = null
    private var pettyCashSupportingDocumentSupplierNameLabel: TextView? = null
    private var pettyCashSupportingDocumentSupplierNameValue: TextView? = null
    private var pettyCashSupportingDocumentTypeLabel: TextView? = null
    private var pettyCashSupportingDocumentTypeValue: TextView? = null
    private var pettyCashSupportingDocumentCuNumberLabel: TextView? = null
    private var pettyCashSupportingDocumentCuNumberValue: TextView? = null
    private var pettyCashSupportingDocumentNumberLabel: TextView? = null
    private var pettyCashSupportingDocumentNumberValue: TextView? = null
    private var pettyCashSupportingDocumentTaxableAmountLabel: TextView? = null
    private var pettyCashSupportingDocumentTaxableAmountValue: TextView? = null
    private var pettyCashSupportingDocumentTaxAmountLabel: TextView? = null
    private var pettyCashSupportingDocumentTaxAmountValue: TextView? = null
    private var pettyCashSupportingDocumentTotalAmountLabel: TextView? = null
    private var pettyCashSupportingDocumentTotalAmountValue: TextView? = null
    private var pettyCashSupportingDocumentDateLabel: TextView? = null
    private var pettyCashSupportingDocumentDateValue: TextView? = null
    private var pettyCashSupportingDocumentImagesLabel: TextView? = null
    private var signatureLabel: TextView? = null
    private var image1: ImageView? = null
    private var image2: ImageView? = null
    private var image3: ImageView? = null
    private var editActionButton: FloatingActionButton? = null
    private var deleteActionButton: FloatingActionButton? = null
    private var shareActionButton: FloatingActionButton? = null
    private var transactorNameTextView: TextView? = null
    private var logoImage: ImageView? = null
    private var signatureImageView: ImageView? = null
    private var companyLogoImageBitmap: Bitmap? = null
    private var signatureBitmap: Bitmap? = null


    private var cardSurfaceLow by Delegates.notNull<Int>()
    private var colorPrimary by Delegates.notNull<Int>()
    private var colorControlNormal by Delegates.notNull<Int>()
    private var transactorColor by Delegates.notNull<Int>()
    private var pettyCash: PettyCash? = null
    private var db_helper: DbHelper? = null
    private var db: SQLiteDatabase? = null

    private val pettyCashUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("PettyCashViewer", "Received broadcast: ${intent?.action}")
            if (intent?.action == "pettycash_updated_action") {
                val updatedPettyCashJson = intent.getStringExtra("updated_petty_cash")
                Log.d("PettyCashViewer", "Received updated petty cash: $updatedPettyCashJson")
                updatedPettyCashJson?.let {
                    try {
                        val gson = Gson()
                        val updatedPettyCash = gson.fromJson(it, PettyCash::class.java)
                        Log.d("PettyCashViewer", "Successfully parsed petty cash, updating UI")
                        updateUI(updatedPettyCash)
                    } catch (e: Exception) {
                        Log.e("PettyCashViewer", "Error updating UI: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPettyCashViewerBinding.inflate(layoutInflater)
        amountCard = binding?.amountCard
        transactorNameTextView = binding?.transactorName
        logoCard = binding?.logoCard
        scrollView = binding?.scrollView
        pettyCashIdChip = binding?.pettyCashIdChip
        transactorCard = binding?.transactorCard
        avatarView = binding?.avatarView
        basicInformationCard = binding?.basicInformationCard
        supportingDocumentCard = binding?.supportingDocumentCard
        signatureCard = binding?.signatureCard
        pettyCashDateLayout = binding?.pettyCashDateLayout
        pettyCashAccountLayout = binding?.pettyCashAccountLayout
        pettyCashPaymentModeLayout = binding?.pettyCashPaymentModeLayout
        pettyCashDescriptionLayout = binding?.pettyCashDescriptionLayout
        pettyCashMpesaTransactionLayout = binding?.pettyCashMpesaTransactionLayout
        pettyCashTrucksLayout = binding?.pettyCashTrucksLayout
        pettyCashUserLayout = binding?.pettyCashUserLayout
        pettyCashSupportingDocumentIdLayout = binding?.pettyCashSupportingDocumentIdLayout
        pettyCashSupportingDocumentSupplierNameLayout = binding?.pettyCashSupportingDocumentSupplierNameLayout
        pettyCashSupportingDocumentTypeLayout = binding?.pettyCashSupportingDocumentTypeLayout
        pettyCashSupportingDocumentCuNumberLayout = binding?.pettyCashSupportingDocumentCuNumberLayout
        pettyCashSupportingDocumentNumberLayout = binding?.pettyCashSupportingDocumentNumberLayout
        pettyCashSupportingDocumentTaxableAmountLayout = binding?.pettyCashSupportingDocumentTaxableAmountLayout
        pettyCashSupportingDocumentTaxAmountLayout = binding?.pettyCashSupportingDocumentTaxAmountLayout
        pettyCashSupportingDocumentTotalAmountLayout = binding?.pettyCashSupportingDocumentTotalAmountLayout
        pettyCashSupportingDocumentDateLayout = binding?.pettyCashSupportingDocumentDateLayout
        pettyCashSupportingDocumentImagesLabelLayout = binding?.pettyCashSupportingDocumentImagesLabelLayout
        pettyCashSupportingDocumentImagesLayout = binding?.pettyCashSupportingDocumentImagesLayout
        actionButtonsLayout = binding?.actionButtonsLayout
        transactionAmount = binding?.transactionAmount
        transactorDetailsLabel = binding?.transactorDetailsLabel
        basicInformationLabel = binding?.basicInformationLabel
        pettyCashDateLabel = binding?.pettyCashDateLabel
        pettyCashDateValue = binding?.pettyCashDateValue
        pettyCashAccountLabel = binding?.pettyCashAccountLabel
        pettyCashAccountValue = binding?.pettyCashAccountValue
        pettyCashPaymentModeLabel = binding?.pettyCashPaymentModeLabel
        pettyCashPaymentModeValue = binding?.pettyCashPaymentModeValue
        pettyCashDescriptionLabel = binding?.pettyCashDescriptionLabel
        pettyCashDescriptionValue = binding?.pettyCashDescriptionValue
        pettyCashMpesaTransactionLabel = binding?.pettyCashMpesaTransactionLabel
        pettyCashMpesaTransactionValue = binding?.pettyCashMpesaTransactionValue
        pettyCashTrucksLabel = binding?.pettyCashTrucksLabel
        pettyCashTrucksValue = binding?.pettyCashTrucksValue
        pettyCashUserLabel = binding?.pettyCashUserLabel
        pettyCashUserValue = binding?.pettyCashUserValue
        pettyCashSupportingDocumentIdLabel = binding?.pettyCashSupportingDocumentIdLabel
        pettyCashSupportingDocumentIdValue = binding?.pettyCashSupportingDocumentIdValue
        pettyCashSupportingDocumentSupplierNameLabel = binding?.pettyCashSupportingDocumentSupplierNameLabel
        pettyCashSupportingDocumentSupplierNameValue = binding?.pettyCashSupportingDocumentSupplierNameValue
        pettyCashSupportingDocumentTypeLabel = binding?.pettyCashSupportingDocumentTypeLabel
        pettyCashSupportingDocumentTypeValue = binding?.pettyCashSupportingDocumentTypeValue
        pettyCashSupportingDocumentCuNumberLabel = binding?.pettyCashSupportingDocumentCuNumberLabel
        pettyCashSupportingDocumentCuNumberValue = binding?.pettyCashSupportingDocumentCuNumberValue
        pettyCashSupportingDocumentNumberLabel = binding?.pettyCashSupportingDocumentNumberLabel
        pettyCashSupportingDocumentNumberValue = binding?.pettyCashSupportingDocumentNumberValue
        pettyCashSupportingDocumentTaxableAmountLabel = binding?.pettyCashSupportingDocumentTaxableAmountLabel
        pettyCashSupportingDocumentTaxableAmountValue = binding?.pettyCashSupportingDocumentTaxableAmountValue
        pettyCashSupportingDocumentTaxAmountLabel = binding?.pettyCashSupportingDocumentTaxAmountLabel
        pettyCashSupportingDocumentTaxAmountValue = binding?.pettyCashSupportingDocumentTaxAmountValue
        pettyCashSupportingDocumentTotalAmountLabel = binding?.pettyCashSupportingDocumentTotalAmountLabel
        pettyCashSupportingDocumentTotalAmountValue = binding?.pettyCashSupportingDocumentTotalAmountValue
        pettyCashSupportingDocumentDateLabel = binding?.pettyCashSupportingDocumentDateLabel
        pettyCashSupportingDocumentDateValue = binding?.pettyCashSupportingDocumentDateValue
        pettyCashSupportingDocumentImagesLabel = binding?.pettyCashSupportingDocumentImagesLabel
        signatureLabel = binding?.signatureLabel
        logoImage = binding?.logoImage
        image1 = binding?.image1
        image2 = binding?.image2
        image3 = binding?.image3
        signatureImageView = binding?.signatureImage
        editActionButton = binding?.editActionButton
        deleteActionButton = binding?.deleteActionButton
        shareActionButton = binding?.shareActionButton
        db_helper = DbHelper(this)
        db = db_helper?.writableDatabase

        // Fix how we get the petty cash number
        val pettyCashNumber = intent.getStringExtra("petty_cash_number")
        Log.d("PettyCashViewerActivity", "Received petty cash number: $pettyCashNumber")

        pettyCash = pettyCashNumber?.let { db_helper?.getPettyCashByPettyCashNumber(it) }

        Log.d("PettyCashViewerActivity", "PettyCash Number: ${pettyCash?.pettyCashNumber}")


        cardSurfaceLow = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0)
        colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0)
        colorControlNormal = MaterialColors.getColor(this, com.google.android.material.R.attr.colorControlNormal, 0)

        transactorColor = ColorDrawable(Color.parseColor(pettyCash?.transactor?.avatarColor ?: "#000000")).color










        companyLogoSetup()
        transactorNameSetup()
        setupPettyCashNumber()
        setUpAvatarView()
        setUpAmountCard()
        setUpSupportingDocumentCard()
        setStatusBarGradient(window, transactorColor, cardSurfaceLow)
        basicInformationCardSetup()
        supportingDocumentCardSetup()
        setUpSignatureCard()
        setUpActionButtons()









        setContentView(binding!!.root)

        // Register broadcast receiver with intent filter
        val filter = IntentFilter("pettycash_updated_action")
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(pettyCashUpdateReceiver, filter)
        
        Log.d("PettyCashViewer", "Registered broadcast receiver")
    }

    private fun setUpActionButtons() {
        shareActionButton?.setOnClickListener {
            showPettyCashViewPickerDialog()
        }
        editActionButton?.setOnClickListener {
            showAddOrEditPettyCashDialog(pettyCash)
        }
        deleteActionButton?.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showAddOrEditPettyCashDialog(pettyCash: PettyCash?, actionType: String = "Edit") {
        val existingFragment = supportFragmentManager.findFragmentByTag("fragment_add_petty_cash")

        if (existingFragment != null && existingFragment.isVisible) {
            return
        }

        val dialogFragment = AddPettyCashFragment()

        // Set the listener
        dialogFragment.setOnAddPettyCashListener(this)

        val bundle = Bundle().apply {
            if (pettyCash != null) {
                pettyCash.id?.let { putInt("pettyCash", it) }
            }
            putString("action", actionType)
        }

        dialogFragment.arguments = bundle
        dialogFragment.show(supportFragmentManager, "fragment_add_petty_cash")
    }

    // Implement the interface method
    override fun onAddPettyCash(pettyCash: PettyCash, transactionCostPettyCash: PettyCash?) {
        Log.d("PettyCashViewer", "onAddPettyCash called with ID: ${pettyCash.id}")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Update main petty cash
                db_helper?.updatePettyCash(pettyCash)
                
                // Update transaction cost if present
                transactionCostPettyCash?.let { 
                    db_helper?.updatePettyCash(it)
                }

                withContext(Dispatchers.Main) {
                    // Update the UI directly
                    updateUI(pettyCash)
                    
                    // Store the updated petty cash
                    this@PettyCashViewerActivity.pettyCash = pettyCash

                    // Set result with just the IDs
                    val resultIntent = Intent().apply {
                        putExtra("updated_petty_cash_id", pettyCash.id)
                        putExtra("transaction_cost_id", transactionCostPettyCash?.id)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)

                    // Send broadcast with just the IDs
                    val broadcastIntent = Intent("pettycash_updated_action")
                    broadcastIntent.putExtra("updated_petty_cash_id", pettyCash.id)
                    transactionCostPettyCash?.let {
                        broadcastIntent.putExtra("transaction_cost_id", it.id)
                    }
                    LocalBroadcastManager.getInstance(this@PettyCashViewerActivity)
                        .sendBroadcast(broadcastIntent)
                    
                    Toast.makeText(this@PettyCashViewerActivity, 
                        "Petty cash updated successfully", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("PettyCashViewer", "Error updating petty cash: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PettyCashViewerActivity,
                        "Error updating petty cash: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showPettyCashViewPickerDialog() {
        val options = arrayOf("Simplified", "Detailed")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Petty Cash View")
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    "Simplified" -> {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
                        } else {
                            simplifiedViewPettyCashShare()
                        }
                    }
                    "Detailed" -> {
                        detailedViewPettyCashShare()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun simplifiedViewPettyCashShare() {
        val file = generateSimplifiedPettyCashPdf()
        if (file != null) {
            sharePdfFile(file)
        }


    }

    private fun sharePdfFile(file: File) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/pdf"

        // Get the URI for the file using FileProvider
        val fileUri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)
        grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Add the file URI as an extra stream to the intent
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

        // Grant read permission to the receiving app
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Create a chooser to select the app
        val chooser = Intent.createChooser(shareIntent, "Share PDF via")

        // Verify that the intent will resolve to an activity
        if (shareIntent.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            Toast.makeText(this, "No app found to share PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateSimplifiedPettyCashPdf(): File? {
        try {
            // Initialize the PdfDocument
            val pdf = PdfDocument()
            val pageWidth = 600
            val pageHeight = 800
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Draw company logo and details
            val logo = companyLogoImageBitmap ?: BitmapFactory.decodeResource(resources, R.drawable.p_logo_cropped)

            // Get the original width and height of the logo
            val originalWidth = logo.width
            val originalHeight = logo.height

            // Define the maximum width and height you want for the scaled image
            val maxWidth = 90
            val maxHeight = 100

            // Calculate the aspect ratio of the original image
            val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

            // Calculate the new dimensions maintaining the aspect ratio
            var scaledWidth = maxWidth
            var scaledHeight = (scaledWidth / aspectRatio).toInt()

            // If the scaled height exceeds the maximum height, scale it by height instead
            if (scaledHeight > maxHeight) {
                scaledHeight = maxHeight
                scaledWidth = (scaledHeight * aspectRatio).toInt()
            }

            // Scale the image to the new dimensions while maintaining the aspect ratio
            val scaledLogo = Bitmap.createScaledBitmap(logo, scaledWidth, scaledHeight, true)

            canvas.drawBitmap(scaledLogo, 25f, 25f, paint)

            // Set paint attributes for text
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(pettyCash?.owner?.name?.uppercase(Locale.getDefault()).orEmpty(), 160f, 75f, paint)

            // Draw voucher title
            paint.textSize = 16f
            // Assuming canvas width and height
            val canvasWidth = canvas.width
            val canvasHeight = canvas.height

            // Text to be drawn
            val text = "PETTY CASH VOUCHER"

            // Measure the width of the text
            val textWidth = paint.measureText(text)

            // Calculate the X position to center the text
            val xPos = (canvasWidth - textWidth) / 2

            // Set the Y position (you can adjust this value as needed)
            val yPos = 130f

            // Draw the text centered
            canvas.drawText(text, xPos, yPos, paint)

            // Draw petty cash fields
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("No.: ", 20f, 160f, paint)
            canvas.drawText("Date: ", 420f, 160f, paint)
            canvas.drawText("Name: ", 20f, 190f, paint)
            // Draw petty cash fields
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(pettyCash?.pettyCashNumber.orEmpty(), 50f, 160f, paint)
            canvas.drawText(pettyCash?.date.orEmpty(), 455f, 160f, paint)
            canvas.drawText(capitalizeEachWord(pettyCash?.transactor?.name.toString()).orEmpty(), 60f, 190f, paint)

            // Draw petty cash fields
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT

            // Draw table headers
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            val cornerRadius = 5f // Adjust this value to control the roundness of the corners
            canvas.drawRoundRect(20f, 220f, pageWidth - 20f, 320f, cornerRadius, cornerRadius, paint)
            paint.style = Paint.Style.FILL
            canvas.drawText("Details of payments (For What's Required)", 30f, 240f, paint)

            // Draw content inside the table
            paint.textSize = 12f
            canvas.drawText(pettyCash?.description.orEmpty(), 30f, 270f, paint)

            // Draw amount section
            canvas.drawText("Amount in words: ${convertAmountToWords(pettyCash?.amount ?: 0.0)}", 20f, 350f, paint)
            canvas.drawText("Total: ${"%.2f".format(pettyCash?.amount ?: 0.0)}", 450f, 350f, paint)

            // Draw signature section
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT

            // Checked by
            canvas.drawText("Checked by: ______________________", 20f, 400f, paint)
            // Draw signature section
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(pettyCash?.user?.name.orEmpty(), 130f, 395f, paint)

            // Draw signature section
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT

            // Authorized by
            canvas.drawText("Authorized by: ___________________", 20f, 430f, paint)
            canvas.drawText("A/C Code: _________________________", 390f, 430f, paint)
            // Draw signature section
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(pettyCash?.user?.name.orEmpty(), 130f, 425f, paint)
            canvas.drawText(pettyCash?.account?.name.orEmpty(), 460f, 425f, paint)

            // Draw signature section
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT

            // Signature of Recipient
            canvas.drawText("Signature of Recipient: ___________________________________________", 20f, 550f, paint)

            // Add signature or Mpesa code
            if (pettyCash?.paymentMode == "Cash") {
                signatureBitmap?.let {
                    val upscaledSignature = Bitmap.createScaledBitmap(it, it.width * 5, it.height * 5, true)
                    val scaledSignature = Bitmap.createScaledBitmap(upscaledSignature, 200, 100, true)
                    canvas.drawBitmap(scaledSignature, 180f, 465f, paint)
                } ?: run {
                    // Draw signature section
                    paint.textSize = 12f
                    paint.typeface = Typeface.DEFAULT_BOLD
                    canvas.drawText("No signature available", 200f, 545f, paint)
                }
            } else {
                paint.textSize = 12f
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("Mpesa Code: ${pettyCash?.mpesaTransaction?.mpesa_code.orEmpty()}", 180f, 545f, paint)
            }

            // Finish the page
            pdf.finishPage(page)

            if (pettyCash?.supportingDocument != null) {
                val imagePath1 = pettyCash?.supportingDocument?.imagePath1
                val imagePath2 = pettyCash?.supportingDocument?.imagePath2
                val imagePath3 = pettyCash?.supportingDocument?.imagePath3

                // Check if at least one image path is not null and create a page for each image
                imagePath1?.let {
                    val imageFile1 = File(it)
                    if (imageFile1.exists()) {
                        val image1Bitmap = BitmapFactory.decodeFile(imageFile1.absolutePath)
                        // Create a new page for the first image
                        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
                        val page1 = pdf.startPage(pageInfo1)
                        val canvas1 = page1.canvas
                        val paint1 = Paint()

                        // Draw the title at the top of the page
                        val titlePaint = Paint()
                        titlePaint.textSize = 22f
                        titlePaint.color = Color.BLACK
                        canvas1.drawText("${pettyCash?.pettyCashNumber} Supporting Document Image 1", 20f, 40f, titlePaint)

                        // Resizing the image to fit the page while keeping aspect ratio
                        val margin = 40f  // Equal margin from all edges
                        val imageWidth = pageWidth - 2 * margin
                        val imageHeight =
                            (image1Bitmap.height.toFloat() / image1Bitmap.width.toFloat()) * imageWidth

                        // Positioning the image below the title with a small margin
                        val imageTopPosition = 60f

                        // Define the rectangle for the image with rounded corners
                        val rectF = RectF(
                            margin,
                            imageTopPosition,
                            margin + imageWidth,
                            imageTopPosition + imageHeight
                        )

                        // Create a rounded bitmap with the radius for all corners
                        val roundedBitmap = getRoundedBitmap(
                            image1Bitmap,
                            20f
                        )  // Apply rounded corners with radius 20f

                        // Draw the rounded image with the defined position
                        canvas1.drawBitmap(roundedBitmap, null, rectF, paint)

                        // Finish the page
                        pdf.finishPage(page1)
                    }
                }

                imagePath2?.let {
                    val imageFile2 = File(it)
                    if (imageFile2.exists()) {
                        val image2Bitmap = BitmapFactory.decodeFile(imageFile2.absolutePath)
                        // Create a new page for the second image
                        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
                        val page2 = pdf.startPage(pageInfo2)
                        val canvas2 = page2.canvas
                        val paint2 = Paint()

                        // Draw the title at the top of the page
                        val titlePaint = Paint()
                        titlePaint.textSize = 22f
                        titlePaint.color = Color.BLACK
                        canvas2.drawText("${pettyCash?.pettyCashNumber} Supporting Document Image 2", 20f, 40f, titlePaint)

                        // Resizing the image to fit the page while keeping aspect ratio
                        val margin = 40f  // Equal margin from all edges
                        val imageWidth = pageWidth - 2 * margin
                        val imageHeight =
                            (image2Bitmap.height.toFloat() / image2Bitmap.width.toFloat()) * imageWidth

                        // Positioning the image below the title with a small margin
                        val imageTopPosition = 60f

                        // Define the rectangle for the image with rounded corners
                        val rectF = RectF(
                            margin,
                            imageTopPosition,
                            margin + imageWidth,
                            imageTopPosition + imageHeight
                        )

                        // Create a rounded bitmap with the radius for all corners
                        val roundedBitmap = getRoundedBitmap(
                            image2Bitmap,
                            20f
                        )  // Apply rounded corners with radius 20f

                        // Draw the rounded image with the defined position
                        canvas2.drawBitmap(roundedBitmap, null, rectF, paint)

                        // Finish the page
                        pdf.finishPage(page2)
                    }
                }

                imagePath3?.let {
                    val imageFile3 = File(it)
                    if (imageFile3.exists()) {
                        val image3Bitmap = BitmapFactory.decodeFile(imageFile3.absolutePath)
                        // Create a new page for the third image
                        val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
                        val page3 = pdf.startPage(pageInfo3)
                        val canvas3 = page3.canvas
                        val paint3 = Paint()

                        // Draw the title at the top of the page
                        val titlePaint = Paint()
                        titlePaint.textSize = 22f
                        titlePaint.color = Color.BLACK
                        canvas3.drawText("${pettyCash?.pettyCashNumber} Supporting Document Image 3", 20f, 40f, titlePaint)

                        // Resizing the image to fit the page while keeping aspect ratio
                        val margin = 40f  // Equal margin from all edges
                        val imageWidth = pageWidth - 2 * margin
                        val imageHeight =
                            (image3Bitmap.height.toFloat() / image3Bitmap.width.toFloat()) * imageWidth

                        // Positioning the image below the title with a small margin
                        val imageTopPosition = 60f

                        // Define the rectangle for the image with rounded corners
                        val rectF = RectF(
                            margin,
                            imageTopPosition,
                            margin + imageWidth,
                            imageTopPosition + imageHeight
                        )

                        // Create a rounded bitmap with the radius for all corners
                        val roundedBitmap = getRoundedBitmap(
                            image3Bitmap,
                            20f
                        )  // Apply rounded corners with radius 20f

                        // Draw the rounded image with the defined position
                        canvas3.drawBitmap(roundedBitmap, null, rectF, paint)

                        // Finish the page
                        pdf.finishPage(page3)
                    }
                }
            }






            // Save PDF to file
            val fileName = "PettyCash_${pettyCash?.pettyCashNumber?.replace("/", "_").orEmpty()}_${System.currentTimeMillis()}.pdf"
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                fileName
            )

            Log.d("PDFGeneration", "File path: ${file.absolutePath}")

            // Write the PDF content to a ByteArrayOutputStream
            val outputStream = ByteArrayOutputStream()
            pdf.writeTo(outputStream)

            // Write the content of ByteArrayOutputStream to the file
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(outputStream.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()

            Log.d("PDFGeneration", "PDF saved to: ${file.absolutePath}")

            // Close the PDF document
            pdf.close()

            // Share the generated PDF file
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Helper functions to decode Base64 strings and get rounded bitmaps
    private fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    private fun getRoundedBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawRoundRect(rectF, radius, radius, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }



    fun convertAmountToWords(amount: Double): String {
        if (amount == 0.0) return "Zero Shillings"

        val units = arrayOf("", "Thousand", "Million", "Billion")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        )

        var integerPart = amount.toInt()
        val fractionalPart = ((amount - integerPart) * 100).toInt() // Get cents if any
        var result = ""

        var unitIndex = 0
        while (integerPart > 0) {
            val chunk = integerPart % 1000
            if (chunk > 0) {
                result = "${chunkToWords(chunk, ones, tens)} ${units[unitIndex]} $result"
            }
            integerPart /= 1000
            unitIndex++
        }

        result = result.trim() + " Shillings"

        if (fractionalPart > 0) {
            result += " and ${chunkToWords(fractionalPart, ones, tens)} Cents"
        }

        return result.trim()
    }

    private fun chunkToWords(number: Int, ones: Array<String>, tens: Array<String>): String {
        val hundreds = number / 100
        val remainder = number % 100
        val tensPart = remainder / 10
        val onesPart = remainder % 10

        val result = StringBuilder()

        if (hundreds > 0) {
            result.append("${ones[hundreds]} Hundred ")
        }

        if (remainder in 1..19) {
            result.append("${ones[remainder]} ")
        } else if (tensPart > 0) {
            result.append("${tens[tensPart]} ")
            if (onesPart > 0) {
                result.append("${ones[onesPart]} ")
            }
        }

        return result.toString().trim()
    }


    private fun detailedViewPettyCashShare() {
        val file = generateDetailedPettyCashPdf()
        if (file != null) {
            sharePdfFile(file)
        }

    }

    private fun generateDetailedPettyCashPdf(): File? {
        try {
            val pdf = PdfDocument()
            val pageWidth = 400
            val pageHeight = 500

            // Create the first page
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            paint.color = Color.BLACK

            // Add gradient background
            val colors = intArrayOf(transactorColor, Color.WHITE)
            val gradient = LinearGradient(0f, -200f, 0f, page.info.pageHeight.toFloat(), colors, null, Shader.TileMode.CLAMP)

            paint.shader = gradient
            canvas.drawRect(0f, 0f, page.info.pageWidth.toFloat(), page.info.pageHeight.toFloat(), paint)
            paint.shader = null // Reset shader

            // Add company logo and title
            val logoBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_p_logo_foreground)
            val logoWidth = 100f
            val logoHeight = logoWidth * logoBitmap.height / logoBitmap.width
            canvas.drawBitmap(logoBitmap, null, RectF(10f, 10f, 20f + logoWidth, 20f + logoHeight), paint)

            paint.textSize = 11.5f
            paint.isFakeBoldText = true
            pettyCash?.owner?.name?.uppercase(Locale.ROOT)
                ?.let { canvas.drawText(it, 115f, 50f, paint) }
            paint.textSize = 11.5f
            canvas.drawText("PETTY CASH VOUCHER", 135f, 90f, paint)
            paint.isFakeBoldText = true

            var yPosition = 125f
            paint.textSize = 11f

            // Add Basic Information Section
            canvas.drawText("Basic Information", 20f, yPosition, paint)
            yPosition += 30f
            paint.isFakeBoldText = false
            paint.textSize = 10f

            val mpesaTransaction = pettyCash?.mpesaTransaction?.mpesa_code ?: "N/A"


            val basicInfo = mapOf(
                "Petty Cash Number" to pettyCash?.pettyCashNumber?.toString(),
                "Date" to pettyCash?.date?.toString(),
                "Amount" to pettyCash?.amount?.toString(),
                "User" to pettyCash?.user?.name?.toString(),
                "Account" to pettyCash?.account?.name?.toString(),
                "Payment Mode" to pettyCash?.paymentMode?.toString(),
                "Description" to pettyCash?.description?.toString(),
                "Mpesa Transaction" to mpesaTransaction,
            )
            drawTableWithBorders(basicInfo, canvas, paint, 20f, yPosition - 10f, pageWidth - 40f)
            yPosition += basicInfo.size * 30f

            paint.isFakeBoldText = true
            paint.textSize = 11f

            // Signature Section
            pettyCash?.signature?.let { signatureBase64 ->
                canvas.drawText("Signature", 20f, yPosition - 20f, paint)
                yPosition += 20f

                // Draw rounded rectangle for signature
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                val rectHeight = 40f * 2 // Double the height
                canvas.drawRoundRect(20f, yPosition - 30f, pageWidth - 20f, yPosition + rectHeight, 10f, 10f, paint)
                paint.style = Paint.Style.FILL

                // Decode and draw the signature image
                val decodedBytes = Base64.decode(signatureBase64, Base64.DEFAULT)
                val signatureBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                signatureBitmap?.let {
                    val reducedHeight = 50f // Adjust the height
                    val reducedWidthPadding = 80f // Increase padding to reduce width
                    canvas.drawBitmap(
                        it,
                        null,
                        RectF(
                            reducedWidthPadding, // Left
                            yPosition - 10f, // Top
                            pageWidth - reducedWidthPadding, // Right
                            yPosition + reducedHeight + 30f // Bottom
                        ),
                        paint
                    )
                }

                yPosition += 90f // Adjust yPosition to account for the doubled height and some spacing

            }

            paint.isFakeBoldText = false
            paint.textSize = 10f

            // Add owner's logo from Base64 to top right corner
            pettyCash?.owner?.logoPath?.let { logoBase64 ->
                val decodedLogoBytes = Base64.decode(logoBase64, Base64.DEFAULT)
                val logoBitmap = BitmapFactory.decodeByteArray(decodedLogoBytes, 0, decodedLogoBytes.size)
                logoBitmap?.let {
                    val logoWidth = 50f
                    val logoHeight = logoWidth * it.height / it.width
                    val logoX = pageWidth - logoWidth - 30f // 10f margin from the right edge
                    val logoY = 35f // 10f margin from the top edge
                    canvas.drawBitmap(it, null, RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight), paint)
                }
            }

            pdf.finishPage(page)

            // Create a new page for the supporting document
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = pdf.startPage(pageInfo2)
            val canvas2 = page2.canvas

            paint.shader = gradient
            canvas2.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)
            paint.shader = null // Reset shader

            val transactor = pettyCash?.transactor
            transactor?.let {
                // Add Transactor Title
                val title: String = "Transactor"
                paint.isFakeBoldText = true
                paint.textSize = 11f
                var yPosition2 = 40f

                canvas2.drawText(title, 20f, yPosition2, paint)
                yPosition2 += 20f

                // Transactor Details Table
                paint.isFakeBoldText = false
                paint.textSize = 10f

                // Define the transactor info table
                val info = mutableMapOf(
                    "Name" to pettyCash?.transactor?.name,
                    "Phone Number" to pettyCash?.transactor?.phoneNumber,
                    "ID Card" to pettyCash?.transactor?.idCard?.toString(),
                    "Address" to pettyCash?.transactor?.address,
                    "KRA PIN" to pettyCash?.transactor?.kraPin
                )

                // Add profile picture and avatar color to the table
                pettyCash?.transactor?.let { transactor ->
                    val profilePicBitmap = transactor.transactorProfilePicturePath?.let {
                        decodeBase64ToBitmap(it)
                    }
                    val avatarColor = transactor.avatarColor

                    // Add profile picture and avatar color as placeholders to info
                    profilePicBitmap?.let { info["Profile Picture"] = "" } // Placeholder for profile picture
                    avatarColor?.let { info["Avatar Color"] = "" } // Placeholder for avatar color
                }

                // Draw the table with transactor details
                drawTableWithBorders(info, canvas2, paint, 20f, yPosition2, pageWidth - 40f)
                yPosition2 += info.size * 30f

                // Now, display the profile picture and avatar color inside the table
                pettyCash?.transactor?.let { transactor ->
                    val profilePicBitmap = transactor.transactorProfilePicturePath?.let {
                        decodeBase64ToBitmap(it)
                    }
                    val avatarColor = transactor.avatarColor

                    // Define a larger cell height for Profile Picture and Avatar Color
                    val increasedCellHeight = 50f // Increase the height for these rows

                    profilePicBitmap?.let { bitmap ->
                        val imageSize = 15f // Size of the profile picture
                        val profilePicPosition = yPosition2 - 85f
                        val roundedBitmap = getRoundedBitmap(bitmap, imageSize / 2)

                        // Position for profile picture in the second column, shifted by 5f to the right
                        val rectF = RectF((pageWidth / 2 + 10f), profilePicPosition, pageWidth / 2 + imageSize + 10f, profilePicPosition + imageSize)
                        canvas2.drawBitmap(roundedBitmap, null, rectF, paint)

                        // Adjust yPosition2 to reflect the increased cell height for the profile picture
                        yPosition2 += increasedCellHeight
                    }

                    // Display the avatar color as a small square in the second column (after "Avatar Color" label)
                    avatarColor?.let { color ->
                        // Set the square size and position
                        val squareSize = 15f
                        val squarePositionX = pageWidth / 2 + 10f
                        var squarePositionY = yPosition2 - 113f

                        if (profilePicBitmap == null) {
                            // Adjust the square position if the profile picture is not available
                            squarePositionY += 60f
                        }

                        // Draw the avatar color square in the second column
                        paint.color = Color.parseColor(color) // Set the color from the avatarColor
                        canvas2.drawRect(squarePositionX, squarePositionY, squarePositionX + squareSize, squarePositionY + squareSize, paint)

                        // Adjust yPosition2 to reflect the increased cell height for the avatar color
                        yPosition2 += increasedCellHeight
                    }
                }

                // Finish the page if no supporting document exists
                if (pettyCash?.supportingDocument == null) {
                    pdf.finishPage(page2)
                }
            }





            // Second page for supporting document
            val supportingDocument = pettyCash?.supportingDocument
            supportingDocument?.let { document ->

                paint.color = Color.BLACK

                // Add title with Petty Cash Number and Supporting Document
                paint.isFakeBoldText = true
                paint.textSize = 11f
                val title = "Supporting Document"
                canvas2.drawText(title, 20f, yPosition-260f, paint)

                // Supporting Document Details Table
                var yPosition2 = yPosition-240f
                paint.isFakeBoldText = false
                paint.textSize = 10f

                val docInfo = mapOf(
                    "Document ID" to document.id.toString(),
                    "Document Type" to document.type,
                    "Document No" to document.documentNo,
                    "Supplier Name" to document.supplierName,
                    "CU Number" to document.cuNumber,
                    "Taxable Total Amount" to document.taxableTotalAmount?.toString(),
                    "Tax Amount" to document.taxAmount?.toString(),
                    "Total Amount" to document.totalAmount?.toString(),
                    "Document Date" to document.documentDate
                )
                drawTableWithBorders(docInfo, canvas2, paint, 20f, yPosition2, pageWidth - 40f)
                yPosition2 += docInfo.size * 30f

                // Finish the second page before starting a new one for images
                pdf.finishPage(page2)

                paint.color = Color.BLACK

                // Add images (if available)
                val imagePaths =
                    listOf(document.imagePath1, document.imagePath2, document.imagePath3)
                imagePaths.forEachIndexed { index, imagePath ->
                    imagePath?.let {
                        // Load the image from file path
                        val imageFile = File(it)
                        if (imageFile.exists()) {
                            // Decode the image file to Bitmap
                            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                            bitmap?.let { img ->
                                // Create a new page for the image if it's available
                                val imagePageInfo =
                                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3 + index)
                                        .create()
                                val imagePage = pdf.startPage(imagePageInfo)
                                val imageCanvas = imagePage.canvas

                                // Resizing the image to fit the page while keeping aspect ratio
                                val margin = 40f  // Equal margin from all edges
                                val imageWidth = pageWidth - 2 * margin
                                val imageHeight =
                                    (img.height.toFloat() / img.width.toFloat()) * imageWidth

                                // Positioning the image below the title with a small margin
                                val imageTopPosition = 60f

                                // Define the rectangle for the image with rounded corners
                                val rectF = RectF(
                                    margin,
                                    imageTopPosition,
                                    margin + imageWidth,
                                    imageTopPosition + imageHeight
                                )

                                // Create a rounded bitmap with the radius for all corners
                                val roundedBitmap = getRoundedBitmap(
                                    img,
                                    20f
                                )  // Apply rounded corners with radius 20f

                                // Draw the rounded image with the defined position
                                imageCanvas.drawBitmap(roundedBitmap, null, rectF, paint)

                                // Draw title above the image
                                paint.isFakeBoldText = true
                                paint.textSize = 14f
                                val imageTitle =
                                    "Supporting Document ${index + 1} - ${pettyCash?.pettyCashNumber}"
                                imageCanvas.drawText(
                                    imageTitle,
                                    margin,
                                    imageTopPosition - 10f,
                                    paint
                                )

                                pdf.finishPage(imagePage) // Finish image page
                            }
                        }
                    }
                }
            }

                // Finalize PDF
            val fileName = "PettyCash_Detailed_${pettyCash?.pettyCashNumber?.replace("/", "_").orEmpty()}_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            pdf.writeTo(FileOutputStream(file))
            pdf.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getComplementaryBlackOrWhite(color: Int): Int {
        // Calculate the brightness of the color (using the luminance formula)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        // Using the luminance formula to calculate brightness
        val brightness = (0.2126 * red + 0.7152 * green + 0.0722 * blue)

        // Return white if brightness is less than 128, otherwise return black
        return if (brightness < 128) Color.WHITE else Color.BLACK
    }

    // Helper function to draw a table with borders and rounded corners
    private fun drawTableWithBorders(
        data: Map<String, String?>,
        canvas: Canvas,
        paint: Paint,
        x: Float,
        startY: Float,
        tableWidth: Float
    ) {
        val defaultCellHeight = 23f
        val columnSpacing = tableWidth / 2f
        val cornerRadius = 10f
        val padding = 10f
        val bottomPadding = 10f // Added padding for the bottom of the table

        var currentY = startY
        val textPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.LEFT
        }

        // Loop through the data entries to determine cell heights dynamically
        data.entries.forEach { (key, value) ->
            val keyLines = calculateTextLines(key, paint, columnSpacing - padding * 2)
            val valueLines = calculateTextLines(value.orEmpty(), paint, columnSpacing - padding * 2)

            val rowHeight = maxOf(
                defaultCellHeight,
                (paint.textSize + 6f) * maxOf(keyLines, valueLines)
            )

            // Draw the key text in the first column
            drawMultilineText(
                canvas,
                key,
                x + padding,
                currentY + padding + paint.textSize,
                textPaint,
                columnSpacing - padding * 2
            )

            // Draw the value text in the second column
            drawMultilineText(
                canvas,
                value.orEmpty(),
                x + columnSpacing + padding,
                currentY + padding + paint.textSize,
                textPaint,
                columnSpacing - padding * 2
            )

            currentY += rowHeight
        }

        // Add bottom padding to the table
        currentY += bottomPadding

        // Draw the outer border of the table (final enclosing rectangle)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f // Increased thickness of the border
        canvas.drawRoundRect(x, startY, x + tableWidth, currentY, cornerRadius, cornerRadius, paint)

        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
    }


    private fun calculateTextLines(text: String, paint: Paint, maxWidth: Float): Int {
        val words = text.split(" ")
        var currentLineWidth = 0f
        var lineCount = 1

        for (word in words) {
            val wordWidth = paint.measureText("$word ")
            if (currentLineWidth + wordWidth > maxWidth) {
                lineCount++
                currentLineWidth = wordWidth
            } else {
                currentLineWidth += wordWidth
            }
        }

        return lineCount
    }


    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float
    ) {
        val words = text.split(" ")
        var currentLineWidth = 0f
        var lineY = y

        for (word in words) {
            val wordWidth = paint.measureText("$word ")
            if (currentLineWidth + wordWidth > maxWidth) {
                lineY += paint.textSize + 6f // Move to the next line
                currentLineWidth = 0f
            }
            canvas.drawText(word, x + currentLineWidth, lineY, paint)
            currentLineWidth += wordWidth
        }
    }












    private fun setUpSignatureCard() {
        if (pettyCash?.signature != null) {
            signatureCard?.visibility = View.VISIBLE
            val decodedString = Base64.decode(pettyCash?.signature, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            signatureBitmap = decodedByte
            signatureImageView?.setImageBitmap(decodedByte)

            // Set click listener to view the image in the gallery
            signatureImageView?.setOnClickListener {
                viewImageInGallery(decodedByte)
            }
        }
    }

    private fun supportingDocumentCardSetup() {
        if (pettyCash?.supportingDocument != null) {
            supportingDocumentCard?.visibility = View.VISIBLE
            pettyCashSupportingDocumentIdValue?.text = pettyCash?.supportingDocument?.id.toString()
            pettyCashSupportingDocumentSupplierNameValue?.text = pettyCash?.supportingDocument?.supplierName
            pettyCashSupportingDocumentTypeValue?.text = pettyCash?.supportingDocument?.type
            pettyCashSupportingDocumentCuNumberValue?.text = pettyCash?.supportingDocument?.cuNumber
            pettyCashSupportingDocumentNumberValue?.text = pettyCash?.supportingDocument?.documentNo
            pettyCashSupportingDocumentTaxableAmountValue?.text = pettyCash?.supportingDocument?.taxableTotalAmount.toString()
            pettyCashSupportingDocumentTaxAmountValue?.text = pettyCash?.supportingDocument?.taxAmount.toString()
            pettyCashSupportingDocumentTotalAmountValue?.text = pettyCash?.supportingDocument?.totalAmount.toString()
            pettyCashSupportingDocumentDateValue?.text = pettyCash?.supportingDocument?.documentDate

            image1.let {
                val imagePath1 = pettyCash?.supportingDocument?.imagePath1
                if (!imagePath1.isNullOrEmpty()) {
                    val imageFile = File(imagePath1)
                    if (imageFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        it?.setImageBitmap(bitmap)

                        // Set click listener to view the image in the gallery
                        it?.setOnClickListener {
                            viewImageInGallery(bitmap)
                        }
                    } else {
                        it?.visibility = View.GONE
                    }
                } else {
                    it?.visibility = View.GONE
                }
            }

            image2.let {
                val imagePath2 = pettyCash?.supportingDocument?.imagePath2
                if (!imagePath2.isNullOrEmpty()) {
                    val imageFile = File(imagePath2)
                    if (imageFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        it?.setImageBitmap(bitmap)

                        // Set click listener to view the image in the gallery
                        it?.setOnClickListener {
                            viewImageInGallery(bitmap)
                        }
                    } else {
                        it?.visibility = View.GONE
                    }
                } else {
                    it?.visibility = View.GONE
                }
            }

            image3.let {
                val imagePath3 = pettyCash?.supportingDocument?.imagePath3
                if (!imagePath3.isNullOrEmpty()) {
                    val imageFile = File(imagePath3)
                    if (imageFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        it?.setImageBitmap(bitmap)

                        // Set click listener to view the image in the gallery
                        it?.setOnClickListener {
                            viewImageInGallery(bitmap)
                        }
                    } else {
                        it?.visibility = View.GONE
                    }
                } else {
                    it?.visibility = View.GONE
                }
            }


        }

    }

    private fun viewImageInGallery(bitmap: Bitmap) {
        try {
            // Save the image as a temporary file
            val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_image.jpg")
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Get the URI for the saved file
            val uri = FileProvider.getUriForFile(
                this, // Use `requireContext()` if inside a fragment
                "${BuildConfig.APPLICATION_ID}.provider",
                imageFile
            )

            // Create an Intent to view the image
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ViewImage", "Error opening image in gallery", e)
        }
    }

    private fun companyLogoSetup() {
        // Set the company logo card image
        if (pettyCash?.owner?.logoPath != null){
            val decodedString = Base64.decode(pettyCash?.owner?.logoPath, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            companyLogoImageBitmap = decodedByte
            logoImage?.setImageBitmap(decodedByte)
        }
    }

    private fun setUpSupportingDocumentCard() {

    }

    private fun basicInformationCardSetup() {
        pettyCashDateValue?.text = pettyCash?.date
        pettyCashAccountValue?.text = pettyCash?.account?.name
        pettyCashPaymentModeValue?.text = pettyCash?.paymentMode
        pettyCashDescriptionValue?.text = pettyCash?.description
        pettyCashMpesaTransactionValue?.text = pettyCash?.mpesaTransaction?.mpesa_code ?: "N/A"
        
        // Check if all trucks for the owner are selected
        val ownerTruckCount = db_helper?.getTruckCountByOwner(pettyCash?.owner?.ownerCode!!) ?: 0
        val selectedTruckCount = pettyCash?.trucks?.size ?: 0

        println("OwnerTruckCount: $ownerTruckCount, SelectedTruckCount: $selectedTruckCount")
        
        pettyCashTrucksValue?.text = if (selectedTruckCount == ownerTruckCount) {
            "All Trucks"
        } else {
            pettyCash?.trucks?.joinToString(", ") { it.truckNo.toString() }
        }
        
        pettyCashUserValue?.text = pettyCash?.user?.name
    }

    private fun transactorNameSetup() {
        transactorNameTextView?.text = pettyCash?.transactor?.name?.let { capitalizeEachWord(it) }
    }

    private fun setupPettyCashNumber() {
        pettyCashIdChip?.text = pettyCash?.pettyCashNumber
    }

    private fun setUpAmountCard(){
        // Create a gradient drawable for the background
        val gradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            colors = intArrayOf(
                transactorColor,
                cardSurfaceLow

            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM // Left to right gradient
        }

        amountCard?.background = gradientDrawable

        pettyCash?.amount?.let { setupAmountTextView(it) }

        val colorStateList = ColorStateList.valueOf(transactorColor)
        pettyCashIdChip?.chipBackgroundColor= colorStateList
    }


    private fun setUpAvatarView(){
        avatarView?.apply {
            text = pettyCash?.transactor?.name?.let { capitalizeEachWord(it) }
            highlightBorderColorEnd = transactorColor
            isAnimating = false
            highlightBorderColor = colorPrimary
            highlightedBorderThickness = 10
            isHighlighted = true
            borderThickness = 10

            setImageDrawable(null)
        }

        if (pettyCash?.transactor?.transactorProfilePicturePath != null) {
            Log.d("PettyCashViewerActivity", "Avatar URL: Found")
            setImageViewFromBase64(avatarView!!, pettyCash?.transactor?.transactorProfilePicturePath!!)
        }


        avatarView?.rotationY = 0f
    }

    private fun setupAmountTextView(
        amount: Double
    ) {
        val isPositiveAmount = false
        val formattedAmount =
            formatAmountWithColor(amount, isPositiveAmount, this)
        transactionAmount?.text = formattedAmount

    }

    private fun formatAmountWithColor(amount: Double, isPositive: Boolean, context: Context): CharSequence {
        val formattedAmount = amount
        var spannableString = SpannableString("$formattedAmount/-")


        // Assuming amountCard is the MaterialCardView and textView is the TextView
        val backgroundColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (amountCard?.background as? GradientDrawable)?.colors?.get(0) ?: this.transactorColor
        } else {
            colorPrimary
        }

        // Calculate the contrast ratio between the background color and white
        val contrastWithWhite = ColorUtils.calculateContrast(Color.WHITE, backgroundColor)

        // Determine the appropriate text color based on the contrast ratio
        val textColor = if (contrastWithWhite > 4.5) Color.WHITE else colorPrimary


        spannableString = if (isPositive) {
            SpannableString("+${addCommasToDoubleValue(formattedAmount)}/-")
        } else {
            SpannableString("-${addCommasToDoubleValue(formattedAmount)}/-")
        }

        spannableString.setSpan(ForegroundColorSpan(textColor), 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }

    fun addCommasToDoubleValue(value: Double): String {
        // Convert the double value to a string
        val stringValue = value.toString()

        // Split the string into integer and fractional parts
        val parts = stringValue.split(".")

        // Format the integer part with commas for thousands separators
        val integerPartWithCommas = parts[0].reversed().chunked(3).joinToString(",").reversed()

        // Combine the integer part with the fractional part (if exists)
        return if (parts.size > 1) {
            "$integerPartWithCommas.${parts[1]}"
        } else {
            integerPartWithCommas
        }
    }

    private fun setImageViewFromBase64(imageView: ImageView, base64String: String) {
        // Decode the Base64 string into a byte array
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)

        // Convert the byte array into a Bitmap
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        // Set the Bitmap to the ImageView
        imageView.setImageBitmap(decodedByte)
    }

    fun setStatusBarGradient(window: Window, startColor: Int, endColor: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // Set the status bar color
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = startColor // Set the status bar color to the start color


            // Adjust the status bar icons color based on the background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun capitalizeEachWord(input: String): String? {
        val words = input.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result = StringBuilder()

        for (word in words) {
            if (word.isNotEmpty()) {
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

    private fun updateUI(pettyCash: PettyCash) {
        try {
            // Update amount
            pettyCash.amount?.let { setupAmountTextView(it) }
            
            // Update petty cash ID chip
            pettyCashIdChip?.text = "${pettyCash.pettyCashNumber}"

            // Update basic information
            pettyCashDateValue?.text = pettyCash.date
            pettyCashAccountValue?.text = pettyCash.account?.name ?: "-"
            pettyCashPaymentModeValue?.text = pettyCash.paymentMode
            pettyCashDescriptionValue?.text = pettyCash.description
            pettyCashTrucksValue?.text = pettyCash.trucks?.joinToString(", ") { it.truckNo ?: "" }
            pettyCashUserValue?.text = pettyCash.user?.name ?: "-"

            // Update transactor information
            pettyCash.transactor?.let { transactor ->
                transactorNameTextView?.text = transactor.name?.let { capitalizeEachWord(it) }
                setUpAvatarView()
                avatarView?.text = transactor.name?.firstOrNull()?.toString() ?: ""
            }

            // Update M-Pesa transaction if exists
            pettyCash.mpesaTransaction?.let { mpesa ->
                pettyCashMpesaTransactionLayout?.visibility = View.VISIBLE
                pettyCashMpesaTransactionValue?.text = pettyCash.mpesaTransaction?.mpesa_code ?: "N/A".trimIndent()
            } ?: run {
                pettyCashMpesaTransactionLayout?.visibility = View.GONE
            }

            // Update supporting document if exists
            pettyCash.supportingDocument?.let { doc ->
                // Show supporting document section
                supportingDocumentCard?.visibility = View.VISIBLE
                
                // Update supporting document fields
                pettyCashSupportingDocumentIdValue?.text = doc.id?.toString() ?: ""
                pettyCashSupportingDocumentSupplierNameValue?.text = doc.supplierName ?: ""
                pettyCashSupportingDocumentTypeValue?.text = doc.type ?: ""
                pettyCashSupportingDocumentCuNumberValue?.text = doc.cuNumber ?: ""
                pettyCashSupportingDocumentNumberValue?.text = doc.documentNo ?: ""
                pettyCashSupportingDocumentTaxableAmountValue?.text = doc.taxableTotalAmount?.toString() ?: ""
                pettyCashSupportingDocumentTaxAmountValue?.text = doc.taxAmount?.toString() ?: ""
                pettyCashSupportingDocumentTotalAmountValue?.text = doc.totalAmount?.toString() ?: ""
                pettyCashSupportingDocumentDateValue?.text = doc.documentDate ?: ""

                // Handle images
                image1?.setImageDrawable(null)
                image2?.setImageDrawable(null)
                image3?.setImageDrawable(null)

                //Clear image onClickListeners
                image1?.setOnClickListener(null)
                image2?.setOnClickListener(null)
                image3?.setOnClickListener(null)

                // Load and display images
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Load image 1
                        doc.imagePath1?.let { path ->
                            val bitmap1 = getImageFromPath(path)
                            withContext(Dispatchers.Main) {
                                bitmap1?.let {
                                    image1?.setImageBitmap(it)
                                    image1?.visibility = View.VISIBLE
                                    image1?.setOnClickListener {
                                        viewImageInGallery(bitmap1)
                                    }
                                    
                                } ?: run {
                                    image1?.visibility = View.GONE
                                }
                            }
                        }

                        // Load image 2
                        doc.imagePath2?.let { path ->
                            val bitmap2 = getImageFromPath(path)
                            withContext(Dispatchers.Main) {
                                bitmap2?.let {
                                    image2?.setImageBitmap(it)
                                    image2?.visibility = View.VISIBLE
                                    image2?.setOnClickListener {
                                        viewImageInGallery(bitmap2)
                                    }
                                } ?: run {
                                    image2?.visibility = View.GONE
                                }
                            }
                        }

                        // Load image 3
                        doc.imagePath3?.let { path ->
                            val bitmap3 = getImageFromPath(path)
                            withContext(Dispatchers.Main) {
                                bitmap3?.let {
                                    image3?.setImageBitmap(it)
                                    image3?.visibility = View.VISIBLE
                                    image3?.setOnClickListener {
                                        viewImageInGallery(bitmap3)
                                    }
                                } ?: run {
                                    image3?.visibility = View.GONE
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PettyCashViewer", "Error loading supporting document images: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PettyCashViewerActivity,
                                "Error loading images: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } ?: run {
                // Hide supporting document section if no document
                supportingDocumentCard?.visibility = View.GONE
            }

            // Update signature if exists
            pettyCash.signature?.let { signature ->
                signatureCard?.visibility = View.VISIBLE
                signatureImageView?.let { imageView ->
                    base64ToBitmap(signature)?.let { bitmap ->
                        imageView.setImageBitmap(bitmap)
                        signatureBitmap = bitmap
                    }
                }
            } ?: run {
                signatureCard?.visibility = View.GONE
            }

            // Update owner logo if exists
            pettyCash.owner?.let { owner ->
                logoCard?.visibility = View.VISIBLE
                owner.logoPath?.let { logo ->
                    base64ToBitmap(logo)?.let { bitmap ->
                        logoImage?.setImageBitmap(bitmap)
                        companyLogoImageBitmap = bitmap
                    }
                }
            } ?: run {
                logoCard?.visibility = View.GONE
            }

            // Store the updated petty cash
            this.pettyCash = pettyCash

            // Handle trucks display
            pettyCash.trucks?.let { trucks ->
                if (trucks.isNotEmpty()) {
                    // Get the owner's total truck count
                    val ownerTruckCount = db_helper?.getTruckCountByOwner(pettyCash.owner?.ownerCode!!) ?: 0

                    // Set trucks text
                    pettyCashTrucksValue?.text = if (trucks.size == ownerTruckCount && ownerTruckCount > 0) {
                        "All Trucks"
                    } else {
                        trucks.joinToString(", ") { it.truckNo ?: "" }
                    }
                } else {
                    pettyCashTrucksValue?.text = ""
                }
            } ?: run {
                pettyCashTrucksValue?.text = ""
            }
        } catch (e: Exception) {
            Log.e("PettyCashViewer", "Error updating UI: ${e.message}")
            Toast.makeText(
                this,
                "Error updating display: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun base64ToImageView(base64String: String, imageView: ImageView) {
        try {
            val bitmap = base64ToBitmap(base64String)
            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("PettyCashViewer", "Error loading image: ${e.message}")
            imageView.visibility = View.GONE
        }
    }

    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Decode the Base64 string to byte array
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            
            // Convert byte array to Bitmap
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.let { originalBitmap ->
                // Create a copy that's mutable
                originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            Log.e("PettyCashViewer", "Error converting base64 to bitmap: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun getImageFromPath(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Log.e("DbHelper", "Error loading image from path $path: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Reset all view references to null
        amountCard = null
        logoCard = null
        scrollView = null
        pettyCashIdChip = null
        transactorCard = null
        avatarView = null
        basicInformationCard = null
        supportingDocumentCard = null
        signatureCard = null
        pettyCashDateLayout = null
        pettyCashAccountLayout = null
        pettyCashPaymentModeLayout = null
        pettyCashDescriptionLayout = null
        pettyCashMpesaTransactionLayout = null
        pettyCashTrucksLayout = null
        pettyCashUserLayout = null
        pettyCashSupportingDocumentIdLayout = null
        pettyCashSupportingDocumentSupplierNameLayout = null
        pettyCashSupportingDocumentTypeLayout = null
        pettyCashSupportingDocumentCuNumberLayout = null
        pettyCashSupportingDocumentNumberLayout = null
        pettyCashSupportingDocumentTaxableAmountLayout = null
        pettyCashSupportingDocumentTaxAmountLayout = null
        pettyCashSupportingDocumentTotalAmountLayout = null
        pettyCashSupportingDocumentDateLayout = null
        pettyCashSupportingDocumentImagesLabelLayout = null
        pettyCashSupportingDocumentImagesLayout = null
        actionButtonsLayout = null
        transactionAmount = null
        transactorDetailsLabel = null
        basicInformationLabel = null
        pettyCashDateLabel = null
        pettyCashDateValue = null
        pettyCashAccountLabel = null
        pettyCashAccountValue = null
        pettyCashPaymentModeLabel = null
        pettyCashPaymentModeValue = null
        pettyCashDescriptionLabel = null
        pettyCashDescriptionValue = null
        pettyCashMpesaTransactionLabel = null
        pettyCashMpesaTransactionValue = null
        pettyCashTrucksLabel = null
        pettyCashTrucksValue = null
        pettyCashUserLabel = null
        pettyCashUserValue = null
        pettyCashSupportingDocumentIdLabel = null
        pettyCashSupportingDocumentIdValue = null
        pettyCashSupportingDocumentSupplierNameLabel = null
        pettyCashSupportingDocumentSupplierNameValue = null
        pettyCashSupportingDocumentTypeLabel = null
        pettyCashSupportingDocumentTypeValue = null
        pettyCashSupportingDocumentCuNumberLabel = null
        pettyCashSupportingDocumentCuNumberValue = null
        pettyCashSupportingDocumentNumberLabel = null
        pettyCashSupportingDocumentNumberValue = null
        pettyCashSupportingDocumentTaxableAmountLabel = null
        pettyCashSupportingDocumentTaxableAmountValue = null
        pettyCashSupportingDocumentTaxAmountLabel = null
        pettyCashSupportingDocumentTaxAmountValue = null
        pettyCashSupportingDocumentTotalAmountLabel = null
        pettyCashSupportingDocumentTotalAmountValue = null
        pettyCashSupportingDocumentDateLabel = null
        pettyCashSupportingDocumentDateValue = null
        pettyCashSupportingDocumentImagesLabel = null
        signatureLabel = null
        image1 = null
        image2 = null
        image3 = null
        editActionButton = null
        deleteActionButton = null
        shareActionButton = null
        binding = null

        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(pettyCashUpdateReceiver)
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Petty Cash")
            .setMessage("Are you sure you want to delete this petty cash? This action cannot be undone.")
            .setIcon(R.drawable.baseline_warning_amber_white_24dp)
            .setPositiveButton("Delete") { dialog, _ ->
                deletePettyCash()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deletePettyCash() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                pettyCash?.id?.let { id ->
                    // Delete from database
                    db_helper?.deletePettyCash(id)

                    // Handle transaction cost deletion if this is an M-Pesa petty cash
                    var transactionCostId: Int? = null
                    pettyCash?.mpesaTransaction?.mpesa_code?.let { mpesaCode ->
                        val transactionCostPettyCash = db_helper?.getTransactionCostPettyCashByMpesaTransaction(mpesaCode)
                        transactionCostId = transactionCostPettyCash?.id
                    }

                    withContext(Dispatchers.Main) {
                        // Send broadcast to update lists with both IDs
                        val intent = Intent("pettycash_deleted_action")
                        intent.putExtra("deleted_petty_cash_id", id)
                        transactionCostId?.let {
                            intent.putExtra("deleted_transaction_cost_id", it)
                        }
                        LocalBroadcastManager.getInstance(this@PettyCashViewerActivity)
                            .sendBroadcast(intent)

                        // Set result and finish activity
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra("deleted_petty_cash_id", id)
                            transactionCostId?.let {
                                putExtra("deleted_transaction_cost_id", it)
                            }
                        })

                        Toast.makeText(
                            this@PettyCashViewerActivity,
                            "Petty cash deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("PettyCashViewer", "Error deleting petty cash: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PettyCashViewerActivity,
                        "Error deleting petty cash: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}