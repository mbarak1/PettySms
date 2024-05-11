package com.example.pettysms

import android.app.Dialog
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.pettysms.databinding.FragmentEditTransactionBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import xyz.schwaab.avvylib.AvatarView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.properties.Delegates

// TODO: Rename parameter arguments, choose names that match

/**
 * A simple [Fragment] subclass.
 * Use the [EditTransactionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditTransactionFragment : DialogFragment() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var avatarView: AvatarView
    private lateinit var idTextInput: TextInputEditText
    private lateinit var transactorTextInput: TextInputEditText
    private lateinit var dateTextInput: TextInputEditText
    private lateinit var descriptionTextInput: TextInputEditText
    private lateinit var amountTextInput: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var calendar: Calendar

    private var listener: OnDescriptionChangeListener? = null


    private var _binding: FragmentEditTransactionBinding? = null
    private var db_helper: DbHelper? = null
    private var db: SQLiteDatabase? = null
    private val binding get() = _binding!!
    private var colorPrimary by Delegates.notNull<Int>()

    interface OnDescriptionChangeListener {
        fun onDescriptionChange(value: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDescriptionChangeListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnValueSelectedListener")
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PettySMS)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentEditTransactionBinding.inflate(inflater, container, false)
        toolbar = binding.editTransactionToolbar
        avatarView = binding.avatarView
        idTextInput = binding.textFieldId
        transactorTextInput = binding.textFieldTransactor
        dateTextInput = binding.textFieldDate
        descriptionTextInput = binding.textFieldDescription
        amountTextInput = binding.textFieldAmount
        saveButton = binding.saveButton
        db_helper = DbHelper(requireContext())
        db = db_helper?.writableDatabase


        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Edit Transaction"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            closeDialog()
        }

        return binding.root
    }

    private fun closeDialog() {
        dismiss()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the JSON string from arguments
        val myObjectJson = arguments?.getString("mpesaTransactionJson")

        // Convert JSON string back to object using Gson
        val gson = Gson()
        val mpesaTransaction = gson.fromJson(myObjectJson, MpesaTransaction::class.java)
        Toast.makeText(requireContext(), mpesaTransaction.smsText, Toast.LENGTH_LONG).show()

        val transactionColor = getColorAvatar(requireContext(), mpesaTransaction.transaction_type.toString())
        colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0)




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

        setupForm(mpesaTransaction)
        setupButton(mpesaTransaction)
    }

    private fun setupButton(mpesaTransaction: MpesaTransaction) {
        saveButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Warning")
                .setMessage("Are you sure you want to save changes to this transaction?")
                .setIcon(R.drawable.baseline_warning_amber_white_24dp) // Center align the message
                .setNegativeButton("Dismiss") { dialog, which ->
                    // Respond to negative button press
                }
                .setPositiveButton("Confirm") { dialog, which ->
                    mpesaTransaction.id?.let { it1 -> db_helper?.updateTransactionDescription(it1, descriptionTextInput.text.toString()) }
                    listener?.onDescriptionChange(descriptionTextInput.text.toString())
                    closeDialog()
                }
                .show()

        }
    }

    private fun setupForm(mpesaTransaction: MpesaTransaction) {
        idTextInput.setText(mpesaTransaction.id.toString())
        idTextInput.isEnabled = false
        transactorTextInput.setText(getTitleTextByTransactionType(mpesaTransaction))
        transactorTextInput.isEnabled = false
        descriptionTextInput.setText(mpesaTransaction.description)
        amountTextInput.setText(mpesaTransaction.amount?.let { addCommasToDoubleValue(it)})
        amountTextInput.isEnabled = false
        dateTextInput.setText(mpesaTransaction.transaction_date)
        dateTextInput.isEnabled = false
        calendar = Calendar.getInstance()

        if (mpesaTransaction.transaction_date.isNullOrEmpty() || mpesaTransaction.transaction_date == ""){
            dateTextInput.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    showDateTimePickerDialog()
                }
                true
            }
        }





    }

    private fun showDateTimePickerDialog() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(calendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            calendar.timeInMillis = selectedDate
            showTimePickerDialog()
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePickerDialog() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            updateDateTimeEditText()
        }

        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun updateDateTimeEditText() {
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        dateTextInput.setText(dateTimeFormat.format(calendar.time))
    }

    fun getTitleTextByTransactionType(transaction: MpesaTransaction): String {
        return when (transaction.transaction_type) {
            "topup", "send_money", "paybill", "till", "withdraw", "reverse" -> {
                transaction.recipient?.name?.let { capitalizeEachWord(it) } ?: ""
            }
            "deposit" -> {
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EditTransactionFragment.
         */
        // TODO: Rename and change types and number of parameters
    }
}