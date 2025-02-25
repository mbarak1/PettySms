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
import android.text.style.StyleSpan
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
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.gson.Gson
import xyz.schwaab.avvylib.AvatarView
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.example.pettysms.utils.TextHighlighter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewAllPettyCashAdapter(
    private val context: Activity,
    private var pettyCashList: MutableList<PettyCash>,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<ViewAllPettyCashAdapter.PettyCashViewHolder>(),
    FastScrollRecyclerView.SectionedAdapter {

    companion object {
        const val PETTY_CASH_VIEWER_REQUEST = 1001
    }

    class PettyCashViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

    private var searchQuery: String = ""
    private var items = mutableListOf<PettyCash>()
    private var onAddPettyCashListener: AddPettyCashFragment.OnAddPettyCashListener? = null

    init {
        items = pettyCashList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PettyCashViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.petty_cash_card, parent, false)
        return PettyCashViewHolder(view)
    }

    override fun onBindViewHolder(holder: PettyCashViewHolder, position: Int) {
        val pettyCash = items[position]
        val colorPrimary = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnPrimary)
        val colorSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceVariant)

        // Only highlight transactor name
        holder.transactorNameTextView.text = TextHighlighter.highlightText(
            capitalizeEachWord(pettyCash.transactor?.name?.trim() ?: ""),
            searchQuery
        )

        // Set other fields without highlighting
        holder.pettyCashDescriptionTextView.text = pettyCash.description
        holder.pettyCashAmountTextView.text = formatAmountWithColor(
            pettyCash.amount ?: 0.0,
            context = context,
            isPositive = false
        )
        holder.pettyCashDateTextView.text = formatDate(pettyCash.date.toString())
        holder.accountChip.text = pettyCash.account?.name ?: "No Account"
        holder.ownerName.text = getFirstTwoWords(pettyCash.owner?.name ?: "")

        // Set lighter greyed-out style if pettyCashNumber is null or empty
        if (pettyCash.pettyCashNumber.isNullOrEmpty()) {
            holder.pettyCashConstraintLayout.apply {
                alpha = 0.6f
                background = ColorDrawable(colorSurfaceVariant)
            }
            if (pettyCash.description?.contains("Mpesa Transaction Cost") == true) {
                holder.pettyCashCard.setOnClickListener(null)
            } else {
                holder.pettyCashCard.setOnClickListener {
                    openAddPettyCashFragment(pettyCash, "Edit", fragmentManager)
                }
            }
        } else {
            holder.pettyCashConstraintLayout.apply {
                alpha = 1f
                background = ColorDrawable(Color.TRANSPARENT)
            }
            holder.pettyCashCard.setOnClickListener {
                openPettyCashViewerActivity(pettyCash)
            }
        }

        // Set payment mode indicator
        when (pettyCash.paymentMode) {
            "M-Pesa" -> {
                holder.mpesaImage.visibility = View.VISIBLE
                holder.cashImage.visibility = View.GONE
                holder.cashLabel.visibility = View.GONE
            }
            "Cash" -> {
                holder.mpesaImage.visibility = View.GONE
                holder.cashImage.visibility = View.VISIBLE
                holder.cashLabel.visibility = View.VISIBLE
            }
            else -> {
                holder.mpesaImage.visibility = View.GONE
                holder.cashImage.visibility = View.GONE
                holder.cashLabel.visibility = View.GONE
            }
        }

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


        // Add user chip
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
                marginStart = 8.dpToPx(context)
                marginEnd = 8.dpToPx(context)
            }
        }
        holder.linearLayoutChips.addView(chipTextViewUser)

        loadAvatarAndLogo(holder, pettyCash)
    }

    private fun openPettyCashViewerActivity(pettyCash: PettyCash) {
        Log.d("PettyCashAdapter", "Opening PettyCashViewer for: ${pettyCash.pettyCashNumber}")
        if (pettyCash.amount!! > 0) {
            val intent = Intent(context, PettyCashViewerActivity::class.java)
            intent.putExtra("petty_cash_number", pettyCash.pettyCashNumber)
            Log.d("PettyCashAdapter", "Starting activity for result")
            (context as Activity).startActivityForResult(intent, PETTY_CASH_VIEWER_REQUEST)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setSearchQuery(query: String) {
        searchQuery = query
        notifyDataSetChanged()
    }

    fun updateData(newList: List<PettyCash>, isFilteredData: Boolean = false) {
        try {
            Log.d("ViewAllPettyCashAdapter", """
            Data update starting:
            Previous size: ${pettyCashList.size}
            New size: ${newList.size}
            Is filtered: $isFilteredData
        """.trimIndent())

            if (isFilteredData) {
                // For filtered/sorted data, replace the entire list
                pettyCashList.clear()
                pettyCashList.addAll(newList)
                notifyDataSetChanged()

                Log.d("ViewAllPettyCashAdapter", "Updated with filtered data, new size: ${pettyCashList.size}")
            } else {
                // For regular updates, use the incremental update logic
                val oldList = pettyCashList.toList()

                // Update existing items
                newList.forEach { newItem ->
                    val existingIndex = pettyCashList.indexOfFirst { it.id == newItem.id }
                    if (existingIndex != -1) {
                        pettyCashList[existingIndex] = newItem
                        notifyItemChanged(existingIndex)
                    }
                }

                // Add new items that don't exist in the current list
                val newItems = newList.filter { newItem ->
                    !pettyCashList.any { it.id == newItem.id }
                }
                if (newItems.isNotEmpty()) {
                    val startPosition = pettyCashList.size
                    pettyCashList.addAll(newItems)
                    notifyItemRangeInserted(startPosition, newItems.size)
                }

                Log.d("ViewAllPettyCashAdapter", """
                Regular update completed:
                Previous size: ${oldList.size}
                New size: ${pettyCashList.size}
                Updated/Added items: ${newItems.size}
            """.trimIndent())
            }
        } catch (e: Exception) {
            Log.e("ViewAllPettyCashAdapter", "Error updating data: ${e.message}")
        }
    }

    fun updateData(newList: List<PettyCash>) {
        // Keep existing items and update/add only what's necessary
        val oldList = pettyCashList.toList()

        Log.d("ViewAllPettyCashAdapter", """
            Data update:
            Previous size: ${oldList.size}
            New size: ${newList.size}
        """.trimIndent())
        
        // Update existing items
        newList.forEach { newItem ->
            val existingIndex = pettyCashList.indexOfFirst { it.id == newItem.id }
            if (existingIndex != -1) {
                pettyCashList[existingIndex] = newItem
                notifyItemChanged(existingIndex)
            }
        }
        
        // Add new items that don't exist in the current list
        val newItems = newList.filter { newItem -> 
            !pettyCashList.any { it.id == newItem.id }
        }
        if (newItems.isNotEmpty()) {
            val startPosition = pettyCashList.size
            pettyCashList.addAll(newItems)
            notifyItemRangeInserted(startPosition, newItems.size)
        }
        
        Log.d("ViewAllPettyCashAdapter", """
            Data update:
            Previous size: ${oldList.size}
            New size: ${pettyCashList.size}
            Updated/Added items: ${newItems.size}
        """.trimIndent())
    }

    fun addMoreItems(newItems: List<PettyCash>) {
        val startPosition = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    private fun setupAvatar(avatarView: AvatarView, name: String) {
        avatarView.apply {
            text = name
        }
    }

    private fun formatDate(inputDate: String): String {
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

    private fun capitalizeEachWord(text: String): String {
        // Split by any number of whitespace characters and filter out empty strings
        return text.split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
            }
    }

    private fun getFirstTwoWords(text: String): String {
        return text.split(" ").take(2).joinToString(" ")
    }

    private fun formatAmountWithColor(amount: Double, context: Context, isPositive: Boolean): CharSequence {
        val formattedAmount = amount

        val textColor = if (isPositive) {
            ContextCompat.getColor(context, android.R.color.holo_green_light)
        } else {
            ContextCompat.getColor(context, R.color.red_color)
        }

        val spannableString = SpannableString("$formattedAmount/-")
        spannableString.setSpan(
            ForegroundColorSpan(textColor), 
            0, 
            spannableString.length, 
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

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
                marginStart = 8.dpToPx(context)
                gravity = Gravity.CENTER_VERTICAL
            }
        }.also { parent.addView(it) }
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun openAddPettyCashFragment(
        pettyCash: PettyCash,
        actionType: String,
        fragmentManager: FragmentManager
    ) {
        val existingFragment = fragmentManager.findFragmentByTag("fragment_add_petty_cash")
        if (existingFragment != null && existingFragment.isVisible) return

        val dialogFragment = AddPettyCashFragment().apply {
            arguments = Bundle().apply {
                pettyCash.id?.let { putInt("pettyCash", it) }
                putString("action", actionType)
            }
            // Set the callback listener
            onAddPettyCashListener?.let { listener -> setOnAddPettyCashListener(listener) }
        }


        dialogFragment.show(fragmentManager, "fragment_add_petty_cash")
    }


    private fun loadAvatarAndLogo(holder: PettyCashViewHolder, pettyCash: PettyCash) {
        // Handle avatar image
        if (!pettyCash.transactor?.transactorProfilePicturePath.isNullOrEmpty()) {
            val base64String = pettyCash.transactor?.transactorProfilePicturePath!!
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            val compressedBitmap = compressImage(bitmap)
            if (compressedBitmap != null) {
                holder.avatarView.setImageBitmap(compressedBitmap)
            }
        } else {
            holder.avatarView.setImageResource(0)
            holder.avatarView.text = capitalizeEachWord(pettyCash.transactor?.name.toString())
        }

        // Set avatar border color
        pettyCash.transactor?.avatarColor?.let {
            holder.avatarView.highlightBorderColorEnd = Color.parseColor(it)
        }

        // Handle logo
        val logoPath = pettyCash.owner?.logoPath
        val logoDrawable: Drawable? = if (!logoPath.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(logoPath, Base64.DEFAULT)
            val logoBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            BitmapDrawable(holder.itemView.resources, logoBitmap)
        } else {
            ContextCompat.getDrawable(context, R.mipmap.ic_p_logo_foreground)
        }
        holder.logoWaterMark.setImageDrawable(logoDrawable)
    }

    private fun compressImage(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    // Add this method for fast scrolling section headers
    override fun getSectionName(position: Int): String {
        val pettyCash = items[position]
        return when {
            !pettyCash.transactor?.name.isNullOrEmpty() -> {
                // Get first letter of transactor name
                pettyCash.transactor?.name?.first()?.uppercase() ?: "#"
            }
            !pettyCash.date.isNullOrEmpty() -> {
                // Return the month and year as section
                try {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .parse(pettyCash.date!!)
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date!!)
                } catch (e: Exception) {
                    pettyCash.date!!.substring(3, 10)
                }
            }
            else -> "#"
        }
    }

    fun filterItems(query: String): List<PettyCash> {
        return items.filter { pettyCash ->
            pettyCash.pettyCashNumber?.contains(query, ignoreCase = true) == true ||
            pettyCash.description?.contains(query, ignoreCase = true) == true ||
            pettyCash.transactor?.name?.contains(query, ignoreCase = true) == true ||
            pettyCash.amount.toString().contains(query, ignoreCase = true)
        }
    }

    fun updateItem(updatedPettyCash: PettyCash) {
        val position = pettyCashList.indexOfFirst { it.id == updatedPettyCash.id }
        if (position != -1) {
            pettyCashList[position] = updatedPettyCash
            notifyItemChanged(position)
            Log.d("ViewAllPettyCashAdapter", "Updated item at position $position")
        }
    }

    fun removeItem(deletedId: Int) {
        try {
            val position = pettyCashList.indexOfFirst { it.id == deletedId }
            if (position != -1) {
                pettyCashList.removeAt(position)
                notifyItemRemoved(position)
                // Notify a range to ensure proper animation and binding
                notifyItemRangeChanged(position, pettyCashList.size)
                Log.d("ViewAllPettyCashAdapter", "Removed item at position $position, remaining items: ${pettyCashList.size}")
            } else {
                Log.d("ViewAllPettyCashAdapter", "Item with ID $deletedId not found in current page")
            }
        } catch (e: Exception) {
            Log.e("ViewAllPettyCashAdapter", "Error removing item: ${e.message}")
        }
    }

    fun updateList(newList: List<PettyCash>) {
        pettyCashList.clear()
        pettyCashList.addAll(newList)
        notifyDataSetChanged()
        Log.d("ViewAllPettyCashAdapter", "Updated list with ${newList.size} items")
    }

    fun setOnAddPettyCashListener(listener: AddPettyCashFragment.OnAddPettyCashListener) {
        this.onAddPettyCashListener = listener
    }

    /**
     * Adds a new item to the top of the list
     */
    fun addItemToTop(newItem: PettyCash) {
        try {
            // Add to the beginning of the list
            pettyCashList.add(0, newItem)
            
            // Also update the filtered items list if it's being used
            if (items !== pettyCashList) {
                items.add(0, newItem)
            }
            
            // Notify adapter about the insertion
            notifyItemInserted(0)
            
            // Notify about potential changes to subsequent items
            notifyItemRangeChanged(0, minOf(5, pettyCashList.size))
            
            Log.d("ViewAllPettyCashAdapter", "Added new item to top: ID=${newItem.id}, Description=${newItem.description}")
        } catch (e: Exception) {
            Log.e("ViewAllPettyCashAdapter", "Error adding item to top: ${e.message}")
        }
    }
} 