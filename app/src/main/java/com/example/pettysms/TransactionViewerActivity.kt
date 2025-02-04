package com.example.pettysms

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import com.example.pettysms.databinding.ActivityTransactionViewerBinding
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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.properties.Delegates

class TransactionViewerActivity : AppCompatActivity(), EditTransactionFragment.OnDescriptionChangeListener {
    private lateinit var binding: ActivityTransactionViewerBinding
    private lateinit var amountCard: MaterialCardView
    private lateinit var avatarView: AvatarView
    private lateinit var chipCode: Chip
    private lateinit var chipType: Chip
    private lateinit var paybillAccountLayout: LinearLayout
    private lateinit var smsTextLayout: LinearLayout
    private lateinit var transactionAmountView: TextView
    private lateinit var transactionIdLabel: TextView
    private lateinit var transactionIdValue: TextView
    private lateinit var transactionDateLabel: TextView
    private lateinit var transactionDateValue: TextView
    private lateinit var transactorLabel: TextView
    private lateinit var transactorValue: TextView
    private lateinit var descriptionLabel: TextView
    private lateinit var descriptionValue: TextView
    private lateinit var smsTextLabel: TextView
    private lateinit var smsTextValue: TextView
    private lateinit var transactionCostLabel: TextView
    private lateinit var transactionCostValue: TextView
    private lateinit var transactionBalanceLabel: TextView
    private lateinit var transactionBalanceValue: TextView
    private lateinit var paybillAccountLabel: TextView
    private lateinit var paybillAccountValue: TextView
    private lateinit var shareActionButton: FloatingActionButton
    private lateinit var deleteActionButton: FloatingActionButton
    private lateinit var editActionButton: FloatingActionButton

    private var transactionColor by Delegates.notNull<Int>()
    private var cardSurfaceLow by Delegates.notNull<Int>()
    private var colorPrimary by Delegates.notNull<Int>()
    private var db_helper: DbHelper? = null
    private var db: SQLiteDatabase? = null




    private val STORAGE_PERMISSION_CODE = 1

    val transactionDateLabelString = "Transaction Date"
    val transactionIdLabelString = "Transaction ID"
    val transactorLabelString = "Transactor"
    val descriptionLabelString = "Description"
    val smsTextLabelString = "SMS Text"
    val transactionCostLabelString = "Transaction Cost"
    val transactionBalanceLabelString = "Balance After Transaction"
    val paybillAccountLabelString = "Paybill Account"
    val smsTextValueString = "View full text"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        amountCard = binding.amountCard
        avatarView = binding.avatarView
        transactionAmountView = binding.transactionAmount
        chipCode = binding.codeChip
        chipType = binding.typeChip
        transactionIdLabel = binding.transactionIdLabel
        transactionIdValue = binding.transactionIdValue
        transactionDateLabel = binding.transactionDateLabel
        transactionDateValue = binding.transactionDateValue
        transactorLabel = binding.transactorLabel
        transactorValue = binding.transactorValue
        descriptionLabel = binding.descriptionLabel
        descriptionValue = binding.descriptionValue
        smsTextLabel = binding.smsTextLabel
        smsTextValue = binding.smsTextValue
        transactionCostLabel = binding.transactionCostLabel
        transactionCostValue = binding.transactionCostValue
        transactionBalanceLabel = binding.transactionBalanceLabel
        transactionBalanceValue = binding.transactionBalanceValue
        paybillAccountLabel = binding.paybillAccountLabel
        paybillAccountValue = binding.paybillAccountValue
        paybillAccountLayout = binding.paybillAccountLayout
        smsTextLayout = binding.smsTextLayout
        shareActionButton = binding.shareActionButton
        deleteActionButton = binding.deleteActionButton
        editActionButton = binding.editActionButton

        cardSurfaceLow = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0)
        colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0)


        Log.d(this.toString(), "Id ya hii : " + amountCard.id.toString())


        // Retrieve JSON string from intent extras
        val mpesaTransactionJson = intent.getStringExtra("mpesaTransactionJson")

        // Convert JSON string back to MpesaTransaction object
        val gson = Gson()
        val mpesaTransaction = gson.fromJson(mpesaTransactionJson, MpesaTransaction::class.java)

        val transactionColor = getColorAvatar(this@TransactionViewerActivity, mpesaTransaction.transaction_type.toString())

        this.transactionColor = transactionColor

        // Create a gradient drawable for the background
        val gradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            colors = intArrayOf(
                transactionColor,
                cardSurfaceLow

            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM // Left to right gradient
        }

        val cornerRadius = this.resources.getDimension(R.dimen.corner_radius)

        val gradientDrawableWithRadius = GradientWithCornerRadiusDrawable(
            this,
            transactionColor,
            cardSurfaceLow,
            cornerRadius
        )

        avatarView.apply {
            text = getTitleTextByTransactionType(mpesaTransaction)
            highlightBorderColorEnd = transactionColor
            isAnimating = false
            highlightBorderColor = colorPrimary
            highlightedBorderThickness = 10
            isHighlighted = true
            borderThickness = 10
            setImageDrawable(null)
        }

        avatarView.rotationY = 0f


        amountCard.background = gradientDrawable

        db_helper = DbHelper(this)
        db = db_helper?.writableDatabase

        setupAmountTextView(mpesaTransaction)
        setupChips(mpesaTransaction)
        transactionDetailsCardFilling(mpesaTransaction)
        setStatusBarGradient(window, transactionColor, cardSurfaceLow)
        setupActionButtons(mpesaTransaction)




        Toast.makeText(this, "Transaction Id in Transaction Viewer: " + mpesaTransaction?.id + " Its Text: " + mpesaTransaction?.smsText, Toast.LENGTH_LONG).show()

    }

    private fun setupActionButtons(mpesaTransaction: MpesaTransaction) {
        shareActionButton.setOnClickListener{
            generatePdf(mpesaTransaction)
        }

        deleteActionButton.setOnClickListener{
            deleteTransaction(mpesaTransaction)
        }

        editActionButton.setOnClickListener{
            editTransaction(mpesaTransaction)
        }



    }

    private fun editTransaction(mpesaTransaction: MpesaTransaction) {
        val gson = Gson()
        val mpesaTransactionJson = gson.toJson(mpesaTransaction)

        val dialogFragment = EditTransactionFragment()
        val args = Bundle()
        args.putString("mpesaTransactionJson", mpesaTransactionJson)
        dialogFragment.arguments = args
        dialogFragment.show(supportFragmentManager, "edit_transaction_fragment")

    }

    private fun deleteTransaction(mpesaTransaction: MpesaTransaction) {

        MaterialAlertDialogBuilder(this)
            .setTitle("Warning")
            .setMessage("Are you sure you want to delete this transaction?")
            .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
            .setNegativeButton("Dismiss") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Confirm") { dialog, which ->
                mpesaTransaction.id?.let { db_helper?.deleteTransaction(it)
                    val resultIntent = Intent()
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

            }
            .show()

    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun generatePdf(mpesaTransaction: MpesaTransaction) {
        try {
            Log.d("PDFGeneration", "Start generating PDF")

            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(400, 500, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Add gradient background
            val colors = intArrayOf(transactionColor, Color.WHITE) // Example gradient colors
            val gradient = LinearGradient(0f, 0f, 0f, page.info.pageHeight.toFloat(), colors, null, Shader.TileMode.CLAMP)
            paint.shader = gradient
            canvas.drawRect(0f, 0f, page.info.pageWidth.toFloat(), page.info.pageHeight.toFloat(), paint)
            paint.shader = null // Reset shader

            // Add first logo
            val firstLogoBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_p_logo_foreground)
            val logoWidth = 100f
            val logoHeight = logoWidth * firstLogoBitmap.height / firstLogoBitmap.width
            val firstLogoLeft = 50f
            val firstLogoTop = 50f
            canvas.drawBitmap(firstLogoBitmap, null, RectF(firstLogoLeft, firstLogoTop, firstLogoLeft + logoWidth, firstLogoTop + logoHeight), paint)

            // Add second logo to the right of the first logo
            val secondLogoBitmap = BitmapFactory.decodeResource(resources, R.drawable.logo_mpesa)
            val secondLogoWidth = 100f
            val secondLogoHeight = secondLogoWidth * secondLogoBitmap.height / secondLogoBitmap.width
            val secondLogoLeft = firstLogoLeft + logoWidth + 20f // Adjust the spacing between the logos as needed
            val secondLogoTop = firstLogoTop + 35f // Align the top of the second logo with the top of the first logo
            canvas.drawBitmap(secondLogoBitmap, null, RectF(secondLogoLeft, secondLogoTop, secondLogoLeft + secondLogoWidth, secondLogoTop + secondLogoHeight), paint)

            // Draw title
            val title = "M-Pesa Transaction Details"
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText(title, 50f, 50f + logoHeight + 20f, paint)

            // Add a new line
            val lineHeight = paint.textSize // Height of the text
            val lineSpacing = 20f // Adjust the spacing between lines as needed
            val newYPosition = 50f + logoHeight + 20f + lineHeight + lineSpacing

            // Reset paint properties for transaction details
            paint.textSize = 12f // Default font size
            paint.isFakeBoldText = false // Default bold style

            // Add M-Pesa transaction details
            val transactionDetails = buildTransactionInfo(mpesaTransaction)
            val lines = transactionDetails.split("\n")

            val startY = newYPosition // Initial Y coordinate for drawing transaction details

            // Draw each line of the transaction details
            var y = startY
            for (line in lines) {
                canvas.drawText(line, 50f, y, paint)
                y += paint.textSize + 10f // Move to the next line, adding some spacing
            }

            pdf.finishPage(page)

            // Define the file name and path for the PDF
            val fileName = "MpesaTransaction_${mpesaTransaction.id}_${System.currentTimeMillis()}.pdf"
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
            sharePdf(file)

            Log.d("PDFGeneration", "PDF generation and sharing completed")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PDFGeneration", "Error generating PDF: ${e.message}")
        }
    }







    private fun sharePdf(file: File) {
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



    private fun getLogoBitmap(): Bitmap {
        // Replace this with your actual logo bitmap
        val logo = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(logo)
        canvas.drawColor(Color.BLUE)
        return logo

    }



    private fun buildTransactionInfo(mpesaTransaction: MpesaTransaction) : String{
        var transactionInfo =  transactionIdLabelString + " : " + transactionIdValue.text + "\n" +
                "Amount" + " : " + mpesaTransaction.amount?.let { addCommasToDoubleValue(it).toString() } + "/-" + "\n" +
                "Transaction Type" + " : " + mpesaTransaction.transaction_type?.let { capitalizeEachWord(it) } + "\n" +
                transactionDateLabelString + " : " + transactionDateValue.text + "\n" +
                transactorLabelString + " : " + transactorValue.text + "\n" +
                descriptionLabelString + " : " + descriptionValue.text + "\n" +
                transactionCostLabelString + " : " + transactionCostValue.text + "\n" +
                transactionBalanceLabelString + " : " + transactionBalanceValue.text

        if(mpesaTransaction.transaction_type == "paybill"){
            transactionInfo += "\n" + paybillAccountLabelString + " : " + paybillAccountValue.text
        }

        return transactionInfo

    }

    private fun transactionDetailsCardFilling(mpesaTransaction: MpesaTransaction) {
        transactionIdLabel.text = transactionIdLabelString
        transactionDateLabel.text = transactionDateLabelString
        transactorLabel.text = transactorLabelString
        descriptionLabel.text = descriptionLabelString
        smsTextLabel.text = smsTextLabelString
        transactionCostLabel.text = transactionCostLabelString
        transactionBalanceLabel.text = transactionBalanceLabelString
        paybillAccountLabel.text = paybillAccountLabelString

        transactionIdValue.text = mpesaTransaction.id.toString()
        transactionDateValue.text = formatDate(mpesaTransaction.transaction_date.toString())
        transactorValue.text = getTitleTextByTransactionType(mpesaTransaction)
        descriptionValue.text = mpesaTransaction.description
        transactionCostValue.text = mpesaTransaction.transactionCost.toString() + "/-"
        transactionBalanceValue.text = mpesaTransaction?.mpesaBalance?.let {
            addCommasToDoubleValue(
                it
            ).toString()
        } + "/-"

        smsTextValue.text = smsTextValueString
        smsTextValue.setOnClickListener{
            mpesaTransaction?.smsText?.let { it1 -> showTextModal(this, it1) }
        }

        if(mpesaTransaction.transaction_type == "paybill"){
            paybillAccountLayout.visibility = View.VISIBLE
            paybillAccountValue.text = mpesaTransaction.paybillAcount
            val layoutParams = smsTextLayout.layoutParams as LinearLayout.LayoutParams
            layoutParams.bottomMargin = 0
            smsTextLayout.layoutParams = layoutParams
        }


    }

    fun showTextModal(context: Context, text: String) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle("SMS Text")
        builder.setMessage(text)
        builder.setPositiveButton("OK") { dialog, which ->
            // Dismiss the dialog when OK button is clicked
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun setupChips(mpesaTransaction: MpesaTransaction) {
        chipCode.text = mpesaTransaction.mpesa_code
        val colorStateList = ColorStateList.valueOf(transactionColor)
        chipType.chipBackgroundColor= colorStateList
        chipType.text = mpesaTransaction.transaction_type?.let { capitalizeEachWord(it) }
        setContrastTextColor(chipType)
        setContrastTextColor(chipCode)


    }

    private fun setupAmountTextView(
        mpesaTransaction: MpesaTransaction
    ) {
        val isPositiveAmount =
            mpesaTransaction.transaction_type == "deposit" || mpesaTransaction.transaction_type == "receival" || mpesaTransaction.transaction_type == "reverse"
        val formattedAmount =
            formatAmountWithColor(mpesaTransaction?.amount!!, isPositiveAmount, this)
        transactionAmountView.text = formattedAmount

    }

    fun formatAmountWithColor(amount: Double, isPositive: Boolean, context: Context): CharSequence {
        val formattedAmount = amount
        var spannableString = SpannableString("$formattedAmount/-")


        // Assuming amountCard is the MaterialCardView and textView is the TextView
        val backgroundColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (amountCard.background as? GradientDrawable)?.colors?.get(0) ?: this.transactionColor
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

    fun setAvatarView(holder: MpesaTransactionAdapter.TransactionViewHolder, text: String?, color: Int) {
        holder.avatarView.apply {
            this.text = text
            highlightBorderColorEnd = color
            isAnimating = false
        }
        //stopAnimating(5000, holder.avatarView)
    }

    fun getTitleTextByTransactionType(transaction: MpesaTransaction): String {
        return when (transaction.transaction_type) {
            "topup", "send_money", "paybill", "till", "reverse" -> {
                transaction.recipient?.name?.let { capitalizeEachWord(it) } ?: ""
            }
            "deposit", "withdraw" -> {
                transaction.mpesaDepositor?.let { capitalizeEachWord(it) } ?: ""
            }
            "receival" -> {
                transaction.sender?.name?.let { capitalizeEachWord(it) } ?: ""
            }
            else -> ""
        }
    }
    fun getColorAvatar(context: Context, transactionType: String): Int {
        return when (transactionType) {
            "topup" -> R.color.aqua_color
            "send_money" -> R.color.orange_color
            "deposit" -> R.color.light_green_color
            "paybill" -> R.color.yellow_color
            "till" -> R.color.purple_color
            "receival" -> R.color.pink_color
            "withdraw" -> R.color.brown_color
            "reverse" -> R.color.grey_color
            else -> android.R.color.black // Default color for unknown type
        }.let {
            ContextCompat.getColor(context, it)
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

    fun removeDecimal(input: String): String {
        val regex = Regex("\\.\\d+")
        return regex.replace(input, "")
    }

    fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US)

        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    fun setContrastTextColor(chip: Chip) {
        val backgroundColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (chip.background as? GradientDrawable)?.colors?.get(0) ?: Color.WHITE
        } else {
            colorPrimary
        }

        // Calculate the contrast ratio between the background color and black
        val contrastWithBlack = ColorUtils.calculateContrast(Color.BLACK, backgroundColor)

        // Calculate the contrast ratio between the background color and white
        val contrastWithWhite = ColorUtils.calculateContrast(Color.WHITE, backgroundColor)

        // Determine the appropriate text color based on the contrast ratio
        val textColor = if (contrastWithBlack > contrastWithWhite) Color.BLACK else Color.WHITE

        chip.setTextColor(textColor)
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

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)

        val chooser = Intent.createChooser(intent, "Share via")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        } else {
            Toast.makeText(context, "No app found to handle this action", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDescriptionChange(value: String) {
        descriptionValue.text = value
    }
}