package com.example.pettysms

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.gson.Gson
import xyz.schwaab.avvylib.AvatarView
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class PettyCashAdapter(
    private val context: Context,
    private var pettyCashList: MutableList<PettyCash>,
    private val fragmentManager: FragmentManager,
    private val pettyCashFragment: PettyCashFragment
) : RecyclerView.Adapter<PettyCashAdapter.PettyCashViewHolder>() {

    inner class PettyCashViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pettyCashCard: MaterialCardView = view.findViewById(R.id.pettyCashCard)
        val logoWaterMark: ImageView = view.findViewById(R.id.logoWaterMark)
        val pettyCashAmountTextView: TextView = view.findViewById(R.id.pettyCashAmountTextView)
        val pettyCashDateTextView: TextView = view.findViewById(R.id.pettyCashDateTextView)
        val transactorNameTextView: TextView = view.findViewById(R.id.transactorNameTextView)
        val pettyCashDescriptionTextView: TextView = view.findViewById(R.id.pettyCashDescriptionTextView)
        val accountChip: TextView = view.findViewById(R.id.accountChip)
        val ownerName: TextView = view.findViewById(R.id.ownerName)
        val avatarView: AvatarView = view.findViewById(R.id.transactorAvatarView)
        val mpesaImage: ImageView = view.findViewById(R.id.mpesaImage)
        val cashImage: ImageView = view.findViewById(R.id.cashImage)
        val cashLabel: TextView = view.findViewById(R.id.cashLabel)
        val linearLayoutChips: LinearLayout = view.findViewById(R.id.linearLayoutChips)
        val pettyCashConstraintLayout: ConstraintLayout = view.findViewById(R.id.pettyCashCardConstraintLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PettyCashViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.petty_cash_card, parent, false)
        return PettyCashViewHolder(view)
    }

    override fun onBindViewHolder(holder: PettyCashViewHolder, position: Int) {
        val pettyCash = pettyCashList[position]


        // Set up transactor name
        val formattedTransactorName = capitalizeEachWord(pettyCash.transactor?.name.toString())
        val colorPrimary = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnPrimary)
        val colorSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceVariant)

        // Set lighter greyed-out style if pettyCashNumber is null or empty
        if (pettyCash.pettyCashNumber.isNullOrEmpty()) {
            holder.pettyCashConstraintLayout.apply {
                alpha = 0.6f // Reduce opacity //Color.parseColor("#E0E0E0") // Lighter grey color
                background = ColorDrawable(colorSurfaceVariant)
            }
            if (pettyCash.description?.contains("Mpesa Transaction Cost") == true){
                holder.pettyCashCard.setOnClickListener{
                }
            }else{
                holder.pettyCashCard.setOnClickListener {
                    // Handle card click, maybe for more details
                    openAddPettyCashFragment(pettyCash, "Edit", fragmentManager, pettyCashFragment = pettyCashFragment)
                }
            }
        }else{
            holder.pettyCashConstraintLayout.apply {
                alpha = 1f // Reset opacity
                background = ColorDrawable(Color.TRANSPARENT)
            }
            // Optional: Add click listeners if needed
            holder.pettyCashCard.setOnClickListener {
                // Handle card click, maybe for more details
                openPettyCashViewerActivity(pettyCash)
            }
        }

        // Binding data to views
        holder.pettyCashAmountTextView.text = formatAmountWithColor(pettyCash.amount ?: 0.0, context = context, isPositive = false) ?: "0.00"
        holder.pettyCashDateTextView.text = formatDate(pettyCash.date.toString()) ?: "No Date"
        holder.transactorNameTextView.text = formattedTransactorName
        holder.pettyCashDescriptionTextView.text = pettyCash.description ?: "No Description"
        holder.accountChip.text = pettyCash.account?.name ?: "No Account"
        holder.ownerName.text = getFirstTwoWords(pettyCash.owner?.name ?: "") ?: "No Owner"

        // Set visibility for payment mode
        holder.mpesaImage.visibility = if (pettyCash.paymentMode == "M-Pesa") View.VISIBLE else View.GONE
        holder.cashImage.visibility = if (pettyCash.paymentMode != "M-Pesa") View.VISIBLE else View.GONE
        holder.cashLabel.visibility = if (pettyCash.paymentMode != "M-Pesa") View.VISIBLE else View.GONE

        loadAvatarAndLogo(holder, pettyCash)

        // Clear previous chips if there are any
        holder.linearLayoutChips.removeAllViews()


        // Create chips
        createChip(holder.linearLayoutChips, pettyCash.pettyCashNumber ?: "N/A", colorPrimary, R.drawable.chip_gradient_border)

        val ownerTruckCount = pettyCash.owner?.let { owner ->
            context.let { ctx ->
                val dbHelper = DbHelper(ctx)
                pettyCash.owner!!.ownerCode?.let { dbHelper.getTruckCountByOwner(it) }
            }
        } ?: 0


        if (pettyCash.trucks?.size == ownerTruckCount){
            createChip(holder.linearLayoutChips, "All Trucks", colorPrimary, R.drawable.chip_gradient_border_truck)
        }
        else {
            pettyCash.trucks?.forEach { truck ->
                createChip(holder.linearLayoutChips, truck.truckNo ?: "N/A", colorPrimary, R.drawable.chip_gradient_border_truck)
            }
        }

        val chipTextViewUser = TextView(context).apply {
            text = pettyCash.user?.name ?: "N/A"
            setTextColor(colorPrimary)
            textSize = 9f
            background = ContextCompat.getDrawable(context, R.drawable.chip_gradient_border_user)
            setTypeface(null, Typeface.BOLD)
            setPadding(8.dpToPx(context), 2.dpToPx(context), 8.dpToPx(context), 2.dpToPx(context))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8.dpToPx(context) // No start margin for the user chip
                marginEnd = 8.dpToPx(context) // Add end margin of 8dp for the user chip
            }
        }
        holder.linearLayoutChips.addView(chipTextViewUser)


    }

    // Helper function to create and add a chip to the LinearLayout
    private fun createChip(parent: LinearLayout, text: String, textColor: Int, backgroundResId: Int) {
        TextView(context).apply {
            this.text = text
            setTextColor(textColor)
            textSize = 9f
            background = ContextCompat.getDrawable(context, backgroundResId)
            setTypeface(null, Typeface.BOLD)
            setPadding(8.dpToPx(context), 2.dpToPx(context), 8.dpToPx(context), 2.dpToPx(context))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8.dpToPx(context) // Add a start margin for the chip
                gravity = Gravity.CENTER_VERTICAL // Center vertically
            }
        }.also { parent.addView(it) }
    }


    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun loadAvatarAndLogo(holder: PettyCashViewHolder, pettyCash: PettyCash) {
        // Load avatar image
        if (!pettyCash.transactor?.transactorProfilePicturePath.isNullOrEmpty()) {
            // Decode the Base64 string into a Bitmap
            val base64String = pettyCash.transactor?.transactorProfilePicturePath!!
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            // Compress the bitmap
            val compressedBitmap = compressImage(bitmap)

            // Set the compressed bitmap into the ImageView
            if (compressedBitmap != null) {
                holder.avatarView.setImageBitmap(compressedBitmap)
            }
        } else {
            holder.avatarView.setImageResource(0) // Clears the image
            holder.avatarView.text = capitalizeEachWord(pettyCash.transactor?.name.toString())
        }

        // Set the highlight border color if avatarColor is not null
        pettyCash.transactor?.avatarColor?.let {
            holder.avatarView.highlightBorderColorEnd = getColorInt(it)
        }

        // Load and set logo image (Handle vector drawable)
        val logoPath = pettyCash.owner?.logoPath
        val logoDrawable: Drawable?

        if (!logoPath.isNullOrEmpty()) {
            // Decode the Base64 string into a Bitmap for the logo (only if the logo is a bitmap)
            val decodedBytes = Base64.decode(logoPath, Base64.DEFAULT)
            val logoBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            logoDrawable = BitmapDrawable(holder.itemView.resources, logoBitmap)
        } else {
            val context = holder.itemView.context
            // Load the vector drawable (no compression needed)
            logoDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_p_logo_foreground)

            if (logoDrawable == null) {
                Log.e("PettyCashAdapter", "Failed to load vector drawable: p_logo_cropped")
            } else {
                Log.d("PettyCashAdapter", "Vector drawable loaded successfully")
            }
        }

        // Set the logo drawable into the ImageView
        holder.logoWaterMark.setImageDrawable(logoDrawable)
    }

    private fun compressImage(bitmap: Bitmap?): Bitmap? {
        // Check if the bitmap is not null
        if (bitmap == null) return null

        val outputStream = ByteArrayOutputStream()
        // Compress the image to JPEG with 80% quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream)

        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }






    private val bitmapCache = mutableMapOf<String, Bitmap>()

    private fun setImageViewFromBase64(imageView: ImageView, base64String: String) {
        // Check if we already have the Bitmap cached
        bitmapCache[base64String]?.let {
            imageView.setImageBitmap(it)
            return
        }

        // Decode the Base64 string into a byte array
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        // Cache the Bitmap
        bitmapCache[base64String] = decodedByte

        // Set the Bitmap to the ImageView
        imageView.setImageBitmap(decodedByte)
    }

    private fun getFirstTwoWords(input: String): String {
        return input.split(" ")
            .take(2)
            .joinToString(" ")
    }

    private fun getColorInt(colorString: String): Int {
        return Color.parseColor(colorString)
    }

    private fun formatAmountWithColor(amount: Double, context: Context, isPositive: Boolean): CharSequence {
        val formattedAmount = amount

        val textColor = if (isPositive) {
            ContextCompat.getColor(context, android.R.color.holo_green_light)
        } else {
            ContextCompat.getColor(context, R.color.red_color)
        }

        val spannableString = SpannableString("$formattedAmount/-")
        spannableString.setSpan(ForegroundColorSpan(textColor), 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
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

    fun addPettyCashItems(newItems: List<PettyCash>) {
        val startIndex = pettyCashList.size
        pettyCashList.addAll(newItems) // Add new items to the existing list
        notifyItemRangeInserted(startIndex, newItems.size) // Notify adapter about the new items
    }

    fun formatDate(inputDate: String): String {
        // Define possible date formats for input
        val dateFormatWithTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateFormatWithoutTime = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Define output date format
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        // Try to parse with both input formats
        val date = try {
            dateFormatWithTime.parse(inputDate)
        } catch (e: Exception) {
            try {
                dateFormatWithoutTime.parse(inputDate)
            } catch (e: Exception) {
                null
            }
        }

        // Return formatted date or original input if parsing failed
        return date?.let { outputFormat.format(it) } ?: inputDate
    }

    private fun openPettyCashViewerActivity(pettyCash: PettyCash) {
        Log.d("PettyCashAdapter", "Opening PettyCashViewer for: ${pettyCash.pettyCashNumber}")
        if (pettyCash.amount!! > 0) {
            val intent = Intent(context, PettyCashViewerActivity::class.java)
            intent.putExtra("petty_cash_number", pettyCash.pettyCashNumber)
            Log.d("PettyCashAdapter", "Starting activity for result")
            pettyCashFragment.startActivityForResult(intent, PettyCashFragment.PETTY_CASH_VIEWER_REQUEST)
        }
    }


    private fun openAddPettyCashFragment(
        pettyCash: PettyCash,
        actionType: String,
        fragmentManager: FragmentManager,
        pettyCashFragment: PettyCashFragment
    ) {
        // Check if the fragment is already displayed
        val existingFragment = fragmentManager.findFragmentByTag("fragment_add_petty_cash")

        if (existingFragment != null && existingFragment.isVisible) {
            // If the fragment is already visible, do nothing
            return
        }

        val dialogFragment = AddPettyCashFragment()
        dialogFragment.setOnAddPettyCashListener(pettyCashFragment)

        val bundle = Bundle().apply {
            pettyCash.id?.let { putInt("pettyCash", it) }  // Pass the PettyCash object
            putString("action", actionType)  // Pass the action as a string (edit or any other value)
        }

        // Set the Bundle as arguments for the fragment
        dialogFragment.arguments = bundle

        // Show the fragment
        dialogFragment.show(fragmentManager, "fragment_add_petty_cash")
    }


    fun updatePettyCashItems(newItems: MutableList<PettyCash>) {
        pettyCashList.clear() // Clear old items
        pettyCashList.addAll(newItems) // Add new items
        notifyDataSetChanged() // Notify adapter to refresh
    }

    fun addItems(newItems: List<PettyCash>) {
        (pettyCashList as MutableList).addAll(newItems)
        notifyDataSetChanged()
    }

    // Find the index of an item by its ID
    fun findItemIndexById(id: Int): Int {
        return pettyCashList.indexOfFirst { it.id == id }
    }

    // Update an item at a specific index
    fun updateItem(index: Int, updatedItem: PettyCash) {
        pettyCashList[index] = updatedItem
        notifyItemChanged(index)
    }

    // Add a new item at the top of the list
    fun addItemToTop(newItem: PettyCash) {

        pettyCashList.add(0, newItem)
        notifyItemInserted(0)

    }

    override fun getItemCount(): Int = pettyCashList.size

    fun removeItem(deletedId: Int) {
        try {
            val position = findItemIndexById(deletedId)
            if (position != -1) {
                pettyCashList.removeAt(position)
                notifyItemRemoved(position)
                // Notify a range to ensure proper animation and binding
                notifyItemRangeChanged(position, pettyCashList.size)
                Log.d("PettyCashAdapter", "Removed item at position $position, remaining items: ${pettyCashList.size}")
            } else {
                Log.d("PettyCashAdapter", "Item with ID $deletedId not found in current list")
            }
        } catch (e: Exception) {
            Log.e("PettyCashAdapter", "Error removing item: ${e.message}")
        }
    }

    fun updateList(newList: List<PettyCash>) {
        pettyCashList.clear()
        pettyCashList.addAll(newList)
        notifyDataSetChanged()
        Log.d("PettyCashAdapter", "Updated list with ${newList.size} items")
    }
}